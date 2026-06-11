package com.yashwanthsurabhi.shielddns.filter

import android.content.Context
import com.yashwanthsurabhi.shielddns.data.db.ShieldDnsDatabase
import com.yashwanthsurabhi.shielddns.data.entity.BlockDomainEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

enum class BlocklistCategory(val assetFile: String, val displayName: String) {
    ADS("blocklist_ads.txt", "Ads"),
    TRACKERS("blocklist_trackers.txt", "Trackers"),
    MALWARE("blocklist_malware.txt", "Malware"),
    PHISHING("blocklist_phishing.txt", "Phishing"),
    SCAM("blocklist_scam.txt", "Scam"),
    ADULT("blocklist_adult.txt", "Adult"),
    GAMBLING("blocklist_gambling.txt", "Gambling"),
    TELEMETRY("blocklist_telemetry.txt", "Telemetry"),
    CRYPTOSCAM("blocklist_cryptoscom.txt", "Crypto Scams")
}

class BlocklistRepository(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()
    private val allowTrie = DomainTrie()
    private val denyTrie = DomainTrie()

    private val blockDomainDao by lazy { ShieldDnsDatabase.get(context).blockDomainDao() }

    @Volatile
    private var activeBloomFilter: BloomFilter? = null

    private val _loadedCount = MutableStateFlow(0)
    val loadedCount: StateFlow<Int> = _loadedCount.asStateFlow()

    private val _categoryCounts = MutableStateFlow(BlocklistCategory.entries.associateWith { 0 })
    val categoryCounts: StateFlow<Map<BlocklistCategory, Int>> = _categoryCounts.asStateFlow()

    private val _updateState = MutableStateFlow(BlocklistUpdateState())
    val updateState: StateFlow<BlocklistUpdateState> = _updateState.asStateFlow()

    init {
        _updateState.value = _updateState.value.copy(
            lastUpdatedEpochMs = metaFile().takeIf { it.exists() }?.readText()?.toLongOrNull() ?: 0L,
        )
        // Load bloom filter from file on start
        activeBloomFilter = BloomFilterStore.load(context)
        updateCount()
    }

    fun getActiveBloomFilter(): BloomFilter? = activeBloomFilter

    suspend fun loadBundledStarter() = withContext(ioDispatcher) {
        mutex.withLock {
            loadBundledStarterLocked()
        }
    }

    private fun loadBundledStarterLocked() {
        runCatching {
            val starter = context.assets.open("blocklist_starter.txt").bufferedReader().readText()
            val domains = BlocklistParser.parse(starter)
            writeCategoryFiles(BlocklistClassifier.splitAnnoyance(domains))
            rebuildBloomFilterFromDisk()
            updateCount()
        }.onFailure {
            Log.e("BlocklistRepository", "Failed to load bundled starter: ${it.message}")
        }
    }

    suspend fun updateRemoteBlocklists(customUrl: String? = null): Result<Int> = withContext(ioDispatcher) {
        mutex.withLock {
            _updateState.value = _updateState.value.copy(isUpdating = true, lastError = null)
            var error: String? = null
            fun addError(message: String?) { if (error == null && message != null) error = message }

            val result = runCatching {
                // Accumulate per REAL category — threats/adult/gambling come from
                // dedicated curated feeds, not keyword guesses against an ad list.
                val byCategory: Map<BlocklistCategory, MutableSet<String>> =
                    BlocklistCategory.entries.associateWith { linkedSetOf() }

                // 1. General ads/tracker annoyance list (mixed; split coarsely into ads/trackers/telemetry only).
                val annoyanceUrl = customUrl?.takeIf { it.isNotBlank() } ?: BlocklistSources.HAGEZI_PRO
                val annoyanceOk = downloadText(annoyanceUrl)
                    .onSuccess { body -> mergeAnnoyance(BlocklistParser.parse(body), byCategory) }
                    .onFailure { addError(it.message) }
                    .isSuccess
                if (!annoyanceOk && customUrl.isNullOrBlank()) {
                    downloadText(BlocklistSources.STEVEN_BLACK_HOSTS)
                        .onSuccess { body -> mergeAnnoyance(BlocklistParser.parse(body), byCategory) }
                        .onFailure { addError(it.message) }
                }

                // 2. Threat intelligence -> real malware / phishing (assigned directly, no guessing).
                downloadInto(BlocklistSources.HAGEZI_TIF, byCategory.getValue(BlocklistCategory.MALWARE), ::addError)
                downloadInto(BlocklistSources.URLHAUS_HOSTFILE, byCategory.getValue(BlocklistCategory.MALWARE), ::addError)
                downloadInto(BlocklistSources.PHISHING_ARMY, byCategory.getValue(BlocklistCategory.PHISHING), ::addError)

                // 3. Dedicated category feeds.
                downloadInto(BlocklistSources.HAGEZI_NSFW, byCategory.getValue(BlocklistCategory.ADULT), ::addError)
                downloadInto(BlocklistSources.HAGEZI_GAMBLING, byCategory.getValue(BlocklistCategory.GAMBLING), ::addError)

                // Persist whatever we successfully fetched.
                var wroteAny = false
                byCategory.forEach { (category, domains) ->
                    if (domains.isNotEmpty()) {
                        writeDomains(category, domains.toList())
                        wroteAny = true
                    }
                }

                if (!wroteAny && !hasCachedLists()) {
                    loadBundledStarterLocked()
                } else {
                    rebuildBloomFilterFromDisk()
                    updateCount()
                }

                val count = _loadedCount.value
                val now = System.currentTimeMillis()
                metaFile().writeText(now.toString())
                _updateState.value = BlocklistUpdateState(
                    isUpdating = false,
                    lastUpdatedEpochMs = now,
                    lastDomainCount = count,
                    lastError = error,
                )
                if (count == 0) {
                    Result.failure(IllegalStateException(error ?: "No blocklists loaded"))
                } else {
                    Result.success(count)
                }
            }.getOrElse { throwable ->
                _updateState.value = _updateState.value.copy(
                    isUpdating = false,
                    lastError = throwable.message ?: "Blocklist update failed",
                )
                Result.failure(throwable)
            }
            result
        }
    }

    suspend fun updateFromUrl(url: String): Result<Int> = updateRemoteBlocklists(customUrl = url)

    suspend fun refreshFromDisk(
        adsEnabled: Boolean,
        trackersEnabled: Boolean,
        malwareEnabled: Boolean,
        allowlist: Set<String>,
        customDeny: Set<String>,
    ) = withContext(ioDispatcher) {
        mutex.withLock {
            allowTrie.clear()
            allowTrie.insertAll(allowlist)
            denyTrie.clear()
            denyTrie.insertAll(customDeny)
            
            if (activeBloomFilter == null) {
                activeBloomFilter = BloomFilterStore.load(context)
            }
            updateCount()
        }
    }

    fun rebuildBloomFilterFromDisk(): Boolean {
        return runCatching {
            val allDomains = mutableListOf<String>()
            val blockDomainEntities = mutableListOf<BlockDomainEntity>()

            BlocklistCategory.entries.forEach { category ->
                val file = blocklistFile(category)
                if (file.exists()) {
                    val list = BlocklistParser.parse(file.readText())
                    allDomains.addAll(list)
                    list.forEach { domain ->
                        blockDomainEntities.add(
                            BlockDomainEntity(
                                domain = domain,
                                category = category.displayName,
                                source = "Remote List"
                            )
                        )
                    }
                }
            }

            // Write all to Room inside database transaction
            runBlocking {
                blockDomainDao.rebuildBlocklist(blockDomainEntities)
            }

            // Build bloom filter in background
            val filter = BloomFilterBuilder.build(allDomains, version = 1)
            activeBloomFilter = filter
            BloomFilterStore.save(context, filter)
        }.getOrDefault(false)
    }

    fun shouldBlock(
        domain: String,
        adsEnabled: Boolean,
        trackersEnabled: Boolean,
        malwareEnabled: Boolean,
        allowlist: Set<String>,
        customDeny: Set<String>,
    ): BlockDecision {
        val normalized = domain.lowercase().removeSuffix(".")
        if (allowTrie.matches(normalized)) {
            return BlockDecision.Allow
        }
        if (denyTrie.matches(normalized)) {
            return BlockDecision.Block("Custom")
        }

        // Bloom Filter match
        val filter = activeBloomFilter ?: return BlockDecision.Allow
        val mightContain = BloomFilterMatcher.matchRecursive(filter, normalized)

        if (mightContain) {
            val matchedEntity = runBlocking {
                blockDomainDao.get(normalized) ?: run {
                    var parts = normalized.split('.')
                    var found: BlockDomainEntity? = null
                    while (parts.size > 1 && found == null) {
                        parts = parts.drop(1)
                        val parent = parts.joinToString(".")
                        found = blockDomainDao.get(parent)
                    }
                    found
                }
            }

            if (matchedEntity != null) {
                val categoryName = matchedEntity.category
                val enabled = when (categoryName) {
                    "Ads" -> adsEnabled
                    "Trackers" -> trackersEnabled
                    "Malware" -> malwareEnabled
                    else -> true // Fallback/Other categories default to true
                }
                if (enabled) {
                    return BlockDecision.Block(categoryName)
                }
            }
        }

        return BlockDecision.Allow
    }

    private fun updateCount() {
        val counts = runBlocking {
            BlocklistCategory.entries.associateWith { category ->
                blockDomainDao.countByCategory(category.displayName)
            }
        }
        _categoryCounts.value = counts
        _loadedCount.value = counts.values.sum()
    }

    suspend fun getMatchedCategory(domain: String): String? {
        val normalized = domain.lowercase().removeSuffix(".")
        val filter = activeBloomFilter ?: return null
        if (!BloomFilterMatcher.matchRecursive(filter, normalized)) return null
        
        val entity = blockDomainDao.get(normalized) ?: run {
            var parts = normalized.split('.')
            var found: BlockDomainEntity? = null
            while (parts.size > 1 && found == null) {
                parts = parts.drop(1)
                val parent = parts.joinToString(".")
                found = blockDomainDao.get(parent)
            }
            found
        }
        return entity?.category
    }

    fun hasCachedLists(): Boolean =
        BlocklistCategory.entries.any { blocklistFile(it).exists() && blocklistFile(it).length() > 0 }

    private fun writeCategoryFiles(split: Map<BlocklistCategory, List<String>>) {
        split.forEach { (category, domains) -> writeDomains(category, domains) }
    }

    private fun writeDomains(category: BlocklistCategory, domains: List<String>) {
        val file = blocklistFile(category)
        file.writeText(domains.distinct().joinToString("\n"))
    }

    /** Coarsely splits a general annoyance list into ads/trackers/telemetry buckets. */
    private fun mergeAnnoyance(
        domains: List<String>,
        byCategory: Map<BlocklistCategory, MutableSet<String>>,
    ) {
        BlocklistClassifier.splitAnnoyance(domains).forEach { (category, list) ->
            byCategory.getValue(category).addAll(list)
        }
    }

    /** Downloads a category-specific feed and assigns every domain to [sink] verbatim. */
    private fun downloadInto(url: String, sink: MutableSet<String>, onError: (String?) -> Unit) {
        downloadText(url)
            .onSuccess { sink.addAll(BlocklistParser.parse(it)) }
            .onFailure { onError(it.message) }
    }

    private fun downloadText(url: String): Result<String> = runCatching {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code} for $url")
            val body = response.body ?: error("Empty body for $url")
            if (url.endsWith(".gz", ignoreCase = true)) {
                java.util.zip.GZIPInputStream(body.byteStream()).use { gzip ->
                    gzip.bufferedReader().readText()
                }
            } else {
                body.string().takeIf { it.isNotBlank() } ?: error("Empty body for $url")
            }
        }
    }

    private fun blocklistFile(category: BlocklistCategory): File =
        File(context.filesDir, category.assetFile)

    private fun metaFile(): File = File(context.filesDir, "blocklist_last_updated.txt")
}

// Utility Log class to prevent package clashes
object Log {
    fun d(tag: String, msg: String) { android.util.Log.d(tag, msg) }
    fun w(tag: String, msg: String) { android.util.Log.w(tag, msg) }
    fun e(tag: String, msg: String, t: Throwable? = null) { android.util.Log.e(tag, msg, t) }
}

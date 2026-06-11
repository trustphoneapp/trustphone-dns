package com.yashwanthsurabhi.shielddns.ui

import android.app.Application
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yashwanthsurabhi.shielddns.ShieldDnsApp
import com.yashwanthsurabhi.shielddns.data.entity.AppRuleEntity
import com.yashwanthsurabhi.shielddns.data.entity.AppRuleMode
import com.yashwanthsurabhi.shielddns.data.entity.BlockedQueryEntity
import com.yashwanthsurabhi.shielddns.data.store.AppSettings
import com.yashwanthsurabhi.shielddns.dns.DnsServerConfig
import com.yashwanthsurabhi.shielddns.firewall.InstalledApp
import com.yashwanthsurabhi.shielddns.rules.NetworkType
import com.yashwanthsurabhi.shielddns.rules.ScheduleManager
import com.yashwanthsurabhi.shielddns.vpn.DnsBlockerService
import kotlinx.coroutines.flow.MutableSharedFlow
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ShieldViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as ShieldDnsApp).container

    private companion object {
        const val PROTECTION_ON_HINT =
            "Protection on. If sites fail to load, set Private DNS to Off in Android network settings."
    }
    private val appContext = application.applicationContext

    val settings: StateFlow<AppSettings> = container.settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val blockedQueries: StateFlow<List<BlockedQueryEntity>> =
        container.blockedQueryDao.observeRecent()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appRules: StateFlow<List<AppRuleEntity>> =
        container.appRuleRepository.rules
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blocklistCount: StateFlow<Int> = container.blocklistRepository.loadedCount
    val blocklistCategoryCounts: StateFlow<Map<com.yashwanthsurabhi.shielddns.filter.BlocklistCategory, Int>> =
        container.blocklistRepository.categoryCounts

    val blocklistUpdateState = container.blocklistRepository.updateState

    val networkType: StateFlow<NetworkType> = container.networkWatcher.networkType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkType.OTHER)

    val isPro: StateFlow<Boolean> = combine(
        settings,
        container.billingManager.isPro,
    ) { s, billingPro -> s.isPro || billingPro }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val billingUiState = container.billingManager.uiState

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredBlocked: StateFlow<List<BlockedQueryEntity>> = combine(
        blockedQueries,
        _searchQuery,
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.domain.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _vpnPermissionIntent = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val vpnPermissionIntent: SharedFlow<Intent> = _vpnPermissionIntent.asSharedFlow()

    private val _requestNotificationPermission = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestNotificationPermission: SharedFlow<Unit> = _requestNotificationPermission.asSharedFlow()

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val blocklistUpdateMutex = Mutex()

    init {
        viewModelScope.launch {
            container.appRuleRepository.refreshCache()
            _installedApps.value = container.packageResolver.installedApps()
            ensureNotificationPermission()
        }
    }

    private suspend fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                _requestNotificationPermission.emit(Unit)
            }
        }
    }

    fun toggleProtection(enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                ensureNotificationPermission()
                val started = DnsBlockerService.prepareOrStart(appContext)
                if (started) {
                    container.settingsStore.setProtection(true)
                    _userMessage.emit(PROTECTION_ON_HINT)
                } else {
                    val prepare = android.net.VpnService.prepare(appContext)
                    if (prepare != null) {
                        _vpnPermissionIntent.emit(prepare)
                    } else {
                        DnsBlockerService.start(appContext)
                        container.settingsStore.setProtection(true)
                        _userMessage.emit(PROTECTION_ON_HINT)
                    }
                }
            } else {
                container.settingsStore.setProtection(false)
                DnsBlockerService.stop(appContext)
                _userMessage.emit("Protection disabled")
            }
        }
    }

    fun onVpnPermissionGranted() {
        viewModelScope.launch {
            DnsBlockerService.start(appContext)
            container.settingsStore.setProtection(true)
            _userMessage.emit("VPN approved — $PROTECTION_ON_HINT")
        }
    }

    fun setListEnabled(ads: Boolean? = null, trackers: Boolean? = null, malware: Boolean? = null) {
        viewModelScope.launch {
            ads?.let { container.settingsStore.setAdsEnabled(it) }
            trackers?.let { container.settingsStore.setTrackersEnabled(it) }
            malware?.let { container.settingsStore.setMalwareEnabled(it) }
            refreshBlocklistsNow()
            _userMessage.emit("Blocklists updated")
        }
    }

    fun refreshBlocklists() {
        viewModelScope.launch {
            refreshBlocklistsNow()
        }
    }

    private suspend fun refreshBlocklistsNow() {
        val s = settings.first()
        container.blocklistRepository.refreshFromDisk(
            s.adsListEnabled,
            s.trackersListEnabled,
            s.malwareListEnabled,
            s.allowlist,
            s.customDeny,
        )
    }

    fun updateBlocklistsNow() {
        viewModelScope.launch {
            if (blocklistUpdateMutex.isLocked) {
                _userMessage.emit("Blocklist update already running")
                return@launch
            }
            blocklistUpdateMutex.withLock {
                val s = settings.first()
                val customUrl = s.customBlocklistUrl.takeIf { s.isPro && it.isNotBlank() }
                val result = container.blocklistRepository.updateRemoteBlocklists(customUrl)
                refreshBlocklistsNow()
                result.fold(
                    onSuccess = { count ->
                        _userMessage.emit("Blocklists updated — $count domains loaded")
                    },
                    onFailure = {
                        _userMessage.emit("Update failed — using cached lists")
                    },
                )
            }
        }
    }

    fun setCustomUrl(url: String) {
        viewModelScope.launch {
            if (!isPro.first()) {
                _userMessage.emit("Pro required for custom blocklist URLs")
                return@launch
            }
            val trimmed = url.trim()
            if (trimmed.isNotBlank() && !trimmed.isValidHttpsUrl()) {
                _userMessage.emit("Enter a valid HTTPS blocklist URL")
                return@launch
            }
            container.settingsStore.setCustomUrl(trimmed)
            _userMessage.emit(if (trimmed.isBlank()) "Custom URL cleared" else "Custom URL saved")
        }
    }

    fun setAllowlist(domains: Set<String>) {
        viewModelScope.launch {
            container.settingsStore.setAllowlist(domains)
            refreshBlocklistsNow()
            _userMessage.emit("Allowlist saved (${domains.size} domains)")
        }
    }

    fun setCustomDeny(domains: Set<String>) {
        viewModelScope.launch {
            container.settingsStore.setCustomDeny(domains)
            refreshBlocklistsNow()
            _userMessage.emit("Deny rules saved (${domains.size} domains)")
        }
    }

    fun setAppRule(packageName: String, label: String, mode: AppRuleMode) {
        viewModelScope.launch {
            if (!isPro.first()) {
                _userMessage.emit("Pro required for per-app rules")
                return@launch
            }
            container.appRuleRepository.setRule(packageName, label, mode)
            _userMessage.emit("$label → ${mode.displayMessage()}")
        }
    }

    fun setSchedule(enabled: Boolean, start: Int, end: Int) {
        viewModelScope.launch {
            if (!isPro.first()) {
                _userMessage.emit("Pro required for schedules")
                return@launch
            }
            container.settingsStore.setScheduleEnabled(enabled)
            container.settingsStore.setSchedule(start, end)
            _userMessage.emit(if (enabled) "Schedule enabled" else "Schedule disabled")
        }
    }

    fun setNetworkRules(mobileOnly: Boolean, wifiOnly: Boolean) {
        viewModelScope.launch {
            if (!isPro.first()) {
                _userMessage.emit("Pro required for network rules")
                return@launch
            }
            container.settingsStore.setBlockOnlyMobile(mobileOnly)
            container.settingsStore.setBlockOnlyWifi(wifiOnly)
            _userMessage.emit("Network rules saved")
        }
    }

    fun setUpstream(dns: String) {
        viewModelScope.launch {
            val upstream = DnsServerConfig.normalizedIpv4OrDefault(dns)
            container.settingsStore.setUpstream(upstream)
            val message = if (upstream == dns.trim()) {
                "Upstream DNS set to $upstream"
            } else {
                "Invalid DNS address — using $upstream"
            }
            _userMessage.emit(message)
        }
    }

    fun setUseDoh(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsStore.setUseDoh(enabled)
            _userMessage.emit(if (enabled) "DoH enabled" else "DoH disabled")
        }
    }

    fun setStartOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsStore.setStartOnBoot(enabled)
            _userMessage.emit(if (enabled) "Start on boot enabled" else "Start on boot disabled")
        }
    }

    fun setDarkTheme(enabled: Boolean?) {
        viewModelScope.launch {
            container.settingsStore.setDarkTheme(enabled)
            _userMessage.emit("Theme updated")
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun exportSettings(): String {
        return kotlinx.coroutines.runBlocking {
            container.settingsStore.exportJson(settings.first())
        }
    }

    fun importSettings(json: String) {
        viewModelScope.launch {
            if (json.isBlank()) {
                _userMessage.emit("Paste JSON before importing")
                return@launch
            }
            container.settingsStore.importJson(json)
            refreshBlocklists()
            _userMessage.emit("Settings imported")
        }
    }

    fun purchasePro(activity: Activity) {
        val launched = container.billingManager.launchProPurchase(activity)
        viewModelScope.launch {
            _userMessage.emit(
                if (launched) {
                    "Opening Google Play checkout"
                } else {
                    "Billing is still loading. Try again in a moment."
                },
            )
        }
    }

    fun scheduleLabel(settings: AppSettings): String =
        if (!settings.scheduleEnabled) "Off"
        else "${ScheduleManager.formatMinutes(settings.scheduleStartMinutes)} – " +
            ScheduleManager.formatMinutes(settings.scheduleEndMinutes)

    val activeDohUrl: String
        get() = container.upstreamResolver.activeDohUrl

    val resolverLatencyMs: Long
        get() = container.upstreamResolver.averageLatencyMs

    val resolverFailureCount: Long
        get() = container.upstreamResolver.dohFallbackCount

    val resolverSwitchHistory: List<String>
        get() = synchronized(container.upstreamResolver.resolverSwitchHistory) {
            container.upstreamResolver.resolverSwitchHistory.toList()
        }

    val isBloomFilterLoaded: Boolean
        get() = container.blocklistRepository.getActiveBloomFilter() != null

    val totalBlockedDomains: Int
        get() = container.blocklistRepository.loadedCount.value

    val estimatedBloomRamBytes: String
        get() {
            val filter = container.blocklistRepository.getActiveBloomFilter() ?: return "0 KB"
            val kb = filter.bits.size / 1024.0
            return "%.2f KB".format(kb)
        }

    val databaseSizeBytes: String
        get() {
            val dbFile = appContext.getDatabasePath("shield_dns.db")
            if (!dbFile.exists()) return "0 KB"
            val kb = dbFile.length() / 1024.0
            return "%.2f KB".format(kb)
        }

    fun setShareTelemetry(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsStore.setShareTelemetry(enabled)
            com.yashwanthsurabhi.shielddns.firebase.FirebaseTracker.setAnalyticsConsent(enabled)
            val msg = if (enabled) "Anonymized analytics enabled" else "Anonymized analytics disabled"
            _userMessage.emit(msg)
        }
    }

    fun setActiveProfile(profileKey: String) {
        viewModelScope.launch {
            container.settingsStore.setActiveProfile(profileKey)
            _userMessage.emit("Profile switched to $profileKey")
        }
    }

    fun setMaxAllowedRiskScore(score: Int) {
        viewModelScope.launch {
            container.settingsStore.setMaxAllowedRiskScore(score)
            _userMessage.emit("Max risk score set to $score")
        }
    }

    fun exportLogsToCsv(): String {
        return container.settingsStore.exportLogsToCsv(blockedQueries.value)
    }
}

private fun AppRuleMode.displayMessage(): String = when (this) {
    AppRuleMode.DEFAULT -> "Default"
    AppRuleMode.BYPASS -> "Bypass DNS filtering"
    AppRuleMode.BLOCK -> "Block DNS"
}

private fun String.isValidHttpsUrl(): Boolean {
    val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return false
    return uri.scheme == "https" && !uri.host.isNullOrBlank()
}

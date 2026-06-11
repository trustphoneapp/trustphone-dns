package com.yashwanthsurabhi.shielddns.benchmark

import android.content.Context
import android.util.Log
import com.yashwanthsurabhi.shielddns.data.db.ShieldDnsDatabase
import com.yashwanthsurabhi.shielddns.data.entity.BlockDomainEntity
import com.yashwanthsurabhi.shielddns.filter.BloomFilterBuilder
import com.yashwanthsurabhi.shielddns.filter.BloomFilterMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BlocklistBenchmark {
    private const val TAG = "BlocklistBenchmark"

    suspend fun runSuite(context: Context, domainCount: Int): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting benchmark test for $domainCount fake domains...")
        
        // 1. Generate fake domains
        val domains = ArrayList<String>(domainCount)
        val entities = ArrayList<BlockDomainEntity>(domainCount)
        for (i in 0 until domainCount) {
            val domain = "ads-tracker-scam-blocklist-node-$i.doubleclick.net"
            domains.add(domain)
            entities.add(BlockDomainEntity(domain, "Ads", "Benchmark Test"))
        }

        // 2. Measure Bloom Filter Rebuild Time
        val startRebuild = System.nanoTime()
        val filter = BloomFilterBuilder.build(domains, version = 1)
        val rebuildDurationMs = (System.nanoTime() - startRebuild) / 1_000_000.0

        // 3. Measure Database Transaction Insertion Time
        val db = ShieldDnsDatabase.get(context)
        val startDb = System.nanoTime()
        db.blockDomainDao().rebuildBlocklist(entities)
        val dbDurationMs = (System.nanoTime() - startDb) / 1_000_000.0

        // 4. Measure Bloom Filter RAM size
        val ramSizeKb = filter.bits.size / 1024.0

        // 5. Measure Lookup Decision Latencies
        // Allowed query test
        val allowedQueries = listOf(
            "google.com", "wikipedia.org", "github.com", "android.com", "kotlinlang.org"
        )
        val startAllowed = System.nanoTime()
        for (query in allowedQueries) {
            BloomFilterMatcher.matchRecursive(filter, query)
        }
        val allowedLatencyMs = ((System.nanoTime() - startAllowed) / allowedQueries.size.toDouble()) / 1_000_000.0

        // Blocked query test
        val blockedQueries = listOf(
            "ads-tracker-scam-blocklist-node-10.doubleclick.net",
            "ads-tracker-scam-blocklist-node-100.doubleclick.net",
            "ads-tracker-scam-blocklist-node-1000.doubleclick.net"
        )
        val startBlocked = System.nanoTime()
        for (query in blockedQueries) {
            val hit = BloomFilterMatcher.matchRecursive(filter, query)
            if (hit) {
                db.blockDomainDao().get(query)
            }
        }
        val blockedLatencyMs = ((System.nanoTime() - startBlocked) / blockedQueries.size.toDouble()) / 1_000_000.0

        val result = BenchmarkResult(
            domainCount = domainCount,
            ramSizeKb = ramSizeKb,
            rebuildTimeMs = rebuildDurationMs,
            dbInsertTimeMs = dbDurationMs,
            allowedLookupTimeMs = allowedLatencyMs,
            blockedLookupTimeMs = blockedLatencyMs
        )
        Log.d(TAG, "Completed benchmark for $domainCount domains: $result")
        
        // Clean up database after benchmark run
        db.blockDomainDao().deleteAll()
        
        result
    }

    data class BenchmarkResult(
        val domainCount: Int,
        val ramSizeKb: Double,
        val rebuildTimeMs: Double,
        val dbInsertTimeMs: Double,
        val allowedLookupTimeMs: Double,
        val blockedLookupTimeMs: Double
    )
}

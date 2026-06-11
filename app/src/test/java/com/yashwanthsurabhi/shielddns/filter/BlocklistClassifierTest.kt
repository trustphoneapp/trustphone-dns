package com.yashwanthsurabhi.shielddns.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlocklistClassifierTest {

    @Test
    fun categorizesAdsDomains() {
        assertEquals(BlocklistCategory.ADS, BlocklistClassifier.categorize("doubleclick.net"))
        assertEquals(BlocklistCategory.ADS, BlocklistClassifier.categorize("adserver.example.com"))
    }

    @Test
    fun categorizesMalwareDomains() {
        assertEquals(BlocklistCategory.MALWARE, BlocklistClassifier.categorize("malware-site.example"))
        assertEquals(BlocklistCategory.MALWARE, BlocklistClassifier.categorize("evil-virus.example"))
    }

    @Test
    fun categorizesTrackersByDefault() {
        assertEquals(BlocklistCategory.TRACKERS, BlocklistClassifier.categorize("some-unknown-tracker.com"))
    }

    @Test
    fun categorizesPhishingAndTelemetry() {
        assertEquals(BlocklistCategory.PHISHING, BlocklistClassifier.categorize("phishing-site.example"))
        assertEquals(BlocklistCategory.TELEMETRY, BlocklistClassifier.categorize("analytics.example.com"))
    }

    @Test
    fun splitAnnoyanceOnlyProducesAdsTrackersTelemetry() {
        val split = BlocklistClassifier.splitAnnoyance(
            listOf(
                "doubleclick.net",          // ads
                "analytics.example.com",    // telemetry
                "some-unknown-host.com",    // trackers (default)
                "malware-virus.example",    // must NOT be fabricated into MALWARE
                "porn-site.example",        // must NOT be fabricated into ADULT
            ),
        )
        // Only the three annoyance buckets exist — never threat/adult/gambling.
        assertEquals(setOf(BlocklistCategory.ADS, BlocklistCategory.TRACKERS, BlocklistCategory.TELEMETRY), split.keys)
        assertEquals(listOf("doubleclick.net"), split[BlocklistCategory.ADS])
        assertEquals(listOf("analytics.example.com"), split[BlocklistCategory.TELEMETRY])
        // Threat/adult-looking domains fall through to trackers, NOT a fabricated category.
        val trackers = split[BlocklistCategory.TRACKERS].orEmpty()
        assertTrue(trackers.contains("malware-virus.example"))
        assertTrue(trackers.contains("porn-site.example"))
        assertTrue(trackers.contains("some-unknown-host.com"))
    }
}

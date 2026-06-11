package com.yashwanthsurabhi.shielddns.filter

object BlocklistClassifier {

    private val malwareKeywords = listOf("malware", "virus", "ransomware", "trojan", "spyware", "badware", "c2", "dropper")
    private val phishingKeywords = listOf("phish", "spoof", "login-update", "phishing")
    private val scamKeywords = listOf("scam", "fraud", "fake", "scams", "blackmail")
    private val adultKeywords = listOf("porn", "adult", "sex", "xxx", "erotic", "nsfw", "tube")
    private val gamblingKeywords = listOf("gamble", "casino", "betting", "poker", "lottery", "blackjack", "slot")
    private val telemetryKeywords = listOf("telemetry", "metrics", "analytics", "stats", "beacon", "measure", "amplitude")
    private val cryptoKeywords = listOf("crypto", "miner", "cryptominer", "coinhive", "minergate", "blockchain", "bitcoin")
    private val adsKeywords = listOf(
        "doubleclick", "googlesyndication", "googleadservices", "syndication", "adservice",
        "banner", "popunder", "popup", "taboola", "outbrain", "moatads", "amazon-adsystem",
        "adserver", "adnxs", "adsrvr", "advertising", "adform", "adtech", "ads"
    )

    fun categorize(domain: String): BlocklistCategory {
        val d = domain.lowercase()
        val labels = d.split('.')

        if (malwareKeywords.any { keyword -> labels.any { it.contains(keyword) } }) {
            return BlocklistCategory.MALWARE
        }
        if (phishingKeywords.any { keyword -> labels.any { it.contains(keyword) } }) {
            return BlocklistCategory.PHISHING
        }
        if (scamKeywords.any { keyword -> labels.any { it.contains(keyword) } }) {
            return BlocklistCategory.SCAM
        }
        if (adultKeywords.any { keyword -> labels.any { it.contains(keyword) } }) {
            return BlocklistCategory.ADULT
        }
        if (gamblingKeywords.any { keyword -> labels.any { it.contains(keyword) } }) {
            return BlocklistCategory.GAMBLING
        }
        if (telemetryKeywords.any { keyword -> labels.any { it.contains(keyword) } }) {
            return BlocklistCategory.TELEMETRY
        }
        if (cryptoKeywords.any { keyword -> labels.any { it.contains(keyword) } }) {
            return BlocklistCategory.CRYPTOSCAM
        }
        if (adsKeywords.any { keyword -> labels.any { it.contains(keyword) } || d.contains(keyword) }) {
            return BlocklistCategory.ADS
        }
        return BlocklistCategory.TRACKERS
    }

    fun split(domains: List<String>): Map<BlocklistCategory, List<String>> {
        val buckets = BlocklistCategory.entries.associateWith { mutableListOf<String>() }
        domains.forEach { domain ->
            buckets.getValue(categorize(domain)).add(domain)
        }
        return buckets.mapValues { it.value }
    }

    /**
     * Splits a general ads/tracker "annoyance" list ONLY into ads, trackers and
     * telemetry. It never assigns malware/phishing/scam/adult/gambling — those
     * categories must come from dedicated, curated feeds rather than keyword guesses,
     * so the per-category counts shown to users reflect reality.
     */
    fun splitAnnoyance(domains: List<String>): Map<BlocklistCategory, List<String>> {
        val ads = mutableListOf<String>()
        val trackers = mutableListOf<String>()
        val telemetry = mutableListOf<String>()
        domains.forEach { domain ->
            val d = domain.lowercase()
            val labels = d.split('.')
            when {
                telemetryKeywords.any { k -> labels.any { it.contains(k) } } -> telemetry.add(domain)
                adsKeywords.any { k -> labels.any { it.contains(k) } || d.contains(k) } -> ads.add(domain)
                else -> trackers.add(domain)
            }
        }
        return mapOf(
            BlocklistCategory.ADS to ads,
            BlocklistCategory.TRACKERS to trackers,
            BlocklistCategory.TELEMETRY to telemetry,
        )
    }
}

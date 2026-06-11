package com.yashwanthsurabhi.shielddns.reputation

import java.util.Locale

object ReputationEngine {

    private val SUSPICIOUS_TLDS = setOf(
        "zip", "mov", "fit", "top", "tk", "ga", "cf", "ml", "gq", 
        "work", "click", "men", "bid", "country", "stream", "download"
    )

    private val FAMOUS_BRANDS = listOf(
        "google", "paypal", "facebook", "amazon", "apple", "microsoft", 
        "netflix", "instagram", "twitter", "linkedin", "dropbox", "adobe", 
        "yahoo", "github", "gitlab", "bitbucket", "chase", "bankofamerica"
    )

    private val SUSPICIOUS_KEYWORDS = listOf(
        "login", "verify", "secure", "update", "signin", "support", 
        "billing", "account", "checkout", "security", "confirm", "wallet"
    )

    data class ReputationResult(
        val riskScore: Int,
        val category: String,
        val reason: String
    )

    fun evaluate(domain: String): ReputationResult {
        val normalized = domain.trim().lowercase(Locale.US).removeSuffix(".")
        if (normalized.isEmpty()) {
            return ReputationResult(0, "Safe", "Empty domain")
        }

        var score = 0
        val reasons = mutableListOf<String>()

        // 1. IDN Homograph check
        if (normalized.startsWith("xn--")) {
            score += 40
            reasons.add("Internationalized Domain Name (Punycode) detected (possible homograph spoofing)")
        }

        // 2. Suspicious TLD check
        val parts = normalized.split('.')
        val tld = parts.lastOrNull()
        if (tld != null && SUSPICIOUS_TLDS.contains(tld)) {
            score += 25
            reasons.add("Suspicious top-level domain (.$tld) frequently used in malicious attacks")
        }

        // 3. Typosquatting check
        val domainLabels = parts.dropLast(1)
        var typosquattingBrand: String? = null
        for (label in domainLabels) {
            for (brand in FAMOUS_BRANDS) {
                if (label != brand && isTyposquatting(label, brand)) {
                    typosquattingBrand = brand
                    break
                }
            }
            if (typosquattingBrand != null) break
        }

        if (typosquattingBrand != null) {
            score += 55
            reasons.add("Typosquatting detected: domain label is deceptively close to famous brand '$typosquattingBrand'")
        }

        // 4. Suspicious keywords check
        val containsKeyword = SUSPICIOUS_KEYWORDS.any { keyword ->
            normalized.contains(keyword)
        }
        if (containsKeyword) {
            score += 15
            reasons.add("Contains urgent phishing keywords (login, verify, secure, etc.)")
        }

        // Final score capping and classification
        val finalScore = score.coerceIn(0, 100)
        val category = when {
            finalScore >= 81 -> "Dangerous"
            finalScore >= 51 -> "Risky"
            finalScore >= 21 -> "Suspicious"
            else -> "Safe"
        }

        val reasonText = if (reasons.isEmpty()) "No threat indicators detected" else reasons.joinToString("; ")
        return ReputationResult(finalScore, category, reasonText)
    }

    private fun isTyposquatting(label: String, brand: String): Boolean {
        if (label.length < 3 || brand.length < 3) return false
        
        // 1. Levenshtein Distance checks (minor edits)
        val distance = levenshteinDistance(label, brand)
        if (distance in 1..2) return true

        // 2. Check if brand is substring of label with other characters (e.g. login-paypal.com)
        if (label.contains(brand) && label.length > brand.length + 2) {
            val cleanLabel = label.replace("-", "").replace("_", "")
            if (cleanLabel.contains(brand)) {
                return true
            }
        }

        return false
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)
        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = Math.min(Math.min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }
}

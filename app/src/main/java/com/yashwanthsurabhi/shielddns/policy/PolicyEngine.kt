package com.yashwanthsurabhi.shielddns.policy

import com.yashwanthsurabhi.shielddns.filter.BlocklistCategory
import com.yashwanthsurabhi.shielddns.filter.BlocklistClassifier
import com.yashwanthsurabhi.shielddns.reputation.ReputationEngine

enum class PolicyProfile(val key: String, val displayName: String) {
    DEFAULT("DEFAULT", "Default"),
    CHILD("CHILD", "Child"),
    TEEN("TEEN", "Teen"),
    FAMILY("FAMILY", "Family"),
    WORK("WORK", "Work"),
    TRAVEL("TRAVEL", "Travel");

    companion object {
        fun fromKey(key: String): PolicyProfile {
            return entries.find { it.key == key } ?: DEFAULT
        }
    }
}

object PolicyEngine {

    data class PolicyResult(
        val shouldBlock: Boolean,
        val reason: String,
        val listName: String
    )

    fun evaluate(
        domain: String,
        packageName: String?,
        profile: PolicyProfile,
        maxAllowedRiskScore: Int,
        adsEnabled: Boolean,
        trackersEnabled: Boolean,
        malwareEnabled: Boolean,
        matchedCategory: String?
    ): PolicyResult {
        // 1. Perform reputation check first
        val repResult = ReputationEngine.evaluate(domain)

        // 2. Evaluate risk score limit override
        if (repResult.riskScore >= maxAllowedRiskScore) {
            return PolicyResult(
                shouldBlock = true,
                reason = "Risk score ${repResult.riskScore} exceeds threshold of $maxAllowedRiskScore: ${repResult.reason}",
                listName = "Threat Prevention"
            )
        }

        // 3. If domain is not on any blocklist and has a safe risk score, allow it instantly!
        if (matchedCategory == null) {
            return PolicyResult(
                shouldBlock = false,
                reason = "Domain is safe (not blacklisted and below risk threshold)",
                listName = "Allowed"
            )
        }

        // 4. Map the SQLite category string to our enum
        val category = when (matchedCategory) {
            "Ads" -> BlocklistCategory.ADS
            "Trackers" -> BlocklistCategory.TRACKERS
            "Malware" -> BlocklistCategory.MALWARE
            "Phishing" -> BlocklistCategory.PHISHING
            "Scam" -> BlocklistCategory.SCAM
            "Adult" -> BlocklistCategory.ADULT
            "Gambling" -> BlocklistCategory.GAMBLING
            "Telemetry" -> BlocklistCategory.TELEMETRY
            "Crypto Scams" -> BlocklistCategory.CRYPTOSCAM
            else -> BlocklistClassifier.categorize(domain)
        }

        // 5. App-specific restrictions (e.g. block gambling/adult on browsers or ads/trackers on kid apps)
        if (packageName != null) {
            val isBrowser = isBrowserApp(packageName)
            val isKidApp = isKidFriendlyApp(packageName)

            if (isBrowser) {
                // Browsers in family profiles should block adult and gambling strictly
                if (profile == PolicyProfile.CHILD || profile == PolicyProfile.TEEN || profile == PolicyProfile.FAMILY) {
                    if (category == BlocklistCategory.ADULT || category == BlocklistCategory.GAMBLING) {
                        return PolicyResult(
                            shouldBlock = true,
                            reason = "Blocked $category on browser app $packageName in family-focused profile",
                            listName = category.displayName
                        )
                    }
                }
            }

            if (isKidApp) {
                // Kid apps should always block ads and trackers
                if (category == BlocklistCategory.ADS || category == BlocklistCategory.TRACKERS) {
                    return PolicyResult(
                        shouldBlock = true,
                        reason = "Blocked ads/trackers for child-targeted app $packageName",
                        listName = category.displayName
                    )
                }
            }
        }

        // 6. Evaluate profile rules
        return when (profile) {
            PolicyProfile.CHILD -> {
                // Child: Blocks everything that is on any blocklist
                PolicyResult(
                    shouldBlock = true,
                    reason = "Blocked $category in Child Profile",
                    listName = category.displayName
                )
            }
            PolicyProfile.TEEN -> {
                // Blocks malware, phishing, scam, cryptoscam, adult, gambling
                val shouldBlock = when (category) {
                    BlocklistCategory.MALWARE,
                    BlocklistCategory.PHISHING,
                    BlocklistCategory.SCAM,
                    BlocklistCategory.CRYPTOSCAM,
                    BlocklistCategory.ADULT,
                    BlocklistCategory.GAMBLING -> true
                    BlocklistCategory.ADS -> adsEnabled
                    BlocklistCategory.TRACKERS, BlocklistCategory.TELEMETRY -> trackersEnabled
                }
                PolicyResult(
                    shouldBlock = shouldBlock,
                    reason = if (shouldBlock) "Blocked $category in Teen Profile" else "Allowed by preference",
                    listName = category.displayName
                )
            }
            PolicyProfile.FAMILY -> {
                // Blocks adult, gambling, scam, malware, phishing, cryptoscam
                val shouldBlock = when (category) {
                    BlocklistCategory.MALWARE,
                    BlocklistCategory.PHISHING,
                    BlocklistCategory.SCAM,
                    BlocklistCategory.CRYPTOSCAM,
                    BlocklistCategory.ADULT,
                    BlocklistCategory.GAMBLING -> true
                    BlocklistCategory.ADS -> adsEnabled
                    BlocklistCategory.TRACKERS, BlocklistCategory.TELEMETRY -> trackersEnabled
                }
                PolicyResult(
                    shouldBlock = shouldBlock,
                    reason = if (shouldBlock) "Blocked $category in Family Profile" else "Allowed by preference",
                    listName = category.displayName
                )
            }
            PolicyProfile.WORK -> {
                // Blocks adult, gambling, cryptoscam, malware, phishing, scam
                val shouldBlock = when (category) {
                    BlocklistCategory.MALWARE,
                    BlocklistCategory.PHISHING,
                    BlocklistCategory.SCAM,
                    BlocklistCategory.CRYPTOSCAM,
                    BlocklistCategory.ADULT,
                    BlocklistCategory.GAMBLING -> true
                    BlocklistCategory.ADS -> adsEnabled
                    BlocklistCategory.TRACKERS, BlocklistCategory.TELEMETRY -> trackersEnabled
                }
                PolicyResult(
                    shouldBlock = shouldBlock,
                    reason = if (shouldBlock) "Blocked $category in Work Profile" else "Allowed by preference",
                    listName = category.displayName
                )
            }
            PolicyProfile.TRAVEL -> {
                // Strict telemetry and tracker/ads blocking to save data, plus malware/scam
                val shouldBlock = when (category) {
                    BlocklistCategory.MALWARE,
                    BlocklistCategory.PHISHING,
                    BlocklistCategory.SCAM,
                    BlocklistCategory.CRYPTOSCAM,
                    BlocklistCategory.TRACKERS,
                    BlocklistCategory.TELEMETRY,
                    BlocklistCategory.ADS -> true
                    BlocklistCategory.ADULT, BlocklistCategory.GAMBLING -> false
                }
                PolicyResult(
                    shouldBlock = shouldBlock,
                    reason = if (shouldBlock) "Blocked $category in Travel Profile to conserve data" else "Allowed by preference",
                    listName = category.displayName
                )
            }
            PolicyProfile.DEFAULT -> {
                // Fallback to standard settings checkboxes
                val shouldBlock = when (category) {
                    BlocklistCategory.MALWARE, BlocklistCategory.PHISHING, BlocklistCategory.SCAM, BlocklistCategory.CRYPTOSCAM -> malwareEnabled
                    BlocklistCategory.ADS -> adsEnabled
                    BlocklistCategory.TRACKERS, BlocklistCategory.TELEMETRY -> trackersEnabled
                    BlocklistCategory.ADULT -> false
                    BlocklistCategory.GAMBLING -> false
                }
                PolicyResult(
                    shouldBlock = shouldBlock,
                    reason = if (shouldBlock) "Standard filter check for $category" else "Allowed by preference",
                    listName = category.displayName
                )
            }
        }
    }

    private fun isBrowserApp(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains("chrome") ||
               lower.contains("browser") ||
               lower.contains("firefox") ||
               lower.contains("opera") ||
               lower.contains("duckduckgo")
    }

    private fun isKidFriendlyApp(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains("kids") ||
               lower.contains("youtube.kids") ||
               lower.contains("learning") ||
               lower.contains("education")
    }
}

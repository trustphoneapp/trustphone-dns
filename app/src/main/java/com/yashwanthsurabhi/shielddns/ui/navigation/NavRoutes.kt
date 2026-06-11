package com.yashwanthsurabhi.shielddns.ui.navigation

object NavRoutes {
    const val HOME = "home"
    const val LISTS = "lists"
    const val ACTIVITY = "activity"
    const val MORE = "more"
    const val ALLOWLIST = "allowlist"
    const val APPS = "apps"
    const val RULES = "rules"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val RESOLVER_HEALTH = "resolver_health"
    const val ADVANCED_ENGINE = "advanced_engine"
    const val PRIVACY_CENTER = "privacy_center"
    const val POLICY_CENTER = "policy_center"
    const val FAMILY_CENTER = "family_center"
    const val THREAT_CENTER = "threat_center"

    private val subScreens = setOf(
        ALLOWLIST, APPS, RULES, STATS, SETTINGS, ABOUT, 
        RESOLVER_HEALTH, ADVANCED_ENGINE, PRIVACY_CENTER,
        POLICY_CENTER, FAMILY_CENTER, THREAT_CENTER
    )

    fun isSubScreen(route: String): Boolean = route in subScreens

    fun title(route: String): String = when (route) {
        ALLOWLIST -> "Allowlist"
        APPS -> "Per-app rules"
        RULES -> "Rules"
        STATS -> "Statistics"
        SETTINGS -> "Settings"
        ABOUT -> "About"
        RESOLVER_HEALTH -> "Resolver Health"
        ADVANCED_ENGINE -> "Advanced Engine"
        PRIVACY_CENTER -> "Privacy Center"
        POLICY_CENTER -> "Policy Center"
        FAMILY_CENTER -> "Family Safety"
        THREAT_CENTER -> "Threat Center"
        else -> "TrustPhone DNS"
    }
}

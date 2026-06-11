package com.yashwanthsurabhi.shielddns.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("shield_dns_settings")

data class AppSettings(
    val protectionEnabled: Boolean = false,
    val adsListEnabled: Boolean = true,
    val trackersListEnabled: Boolean = true,
    val malwareListEnabled: Boolean = true,
    val useDoh: Boolean = false,
    val upstreamDns: String = "1.1.1.1",
    val startOnBoot: Boolean = false,
    val darkTheme: Boolean? = null,
    val allowlist: Set<String> = emptySet(),
    val customDeny: Set<String> = emptySet(),
    val customBlocklistUrl: String = "",
    val scheduleEnabled: Boolean = false,
    val scheduleStartMinutes: Int = 22 * 60,
    val scheduleEndMinutes: Int = 7 * 60,
    val blockOnlyMobile: Boolean = false,
    val blockOnlyWifi: Boolean = false,
    val isPro: Boolean = false,
    val blockedTodayCount: Long = 0,
    val blockedWeekCount: Long = 0,
    val lastStatsResetDay: Long = 0,
    val updateOnlyWifi: Boolean = true,
    val updateOnlyCharging: Boolean = false,
    val shareTelemetry: Boolean = false,
    val activeProfile: String = "DEFAULT",
    val maxAllowedRiskScore: Int = 80
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val PROTECTION = booleanPreferencesKey("protection_enabled")
        val ADS = booleanPreferencesKey("ads_list")
        val TRACKERS = booleanPreferencesKey("trackers_list")
        val MALWARE = booleanPreferencesKey("malware_list")
        val DOH = booleanPreferencesKey("use_doh")
        val UPSTREAM = stringPreferencesKey("upstream_dns")
        val BOOT = booleanPreferencesKey("start_on_boot")
        val DARK = stringPreferencesKey("dark_theme")
        val ALLOWLIST = stringSetPreferencesKey("allowlist")
        val DENY = stringSetPreferencesKey("custom_deny")
        val CUSTOM_URL = stringPreferencesKey("custom_url")
        val SCHEDULE = booleanPreferencesKey("schedule_enabled")
        val SCHED_START = intPreferencesKey("schedule_start")
        val SCHED_END = intPreferencesKey("schedule_end")
        val MOBILE_ONLY = booleanPreferencesKey("block_mobile_only")
        val WIFI_ONLY = booleanPreferencesKey("block_wifi_only")
        val PRO = booleanPreferencesKey("is_pro")
        val BLOCKED_TODAY = longPreferencesKey("blocked_today")
        val BLOCKED_WEEK = longPreferencesKey("blocked_week")
        val STATS_DAY = longPreferencesKey("stats_day")
        val STATS_WEEK_START = longPreferencesKey("stats_week_start")
        val UPDATE_WIFI_ONLY = booleanPreferencesKey("update_wifi_only")
        val UPDATE_CHARGING_ONLY = booleanPreferencesKey("update_charging_only")
        val SHARE_TELEMETRY = booleanPreferencesKey("share_telemetry")
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        val MAX_RISK_SCORE = intPreferencesKey("max_risk_score")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            protectionEnabled = prefs[Keys.PROTECTION] ?: false,
            adsListEnabled = prefs[Keys.ADS] ?: true,
            trackersListEnabled = prefs[Keys.TRACKERS] ?: true,
            malwareListEnabled = prefs[Keys.MALWARE] ?: true,
            useDoh = prefs[Keys.DOH] ?: false,
            upstreamDns = prefs[Keys.UPSTREAM] ?: "1.1.1.1",
            startOnBoot = prefs[Keys.BOOT] ?: false,
            darkTheme = prefs[Keys.DARK]?.let { it == "true" },
            allowlist = prefs[Keys.ALLOWLIST] ?: emptySet(),
            customDeny = prefs[Keys.DENY] ?: emptySet(),
            customBlocklistUrl = prefs[Keys.CUSTOM_URL] ?: "",
            scheduleEnabled = prefs[Keys.SCHEDULE] ?: false,
            scheduleStartMinutes = prefs[Keys.SCHED_START] ?: 22 * 60,
            scheduleEndMinutes = prefs[Keys.SCHED_END] ?: 7 * 60,
            blockOnlyMobile = prefs[Keys.MOBILE_ONLY] ?: false,
            blockOnlyWifi = prefs[Keys.WIFI_ONLY] ?: false,
            isPro = prefs[Keys.PRO] ?: false,
            blockedTodayCount = prefs[Keys.BLOCKED_TODAY] ?: 0,
            blockedWeekCount = prefs[Keys.BLOCKED_WEEK] ?: 0,
            lastStatsResetDay = prefs[Keys.STATS_DAY] ?: 0,
            updateOnlyWifi = prefs[Keys.UPDATE_WIFI_ONLY] ?: true,
            updateOnlyCharging = prefs[Keys.UPDATE_CHARGING_ONLY] ?: false,
            shareTelemetry = prefs[Keys.SHARE_TELEMETRY] ?: false,
            activeProfile = prefs[Keys.ACTIVE_PROFILE] ?: "DEFAULT",
            maxAllowedRiskScore = prefs[Keys.MAX_RISK_SCORE] ?: 80
        )
    }

    suspend fun setProtection(enabled: Boolean) = edit { it[Keys.PROTECTION] = enabled }
    suspend fun setAdsEnabled(enabled: Boolean) = edit { it[Keys.ADS] = enabled }
    suspend fun setTrackersEnabled(enabled: Boolean) = edit { it[Keys.TRACKERS] = enabled }
    suspend fun setMalwareEnabled(enabled: Boolean) = edit { it[Keys.MALWARE] = enabled }
    suspend fun setUseDoh(enabled: Boolean) = edit { it[Keys.DOH] = enabled }
    suspend fun setUpstream(dns: String) = edit { it[Keys.UPSTREAM] = dns }
    suspend fun setStartOnBoot(enabled: Boolean) = edit { it[Keys.BOOT] = enabled }
    suspend fun setDarkTheme(enabled: Boolean?) = edit {
        if (enabled == null) it.remove(Keys.DARK) else it[Keys.DARK] = enabled.toString()
    }
    suspend fun setAllowlist(domains: Set<String>) = edit { it[Keys.ALLOWLIST] = domains }
    suspend fun setCustomDeny(domains: Set<String>) = edit { it[Keys.DENY] = domains }
    suspend fun setCustomUrl(url: String) = edit { it[Keys.CUSTOM_URL] = url }
    suspend fun setScheduleEnabled(enabled: Boolean) = edit { it[Keys.SCHEDULE] = enabled }
    suspend fun setSchedule(start: Int, end: Int) = edit {
        it[Keys.SCHED_START] = start
        it[Keys.SCHED_END] = end
    }
    suspend fun setBlockOnlyMobile(enabled: Boolean) = edit { it[Keys.MOBILE_ONLY] = enabled }
    suspend fun setBlockOnlyWifi(enabled: Boolean) = edit { it[Keys.WIFI_ONLY] = enabled }
    suspend fun setPro(isPro: Boolean) = edit { it[Keys.PRO] = isPro }
    /**
     * Adds [delta] genuine blocks to the running counters, rolling the daily and
     * weekly totals over when the calendar day / 7-day window has advanced.
     * [epochDay] is days-since-epoch (UTC). Called in batches from the VPN service,
     * never per query, to avoid write amplification.
     */
    suspend fun recordBlocked(delta: Long, epochDay: Long) = edit { prefs ->
        if (delta <= 0L) return@edit
        val lastDay = prefs[Keys.STATS_DAY] ?: 0L
        var today = prefs[Keys.BLOCKED_TODAY] ?: 0L
        if (lastDay != epochDay) {
            today = 0L
            prefs[Keys.STATS_DAY] = epochDay
        }
        var weekStart = prefs[Keys.STATS_WEEK_START] ?: 0L
        var week = prefs[Keys.BLOCKED_WEEK] ?: 0L
        if (weekStart == 0L || epochDay - weekStart >= 7L) {
            week = 0L
            weekStart = epochDay
            prefs[Keys.STATS_WEEK_START] = weekStart
        }
        prefs[Keys.BLOCKED_TODAY] = today + delta
        prefs[Keys.BLOCKED_WEEK] = week + delta
    }
    suspend fun setUpdateOnlyWifi(enabled: Boolean) = edit { it[Keys.UPDATE_WIFI_ONLY] = enabled }
    suspend fun setUpdateOnlyCharging(enabled: Boolean) = edit { it[Keys.UPDATE_CHARGING_ONLY] = enabled }
    suspend fun setShareTelemetry(enabled: Boolean) = edit { it[Keys.SHARE_TELEMETRY] = enabled }
    suspend fun setActiveProfile(profile: String) = edit { it[Keys.ACTIVE_PROFILE] = profile }
    suspend fun setMaxAllowedRiskScore(score: Int) = edit { it[Keys.MAX_RISK_SCORE] = score }

    fun exportLogsToCsv(logs: List<com.yashwanthsurabhi.shielddns.data.entity.BlockedQueryEntity>): String {
        val sb = java.lang.StringBuilder()
        sb.append("ID,Domain,ListName,Timestamp,UID,PackageName,LatencyMs,Blocked,RiskScore,DnssecStatus,BlockedReason\n")
        for (log in logs) {
            val escapedDomain = log.domain.replace("\"", "\"\"")
            val escapedReason = (log.blockedReason ?: "").replace("\"", "\"\"")
            val escapedListName = log.listName.replace("\"", "\"\"")
            val escapedPackageName = (log.packageName ?: "").replace("\"", "\"\"")
            val escapedDnssec = log.dnssecStatus.replace("\"", "\"\"")
            sb.append("${log.id},\"$escapedDomain\",\"$escapedListName\",${log.timestamp},${log.uid},\"$escapedPackageName\",${log.latencyMs},${log.isBlocked},${log.riskScore},\"$escapedDnssec\",\"$escapedReason\"\n")
        }
        return sb.toString()
    }

    suspend fun importJson(json: String) {
        val exported = SettingsExporter.fromJson(json)
        edit { prefs ->
            prefs[Keys.PROTECTION] = exported.protectionEnabled
            prefs[Keys.ADS] = exported.adsListEnabled
            prefs[Keys.TRACKERS] = exported.trackersListEnabled
            prefs[Keys.MALWARE] = exported.malwareListEnabled
            prefs[Keys.DOH] = exported.useDoh
            prefs[Keys.UPSTREAM] = exported.upstreamDns
            prefs[Keys.BOOT] = exported.startOnBoot
            exported.darkTheme?.let { prefs[Keys.DARK] = it.toString() } ?: prefs.remove(Keys.DARK)
            prefs[Keys.ALLOWLIST] = exported.allowlist
            prefs[Keys.DENY] = exported.customDeny
            prefs[Keys.CUSTOM_URL] = exported.customBlocklistUrl
            prefs[Keys.SCHEDULE] = exported.scheduleEnabled
            prefs[Keys.SCHED_START] = exported.scheduleStartMinutes
            prefs[Keys.SCHED_END] = exported.scheduleEndMinutes
            prefs[Keys.MOBILE_ONLY] = exported.blockOnlyMobile
            prefs[Keys.WIFI_ONLY] = exported.blockOnlyWifi
        }
    }

    suspend fun exportJson(current: AppSettings): String = SettingsExporter.toJson(current)

    private suspend fun edit(block: suspend (MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}

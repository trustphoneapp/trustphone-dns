package com.yashwanthsurabhi.shielddns.data.store

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ExportedSettings(
    val version: Int = 3,
    val protectionEnabled: Boolean = false,
    val adsListEnabled: Boolean = true,
    val trackersListEnabled: Boolean = true,
    val malwareListEnabled: Boolean = true,
    val useDoh: Boolean = true,
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
)

object SettingsExporter {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun toJson(settings: AppSettings): String = json.encodeToString(
        ExportedSettings(
            protectionEnabled = settings.protectionEnabled,
            adsListEnabled = settings.adsListEnabled,
            trackersListEnabled = settings.trackersListEnabled,
            malwareListEnabled = settings.malwareListEnabled,
            useDoh = settings.useDoh,
            upstreamDns = settings.upstreamDns,
            startOnBoot = settings.startOnBoot,
            darkTheme = settings.darkTheme,
            allowlist = settings.allowlist,
            customDeny = settings.customDeny,
            customBlocklistUrl = settings.customBlocklistUrl,
            scheduleEnabled = settings.scheduleEnabled,
            scheduleStartMinutes = settings.scheduleStartMinutes,
            scheduleEndMinutes = settings.scheduleEndMinutes,
            blockOnlyMobile = settings.blockOnlyMobile,
            blockOnlyWifi = settings.blockOnlyWifi,
        ),
    )

    fun fromJson(text: String): ExportedSettings = json.decodeFromString(text)
}

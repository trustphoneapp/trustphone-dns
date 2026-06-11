package com.yashwanthsurabhi.shielddns.firebase

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.yashwanthsurabhi.shielddns.data.store.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object FirebaseTracker {
    private const val TAG = "FirebaseTracker"
    private var isInitialized = false
    @Volatile private var isAnalyticsConsentGranted = false
    private var appContext: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Analytics and Crashlytics collection are disabled by default in the manifest
     * and stay off until the user opts in via the privacy "Share anonymous telemetry"
     * toggle. Remote Config is the only Firebase service active without consent; it
     * fetches DoH/blocklist config and carries no app-supplied personal data.
     */
    fun init(context: Context, settingsStore: SettingsStore) {
        appContext = context.applicationContext
        runCatching {
            val config = FirebaseRemoteConfig.getInstance()
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // 1 hour
                .build()
            config.setConfigSettingsAsync(settings)

            // Setup defaults
            val defaults = mapOf(
                "default_doh_url" to "https://cloudflare-dns.com/dns-query",
                "backup_doh_urls" to "https://dns.google/dns-query,https://doh.opendns.com/dns-query",
                "blocklist_download_url" to "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
                "min_supported_blocklist_version" to 1L,
                "emergency_disable" to false,
                "premium_features_enabled" to true
            )
            config.setDefaultsAsync(defaults)
            config.fetchAndActivate()

            isInitialized = true
            Log.d(TAG, "Firebase services initialized successfully")
        }.onFailure {
            Log.e(TAG, "Failed to initialize Firebase: ${it.message}")
        }

        // Track the user's telemetry consent reactively and apply it to Firebase
        // collection. Defaults to off; never tied to purchase/Pro status.
        scope.launch {
            settingsStore.settings
                .map { it.shareTelemetry }
                .distinctUntilChanged()
                .collect { granted -> applyConsent(granted) }
        }
    }

    fun logEvent(context: Context, eventName: String, params: Bundle = Bundle()) {
        if (!isInitialized || !isAnalyticsConsentGranted) return
        runCatching {
            FirebaseAnalytics.getInstance(context).logEvent(eventName, params)
            Log.d(TAG, "Logged event: $eventName with params: $params")
        }.onFailure {
            Log.e(TAG, "Failed to log event: ${it.message}")
        }
    }

    /** Called from the privacy toggle; flips both consent state and live collection. */
    fun setAnalyticsConsent(granted: Boolean) = applyConsent(granted)

    private fun applyConsent(granted: Boolean) {
        isAnalyticsConsentGranted = granted
        val ctx = appContext ?: return
        runCatching {
            FirebaseAnalytics.getInstance(ctx).setAnalyticsCollectionEnabled(granted)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(granted)
        }.onFailure {
            Log.e(TAG, "Failed to apply analytics consent: ${it.message}")
        }
    }

    fun logException(throwable: Throwable, message: String? = null) {
        // Respect consent: with telemetry off we keep crash details on-device only.
        if (!isInitialized || !isAnalyticsConsentGranted) {
            Log.e(TAG, "Unhandled Exception [local only]: $message", throwable)
            return
        }
        runCatching {
            message?.let { FirebaseCrashlytics.getInstance().log(it) }
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }.onFailure {
            Log.e(TAG, "Failed to log exception: ${it.message}")
        }
    }

    // Remote Config Getters
    fun getDefaultDohUrl(): String = runCatching {
        FirebaseRemoteConfig.getInstance().getString("default_doh_url")
    }.getOrDefault("https://cloudflare-dns.com/dns-query")

    fun getBackupDohUrls(): List<String> = runCatching {
        val raw = FirebaseRemoteConfig.getInstance().getString("backup_doh_urls")
        raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }.getOrDefault(listOf("https://dns.google/dns-query", "https://doh.opendns.com/dns-query"))

    fun getBlocklistDownloadUrl(): String = runCatching {
        FirebaseRemoteConfig.getInstance().getString("blocklist_download_url")
    }.getOrDefault("https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts")

    fun getEmergencyDisable(): Boolean = runCatching {
        FirebaseRemoteConfig.getInstance().getBoolean("emergency_disable")
    }.getOrDefault(false)

    fun getPremiumFeaturesEnabled(): Boolean = runCatching {
        FirebaseRemoteConfig.getInstance().getBoolean("premium_features_enabled")
    }.getOrDefault(true)
}

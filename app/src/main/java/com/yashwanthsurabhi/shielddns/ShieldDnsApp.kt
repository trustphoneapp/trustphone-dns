package com.yashwanthsurabhi.shielddns

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.yashwanthsurabhi.shielddns.worker.BlocklistUpdateWorker

class ShieldDnsApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        com.yashwanthsurabhi.shielddns.firebase.FirebaseTracker.init(this, container.settingsStore)
        createNotificationChannel()
        BlocklistUpdateWorker.schedule(this)
        container.billingManager.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Shield DNS Protection",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Persistent status while DNS blocking is active"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "shield_dns_protection"
    }
}

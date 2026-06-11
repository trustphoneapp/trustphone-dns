package com.yashwanthsurabhi.shielddns.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yashwanthsurabhi.shielddns.ShieldDnsApp
import com.yashwanthsurabhi.shielddns.rules.ScheduleManager
import com.yashwanthsurabhi.shielddns.vpn.DnsBlockerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val container = (context.applicationContext as ShieldDnsApp).container
        val pending = goAsync()
        scope.launch {
            try {
                val settings = container.settingsStore.settings.first()
                if (!settings.startOnBoot || !settings.protectionEnabled) return@launch
                val networkOk = container.networkWatcher.shouldBlockOnCurrentNetwork(settings)
                val scheduleOk = ScheduleManager.shouldProtectionBeActive(settings)
                if (networkOk && scheduleOk) {
                    DnsBlockerService.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

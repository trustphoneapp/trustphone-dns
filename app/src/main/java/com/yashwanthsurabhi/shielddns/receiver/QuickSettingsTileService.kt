package com.yashwanthsurabhi.shielddns.receiver

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.yashwanthsurabhi.shielddns.MainActivity
import com.yashwanthsurabhi.shielddns.ShieldDnsApp
import com.yashwanthsurabhi.shielddns.vpn.DnsBlockerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QuickSettingsTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            val settings = (application as ShieldDnsApp).container.settingsStore.settings.first()
            updateTile(settings.protectionEnabled)
        }
    }

    override fun onClick() {
        val container = (application as ShieldDnsApp).container
        scope.launch {
            val settings = container.settingsStore.settings.first()
            val enable = !settings.protectionEnabled
            if (enable) {
                val started = DnsBlockerService.prepareOrStart(this@QuickSettingsTileService)
                if (!started) {
                    val intent = Intent(this@QuickSettingsTileService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("request_vpn", true)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val pendingIntent = PendingIntent.getActivity(
                            this@QuickSettingsTileService,
                            0,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                        )
                        startActivityAndCollapse(pendingIntent)
                    } else {
                        startActivityAndCollapseCompat(intent)
                    }
                } else {
                    container.settingsStore.setProtection(true)
                    updateTile(true)
                }
            } else {
                container.settingsStore.setProtection(false)
                DnsBlockerService.stop(this@QuickSettingsTileService)
                updateTile(false)
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun startActivityAndCollapseCompat(intent: Intent) {
        startActivityAndCollapse(intent)
    }

    private fun updateTile(active: Boolean) {
        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "TrustPhone DNS"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (active) "Protection on" else "Protection off"
            }
            updateTile()
        }
    }
}

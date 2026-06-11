package com.yashwanthsurabhi.shielddns.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import android.util.Log
import androidx.core.app.NotificationCompat
import com.yashwanthsurabhi.shielddns.MainActivity
import com.yashwanthsurabhi.shielddns.R
import com.yashwanthsurabhi.shielddns.ShieldDnsApp
import com.yashwanthsurabhi.shielddns.data.store.AppSettings
import com.yashwanthsurabhi.shielddns.dns.DnsServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileOutputStream

class DnsBlockerService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunInterface: ParcelFileDescriptor? = null
    private var tunOutput: FileOutputStream? = null
    private val tunWriteLock = Any()
    private var tunReader: TunReader? = null
    private var packetHandler: DnsPacketHandler? = null
    private var currentSettings: AppSettings = AppSettings()
    private var activeDnsServer: String = DnsServerConfig.DEFAULT_UPSTREAM
    private val insertedCount = java.util.concurrent.atomic.AtomicInteger(0)
    private val pendingBlocked = java.util.concurrent.atomic.AtomicLong(0)

    override fun onCreate() {
        super.onCreate()
        val container = (application as ShieldDnsApp).container
        serviceScope.launch {
            container.settingsStore.settings.collect { settings ->
                currentSettings = settings
            }
        }
        serviceScope.launch {
            container.settingsStore.settings
                .map { it.blockedTodayCount }
                .distinctUntilChanged()
                .collect { blockedToday ->
                    if (tunInterface != null) {
                        val nm = getSystemService(android.app.NotificationManager::class.java)
                        nm.notify(NOTIFICATION_ID, buildNotification(blockedToday))
                    }
                }
        }
        container.upstreamResolver.socketProtector = { socket -> protect(socket) }
        container.upstreamResolver.datagramProtector = { socket -> protect(socket) }
        packetHandler = DnsPacketHandler(
            context = applicationContext,
            blocklistRepository = container.blocklistRepository,
            upstreamResolver = container.upstreamResolver,
            appRuleRepository = container.appRuleRepository,
            networkWatcher = container.networkWatcher,
            scope = serviceScope,
            onBlocked = { entity ->
                container.blockedQueryDao.insert(entity)
                if (insertedCount.incrementAndGet() % 200 == 0) {
                    container.blockedQueryDao.trimToLimit()
                }
                // Count only genuine policy/blocklist blocks — not allowed
                // (forwarded) queries, and not upstream ServFail failures.
                if (entity.isBlocked && entity.listName != "ServFail") {
                    pendingBlocked.incrementAndGet()
                }
            },
            settingsProvider = { currentSettings },
        )
        startStatsFlusher()
    }

    /**
     * Flushes accumulated block counts to persistent storage at most once every
     * few seconds. A typical device emits tens of thousands of DNS queries a day;
     * writing DataStore (and refreshing the notification) per query would dominate
     * battery and I/O. Batching keeps it to a handful of writes per active minute.
     */
    private fun startStatsFlusher() {
        serviceScope.launch {
            val container = (application as ShieldDnsApp).container
            while (isActive) {
                delay(STATS_FLUSH_INTERVAL_MS)
                val delta = pendingBlocked.getAndSet(0)
                if (delta > 0) {
                    val epochDay = System.currentTimeMillis() / 86_400_000L
                    container.settingsStore.recordBlocked(delta, epochDay)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildNotification(currentSettings.blockedTodayCount),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                    )
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification(currentSettings.blockedTodayCount))
                }
                if (tunInterface == null) establishVpn()
            }
        }
        return START_STICKY
    }

    private fun establishVpn() {
        activeDnsServer = DnsServerConfig.normalizedIpv4OrDefault(currentSettings.upstreamDns)
        val builder = Builder()
            .setSession(getString(R.string.vpn_session_name))
            .setMtu(1280)
            .addAddress(VPN_INTERFACE_ADDRESS, 32)
            // Proven DNS-only pattern: system DNS points at a public resolver, only that /32 is routed into TUN.
            .addDnsServer(activeDnsServer)
            .addRoute(activeDnsServer, 32)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
            builder.allowFamily(OsConstants.AF_INET)
        }

        tunInterface = runCatching { builder.establish() }.getOrNull()
        if (tunInterface == null) {
            log("VPN establish() returned null")
            serviceScope.launch {
                (application as ShieldDnsApp).container.settingsStore.setProtection(false)
            }
            return
        }
        log("VPN up — DNS=$activeDnsServer route=${activeDnsServer}/32")
        tunOutput = FileOutputStream(tunInterface!!.fileDescriptor)
        tunReader = TunReader(applicationContext, tunInterface!!, ::onTunPacket).also { it.start() }
    }

    private fun onTunPacket(packet: ByteArray, uid: Int) {
        val ihl = IpPacketSupport.ipv4HeaderLength(packet) ?: return
        if (!IpPacketSupport.isUdp(packet)) return

        val packetCopy = packet.copyOf()
        serviceScope.launch {
            runCatching {
                val result = packetHandler?.handleUdpDns(packetCopy, ihl, uid) ?: return@launch
                writeResponse(packetCopy, ihl, result)
            }.onFailure { t ->
                android.util.Log.e("DnsBlockerService", "Error handling UDP DNS packet: ${t.message}", t)
            }
        }
    }

    private fun writeResponse(original: ByteArray, ipHeaderLength: Int, result: DnsPacketHandler.HandlerResult) {
        val responsePacket = DnsIpPacketBuilder.buildResponseIpPacket(original, ipHeaderLength, result) ?: return
        synchronized(tunWriteLock) {
            runCatching {
                tunOutput?.write(responsePacket)
                tunOutput?.flush()
            }
        }
    }

    private fun stopVpn() {
        tunReader?.stop()
        tunReader = null
        synchronized(tunWriteLock) {
            runCatching { tunOutput?.close() }
            tunOutput = null
        }
        tunInterface?.close()
        tunInterface = null
        runBlocking {
            val container = (application as ShieldDnsApp).container
            val delta = pendingBlocked.getAndSet(0)
            if (delta > 0) {
                container.settingsStore.recordBlocked(delta, System.currentTimeMillis() / 86_400_000L)
            }
            container.settingsStore.setProtection(false)
        }
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(blockedToday: Long): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val disconnect = PendingIntent.getService(
            this,
            1,
            Intent(this, DnsBlockerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, ShieldDnsApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSubText(getString(R.string.notification_subtext, blockedToday))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(getString(R.string.notification_big_text, blockedToday)),
            )
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_shield, getString(R.string.notification_disconnect), disconnect)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }

    companion object {
        const val ACTION_STOP = "com.yashwanthsurabhi.shielddns.STOP_VPN"
        private const val TAG = "DnsBlockerService"
        private const val NOTIFICATION_ID = 42
        private const val VPN_INTERFACE_ADDRESS = "10.0.0.2"
        private const val STATS_FLUSH_INTERVAL_MS = 4000L

        fun start(context: Context) {
            val intent = Intent(context, DnsBlockerService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DnsBlockerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun prepareOrStart(context: Context): Boolean {
            val prepare = VpnService.prepare(context)
            if (prepare != null) return false
            start(context)
            return true
        }
    }
}

package com.yashwanthsurabhi.shielddns.dns

import android.util.Base64
import android.util.Log
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory

class UpstreamResolver {

    companion object {
        const val CLOUDFLARE_DEFAULT = "https://cloudflare-dns.com/dns-query"
        const val CLOUDFLARE_SECURITY = "https://security.cloudflare-dns.com/dns-query"
        const val CLOUDFLARE_FAMILY = "https://family.cloudflare-dns.com/dns-query"
        const val NEXTDNS_DEFAULT = "https://dns.nextdns.io"
        const val CONTROLD_UNFILTERED = "https://dns.controld.com"
        const val QUAD9_SECURE = "https://dns.quad9.net/dns-query"
    }

    var socketProtector: ((Socket) -> Boolean)? = null
    var datagramProtector: ((DatagramSocket) -> Boolean)? = null

    @Volatile var activeDohUrl: String = CLOUDFLARE_DEFAULT
    @Volatile var totalQueriesCount = 0L
    @Volatile var dohFallbackCount = 0L
    @Volatile var averageLatencyMs = 0L
    @Volatile var consecutiveFailures = 0
    @Volatile var lastSuccessTime = 0L
    val resolverSwitchHistory = mutableListOf<String>()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .dns(CloudflareBootstrapDns)
            // Read the protector dynamically: the VPN service that owns protect()
            // can be torn down and recreated, so capturing it once would route DoH
            // sockets into the dead TUN after a restart.
            .socketFactory(ProtectedSocketFactory { socketProtector })
            .build()
    }

    fun resolve(
        query: ByteArray,
        useDoh: Boolean,
        upstreamHost: String,
    ): ByteArray? {
        if (useDoh) {
            // Apply Remote Config url on fallback check
            val configuredDoh = com.yashwanthsurabhi.shielddns.firebase.FirebaseTracker.getDefaultDohUrl()
            if (activeDohUrl == CLOUDFLARE_DEFAULT && configuredDoh != activeDohUrl) {
                activeDohUrl = configuredDoh
            }
            resolveDohPost(query)?.let { return it }
            resolveDohGet(query)?.let { return it }
            // The user explicitly chose encrypted DNS — never silently downgrade to
            // plaintext UDP. Fail closed; repeated failures rotate to a backup DoH
            // resolver via recordFailure(), and the client retries the query.
            return null
        }
        return resolveUdp(query, upstreamHost)
    }

    private fun resolveDohPost(query: ByteArray): ByteArray? {
        val startTime = System.currentTimeMillis()
        return runCatching {
            val body = query.toRequestBody("application/dns-message".toMediaType())
            val request = Request.Builder()
                .url(activeDohUrl)
                .header("Accept", "application/dns-message")
                .post(body)
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                val bytes = response.body?.bytes() ?: throw Exception("Empty body")
                recordSuccess(startTime)
                bytes
            }
        }.getOrElse {
            recordFailure()
            null
        }
    }

    private fun resolveDohGet(query: ByteArray): ByteArray? {
        val startTime = System.currentTimeMillis()
        return runCatching {
            val encoded = Base64.encodeToString(query, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val request = Request.Builder()
                .url("$activeDohUrl?dns=$encoded")
                .header("Accept", "application/dns-message")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                val bytes = response.body?.bytes() ?: throw Exception("Empty body")
                recordSuccess(startTime)
                bytes
            }
        }.getOrElse {
            recordFailure()
            null
        }
    }

    private fun recordSuccess(startTime: Long) {
        val latency = System.currentTimeMillis() - startTime
        lastSuccessTime = System.currentTimeMillis()
        consecutiveFailures = 0
        totalQueriesCount++
        averageLatencyMs = if (averageLatencyMs == 0L) {
            latency
        } else {
            (averageLatencyMs * 9 + latency) / 10
        }
    }

    private fun recordFailure() {
        consecutiveFailures++
        if (consecutiveFailures >= 3) {
            dohFallbackCount++
            triggerResolverFailover()
        }
    }

    private fun triggerResolverFailover() {
        val backups = com.yashwanthsurabhi.shielddns.firebase.FirebaseTracker.getBackupDohUrls()
        if (backups.isEmpty()) return
        val currentIndex = backups.indexOf(activeDohUrl)
        val nextIndex = (currentIndex + 1) % backups.size
        val nextResolver = backups[nextIndex]
        activeDohUrl = nextResolver
        consecutiveFailures = 0

        val formattedTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        synchronized(resolverSwitchHistory) {
            resolverSwitchHistory.add("$formattedTime: Switched to $nextResolver")
            if (resolverSwitchHistory.size > 10) {
                resolverSwitchHistory.removeAt(0)
            }
        }
        Log.w("UpstreamResolver", "Switched DNS resolver to $nextResolver due to consecutive failures")
    }

    private fun resolveUdp(query: ByteArray, host: String): ByteArray? = runCatching {
        DatagramSocket().use { socket ->
            datagramProtector?.invoke(socket)
            socket.soTimeout = 5000
            val address = parseHostAddress(host)
            val send = java.net.DatagramPacket(query, query.size, address, 53)
            socket.send(send)
            val buffer = ByteArray(4096)
            val receive = java.net.DatagramPacket(buffer, buffer.size)
            socket.receive(receive)
            buffer.copyOf(receive.length)
        }
    }.getOrNull()

    private fun parseHostAddress(host: String): InetAddress {
        val parts = host.trim().split('.')
        if (parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }) {
            return InetAddress.getByAddress(parts.map { it.toInt().toByte() }.toByteArray())
        }
        return InetAddress.getByName(host.trim())
    }

    private object CloudflareBootstrapDns : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            if (hostname.equals("cloudflare-dns.com", ignoreCase = true)) {
                return listOf(
                    InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)),
                    InetAddress.getByAddress(byteArrayOf(1, 0, 0, 1)),
                )
            }
            return Dns.SYSTEM.lookup(hostname)
        }
    }

    private class ProtectedSocketFactory(
        private val protectorProvider: () -> ((Socket) -> Boolean)?,
    ) : SocketFactory() {
        override fun createSocket(): Socket {
            val socket = Socket()
            protectorProvider()?.invoke(socket)
            return socket
        }

        override fun createSocket(host: String, port: Int): Socket {
            val socket = createSocket()
            socket.connect(InetSocketAddress(host, port))
            return socket
        }

        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
            val socket = createSocket()
            socket.bind(InetSocketAddress(localHost, localPort))
            socket.connect(IntentSocketAddress(host, port))
            return socket
        }

        private fun IntentSocketAddress(host: String, port: Int) = InetSocketAddress(host, port)

        override fun createSocket(host: InetAddress, port: Int): Socket {
            val socket = createSocket()
            socket.connect(InetSocketAddress(host, port))
            return socket
        }

        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
            val socket = createSocket()
            socket.bind(InetSocketAddress(localAddress, localPort))
            socket.connect(InetSocketAddress(address, port))
            return socket
        }
    }
}

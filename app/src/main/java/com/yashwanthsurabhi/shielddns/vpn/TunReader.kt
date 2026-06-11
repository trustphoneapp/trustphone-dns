package com.yashwanthsurabhi.shielddns.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import java.io.FileInputStream
import java.net.InetAddress
import java.net.InetSocketAddress

class TunReader(
    context: Context,
    private val tunFd: ParcelFileDescriptor,
    private val onPacket: (ByteArray, Int) -> Unit,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    @Volatile
    private var running = false

    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread({ readLoop() }, "TunReader").also { it.start() }
    }

    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
    }

    private fun readLoop() {
        val input = FileInputStream(tunFd.fileDescriptor)
        val buffer = ByteArray(32767)
        while (running) {
            try {
                val length = input.read(buffer)
                if (length < 0) {
                    running = false
                    break
                }
                if (length == 0) continue
                val packet = buffer.copyOf(length)
                val uid = resolveUid(packet)
                onPacket(packet, uid)
            } catch (_: InterruptedException) {
                break
            } catch (_: Exception) {
                if (!running || Thread.currentThread().isInterrupted) {
                    break
                }
                try {
                    Thread.sleep(10)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
    }

    private fun resolveUid(packet: ByteArray): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return -1
        if (packet.size < 24) return -1
        val version = (packet[0].toInt() shr 4) and 0xF
        if (version != 4) return -1
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != OsConstants.IPPROTO_UDP) return -1
        return runCatching {
            val src = InetSocketAddress(
                InetAddress.getByAddress(packet.copyOfRange(12, 16)),
                readPort(packet, 20),
            )
            val dst = InetSocketAddress(
                InetAddress.getByAddress(packet.copyOfRange(16, 20)),
                readPort(packet, 22),
            )
            connectivityManager.getConnectionOwnerUid(protocol, src, dst)
        }.getOrDefault(-1)
    }

    private fun readPort(packet: ByteArray, offset: Int): Int =
        ((packet[offset].toInt() and 0xFF) shl 8) or (packet[offset + 1].toInt() and 0xFF)
}

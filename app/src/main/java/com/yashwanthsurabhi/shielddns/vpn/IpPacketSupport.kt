package com.yashwanthsurabhi.shielddns.vpn

object IpPacketSupport {

    fun ipv4HeaderLength(packet: ByteArray): Int? {
        if (packet.size < 20) return null
        val version = (packet[0].toInt() shr 4) and 0xF
        if (version != 4) return null
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (ihl < 20 || packet.size < ihl) return null
        return ihl
    }

    fun ipv4Address(packet: ByteArray, offset: Int): String? {
        if (packet.size < offset + 4) return null
        return "${packet[offset].toInt() and 0xFF}.${packet[offset + 1].toInt() and 0xFF}." +
            "${packet[offset + 2].toInt() and 0xFF}.${packet[offset + 3].toInt() and 0xFF}"
    }

    fun isUdp(packet: ByteArray): Boolean =
        packet.size >= 10 && packet[9].toInt() and 0xFF == 17
}

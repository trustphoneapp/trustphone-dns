package com.yashwanthsurabhi.shielddns.vpn

object DnsIpPacketBuilder {
    fun buildResponseIpPacket(
        request: ByteArray,
        ipHeaderLength: Int,
        result: DnsPacketHandler.HandlerResult,
    ): ByteArray? {
        if (request.size < ipHeaderLength + UDP_HEADER_LENGTH) return null
        val totalLen = ipHeaderLength + UDP_HEADER_LENGTH + result.dnsPayload.size
        val packet = ByteArray(totalLen)

        System.arraycopy(request, 0, packet, 0, ipHeaderLength)
        System.arraycopy(request, 12, packet, 16, 4)
        System.arraycopy(request, 16, packet, 12, 4)
        packet[8] = 64
        packet[10] = 0
        packet[11] = 0
        writeU16(packet, 2, totalLen)

        val udpOffset = ipHeaderLength
        writeU16(packet, udpOffset, result.sourcePort)
        writeU16(packet, udpOffset + 2, result.destPort)
        writeU16(packet, udpOffset + 4, UDP_HEADER_LENGTH + result.dnsPayload.size)
        writeU16(packet, udpOffset + 6, 0)
        System.arraycopy(result.dnsPayload, 0, packet, udpOffset + UDP_HEADER_LENGTH, result.dnsPayload.size)

        writeU16(packet, 10, checksum(packet, 0, ipHeaderLength))
        return packet
    }

    fun checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + length
        while (i < end - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (length % 2 == 1) {
            sum += (data[end - 1].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv().toInt() and 0xFFFF
    }

    private fun writeU16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = ((value shr 8) and 0xFF).toByte()
        data[offset + 1] = (value and 0xFF).toByte()
    }

    private const val UDP_HEADER_LENGTH = 8
}

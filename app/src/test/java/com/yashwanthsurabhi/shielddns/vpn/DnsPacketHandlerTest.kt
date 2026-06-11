package com.yashwanthsurabhi.shielddns.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DnsPacketHandlerTest {

    @Test
    fun buildBlockedResponseReturnsSinkhole() {
        val query = byteArrayOf(
            0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x03, 0x77, 0x77, 0x77,
            0x07, 0x65, 0x78, 0x61, 0x6D, 0x70, 0x6C, 0x65,
            0x03, 0x63, 0x6F, 0x6D,
            0x00,
            0x00, 0x01,
            0x00, 0x01,
        )
        val response = DnsResponseBuilder.buildBlockedResponse(query)
        assertNotNull(response)
        val domain = DnsResponseBuilder.extractQueryDomain(query)
        assertEquals("www.example.com", domain)
    }

    @Test
    fun responsePacketSwapsAddressesAndPorts() {
        val request = byteArrayOf(
            0x45, 0x00, 0x00, 0x20,
            0x12, 0x34, 0x00, 0x00,
            0x40, 0x11, 0x00, 0x00,
            10, 0, 0, 2,
            1, 1, 1, 1,
            0x30, 0x39,
            0x00, 0x35,
            0x00, 0x0c,
            0x00, 0x00,
            0x12, 0x34, 0x81.toByte(), 0x80.toByte(),
        )
        val result = DnsPacketHandler.HandlerResult(
            dnsPayload = byteArrayOf(0x12, 0x34, 0x81.toByte(), 0x80.toByte()),
            sourcePort = 53,
            destPort = 12345,
            clientIp = "10.0.0.2",
            uid = -1,
        )

        val response = DnsIpPacketBuilder.buildResponseIpPacket(request, 20, result)

        assertNotNull(response)
        response!!
        assertEquals("1.1.1.1", ipv4(response, 12))
        assertEquals("10.0.0.2", ipv4(response, 16))
        assertEquals(53, readU16(response, 20))
        assertEquals(12345, readU16(response, 22))
        assertEquals(12, readU16(response, 24))
        assertEquals(0, readU16(response, 26))
        assertEquals(0, DnsIpPacketBuilder.checksum(response, 0, 20))
    }

    private fun readU16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun ipv4(data: ByteArray, offset: Int): String =
        "${data[offset].toInt() and 0xFF}.${data[offset + 1].toInt() and 0xFF}." +
            "${data[offset + 2].toInt() and 0xFF}.${data[offset + 3].toInt() and 0xFF}"
}

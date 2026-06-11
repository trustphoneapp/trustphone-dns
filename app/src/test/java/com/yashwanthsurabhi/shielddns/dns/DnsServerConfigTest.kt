package com.yashwanthsurabhi.shielddns.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsServerConfigTest {

    @Test
    fun acceptsValidIpv4Resolvers() {
        assertTrue(DnsServerConfig.isIpv4("1.1.1.1"))
        assertTrue(DnsServerConfig.isIpv4("8.8.4.4"))
    }

    @Test
    fun rejectsHostnamesAndOutOfRangeIpv4() {
        assertFalse(DnsServerConfig.isIpv4("cloudflare-dns.com"))
        assertFalse(DnsServerConfig.isIpv4("999.1.1.1"))
        assertFalse(DnsServerConfig.isIpv4("1.1.1"))
    }

    @Test
    fun fallsBackToDefaultForUnsafeBuilderInput() {
        assertEquals("1.1.1.1", DnsServerConfig.normalizedIpv4OrDefault("cloudflare-dns.com"))
        assertEquals("9.9.9.9", DnsServerConfig.normalizedIpv4OrDefault(" 9.9.9.9 "))
    }
}

package com.yashwanthsurabhi.shielddns.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DnsCacheTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    // www.example.com  A  IN
    private val question = intArrayOf(
        0x03, 0x77, 0x77, 0x77,
        0x07, 0x65, 0x78, 0x61, 0x6D, 0x70, 0x6C, 0x65,
        0x03, 0x63, 0x6F, 0x6D,
        0x00,
        0x00, 0x01,
        0x00, 0x01,
    )

    private fun query(id0: Int, id1: Int): ByteArray {
        val header = intArrayOf(id0, id1, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        return bytes(*(header + question))
    }

    // Response for www.example.com -> 1.2.3.4, TTL 300
    private fun response(): ByteArray {
        val header = intArrayOf(0x12, 0x34, 0x81, 0x80, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00)
        val answer = intArrayOf(
            0xC0, 0x0C, 0x00, 0x01, 0x00, 0x01,
            0x00, 0x00, 0x01, 0x2C, // TTL = 300
            0x00, 0x04, 0x01, 0x02, 0x03, 0x04,
        )
        return bytes(*(header + question + answer))
    }

    @Test
    fun parsesMinAnswerTtl() {
        assertEquals(300L, DnsResponseBuilder.minAnswerTtlSeconds(response()))
    }

    @Test
    fun cacheHitStampsRequesterTransactionId() {
        val cache = DnsCache()
        cache.put(query(0x12, 0x34), response(), nowMs = 0)

        val hit = cache.get(query(0xAB, 0xCD), nowMs = 299_000)
        assertNotNull(hit)
        hit!!
        // The cached payload is returned with the *new* query's transaction id.
        assertEquals(0xAB, hit[0].toInt() and 0xFF)
        assertEquals(0xCD, hit[1].toInt() and 0xFF)
        // ...and the rest of the DNS message is unchanged (flags byte still 0x81).
        assertEquals(0x81, hit[2].toInt() and 0xFF)
    }

    @Test
    fun cacheEntryExpiresAfterTtl() {
        val cache = DnsCache()
        cache.put(query(0x12, 0x34), response(), nowMs = 0)
        assertNull(cache.get(query(0x12, 0x34), nowMs = 300_000))
    }

    @Test
    fun errorResponsesAreNotCached() {
        val cache = DnsCache()
        val servfail = response().copyOf()
        servfail[3] = ((servfail[3].toInt() and 0xF0) or 0x02).toByte() // RCODE = SERVFAIL
        cache.put(query(0x12, 0x34), servfail, nowMs = 0)
        assertNull(cache.get(query(0x12, 0x34), nowMs = 1))
    }
}

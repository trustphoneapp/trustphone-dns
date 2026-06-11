package com.yashwanthsurabhi.shielddns.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlocklistParserTest {

    @Test
    fun parsesHostsAndComments() {
        val input = """
            # comment
            ||ads.example.com^
            0.0.0.0 tracker.test
            example.org
        """.trimIndent()
        val domains = BlocklistParser.parse(input)
        assertTrue(domains.contains("ads.example.com"))
        assertTrue(domains.contains("tracker.test"))
        assertTrue(domains.contains("example.org"))
        assertEquals(3, domains.size)
    }
}

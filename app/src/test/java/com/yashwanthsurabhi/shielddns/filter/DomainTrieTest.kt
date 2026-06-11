package com.yashwanthsurabhi.shielddns.filter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTrieTest {

    @Test
    fun matchesExactDomain() {
        val trie = DomainTrie()
        trie.insert("ads.example.com")
        assertTrue(trie.matches("ads.example.com"))
        assertFalse(trie.matches("example.com"))
    }

    @Test
    fun matchesSubdomain() {
        val trie = DomainTrie()
        trie.insert("example.com")
        assertTrue(trie.matches("tracker.example.com"))
        assertTrue(trie.matches("deep.tracker.example.com"))
    }

    @Test
    fun clearRemovesEntries() {
        val trie = DomainTrie()
        trie.insert("a.com")
        trie.insert("b.com")
        trie.clear()
        assertFalse(trie.matches("a.com"))
        assertFalse(trie.matches("b.com"))
    }
}

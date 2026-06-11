package com.yashwanthsurabhi.shielddns.filter

/**
 * Reverse-domain trie for fast suffix matching (subdomain blocking).
 * Stored labels are reversed: "ads.example.com" -> ["com","example","ads"].
 */
class DomainTrie {
    private val root = TrieNode()
    private var entryCount = 0

    val size: Int
        get() = entryCount

    fun insert(domain: String) {
        val labels = normalize(domain) ?: return
        var node = root
        for (label in labels) {
            node = node.children.getOrPut(label) { TrieNode() }
        }
        if (!node.isTerminal) {
            node.isTerminal = true
            entryCount++
        }
    }

    fun insertAll(domains: Collection<String>) {
        domains.forEach { insert(it) }
    }

    fun clear() {
        root.children.clear()
        entryCount = 0
    }

    /** True if [query] or any parent domain is blocked. */
    fun matches(query: String): Boolean {
        val labels = normalize(query) ?: return false
        var node = root
        for (label in labels) {
            node = node.children[label] ?: return false
            if (node.isTerminal) return true
        }
        return false
    }

    fun contains(domain: String): Boolean = matches(domain)

    private fun normalize(domain: String): List<String>? {
        val trimmed = domain.trim().lowercase().removeSuffix(".")
        if (trimmed.isEmpty() || trimmed.contains(' ')) return null
        val labels = trimmed.split('.').filter { it.isNotEmpty() }
        if (labels.isEmpty()) return null
        return labels.reversed()
    }

    private class TrieNode(
        val children: MutableMap<String, TrieNode> = mutableMapOf(),
        var isTerminal: Boolean = false,
    )
}

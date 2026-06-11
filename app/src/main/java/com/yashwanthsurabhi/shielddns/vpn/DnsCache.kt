package com.yashwanthsurabhi.shielddns.vpn

/**
 * Small in-memory, TTL-respecting DNS response cache.
 *
 * Keyed by (qtype, qname). On a hit within the response's minimum answer TTL the
 * cached bytes are returned with the requester's transaction id stamped in, so the
 * client accepts the reply. This removes a full upstream round-trip (the dominant
 * latency and battery cost) for repeat lookups, which are the bulk of real traffic.
 *
 * Bounded LRU; entries past TTL are treated as misses and evicted on access.
 * The cache lives for one VPN session (recreated with the packet handler), so it is
 * naturally flushed when protection is toggled.
 */
class DnsCache(private val maxEntries: Int = 1024) {

    private class Entry(val response: ByteArray, val expiresAtMs: Long)

    // accessOrder = true makes this a true LRU; eldest evicted past capacity.
    private val lru = object : LinkedHashMap<String, Entry>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>): Boolean =
            size > maxEntries
    }

    @Synchronized
    fun get(query: ByteArray, nowMs: Long = System.currentTimeMillis()): ByteArray? {
        val key = keyOf(query) ?: return null
        val entry = lru[key] ?: return null
        if (nowMs >= entry.expiresAtMs) {
            lru.remove(key)
            return null
        }
        val out = entry.response.copyOf()
        // Stamp the caller's transaction id (first two bytes) so the client matches it.
        if (out.size >= 2 && query.size >= 2) {
            out[0] = query[0]
            out[1] = query[1]
        }
        return out
    }

    @Synchronized
    fun put(query: ByteArray, response: ByteArray, nowMs: Long = System.currentTimeMillis()) {
        val key = keyOf(query) ?: return
        val ttl = DnsResponseBuilder.minAnswerTtlSeconds(response)
        if (ttl <= 0) return // uncacheable (error, no answers, or zero TTL)
        val cappedTtl = ttl.coerceAtMost(MAX_TTL_SECONDS)
        lru[key] = Entry(response.copyOf(), nowMs + cappedTtl * 1000L)
    }

    @Synchronized
    fun clear() = lru.clear()

    @get:Synchronized
    val size: Int get() = lru.size

    private fun keyOf(query: ByteArray): String? {
        val name = DnsResponseBuilder.extractQueryDomain(query)?.lowercase() ?: return null
        if (name.isEmpty()) return null
        val type = DnsResponseBuilder.extractQueryType(query)
        return "$type:$name"
    }

    private companion object {
        const val MAX_TTL_SECONDS = 3600L // cap absurd upstream TTLs at 1h
    }
}

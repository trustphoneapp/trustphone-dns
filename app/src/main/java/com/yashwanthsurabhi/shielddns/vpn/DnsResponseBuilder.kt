package com.yashwanthsurabhi.shielddns.vpn

object DnsResponseBuilder {

    fun buildBlockedResponse(query: ByteArray): ByteArray? {
        if (query.size < 12) return null
        val response = query.copyOf()
        // QR=1 response, AA=1
        response[2] = ((response[2].toInt() and 0x78) or 0x84).toByte()
        response[3] = response[3] // RA unchanged

        val qdCount = readU16(response, 4)
        if (qdCount != 1) return null

        var offset = 12
        offset = skipName(response, offset) ?: return null
        if (offset + 4 > response.size) return null
        val qType = readU16(response, offset)
        val qClass = readU16(response, offset + 2)
        offset += 4

        if (qType == 28) {
            // AAAA: return empty NOERROR so browsers can fall back to IPv4.
            val trimmed = response.copyOf(offset)
            trimmed[2] = ((trimmed[2].toInt() and 0x78) or 0x80).toByte()
            writeU16(trimmed, 6, 0)
            writeU16(trimmed, 8, 0)
            writeU16(trimmed, 10, 0)
            return trimmed
        }
        if (qType != 1) return buildServFailResponse(query)

        val answer = ByteArray(offset + 16)
        System.arraycopy(response, 0, answer, 0, offset)
        writeU16(answer, 6, 1) // one answer
        writeU16(answer, 8, 0)
        writeU16(answer, 10, 0)

        // NAME pointer to question
        answer[offset] = 0xC0.toByte()
        answer[offset + 1] = 0x0C
        writeU16(answer, offset + 2, 1) // A
        writeU16(answer, offset + 4, qClass)
        writeU32(answer, offset + 6, 60) // TTL
        writeU16(answer, offset + 10, 4) // RDLENGTH
        answer[offset + 12] = 0
        answer[offset + 13] = 0
        answer[offset + 14] = 0
        answer[offset + 15] = 0
        return answer
    }

    fun buildServFailResponse(query: ByteArray): ByteArray? {
        if (query.size < 12) return null
        val response = query.copyOf()
        response[2] = ((response[2].toInt() and 0x78) or 0x80).toByte()
        response[3] = ((response[3].toInt() and 0xF0) or 0x02).toByte()
        writeU16(response, 6, 0)
        writeU16(response, 8, 0)
        writeU16(response, 10, 0)
        return response
    }

    fun extractQueryDomain(query: ByteArray): String? {
        if (query.size < 12) return null
        return readName(query, 12)
    }

    fun extractTransactionId(packet: ByteArray): Int {
        if (packet.size < 2) return 0
        return readU16(packet, 0)
    }

    /** QTYPE of the (single) question, or 0 if it can't be parsed. */
    fun extractQueryType(query: ByteArray): Int {
        if (query.size < 12) return 0
        val afterName = skipName(query, 12) ?: return 0
        if (afterName + 2 > query.size) return 0
        return readU16(query, afterName)
    }

    /**
     * Smallest TTL (seconds) across the answer records of a successful response,
     * used to decide how long the response may be cached. Returns 0 when the
     * response is an error, has no answers, or can't be parsed — i.e. "do not cache".
     */
    fun minAnswerTtlSeconds(response: ByteArray): Long {
        if (response.size < 12) return 0
        val rcode = response[3].toInt() and 0x0F
        if (rcode != 0) return 0
        val qdCount = readU16(response, 4)
        val anCount = readU16(response, 6)
        if (anCount <= 0) return 0

        var offset = 12
        for (i in 0 until qdCount) {
            offset = skipName(response, offset) ?: return 0
            if (offset + 4 > response.size) return 0
            offset += 4 // QTYPE + QCLASS
        }

        var minTtl = Long.MAX_VALUE
        for (i in 0 until anCount) {
            offset = skipName(response, offset) ?: break
            if (offset + 10 > response.size) break
            val ttl = readU32(response, offset + 4)
            val rdLength = readU16(response, offset + 8)
            if (ttl < minTtl) minTtl = ttl
            offset += 10 + rdLength
        }
        return if (minTtl == Long.MAX_VALUE) 0 else minTtl
    }

    private fun readName(data: ByteArray, start: Int): String? {
        val labels = mutableListOf<String>()
        var offset = start
        var jumps = 0
        while (offset < data.size && jumps < 10) {
            val len = data[offset].toInt() and 0xFF
            if (len == 0) break
            if ((len and 0xC0) == 0xC0) {
                if (offset + 1 >= data.size) return null
                offset = ((len and 0x3F) shl 8) or (data[offset + 1].toInt() and 0xFF)
                jumps++
                continue
            }
            offset++
            if (offset + len > data.size) return null
            labels += String(data, offset, len, Charsets.UTF_8)
            offset += len
        }
        return labels.joinToString(".")
    }

    private fun skipName(data: ByteArray, start: Int): Int? {
        var offset = start
        var jumps = 0
        while (offset < data.size && jumps < 10) {
            val len = data[offset].toInt() and 0xFF
            if (len == 0) return offset + 1
            if ((len and 0xC0) == 0xC0) return offset + 2
            offset += 1 + len
        }
        return null
    }

    fun parseDnssecStatus(response: ByteArray): String {
        if (response.size < 12) return "NONE"
        
        // Check AD (Authentic Data) bit: bit 10 of flags (bit 5 of byte 3, 0x20)
        val hasAdBit = (response[3].toInt() and 0x20) != 0
        
        val qdCount = readU16(response, 4)
        val anCount = readU16(response, 6)
        val nsCount = readU16(response, 8)
        val arCount = readU16(response, 10)
        
        var offset = 12
        
        // Skip Questions
        for (i in 0 until qdCount) {
            offset = skipName(response, offset) ?: return if (hasAdBit) "SECURE" else "INSECURE"
            if (offset + 4 > response.size) return if (hasAdBit) "SECURE" else "INSECURE"
            offset += 4 // TYPE and CLASS
        }
        
        var hasDnssecRecord = false
        val totalRrs = anCount + nsCount + arCount
        
        for (i in 0 until totalRrs) {
            offset = skipName(response, offset) ?: break
            if (offset + 10 > response.size) break
            val rrType = readU16(response, offset)
            val rrClass = readU16(response, offset + 2)
            val ttl = readU32(response, offset + 4)
            val rdLength = readU16(response, offset + 8)
            offset += 10
            
            if (rrType == 46 || rrType == 48) { // RRSIG = 46, DNSKEY = 48
                hasDnssecRecord = true
            }
            offset += rdLength
        }
        
        return when {
            hasAdBit -> "SECURE"
            hasDnssecRecord -> "VALIDATED"
            else -> "INSECURE"
        }
    }

    private fun readU32(data: ByteArray, offset: Int): Long =
        ((data[offset].toLong() and 0xFF) shl 24) or
        ((data[offset + 1].toLong() and 0xFF) shl 16) or
        ((data[offset + 2].toLong() and 0xFF) shl 8) or
        (data[offset + 3].toLong() and 0xFF)

    private fun readU16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun writeU16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = ((value shr 8) and 0xFF).toByte()
        data[offset + 1] = (value and 0xFF).toByte()
    }

    private fun writeU32(data: ByteArray, offset: Int, value: Int) {
        data[offset] = ((value shr 24) and 0xFF).toByte()
        data[offset + 1] = ((value shr 16) and 0xFF).toByte()
        data[offset + 2] = ((value shr 8) and 0xFF).toByte()
        data[offset + 3] = (value and 0xFF).toByte()
    }
}

package com.yashwanthsurabhi.shielddns.filter

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest

class BloomFilter(
    val version: Int,
    val domainCount: Int,
    val k: Int,
    val m: Int,
    val bits: ByteArray
) {
    companion object {
        private const val TAG = "BloomFilter"
        private const val MAGIC = 0x53484246 // 'SHBF'
        private const val DEFAULT_FPR = 0.01 // 1% false positive rate

        // MurmurHash3 32-bit implementation
        fun murmurHash3(data: ByteArray, seed: Int): Int {
            var h1 = seed
            val len = data.size
            val roundedEnd = (len and 0xfffffffc.toInt())
            for (i in 0 until roundedEnd step 4) {
                var k1 = (data[i].toInt() and 0xff) or
                        ((data[i + 1].toInt() and 0xff) shl 8) or
                        ((data[i + 2].toInt() and 0xff) shl 16) or
                        ((data[i + 3].toInt() and 0xff) shl 24)
                k1 *= 0xcc9e2d51.toInt()
                k1 = (k1 shl 15) or (k1 ushr 17)
                k1 *= 0x1b873593.toInt()
                h1 = h1 xor k1
                h1 = (h1 shl 13) or (h1 ushr 19)
                h1 = h1 * 5 + 0xe6546b64.toInt()
            }
            var k1 = 0
            val tailLen = len - roundedEnd
            if (tailLen >= 3) {
                k1 = k1 xor ((data[roundedEnd + 2].toInt() and 0xff) shl 16)
            }
            if (tailLen >= 2) {
                k1 = k1 xor ((data[roundedEnd + 1].toInt() and 0xff) shl 8)
            }
            if (tailLen >= 1) {
                k1 = k1 xor (data[roundedEnd].toInt() and 0xff)
                k1 *= 0xcc9e2d51.toInt()
                k1 = (k1 shl 15) or (k1 ushr 17)
                k1 *= 0x1b873593.toInt()
                h1 = h1 xor k1
            }
            h1 = h1 xor len
            h1 = h1 xor (h1 ushr 16)
            h1 *= 0x85ebca6b.toInt()
            h1 = h1 xor (h1 ushr 13)
            h1 *= 0xc2b2ae35.toInt()
            h1 = h1 xor (h1 ushr 16)
            return h1
        }
    }

    private val bitCountLong = m.toLong()

    fun mightContain(domain: String): Boolean {
        val bytes = domain.trim().lowercase().toByteArray(Charsets.UTF_8)
        for (i in 0 until k) {
            val hash = murmurHash3(bytes, i)
            val bitIdx = (hash.toLong() and 0xffffffffL) % bitCountLong
            if (!getBit(bitIdx)) {
                return false
            }
        }
        return true
    }

    private fun getBit(index: Long): Boolean {
        val byteIndex = (index / 8).toInt()
        val bitIndex = (index % 8).toInt()
        if (byteIndex < 0 || byteIndex >= bits.size) return false
        return (bits[byteIndex].toInt() and (1 shl bitIndex)) != 0
    }
}

object BloomFilterBuilder {
    private const val DEFAULT_FPR = 0.01 // 1%

    fun build(domains: Collection<String>, version: Int): BloomFilter {
        val n = domains.size.coerceAtLeast(1)
        // m = - (n * ln(p)) / (ln(2)^2)
        val m = (- (n * Math.log(DEFAULT_FPR)) / (Math.log(2.0) * Math.log(2.0))).toInt().coerceAtLeast(8)
        // k = (m/n) * ln(2)
        val k = ((m.toDouble() / n) * Math.log(2.0)).toInt().coerceAtLeast(1)

        val byteSize = (m + 7) / 8
        val bits = ByteArray(byteSize)

        val bitCountLong = m.toLong()
        for (domain in domains) {
            val bytes = domain.trim().lowercase().toByteArray(Charsets.UTF_8)
            for (i in 0 until k) {
                val hash = BloomFilter.murmurHash3(bytes, i)
                val bitIdx = (hash.toLong() and 0xffffffffL) % bitCountLong
                setBit(bits, bitIdx)
            }
        }

        return BloomFilter(version, n, k, m, bits)
    }

    private fun setBit(bits: ByteArray, index: Long) {
        val byteIndex = (index / 8).toInt()
        val bitIndex = (index % 8).toInt()
        if (byteIndex >= 0 && byteIndex < bits.size) {
            bits[byteIndex] = (bits[byteIndex].toInt() or (1 shl bitIndex)).toByte()
        }
    }
}

object BloomFilterStore {
    private const val TAG = "BloomFilterStore"
    private const val MAGIC = 0x53484246 // 'SHBF'
    private const val FILE_NAME = "bloom_filter.bin"
    private const val BACKUP_FILE_NAME = "bloom_filter_backup.bin"

    fun save(context: Context, filter: BloomFilter): Boolean {
        return runCatching {
            val file = File(context.filesDir, FILE_NAME)
            val backup = File(context.filesDir, BACKUP_FILE_NAME)
            if (file.exists()) {
                file.copyTo(backup, overwrite = true)
            }

            FileOutputStream(file).use { out ->
                val header = ByteBuffer.allocate(24)
                header.putInt(MAGIC)
                header.putInt(filter.version)
                header.putInt(filter.domainCount)
                header.putInt(filter.k)
                header.putInt(filter.m)

                // Compute hash checksum of bitarray for corruption check
                val checksum = computeChecksum(filter.bits)
                header.putInt(checksum)

                out.write(header.array())
                out.write(filter.bits)
            }
            Log.d(TAG, "Bloom filter saved successfully: ${filter.domainCount} domains, size = ${filter.bits.size} bytes")
            true
        }.getOrElse {
            Log.e(TAG, "Failed to save Bloom Filter: ${it.message}")
            false
        }
    }

    fun load(context: Context): BloomFilter? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null

        val filter = runCatching {
            loadFromFile(file)
        }.getOrNull()

        if (filter != null) {
            return filter
        }

        // Fallback to backup if corrupted
        Log.w(TAG, "Bloom filter file corrupted or invalid. Attempting to load backup...")
        val backup = File(context.filesDir, BACKUP_FILE_NAME)
        if (backup.exists()) {
            return runCatching {
                loadFromFile(backup).also {
                    // Restore backup as main
                    backup.copyTo(file, overwrite = true)
                }
            }.getOrNull()
        }
        return null
    }

    private fun loadFromFile(file: File): BloomFilter {
        FileInputStream(file).use { input ->
            val headerBytes = ByteArray(24)
            if (input.read(headerBytes) != 24) throw IllegalStateException("Invalid header size")
            val buffer = ByteBuffer.wrap(headerBytes)
            val magic = buffer.int
            if (magic != MAGIC) throw IllegalStateException("Invalid magic number")
            val version = buffer.int
            val domainCount = buffer.int
            val k = buffer.int
            val m = buffer.int
            val expectedChecksum = buffer.int

            val bits = ByteArray((m + 7) / 8)
            var bytesRead = 0
            while (bytesRead < bits.size) {
                val read = input.read(bits, bytesRead, bits.size - bytesRead)
                if (read == -1) break
                bytesRead += read
            }
            if (bytesRead != bits.size) throw IllegalStateException("Incomplete bits payload read")

            val actualChecksum = computeChecksum(bits)
            if (actualChecksum != expectedChecksum) throw IllegalStateException("BitArray checksum mismatch (file corrupted)")

            return BloomFilter(version, domainCount, k, m, bits)
        }
    }

    private fun computeChecksum(bits: ByteArray): Int {
        // Fast MurmurHash3 checksum of bits bytes
        return BloomFilter.murmurHash3(bits, 0)
    }
}

object BloomFilterMatcher {
    fun matchRecursive(filter: BloomFilter, domain: String): Boolean {
        val normalized = domain.trim().lowercase().removeSuffix(".")
        if (normalized.isEmpty()) return false

        // Check exact domain
        if (filter.mightContain(normalized)) return true

        // Check parent subdomains recursively
        var parts = normalized.split('.')
        while (parts.size > 1) {
            parts = parts.drop(1)
            val parent = parts.joinToString(".")
            if (filter.mightContain(parent)) {
                return true
            }
        }
        return false
    }
}

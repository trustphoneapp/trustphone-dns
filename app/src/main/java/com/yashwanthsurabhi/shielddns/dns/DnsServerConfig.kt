package com.yashwanthsurabhi.shielddns.dns

object DnsServerConfig {
    const val DEFAULT_UPSTREAM = "1.1.1.1"

    fun normalizedIpv4OrDefault(value: String): String {
        val candidate = value.trim()
        return if (isIpv4(candidate)) candidate else DEFAULT_UPSTREAM
    }

    fun isIpv4(value: String): Boolean {
        val parts = value.split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() &&
                part.length <= 3 &&
                part.all { it.isDigit() } &&
                part.toIntOrNull() in 0..255
        }
    }
}

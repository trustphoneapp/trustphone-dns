package com.yashwanthsurabhi.shielddns.filter

object BlocklistParser {

    fun parse(text: String): List<String> {
        return text.lineSequence()
            .map { it.substringBefore("#").substringBefore("!").trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val domain = when {
                    line.startsWith("||") -> line.removePrefix("||").substringBefore("^").substringBefore("/")
                    else -> {
                        val parts = line.split(Regex("\\s+"))
                        if (parts.size >= 2 && (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1")) {
                            parts[1]
                        } else {
                            parts.firstOrNull() ?: ""
                        }
                    }
                }
                domain.lowercase().removeSuffix(".")
                    .takeIf { it.isNotEmpty() && isValidDomain(it) }
            }
            .distinct()
            .toList()
    }

    private fun isValidDomain(domain: String): Boolean {
        if (domain.length > 253) return false
        return domain.all { it.isLetterOrDigit() || it == '.' || it == '-' }
            && domain.contains('.')
    }
}

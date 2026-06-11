package com.yashwanthsurabhi.shielddns.filter

data class BlocklistUpdateState(
    val isUpdating: Boolean = false,
    val lastUpdatedEpochMs: Long = 0L,
    val lastDomainCount: Int = 0,
    val lastError: String? = null,
)

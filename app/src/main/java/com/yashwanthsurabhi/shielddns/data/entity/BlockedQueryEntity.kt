package com.yashwanthsurabhi.shielddns.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_queries")
data class BlockedQueryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val listName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val uid: Int = -1,
    val packageName: String? = null,
    val latencyMs: Long = 0,
    val isBlocked: Boolean = true,
    val riskScore: Int = 0,
    val dnssecStatus: String = "NONE",
    val blockedReason: String? = null
)


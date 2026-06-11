package com.yashwanthsurabhi.shielddns.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "block_domains",
    indices = [
        Index(value = ["domain"], unique = true),
        Index(value = ["category"])
    ]
)
data class BlockDomainEntity(
    @PrimaryKey val domain: String,
    val category: String,
    val source: String,
    val addedAt: Long = System.currentTimeMillis()
)

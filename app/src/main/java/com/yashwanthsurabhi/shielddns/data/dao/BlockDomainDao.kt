package com.yashwanthsurabhi.shielddns.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.yashwanthsurabhi.shielddns.data.entity.BlockDomainEntity

@Dao
interface BlockDomainDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(domains: List<BlockDomainEntity>)

    @Query("SELECT * FROM block_domains WHERE domain = :domain LIMIT 1")
    suspend fun get(domain: String): BlockDomainEntity?

    @Query("DELETE FROM block_domains WHERE domain = :domain")
    suspend fun delete(domain: String)

    @Query("DELETE FROM block_domains")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM block_domains")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM block_domains WHERE category = :category")
    suspend fun countByCategory(category: String): Int

    @Transaction
    suspend fun rebuildBlocklist(domains: List<BlockDomainEntity>) {
        deleteAll()
        insertAll(domains)
    }
}

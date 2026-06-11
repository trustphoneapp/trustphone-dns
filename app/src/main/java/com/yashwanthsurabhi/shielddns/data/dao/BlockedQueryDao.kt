package com.yashwanthsurabhi.shielddns.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yashwanthsurabhi.shielddns.data.entity.BlockedQueryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedQueryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlockedQueryEntity)

    @Query("SELECT * FROM blocked_queries ORDER BY timestamp DESC LIMIT 500")
    fun observeRecent(): Flow<List<BlockedQueryEntity>>

    @Query("SELECT * FROM blocked_queries WHERE domain LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 500")
    fun search(query: String): Flow<List<BlockedQueryEntity>>

    @Query("SELECT COUNT(*) FROM blocked_queries WHERE timestamp >= :since")
    suspend fun countSince(since: Long): Int

    @Query("DELETE FROM blocked_queries WHERE id NOT IN (SELECT id FROM blocked_queries ORDER BY timestamp DESC LIMIT 500)")
    suspend fun trimToLimit()

    @Query("DELETE FROM blocked_queries")
    suspend fun clearAll()
}

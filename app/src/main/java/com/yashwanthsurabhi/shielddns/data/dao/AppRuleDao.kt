package com.yashwanthsurabhi.shielddns.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yashwanthsurabhi.shielddns.data.entity.AppRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AppRuleEntity)

    @Query("SELECT * FROM app_rules ORDER BY appLabel ASC")
    fun observeAll(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun get(packageName: String): AppRuleEntity?

    @Query("DELETE FROM app_rules WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

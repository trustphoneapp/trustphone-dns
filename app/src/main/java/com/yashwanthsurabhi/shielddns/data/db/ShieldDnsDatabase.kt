package com.yashwanthsurabhi.shielddns.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yashwanthsurabhi.shielddns.data.dao.AppRuleDao
import com.yashwanthsurabhi.shielddns.data.dao.BlockedQueryDao
import com.yashwanthsurabhi.shielddns.data.entity.AppRuleEntity
import com.yashwanthsurabhi.shielddns.data.entity.BlockedQueryEntity

@Database(
    entities = [BlockedQueryEntity::class, AppRuleEntity::class, com.yashwanthsurabhi.shielddns.data.entity.BlockDomainEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class ShieldDnsDatabase : RoomDatabase() {
    abstract fun blockedQueryDao(): BlockedQueryDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun blockDomainDao(): com.yashwanthsurabhi.shielddns.data.dao.BlockDomainDao

    companion object {
        @Volatile
        private var instance: ShieldDnsDatabase? = null

        fun get(context: Context): ShieldDnsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShieldDnsDatabase::class.java,
                    "shield_dns.db",
                )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
    }
}

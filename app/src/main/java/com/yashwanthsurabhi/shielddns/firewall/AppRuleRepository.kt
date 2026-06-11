package com.yashwanthsurabhi.shielddns.firewall

import com.yashwanthsurabhi.shielddns.data.dao.AppRuleDao
import com.yashwanthsurabhi.shielddns.data.entity.AppRuleEntity
import com.yashwanthsurabhi.shielddns.data.entity.AppRuleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import java.util.concurrent.ConcurrentHashMap

class AppRuleRepository(
    private val appRuleDao: AppRuleDao,
    private val packageResolver: PackageResolver,
) {
    private val uidCache = ConcurrentHashMap<Int, String>()
    private val rulesCache = ConcurrentHashMap<String, AppRuleMode>()

    val rules: Flow<List<AppRuleEntity>> = appRuleDao.observeAll()

    suspend fun refreshCache() {
        rulesCache.clear()
        uidCache.clear()
        appRuleDao.observeAll().first().forEach { rule ->
            rulesCache[rule.packageName] = rule.mode
        }
        packageResolver.installedApps().forEach { app ->
            uidCache[app.uid] = app.packageName
        }
    }

    suspend fun setRule(packageName: String, label: String, mode: AppRuleMode) {
        if (mode == AppRuleMode.DEFAULT) {
            appRuleDao.delete(packageName)
            rulesCache.remove(packageName)
        } else {
            appRuleDao.upsert(AppRuleEntity(packageName, label, mode))
            rulesCache[packageName] = mode
        }
        refreshCache()
    }

    fun evaluateUid(uid: Int): UidDecision {
        if (uid < 0) return UidDecision.DEFAULT
        val packageName = packageForUid(uid) ?: return UidDecision.DEFAULT
        return when (rulesCache[packageName] ?: AppRuleMode.DEFAULT) {
            AppRuleMode.BYPASS -> UidDecision.BYPASS
            AppRuleMode.BLOCK -> UidDecision.FORCE_BLOCK
            AppRuleMode.DEFAULT -> UidDecision.DEFAULT
        }
    }

    fun packageForUid(uid: Int): String? {
        if (uidCache.isEmpty()) {
            runBlocking {
                refreshCache()
            }
        }
        return uidCache[uid] ?: run {
            val resolved = packageResolver.packageForUid(uid)
            if (resolved != null) {
                uidCache[uid] = resolved
            }
            resolved
        }
    }

    enum class UidDecision {
        DEFAULT,
        BYPASS,
        FORCE_BLOCK,
    }
}

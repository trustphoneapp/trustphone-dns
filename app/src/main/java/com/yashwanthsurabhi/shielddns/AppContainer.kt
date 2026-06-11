package com.yashwanthsurabhi.shielddns

import android.content.Context
import com.yashwanthsurabhi.shielddns.billing.BillingManager
import com.yashwanthsurabhi.shielddns.data.dao.AppRuleDao
import com.yashwanthsurabhi.shielddns.data.dao.BlockedQueryDao
import com.yashwanthsurabhi.shielddns.data.db.ShieldDnsDatabase
import com.yashwanthsurabhi.shielddns.data.store.SettingsStore
import com.yashwanthsurabhi.shielddns.dns.UpstreamResolver
import com.yashwanthsurabhi.shielddns.filter.BlocklistRepository
import com.yashwanthsurabhi.shielddns.firewall.AppRuleRepository
import com.yashwanthsurabhi.shielddns.firewall.PackageResolver
import com.yashwanthsurabhi.shielddns.rules.NetworkConditionWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database = ShieldDnsDatabase.get(appContext)

    val blockedQueryDao: BlockedQueryDao = database.blockedQueryDao()
    val appRuleDao: AppRuleDao = database.appRuleDao()
    val blockDomainDao = database.blockDomainDao()

    val settingsStore = SettingsStore(appContext)
    val upstreamResolver = UpstreamResolver()
    val blocklistRepository = BlocklistRepository(appContext)
    val packageResolver = PackageResolver(appContext)
    val appRuleRepository = AppRuleRepository(appRuleDao, packageResolver)
    val networkWatcher = NetworkConditionWatcher(appContext)

    val billingManager = BillingManager(appContext) { isPro ->
        scope.launch {
            settingsStore.setPro(isPro)
        }
    }

    init {
        scope.launch {
            if (!blocklistRepository.hasCachedLists()) {
                blocklistRepository.loadBundledStarter()
            }
            val refreshed = settingsStore.settings.first()
            blocklistRepository.refreshFromDisk(
                adsEnabled = refreshed.adsListEnabled,
                trackersEnabled = refreshed.trackersListEnabled,
                malwareEnabled = refreshed.malwareListEnabled,
                allowlist = refreshed.allowlist,
                customDeny = refreshed.customDeny,
            )
            appRuleRepository.refreshCache()
        }
    }
}

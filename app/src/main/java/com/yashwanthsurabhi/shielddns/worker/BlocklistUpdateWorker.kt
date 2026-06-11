package com.yashwanthsurabhi.shielddns.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yashwanthsurabhi.shielddns.ShieldDnsApp
import com.yashwanthsurabhi.shielddns.firebase.FirebaseTracker
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class BlocklistUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "BlocklistUpdateWorker"
        private const val WORK_NAME = "blocklist_update"

        fun schedule(context: Context, wifiOnly: Boolean = true, chargingOnly: Boolean = false) {
            val constraints = Constraints.Builder().apply {
                if (wifiOnly) {
                    setRequiredNetworkType(NetworkType.UNMETERED)
                } else {
                    setRequiredNetworkType(NetworkType.CONNECTED)
                }
                if (chargingOnly) {
                    setRequiresCharging(true)
                }
                setRequiresBatteryNotLow(true)
            }.build()

            val request = PeriodicWorkRequestBuilder<BlocklistUpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
            Log.d(TAG, "Worker scheduled with wifiOnly=$wifiOnly, chargingOnly=$chargingOnly")
        }
    }

    override suspend fun doWork(): Result {
        val container = (applicationContext as ShieldDnsApp).container
        val settings = container.settingsStore.settings.first()
        Log.d(TAG, "Starting periodic blocklist update worker run...")

        return try {
            val customUrl = settings.customBlocklistUrl.takeIf {
                settings.isPro && it.isNotBlank()
            }
            
            // Download remote lists and compile Bloom Filter
            val result = container.blocklistRepository.updateRemoteBlocklists(customUrl)
            
            if (result.isSuccess) {
                val count = result.getOrThrow()
                Log.d(TAG, "Successfully updated blocklists: $count domains loaded")
                
                FirebaseTracker.logEvent(applicationContext, "blocklist_updated")
                Result.success()
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Failed to update blocklists: ${error?.message}")
                FirebaseTracker.logException(error ?: Exception("Unknown update failure"), "Blocklist update failed")
                FirebaseTracker.logEvent(applicationContext, "blocklist_update_failed")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in blocklist updater: ${e.message}", e)
            FirebaseTracker.logException(e, "BlocklistUpdateWorker crashed")
            FirebaseTracker.logEvent(applicationContext, "blocklist_update_failed")
            Result.retry()
        }
    }
}

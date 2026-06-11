package com.yashwanthsurabhi.shielddns.firewall

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

data class InstalledApp(
    val packageName: String,
    val label: String,
    val uid: Int,
    val isSystem: Boolean,
)

class PackageResolver(private val context: Context) {

    fun installedApps(): List<InstalledApp> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }
        return activities
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { info ->
                InstalledApp(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    uid = info.uid,
                    isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun labelFor(packageName: String): String = runCatching {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

    fun packageForUid(uid: Int): String? = runCatching {
        val pm = context.packageManager
        pm.getPackagesForUid(uid)?.firstOrNull()
    }.getOrNull()
}

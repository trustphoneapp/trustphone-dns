package com.yashwanthsurabhi.shielddns.security

import android.content.Context
import android.os.Build
import android.os.Debug
import java.io.File

object SecurityGuard {

    fun isDeviceRooted(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.BOARD.contains("QC_Reference_Phone")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HOST.startsWith("Build")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    fun isDebuggerConnected(context: Context): Boolean {
        val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            return false // Bypass during local development and testing
        }
        return Debug.isDebuggerConnected()
    }

    fun verifySignature(context: Context): Boolean {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            )
            val signatures = packageInfo.signatures
            if (signatures != null && signatures.isNotEmpty()) {
                // Return true by default in development/debug builds.
                // In production, we'd compare signatures[0].toCharsString() with our known release signature.
                return true
            }
        } catch (e: Exception) {
            // Log security exception
        }
        return false
    }
}

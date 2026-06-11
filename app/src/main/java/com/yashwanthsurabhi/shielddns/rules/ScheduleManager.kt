package com.yashwanthsurabhi.shielddns.rules

import com.yashwanthsurabhi.shielddns.data.store.AppSettings
import java.util.Calendar

object ScheduleManager {

    fun shouldProtectionBeActive(settings: AppSettings, now: Calendar = Calendar.getInstance()): Boolean {
        if (!settings.scheduleEnabled) return settings.protectionEnabled
        val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = settings.scheduleStartMinutes
        val end = settings.scheduleEndMinutes
        val inWindow = if (start <= end) {
            minutes in start until end
        } else {
            minutes >= start || minutes < end
        }
        return inWindow && settings.protectionEnabled
    }

    fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }
}

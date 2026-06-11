package com.yashwanthsurabhi.shielddns.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yashwanthsurabhi.shielddns.data.store.AppSettings
import com.yashwanthsurabhi.shielddns.ui.components.StatCard

@Composable
fun StatsScreen(
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            "On-device counters only",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard("Today", settings.blockedTodayCount.toString(), Modifier.weight(1f))
            StatCard("This week", settings.blockedWeekCount.toString(), Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard("Allowlist", settings.allowlist.size.toString(), Modifier.weight(1f))
            StatCard("Deny rules", settings.customDeny.size.toString(), Modifier.weight(1f))
        }
    }
}

package com.yashwanthsurabhi.shielddns.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yashwanthsurabhi.shielddns.data.store.AppSettings
import com.yashwanthsurabhi.shielddns.ui.components.SettingToggleRow

@Composable
fun RulesScreen(
    settings: AppSettings,
    isPro: Boolean,
    scheduleLabel: String,
    onScheduleToggle: (Boolean) -> Unit,
    onScheduleChange: (Int, Int) -> Unit,
    onNetworkRules: (Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            "Automate when protection runs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingToggleRow(
                    title = "Scheduled protection",
                    subtitle = if (isPro) scheduleLabel else "Pro feature",
                    checked = settings.scheduleEnabled,
                    onCheckedChange = onScheduleToggle,
                    enabled = isPro,
                )
                if (settings.scheduleEnabled) {
                    Text("Start hour: ${settings.scheduleStartMinutes / 60}:00", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = (settings.scheduleStartMinutes / 60).toFloat(),
                        onValueChange = { hour ->
                            onScheduleChange(hour.toInt() * 60, settings.scheduleEndMinutes)
                        },
                        valueRange = 0f..23f,
                        steps = 22,
                    )
                    Text("End hour: ${settings.scheduleEndMinutes / 60}:00", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = (settings.scheduleEndMinutes / 60).toFloat(),
                        onValueChange = { hour ->
                            onScheduleChange(settings.scheduleStartMinutes, hour.toInt() * 60)
                        },
                        valueRange = 0f..23f,
                        steps = 22,
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingToggleRow(
                    title = "Mobile data only",
                    subtitle = if (isPro) "Block DNS only on cellular" else "Pro feature",
                    checked = settings.blockOnlyMobile,
                    onCheckedChange = { onNetworkRules(it, if (it) false else settings.blockOnlyWifi) },
                    enabled = isPro,
                )
                SettingToggleRow(
                    title = "Wi‑Fi only",
                    subtitle = if (isPro) "Block DNS only on Wi‑Fi" else "Pro feature",
                    checked = settings.blockOnlyWifi,
                    onCheckedChange = { onNetworkRules(if (it) false else settings.blockOnlyMobile, it) },
                    enabled = isPro,
                )
            }
        }
    }
}

package com.yashwanthsurabhi.shielddns.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yashwanthsurabhi.shielddns.data.store.AppSettings
import com.yashwanthsurabhi.shielddns.filter.BlocklistCategory
import com.yashwanthsurabhi.shielddns.filter.BlocklistUpdateState
import com.yashwanthsurabhi.shielddns.ui.components.ProUpgradeCard
import com.yashwanthsurabhi.shielddns.ui.components.ScreenHeader
import com.yashwanthsurabhi.shielddns.ui.components.SettingToggleRow
import java.text.DateFormat
import java.util.Date

@Composable
fun ListsScreen(
    settings: AppSettings,
    isPro: Boolean,
    proPrice: String?,
    blocklistCount: Int,
    categoryCounts: Map<BlocklistCategory, Int>,
    updateState: BlocklistUpdateState,
    onToggle: (ads: Boolean?, trackers: Boolean?, malware: Boolean?) -> Unit,
    onUpdate: () -> Unit,
    onCustomUrl: (String) -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var url by rememberSaveable(settings.customBlocklistUrl) { mutableStateOf(settings.customBlocklistUrl) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        ScreenHeader("Shield Lists", "Choose what to block at the DNS level")

        Text(
            text = when {
                updateState.isUpdating -> "Downloading open-source blocklists…"
                blocklistCount > 0 -> "${formatCount(blocklistCount)} domains loaded"
                else -> "Starter list only — tap Update below"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (updateState.lastUpdatedEpochMs > 0 && !updateState.isUpdating) {
            Text(
                "Last updated: ${DateFormat.getDateTimeInstance().format(Date(updateState.lastUpdatedEpochMs))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        updateState.lastError?.let { error ->
            Text(
                "Some sources failed ($error) — cached lists still work.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingToggleRow(
                    title = "Ads",
                    subtitle = "${formatCount(categoryCounts[BlocklistCategory.ADS] ?: 0)} advertising domains",
                    checked = settings.adsListEnabled,
                    onCheckedChange = { onToggle(it, null, null) },
                )
                SettingToggleRow(
                    title = "Trackers",
                    subtitle = "${formatCount(categoryCounts[BlocklistCategory.TRACKERS] ?: 0)} analytics and tracking domains",
                    checked = settings.trackersListEnabled,
                    onCheckedChange = { onToggle(null, it, null) },
                )
                SettingToggleRow(
                    title = "Malware",
                    subtitle = "${formatCount(categoryCounts[BlocklistCategory.MALWARE] ?: 0)} malware and phishing domains",
                    checked = settings.malwareListEnabled,
                    onCheckedChange = { onToggle(null, null, it) },
                )
            }
        }

        Button(
            onClick = onUpdate,
            enabled = !updateState.isUpdating,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(if (updateState.isUpdating) "Updating…" else "Update blocklists now")
        }

        if (!isPro) {
            ProUpgradeCard(
                price = proPrice,
                onUpgrade = onUpgrade,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        if (isPro) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Custom blocklist URL") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Button(
                onClick = { onCustomUrl(url) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Save custom URL")
            }
        } else {
            Text(
                "Pro unlocks custom blocklist URLs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
    count >= 1_000 -> "%.1fK".format(count / 1_000.0)
    else -> count.toString()
}

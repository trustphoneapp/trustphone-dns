package com.yashwanthsurabhi.shielddns.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.yashwanthsurabhi.shielddns.ui.components.SettingToggleRow

@Composable
fun SettingsScreen(
    settings: AppSettings,
    isPro: Boolean,
    proPrice: String?,
    exportedJson: String,
    onUpstreamChange: (String) -> Unit,
    onDohChange: (Boolean) -> Unit,
    onBootChange: (Boolean) -> Unit,
    onThemeChange: (Boolean?) -> Unit,
    onExport: () -> String,
    onImport: (String) -> Unit,
    onUnlockPro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var upstream by rememberSaveable(settings.upstreamDns) { mutableStateOf(settings.upstreamDns) }
    var importText by rememberSaveable { mutableStateOf("") }
    var exportText by rememberSaveable(exportedJson) { mutableStateOf(exportedJson) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            "DNS resolver, theme, and backup",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = upstream,
                    onValueChange = { upstream = it },
                    label = { Text("Upstream DNS (UDP)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Button(
                    onClick = { onUpstreamChange(upstream) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Save upstream") }

                Text(
                    "If pages won't load, set Android Private DNS to Off (Settings → Connections → More connection settings).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                SettingToggleRow("DNS-over-HTTPS", "Encrypt DNS over HTTPS (no plaintext fallback)", settings.useDoh, onDohChange)
                SettingToggleRow("Start on boot", "Reconnect after restart", settings.startOnBoot, onBootChange)
                Text("Appearance", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.darkTheme == null,
                        onClick = { onThemeChange(null) },
                        label = { Text("System") },
                    )
                    FilterChip(
                        selected = settings.darkTheme == false,
                        onClick = { onThemeChange(false) },
                        label = { Text("Light") },
                    )
                    FilterChip(
                        selected = settings.darkTheme == true,
                        onClick = { onThemeChange(true) },
                        label = { Text("Dark") },
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Backup", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = { exportText = onExport() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Export settings JSON") }
                if (exportText.isNotBlank()) {
                    Text(
                        exportText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text("Paste JSON to import") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    minLines = 3,
                )
                Button(
                    onClick = { onImport(importText) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Import settings") }
            }
        }

        if (!isPro) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Shield DNS Pro", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Unlock custom blocklists, per-app DNS rules, schedules, and network-based automation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Button(
                        onClick = onUnlockPro,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) {
                        Text("Unlock lifetime Pro${proPrice?.let { " • $it" } ?: ""}")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Text(
                    "Pro active",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

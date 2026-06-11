package com.yashwanthsurabhi.shielddns.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yashwanthsurabhi.shielddns.data.entity.AppRuleEntity
import com.yashwanthsurabhi.shielddns.data.entity.AppRuleMode
import com.yashwanthsurabhi.shielddns.firewall.InstalledApp

@Composable
fun AppsScreen(
    apps: List<InstalledApp>,
    rules: List<AppRuleEntity>,
    isPro: Boolean,
    onSetRule: (String, String, AppRuleMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ruleMap = rules.associate { it.packageName to it.mode }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            if (isPro) "${apps.size} installed apps" else "Pro unlocks per-app DNS bypass and block rules",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        LazyColumn {
            items(apps, key = { it.packageName }) { app ->
                val mode = ruleMap[app.packageName] ?: AppRuleMode.DEFAULT
                AppRuleRow(app, mode, isPro, onSetRule)
            }
        }
    }
}

@Composable
private fun AppRuleRow(
    app: InstalledApp,
    mode: AppRuleMode,
    isPro: Boolean,
    onSetRule: (String, String, AppRuleMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(app.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { if (isPro) expanded = true },
                enabled = isPro,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(if (isPro) mode.displayName() else "Pro required")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                AppRuleMode.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName()) },
                        onClick = {
                            onSetRule(app.packageName, app.label, option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun AppRuleMode.displayName(): String = when (this) {
    AppRuleMode.DEFAULT -> "Default"
    AppRuleMode.BYPASS -> "Bypass DNS"
    AppRuleMode.BLOCK -> "Block DNS"
}

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yashwanthsurabhi.shielddns.filter.BlocklistParser

@Composable
fun AllowlistScreen(
    allowlist: Set<String>,
    customDeny: Set<String>,
    onAllowlistChange: (Set<String>) -> Unit,
    onDenyChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var allowInput by rememberSaveable { mutableStateOf(allowlist.joinToString("\n")) }
    var denyInput by rememberSaveable { mutableStateOf(customDeny.joinToString("\n")) }

    LaunchedEffect(allowlist) {
        val synced = allowlist.joinToString("\n")
        if (allowInput != synced && !allowInput.contains("\n")) allowInput = synced
    }
    LaunchedEffect(customDeny) {
        val synced = customDeny.joinToString("\n")
        if (denyInput != synced && !denyInput.contains("\n")) denyInput = synced
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            "One domain per line",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Allowlist", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = allowInput,
                    onValueChange = { allowInput = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    minLines = 4,
                    placeholder = { Text("example.com") },
                )
                Button(onClick = {
                    onAllowlistChange(parseDomains(allowInput))
                }) { Text("Save allowlist") }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Custom deny", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = denyInput,
                    onValueChange = { denyInput = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    minLines = 4,
                    placeholder = { Text("bad-site.com") },
                )
                Button(onClick = {
                    onDenyChange(parseDomains(denyInput))
                }) { Text("Save deny rules") }
            }
        }
    }
}

private fun parseDomains(text: String): Set<String> =
    BlocklistParser.parse(text).toSet()

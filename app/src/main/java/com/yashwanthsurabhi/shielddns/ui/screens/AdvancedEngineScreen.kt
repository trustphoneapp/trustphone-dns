package com.yashwanthsurabhi.shielddns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashwanthsurabhi.shielddns.ui.ShieldViewModel

@Composable
fun AdvancedEngineScreen(viewModel: ShieldViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Blocklist Engine Metrics",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Bloom Filter Loaded:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (viewModel.isBloomFilterLoaded) "Yes (RAM)" else "No",
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.isBloomFilterLoaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "SQLite Fallback Verify:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Active (Room Indexed)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Consolidated Domains:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${viewModel.totalBlockedDomains}", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Estimated RAM Usage:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = viewModel.estimatedBloomRamBytes, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Database Size (On Disk):", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = viewModel.databaseSizeBytes, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "Engine Architecture Info",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "To keep memory consumption low for continuous VPN service execution, TrustPhone DNS uses a double-verification pipeline:\n\n" +
                    "1. Bloom Filter: Checks incoming queries recursively (e.g. sub.example.com -> example.com -> com) in-memory using fast MurmurHash3 computations. Allowed queries typically resolve in microseconds.\n\n" +
                    "2. Room DB Lookup: If the Bloom Filter reports a possible match (a real blocked domain, or its ~1% false-positive rate), Room queries the indexed SQLite database on disk to confirm before blocking.\n\n" +
                    "This design limits garbage-collection pressure and keeps the on-device block decision in the low-millisecond range. Actual latency and memory depend on blocklist size and device.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
            lineHeight = 20.sp
        )
    }
}

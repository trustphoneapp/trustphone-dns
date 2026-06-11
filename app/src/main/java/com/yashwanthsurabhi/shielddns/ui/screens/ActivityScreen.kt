package com.yashwanthsurabhi.shielddns.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yashwanthsurabhi.shielddns.data.entity.BlockedQueryEntity
import com.yashwanthsurabhi.shielddns.ui.components.ScreenHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    queries: List<BlockedQueryEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        ScreenHeader("Activity", "${queries.size} recent blocked queries")
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = { Text("Search domains") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            singleLine = true,
        )
        if (queries.isEmpty()) {
            Text(
                "No blocked queries yet. Connect protection and browse to see activity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn {
                items(queries, key = { it.id }) { item ->
                    val time = SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(item.domain, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "$time · ${item.listName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.yashwanthsurabhi.shielddns.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yashwanthsurabhi.shielddns.data.store.AppSettings
import com.yashwanthsurabhi.shielddns.rules.NetworkType
import com.yashwanthsurabhi.shielddns.ui.components.ScreenHeader
import com.yashwanthsurabhi.shielddns.ui.components.ShieldHero3D
import com.yashwanthsurabhi.shielddns.ui.components.StatCard
import com.yashwanthsurabhi.shielddns.ui.theme.ShieldDanger
import com.yashwanthsurabhi.shielddns.ui.theme.ShieldSuccess

@Composable
fun HomeScreen(
    settings: AppSettings,
    networkType: NetworkType,
    blocklistCount: Int,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = settings.protectionEnabled
    val statusColor by animateColorAsState(
        targetValue = if (active) ShieldSuccess else ShieldDanger,
        animationSpec = tween(450),
        label = "statusColor",
    )
    val readiness by animateFloatAsState(
        targetValue = if (active && blocklistCount > 0) 1f else if (active) 0.68f else 0.18f,
        animationSpec = tween(700),
        label = "readiness",
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        ScreenHeader("TrustPhone DNS", "System-wide DNS firewall")

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ),
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StatusPill(active = active, statusColor = statusColor)
                ShieldHero3D(active = active)
                AnimatedContent(
                    targetState = active,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "statusText",
                ) { isActive ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (isActive) "Protection is active" else "Protection is off",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                        )
                        Text(
                            if (isActive) {
                                "Filtering DNS requests before ads, trackers, and risky domains load."
                            } else {
                                "Turn on local DNS filtering to protect apps and browsers on this device."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { readiness },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { onToggle(!active) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            if (active) "Disconnect" else "Connect",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = { onToggle(true) },
                        enabled = !active,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Secure", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InsightChip(
                label = settings.upstreamDns,
                icon = Icons.Default.Dns,
                modifier = Modifier.weight(1f),
            )
            InsightChip(
                label = if (settings.useDoh) "DoH fallback" else "UDP DNS",
                icon = Icons.Default.Public,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = "Blocked today",
                value = settings.blockedTodayCount.toString(),
                icon = Icons.Default.Shield,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "This week",
                value = settings.blockedWeekCount.toString(),
                icon = Icons.Default.Timeline,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = "Blocklist",
                value = formatCount(blocklistCount),
                icon = Icons.Default.Bolt,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Network",
                value = networkType.label(),
                icon = Icons.Default.NetworkCheck,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = "Memory Mode",
                value = "Optimized",
                icon = Icons.Default.Memory,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Engine Type",
                value = "Bloom Filter",
                icon = Icons.Default.Security,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatusPill(active: Boolean, statusColor: Color) {
    Surface(
        color = statusColor.copy(alpha = 0.12f),
        contentColor = statusColor,
        shape = CircleShape,
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Text(
                if (active) "Live DNS firewall" else "Ready to protect",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun InsightChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fK".format(n / 1_000.0)
    else -> n.toString()
}

private fun NetworkType.label(): String = when (this) {
    NetworkType.WIFI -> "Wi‑Fi"
    NetworkType.MOBILE -> "Mobile"
    NetworkType.NONE -> "Offline"
    NetworkType.OTHER -> "Other"
}

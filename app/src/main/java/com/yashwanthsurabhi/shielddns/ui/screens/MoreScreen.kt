package com.yashwanthsurabhi.shielddns.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yashwanthsurabhi.shielddns.ui.components.ProUpgradeCard
import com.yashwanthsurabhi.shielddns.ui.components.ScreenHeader
import com.yashwanthsurabhi.shielddns.ui.navigation.NavRoutes

data class MoreItem(val route: String, val title: String, val subtitle: String, val icon: ImageVector)

private val moreItems = listOf(
    MoreItem(NavRoutes.ALLOWLIST, "Allowlist", "Trusted domains", Icons.Default.Shield),
    MoreItem(NavRoutes.APPS, "Apps", "Per-app rules", Icons.Default.Apps),
    MoreItem(NavRoutes.RULES, "Rules", "Schedule & network", Icons.AutoMirrored.Filled.Rule),
    MoreItem(NavRoutes.STATS, "Stats", "Blocked counts", Icons.Default.BarChart),
    MoreItem(NavRoutes.SETTINGS, "Settings", "DNS & backup", Icons.Default.Settings),
    MoreItem(NavRoutes.RESOLVER_HEALTH, "Resolver Health", "DNS performance", Icons.Default.HealthAndSafety),
    MoreItem(NavRoutes.ADVANCED_ENGINE, "Advanced Engine", "Bloom stats", Icons.Default.DeveloperMode),
    MoreItem(NavRoutes.PRIVACY_CENTER, "Privacy Center", "Consent & logs", Icons.Default.PrivacyTip),
    MoreItem(NavRoutes.POLICY_CENTER, "Policy Center", "Filtering profiles", Icons.Default.Policy),
    MoreItem(NavRoutes.FAMILY_CENTER, "Family Safety", "Parental controls", Icons.Default.People),
    MoreItem(NavRoutes.THREAT_CENTER, "Threat Center", "Threat metrics", Icons.Default.Security),
    MoreItem(NavRoutes.ABOUT, "About", "Privacy info", Icons.Default.Info),
)

@Composable
fun MoreScreen(
    isPro: Boolean,
    proPrice: String?,
    onNavigate: (String) -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        ScreenHeader("More", "Tools & configuration")
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(moreItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(item.route) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Text(
                            item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (!isPro) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ProUpgradeCard(
                        price = proPrice,
                        onUpgrade = onUpgrade,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

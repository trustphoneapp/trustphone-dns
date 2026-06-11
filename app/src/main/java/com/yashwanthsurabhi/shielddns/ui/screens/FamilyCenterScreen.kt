package com.yashwanthsurabhi.shielddns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashwanthsurabhi.shielddns.ui.ShieldViewModel
import com.yashwanthsurabhi.shielddns.policy.PolicyProfile
import com.yashwanthsurabhi.shielddns.filter.BlocklistCategory

@Composable
fun FamilyCenterScreen(viewModel: ShieldViewModel) {
    val settings by viewModel.settings.collectAsState()
    val blockedQueries by viewModel.blockedQueries.collectAsState()
    val categoryCounts by viewModel.blocklistCategoryCounts.collectAsState()
    val scrollState = rememberScrollState()

    // Calculate how many adult/gambling queries were blocked recently
    val blockedAdultCount = blockedQueries.count {
        it.isBlocked && (it.listName == BlocklistCategory.ADULT.displayName || it.listName == BlocklistCategory.GAMBLING.displayName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Gradient Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Family Safety Hub",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enforce parental controls, restrict adult content, and maintain clean browsing.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Active Family Filtering",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick Status Indicator",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Family Filtering Active:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val active = settings.activeProfile == PolicyProfile.CHILD.key || 
                                 settings.activeProfile == PolicyProfile.TEEN.key || 
                                 settings.activeProfile == PolicyProfile.FAMILY.key
                    Text(
                        text = if (active) "YES" else "NO",
                        fontWeight = FontWeight.Bold,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Active Profile Type:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = settings.activeProfile,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Text(
            text = "Protected Database Information",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
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
                    Text(text = "Known Adult Domains:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val adultCount = categoryCounts[BlocklistCategory.ADULT] ?: 0
                    Text(text = "$adultCount", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Known Gambling Domains:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val gamblingCount = categoryCounts[BlocklistCategory.GAMBLING] ?: 0
                    Text(text = "$gamblingCount", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Violations Blocked Today:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$blockedAdultCount",
                        fontWeight = FontWeight.Bold,
                        color = if (blockedAdultCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Text(
            text = "Enforced Controls Info",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "To keep your kids safer, select the 'Child' or 'Teen' profile inside the Policy Center. " +
                    "These profiles block domains categorized as adult, gambling, scam, phishing, and malware, " +
                    "and strip advertising and tracking from apps so commercial profiles aren't built on minors. " +
                    "Filtering quality depends on the on-device blocklists, and category coverage is best-effort — " +
                    "it is not a guarantee that every adult or unsafe site will be blocked.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

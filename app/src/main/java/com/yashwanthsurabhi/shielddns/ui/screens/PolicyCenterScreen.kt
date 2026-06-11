package com.yashwanthsurabhi.shielddns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashwanthsurabhi.shielddns.policy.PolicyProfile
import com.yashwanthsurabhi.shielddns.ui.ShieldViewModel

@Composable
fun PolicyCenterScreen(viewModel: ShieldViewModel) {
    val settings by viewModel.settings.collectAsState()
    val scrollState = rememberScrollState()

    val profiles = listOf(
        PolicyProfile.DEFAULT to "Standard blocking filters matching toggle preferences.",
        PolicyProfile.CHILD to "Ultra-strict blocking of all trackers, ads, adult content, gambling, telemetry, and risky sites.",
        PolicyProfile.TEEN to "Restricts adult content, gambling, scams, and high-risk domains while preserving normal site access.",
        PolicyProfile.FAMILY to "Secures your entire household. Auto-blocks malware, adult content, gambling, and active scams.",
        PolicyProfile.WORK to "Optimized for productivity. Blocks malware, gambling, adult content, and tracking beacons.",
        PolicyProfile.TRAVEL to "Data-saving profile. Heavily blocks telemetry, ads, trackers, and malicious sites."
    )

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
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Policy Configuration Center",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select profiles to control DNS resolution rules across all applications.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Active Profile Selection",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        profiles.forEach { (profile, description) ->
            val isSelected = settings.activeProfile == profile.key
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { viewModel.setActiveProfile(profile.key) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.setActiveProfile(profile.key) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = profile.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Threat Intelligence Threshold",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Domains exceeding this risk score will be immediately blocked by the Reputation Engine.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Maximum Allowed Risk Score:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${settings.maxAllowedRiskScore} / 100",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = settings.maxAllowedRiskScore.toFloat(),
                    onValueChange = { viewModel.setMaxAllowedRiskScore(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 20
                )

                Spacer(modifier = Modifier.height(8.dp))

                val riskLabel = when {
                    settings.maxAllowedRiskScore <= 20 -> "Extremely Paranoid (Blocks almost all suspicious sites)"
                    settings.maxAllowedRiskScore <= 50 -> "High Security (Strict block of typosquats and homographs)"
                    settings.maxAllowedRiskScore <= 80 -> "Balanced Protection (Standard consumer defense, recommended)"
                    else -> "Relaxed Security (Only block confirmed malware domains)"
                }

                Text(
                    text = "Protection Mode: $riskLabel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

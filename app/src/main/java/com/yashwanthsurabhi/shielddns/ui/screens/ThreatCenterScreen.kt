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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ThreatCenterScreen(viewModel: ShieldViewModel) {
    val blockedQueries by viewModel.blockedQueries.collectAsState()
    val scrollState = rememberScrollState()

    // Filter to queries that have reputation/phishing/scam indications or non-zero risk scores
    val threatLogs = blockedQueries.filter { it.riskScore > 0 || it.listName == "Threat Prevention" }

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
                        colors = listOf(Color(0xFF3F51B5), Color(0xFF00BCD4))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Threat Intelligence Hub",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Typosquatting detection, homograph indicators, and DNSSEC verification statuses.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Real-time Threat Analysis Feed",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (threatLogs.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No threat indicators detected in recent DNS traffic.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            threatLogs.take(20).forEach { log ->
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
                val timeString = timeFormat.format(Date(log.timestamp))
                
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeString,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // Risk Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = when {
                                            log.riskScore >= 75 -> Color(0xFFEF5350)
                                            log.riskScore >= 50 -> Color(0xFFFFB74D)
                                            else -> Color(0xFF81C784)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Risk: ${log.riskScore}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = log.domain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DNSSEC: ${log.dnssecStatus}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (log.dnssecStatus) {
                                    "SECURE", "VALIDATED" -> Color(0xFF4CAF50)
                                    "INSECURE" -> Color(0xFFFF9800)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            
                            Text(
                                text = "App: ${log.packageName?.substringAfterLast('.') ?: "System"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (!log.blockedReason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Indicator: ${log.blockedReason}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Security Definitions",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "• Typosquatting: Domain names similar to popular services (e.g. paypa1.com) created by malicious actors.\n\n" +
                    "• IDN Homograph: Internationalized domains containing non-ASCII characters that look visually identical to standard characters (e.g. Cyrillic 'а' replacing Latin 'a') to spoof users.\n\n" +
                    "• DNSSEC (Domain Name System Security Extensions): Cryptographic signatures ensuring the DNS responses have not been intercepted or altered in transit. A state of SECURE or VALIDATED confirms validation against upstream trust chains.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

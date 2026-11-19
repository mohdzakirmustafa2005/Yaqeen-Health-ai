package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.HealthCheckIn
import com.example.model.RiskLevel
import com.example.ui.components.MultiMetricTrendChart
import com.example.ui.components.NonDiagnosticBanner
import com.example.ui.components.RiskBadge

@Composable
fun TrendsAndReportsScreen(
    strings: LocalizedStrings,
    checkIns: List<HealthCheckIn>,
    onBuildReportText: (periodDays: Int) -> String
) {
    val context = LocalContext.current
    var periodDays by remember { mutableIntStateOf(7) }
    val reportPreview = remember(checkIns, periodDays) { onBuildReportText(periodDays) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item { Text(strings.trendsAndReports, style = MaterialTheme.typography.headlineSmall) }
        item { NonDiagnosticBanner(text = strings.nonDiagnosticBanner) }

        item {
            MultiMetricTrendChart(checkIns = checkIns)
        }

        // Report Generator Card
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Personal Health Report", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7, 14, 30).forEach { days ->
                            FilterChip(
                                selected = periodDays == days,
                                onClick = { periodDays = days },
                                label = { Text("Last $days Check-Ins") }
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = reportPreview.take(420) + if (reportPreview.length > 420) "\n..." else "",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Yaqeen Health Personal Wellness Report")
                                putExtra(Intent.EXTRA_TEXT, reportPreview)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, strings.exportReport))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("export_report_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.exportReport)
                    }
                }
            }
        }

        // Check-In History Timeline
        item { Text("Check-In History (${checkIns.size})", style = MaterialTheme.typography.titleMedium) }
        if (checkIns.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Text(
                        text = strings.emptyCheckInTip,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(checkIns, key = { it.id.ifBlank { "${it.dateKey}_${it.hashCode()}" } }) { ci ->
                val level = RiskLevel.fromRaw(ci.computedRiskLevel)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(ci.dateKey, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            RiskBadge(level = level, strings = strings)
                        }
                        Text(
                            text = "Wellbeing ${ci.overallWellbeing}/10 · Sleep ${ci.sleepHours}h (q=${ci.sleepQuality}/10) · Stress ${ci.stressLevel}/10 · Fatigue ${ci.fatigueLevel}/10",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (ci.symptoms.isNotEmpty()) {
                            Text(
                                text = "Symptoms: ${ci.symptoms.joinToString(", ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!ci.notes.isNullOrBlank()) {
                            Text(ci.notes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

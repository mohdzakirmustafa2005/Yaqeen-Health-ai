package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.AlertStatus
import com.example.model.HealthCheckIn
import com.example.model.RiskAlert
import com.example.model.RiskLevel
import com.example.model.UserProfile
import com.example.model.WellnessRecommendation
import com.example.ui.components.MultiMetricTrendChart
import com.example.ui.components.NonDiagnosticBanner
import com.example.ui.components.RiskBadge
import com.example.ui.components.WellnessScoreRing
import com.example.viewmodel.HealthDashboardMetrics

@Composable
fun DashboardScreen(
    strings: LocalizedStrings,
    profile: UserProfile?,
    metrics: HealthDashboardMetrics,
    checkIns: List<HealthCheckIn>,
    alerts: List<RiskAlert>,
    recommendations: List<WellnessRecommendation>,
    onStartCheckIn: () -> Unit,
    onOpenTrendsReports: () -> Unit,
    onOpenPreventionPlan: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenDoctorHome: () -> Unit = {},
    onOpenHeartHome: () -> Unit = {},
    onOpenReportExplainer: () -> Unit = {},
    onOpenMentalWellness: () -> Unit = {},
    onToggleRecommendation: (String, Boolean) -> Unit
) {
    val activeAlerts = alerts.filter { it.status == AlertStatus.ACTIVE.name }
    val latestCheckIn = checkIns.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            NonDiagnosticBanner(text = strings.nonDiagnosticBanner)
        }

        // Hero Wellness Score + Risk Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hello, ${profile?.displayName ?: "Team Member"}",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${profile?.department ?: "Operations"} · ${strings.riskStatus}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            RiskBadge(level = metrics.currentRiskLevel, strings = strings)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                MiniMetricStat("Burnout", "${metrics.burnoutIndex}%")
                                MiniMetricStat(
                                    "Avg Sleep",
                                    if (metrics.avgSleepHours > 0f) String.format("%.1fh", metrics.avgSleepHours) else "--"
                                )
                                MiniMetricStat(
                                    "Avg Stress",
                                    if (metrics.avgStress > 0f) String.format("%.1f/10", metrics.avgStress) else "--"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        WellnessScoreRing(
                            score = metrics.latestWellnessScore,
                            label = strings.wellnessScore,
                            riskLevel = metrics.currentRiskLevel
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onStartCheckIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dashboard_start_checkin_button")
                    ) {
                        Icon(Icons.Filled.AddTask, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.startCheckIn)
                    }
                }
            }
        }

        // Latest AI Insight Highlight or Empty Check-in Tip
        item {
            if (latestCheckIn == null) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = strings.emptyCheckInTip,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (!latestCheckIn.aiSummary.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAssistant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = "AI Insight",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Latest AI Trend Insight", style = MaterialTheme.typography.titleSmall)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open AI Guide", modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(latestCheckIn.aiSummary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Active Alerts Strip
        if (activeAlerts.isNotEmpty()) {
            item {
                val topAlert = activeAlerts.first()
                val level = RiskLevel.fromRaw(topAlert.riskLevel)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_active_alert_card")
                        .clickable(onClick = onOpenAlerts)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            Icons.Filled.WarningAmber,
                            contentDescription = "Active early warning alert",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${activeAlerts.size} Active Early Warning (${level.label})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = topAlert.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = onOpenAlerts) {
                            Text("Review")
                        }
                    }
                }
            }
        }

        // 7-Day Trend Chart
        item {
            MultiMetricTrendChart(checkIns = checkIns)
        }

        // Quick Hub Navigation Row (AI Doctor + AI Heart Specialist)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_open_doctor_card")
                        .clickable(onClick = onOpenDoctorHome)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Filled.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("AI Doctor Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("7 clinical modes & care navigation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_open_heart_card")
                        .clickable(onClick = onOpenHeartHome)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Heart Specialist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Cardiology AI, vitals & 112 check", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Quick Hub Row 2 (Report Explainer + Mental Wellness)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_open_report_explainer_card")
                        .clickable(onClick = onOpenReportExplainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Report Explainer", style = MaterialTheme.typography.titleSmall)
                        Text("Explain lab & ECG reports", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_open_mental_wellness_card")
                        .clickable(onClick = onOpenMentalWellness)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Filled.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Mental Wellness", style = MaterialTheme.typography.titleSmall)
                        Text("Breathing, grounding & burnout", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Quick Hub Row 3 (Trends/Reports + Alerts/Notifications)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_trends_reports_card")
                        .clickable(onClick = onOpenTrendsReports)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Filled.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(strings.trendsAndReports, style = MaterialTheme.typography.titleSmall)
                        Text("${checkIns.size} logged entries", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_alerts_notifs_card")
                        .clickable(onClick = onOpenAlerts)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(strings.alertsAndNotifs, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${metrics.activeAlertsCount} alerts · ${metrics.unreadNotifsCount} unread",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Today's Personalized Prevention Actions
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${strings.preventionPlan} (${metrics.completedRecsCount}/${metrics.totalRecsCount})",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onOpenPreventionPlan) {
                    Text("View All")
                }
            }
        }

        if (recommendations.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Submit a daily check-in to generate personalized sleep, stress, and hydration habits.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(recommendations.take(3), key = { it.id }) { rec ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleRecommendation(rec.id, !rec.completed) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = if (rec.completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = if (rec.completed) "Completed" else "Mark completed",
                            tint = if (rec.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rec.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                rec.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun MiniMetricStat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

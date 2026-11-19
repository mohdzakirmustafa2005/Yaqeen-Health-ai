package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.AlertStatus
import com.example.model.RecCategory
import com.example.model.RiskAlert
import com.example.model.RiskLevel
import com.example.model.SmartNotification
import com.example.model.UserProfile
import com.example.model.WellnessRecommendation
import com.example.ui.components.NonDiagnosticBanner
import com.example.ui.components.RiskBadge

@Composable
fun PreventionPlanScreen(
    strings: LocalizedStrings,
    recommendations: List<WellnessRecommendation>,
    onToggleRecommendation: (String, Boolean) -> Unit
) {
    val grouped = remember(recommendations) {
        recommendations.groupBy { RecCategory.fromRaw(it.category) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item { Text(strings.preventionPlan, style = MaterialTheme.typography.headlineSmall) }
        item { NonDiagnosticBanner(text = strings.nonDiagnosticBanner) }

        if (recommendations.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Text(
                        text = "Complete a daily health check-in to receive personalized sleep, hydration, movement, and stress prevention actions.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            grouped.forEach { (category, itemsList) ->
                item(key = "header_${category.name}") {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(itemsList, key = { it.id }) { rec ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rec_card_${rec.id}")
                            .clickable { onToggleRecommendation(rec.id, !rec.completed) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                imageVector = if (rec.completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = "Toggle completion",
                                tint = if (rec.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(rec.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${rec.frequency} · ${rec.source}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(rec.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertsAndNotificationsScreen(
    strings: LocalizedStrings,
    profile: UserProfile?,
    alerts: List<RiskAlert>,
    notifications: List<SmartNotification>,
    onUpdateAlertStatus: (String, AlertStatus) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onSaveReminderSettings: (enabled: Boolean, hour: Long, minute: Long) -> Unit
) {
    var remindersEnabled by remember(profile?.remindersEnabled) { mutableStateOf(profile?.remindersEnabled ?: true) }
    var reminderHour by remember(profile?.reminderHour) { mutableLongStateOf(profile?.reminderHour ?: 9L) }
    var permissionNotice by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionNotice = if (granted) {
            "System notification permission granted."
        } else {
            "System notifications denied; in-app smart reminders remain active."
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item { Text(strings.alertsAndNotifs, style = MaterialTheme.typography.headlineSmall) }

        // Smart Reminder Preferences Card
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Daily Check-In Reminder", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Scheduled at ${String.format("%02d:00", reminderHour)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { enabled ->
                                remindersEnabled = enabled
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onSaveReminderSettings(enabled, reminderHour, 0L)
                            },
                            modifier = Modifier.testTag("reminders_toggle")
                        )
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(8L, 9L, 12L, 18L, 20L).forEach { hour ->
                            AssistChip(
                                onClick = {
                                    reminderHour = hour
                                    onSaveReminderSettings(remindersEnabled, hour, 0L)
                                },
                                label = { Text(String.format("%02d:00", hour)) }
                            )
                        }
                    }

                    if (permissionNotice != null) {
                        Text(permissionNotice.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Early Warning Alerts
        item { Text("Early Warning Alert History", style = MaterialTheme.typography.titleMedium) }

        if (alerts.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Text(
                        "No early warning alerts recorded. Your check-in metrics are within stable thresholds.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                val level = RiskLevel.fromRaw(alert.riskLevel)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RiskBadge(level = level, strings = strings)
                            Text(
                                text = "${alert.dateKey} · ${alert.status}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(alert.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(alert.explanation, style = MaterialTheme.typography.bodySmall)
                        if (alert.triggerFactors.isNotEmpty()) {
                            Text(
                                text = "Triggers: ${alert.triggerFactors.joinToString(" · ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Next step: ${alert.recommendedNextStep}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        if (alert.status == AlertStatus.ACTIVE.name) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onUpdateAlertStatus(alert.id, AlertStatus.ACKNOWLEDGED) },
                                    modifier = Modifier.weight(1f).testTag("ack_alert_${alert.id}")
                                ) {
                                    Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Acknowledge")
                                }
                                OutlinedButton(
                                    onClick = { onUpdateAlertStatus(alert.id, AlertStatus.DISMISSED) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Smart Notifications Inbox
        item { Text("Smart Notifications", style = MaterialTheme.typography.titleMedium) }
        items(notifications, key = { it.id }) { notif ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (notif.readStatus) {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(notif.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!notif.readStatus) {
                        TextButton(onClick = { onMarkNotificationRead(notif.id) }) {
                            Icon(Icons.Filled.MarkEmailRead, contentDescription = "Mark read", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Read")
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

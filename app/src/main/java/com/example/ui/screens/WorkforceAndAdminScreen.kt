package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.AuditLogEntry
import com.example.model.CoordinatorOutreachCase
import com.example.model.DepartmentAggregate
import com.example.model.UserRole
import com.example.model.WorkforceAnalyticsSummary
import com.example.ui.components.DemoModePill
import com.example.ui.components.RiskBadge
import com.example.ui.theme.RiskGreenFg
import com.example.ui.theme.RiskOrangeFg
import com.example.ui.theme.RiskRedFg
import com.example.ui.theme.RiskYellowFg

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkforceAndAdminScreen(
    strings: LocalizedStrings,
    activeRole: UserRole,
    isDemoMode: Boolean,
    analytics: WorkforceAnalyticsSummary,
    coordinatorCases: List<CoordinatorOutreachCase>,
    auditLogs: List<AuditLogEntry>,
    aiCallsCount: Int,
    onSwitchDemoRole: (UserRole) -> Unit,
    onRecordFollowUpNote: (caseId: String, note: String) -> Unit
) {
    var selectedTab by remember(activeRole) {
        mutableIntStateOf(
            when (activeRole) {
                UserRole.COORDINATOR -> 1
                UserRole.PLATFORM_ADMIN -> 2
                else -> 0
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Workforce & Governance Hub", style = MaterialTheme.typography.headlineSmall)
                    Text("Role: ${activeRole.label}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (isDemoMode) {
                    DemoModePill(label = strings.demoBadge)
                }
            }
        }

        if (isDemoMode) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Judge Role Preview Switcher", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            UserRole.entries.forEach { role ->
                                FilterChip(
                                    selected = activeRole == role,
                                    onClick = { onSwitchDemoRole(role) },
                                    label = { Text(role.label) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Tabs (Org Analytics | Coordinator Outreach | Admin & Audit)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Org Analytics") },
                    leadingIcon = { Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                if (activeRole == UserRole.COORDINATOR || activeRole == UserRole.PLATFORM_ADMIN || isDemoMode) {
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Coordinator") },
                        leadingIcon = { Icon(Icons.Filled.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                if (activeRole == UserRole.PLATFORM_ADMIN || activeRole == UserRole.ORG_ADMIN || isDemoMode) {
                    FilterChip(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("Admin & Audit") },
                        leadingIcon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // Privacy-Preserving Organization Analytics
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Privacy Shield Active: Individual records are isolated. Groups with fewer than ${DepartmentAggregate.MIN_GROUP_SIZE} members are automatically suppressed to prevent re-identification.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(analytics.organizationName, style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                OrgKpi("Consented", "${analytics.totalConsentedMembers}")
                                OrgKpi("Participation", "${analytics.participationRatePercent}%")
                                OrgKpi("Avg Wellbeing", String.format("%.1f/10", analytics.avgWellbeingScore))
                            }
                            Text("Anonymized Risk Distribution", style = MaterialTheme.typography.labelLarge)
                            RiskDistributionStackedBar(
                                green = analytics.greenCount,
                                yellow = analytics.yellowCount,
                                orange = analytics.orangeCount,
                                red = analytics.redCount
                            )
                            Text(
                                text = "Green ${analytics.greenCount} · Yellow ${analytics.yellowCount} · Orange ${analytics.orangeCount} · Red ${analytics.redCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item { Text("Department Trends", style = MaterialTheme.typography.titleMedium) }
                items(analytics.departments, key = { it.departmentName }) { dept ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(dept.departmentName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("n = ${dept.memberCount}", style = MaterialTheme.typography.labelMedium)
                            }
                            if (dept.isSuppressedForPrivacy) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Suppressed (group size < ${DepartmentAggregate.MIN_GROUP_SIZE}) to protect member anonymity.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            } else {
                                Text(
                                    text = "Participation ${dept.participationPercent}% · Wellbeing ${String.format("%.1f", dept.avgWellbeing)} · Stress ${String.format("%.1f", dept.avgStress)} · Sleep ${String.format("%.1f", dept.avgSleepHours)}h · Elevated Risk ${dept.highRiskSharePercent}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // Health Coordinator Dashboard
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))) {
                        Text(
                            "Permission-Based Coordinator Queue: Only members who explicitly granted outreach consent display contact workflows.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                items(coordinatorCases, key = { it.caseId }) { itemCase ->
                    CoordinatorCaseCard(
                        itemCase = itemCase,
                        strings = strings,
                        onSaveNote = { note -> onRecordFollowUpNote(itemCase.caseId, note) }
                    )
                }
            }

            2 -> {
                // Platform Admin & Immutable Audit Logs
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Platform Governance & AI Telemetry", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                OrgKpi("AI Queries", "$aiCallsCount")
                                OrgKpi("Audit Events", "${auditLogs.size}")
                                OrgKpi("RBAC", "Enforced")
                            }
                            Text(
                                "Firestore zero-trust rules isolate all personal health records under /users/{uid}/ with immutable append-only audit trails.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item { Text("Recent Audit Log Trail", style = MaterialTheme.typography.titleMedium) }
                items(auditLogs, key = { it.id.ifBlank { "${it.action}_${it.details.hashCode()}" } }) { log ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(log.action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(log.actorRole, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(log.details, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun CoordinatorCaseCard(
    itemCase: CoordinatorOutreachCase,
    strings: LocalizedStrings,
    onSaveNote: (String) -> Unit
) {
    var draftNote by remember(itemCase.caseId) { mutableStateOf("") }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${itemCase.caseId} · ${itemCase.anonymizedAlias}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                RiskBadge(level = itemCase.riskLevel, strings = strings)
            }
            Text("${itemCase.department} · Last check-in ${itemCase.lastCheckInDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(itemCase.triggerSummary, style = MaterialTheme.typography.bodySmall)
            Text("Status: ${itemCase.status} — ${itemCase.outreachNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

            if (itemCase.consentGranted) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draftNote,
                        onValueChange = { draftNote = it },
                        label = { Text("Record outreach note") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("coordinator_note_${itemCase.caseId}")
                    )
                    Button(
                        onClick = {
                            onSaveNote(draftNote)
                            draftNote = ""
                        },
                        enabled = draftNote.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun OrgKpi(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RiskDistributionStackedBar(green: Int, yellow: Int, orange: Int, red: Int) {
    val total = (green + yellow + orange + red).coerceAtLeast(1).toFloat()
    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
        val segments = listOf(
            green / total to RiskGreenFg,
            yellow / total to RiskYellowFg,
            orange / total to RiskOrangeFg,
            red / total to RiskRedFg
        )
        var x = 0f
        segments.forEach { (fraction, color) ->
            val segW = size.width * fraction
            if (segW > 0f) {
                drawRect(color = color, topLeft = Offset(x, 0f), size = Size(segW, size.height))
                x += segW
            }
        }
    }
}

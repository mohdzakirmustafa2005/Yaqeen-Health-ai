package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.GroundingSource
import com.example.data.StructuredAiResponseParser
import com.example.engine.HeartRiskEducationEngine
import com.example.model.AiHealthInsightRecord
import com.example.model.BloodPressureRecord
import com.example.model.DoctorAiMessage
import com.example.model.EmergencyScreenResult
import com.example.model.HeartMeasurement
import com.example.model.HeartSymptomReport
import com.example.model.StructuredAiResponse
import com.example.ui.components.BloodPressureDualChart
import com.example.ui.components.DataSourceLabelChip
import com.example.ui.components.EmergencySafetyBannerCard
import com.example.ui.components.HeartRateTrendChart
import com.example.ui.components.NonDiagnosticBanner
import com.example.ui.components.StructuredClinicalResponseCard

@Composable
fun HeartHomeScreen(
    measurements: List<HeartMeasurement>,
    bpRecords: List<BloodPressureRecord>,
    emergencyBanner: EmergencyScreenResult?,
    onOpenHeartChat: () -> Unit,
    onOpenHeartDashboard: () -> Unit,
    onOpenMeasurements: () -> Unit,
    onOpenSymptomChecker: () -> Unit,
    onOpenReportExplainer: () -> Unit,
    onOpenAppointmentPrep: () -> Unit,
    onDismissEmergency: () -> Unit
) {
    val latest = measurements.firstOrNull()
    val latestBp = bpRecords.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        if (emergencyBanner?.triggered == true) {
            item { EmergencySafetyBannerCard(banner = emergencyBanner, onDismiss = onDismissEmergency) }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MonitorHeart, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "YAQEEN AI CARDIOLOGY ASSISTANT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Cardiovascular health education, vital trend tracking, and emergency red-flag screening.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onOpenHeartChat,
                            modifier = Modifier.weight(1f).testTag("heart_home_open_chat_button")
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Heart Chat")
                        }
                        OutlinedButton(
                            onClick = onOpenSymptomChecker,
                            modifier = Modifier.weight(1f).testTag("heart_home_symptom_checker_button")
                        ) {
                            Icon(Icons.Filled.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Symptom Check")
                        }
                    }
                }
            }
        }

        item { NonDiagnosticBanner(text = StructuredAiResponse.HEART_MODE_DISCLAIMER) }

        // Latest User-Entered Vitals Card (no fake clinical score!)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Latest Recorded Heart Snapshot", style = MaterialTheme.typography.titleSmall)
                        DataSourceLabelChip(sourceLabel = latest?.sourceLabel ?: "USER_ENTERED")
                    }
                    if (latest == null && latestBp == null) {
                        Text(
                            "No user-entered heart readings yet. Log heart rate, blood pressure, SpO2, or cholesterol to track trends.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            VitalStatTile("Resting HR", latest?.restingHeartRateBpm?.let { "$it bpm" } ?: "--")
                            VitalStatTile(
                                "Blood Pressure",
                                if (latestBp != null) "${latestBp.systolicMmHg}/${latestBp.diastolicMmHg}"
                                else if (latest?.systolicMmHg != null && latest.diastolicMmHg != null) "${latest.systolicMmHg}/${latest.diastolicMmHg}"
                                else "--"
                            )
                            VitalStatTile("SpO2", latest?.oxygenSaturationPct?.let { "$it%" } ?: "--")
                            VitalStatTile("Glucose", latest?.bloodGlucoseMgDl?.let { "${it.toInt()} mg/dL" } ?: "--")
                        }
                    }
                }
            }
        }

        // Hub Navigation Grid
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    modifier = Modifier.weight(1f).testTag("heart_home_dashboard_card").clickable(onClick = onOpenHeartDashboard)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Heart Dashboard", style = MaterialTheme.typography.titleSmall)
                        Text("Charts, observations & 10 risk topics", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedCard(
                    modifier = Modifier.weight(1f).testTag("heart_home_measurements_card").clickable(onClick = onOpenMeasurements)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.AddChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Log Vitals & History", style = MaterialTheme.typography.titleSmall)
                        Text("${measurements.size} readings logged", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    modifier = Modifier.weight(1f).testTag("heart_home_report_card").clickable(onClick = onOpenReportExplainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("ECG & Cardiac Labs", style = MaterialTheme.typography.titleSmall)
                        Text("Upload & explain report terms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedCard(
                    modifier = Modifier.weight(1f).testTag("heart_home_appt_card").clickable(onClick = onOpenAppointmentPrep)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Cardiology Visit Prep", style = MaterialTheme.typography.titleSmall)
                        Text("Organize meds & questions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun VitalStatTile(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HeartChatScreen(
    messages: List<DoctorAiMessage>,
    isBusy: Boolean,
    errorMessage: String?,
    emergencyBanner: EmergencyScreenResult?,
    groundingSourcesByMessage: Map<String, List<GroundingSource>>,
    onSendMessage: (String) -> Unit,
    onNewChat: () -> Unit,
    onRetry: () -> Unit,
    onFeedback: (String, Long) -> Unit,
    onDismissEmergency: () -> Unit,
    onOpenSymptomChecker: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val cardiologyStarters = remember {
        listOf(
            "Explain how resting blood pressure is measured accurately at home",
            "Why can stress cause temporary palpitations?",
            "Help me understand LDL vs HDL cholesterol units",
            "What warning signs always require urgent emergency care?"
        )
    }

    LaunchedEffect(messages.size, isBusy) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("YAQEEN AI CARDIOLOGY ASSISTANT", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Cardiovascular Education & Support", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onNewChat, modifier = Modifier.testTag("heart_new_chat_button")) {
                Icon(Icons.Filled.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Chat")
            }
        }

        NonDiagnosticBanner(text = StructuredAiResponse.HEART_MODE_DISCLAIMER)
        Spacer(Modifier.height(6.dp))

        if (emergencyBanner?.triggered == true) {
            EmergencySafetyBannerCard(banner = emergencyBanner, onDismiss = onDismissEmergency)
            Spacer(Modifier.height(6.dp))
        }

        if (errorMessage != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(10.dp)) {
                    Text(errorMessage, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 6.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Cardiology Education Assistant", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Ask about blood pressure, resting pulse, lipid panels, ECG terminology, or lifestyle prevention. Urgent chest pain or shortness of breath immediately triggers emergency 112 guidance.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = onOpenSymptomChecker) {
                                Text("Run Heart Symptom Safety Screening")
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id.ifBlank { "${it.sender}_${it.text.hashCode()}" } }) { msg ->
                when (msg.sender) {
                    "USER" -> {
                        Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                            Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.medium, modifier = Modifier.widthIn(max = 520.dp)) {
                                Text(msg.text, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                    "SYSTEM_EMERGENCY" -> {
                        EmergencySafetyBannerCard(banner = EmergencyScreenResult(triggered = true, guidance = msg.text))
                    }
                    else -> {
                        val parsed = msg.structuredJson?.let {
                            StructuredAiResponseParser.parseAndValidate(
                                rawJson = it,
                                fallbackMode = msg.mode,
                                fallbackDisclaimer = StructuredAiResponse.HEART_MODE_DISCLAIMER,
                                providerName = msg.providerUsed
                            )
                        }
                        if (parsed != null) {
                            StructuredClinicalResponseCard(
                                structured = parsed,
                                groundingSources = groundingSourcesByMessage[msg.id].orEmpty(),
                                currentFeedback = msg.feedback,
                                onFollowUpClick = { q -> if (!isBusy) onSendMessage(q) },
                                onFeedbackClick = { score -> if (msg.id.isNotBlank()) onFeedback(msg.id, score) },
                                onRetry = onRetry
                            )
                        } else {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                                Text(msg.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }

            if (isBusy) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Screening emergency signals & reviewing cardiology context...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        ) {
            cardiologyStarters.forEach { q ->
                AssistChip(onClick = { if (!isBusy) onSendMessage(q) }, label = { Text(q, maxLines = 1, style = MaterialTheme.typography.labelSmall) })
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask AI Heart Specialist about BP, ECG terms, lipids...") },
                maxLines = 3,
                modifier = Modifier.weight(1f).testTag("heart_chat_input")
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty() && !isBusy) {
                        input = ""
                        onSendMessage(text)
                    }
                },
                enabled = input.isNotBlank() && !isBusy,
                modifier = Modifier.size(48.dp).testTag("heart_chat_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send cardiology question", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun HeartDashboardScreen(
    measurements: List<HeartMeasurement>,
    bpRecords: List<BloodPressureRecord>,
    symptomReports: List<HeartSymptomReport>,
    aiInsights: List<AiHealthInsightRecord>,
    onGenerateHeartSuggestions: () -> Unit,
    onExportHeartReportText: () -> String,
    onOpenAppointmentPrep: () -> Unit,
    onOpenMeasurements: () -> Unit
) {
    val context = LocalContext.current
    var expandedTopicId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            Text("Heart Health Dashboard", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Strictly separates user-entered readings, AI educational observations, general education, and physician-only clinical diagnosis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Clinical Clarity Card
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Information Boundaries", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("1. Recorded Measurements: Values you manually entered (or labeled Fictional Demo Data).", style = MaterialTheme.typography.bodySmall)
                    Text("2. AI Observations: Non-diagnostic educational patterns.", style = MaterialTheme.typography.bodySmall)
                    Text("3. Clinical Diagnoses: Provided ONLY by licensed physicians — no synthetic heart risk score is computed.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { HeartRateTrendChart(measurements = measurements) }
        item { BloodPressureDualChart(records = bpRecords) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onGenerateHeartSuggestions,
                    modifier = Modifier.weight(1f).testTag("heart_dashboard_generate_suggestions_button")
                ) {
                    Text("Safe Heart Plan", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = {
                        val txt = onExportHeartReportText()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Yaqeen Cardiology Summary Report")
                            putExtra(Intent.EXTRA_TEXT, txt)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Heart Report"))
                    },
                    modifier = Modifier.weight(1f).testTag("heart_dashboard_export_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpenMeasurements, modifier = Modifier.weight(1f)) {
                    Text("Add Measurement")
                }
                OutlinedButton(onClick = onOpenAppointmentPrep, modifier = Modifier.weight(1f)) {
                    Text("Appointment Prep")
                }
            }
        }

        if (aiInsights.isNotEmpty()) {
            item { Text("Recent AI Observations (Non-Diagnostic)", style = MaterialTheme.typography.titleMedium) }
            items(aiInsights.take(3), key = { it.id.ifBlank { it.title } }) { ins ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ins.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(ins.summary, style = MaterialTheme.typography.bodySmall)
                        Text("Provider: ${ins.providerUsed}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (symptomReports.isNotEmpty()) {
            item { Text("Recent Symptom Checks (${symptomReports.size})", style = MaterialTheme.typography.titleMedium) }
            items(symptomReports.take(3), key = { it.id.ifBlank { it.screeningReason } }) { s ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Symptoms: ${s.symptoms.joinToString(", ")} (Severity ${s.severityScore}/10)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(s.aiSummary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { Text("10 Cardiovascular Risk-Factor Education Guides", style = MaterialTheme.typography.titleMedium) }
        items(HeartRiskEducationEngine.educationalRiskFactors, key = { it.id }) { topic ->
            val expanded = expandedTopicId == topic.id
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth().clickable { expandedTopicId = if (expanded) null else topic.id }
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(topic.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = "Toggle details")
                    }
                    Text(topic.summary, style = MaterialTheme.typography.bodySmall)
                    if (expanded) {
                        Text("Preventive Focus: ${topic.preventiveFocus}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text("Discuss With Clinician: ${topic.discussWithClinician}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

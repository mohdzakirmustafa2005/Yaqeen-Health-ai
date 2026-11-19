package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.HealthCheckIn
import com.example.model.MoodOption
import com.example.ui.components.NonDiagnosticBanner
import com.example.ui.components.RiskBadge
import com.example.viewmodel.CheckInSubmissionOutcome
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInScreen(
    strings: LocalizedStrings,
    isSubmitting: Boolean,
    outcome: CheckInSubmissionOutcome?,
    onSubmitCheckIn: (HealthCheckIn) -> Unit,
    onDismissOutcome: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToAssistant: () -> Unit
) {
    var wellbeing by remember { mutableFloatStateOf(7f) }
    var sleepQuality by remember { mutableFloatStateOf(7f) }
    var sleepHours by remember { mutableFloatStateOf(7.0f) }
    var stress by remember { mutableFloatStateOf(4f) }
    var workStress by remember { mutableFloatStateOf(5f) }
    var energy by remember { mutableFloatStateOf(7f) }
    var fatigue by remember { mutableFloatStateOf(4f) }
    var hydration by remember { mutableFloatStateOf(2.0f) }
    var activityMinutes by remember { mutableFloatStateOf(30f) }
    var selectedMood by remember { mutableStateOf(MoodOption.CALM) }
    var selectedSymptoms by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }

    val symptomChoices = listOf(
        "Mild headache", "Eye strain", "Neck/shoulder tension",
        "Sore throat", "Stomach discomfort", "Restless sleep",
        "Severe dizziness", "Chest discomfort", "Shortness of breath"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(strings.startCheckIn, style = MaterialTheme.typography.headlineSmall)
        NonDiagnosticBanner(text = strings.nonDiagnosticBanner)

        // Post-submission outcome card showing Risk Analysis + AI Insight immediately
        if (outcome != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().testTag("checkin_outcome_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check-In Saved & Analyzed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    RiskBadge(level = outcome.evaluation.riskLevel, strings = strings)
                    Text(outcome.evaluation.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Triggers: ${outcome.evaluation.triggerFactors.joinToString(" · ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (outcome.fallbackUsed) "Safe Rule-Based Insight" else "Gemini AI Summary",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Text(outcome.aiSummary, style = MaterialTheme.typography.bodyMedium)
                            if (outcome.aiNotice != null) {
                                Text(outcome.aiNotice, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                onDismissOutcome()
                                onNavigateToDashboard()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View Trends")
                        }
                        OutlinedButton(
                            onClick = {
                                onDismissOutcome()
                                onNavigateToAssistant()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ask AI Guide")
                        }
                    }
                }
            }
        }

        // Core Wellbeing & Sleep Sliders
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Wellbeing, Energy & Sleep", style = MaterialTheme.typography.titleMedium)
                MetricSlider("Overall Wellbeing", "${wellbeing.roundToInt()}/10", wellbeing, 1f..10f) { wellbeing = it }
                MetricSlider("Sleep Quality", "${sleepQuality.roundToInt()}/10", sleepQuality, 1f..10f) { sleepQuality = it }
                MetricSlider("Sleep Duration", String.format("%.1f hrs", sleepHours), sleepHours, 2f..12f) { sleepHours = it }
                MetricSlider("Energy Level", "${energy.roundToInt()}/10", energy, 1f..10f) { energy = it }
            }
        }

        // Stress & Fatigue Sliders
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Stress, Workload & Fatigue", style = MaterialTheme.typography.titleMedium)
                MetricSlider("Stress Level", "${stress.roundToInt()}/10", stress, 1f..10f) { stress = it }
                MetricSlider("Work-Related Stress", "${workStress.roundToInt()}/10", workStress, 1f..10f) { workStress = it }
                MetricSlider("Fatigue Level", "${fatigue.roundToInt()}/10", fatigue, 1f..10f) { fatigue = it }
            }
        }

        // Lifestyle (Hydration + Physical Activity)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Lifestyle & Recovery", style = MaterialTheme.typography.titleMedium)
                MetricSlider("Hydration", String.format("%.1f L", hydration), hydration, 0.2f..4.5f) { hydration = it }
                MetricSlider("Physical Activity", "${activityMinutes.roundToInt()} min", activityMinutes, 0f..180f) { activityMinutes = it }
            }
        }

        // Mood & Physical Symptoms
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Mood", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MoodOption.entries.forEach { mood ->
                        FilterChip(
                            selected = selectedMood == mood,
                            onClick = { selectedMood = mood },
                            label = { Text(mood.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Physical Symptoms (Optional)", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    symptomChoices.forEach { sym ->
                        val active = sym in selectedSymptoms
                        FilterChip(
                            selected = active,
                            onClick = {
                                selectedSymptoms = if (active) selectedSymptoms - sym else selectedSymptoms + sym
                            },
                            label = { Text(sym) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 500) notes = it },
                    label = { Text("Optional Notes (sleep, workload, symptoms)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("checkin_notes_input")
                )
            }
        }

        Button(
            onClick = {
                onSubmitCheckIn(
                    HealthCheckIn(
                        overallWellbeing = wellbeing.roundToLong(),
                        sleepQuality = sleepQuality.roundToLong(),
                        sleepHours = (sleepHours * 10.0).roundToInt() / 10.0,
                        stressLevel = stress.roundToLong(),
                        energyLevel = energy.roundToLong(),
                        fatigueLevel = fatigue.roundToLong(),
                        mood = selectedMood.name,
                        symptoms = selectedSymptoms.toList(),
                        workStress = workStress.roundToLong(),
                        hydrationLiters = (hydration * 10.0).roundToInt() / 10.0,
                        activityMinutes = activityMinutes.roundToLong(),
                        notes = notes.trim().takeIf { it.isNotBlank() }
                    )
                )
            },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_checkin_button")
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Analyzing Wellness Trends...")
            } else {
                Text(strings.submitCheckIn)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun MetricSlider(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range
        )
    }
}

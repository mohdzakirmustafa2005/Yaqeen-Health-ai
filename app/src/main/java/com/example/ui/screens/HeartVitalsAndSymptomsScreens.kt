package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ClinicalAiInvocationResult
import com.example.engine.SafetyScreeningEngine
import com.example.model.EmergencyScreenResult
import com.example.model.HeartMeasurement
import com.example.model.HeartSymptomReport
import com.example.ui.components.DataSourceLabelChip
import com.example.ui.components.EmergencySafetyBannerCard
import com.example.ui.components.NonDiagnosticBanner
import com.example.ui.components.StructuredClinicalResponseCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeartMeasurementScreen(
    measurements: List<HeartMeasurement>,
    emergencyBanner: EmergencyScreenResult?,
    onSaveMeasurement: (HeartMeasurement) -> Unit,
    onExportHeartReportText: () -> String,
    onDismissEmergency: () -> Unit
) {
    val context = LocalContext.current
    val nowDefault = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()) }
    var recordedAtIso by remember { mutableStateOf(nowDefault) }
    var hrText by remember { mutableStateOf("") }
    var sysText by remember { mutableStateOf("") }
    var diaText by remember { mutableStateOf("") }
    var spo2Text by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var glucoseText by remember { mutableStateOf("") }
    var totalCholText by remember { mutableStateOf("") }
    var ldlText by remember { mutableStateOf("") }
    var hdlText by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf("RESTING") }
    var notes by remember { mutableStateOf("") }
    val selectedSymptoms = remember { mutableStateListOf<String>() }

    val activityOptions = listOf("RESTING", "LIGHT_ACTIVITY", "POST_EXERCISE", "MODERATE", "VIGOROUS")
    val quickSymptoms = listOf("Palpitations", "Mild fatigue", "Dizziness", "Chest discomfort", "Shortness of breath")

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            Text("Heart Measurement & History", style = MaterialTheme.typography.headlineSmall)
            Text(
                "All manually entered metrics are tagged 'User-Entered Data' and automatically screened for urgent thresholds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (emergencyBanner?.triggered == true) {
            item { EmergencySafetyBannerCard(banner = emergencyBanner, onDismiss = onDismissEmergency) }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Log Heart & Metabolic Readings", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = recordedAtIso,
                        onValueChange = { recordedAtIso = it },
                        label = { Text("Date & Time (YYYY-MM-DD HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = hrText,
                            onValueChange = { hrText = it },
                            label = { Text("Resting HR (bpm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("measurement_hr_input")
                        )
                        OutlinedTextField(
                            value = spo2Text,
                            onValueChange = { spo2Text = it },
                            label = { Text("SpO2 (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("measurement_spo2_input")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = sysText,
                            onValueChange = { sysText = it },
                            label = { Text("Systolic BP (mmHg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("measurement_sys_input")
                        )
                        OutlinedTextField(
                            value = diaText,
                            onValueChange = { diaText = it },
                            label = { Text("Diastolic BP (mmHg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("measurement_dia_input")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = glucoseText,
                            onValueChange = { glucoseText = it },
                            label = { Text("Glucose (mg/dL)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = totalCholText,
                            onValueChange = { totalCholText = it },
                            label = { Text("Total Chol") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ldlText,
                            onValueChange = { ldlText = it },
                            label = { Text("LDL") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = hdlText,
                            onValueChange = { hdlText = it },
                            label = { Text("HDL") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Activity Context", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        activityOptions.forEach { act ->
                            FilterChip(
                                selected = activityLevel == act,
                                onClick = { activityLevel = act },
                                label = { Text(act.replace("_", " "), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text("Optional Symptoms Present", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickSymptoms.forEach { sym ->
                            val checked = sym in selectedSymptoms
                            FilterChip(
                                selected = checked,
                                onClick = {
                                    if (checked) selectedSymptoms.remove(sym) else selectedSymptoms.add(sym)
                                },
                                label = { Text(sym, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (e.g., before coffee, after sitting 5 min)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val hasAnyValue = hrText.isNotBlank() || sysText.isNotBlank() || spo2Text.isNotBlank() ||
                        weightText.isNotBlank() || glucoseText.isNotBlank() || totalCholText.isNotBlank()

                    Button(
                        onClick = {
                            onSaveMeasurement(
                                HeartMeasurement(
                                    recordedAtIso = recordedAtIso.ifBlank { nowDefault },
                                    restingHeartRateBpm = hrText.trim().toLongOrNull(),
                                    systolicMmHg = sysText.trim().toLongOrNull(),
                                    diastolicMmHg = diaText.trim().toLongOrNull(),
                                    oxygenSaturationPct = spo2Text.trim().toLongOrNull(),
                                    weightKg = weightText.trim().toDoubleOrNull(),
                                    bloodGlucoseMgDl = glucoseText.trim().toDoubleOrNull(),
                                    totalCholesterolMgDl = totalCholText.trim().toDoubleOrNull(),
                                    ldlCholesterolMgDl = ldlText.trim().toDoubleOrNull(),
                                    hdlCholesterolMgDl = hdlText.trim().toDoubleOrNull(),
                                    symptoms = selectedSymptoms.toList(),
                                    activityLevel = activityLevel,
                                    notes = notes.trim().takeIf { it.isNotEmpty() }
                                )
                            )
                            hrText = ""; sysText = ""; diaText = ""; spo2Text = ""; notes = ""; selectedSymptoms.clear()
                        },
                        enabled = hasAnyValue,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_heart_measurement_button")
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Heart Measurement")
                    }
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Measurement History (${measurements.size})", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = {
                        val txt = onExportHeartReportText()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Yaqeen Heart Measurement Report")
                            putExtra(Intent.EXTRA_TEXT, txt)
                        }
                        context.startActivity(Intent.createChooser(intent, "Export Heart Report"))
                    }
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export")
                }
            }
        }

        items(measurements, key = { it.id.ifBlank { it.recordedAtIso + it.hashCode() } }) { m ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(m.recordedAtIso, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        DataSourceLabelChip(sourceLabel = m.sourceLabel)
                    }
                    val metricsLine = buildList {
                        m.restingHeartRateBpm?.let { add("HR: $it bpm") }
                        if (m.systolicMmHg != null && m.diastolicMmHg != null) add("BP: ${m.systolicMmHg}/${m.diastolicMmHg} mmHg")
                        m.oxygenSaturationPct?.let { add("SpO2: $it%") }
                        m.weightKg?.let { add("Weight: $it kg") }
                        m.bloodGlucoseMgDl?.let { add("Glucose: $it mg/dL") }
                        m.totalCholesterolMgDl?.let { add("Chol: $it mg/dL") }
                    }.joinToString(" · ")
                    Text(metricsLine.ifBlank { "Vital note logged" }, style = MaterialTheme.typography.bodyMedium)
                    if (m.symptoms.isNotEmpty()) {
                        Text("Symptoms: ${m.symptoms.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (!m.notes.isNullOrBlank()) {
                        Text(m.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeartSymptomCheckerScreen(
    isBusy: Boolean,
    emergencyBanner: EmergencyScreenResult?,
    lastResult: ClinicalAiInvocationResult?,
    symptomReports: List<HeartSymptomReport>,
    onSubmitCheck: (
        symptoms: List<String>,
        severity: Long,
        duration: String,
        associated: List<String>,
        notes: String
    ) -> Unit,
    onDismissEmergency: () -> Unit
) {
    val allSymptoms = listOf(
        "Chest pain",
        "Chest pressure or tightness",
        "Shortness of breath",
        "Fainting or near-fainting",
        "Sudden weakness or numbness",
        "Confusion or slurred speech",
        "Severe dizziness",
        "Palpitations / racing heart",
        "Mild fatigue",
        "Swelling in ankles"
    )
    val associatedOptions = listOf("Cold sweat", "Nausea", "Jaw or left-arm ache", "Poor sleep (<6h)", "High work stress")
    val chosenSymptoms = remember { mutableStateListOf<String>() }
    val chosenAssoc = remember { mutableStateListOf<String>() }
    var severity by remember { mutableFloatStateOf(3f) }
    var duration by remember { mutableStateOf("Started today") }
    var notes by remember { mutableStateOf("") }

    val livePreviewScreen = remember(chosenSymptoms.toList(), notes) {
        SafetyScreeningEngine.screenTextAndSymptoms(notes, chosenSymptoms.toList())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            Text("Heart Symptom & Red-Flag Checker", style = MaterialTheme.typography.headlineSmall)
            NonDiagnosticBanner(text = "Safety screening runs automatically BEFORE AI. Chest pain, pressure, breathlessness, fainting, or stroke signs immediately urge emergency 112 care and halt fitness advice.")
        }

        if (livePreviewScreen.triggered || emergencyBanner?.triggered == true) {
            item {
                EmergencySafetyBannerCard(
                    banner = emergencyBanner?.takeIf { it.triggered } ?: livePreviewScreen,
                    onDismiss = if (emergencyBanner?.triggered == true) onDismissEmergency else null
                )
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Symptoms You Are Experiencing", style = MaterialTheme.typography.titleMedium)
                    allSymptoms.forEach { sym ->
                        val checked = sym in chosenSymptoms
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { if (checked) chosenSymptoms.remove(sym) else chosenSymptoms.add(sym) }
                                .testTag("symptom_checkbox_${sym.lowercase().replace(" ", "_")}")
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(sym, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Text("Severity Score: ${severity.toInt()} / 10", style = MaterialTheme.typography.labelLarge)
                    Slider(value = severity, onValueChange = { severity = it }, valueRange = 1f..10f, steps = 8)

                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration (e.g., 20 minutes, 2 days)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Associated Factors", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        associatedOptions.forEach { assoc ->
                            val c = assoc in chosenAssoc
                            FilterChip(
                                selected = c,
                                onClick = { if (c) chosenAssoc.remove(assoc) else chosenAssoc.add(assoc) },
                                label = { Text(assoc, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Describe what you feel & when it started") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("symptom_notes_input")
                    )

                    Button(
                        onClick = {
                            onSubmitCheck(
                                chosenSymptoms.toList(),
                                severity.toLong(),
                                duration.ifBlank { "Not specified" },
                                chosenAssoc.toList(),
                                notes
                            )
                        },
                        enabled = !isBusy && (chosenSymptoms.isNotEmpty() || notes.isNotBlank()),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("run_symptom_safety_check_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Screening...")
                        } else {
                            Icon(Icons.Filled.HealthAndSafety, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Run Safety & Symptom Check")
                        }
                    }
                }
            }
        }

        lastResult?.response?.let { resp ->
            if (!resp.emergencyScreen.triggered) {
                item {
                    StructuredClinicalResponseCard(structured = resp, groundingSources = lastResult.groundingSources)
                }
            }
        }

        if (symptomReports.isNotEmpty()) {
            item { Text("Past Symptom Screenings (${symptomReports.size})", style = MaterialTheme.typography.titleMedium) }
            items(symptomReports.take(5), key = { it.id.ifBlank { it.screeningReason + it.hashCode() } }) { rep ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "[${rep.urgencyLevel.uppercase()}] ${rep.symptoms.joinToString(", ")}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(rep.aiSummary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ClinicalAiInvocationResult
import com.example.engine.DemoDataFactory
import com.example.model.HealthReportUpload
import com.example.model.HeartHealthProfile
import com.example.model.HeartMeasurement
import com.example.model.StructuredAiResponse
import com.example.ui.components.NonDiagnosticBanner
import com.example.ui.components.StructuredClinicalResponseCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportExplainerScreen(
    isBusy: Boolean,
    lastResult: ClinicalAiInvocationResult?,
    uploads: List<HealthReportUpload>,
    onAnalyzeReport: (
        title: String,
        reportType: String,
        fileName: String?,
        rawText: String,
        bitmap: Bitmap?
    ) -> Unit
) {
    val context = LocalContext.current
    val reportTypes = listOf(
        "GENERAL_LAB" to "General Lab",
        "ECG_REPORT" to "ECG Report",
        "CARDIAC_LAB" to "Cardiac / Lipid",
        "BLOOD_PRESSURE_LOG" to "BP Log",
        "OTHER" to "Other"
    )

    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("GENERAL_LAB") }
    var fileName by remember { mutableStateOf<String?>(null) }
    var reportText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Zero-permission Photo Picker for lab/ECG photos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    attachedBitmap = BitmapFactory.decodeStream(stream, null, opts)
                }
                fileName = resolveDisplayName(context.contentResolver, uri) ?: "report_photo.jpg"
                if (title.isBlank()) title = "Uploaded Report Photo"
            }
        }
    }

    // Text/CSV lab file picker via SAF OpenDocument (no broad storage permissions)
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    reportText = reader.readText().take(4800)
                }
                fileName = resolveDisplayName(context.contentResolver, uri) ?: "report.txt"
                if (title.isBlank()) title = fileName.orEmpty().substringBeforeLast('.')
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            Text("Medical & Lab Report Explainer", style = MaterialTheme.typography.headlineSmall)
            NonDiagnosticBanner(
                text = "Explains terminology, preserves exact units, highlights out-of-range markers against printed ranges, and lists questions for your doctor. Never diagnoses."
            )
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Report Title (e.g., Annual Lipid Panel, Resting ECG)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("report_title_input")
                    )

                    Text("Report Type", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        reportTypes.forEach { (id, label) ->
                            FilterChip(
                                selected = selectedType == id,
                                onClick = { selectedType = id },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("report_type_${id.lowercase()}")
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("pick_report_photo_button")
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Photo")
                        }
                        OutlinedButton(
                            onClick = {
                                docPickerLauncher.launch(arrayOf("text/plain", "text/csv"))
                            },
                            modifier = Modifier.weight(1f).testTag("pick_report_doc_button")
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Text / CSV")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                title = "Fictional Demo ECG Report"
                                selectedType = "ECG_REPORT"
                                fileName = "demo_ecg.txt"
                                attachedBitmap = null
                                reportText = DemoDataFactory.sampleDemoEcgText
                            },
                            modifier = Modifier.weight(1f).testTag("load_demo_ecg_button")
                        ) {
                            Text("Demo ECG", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = {
                                title = "Fictional Demo Lipid & Glucose"
                                selectedType = "CARDIAC_LAB"
                                fileName = "demo_lipid.txt"
                                attachedBitmap = null
                                reportText = DemoDataFactory.sampleDemoLipidText
                            },
                            modifier = Modifier.weight(1f).testTag("load_demo_lipid_button")
                        ) {
                            Text("Demo Lipid Lab", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (fileName != null || attachedBitmap != null) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(10.dp)
                            ) {
                                Text(
                                    text = buildString {
                                        append("Attached: ${fileName ?: "Report"}")
                                        if (attachedBitmap != null) append(" (Image ready for multimodal AI)")
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reportText,
                        onValueChange = { reportText = it },
                        label = { Text("Paste report values, units, and printed reference ranges") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("report_text_input")
                    )

                    Button(
                        onClick = {
                            onAnalyzeReport(
                                title.ifBlank { "Health Report" },
                                selectedType,
                                fileName,
                                reportText,
                                attachedBitmap
                            )
                        },
                        enabled = !isBusy && (reportText.isNotBlank() || attachedBitmap != null),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("analyze_report_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Explaining Report...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Explain Terms & Ranges")
                        }
                    }
                }
            }
        }

        lastResult?.response?.let { resp ->
            item {
                StructuredClinicalResponseCard(structured = resp, groundingSources = lastResult.groundingSources)
            }
        }

        if (uploads.isNotEmpty()) {
            item { Text("Saved Explained Reports (${uploads.size})", style = MaterialTheme.typography.titleMedium) }
            items(uploads, key = { it.id.ifBlank { it.title + it.hashCode() } }) { rep ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(rep.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text("${rep.reportType} · Provider: ${rep.providerUsed}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(rep.summary, style = MaterialTheme.typography.bodySmall)
                        if (rep.outOfRangeFindings.isNotEmpty()) {
                            Text("Highlighted Observations:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            rep.outOfRangeFindings.forEach { f -> Text("• $f", style = MaterialTheme.typography.bodySmall) }
                        }
                        if (rep.questionsForDoctor.isNotEmpty()) {
                            Text("Questions for Your Doctor:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            rep.questionsForDoctor.forEach { q -> Text("• $q", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun resolveDisplayName(contentResolver: android.content.ContentResolver, uri: Uri): String? {
    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppointmentPrepScreen(
    isBusy: Boolean,
    heartProfile: HeartHealthProfile?,
    measurements: List<HeartMeasurement>,
    prepResult: StructuredAiResponse?,
    onSaveMedicationsAndAllergies: (HeartHealthProfile) -> Unit,
    onGenerateChecklist: (clinicianType: String, focusNotes: String) -> Unit,
    onExportHeartSummary: () -> String
) {
    val context = LocalContext.current
    val clinicians = listOf(
        "Primary Care Physician",
        "Cardiologist",
        "Endocrinologist",
        "Counselor / Psychiatrist",
        "Neurologist"
    )
    var selectedClinician by remember { mutableStateOf("Primary Care Physician") }
    var medicationsText by remember(heartProfile?.medications) {
        mutableStateOf(heartProfile?.medications?.joinToString(", ").orEmpty())
    }
    var allergiesText by remember(heartProfile?.allergies) {
        mutableStateOf(heartProfile?.allergies?.joinToString(", ").orEmpty())
    }
    var focusNotes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            Text("Medical Appointment Preparation", style = MaterialTheme.typography.headlineSmall)
            NonDiagnosticBanner(text = "Organizes your user-entered vitals, medications, allergies, and thoughtful clinician questions so visits are clear and productive.")
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Select Clinician Specialty", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        clinicians.forEach { doc ->
                            FilterChip(
                                selected = selectedClinician == doc,
                                onClick = { selectedClinician = doc },
                                label = { Text(doc, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text("2. Current Medications & Allergies Summary", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = medicationsText,
                        onValueChange = { medicationsText = it },
                        label = { Text("Medications / Supplements (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = allergiesText,
                        onValueChange = { allergiesText = it },
                        label = { Text("Medication / Food Allergies (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            val base = heartProfile ?: HeartHealthProfile()
                            onSaveMedicationsAndAllergies(
                                base.copy(
                                    medications = medicationsText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                    allergies = allergiesText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Meds & Allergies Context")
                    }

                    OutlinedTextField(
                        value = focusNotes,
                        onValueChange = { focusNotes = it },
                        label = { Text("Main questions or symptoms to discuss") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("appt_focus_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onGenerateChecklist(selectedClinician, focusNotes) },
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("generate_appt_prep_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(6.dp))
                                Text("Preparing...")
                            } else {
                                Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Generate Checklist")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val text = buildString {
                                    appendLine(onExportHeartSummary())
                                    if (prepResult != null) {
                                        appendLine()
                                        appendLine("APPOINTMENT QUESTIONS FOR $selectedClinician")
                                        prepResult.recommendations.forEach { appendLine("- $it") }
                                        prepResult.followUpQuestions.forEach { appendLine("- $it") }
                                    }
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Visit Summary for $selectedClinician")
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Visit Summary"))
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share visit summary")
                        }
                    }
                }
            }
        }

        if (measurements.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Snapshot Ready for Visit (${measurements.size} vitals)", style = MaterialTheme.typography.titleSmall)
                        val latest = measurements.first()
                        Text(
                            "Latest (${latest.recordedAtIso}): HR ${latest.restingHeartRateBpm ?: "--"} bpm · BP ${latest.systolicMmHg ?: "--"}/${latest.diastolicMmHg ?: "--"} mmHg · SpO2 ${latest.oxygenSaturationPct ?: "--"}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (prepResult != null) {
            item {
                StructuredClinicalResponseCard(structured = prepResult)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

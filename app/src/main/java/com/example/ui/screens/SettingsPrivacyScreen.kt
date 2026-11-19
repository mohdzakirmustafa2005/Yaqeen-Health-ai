package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AiProviderHealth
import com.example.data.local.LocalSqlSummary
import com.example.localization.AppLanguage
import com.example.localization.LocalizedStrings
import com.example.model.HeartHealthProfile
import com.example.model.MedicalSafetyEvent
import com.example.model.UserConsentRecord
import com.example.model.UserProfile
import com.example.ui.components.NonDiagnosticBanner
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPrivacyScreen(
    strings: LocalizedStrings,
    language: AppLanguage,
    darkMode: Boolean,
    profile: UserProfile?,
    heartProfile: HeartHealthProfile? = null,
    latestConsent: UserConsentRecord? = null,
    safetyEvents: List<MedicalSafetyEvent> = emptyList(),
    providerStatuses: List<AiProviderHealth> = emptyList(),
    localSqlSummary: LocalSqlSummary?,
    isDemoMode: Boolean,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onSavePrivacy: (
        displayName: String,
        department: String,
        consentAnalytics: Boolean,
        consentAi: Boolean,
        consentOutreach: Boolean
    ) -> Unit,
    onSaveClinicalConsent: ((
        consentAnalytics: Boolean,
        consentAi: Boolean,
        consentHeartAi: Boolean,
        consentReportAnalysis: Boolean,
        consentOutreach: Boolean
    ) -> Unit)? = null,
    onSaveHeartProfile: ((HeartHealthProfile) -> Unit)? = null,
    onExportJson: () -> String,
    onDeleteAccount: () -> Unit,
    onSignOutOrExitDemo: () -> Unit
) {
    val context = LocalContext.current
    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "Team Member") }
    var department by remember(profile?.department) { mutableStateOf(profile?.department ?: "Operations") }
    var consentAnalytics by remember(profile?.consentAnalytics) { mutableStateOf(profile?.consentAnalytics ?: true) }
    var consentAi by remember(profile?.consentAiGuidance) { mutableStateOf(profile?.consentAiGuidance ?: true) }
    var consentHeartAi by remember(latestConsent?.consentHeartAiContext) { mutableStateOf(latestConsent?.consentHeartAiContext ?: true) }
    var consentReportAnalysis by remember(latestConsent?.consentReportAnalysis) { mutableStateOf(latestConsent?.consentReportAnalysis ?: true) }
    var consentOutreach by remember(profile?.consentCoordinatorOutreach) { mutableStateOf(profile?.consentCoordinatorOutreach ?: false) }

    var ageText by remember(heartProfile?.ageYears) { mutableStateOf(heartProfile?.ageYears?.toString().orEmpty()) }
    var conditionsText by remember(heartProfile?.knownConditions) { mutableStateOf(heartProfile?.knownConditions?.joinToString(", ").orEmpty()) }
    var smokingStatus by remember(heartProfile?.smokingStatus) { mutableStateOf(heartProfile?.smokingStatus ?: "NEVER") }
    var familyHistory by remember(heartProfile?.familyHeartHistory) { mutableStateOf(heartProfile?.familyHeartHistory ?: false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(strings.privacyAndSettings, style = MaterialTheme.typography.headlineSmall)
        NonDiagnosticBanner(text = strings.nonDiagnosticBanner)

        // Language & Appearance
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Language & RTL Support", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { onSelectLanguage(lang) },
                            label = { Text("${lang.nativeLabel} (${lang.code})") },
                            modifier = Modifier.testTag("settings_lang_${lang.code.lowercase()}")
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dark Theme", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = darkMode, onCheckedChange = onToggleDarkMode)
                }
            }
        }

        // Profile & Granular Consent
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Profile & Consent Management", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ConsentRow(
                    title = "Anonymized Aggregate Analytics",
                    subtitle = "Contribute de-identified scores to teams with size ≥ 5.",
                    checked = consentAnalytics,
                    onCheckedChange = { consentAnalytics = it }
                )
                ConsentRow(
                    title = "AI Doctor & Wellness Guidance",
                    subtitle = "Allow AI to summarize non-identifying check-in context.",
                    checked = consentAi,
                    onCheckedChange = { consentAi = it }
                )
                ConsentRow(
                    title = "Heart Metrics AI Context",
                    subtitle = "Allow de-identified BP, HR & meds context in Heart Specialist AI.",
                    checked = consentHeartAi,
                    onCheckedChange = { consentHeartAi = it }
                )
                ConsentRow(
                    title = "Medical Report & Lab Analysis",
                    subtitle = "Allow Report Explainer to process uploaded lab or ECG snippets.",
                    checked = consentReportAnalysis,
                    onCheckedChange = { consentReportAnalysis = it }
                )
                ConsentRow(
                    title = "Coordinator Outreach Permission",
                    subtitle = "Allow authorized health coordinator follow-up on Orange/Red alerts.",
                    checked = consentOutreach,
                    onCheckedChange = { consentOutreach = it }
                )

                Button(
                    onClick = {
                        onSavePrivacy(displayName, department, consentAnalytics, consentAi, consentOutreach)
                        onSaveClinicalConsent?.invoke(
                            consentAnalytics,
                            consentAi,
                            consentHeartAi,
                            consentReportAnalysis,
                            consentOutreach
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_privacy_button")
                ) {
                    Text("Save Privacy & Profile Preferences")
                }
            }
        }

        // Heart Health Baseline Profile
        if (onSaveHeartProfile != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cardiovascular & Clinical Context Profile", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it },
                        label = { Text("Age (years, optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = conditionsText,
                        onValueChange = { conditionsText = it },
                        label = { Text("Declared Conditions (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Smoking Status", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("NEVER", "FORMER", "CURRENT", "PREFER_NOT_TO_SAY").forEach { st ->
                            FilterChip(
                                selected = smokingStatus == st,
                                onClick = { smokingStatus = st },
                                label = { Text(st.replace("_", " "), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    ConsentRow(
                        title = "Family Cardiovascular History",
                        subtitle = "Close relative with early heart condition.",
                        checked = familyHistory,
                        onCheckedChange = { familyHistory = it }
                    )
                    OutlinedButton(
                        onClick = {
                            val base = heartProfile ?: HeartHealthProfile()
                            onSaveHeartProfile(
                                base.copy(
                                    ageYears = ageText.trim().toLongOrNull(),
                                    knownConditions = conditionsText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                    smokingStatus = smokingStatus,
                                    familyHeartHistory = familyHistory
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_heart_profile_button")
                    ) {
                        Text("Save Heart Health Context")
                    }
                }
            }
        }

        // Database & Local SQLite Cache Diagnostics
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cloud Sync, AI Providers & Local SQLite Store", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (isDemoMode) {
                        "Demo Mode active: in-memory state + local SQL statistics."
                    } else {
                        "Primary cloud sync: Firebase Firestore (owner-isolated). Offline SQL cache: Room SQLite (yaqeen_health_local.db)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val sqlCount = localSqlSummary?.totalCount ?: 0
                val heartSqlCount = localSqlSummary?.heartReadingsCount ?: 0
                val avgWb = localSqlSummary?.avgWellbeing?.let { String.format(Locale.US, "%.1f/10", it) } ?: "--"
                val avgSlp = localSqlSummary?.avgSleep?.let { String.format(Locale.US, "%.1fh", it) } ?: "--"
                Text(
                    "SQLite Check-Ins: $sqlCount • SQLite Heart Readings: $heartSqlCount • Avg Wellbeing: $avgWb • Avg Sleep: $avgSlp",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (providerStatuses.isNotEmpty()) {
                    providerStatuses.forEach { p ->
                        Text(
                            "• ${p.name} [${p.roleLabel}]: ${if (p.configured) "Ready" else "Not configured"} — ${p.detail}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (safetyEvents.isNotEmpty()) {
                    Text(
                        "Recent Safety Screening Events (${safetyEvents.size}): ${safetyEvents.first().sourceModule} — ${safetyEvents.first().reason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Data Portability & Help
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Data Rights & Support", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = {
                        val json = onExportJson()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "Yaqeen Health Personal Data Export")
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(intent, "Export Personal Data"))
                    },
                    modifier = Modifier.fillMaxWidth().testTag("export_json_button")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Personal Health Data (JSON)")
                }

                OutlinedButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Help, Safety Policy & Support")
                }

                OutlinedButton(
                    onClick = onSignOutOrExitDemo,
                    modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isDemoMode) "Exit Demo Mode" else "Sign Out")
                }

                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("delete_account_button")
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isDemoMode) "Reset Demo Data" else "Delete Account & Health Records")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Health Records?") },
            text = {
                Text("This permanently removes your stored check-ins, alerts, recommendations, and AI conversation history.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                    }
                ) {
                    Text("Confirm Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Help & Clinical Safety Policy") },
            text = {
                Text(
                    "Yaqeen Health (YAQEEN ALLAH WORKFORCE HEALTH AI) provides early wellness monitoring, burnout prevention, and non-diagnostic health education.\n\n" +
                        "• Not a medical device or diagnostic tool.\n" +
                        "• For emergencies (chest pain, difficulty breathing, severe dizziness), contact local emergency services immediately.\n" +
                        "• All personal records are owner-isolated in Firestore and never exposed individually to employers without explicit consent."
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ConsentRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

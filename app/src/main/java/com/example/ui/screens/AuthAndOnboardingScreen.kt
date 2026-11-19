package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.onGoogleSignInClicked
import com.example.localization.AppLanguage
import com.example.localization.LocalizedStrings
import com.example.model.UserRole
import com.example.ui.components.LanguageSelectorChip
import com.example.ui.components.NonDiagnosticBanner

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthAndLandingScreen(
    language: AppLanguage,
    strings: LocalizedStrings,
    onLanguageChange: (AppLanguage) -> Unit,
    onAuthSuccess: () -> Unit,
    onEnterDemoMode: (UserRole) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var selectedDemoRole by remember { mutableStateOf(UserRole.INDIVIDUAL) }

    Scaffold { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.HealthAndSafety,
                            contentDescription = "Yaqeen Health logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strings.appTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "YAQEEN ALLAH WORKFORCE HEALTH AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    LanguageSelectorChip(current = language, onSelect = onLanguageChange)
                }

                // Hero Illustration Card
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    Column {
                        Image(
                            painter = painterResource(id = R.drawable.img_wellness_hero),
                            contentDescription = "Workforce wellness early warning illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp)
                                .clip(MaterialTheme.shapes.large)
                        )
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = strings.tagline,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Monitor daily wellbeing, detect burnout & sleep deficits early, receive multilingual Gemini guidance, and protect workforce privacy.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                NonDiagnosticBanner(text = strings.nonDiagnosticBanner)

                // Key Platform Capabilities Highlights
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CapabilityChip(Icons.Filled.AutoAwesome, "Gemini Health Guide")
                    CapabilityChip(Icons.Filled.Analytics, "Early Warning Engine")
                    CapabilityChip(Icons.Filled.Lock, "Privacy-First k≥5 Analytics")
                    CapabilityChip(Icons.Filled.AdminPanelSettings, "4-Role Access Control")
                }

                // Google Sign-In Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Secure Cloud Workspace",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Sign in with Google to save private health check-ins, AI insights, and alerts to your encrypted Firestore profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (authError != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = authError.orEmpty(),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                authError = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        authError = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isLoading = false }
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.AccountCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.signInGoogle)
                            }
                        }
                    }
                }

                // Safe Judge Demo Mode Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Judge & Evaluator Preview (Fictional Data)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Explore the full interactive workflow—check-ins, trend intelligence, early warnings, AI assistant, and workforce dashboards—without creating an account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            UserRole.entries.forEach { role ->
                                FilterChip(
                                    selected = selectedDemoRole == role,
                                    onClick = { selectedDemoRole = role },
                                    label = { Text(role.label) },
                                    modifier = Modifier.testTag("demo_role_${role.name.lowercase()}")
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { onEnterDemoMode(selectedDemoRole) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("enter_demo_mode_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${strings.exploreDemo} (${selectedDemoRole.label})")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun OnboardingScreen(
    initialName: String,
    onComplete: (name: String, department: String, consentAnalytics: Boolean, consentAi: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var department by remember { mutableStateOf("Operations") }
    var consentAnalytics by remember { mutableStateOf(true) }
    var consentAi by remember { mutableStateOf(true) }
    val departments = listOf("Operations", "Engineering & Product", "Care & Support", "Field Logistics")

    Scaffold { innerPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Welcome to Yaqeen Health", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Set up your private wellness profile and review how your data is protected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
                )

                Text("Select Department", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    departments.forEach { dept ->
                        FilterChip(
                            selected = department == dept,
                            onClick = { department = dept },
                            label = { Text(dept) },
                            leadingIcon = if (department == dept) {
                                { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Privacy & Consent Controls", style = MaterialTheme.typography.titleMedium)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Anonymized Aggregate Analytics", style = MaterialTheme.typography.bodyMedium)
                                Text("Never shared individually; requires group size ≥ 5.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = consentAnalytics, onCheckedChange = { consentAnalytics = it })
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Wellness & Trend Guidance", style = MaterialTheme.typography.bodyMedium)
                                Text("Non-diagnostic explanations via Gemini.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = consentAi, onCheckedChange = { consentAi = it })
                        }
                    }
                }

                Button(
                    onClick = { onComplete(name, department, consentAnalytics, consentAi) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("complete_onboarding_button")
                ) {
                    Text("Continue to Health Dashboard")
                }
            }
        }
    }
}

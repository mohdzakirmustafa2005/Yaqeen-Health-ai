package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AiProviderHealth
import com.example.data.GroundingSource
import com.example.data.StructuredAiResponseParser
import com.example.model.AiDoctorMode
import com.example.model.DoctorAiMessage
import com.example.model.DoctorConversation
import com.example.model.DoctorDomain
import com.example.model.EmergencyScreenResult
import com.example.model.StructuredAiResponse
import com.example.ui.components.EmergencySafetyBannerCard
import com.example.ui.components.NonDiagnosticBanner
import com.example.ui.components.StructuredClinicalResponseCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoctorHomeScreen(
    providerStatuses: List<AiProviderHealth>,
    conversationsCount: Int,
    onOpenModeChat: (AiDoctorMode) -> Unit,
    onOpenReportExplainer: () -> Unit,
    onOpenHeartHome: () -> Unit,
    onOpenAppointmentPrep: () -> Unit,
    onOpenHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("AI DOCTOR", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Health information, personalized wellness guidance and care navigation.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onOpenModeChat(AiDoctorMode.GENERAL_HEALTH) },
                            modifier = Modifier.weight(1f).testTag("doctor_home_start_chat_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ask AI Doctor")
                        }
                        OutlinedButton(
                            onClick = onOpenHeartHome,
                            modifier = Modifier.weight(1f).testTag("doctor_home_open_heart_button")
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Heart Hub")
                        }
                    }
                }
            }
        }

        item {
            NonDiagnosticBanner(text = StructuredAiResponse.DOCTOR_MODE_DISCLAIMER)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    modifier = Modifier.weight(1f).testTag("doctor_home_report_explainer_card").clickable(onClick = onOpenReportExplainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Report Explainer", style = MaterialTheme.typography.titleSmall)
                        Text("Upload labs & ECG text/images", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedCard(
                    modifier = Modifier.weight(1f).testTag("doctor_home_appointment_prep_card").clickable(onClick = onOpenAppointmentPrep)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Appointment Prep", style = MaterialTheme.typography.titleSmall)
                        Text("Questions & visit checklist", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("Selectable AI Doctor Modes", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onOpenHistory, modifier = Modifier.testTag("doctor_home_history_button")) {
                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("History ($conversationsCount)")
                }
            }
        }

        items(AiDoctorMode.entries, key = { it.name }) { mode ->
            val icon = when (mode) {
                AiDoctorMode.HEART_SPECIALIST -> Icons.Filled.Favorite
                AiDoctorMode.REPORT_EXPLAINER -> Icons.Filled.Description
                AiDoctorMode.MENTAL_WELLNESS -> Icons.Filled.Psychology
                AiDoctorMode.HEALTH_NAVIGATOR -> Icons.Filled.LocalHospital
                AiDoctorMode.WELLNESS -> Icons.Filled.SelfImprovement
                else -> Icons.Filled.MedicalServices
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("doctor_mode_card_${mode.modeId}")
                    .clickable {
                        if (mode == AiDoctorMode.REPORT_EXPLAINER) onOpenReportExplainer()
                        else onOpenModeChat(mode)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mode.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(mode.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("AI Provider Routing & Reliability", style = MaterialTheme.typography.titleSmall)
                    providerStatuses.forEach { p ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${p.name} (${p.roleLabel})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Text(p.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                color = if (p.configured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    if (p.configured) "Ready" else "Optional",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoctorChatScreen(
    selectedMode: AiDoctorMode,
    messages: List<DoctorAiMessage>,
    isBusy: Boolean,
    errorMessage: String?,
    emergencyBanner: EmergencyScreenResult?,
    googleSearchEnabled: Boolean,
    groundingSourcesByMessage: Map<String, List<GroundingSource>>,
    onSelectMode: (AiDoctorMode) -> Unit,
    onToggleGoogleSearch: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    onNewChat: () -> Unit,
    onRetry: () -> Unit,
    onFeedback: (String, Long) -> Unit,
    onDismissEmergency: () -> Unit,
    onOpenAppointmentPrep: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val starterPrompts = remember(selectedMode) {
        when (selectedMode) {
            AiDoctorMode.SYMPTOM_CHECKER -> listOf(
                "I've had fatigue and mild dizziness for 2 days. What should I monitor?",
                "How do I know if a headache needs urgent care?",
                "What questions help assess persistent cough duration?"
            )
            AiDoctorMode.WELLNESS -> listOf(
                "How can I build a 7-hour sleep routine with shift work?",
                "Practical hydration & nutrition habits for high-stress days"
            )
            AiDoctorMode.MENTAL_WELLNESS -> listOf(
                "Suggest calm grounding techniques for acute work burnout",
                "When should stress support involve a mental health specialist?"
            )
            AiDoctorMode.HEALTH_NAVIGATOR -> listOf(
                "Which specialist evaluates palpitations vs fatigue?",
                "Help me prepare questions for my upcoming doctor checkup"
            )
            else -> listOf(
                "Explain normal resting blood pressure vs elevated readings",
                "How do sleep deficit and stress affect afternoon energy?",
                "What preventive screenings should working adults discuss?"
            )
        }
    }

    LaunchedEffect(messages.size, isBusy) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("AI DOCTOR · ${selectedMode.displayName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Health information, personalized wellness guidance and care navigation.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onNewChat, modifier = Modifier.testTag("doctor_new_chat_button")) {
                Icon(Icons.Filled.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Chat")
            }
        }

        NonDiagnosticBanner(text = StructuredAiResponse.DOCTOR_MODE_DISCLAIMER)
        Spacer(Modifier.height(6.dp))

        if (emergencyBanner?.triggered == true) {
            EmergencySafetyBannerCard(banner = emergencyBanner, onDismiss = onDismissEmergency)
            Spacer(Modifier.height(6.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        ) {
            AiDoctorMode.entries.filter { it != AiDoctorMode.REPORT_EXPLAINER }.forEach { m ->
                FilterChip(
                    selected = m == selectedMode,
                    onClick = { onSelectMode(m) },
                    label = { Text(m.displayName, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("doctor_chat_mode_${m.modeId}")
                )
            }
            FilterChip(
                selected = googleSearchEnabled,
                onClick = { onToggleGoogleSearch(!googleSearchEnabled) },
                leadingIcon = { Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(14.dp)) },
                label = { Text(if (googleSearchEnabled) "Web Search ON" else "Web Search OFF", style = MaterialTheme.typography.labelSmall) }
            )
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
                            Text("Start a Guided Consultation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Tap a suggested question below or describe your symptoms, duration, and severity.", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = onOpenAppointmentPrep) {
                                Text("Open Appointment Preparation Tool")
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id.ifBlank { "${it.sender}_${it.text.hashCode()}" } }) { msg ->
                when (msg.sender) {
                    "USER" -> {
                        Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.widthIn(max = 520.dp)
                            ) {
                                Text(msg.text, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                    "SYSTEM_EMERGENCY" -> {
                        EmergencySafetyBannerCard(
                            banner = EmergencyScreenResult(triggered = true, guidance = msg.text)
                        )
                    }
                    else -> {
                        val parsed = msg.structuredJson?.let {
                            StructuredAiResponseParser.parseAndValidate(
                                rawJson = it,
                                fallbackMode = msg.mode,
                                fallbackDisclaimer = StructuredAiResponse.DOCTOR_MODE_DISCLAIMER,
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
                        Text("Screening safety signals & preparing structured guidance...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        ) {
            starterPrompts.forEach { prompt ->
                AssistChip(
                    onClick = { if (!isBusy) onSendMessage(prompt) },
                    label = { Text(prompt, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask a health question, symptom, or report term...") },
                maxLines = 3,
                modifier = Modifier.weight(1f).testTag("doctor_chat_input")
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
                modifier = Modifier.size(48.dp).testTag("doctor_chat_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send question", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ConversationHistoryScreen(
    conversations: List<DoctorConversation>,
    onResumeConversation: (DoctorConversation) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onStartNewDoctorChat: () -> Unit,
    onStartNewHeartChat: () -> Unit
) {
    var filterDomain by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }

    val filtered = remember(conversations, filterDomain, search) {
        conversations.filter { conv ->
            val domainMatch = filterDomain == null || conv.domain == filterDomain
            val textMatch = search.isBlank() ||
                conv.title.contains(search, ignoreCase = true) ||
                conv.lastPreview.contains(search, ignoreCase = true)
            domainMatch && textMatch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Consultation History", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onStartNewDoctorChat, modifier = Modifier.weight(1f)) {
                    Text("New Doctor Chat")
                }
                OutlinedButton(onClick = onStartNewHeartChat, modifier = Modifier.weight(1f)) {
                    Text("New Heart Chat")
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search consultations") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filterDomain == null, onClick = { filterDomain = null }, label = { Text("All") })
                FilterChip(selected = filterDomain == DoctorDomain.DOCTOR.name, onClick = { filterDomain = DoctorDomain.DOCTOR.name }, label = { Text("AI Doctor") })
                FilterChip(selected = filterDomain == DoctorDomain.HEART_SPECIALIST.name, onClick = { filterDomain = DoctorDomain.HEART_SPECIALIST.name }, label = { Text("Heart Specialist") })
            }
        }
        if (filtered.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Text(
                        "No saved consultations match your filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { conv ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.fillMaxWidth().clickable { onResumeConversation(conv) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = if (conv.domain == DoctorDomain.HEART_SPECIALIST.name) Icons.Filled.Favorite else Icons.Filled.MedicalServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(conv.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(conv.lastPreview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${AiDoctorMode.fromRaw(conv.mode).displayName} · ${conv.messageCount} msgs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDeleteConversation(conv.id) }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete conversation")
                        }
                    }
                }
            }
        }
    }
}

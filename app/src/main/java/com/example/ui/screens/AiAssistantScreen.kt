package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AiAssistantRole
import com.example.data.GroundingSource
import com.example.localization.LocalizedStrings
import com.example.model.AiChatMessage
import com.example.model.AiSender
import com.example.ui.components.NonDiagnosticBanner

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    strings: LocalizedStrings,
    messages: List<AiChatMessage>,
    streamingReply: String?,
    isBusy: Boolean,
    selectedRole: AiAssistantRole,
    googleSearchEnabled: Boolean,
    groundingSourcesByMessage: Map<String, List<GroundingSource>>,
    onSelectRole: (AiAssistantRole) -> Unit,
    onToggleGoogleSearch: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickPrompts = remember(selectedRole, googleSearchEnabled) {
        if (googleSearchEnabled || selectedRole == AiAssistantRole.PUBLIC_HEALTH_SEARCH) {
            listOf(
                "Latest CDC workplace hydration & heat stress tips",
                "WHO recommended weekly physical activity for office workers",
                "Evidence-based signs of occupational burnout recovery",
                "Explain my recent sleep & stress trends"
            )
        } else {
            listOf(
                "Explain my recent sleep & stress trends",
                "How can I prevent afternoon burnout?",
                "What symptoms mean I should see a doctor?",
                "Give me a 3-step sleep recovery plan"
            )
        }
    }

    LaunchedEffect(messages.size, streamingReply) {
        val total = messages.size + if (streamingReply != null) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        NonDiagnosticBanner(text = strings.nonDiagnosticBanner)
        Spacer(modifier = Modifier.height(6.dp))

        // Role-based Gemini Chatbot Selector + Google Search Grounding Toggle
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            AiAssistantRole.entries.forEach { role ->
                FilterChip(
                    selected = selectedRole == role,
                    onClick = { onSelectRole(role) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (role == AiAssistantRole.PUBLIC_HEALTH_SEARCH) Icons.Filled.Public else Icons.Filled.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(role.displayName, style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.testTag("ai_role_${role.name.lowercase()}")
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = googleSearchEnabled || selectedRole.defaultSearchEnabled,
                onClick = { onToggleGoogleSearch(!googleSearchEnabled) },
                enabled = !selectedRole.defaultSearchEnabled,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        if (googleSearchEnabled || selectedRole.defaultSearchEnabled) {
                            "Google Search Grounding: ON"
                        } else {
                            "Google Search Grounding: OFF"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier.testTag("ai_google_search_toggle")
            )

            Text(
                text = "${messages.size.coerceAtMost(12)} turns in context • ${selectedRole.modelName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Emergency Care Notice
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Filled.Emergency,
                    contentDescription = "Emergency care notice",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.emergencyNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && streamingReply == null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Yaqeen Multi-Turn Gemini Guide (${selectedRole.displayName})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(strings.aiGreeting, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                selectedRole.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id.ifBlank { "${it.sender}_${it.text.hashCode()}" } }) { msg ->
                val sources = groundingSourcesByMessage[msg.id].orEmpty()
                ChatBubble(
                    isUser = msg.sender == AiSender.USER.name,
                    text = msg.text,
                    fallbackUsed = msg.fallbackUsed,
                    sources = sources
                )
            }

            if (streamingReply != null) {
                item {
                    ChatBubble(
                        isUser = false,
                        text = streamingReply.ifBlank { "Thinking..." },
                        fallbackUsed = false,
                        isStreaming = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            quickPrompts.forEach { prompt ->
                AssistChip(
                    onClick = { if (!isBusy) onSendMessage(prompt) },
                    label = { Text(prompt, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(strings.askAssistantHint) },
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty() && !isBusy) {
                        input = ""
                        onSendMessage(text)
                    }
                },
                enabled = input.isNotBlank() && !isBusy,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("ai_chat_send_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = strings.send,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatBubble(
    isUser: Boolean,
    text: String,
    fallbackUsed: Boolean,
    sources: List<GroundingSource> = emptyList(),
    isStreaming: Boolean = false
) {
    val uriHandler = LocalUriHandler.current
    Box(
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
            shape = MaterialTheme.shapes.medium,
            tonalElevation = if (isUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser && fallbackUsed) {
                    Text(
                        text = "Safe Rule-Based Fallback",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                if (!isUser && sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val query = sources.firstOrNull()?.searchQuery.orEmpty()
                    Text(
                        text = if (query.isNotBlank()) "Grounded via Google Search: \"$query\"" else "Grounded Sources",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sources.forEach { src ->
                            AssistChip(
                                onClick = { runCatching { uriHandler.openUri(src.uri) } },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Open source ${src.title}",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        "${src.domain}: ${src.title}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
                if (isStreaming) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Streaming live...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

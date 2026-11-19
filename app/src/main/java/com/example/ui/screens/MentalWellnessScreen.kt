package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.localization.LocalizedStrings
import com.example.ui.components.NonDiagnosticBanner
import com.example.ui.theme.RiskGreenFg
import com.example.ui.theme.RiskOrangeFg
import com.example.ui.theme.RiskYellowFg
import com.example.viewmodel.HealthDashboardMetrics
import kotlinx.coroutines.delay

@Composable
fun MentalWellnessScreen(
    strings: LocalizedStrings,
    metrics: HealthDashboardMetrics,
    onOpenPreventionPlan: () -> Unit
) {
    var breathingActive by remember { mutableStateOf(false) }
    var phaseIndex by remember { mutableIntStateOf(0) } // 0=Inhale, 1=Hold, 2=Exhale, 3=Rest
    var secondsLeft by remember { mutableIntStateOf(4) }

    LaunchedEffect(breathingActive) {
        if (!breathingActive) {
            phaseIndex = 0
            secondsLeft = 4
            return@LaunchedEffect
        }
        while (breathingActive) {
            for (s in 4 downTo 1) {
                secondsLeft = s
                delay(1000L)
            }
            phaseIndex = (phaseIndex + 1) % 4
        }
    }

    val targetScale = when {
        !breathingActive -> 0.72f
        phaseIndex == 0 || phaseIndex == 1 -> 1.0f
        else -> 0.65f
    }
    val circleScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 900),
        label = "breathScale"
    )
    val phaseText = when (phaseIndex) {
        0 -> strings.inhale
        1 -> strings.hold
        2 -> strings.exhale
        else -> strings.hold
    }

    val burnoutColor = when {
        metrics.burnoutIndex >= 65 -> RiskOrangeFg
        metrics.burnoutIndex >= 40 -> RiskYellowFg
        else -> RiskGreenFg
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Mental Wellness & Burnout Prevention", style = MaterialTheme.typography.headlineSmall)
        NonDiagnosticBanner(text = strings.nonDiagnosticBanner)

        // Burnout Prevention Gauge Card
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Burnout Strain Index", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${metrics.burnoutIndex} / 100",
                        style = MaterialTheme.typography.titleMedium,
                        color = burnoutColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { (metrics.burnoutIndex / 100f).coerceIn(0f, 1f) },
                    color = burnoutColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                )
                Text(
                    text = when {
                        metrics.burnoutIndex >= 65 -> "Elevated workload & fatigue signals detected. Protect rest blocks and speak with a wellness coordinator if strain persists."
                        metrics.burnoutIndex >= 40 -> "Moderate stress load. Micro-breaks and sleep consistency will help rebalance energy."
                        else -> "Healthy recovery balance across recent stress, sleep, and fatigue logs."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Guided 4-4-4-4 Box Breathing Timer
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_mindful_breathing),
                    contentDescription = "Mindful breathing wellness art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(MaterialTheme.shapes.medium)
                )

                Text(strings.breathingTitle, style = MaterialTheme.typography.titleLarge)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(144.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .scale(circleScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (breathingActive) "${secondsLeft}s" else "4-4-4-4",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (breathingActive) phaseText else "Ready",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Button(
                    onClick = { breathingActive = !breathingActive },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("toggle_breathing_button")
                ) {
                    Icon(Icons.Filled.SelfImprovement, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (breathingActive) strings.stopBreathing else strings.startBreathing)
                }
            }
        }

        // Work-Life Balance & Micro-Break Habits
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Coffee, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Work-Life Recovery Micro-Habits", style = MaterialTheme.typography.titleMedium)
                }
                Text("• 90-minute focus block followed by a 5-minute away-from-screen stretch.", style = MaterialTheme.typography.bodySmall)
                Text("• Hard stop boundary: silence work messaging 60 minutes before sleep.", style = MaterialTheme.typography.bodySmall)
                Text("• Progressive shoulder & jaw relaxation for 60 seconds after tense meetings.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = onOpenPreventionPlan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Personalized Prevention Plan")
                }
            }
        }

        // Support Resources
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Confidential Support Resources", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "If you feel overwhelmed or persistently anxious, reach out to a licensed counselor, Employee Assistance Program (EAP), or local healthcare helpline.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

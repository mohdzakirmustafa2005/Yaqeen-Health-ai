package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.GroundingSource
import com.example.model.BloodPressureRecord
import com.example.model.EmergencyScreenResult
import com.example.model.HeartMeasurement
import com.example.model.StructuredAiResponse
import com.example.ui.theme.RiskOrangeFg
import com.example.ui.theme.RiskRedBg
import com.example.ui.theme.RiskRedFg

@Composable
fun EmergencySafetyBannerCard(
    banner: EmergencyScreenResult,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RiskRedBg),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .testTag("emergency_safety_banner")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Filled.Emergency,
                        contentDescription = "Emergency warning alert",
                        tint = RiskRedFg,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EMERGENCY WARNING — CALL 112 OR LOCAL EMS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = RiskRedFg
                    )
                }
                if (onDismiss != null) {
                    TextButton(onClick = onDismiss) {
                        Text("Acknowledge", color = RiskRedFg)
                    }
                }
            }
            if (banner.matchedSignals.isNotEmpty()) {
                Text(
                    text = "Detected signals: ${banner.matchedSignals.joinToString(", ")}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = RiskRedFg
                )
            }
            Text(
                text = banner.guidance.ifBlank { banner.reason },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun DataSourceLabelChip(
    sourceLabel: String,
    modifier: Modifier = Modifier
) {
    val isDemo = sourceLabel == "DEMO_FICTIONAL"
    Surface(
        color = if (isDemo) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = if (isDemo) "Fictional Demo Data" else "User-Entered Data",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isDemo) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun HeartRateTrendChart(
    measurements: List<HeartMeasurement>,
    modifier: Modifier = Modifier
) {
    val points = remember(measurements) {
        measurements.filter { it.restingHeartRateBpm != null }.take(7).reversed()
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = lineColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Heart Rate Trend (bpm)", style = MaterialTheme.typography.titleSmall)
                }
                if (points.isNotEmpty()) {
                    DataSourceLabelChip(sourceLabel = points.last().sourceLabel)
                }
            }
            if (points.size < 2) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().height(110.dp)
                ) {
                    Text(
                        "Log at least 2 heart rate readings to chart your resting pulse.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val values = points.mapNotNull { it.restingHeartRateBpm?.toFloat() }
                val minV = (values.minOrNull() ?: 50f) - 10f
                val maxV = ((values.maxOrNull() ?: 110f) + 10f).coerceAtLeast(minV + 20f)
                Canvas(modifier = Modifier.fillMaxWidth().height(128.dp)) {
                    val w = size.width
                    val h = size.height
                    listOf(0.25f, 0.5f, 0.75f).forEach { r ->
                        drawLine(gridColor, Offset(0f, h * r), Offset(w, h * r), strokeWidth = 1.dp.toPx())
                    }
                    val stepX = w / (values.size - 1).coerceAtLeast(1)
                    val path = Path()
                    values.forEachIndexed { idx, v ->
                        val x = idx * stepX
                        val norm = ((v - minV) / (maxV - minV)).coerceIn(0f, 1f)
                        val y = h - norm * (h - 16.dp.toPx()) - 8.dp.toPx()
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    val area = Path().apply {
                        addPath(path)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        area,
                        brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.22f), Color.Transparent))
                    )
                    drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    values.forEachIndexed { idx, v ->
                        val x = idx * stepX
                        val norm = ((v - minV) / (maxV - minV)).coerceIn(0f, 1f)
                        val y = h - norm * (h - 16.dp.toPx()) - 8.dp.toPx()
                        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
                    }
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    points.forEach { m ->
                        Text(
                            text = "${m.restingHeartRateBpm} bpm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BloodPressureDualChart(
    records: List<BloodPressureRecord>,
    modifier: Modifier = Modifier
) {
    val points = remember(records) { records.take(7).reversed() }
    val sysColor = RiskRedFg
    val diaColor = RiskOrangeFg
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Blood Pressure Trend (mmHg)", style = MaterialTheme.typography.titleSmall)
                if (points.isNotEmpty()) {
                    DataSourceLabelChip(sourceLabel = points.last().sourceLabel)
                }
            }
            if (points.size < 2) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().height(110.dp)
                ) {
                    Text(
                        "Log at least 2 blood pressure readings to view systolic/diastolic trends.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxWidth().height(132.dp)) {
                    val w = size.width
                    val h = size.height
                    listOf(0.25f, 0.5f, 0.75f).forEach { r ->
                        drawLine(gridColor, Offset(0f, h * r), Offset(w, h * r), strokeWidth = 1.dp.toPx())
                    }
                    val stepX = w / (points.size - 1).coerceAtLeast(1)
                    fun drawSeries(vals: List<Float>, color: Color) {
                        val path = Path()
                        vals.forEachIndexed { idx, v ->
                            val x = idx * stepX
                            val norm = ((v - 50f) / 150f).coerceIn(0f, 1f)
                            val y = h - norm * (h - 16.dp.toPx()) - 8.dp.toPx()
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                        vals.forEachIndexed { idx, v ->
                            val x = idx * stepX
                            val norm = ((v - 50f) / 150f).coerceIn(0f, 1f)
                            val y = h - norm * (h - 16.dp.toPx()) - 8.dp.toPx()
                            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
                        }
                    }
                    drawSeries(points.map { it.systolicMmHg.toFloat() }, sysColor)
                    drawSeries(points.map { it.diastolicMmHg.toFloat() }, diaColor)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    points.forEach { bp ->
                        Text(
                            text = "${bp.systolicMmHg}/${bp.diastolicMmHg}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StructuredClinicalResponseCard(
    structured: StructuredAiResponse,
    groundingSources: List<GroundingSource> = emptyList(),
    currentFeedback: Long = 0L,
    onFollowUpClick: ((String) -> Unit)? = null,
    onFeedbackClick: ((Long) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val urgency = structured.medicalAttention.urgency.lowercase()
    val urgencyColor = when (urgency) {
        "emergency" -> RiskRedFg
        "urgent" -> RiskOrangeFg
        "prompt" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(color = urgencyColor.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                    Text(
                        text = "Care Level: ${urgency.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = urgencyColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (!structured.doctorType.isNullOrBlank()) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.LocalHospital, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(structured.doctorType, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)) {
                    Text(
                        text = structured.providerUsed,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (structured.summary.isNotBlank()) {
                Text(
                    text = structured.summary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(text = structured.answer, style = MaterialTheme.typography.bodyMedium)

            if (structured.observations.isNotEmpty()) {
                Text("Key Observations", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                structured.observations.forEach { obs ->
                    Text("• $obs", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (structured.recommendations.isNotEmpty()) {
                Text("Suggested Next Steps", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                structured.recommendations.forEach { rec ->
                    Text("• $rec", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (structured.followUpQuestions.isNotEmpty() && onFollowUpClick != null) {
                Text("Follow-Up Questions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    structured.followUpQuestions.forEach { q ->
                        AssistChip(
                            onClick = { onFollowUpClick(q) },
                            label = { Text(q, style = MaterialTheme.typography.labelSmall, maxLines = 2) }
                        )
                    }
                }
            }

            if (groundingSources.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    groundingSources.forEach { src ->
                        AssistChip(
                            onClick = { runCatching { uriHandler.openUri(src.uri) } },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            label = { Text("${src.domain}: ${src.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (onFeedbackClick != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onFeedbackClick(1L) },
                            modifier = Modifier.size(48.dp).testTag("ai_feedback_up")
                        ) {
                            Icon(
                                Icons.Filled.ThumbUp,
                                contentDescription = "Helpful response",
                                tint = if (currentFeedback > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onFeedbackClick(-1L) },
                            modifier = Modifier.size(48.dp).testTag("ai_feedback_down")
                        ) {
                            Icon(
                                Icons.Filled.ThumbDown,
                                contentDescription = "Not helpful",
                                tint = if (currentFeedback < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                if (onRetry != null) {
                    OutlinedButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry")
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = structured.disclaimer,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

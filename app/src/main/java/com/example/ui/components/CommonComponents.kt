package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.AppLanguage
import com.example.localization.LocalizedStrings
import com.example.model.HealthCheckIn
import com.example.model.RiskLevel
import com.example.ui.theme.RiskGreenBg
import com.example.ui.theme.RiskGreenFg
import com.example.ui.theme.RiskOrangeBg
import com.example.ui.theme.RiskOrangeFg
import com.example.ui.theme.RiskRedBg
import com.example.ui.theme.RiskRedFg
import com.example.ui.theme.RiskYellowBg
import com.example.ui.theme.RiskYellowFg

@Composable
fun riskColors(level: RiskLevel): Pair<Color, Color> = when (level) {
    RiskLevel.GREEN -> RiskGreenFg to RiskGreenBg
    RiskLevel.YELLOW -> RiskYellowFg to RiskYellowBg
    RiskLevel.ORANGE -> RiskOrangeFg to RiskOrangeBg
    RiskLevel.RED -> RiskRedFg to RiskRedBg
}

@Composable
fun RiskBadge(level: RiskLevel, strings: LocalizedStrings, modifier: Modifier = Modifier) {
    val (fg, bg) = riskColors(level)
    val label = when (level) {
        RiskLevel.GREEN -> strings.riskGreen
        RiskLevel.YELLOW -> strings.riskYellow
        RiskLevel.ORANGE -> strings.riskOrange
        RiskLevel.RED -> strings.riskRed
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = modifier.testTag("risk_badge_${level.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = fg,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NonDiagnosticBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Medical disclaimer",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun DemoModePill(label: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Science,
                contentDescription = "Fictional demo data",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun LanguageSelectorChip(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .testTag("language_selector_button")
                .clickable { expanded = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = "Switch language",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = current.code,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppLanguage.entries.forEach { lang ->
                DropdownMenuItem(
                    text = { Text("${lang.nativeLabel} (${lang.code})") },
                    onClick = {
                        expanded = false
                        onSelect(lang)
                    },
                    modifier = Modifier.testTag("lang_option_${lang.code.lowercase()}")
                )
            }
        }
    }
}

@Composable
fun WellnessScoreRing(
    score: Int,
    label: String,
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f) * 260f,
        animationSpec = tween(durationMillis = 650),
        label = "scoreSweep"
    )
    val (riskFg, _) = riskColors(riskLevel)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(132.dp)
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val stroke = 12.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(size.width - stroke, size.height - stroke)
            )
            drawArc(
                color = riskFg,
                startAngle = 140f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(size.width - stroke, size.height - stroke)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (score > 0) "$score" else "--",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MultiMetricTrendChart(
    checkIns: List<HealthCheckIn>,
    modifier: Modifier = Modifier
) {
    val ordered = remember(checkIns) { checkIns.take(7).reversed() }
    val primaryColor = MaterialTheme.colorScheme.primary
    val stressColor = RiskOrangeFg
    val sleepColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "7-Day Health Trajectory",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendDot(color = primaryColor, text = "Wellbeing")
                    LegendDot(color = sleepColor, text = "Sleep")
                    LegendDot(color = stressColor, text = "Stress")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (ordered.size < 2) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.HealthAndSafety,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Log at least 2 check-ins to visualize trend trajectories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                ) {
                    val h = size.height
                    val w = size.width
                    // Horizontal grid guides (2, 5, 8)
                    listOf(0.2f, 0.5f, 0.8f).forEach { ratio ->
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, h * ratio),
                            end = Offset(w, h * ratio),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    fun plotSeries(values: List<Float>, lineColor: Color, fillArea: Boolean = false) {
                        val path = Path()
                        val stepX = if (values.size > 1) w / (values.size - 1) else w
                        values.forEachIndexed { idx, v ->
                            val x = idx * stepX
                            val y = h - ((v.coerceIn(0f, 10f) / 10f) * (h - 16.dp.toPx())) - 8.dp.toPx()
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        if (fillArea) {
                            val areaPath = Path().apply {
                                addPath(path)
                                lineTo(w, h)
                                lineTo(0f, h)
                                close()
                            }
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(lineColor.copy(alpha = 0.22f), Color.Transparent)
                                )
                            )
                        }
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                        values.forEachIndexed { idx, v ->
                            val x = idx * stepX
                            val y = h - ((v.coerceIn(0f, 10f) / 10f) * (h - 16.dp.toPx())) - 8.dp.toPx()
                            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
                        }
                    }

                    plotSeries(ordered.map { it.overallWellbeing.toFloat() }, primaryColor, fillArea = true)
                    plotSeries(ordered.map { it.sleepHours.toFloat().coerceIn(0f, 10f) }, sleepColor)
                    plotSeries(ordered.map { it.stressLevel.toFloat() }, stressColor)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ordered.forEach { ci ->
                        Text(
                            text = ci.dateKey.takeLast(5),
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
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

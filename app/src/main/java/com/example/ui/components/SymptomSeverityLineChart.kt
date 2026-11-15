package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SymptomLogEntity
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SymptomSeverityLineChart(
    symptomLogs: List<SymptomLogEntity>,
    title: String = tr(
        "Symptom Severity Trend (1-10)",
        "લક્ષણની તીવ્રતાનો ટ્રેન્ડ (૧-૧૦)",
        "लक्षण तीव्रता रुझान (1-10)"
    ),
    modifier: Modifier = Modifier
) {
    if (symptomLogs.isEmpty()) return

    // Sort chronologically ascending to plot over time
    val sortedLogs = symptomLogs.sortedBy { it.timestamp }.takeLast(10)
    if (sortedLogs.isEmpty()) return

    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val latestSeverity = sortedLogs.lastOrNull()?.severity ?: 5
    val averageSeverity = String.format(Locale.US, "%.1f", sortedLogs.map { it.severity }.average())

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("symptom_severity_line_chart")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = "Severity Chart",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                "Avg: $averageSeverity/10 • Latest: $latestSeverity/10",
                                "સરેરાશ: $averageSeverity/10 • છેલ્લું: $latestSeverity/10",
                                "औसत: $averageSeverity/10 • नवीनतम: $latestSeverity/10"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        latestSeverity >= 8 -> EmergencyRed.copy(alpha = 0.15f)
                        latestSeverity >= 4 -> PrimaryTeal.copy(alpha = 0.15f)
                        else -> SafeMint.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = when {
                            latestSeverity >= 8 -> tr("Severe (8-10)", "તીવ્ર (૮-૧૦)", "तीव्र (8-10)")
                            latestSeverity >= 4 -> tr("Moderate (4-7)", "મધ્યમ (૪-૭)", "मध्यम (4-7)")
                            else -> tr("Mild (1-3)", "હળવું (૧-૩)", "हल्का (1-3)")
                        },
                        color = when {
                            latestSeverity >= 8 -> EmergencyRed
                            latestSeverity >= 4 -> PrimaryTeal
                            else -> Color(0xFF2E7D32)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Chart
            val lineColor = MaterialTheme.colorScheme.primary
            val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            val fillGradient = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.35f),
                    lineColor.copy(alpha = 0.0f)
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 28.dp, end = 12.dp, top = 12.dp, bottom = 24.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw horizontal grid lines for Severity levels: 10, 5, 1
                    val gridLines = listOf(10f, 5f, 1f)
                    for (level in gridLines) {
                        val y = h - ((level - 1f) / 9f * h)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // If only 1 data point, draw a central point
                    if (sortedLogs.size == 1) {
                        val singleY = h - ((sortedLogs.first().severity - 1f) / 9f * h)
                        drawCircle(
                            color = lineColor,
                            radius = 6.dp.toPx(),
                            center = Offset(w / 2f, singleY)
                        )
                        return@Canvas
                    }

                    // Calculate point offsets
                    val stepX = w / (sortedLogs.size - 1)
                    val points = sortedLogs.mapIndexed { index, log ->
                        val x = index * stepX
                        // Normalize severity (1 to 10) -> (0 to 1) -> y
                        val clampedSeverity = log.severity.coerceIn(1, 10)
                        val normalizedY = (clampedSeverity - 1f) / 9f
                        val y = h - (normalizedY * h)
                        Offset(x, y)
                    }

                    // 2. Build fill path under the line
                    val fillPath = Path().apply {
                        moveTo(points.first().x, h)
                        lineTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val pPrev = points[i - 1]
                            val pCurr = points[i]
                            val cX1 = pPrev.x + (pCurr.x - pPrev.x) / 2
                            val cY1 = pPrev.y
                            val cX2 = pPrev.x + (pCurr.x - pPrev.x) / 2
                            val cY2 = pCurr.y
                            cubicTo(cX1, cY1, cX2, cY2, pCurr.x, pCurr.y)
                        }
                        lineTo(points.last().x, h)
                        close()
                    }
                    drawPath(path = fillPath, brush = fillGradient)

                    // 3. Build smooth stroke line path
                    val strokePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val pPrev = points[i - 1]
                            val pCurr = points[i]
                            val cX1 = pPrev.x + (pCurr.x - pPrev.x) / 2
                            val cY1 = pPrev.y
                            val cX2 = pPrev.x + (pCurr.x - pPrev.x) / 2
                            val cY2 = pCurr.y
                            cubicTo(cX1, cY1, cX2, cY2, pCurr.x, pCurr.y)
                        }
                    }
                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 4. Draw prominent point circles with inner and outer rings
                    points.forEachIndexed { index, pt ->
                        val severity = sortedLogs[index].severity
                        val pointColor = when {
                            severity >= 8 -> EmergencyRed
                            severity >= 4 -> lineColor
                            else -> SafeMint
                        }

                        // Outer halo
                        drawCircle(
                            color = pointColor.copy(alpha = 0.25f),
                            radius = 7.dp.toPx(),
                            center = pt
                        )
                        // Core dot
                        drawCircle(
                            color = pointColor,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        // White highlight
                        drawCircle(
                            color = Color.White,
                            radius = 1.5.dp.toPx(),
                            center = pt
                        )
                    }
                }

                // Y-Axis Labels (10, 5, 1)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 4.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "10",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmergencyRed
                    )
                    Text(
                        text = "5",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "1",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            // X-Axis Timestamps (Dates)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (sortedLogs.size >= 2) {
                    Text(
                        text = dateFormat.format(Date(sortedLogs.first().timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (sortedLogs.size >= 3) {
                        Text(
                            text = dateFormat.format(Date(sortedLogs[sortedLogs.size / 2].timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = dateFormat.format(Date(sortedLogs.last().timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                } else if (sortedLogs.isNotEmpty()) {
                    Text(
                        text = dateFormat.format(Date(sortedLogs.first().timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

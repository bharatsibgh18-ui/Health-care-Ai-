package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.ui.components.UltraGlassCard
import com.example.ui.components.UltraGradientHeroCard
import com.example.ui.components.UltraSectionHeader
import com.example.ui.theme.SafeMint
import com.example.ui.theme.UltraCyanEnd
import com.example.ui.theme.UltraIndigoStart
import com.example.ui.theme.UltraMintEnd
import com.example.ui.theme.UltraTealStart
import com.example.ui.theme.trLang
import com.example.utils.CoachTask
import com.example.utils.CoachTimeSlot
import com.example.utils.DailyCoachPlan

@Composable
fun UltraAiCoachTab(
    plan: DailyCoachPlan,
    completedTaskIds: Set<String>,
    isGenerating: Boolean,
    selectedLanguage: String,
    onToggleTask: (String) -> Unit,
    onRefreshAiCoach: () -> Unit
) {
    val totalTasks = plan.tasks.size.coerceAtLeast(1)
    val completedCount = plan.tasks.count { it.id in completedTaskIds }
    val completionFraction = (completedCount.toFloat() / totalTasks.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = completionFraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "coach_completion_ring"
    )

    // Hero Readiness & Progress Ring Card
    UltraGradientHeroCard(
        modifier = Modifier.testTag("ultra_coach_hero_card"),
        colors = listOf(UltraTealStart, UltraCyanEnd, UltraIndigoStart)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(86.dp)
            ) {
                Canvas(modifier = Modifier.size(86.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    drawArc(
                        color = Color.White.copy(alpha = 0.22f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color.White, Color(0xFF67E8F9), UltraMintEnd, Color.White)),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$completedCount/$totalTasks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = trLang(selectedLanguage, "Done", "પૂર્ણ", "पूर्ण"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = if (plan.isAiGenerated) {
                            trLang(selectedLanguage, "AI Personalized Brief", "AI વ્યક્તિગત કોચ", "AI व्यक्तिगत कोच")
                        } else {
                            trLang(selectedLanguage, "Smart Clinical Coach", "સ્માર્ટ ક્લિનિકલ કોચ", "स्मार्ट क्लिनिकल कोच")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = plan.readinessHeadline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White.copy(alpha = 0.14f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = plan.summaryBrief,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onRefreshAiCoach,
            enabled = !isGenerating,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = UltraTealStart
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ultra_coach_refresh_btn")
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = UltraTealStart
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trLang(selectedLanguage, "Personalizing AI Plan...", "AI પ્લાન તૈયાર થઈ રહ્યો છે...", "AI प्लान तैयार हो रहा है..."),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = trLang(selectedLanguage, "Generate Fresh AI Plan", "નવો AI કોચ પ્લાન બનાવો", "नया AI कोच प्लान बनाएं"),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Morning / Afternoon / Evening Action Sections
    val slotSections = listOf(
        Triple(
            CoachTimeSlot.MORNING,
            Icons.Default.WbSunny,
            trLang(selectedLanguage, "Morning Routine", "સવારની દિનચર્યા", "सुबह की दिनचर्या")
        ),
        Triple(
            CoachTimeSlot.AFTERNOON,
            Icons.Default.WbTwilight,
            trLang(selectedLanguage, "Afternoon Boost", "બપોરનું આરોગ્ય લક્ષ્ય", "दोपहर का स्वास्थ्य लक्ष्य")
        ),
        Triple(
            CoachTimeSlot.EVENING,
            Icons.Default.NightsStay,
            trLang(selectedLanguage, "Evening Wind-Down", "સાંજ અને ઊંઘની તૈયારી", "शाम और नींद की तैयारी")
        )
    )

    slotSections.forEach { (slot, icon, title) ->
        val slotTasks = plan.tasks.filter { it.slot == slot }
        if (slotTasks.isNotEmpty()) {
            UltraSectionHeader(title = title, icon = icon)
            slotTasks.forEach { task ->
                CoachTaskGlassRow(
                    task = task,
                    completed = task.id in completedTaskIds,
                    onClick = { onToggleTask(task.id) }
                )
            }
        }
    }
}

@Composable
private fun CoachTaskGlassRow(
    task: CoachTask,
    completed: Boolean,
    onClick: () -> Unit
) {
    val iconTint by animateColorAsState(
        targetValue = if (completed) SafeMint else MaterialTheme.colorScheme.primary,
        label = "coach_task_tint"
    )
    UltraGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("ultra_coach_task_${task.id}"),
        contentPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = task.title,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (completed) SafeMint.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = task.metricBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (completed) SafeMint else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

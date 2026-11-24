package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.UltraGlassCard
import com.example.ui.components.UltraSectionHeader
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.UltraAmberStart
import com.example.ui.theme.UltraCyanEnd
import com.example.ui.theme.UltraIndigoStart
import com.example.ui.theme.UltraTealStart
import com.example.ui.theme.trLang
import kotlin.math.hypot

data class BodyZoneSpec(
    val id: String,
    val titleEn: String,
    val titleGu: String,
    val titleHi: String,
    val normX: Float, // 0..1 on Canvas
    val normY: Float, // 0..1 on Canvas
    val isBackPreferred: Boolean = false,
    val quickSymptoms: List<String>
)

private val BODY_ZONES = listOf(
    BodyZoneSpec("HEAD", "Head & Neuro", "માથું અને મગજ", "सिर और तंत्रिका", 0.50f, 0.12f, false, listOf("Headache", "Dizziness", "Migraine", "Eye Strain")),
    BodyZoneSpec("THROAT", "Throat & ENT", "ગળું, કાન અને નાક", "गला, कान व नाक", 0.50f, 0.22f, false, listOf("Sore Throat", "Cough", "Blocked Nose", "Ear Pain")),
    BodyZoneSpec("CHEST", "Chest & Heart", "છાતી અને હૃદય", "छाती और हृदय", 0.50f, 0.34f, false, listOf("Chest Tightness", "Shortness of Breath", "Palpitations", "Dry Cough")),
    BodyZoneSpec("STOMACH", "Stomach & GI", "પેટ અને પાચન", "पेट और पाचन", 0.50f, 0.49f, false, listOf("Acidity / Gas", "Stomach Pain", "Nausea", "Indigestion")),
    BodyZoneSpec("BACK", "Back & Spine", "કમર અને કરોડરજ્જુ", "पीठ और रीढ़", 0.50f, 0.41f, true, listOf("Lower Back Pain", "Neck Stiffness", "Muscle Spasm", "Spine Ache")),
    BodyZoneSpec("ARMS", "Arms & Joints", "હાથ અને સાંધા", "हाथ और जोड़", 0.25f, 0.42f, false, listOf("Shoulder Pain", "Joint Stiffness", "Wrist Ache", "Numbness")),
    BodyZoneSpec("LEGS", "Legs & Mobility", "પગ અને ઘૂંટણ", "पैर और घुटने", 0.44f, 0.74f, false, listOf("Knee Pain", "Leg Cramps", "Ankle Swelling", "Weakness")),
    BodyZoneSpec("SKIN", "Skin & General", "ચામડી અને સામાન્ય", "त्वचा और सामान्य", 0.75f, 0.42f, false, listOf("Fever / Chills", "Skin Rash", "Body Ache", "Fatigue"))
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UltraBodyMapTab(
    selectedLanguage: String,
    onAnalyzeWithAiTriage: (zoneTitle: String, symptoms: List<String>, severity: String, duration: String, notes: String) -> Unit
) {
    var isBackView by remember { mutableStateOf(false) }
    var selectedZone by remember { mutableStateOf(BODY_ZONES.first()) }
    val selectedSymptoms = remember { mutableStateListOf<String>() }
    var severity by remember { mutableStateOf("Moderate") }
    var duration by remember { mutableStateOf("Today") }
    var notes by remember { mutableStateOf("") }

    val pulseTransition = rememberInfiniteTransition(label = "bodymap_pulse")
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 16f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bodymap_halo"
    )

    UltraGlassCard(modifier = Modifier.fillMaxWidth().testTag("ultra_bodymap_card")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trLang(
                        selectedLanguage,
                        "Interactive Symptom Body Map",
                        "ઇન્ટરેક્ટિવ બોડી મેપ તપાસ",
                        "इंटरैक्टिव बॉडी मैप जांच"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = trLang(
                        selectedLanguage,
                        "Tap the area where you feel discomfort to launch instant AI triage.",
                        "તમને જ્યાં દુખાવો કે તકલીફ હોય તે ભાગ પર ટેપ કરો અને તરત AI સલાહ મેળવો.",
                        "जहाँ दर्द या परेशानी हो उस हिस्से पर टैप करें और तुरंत AI सलाह पाएं।"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    isBackView = !isBackView
                    if (isBackView) {
                        selectedZone = BODY_ZONES.first { it.id == "BACK" }
                        selectedSymptoms.clear()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(44.dp).testTag("ultra_bodymap_flip_btn")
            ) {
                Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isBackView) {
                        trLang(selectedLanguage, "Back", "પાછળ", "पीछे")
                    } else {
                        trLang(selectedLanguage, "Front", "આગળ", "आगे")
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Canvas Human Silhouette
        val bodySilhouetteColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        val bodyStrokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .pointerInput(isBackView) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val nearest = BODY_ZONES.minByOrNull { zone ->
                                val zx = zone.normX * w
                                val zy = zone.normY * h
                                hypot(tapOffset.x - zx, tapOffset.y - zy)
                            }
                            if (nearest != null) {
                                selectedZone = nearest
                                selectedSymptoms.clear()
                            }
                        }
                    }
                    .testTag("ultra_bodymap_canvas")
            ) {
                val w = size.width
                val h = size.height
                val cx = w / 2f

                // Ambient teal glow backdrop
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(UltraCyanEnd.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(cx, h * 0.45f),
                        radius = h * 0.48f
                    ),
                    radius = h * 0.48f,
                    center = Offset(cx, h * 0.45f)
                )

                // Stylized clinical human silhouette (Head, Neck, Torso, Arms, Legs)
                // Head
                drawCircle(
                    color = bodySilhouetteColor,
                    radius = h * 0.085f,
                    center = Offset(cx, h * 0.12f)
                )
                drawCircle(
                    color = bodyStrokeColor,
                    radius = h * 0.085f,
                    center = Offset(cx, h * 0.12f),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Neck
                drawRoundRect(
                    color = bodySilhouetteColor,
                    topLeft = Offset(cx - w * 0.045f, h * 0.20f),
                    size = Size(w * 0.09f, h * 0.05f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // Torso
                drawRoundRect(
                    color = bodySilhouetteColor,
                    topLeft = Offset(cx - w * 0.15f, h * 0.25f),
                    size = Size(w * 0.30f, h * 0.32f),
                    cornerRadius = CornerRadius(24f, 24f)
                )
                drawRoundRect(
                    color = bodyStrokeColor,
                    topLeft = Offset(cx - w * 0.15f, h * 0.25f),
                    size = Size(w * 0.30f, h * 0.32f),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Left & Right Arms
                drawRoundRect(
                    color = bodySilhouetteColor,
                    topLeft = Offset(cx - w * 0.26f, h * 0.27f),
                    size = Size(w * 0.09f, h * 0.28f),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                drawRoundRect(
                    color = bodySilhouetteColor,
                    topLeft = Offset(cx + w * 0.17f, h * 0.27f),
                    size = Size(w * 0.09f, h * 0.28f),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                // Left & Right Legs
                drawRoundRect(
                    color = bodySilhouetteColor,
                    topLeft = Offset(cx - w * 0.12f, h * 0.59f),
                    size = Size(w * 0.10f, h * 0.34f),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                drawRoundRect(
                    color = bodySilhouetteColor,
                    topLeft = Offset(cx + w * 0.02f, h * 0.59f),
                    size = Size(w * 0.10f, h * 0.34f),
                    cornerRadius = CornerRadius(18f, 18f)
                )

                // Draw interactive anatomical zone markers
                BODY_ZONES.forEach { zone ->
                    val center = Offset(zone.normX * w, zone.normY * h)
                    val isSelected = zone.id == selectedZone.id
                    if (isSelected) {
                        drawCircle(
                            color = UltraCyanEnd.copy(alpha = 0.28f),
                            radius = pulseRadius,
                            center = center
                        )
                        drawCircle(
                            color = UltraTealStart,
                            radius = 12.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = center
                        )
                    } else {
                        drawCircle(
                            color = UltraIndigoStart.copy(alpha = 0.55f),
                            radius = 7.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = 3.dp.toPx(),
                            center = center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Direct accessible zone selector chips (guarantees 48dp touch targets & screen reader support)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BODY_ZONES.forEach { zone ->
                val label = trLang(selectedLanguage, zone.titleEn, zone.titleGu, zone.titleHi)
                FilterChip(
                    selected = selectedZone.id == zone.id,
                    onClick = {
                        selectedZone = zone
                        selectedSymptoms.clear()
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("ultra_bodymap_zone_${zone.id.lowercase()}")
                )
            }
        }
    }

    // Selected Zone Details + Symptom Chips + Severity + Launch Triage
    val activeZoneTitle = trLang(selectedLanguage, selectedZone.titleEn, selectedZone.titleGu, selectedZone.titleHi)
    UltraSectionHeader(title = activeZoneTitle, icon = Icons.Default.AccessibilityNew)

    UltraGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = trLang(selectedLanguage, "Select Symptoms", "લક્ષણો પસંદ કરો", "लक्षण चुनें"),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            selectedZone.quickSymptoms.forEach { symptom ->
                val checked = symptom in selectedSymptoms
                FilterChip(
                    selected = checked,
                    onClick = {
                        if (checked) selectedSymptoms.remove(symptom) else selectedSymptoms.add(symptom)
                    },
                    label = { Text(symptom, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = trLang(selectedLanguage, "Severity", "તીવ્રતા", "गंभीरता"),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("Mild", "હળવું", "हल्का"),
                Triple("Moderate", "મધ્યમ", "मध्यम"),
                Triple("Severe", "ગંભીર", "गंभीर")
            ).forEach { (en, gu, hi) ->
                val badgeColor = when (en) {
                    "Severe" -> EmergencyRed
                    "Moderate" -> UltraAmberStart
                    else -> UltraTealStart
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (severity == en) badgeColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    onClick = { severity = en },
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = trLang(selectedLanguage, en, gu, hi),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (severity == en) badgeColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = trLang(selectedLanguage, "Duration", "સમયગાળો", "अवधि"),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Today", "2-3 Days", "1+ Week").forEach { dur ->
                FilterChip(
                    selected = duration == dur,
                    onClick = { duration = dur },
                    label = { Text(dur, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(trLang(selectedLanguage, "Optional Details", "વધારાની વિગત (વૈકલ્પિક)", "अतिरिक्त विवरण (वैकल्पिक)")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("ultra_bodymap_notes_input")
        )

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = {
                val chosen = if (selectedSymptoms.isNotEmpty()) selectedSymptoms.toList() else listOf(selectedZone.quickSymptoms.first())
                onAnalyzeWithAiTriage(selectedZone.titleEn, chosen, severity, duration, notes)
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("ultra_bodymap_analyze_btn")
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = trLang(
                    selectedLanguage,
                    "Analyze with AI Triage",
                    "AI દ્વારા તાત્કાલિક તપાસ કરો",
                    "AI ट्राइएज से जांच करें"
                ),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

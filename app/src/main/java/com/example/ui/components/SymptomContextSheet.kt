package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.tr
import com.example.ui.viewmodel.SymptomDetails

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SymptomContextSheet(
    onDismiss: () -> Unit,
    onSubmit: (SymptomDetails) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var severity by remember { mutableFloatStateOf(5f) }
    var selectedDuration by remember { mutableStateOf("1-2 Days") }
    var selectedLocation by remember { mutableStateOf("General") }
    val associatedSymptoms = remember { mutableStateListOf<String>() }

    val durationOptions = listOf(
        "Few Hours" to tr("Few Hours", "થોડા કલાકો", "कुछ घंटे"),
        "1-2 Days" to tr("1-2 Days", "૧-૨ દિવસ", "1-2 दिन"),
        "3-7 Days" to tr("3-7 Days", "૩-૭ દિવસ", "3-7 दिन"),
        "1-2 Weeks" to tr("1-2 Weeks", "૧-૨ અઠવાડિયા", "1-2 सप्ताह"),
        ">1 Month" to tr(">1 Month", "૧ મહિનાથી વધુ", "1 महीने से अधिक")
    )
    val locationOptions = listOf(
        "Head/Neck" to tr("Head/Neck", "માથું / ગળું", "सिर / गर्दन"),
        "Chest" to tr("Chest", "છાતી", "छाती"),
        "Throat" to tr("Throat", "ગળું", "गला"),
        "Abdomen" to tr("Abdomen", "પેટ", "पेट"),
        "Joints/Limbs" to tr("Joints/Limbs", "સાંધા / હાથ-પગ", "जोड़ / हाथ-पैर"),
        "Skin" to tr("Skin", "ચામડી", "त्वचा"),
        "General" to tr("General", "સામાન્ય / આખું શરીર", "सामान्य / पूरा शरीर")
    )
    val symptomOptions = listOf(
        "Fever/Chills" to tr("Fever/Chills", "તાવ / ઠંડી", "बुखार / ठंड"),
        "Nausea/Vomiting" to tr("Nausea/Vomiting", "ઉબકા / ઉલટી", "मतली / उल्टी"),
        "Dizziness" to tr("Dizziness", "ચક્કર આવવા", "चक्कर आना"),
        "Cough" to tr("Cough", "ઉધરસ", "खांसी"),
        "Rash" to tr("Rash", "ચામડી પર ચકામા", "त्वचा पर चकत्ते"),
        "Shortness of Breath" to tr("Shortness of Breath", "શ્વાસ ચઢવો", "सांस फूलना"),
        "Fatigue" to tr("Fatigue", "થાક લાગવો", "थकान")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FactCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr(
                            "Clinical Context Questionnaire",
                            "લક્ષણોની વિગતવાર પ્રશ્નાવલી",
                            "लक्षण विवरण प्रश्नावली"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = tr("Close", "બંધ કરો", "बंद करें"))
                }
            }

            Text(
                text = tr(
                    "Providing details helps Health Care AI ask targeted clarifying questions and guide care accurately.",
                    "વિગતો આપવાથી AI તમારા લક્ષણો વધુ સારી રીતે સમજીને યોગ્ય માર્ગદર્શન આપી શકશે.",
                    "विवरण देने से AI आपके लक्षणों को बेहतर समझकर सही मार्गदर्शन दे सकेगा।"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Severity Slider
            Text(
                text = tr(
                    "Severity (1 - 10 Scale): ${severity.toInt()} / 10",
                    "તીવ્રતા (૧ થી ૧૦): ${severity.toInt()} / 10",
                    "तीव्रता (1 से 10): ${severity.toInt()} / 10"
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (severity >= 8) EmergencyRed else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when {
                    severity <= 3 -> tr(
                        "Mild: Barely interferes with normal activities",
                        "હળવું: રોજિંદા કામમાં બહુ તકલીફ નથી",
                        "हल्का: दैनिक कार्यों में बहुत कम परेशानी"
                    )
                    severity <= 6 -> tr(
                        "Moderate: Noticeable discomfort, partly limits activities",
                        "મધ્યમ: અગવડતા અનુભવાય છે, કામમાં થોડી અસર",
                        "मध्यम: स्पष्ट असुविधा, काम में थोड़ी बाधा"
                    )
                    severity <= 8 -> tr(
                        "Severe: Strongly interferes with concentration or rest",
                        "તીવ્ર: આરામ કે કામ કરવામાં ભારે તકલીફ",
                        "तीव्र: आराम या काम करने में काफी कठिनाई"
                    )
                    else -> tr(
                        "Very Severe / Unbearable: Seek emergency medical care immediately",
                        "ખૂબ જ ગંભીર / અસહ્ય: તાત્કાલિક ડોક્ટર અથવા ઇમરજન્સી મદદ લો",
                        "अत्यधिक गंभीर / असहनीय: तुरंत आपातकालीन चिकित्सा सहायता लें"
                    )
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (severity >= 8) EmergencyRed else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Slider(
                value = severity,
                onValueChange = { severity = it },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = if (severity >= 8) EmergencyRed else PrimaryTeal,
                    activeTrackColor = if (severity >= 8) EmergencyRed else PrimaryTeal
                ),
                modifier = Modifier.padding(vertical = 4.dp).testTag("severity_slider")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Duration
            Text(
                text = tr("Onset & Duration:", "ક્યારથી તકલીફ છે (સમયગાળો):", "कब से है (अवधि):"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                durationOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedDuration == key,
                        onClick = { selectedDuration = key },
                        label = { Text(label) },
                        leadingIcon = if (selectedDuration == key) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            Text(
                text = tr("Primary Location of Discomfort:", "કઈ જગ્યાએ તકલીફ છે:", "तकलीफ का मुख्य स्थान:"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                locationOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedLocation == key,
                        onClick = { selectedLocation = key },
                        label = { Text(label) },
                        leadingIcon = if (selectedLocation == key) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Associated Symptoms
            Text(
                text = tr("Associated Symptoms:", "અન્ય સાથેના લક્ષણો:", "अन्य जुड़े लक्षण:"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                symptomOptions.forEach { (key, label) ->
                    val isSelected = associatedSymptoms.contains(key)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) associatedSymptoms.remove(key)
                            else associatedSymptoms.add(key)
                        },
                        label = { Text(label) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val details = SymptomDetails(
                        severity = severity.toInt(),
                        duration = selectedDuration,
                        location = selectedLocation,
                        associatedSymptoms = associatedSymptoms.joinToString(", ")
                    )
                    onSubmit(details)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apply_symptom_context_button")
            ) {
                Text(
                    tr("Attach Clinical Context", "લક્ષણની વિગતો જોડો", "लक्षण विवरण जोड़ें"),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

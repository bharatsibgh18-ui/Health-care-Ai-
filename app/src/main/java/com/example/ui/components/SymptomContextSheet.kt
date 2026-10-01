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

    val durationOptions = listOf("Few Hours", "1-2 Days", "3-7 Days", "1-2 Weeks", ">1 Month")
    val locationOptions = listOf("Head/Neck", "Chest", "Throat", "Abdomen", "Joints/Limbs", "Skin", "General")
    val symptomOptions = listOf("Fever/Chills", "Nausea/Vomiting", "Dizziness", "Cough", "Rash", "Shortness of Breath", "Fatigue")

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
                        text = "Clinical Context Questionnaire",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Text(
                text = "Providing details helps MediAssist ask targeted clarifying questions and guide care accurately.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Severity Slider
            Text(
                text = "Severity (1 - 10 Scale): ${severity.toInt()} / 10",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (severity >= 8) EmergencyRed else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when {
                    severity <= 3 -> "Mild: Barely interferes with normal activities"
                    severity <= 6 -> "Moderate: Noticeable discomfort, partly limits activities"
                    severity <= 8 -> "Severe: Strongly interferes with concentration or rest"
                    else -> "Very Severe / Unbearable: Seek emergency medical care immediately"
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
                text = "Onset & Duration:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                durationOptions.forEach { duration ->
                    FilterChip(
                        selected = selectedDuration == duration,
                        onClick = { selectedDuration = duration },
                        label = { Text(duration) },
                        leadingIcon = if (selectedDuration == duration) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            Text(
                text = "Primary Location of Discomfort:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                locationOptions.forEach { loc ->
                    FilterChip(
                        selected = selectedLocation == loc,
                        onClick = { selectedLocation = loc },
                        label = { Text(loc) },
                        leadingIcon = if (selectedLocation == loc) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Associated Symptoms
            Text(
                text = "Associated Symptoms:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                symptomOptions.forEach { sym ->
                    val isSelected = associatedSymptoms.contains(sym)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) associatedSymptoms.remove(sym)
                            else associatedSymptoms.add(sym)
                        },
                        label = { Text(sym) },
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
                Text("Attach Clinical Context", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

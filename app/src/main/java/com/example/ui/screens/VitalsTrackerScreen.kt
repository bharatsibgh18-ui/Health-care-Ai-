package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.VitalLogEntity
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ClinicalAiTab
import com.example.ui.viewmodel.MediAssistViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsTrackerScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val vitalLogs by viewModel.vitalLogs.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        Triple("Blood Pressure", "બીપી (BP)", "बीपी (BP)"),
        Triple("Blood Sugar", "સુગર (Sugar)", "शुगर (Sugar)"),
        Triple("Heart Rate", "હાર્ટ રેટ (HR)", "हृदय गति (HR)"),
        Triple("SpO2", "ઓક્સિજન (SpO2)", "ऑक्सीजन (SpO2)"),
        Triple("Weight", "વજન (Weight)", "वज़न (Weight)"),
        Triple("Sleep", "ઊંઘ (Sleep)", "नींद (Sleep)")
    )

    val safeSelectedTab = selectedTab.coerceIn(0, tabs.lastIndex)
    val currentVitalType = tabs[safeSelectedTab].first
    val currentVitalLabel = trLang(
        selectedLanguage,
        tabs[safeSelectedTab].first,
        tabs[safeSelectedTab].second,
        tabs[safeSelectedTab].third
    )

    val filteredLogs = remember(vitalLogs, currentVitalType) {
        vitalLogs.filter { it.vitalType.equals(currentVitalType, ignoreCase = true) }
    }

    if (showAddDialog) {
        AddVitalDialog(
            vitalType = currentVitalType,
            vitalLabel = currentVitalLabel,
            language = selectedLanguage,
            onDismiss = { showAddDialog = false },
            onSave = { pVal, sVal, unit, status, notes ->
                viewModel.recordVitalLog(
                    type = currentVitalType,
                    primaryValue = pVal,
                    secondaryValue = sVal,
                    unit = unit,
                    status = status,
                    notes = notes
                )
                showAddDialog = false
                Toast.makeText(
                    context,
                    trLang(
                        selectedLanguage,
                        "Vital Logged Successfully",
                        "વાઇટલ સફળતાપૂર્વક સેવ થયું",
                        "वाइटल सफलतापूर्वक सहेजा गया"
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MonitorHeart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = tr("Smart Vitals Tracker", "સ્માર્ટ હેલ્થ વાઇટલ્સ ટ્રેકર", "स्मार्ट हेल्थ वाइटल्स ट्रैकर"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr(
                                    "BP, Sugar, Heart Rate, SpO2 & Sleep",
                                    "બીપી, સુગર, હાર્ટ રેટ, ઓક્સિજન અને ઊંઘ",
                                    "बीपी, शुगर, हृदय गति, ऑक्सीजन और नींद"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.TRIAGE_CHAT) },
                        modifier = Modifier.testTag("vitals_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "પાછા જાઓ", "वापस जाएं")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openClinicalAiSuite(ClinicalAiTab.REMOTE_MONITORING) },
                        modifier = Modifier.testTag("vitals_open_wearable_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = tr("Connect Wearable", "સ્માર્ટવોચ જોડો", "वियरेबल कनेक्ट करें"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_vital_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr(
                            "Log ${tabs[safeSelectedTab].first.split(" ").first()}",
                            "નોંધ ઉમેરો",
                            "दर्ज करें"
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Single ScrollableTabRow guarantees selectedTabIndex is always within bounds (0..5)
            ScrollableTabRow(
                selectedTabIndex = safeSelectedTab,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { idx, (eng, gu, hi) ->
                    Tab(
                        selected = safeSelectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = {
                            Text(
                                text = tr(eng, gu, hi),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("vitals_tab_$idx")
                    )
                }
            }

            // Overview Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (currentVitalType) {
                        "Blood Pressure" -> Icons.Default.Favorite
                        "Blood Sugar" -> Icons.Default.WaterDrop
                        "Heart Rate" -> Icons.Default.MonitorHeart
                        "SpO2" -> Icons.Default.Psychology
                        "Weight" -> Icons.Default.Scale
                        else -> Icons.Default.Bedtime
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    val latestLog = filteredLogs.firstOrNull()
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (latestLog != null) {
                                val displayUnit = if (latestLog.unit == "hours") tr("hours", "કલાક", "घंटे") else latestLog.unit
                                val valueText = if (latestLog.secondaryValue > 0) {
                                    "${latestLog.primaryValue.toInt()}/${latestLog.secondaryValue.toInt()} $displayUnit"
                                } else {
                                    "${formatVitalNumber(latestLog.primaryValue)} $displayUnit"
                                }
                                tr("Latest: $valueText", "તાજેતરનું: $valueText", "नवीनतम: $valueText")
                            } else {
                                tr(
                                    "No $currentVitalLabel Logs",
                                    "$currentVitalLabel ની કોઈ નોંધ નથી",
                                    "$currentVitalLabel का कोई रिकॉर्ड नहीं"
                                )
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val localizedStatus = latestLog?.let {
                            when (it.status) {
                                "Normal" -> tr("Normal", "સામાન્ય", "सामान्य")
                                "Elevated" -> tr("Elevated", "વધારે", "बढ़ा हुआ")
                                "Low SpO2" -> tr("Low SpO2", "ઓછું SpO2", "कम SpO2")
                                else -> it.status
                            }
                        } ?: ""
                        Text(
                            text = if (latestLog != null) {
                                tr(
                                    "Status: $localizedStatus • ${filteredLogs.size} Total Entries",
                                    "સ્થિતિ: $localizedStatus • કુલ ${filteredLogs.size} નોંધ",
                                    "स्थिति: $localizedStatus • कुल ${filteredLogs.size} लॉग"
                                )
                            } else {
                                tr(
                                    "Tap + below to start logging your $currentVitalLabel trends",
                                    "નવું $currentVitalLabel રીડિંગ ઉમેરવા નીચે + બટન દબાવો",
                                    "नया $currentVitalLabel रीडिंग जोड़ने के लिए नीचे + बटन दबाएं"
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Logs List
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tr(
                                "No logs for $currentVitalLabel yet.",
                                "હજી સુધી $currentVitalLabel માટે કોઈ નોંધ નથી.",
                                "अभी तक $currentVitalLabel के लिए कोई लॉग नहीं है।"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tr(
                                "Track your daily levels to detect trends and share reports with your physician.",
                                "તમારા રોજિંદા રીડિંગ નોંધો જેથી ડૉક્ટરને બતાવવામાં સરળતા રહે.",
                                "अपने दैनिक रीडिंग दर्ज करें ताकि डॉक्टर को दिखाना आसान हो।"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        VitalLogCard(log = log, onDelete = { viewModel.deleteVitalLog(log) })
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

private fun formatVitalNumber(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
}

@Composable
private fun VitalLogCard(log: VitalLogEntity, onDelete: () -> Unit) {
    val dateStr = remember(log.timestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().testTag("vital_log_card_${log.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val displayUnit = if (log.unit == "hours") tr("hours", "કલાક", "घंटे") else log.unit
                    Text(
                        text = if (log.secondaryValue > 0) {
                            "${log.primaryValue.toInt()}/${log.secondaryValue.toInt()} $displayUnit"
                        } else {
                            "${formatVitalNumber(log.primaryValue)} $displayUnit"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (log.status == "Normal") SafeMint.copy(alpha = 0.2f) else EmergencyRed.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = when (log.status) {
                                "Normal" -> tr("Normal", "સામાન્ય", "सामान्य")
                                "Elevated" -> tr("Elevated", "વધારે", "बढ़ा हुआ")
                                "Low SpO2" -> tr("Low SpO2", "ઓછું SpO2", "कम SpO2")
                                else -> log.status
                            },
                            color = if (log.status == "Normal") Color(0xFF1B5E20) else EmergencyRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dateStr ${if (log.notes.isNotBlank()) "• ${log.notes}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = tr("Delete", "કાઢી નાખો", "हटाएं"),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddVitalDialog(
    vitalType: String,
    vitalLabel: String,
    language: String,
    onDismiss: () -> Unit,
    onSave: (Double, Double, String, String, String) -> Unit
) {
    var primaryInput by remember(vitalType) {
        mutableStateOf(
            when (vitalType) {
                "Blood Pressure" -> "120"
                "Blood Sugar" -> "95"
                "Heart Rate" -> "72"
                "SpO2" -> "98"
                "Weight" -> "68"
                "Sleep" -> "7.5"
                else -> "0"
            }
        )
    }
    var secondaryInput by remember(vitalType) { mutableStateOf("80") }
    var notes by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf(false) }

    val defaultUnit = when (vitalType) {
        "Blood Pressure" -> "mmHg"
        "Blood Sugar" -> "mg/dL"
        "Heart Rate" -> "bpm"
        "SpO2" -> "%"
        "Weight" -> "kg"
        "Sleep" -> "hours"
        else -> ""
    }
    val displayUnit = if (defaultUnit == "hours") trLang(language, "hours", "કલાક", "घंटे") else defaultUnit

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = trLang(
                    language,
                    "Log $vitalLabel Reading",
                    "$vitalLabel ની નોંધ ઉમેરો",
                    "$vitalLabel रीडिंग दर्ज करें"
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (vitalType == "Blood Pressure") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = primaryInput,
                            onValueChange = {
                                primaryInput = it
                                inputError = false
                            },
                            label = { Text(trLang(language, "Systolic (120)", "ઉપરનું (Systolic)", "सिस्टोलिक (120)")) },
                            singleLine = true,
                            isError = inputError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("vital_primary_input")
                        )
                        OutlinedTextField(
                            value = secondaryInput,
                            onValueChange = {
                                secondaryInput = it
                                inputError = false
                            },
                            label = { Text(trLang(language, "Diastolic (80)", "નીચેનું (Diastolic)", "डायस्टोलिक (80)")) },
                            singleLine = true,
                            isError = inputError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("vital_secondary_input")
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = primaryInput,
                        onValueChange = {
                            primaryInput = it
                            inputError = false
                        },
                        label = { Text(trLang(language, "Value ($displayUnit)", "માપ ($displayUnit)", "मान ($displayUnit)")) },
                        singleLine = true,
                        isError = inputError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("vital_primary_input")
                    )
                }

                if (inputError) {
                    Text(
                        text = trLang(
                            language,
                            "Please enter a valid number.",
                            "કૃપા કરીને સાચો નંબર દાખલ કરો.",
                            "कृपया एक मान्य संख्या दर्ज करें।"
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = {
                        Text(
                            trLang(
                                language,
                                "Notes (e.g. Fasting, After meal)",
                                "નોંધ (દા.ત. જમ્યા પહેલાં, ચાલ્યા પછી)",
                                "नोट्स (जैसे खाली पेट, खाने के बाद)"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("vital_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = primaryInput.trim().toDoubleOrNull()
                    val s = if (vitalType == "Blood Pressure") secondaryInput.trim().toDoubleOrNull() else 0.0
                    if (p == null || p <= 0.0 || (vitalType == "Blood Pressure" && (s == null || s <= 0.0))) {
                        inputError = true
                        return@Button
                    }
                    val safeSecondary = s ?: 0.0
                    val status = when (vitalType) {
                        "Blood Pressure" -> if (p in 90.0..129.0 && safeSecondary in 60.0..84.0) "Normal" else "Elevated"
                        "Blood Sugar" -> if (p in 70.0..140.0) "Normal" else "Elevated"
                        "Heart Rate" -> if (p in 60.0..100.0) "Normal" else "Elevated"
                        "SpO2" -> if (p >= 95.0) "Normal" else "Low SpO2"
                        else -> "Normal"
                    }
                    onSave(p, safeSecondary, defaultUnit, status, notes.trim())
                },
                modifier = Modifier.testTag("vital_save_button")
            ) {
                Text(trLang(language, "Save", "સેવ કરો", "सहेजें"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(trLang(language, "Cancel", "રદ કરો", "रद्द करें"))
            }
        }
    )
}

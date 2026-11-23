package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.SymptomLogEntity
import com.example.data.local.SymptomNoteEntity
import com.example.ui.components.ClinicalDataExportDialog
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailySymptomLoggerScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val symptomLogs by viewModel.symptomLogs.collectAsStateWithLifecycle()
    val allNotes by viewModel.symptomNotes.collectAsStateWithLifecycle()
    val adherenceLogs by viewModel.allAdherenceLogs.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val todayDateKey = viewModel.todayDateString
    val context = LocalContext.current

    var symptomTitle by remember { mutableStateOf("") }
    var severityValue by remember { mutableFloatStateOf(5f) }
    var selectedLocation by remember { mutableStateOf("Head") }
    var selectedStatus by remember { mutableStateOf("Ongoing") }
    var noteInput by remember { mutableStateOf("") }
    var showComposer by remember { mutableStateOf(true) }
    var showExportDialog by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf("ALL") }
    var noteTargetSymptom by remember { mutableStateOf<SymptomLogEntity?>(null) }

    val notesBySymptom = remember(allNotes) {
        allNotes.groupBy { it.symptomLogId }
    }

    val todayCount = remember(symptomLogs, todayDateKey) {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        symptomLogs.count { log ->
            val key = log.dateKey.ifBlank { dateFmt.format(Date(log.timestamp)) }
            key == todayDateKey
        }
    }

    val filteredSymptoms = remember(symptomLogs, activeFilter, todayDateKey) {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        when (activeFilter) {
            "TODAY" -> symptomLogs.filter {
                val key = it.dateKey.ifBlank { dateFmt.format(Date(it.timestamp)) }
                key == todayDateKey
            }
            "SEVERE" -> symptomLogs.filter { it.severity >= 7 }
            "ONGOING" -> symptomLogs.filter { it.status == "Ongoing" }
            else -> symptomLogs
        }
    }

    val avgSeverity = remember(symptomLogs) {
        if (symptomLogs.isEmpty()) 0.0 else symptomLogs.map { it.severity }.average()
    }

    Scaffold(
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
                                    imageVector = Icons.Default.Healing,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = tr(
                                    "Daily Symptom Logger",
                                    "દૈનિક લક્ષણ નોંધણી",
                                    "दैनिक लक्षण लॉगर"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr(
                                    "Room Persistence • Severity & Timestamped Notes",
                                    "Room ડેટાબેઝ • તીવ્રતા અને સમય-નોંધ",
                                    "Room डेटाबेस • गंभीरता और समय-नोट्स"
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
                        modifier = Modifier.testTag("symptom_logger_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "પાછા જાઓ", "वापस जाएं")
                        )
                    }
                },
                actions = {
                    LanguageSelector(
                        viewModel = viewModel,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("symptom_logger_export_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = tr("Export CSV / JSON", "CSV / JSON એક્સપોર્ટ", "CSV / JSON एक्सपोर्ट"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.DEVELOPER_LOGS) },
                        modifier = Modifier.testTag("symptom_logger_dev_logs_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = tr("Developer Logs", "ડેવલપર લોગ્સ", "डेवलपर लॉग्स"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Daily Summary Header Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_symptom_summary_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    "Today ($todayDateKey)",
                                    "આજની તારીખ ($todayDateKey)",
                                    "आज की तारीख ($todayDateKey)"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tr(
                                    "$todayCount logged today • ${symptomLogs.size} total • ${allNotes.size} notes",
                                    "આજે $todayCount નોંધ • કુલ ${symptomLogs.size} • ${allNotes.size} નોટ્સ",
                                    "आज $todayCount दर्ज • कुल ${symptomLogs.size} • ${allNotes.size} नोट्स"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Avg severity: ${"%.1f".format(avgSeverity)} / 10",
                                    "સરેરાશ તીવ્રતા: ${"%.1f".format(avgSeverity)} / 10",
                                    "औसत गंभीरता: ${"%.1f".format(avgSeverity)} / 10"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { showComposer = !showComposer },
                            modifier = Modifier.testTag("toggle_symptom_composer_btn")
                        ) {
                            Icon(
                                imageVector = if (showComposer) Icons.Default.ExpandLess else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showComposer) {
                                    tr("Hide", "છુપાવો", "छुपाएं")
                                } else {
                                    tr("Log", "નોંધો", "दर्ज करें")
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Daily Symptom & Severity Composer Form
            item {
                AnimatedVisibility(visible = showComposer) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_symptom_composer_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = tr(
                                    "Log Today's Symptom",
                                    "આજનું લક્ષણ નોંધો",
                                    "आज का लक्षण दर्ज करें"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = symptomTitle,
                                onValueChange = { symptomTitle = it },
                                label = { Text(tr("Symptom Name *", "લક્ષણનું નામ *", "लक्षण का नाम *")) },
                                placeholder = {
                                    Text(tr("e.g. Migraine, Fever, Sore Throat", "દા.ત. માથાનો દુખાવો, તાવ", "उदा. सिरदर्द, बुखार"))
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("daily_symptom_title_input")
                            )

                            // Quick common symptoms presets
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Headache", "Fever", "Fatigue", "Cough", "Stomach Pain", "Dizziness").forEach { preset ->
                                    SuggestionChip(
                                        onClick = { symptomTitle = preset },
                                        label = { Text(preset, fontSize = 11.sp) }
                                    )
                                }
                            }

                            // Severity Slider + Level Indicator
                            val sevInt = severityValue.toInt()
                            val (sevColor, sevTier) = when {
                                sevInt >= 8 -> EmergencyRed to tr("Severe", "ગંભીર", "गंभीर")
                                sevInt >= 4 -> MaterialTheme.colorScheme.tertiary to tr("Moderate", "મધ્યમ", "मध्यम")
                                else -> SafeMint to tr("Mild", "સામાન્ય", "हल्का")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr("Severity Level", "તીવ્રતાનું સ્તર", "गंभीरता स्तर"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = sevColor.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "$sevInt / 10 ($sevTier)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = sevColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Slider(
                                value = severityValue,
                                onValueChange = { severityValue = it },
                                valueRange = 1f..10f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = sevColor,
                                    activeTrackColor = sevColor
                                ),
                                modifier = Modifier.testTag("daily_symptom_severity_slider")
                            )

                            // Quick severity level buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2 to "Mild (2)", 5 to "Mod (5)", 8 to "Severe (8)").forEach { (lvl, lbl) ->
                                    FilterChip(
                                        selected = sevInt == lvl,
                                        onClick = { severityValue = lvl.toFloat() },
                                        label = { Text(lbl, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Body Location Chips
                            Text(
                                text = tr("Body Area & Status:", "શરીરનો ભાગ અને સ્થિતિ:", "शरीर का भाग और स्थिति:"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Head", "Throat", "Chest", "Stomach", "General").forEach { loc ->
                                    FilterChip(
                                        selected = selectedLocation == loc,
                                        onClick = { selectedLocation = loc },
                                        label = { Text(loc, fontSize = 11.sp) }
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Ongoing", "Improving", "Resolved").forEach { st ->
                                    FilterChip(
                                        selected = selectedStatus == st,
                                        onClick = { selectedStatus = st },
                                        label = { Text(st, fontSize = 11.sp) }
                                    )
                                }
                            }

                            val nowLabel = remember {
                                SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date())
                            }
                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                label = {
                                    Text(
                                        tr(
                                            "Timestamped Note ($nowLabel)",
                                            "સમય સાથે નોંધ ($nowLabel)",
                                            "समय के साथ नोट ($nowLabel)"
                                        )
                                    )
                                },
                                placeholder = {
                                    Text(
                                        tr(
                                            "Triggers, food, sleep, or medicine reaction...",
                                            "કારણો, ખોરાક, ઊંઘ અથવા દવાની અસર લખો...",
                                            "लक्षण का कारण, भोजन, नींद या दवा का असर लिखें..."
                                        )
                                    )
                                },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("daily_symptom_note_input")
                            )

                            Button(
                                onClick = {
                                    if (symptomTitle.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            trLang(
                                                selectedLanguage,
                                                "Please enter a symptom name",
                                                "કૃપા કરીને લક્ષણનું નામ લખો",
                                                "कृपया लक्षण का नाम दर्ज करें"
                                            ),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        viewModel.logNewSymptom(
                                            title = symptomTitle,
                                            severity = sevInt,
                                            location = selectedLocation,
                                            duration = "Today",
                                            notes = noteInput,
                                            status = selectedStatus
                                        )
                                        symptomTitle = ""
                                        noteInput = ""
                                        Toast.makeText(
                                            context,
                                            trLang(
                                                selectedLanguage,
                                                "Symptom saved to Room database!",
                                                "લક્ષણ Room ડેટાબેઝમાં સેવ થયું!",
                                                "लक्षण Room डेटाबेस में सहेजा गया!"
                                            ),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("daily_symptom_save_btn")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr("Save Daily Symptom", "દૈનિક લક્ષણ સેવ કરો", "दैनिक लक्षण सहेजें"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Filter Chips
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "ALL" to tr("All (${symptomLogs.size})", "બધા (${symptomLogs.size})", "सभी (${symptomLogs.size})"),
                        "TODAY" to tr("Today ($todayCount)", "આજ ($todayCount)", "आज ($todayCount)"),
                        "SEVERE" to tr("Severe 7+", "ગંભીર 7+", "गंभीर 7+"),
                        "ONGOING" to tr("Ongoing", "ચાલુ", "जारी")
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = activeFilter == key,
                            onClick = { activeFilter = key },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.testTag("daily_symptom_filter_$key")
                        )
                    }
                }
            }

            if (filteredSymptoms.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = tr(
                                    "No symptoms logged for this filter",
                                    "આ ફિલ્ટર માટે કોઈ લક્ષણ નોંધાયેલ નથી",
                                    "इस फ़िल्टर के लिए कोई लक्षण दर्ज नहीं है"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                items(filteredSymptoms, key = { it.id }) { log ->
                    DailySymptomEntryCard(
                        log = log,
                        notes = notesBySymptom[log.id].orEmpty(),
                        onAddNote = { noteTargetSymptom = log },
                        onDeleteNote = { viewModel.deleteSymptomNote(it) },
                        onStatusChange = { newStatus -> viewModel.updateSymptomStatus(log, newStatus) },
                        onDeleteLog = { viewModel.deleteSymptomLog(log) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showExportDialog) {
        ClinicalDataExportDialog(
            adherenceCount = adherenceLogs.size,
            symptomCount = symptomLogs.size,
            noteCount = allNotes.size,
            generatePreview = { format -> viewModel.generateClinicalDataExport(format) },
            onSaveFile = { format -> viewModel.saveClinicalDataExport(context, format) },
            onShareFile = { format -> viewModel.shareClinicalDataExport(context, format) },
            onDismiss = { showExportDialog = false }
        )
    }

    // Add Timestamped Follow-Up Note Dialog
    noteTargetSymptom?.let { targetLog ->
        var followUpNote by remember { mutableStateOf("") }
        var followUpSeverity by remember { mutableFloatStateOf(targetLog.severity.toFloat()) }
        AlertDialog(
            onDismissRequest = { noteTargetSymptom = null },
            title = {
                Text(
                    text = tr(
                        "Add Timestamped Note • ${targetLog.symptomTitle}",
                        "સમય-નોંધ ઉમેરો • ${targetLog.symptomTitle}",
                        "नोट जोड़ें • ${targetLog.symptomTitle}"
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = tr(
                            "Current Severity Snapshot: ${followUpSeverity.toInt()} / 10",
                            "હાલની તીવ્રતા: ${followUpSeverity.toInt()} / 10",
                            "वर्तमान गंभीरता: ${followUpSeverity.toInt()} / 10"
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = followUpSeverity,
                        onValueChange = { followUpSeverity = it },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    OutlinedTextField(
                        value = followUpNote,
                        onValueChange = { followUpNote = it },
                        label = { Text(tr("Follow-up Note *", "ફોલો-અપ નોંધ *", "फ़ॉलो-अप नोट *")) },
                        placeholder = { Text(tr("e.g. Pain decreased after rest", "દા.ત. આરામ પછી દુખાવો ઘટ્યો", "उदा. आराम के बाद दर्द कम हुआ")) },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_dialog_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (followUpNote.isNotBlank()) {
                            viewModel.addTimestampedSymptomNote(
                                symptomLogId = targetLog.id,
                                noteText = followUpNote,
                                severitySnapshot = followUpSeverity.toInt()
                            )
                            noteTargetSymptom = null
                        }
                    },
                    enabled = followUpNote.isNotBlank(),
                    modifier = Modifier.testTag("note_dialog_save_btn")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { noteTargetSymptom = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun DailySymptomEntryCard(
    log: SymptomLogEntity,
    notes: List<SymptomNoteEntity>,
    onAddNote: () -> Unit,
    onDeleteNote: (SymptomNoteEntity) -> Unit,
    onStatusChange: (String) -> Unit,
    onDeleteLog: () -> Unit
) {
    var expandedNotes by remember { mutableStateOf(true) }
    val headerFmt = remember { SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()) }
    val noteFmt = remember { SimpleDateFormat("dd MMM • hh:mm:ss a", Locale.getDefault()) }

    val sevColor = when {
        log.severity >= 8 -> EmergencyRed
        log.severity >= 4 -> MaterialTheme.colorScheme.tertiary
        else -> SafeMint
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_symptom_card_${log.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.symptomTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = headerFmt.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = sevColor
                ) {
                    Text(
                        text = "${log.severity}/10",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteLog,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("daily_symptom_delete_${log.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "📍 ${log.bodyLocation}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = log.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (log.notesOrTriggers.isNotBlank() && notes.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = log.notesOrTriggers,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(
                            "Timestamped Notes (${notes.size})",
                            "સમય સાથેની નોંધ (${notes.size})",
                            "समय-अंकित नोट्स (${notes.size})"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { expandedNotes = !expandedNotes }) {
                        Icon(
                            imageVector = if (expandedNotes) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(if (expandedNotes) "Hide" else "Show", fontSize = 11.sp)
                    }
                }

                AnimatedVisibility(visible = expandedNotes) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        notes.forEach { note ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("symptom_note_item_${note.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${noteFmt.format(Date(note.timestamp))} • Severity ${note.severitySnapshot}/10",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = note.noteText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteNote(note) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.action_delete),
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Improving", "Resolved").forEach { st ->
                        if (log.status != st) {
                            TextButton(onClick = { onStatusChange(st) }) {
                                Text(st, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onAddNote,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_note_btn_${log.id}")
                ) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Add Note", "નોંધ ઉમેરો", "नोट जोड़ें"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

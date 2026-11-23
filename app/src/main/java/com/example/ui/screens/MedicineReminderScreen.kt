package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.MedicationAdherenceEntity
import com.example.data.local.MedicineEntity
import com.example.ui.components.ClinicalDataExportDialog
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SafeMint
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineReminderScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val medicines by viewModel.medicines.collectAsStateWithLifecycle()
    val todayAdherenceLogs by viewModel.todayAdherenceLogs.collectAsStateWithLifecycle()
    val allAdherenceLogs by viewModel.allAdherenceLogs.collectAsStateWithLifecycle()
    val symptomLogs by viewModel.symptomLogs.collectAsStateWithLifecycle()
    val symptomNotes by viewModel.symptomNotes.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var historyFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var medicineToEdit by remember { mutableStateOf<MedicineEntity?>(null) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        val msg = if (granted) {
            "Notifications enabled for medication reminders"
        } else {
            "Allow notification permission to receive scheduled pill alerts"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    val activeMedicinesCount = remember(medicines) { medicines.count { it.isActive } }
    val todayStatusMap = remember(todayAdherenceLogs) {
        todayAdherenceLogs.associateBy { it.medicineId }
    }
    val takenTodayCount = remember(medicines, todayStatusMap) {
        medicines.count { it.isActive && todayStatusMap[it.id]?.status == "TAKEN" }
    }
    val todayProgress = if (activeMedicinesCount > 0) {
        (takenTodayCount.toFloat() / activeMedicinesCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedTodayProgress by animateFloatAsState(
        targetValue = todayProgress,
        label = "medTodayProgress"
    )

    val takenTotal = remember(allAdherenceLogs) { allAdherenceLogs.count { it.status == "TAKEN" } }
    val skippedTotal = remember(allAdherenceLogs) { allAdherenceLogs.count { it.status == "SKIPPED" } }
    val snoozedTotal = remember(allAdherenceLogs) { allAdherenceLogs.count { it.status == "SNOOZED" } }
    val overallRate = remember(takenTotal, skippedTotal) {
        val denom = takenTotal + skippedTotal
        if (denom > 0) ((takenTotal * 100f) / denom).toInt() else 100
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
                                    imageVector = Icons.Default.Medication,
                                    contentDescription = tr("Medicine", "દવા", "दवा"),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.medicine_reminder_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.medicine_reminder_subtitle),
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
                        modifier = Modifier.testTag("med_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "પાછા જાઓ", "वापस जाएं")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("med_export_topbar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = tr("Export CSV / JSON", "CSV / JSON એક્સપોર્ટ", "CSV / JSON एक्सपोर्ट"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    LanguageSelector(
                        viewModel = viewModel,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(
                        onClick = { viewModel.toggleThemeMode() },
                        modifier = Modifier.testTag("med_theme_toggle_button")
                    ) {
                        val (themeIcon, themeName) = when (themeMode) {
                            ThemeMode.DARK -> Icons.Default.DarkMode to stringResource(R.string.theme_dark)
                            ThemeMode.LIGHT -> Icons.Default.LightMode to stringResource(R.string.theme_light)
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto to stringResource(R.string.theme_system)
                        }
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = themeName,
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
                modifier = Modifier.testTag("add_medicine_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.add_medicine), fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tr(
                                "Enable notification permission so scheduled pill alerts arrive on time.",
                                "સમયસર દવાના એલર્ટ મેળવવા માટે નોટિફિકેશન પરવાનગી આપો.",
                                "समय पर दवा अलर्ट पाने के लिए नोटिफिकेशन अनुमति चालू करें।"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("med_enable_notifications_btn")
                        ) {
                            Text(tr("Enable", "ચાલુ કરો", "चालू करें"), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Tab Switcher: Schedule & Today vs Adherence History
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = tr(
                                "Schedule (${medicines.size})",
                                "શેડ્યૂલ (${medicines.size})",
                                "शेड्यूल (${medicines.size})"
                            ),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("med_tab_schedule")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = tr(
                                "Adherence (${allAdherenceLogs.size})",
                                "ઇતિહાસ (${allAdherenceLogs.size})",
                                "इतिहास (${allAdherenceLogs.size})"
                            ),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("med_tab_history")
                )
            }

            if (selectedTab == 0) {
                // Hero Adherence + WorkManager Alerts Summary Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("med_adherence_hero_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.medicine_automated_alerts_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = tr(
                                        "$activeMedicinesCount active WorkManager schedules • $overallRate% adherence",
                                        "$activeMedicinesCount સક્રિય WorkManager શેડ્યૂલ • $overallRate% નિયમિતતા",
                                        "$activeMedicinesCount सक्रिय WorkManager शेड्यूल • $overallRate% नियमितता"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.triggerMedicineTestNotification()
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_med_notification_sent),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_test_medicine_alert")
                            ) {
                                Text(tr("Test", "ટેસ્ટ", "टेस्ट"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (activeMedicinesCount > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr(
                                        "Today's Adherence: $takenTodayCount / $activeMedicinesCount doses taken",
                                        "આજની નિયમિતતા: $takenTodayCount / $activeMedicinesCount ડોઝ લીધા",
                                        "आज की नियमितता: $takenTodayCount / $activeMedicinesCount खुराक ली गई"
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(todayProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryTeal
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { animatedTodayProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = PrimaryTeal,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }

                if (medicines.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Medication,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = stringResource(R.string.medicine_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.medicine_empty_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.testTag("add_first_medicine_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.add_daily_medicine))
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(medicines, key = { it.id }) { med ->
                            val todayLog = todayStatusMap[med.id]
                            MedicineCard(
                                medicine = med,
                                todayStatus = todayLog?.status,
                                onMarkTaken = {
                                    viewModel.recordMedicineAdherence(med, "TAKEN")
                                    Toast.makeText(
                                        context,
                                        trLang(
                                            selectedLanguage,
                                            "${med.name} marked as Taken",
                                            "${med.name} લીધેલ તરીકે નોંધાઈ",
                                            "${med.name} ली गई के रूप में दर्ज"
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onSnooze = {
                                    viewModel.recordMedicineAdherence(med, "SNOOZED", "Snoozed for 15 minutes")
                                    Toast.makeText(
                                        context,
                                        trLang(
                                            selectedLanguage,
                                            "${med.name} snoozed for 15 min",
                                            "${med.name} 15 મિનિટ માટે સ્નૂઝ કર્યું",
                                            "${med.name} 15 मिनट के लिए स्नूज़ किया गया"
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onSkip = {
                                    viewModel.recordMedicineAdherence(med, "SKIPPED")
                                    Toast.makeText(
                                        context,
                                        trLang(
                                            selectedLanguage,
                                            "${med.name} logged as Skipped",
                                            "${med.name} છોડેલ તરીકે નોંધાઈ",
                                            "${med.name} छोड़ी गई के रूप में दर्ज"
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onToggleActive = { viewModel.toggleMedicineActive(med) },
                                onAdjustTime = { medicineToEdit = med },
                                onDelete = {
                                    viewModel.deleteMedicine(med)
                                    val msg = context.getString(R.string.medicine_deleted_format, med.name)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            } else {
                // Adherence History Tab
                AdherenceHistoryContent(
                    logs = allAdherenceLogs,
                    filter = historyFilter,
                    onFilterChange = { historyFilter = it },
                    takenCount = takenTotal,
                    skippedCount = skippedTotal,
                    snoozedCount = snoozedTotal,
                    overallRate = overallRate,
                    onExportClick = { showExportDialog = true },
                    onDeleteLog = { viewModel.deleteAdherenceLog(it) },
                    onClearAll = { viewModel.clearAllAdherenceHistory() }
                )
            }
        }
    }

    if (showExportDialog) {
        ClinicalDataExportDialog(
            adherenceCount = allAdherenceLogs.size,
            symptomCount = symptomLogs.size,
            noteCount = symptomNotes.size,
            generatePreview = { format -> viewModel.generateClinicalDataExport(format) },
            onSaveFile = { format -> viewModel.saveClinicalDataExport(context, format) },
            onShareFile = { format -> viewModel.shareClinicalDataExport(context, format) },
            onDismiss = { showExportDialog = false }
        )
    }

    // Add Medicine Dialog
    if (showAddDialog) {
        AdjustableMedicineDialog(
            isEditMode = false,
            initialName = "",
            initialDosage = tr("1 Tablet", "૧ ગોળી", "1 गोली"),
            initialTime = "08:00 AM",
            initialInstruction = stringResource(R.string.medicine_inst_after_food),
            initialFrequencyHours = 24,
            initialPrescribedBy = "",
            onDismiss = { showAddDialog = false },
            onSave = { name, dosage, time, instruction, frequencyHours, prescribedBy ->
                viewModel.addMedicine(
                    name = name,
                    dosage = dosage,
                    timeOfDay = time,
                    instruction = instruction,
                    frequencyHours = frequencyHours,
                    prescribedBy = prescribedBy
                )
                Toast.makeText(context, context.getString(R.string.medicine_saved), Toast.LENGTH_SHORT).show()
                showAddDialog = false
            }
        )
    }

    // Edit & Adjust Medicine Dialog
    medicineToEdit?.let { med ->
        AdjustableMedicineDialog(
            isEditMode = true,
            initialName = med.name,
            initialDosage = med.dosage,
            initialTime = med.timeOfDay,
            initialInstruction = med.instruction,
            initialFrequencyHours = med.frequencyHours,
            initialPrescribedBy = med.prescribedBy,
            onDismiss = { medicineToEdit = null },
            onSave = { name, dosage, time, instruction, frequencyHours, prescribedBy ->
                viewModel.updateMedicineTimeAndDetails(
                    medicine = med,
                    newTime = time,
                    newName = name,
                    newDosage = dosage,
                    newInstruction = instruction,
                    newFrequencyHours = frequencyHours,
                    newPrescribedBy = prescribedBy
                )
                Toast.makeText(context, context.getString(R.string.medicine_updated), Toast.LENGTH_SHORT).show()
                medicineToEdit = null
            }
        )
    }
}

@Composable
fun MedicineCard(
    medicine: MedicineEntity,
    todayStatus: String? = null,
    onMarkTaken: () -> Unit = {},
    onSnooze: () -> Unit = {},
    onSkip: () -> Unit = {},
    onToggleActive: () -> Unit,
    onAdjustTime: () -> Unit,
    onDelete: () -> Unit
) {
    val freqLabel = when (medicine.frequencyHours) {
        8 -> tr("Every 8h", "દર 8 કલાકે", "हर 8 घंटे")
        12 -> tr("Every 12h", "દર 12 કલાકે", "हर 12 घंटे")
        else -> tr("Daily", "દૈનિક", "दैनिक")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (medicine.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (medicine.isActive) 2.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("medicine_card_${medicine.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (medicine.isActive) PrimaryTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = if (medicine.isActive) PrimaryTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = medicine.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = medicine.dosage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${medicine.timeOfDay} • $freqLabel",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = medicine.instruction,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (medicine.prescribedBy.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalPharmacy,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Rx: ${medicine.prescribedBy}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Switch(
                    checked = medicine.isActive,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.testTag("med_active_switch_${medicine.id}")
                )
            }

            if (todayStatus != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AdherenceStatusBadge(status = todayStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Adherence Tracking Actions (Taken / Snooze 15m / Skip)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onMarkTaken,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (todayStatus == "TAKEN") SafeMint.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (todayStatus == "TAKEN") PrimaryTeal else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("med_mark_taken_btn_${medicine.id}")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Taken", "લીધી", "ली गई"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSnooze,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("med_snooze_btn_${medicine.id}")
                ) {
                    Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("15m", "15મિ", "15मि"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onSkip,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("med_skip_btn_${medicine.id}")
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Skip", "છોડો", "छोड़ें"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Secondary Row: Adjust Schedule & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAdjustTime,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("adjust_time_button_${medicine.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.adjust_time_btn),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("med_delete_button_${medicine.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdherenceStatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status.uppercase()) {
        "TAKEN" -> Triple(
            SafeMint.copy(alpha = 0.18f),
            PrimaryTeal,
            tr("✓ Taken Today", "✓ આજે દવા લીધી", "✓ आज खुराक ली गई")
        )
        "SNOOZED" -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            tr("⏰ Snoozed (15m)", "⏰ 15 મિનિટ સ્નૂઝ", "⏰ 15 मिनट स्नूज़")
        )
        "SKIPPED" -> Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.onErrorContainer,
            tr("✕ Skipped Today", "✕ આજે ડોઝ છોડ્યો", "✕ आज खुराक छोड़ी")
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            status
        )
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdherenceHistoryContent(
    logs: List<MedicationAdherenceEntity>,
    filter: String,
    onFilterChange: (String) -> Unit,
    takenCount: Int,
    skippedCount: Int,
    snoozedCount: Int,
    overallRate: Int,
    onExportClick: () -> Unit,
    onDeleteLog: (MedicationAdherenceEntity) -> Unit,
    onClearAll: () -> Unit
) {
    val filteredLogs = remember(logs, filter) {
        if (filter == "ALL") logs else logs.filter { it.status.equals(filter, ignoreCase = true) }
    }
    val dateTimeFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("adherence_analytics_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    "Room Adherence Summary",
                                    "દવા નિયમિતતા સારાંશ (Room)",
                                    "दवा नियमितता सारांश (Room)"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Overall Rate: $overallRate%",
                                    "કુલ નિયમિતતા: $overallRate%",
                                    "कुल नियमितता: $overallRate%"
                                ),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = onExportClick,
                                modifier = Modifier.testTag("med_export_adherence_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tr("Export", "એક્સપોર્ટ", "एक्सपोर्ट"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            if (logs.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = onClearAll,
                                    modifier = Modifier.testTag("med_clear_adherence_btn")
                                ) {
                                    Text(tr("Clear", "સાફ કરો", "साफ़ करें"), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdherenceStatPill(
                            label = tr("Taken", "લીધી", "ली गई"),
                            count = takenCount,
                            color = PrimaryTeal,
                            modifier = Modifier.weight(1f)
                        )
                        AdherenceStatPill(
                            label = tr("Snoozed", "સ્નૂઝ", "स्नूज़"),
                            count = snoozedCount,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        AdherenceStatPill(
                            label = tr("Skipped", "છોડી", "छोड़ी"),
                            count = skippedCount,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "ALL" to tr("All", "બધા", "सभी"),
                    "TAKEN" to tr("Taken", "લીધી", "ली गई"),
                    "SNOOZED" to tr("Snoozed", "સ્નૂઝ", "स्नूज़"),
                    "SKIPPED" to tr("Skipped", "છોડી", "छोड़ी")
                ).forEach { (key, title) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { onFilterChange(key) },
                        label = { Text(title, fontSize = 12.sp) },
                        modifier = Modifier.testTag("adherence_filter_$key")
                    )
                }
            }
        }

        if (filteredLogs.isEmpty()) {
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
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = tr(
                                "No adherence records logged yet",
                                "હજી કોઈ દવા નિયમિતતા રેકોર્ડ નથી",
                                "अभी तक कोई दवा रिकॉर्ड दर्ज नहीं है"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tr(
                                "Tap Taken, Snooze, or Skip on a scheduled medicine or notification to track history.",
                                "ઇતિહાસ નોંધવા માટે શેડ્યૂલ કરેલી દવા અથવા નોટિફિકેશન પર Taken, Snooze અથવા Skip દબાવો.",
                                "इतिहास ट्रैक करने के लिए शेड्यूल की गई दवा या नोटिफिकेशन पर Taken, Snooze या Skip दबाएं।"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adherence_log_card_${log.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = log.medicineName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log.dosage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${log.scheduledDate} • ${log.scheduledTime} (${dateTimeFormatter.format(Date(log.actionTimestamp))})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (log.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.notes,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            AdherenceStatusBadge(status = log.status)
                        }

                        IconButton(
                            onClick = { onDeleteLog(log) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("adherence_delete_btn_${log.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdherenceStatPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Universal Dialog for both Adding and Adjusting Medicine reminder schedules.
 * Supports time-of-day adjustment, repeat frequency (24h/12h/8h), food instructions, and prescriber name.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdjustableMedicineDialog(
    isEditMode: Boolean,
    initialName: String,
    initialDosage: String,
    initialTime: String,
    initialInstruction: String,
    initialFrequencyHours: Int = 24,
    initialPrescribedBy: String = "",
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        dosage: String,
        time: String,
        instruction: String,
        frequencyHours: Int,
        prescribedBy: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var dosage by remember { mutableStateOf(initialDosage) }
    var instruction by remember { mutableStateOf(initialInstruction) }
    var frequencyHours by remember { mutableIntStateOf(initialFrequencyHours) }
    var prescribedBy by remember { mutableStateOf(initialPrescribedBy) }

    val parsedHour: Int
    val parsedMin: Int
    val parsedAmPm: String

    val match = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""", RegexOption.IGNORE_CASE).find(initialTime)
    if (match != null) {
        parsedHour = match.groupValues[1].toIntOrNull()?.coerceIn(1, 12) ?: 8
        parsedMin = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
        parsedAmPm = if (match.groupValues[3].equals("PM", ignoreCase = true)) "PM" else "AM"
    } else {
        parsedHour = 8
        parsedMin = 0
        parsedAmPm = "AM"
    }

    var selectedHour by remember { mutableIntStateOf(parsedHour) }
    var selectedMinute by remember { mutableIntStateOf(parsedMin) }
    var selectedAmPm by remember { mutableStateOf(parsedAmPm) }

    val formattedTimeString = "%02d:%02d %s".format(selectedHour, selectedMinute, selectedAmPm)
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.Medication,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) {
                        stringResource(R.string.edit_medicine_dialog_title)
                    } else {
                        stringResource(R.string.add_medicine_dialog_title)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.medicine_name) + " *") },
                    placeholder = { Text(tr("e.g. Paracetamol, Metformin", "દા.ત. પેરાસીટામોલ, મેટફોર્મિન", "उदा. पेरासिटामोल, मेटफॉर्मिन")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("med_name_input")
                )

                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text(stringResource(R.string.medicine_dosage)) },
                    placeholder = { Text(tr("e.g. 500mg, 1 Tablet, 5ml", "દા.ત. 500mg, ૧ ગોળી, 5ml", "उदा. 500mg, 1 गोली, 5ml")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("med_dosage_input")
                )

                OutlinedTextField(
                    value = prescribedBy,
                    onValueChange = { prescribedBy = it },
                    label = { Text(tr("Prescribed By (Optional)", "ડૉક્ટરનું નામ (વૈકલ્પિક)", "चिकित्सक / पर्चा (वैकल्पिक)")) },
                    placeholder = { Text(tr("e.g. Dr. Patel (General Physician)", "દા.ત. ડૉ. પટેલ", "उदा. डॉ. पटेल")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("med_prescriber_input")
                )

                // Adjustable Time Container
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.adjust_time_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = formattedTimeString,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        selectedHour = if (selectedHour <= 1) 12 else selectedHour - 1
                                    },
                                    modifier = Modifier.size(36.dp).testTag("hour_decrement_btn")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Hour -")
                                }
                                Text(
                                    text = "%02d %s".format(selectedHour, stringResource(R.string.medicine_hour)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                                IconButton(
                                    onClick = {
                                        selectedHour = if (selectedHour >= 12) 1 else selectedHour + 1
                                    },
                                    modifier = Modifier.size(36.dp).testTag("hour_increment_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Hour +")
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        selectedMinute = if (selectedMinute <= 0) 45 else selectedMinute - 15
                                    },
                                    modifier = Modifier.size(36.dp).testTag("minute_decrement_btn")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Min -")
                                }
                                Text(
                                    text = "%02d %s".format(selectedMinute, stringResource(R.string.medicine_minute)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                                IconButton(
                                    onClick = {
                                        selectedMinute = if (selectedMinute >= 45) 0 else selectedMinute + 15
                                    },
                                    modifier = Modifier.size(36.dp).testTag("minute_increment_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Min +")
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = selectedAmPm == "AM",
                                    onClick = { selectedAmPm = "AM" },
                                    label = { Text("AM", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag("time_am_chip")
                                )
                                FilterChip(
                                    selected = selectedAmPm == "PM",
                                    onClick = { selectedAmPm = "PM" },
                                    label = { Text("PM", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag("time_pm_chip")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(R.string.medicine_time_preference),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                Triple(8, 0, "AM") to "08:00 AM",
                                Triple(1, 0, "PM") to "01:00 PM",
                                Triple(6, 0, "PM") to "06:00 PM",
                                Triple(9, 0, "PM") to "09:00 PM"
                            ).forEach { (parts, label) ->
                                val (h, m, ap) = parts
                                FilterChip(
                                    selected = selectedHour == h && selectedMinute == m && selectedAmPm == ap,
                                    onClick = {
                                        selectedHour = h; selectedMinute = m; selectedAmPm = ap
                                    },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Frequency Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Reminder Frequency:", "રિમાઇન્ડર આવર્તન:", "रिमाइंडर आवृत्ति:"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        24 to tr("Once Daily (24h)", "દૈનિક (24 કલાક)", "दैनिक (24 घंटे)"),
                        12 to tr("Twice Daily (12h)", "દિવસમાં 2 વાર (12h)", "दिन में 2 बार (12h)"),
                        8 to tr("Every 8 Hours", "દર 8 કલાકે", "हर 8 घंटे")
                    ).forEach { (hours, label) ->
                        FilterChip(
                            selected = frequencyHours == hours,
                            onClick = { frequencyHours = hours },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("med_freq_${hours}h_chip")
                        )
                    }
                }

                // Instructions Row
                Text(
                    text = stringResource(R.string.medicine_instruction),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val instructions = listOf(
                    stringResource(R.string.medicine_inst_after_food),
                    stringResource(R.string.medicine_inst_before_food),
                    stringResource(R.string.medicine_inst_with_water),
                    stringResource(R.string.medicine_inst_bedtime)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    instructions.forEach { inst ->
                        FilterChip(
                            selected = instruction == inst,
                            onClick = { instruction = inst },
                            label = { Text(inst, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val emptyNameWarning = tr(
                "Please enter medicine name",
                "કૃપા કરીને દવાનું નામ લખો",
                "कृपया दवा का नाम दर्ज करें"
            )
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, dosage, formattedTimeString, instruction, frequencyHours, prescribedBy)
                    } else {
                        Toast.makeText(
                            context,
                            emptyNameWarning,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.testTag("confirm_save_medicine_button")
            ) {
                Text(
                    text = if (isEditMode) {
                        stringResource(R.string.medicine_update_btn)
                    } else {
                        stringResource(R.string.medicine_schedule_btn)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_save_medicine_button")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

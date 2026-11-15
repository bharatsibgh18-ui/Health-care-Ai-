package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.MedicineEntity
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineReminderScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val medicines by viewModel.medicines.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var medicineToEdit by remember { mutableStateOf<MedicineEntity?>(null) }

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
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.medicine_reminder_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            // Informational Hero Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                                tint = androidx.compose.ui.graphics.Color.White,
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
                            text = stringResource(R.string.medicine_automated_alerts_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.triggerMedicineTestNotification()
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_med_notification_sent),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_test_medicine_alert")
                    ) {
                        Text(tr("Test", "ટેસ્ટ", "टेस्ट"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(medicines, key = { it.id }) { med ->
                        MedicineCard(
                            medicine = med,
                            onToggleActive = { viewModel.toggleMedicineActive(med) },
                            onAdjustTime = { medicineToEdit = med },
                            onDelete = {
                                viewModel.deleteMedicine(med)
                                val msg = context.getString(R.string.medicine_deleted_format, med.name)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Add Medicine Dialog with Adjustable Time Picker
    if (showAddDialog) {
        AdjustableMedicineDialog(
            isEditMode = false,
            initialName = "",
            initialDosage = tr("1 Tablet", "૧ ગોળી", "1 गोली"),
            initialTime = "08:00 AM",
            initialInstruction = stringResource(R.string.medicine_inst_after_food),
            onDismiss = { showAddDialog = false },
            onSave = { name, dosage, time, instruction ->
                viewModel.addMedicine(
                    name = name,
                    dosage = dosage,
                    timeOfDay = time,
                    instruction = instruction
                )
                Toast.makeText(context, context.getString(R.string.medicine_saved), Toast.LENGTH_SHORT).show()
                showAddDialog = false
            }
        )
    }

    // Edit & Adjust Medicine Time Dialog
    medicineToEdit?.let { med ->
        AdjustableMedicineDialog(
            isEditMode = true,
            initialName = med.name,
            initialDosage = med.dosage,
            initialTime = med.timeOfDay,
            initialInstruction = med.instruction,
            onDismiss = { medicineToEdit = null },
            onSave = { name, dosage, time, instruction ->
                viewModel.updateMedicineTimeAndDetails(
                    medicine = med,
                    newTime = time,
                    newName = name,
                    newDosage = dosage,
                    newInstruction = instruction
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
    onToggleActive: () -> Unit,
    onAdjustTime: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
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
                            fontWeight = FontWeight.Bold
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
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = medicine.timeOfDay,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = medicine.instruction,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = medicine.isActive,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.testTag("med_active_switch_${medicine.id}")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row with Adjust Time Button & Delete
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
                        .size(36.dp)
                        .testTag("med_delete_button_${medicine.id}")
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

/**
 * Universal Dialog for both Adding and Adjusting Medicine reminder times.
 * Includes interactive hour/minute selector, AM/PM toggle, and quick-presets.
 */
@Composable
fun AdjustableMedicineDialog(
    isEditMode: Boolean,
    initialName: String,
    initialDosage: String,
    initialTime: String,
    initialInstruction: String,
    onDismiss: () -> Unit,
    onSave: (name: String, dosage: String, time: String, instruction: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var dosage by remember { mutableStateOf(initialDosage) }
    var instruction by remember { mutableStateOf(initialInstruction) }

    // Parse initial time e.g. "08:00 AM" or "01:30 PM"
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

    // Formatted current time string
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
                modifier = Modifier.fillMaxWidth(),
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

                        // Hour / Minute adjustments
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour Picker
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        selectedHour = if (selectedHour <= 1) 12 else selectedHour - 1
                                    },
                                    modifier = Modifier.size(32.dp).testTag("hour_decrement_btn")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Hour -")
                                }
                                Text(
                                    text = "%02d %s".format(selectedHour, stringResource(R.string.medicine_hour)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                IconButton(
                                    onClick = {
                                        selectedHour = if (selectedHour >= 12) 1 else selectedHour + 1
                                    },
                                    modifier = Modifier.size(32.dp).testTag("hour_increment_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Hour +")
                                }
                            }

                            // Minute Picker
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        selectedMinute = if (selectedMinute <= 0) 45 else selectedMinute - 15
                                    },
                                    modifier = Modifier.size(32.dp).testTag("minute_decrement_btn")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Min -")
                                }
                                Text(
                                    text = "%02d %s".format(selectedMinute, stringResource(R.string.medicine_minute)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                IconButton(
                                    onClick = {
                                        selectedMinute = if (selectedMinute >= 45) 0 else selectedMinute + 15
                                    },
                                    modifier = Modifier.size(32.dp).testTag("minute_increment_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Min +")
                                }
                            }

                            // AM/PM Switcher
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

                        // Quick Presets
                        Text(
                            text = stringResource(R.string.medicine_time_preference),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedHour == 8 && selectedMinute == 0 && selectedAmPm == "AM",
                                onClick = {
                                    selectedHour = 8; selectedMinute = 0; selectedAmPm = "AM"
                                },
                                label = { Text("08:00 AM", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = selectedHour == 1 && selectedMinute == 0 && selectedAmPm == "PM",
                                onClick = {
                                    selectedHour = 1; selectedMinute = 0; selectedAmPm = "PM"
                                },
                                label = { Text("01:00 PM", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = selectedHour == 6 && selectedMinute == 0 && selectedAmPm == "PM",
                                onClick = {
                                    selectedHour = 6; selectedMinute = 0; selectedAmPm = "PM"
                                },
                                label = { Text("06:00 PM", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = selectedHour == 9 && selectedMinute == 0 && selectedAmPm == "PM",
                                onClick = {
                                    selectedHour = 9; selectedMinute = 0; selectedAmPm = "PM"
                                },
                                label = { Text("09:00 PM", fontSize = 10.sp) }
                            )
                        }
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    instructions.forEach { inst ->
                        FilterChip(
                            selected = instruction == inst,
                            onClick = { instruction = inst },
                            label = { Text(inst, fontSize = 10.sp) }
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
                        onSave(name, dosage, formattedTimeString, instruction)
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

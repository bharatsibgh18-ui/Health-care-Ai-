package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.StepRecordEntity
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SafeMint
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepCounterScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val themeMode by viewModel.themeMode.collectAsState()
    val todayRecord by viewModel.todayStepRecord.collectAsState()
    val weeklyRecords by viewModel.weeklyStepRecords.collectAsState()
    val isTracking by viewModel.isStepTrackingActive.collectAsState()
    val sensorDesc by viewModel.sensorHardwareDescription.collectAsState()

    val currentSteps = todayRecord?.steps ?: 0
    val targetSteps = todayRecord?.targetSteps ?: 8000
    val calories = todayRecord?.caloriesBurned ?: (currentSteps * 0.04)
    val distanceKm = (todayRecord?.distanceMeters ?: (currentSteps * 0.762)) / 1000.0
    val activeMinutes = todayRecord?.activeMinutes ?: (currentSteps / 100)

    val progressFraction = (currentSteps.toFloat() / targetSteps.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 800),
        label = "stepProgress"
    )

    var showGoalDialog by remember { mutableStateOf(false) }
    var customGoalInput by remember { mutableStateOf(targetSteps.toString()) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Activity Recognition runtime permission handling for Android 10+ (API 29+)
    var hasActivityPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasActivityPermission = isGranted
        if (isGranted) {
            viewModel.toggleStepTracking()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasActivityPermission) {
            hasActivityPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.step_counter_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.step_counter_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    LanguageSelector(viewModel = viewModel, modifier = Modifier.padding(end = 4.dp))

                    IconButton(
                        onClick = { viewModel.toggleThemeMode() },
                        modifier = Modifier.testTag("step_theme_toggle_btn")
                    ) {
                        val themeIcon = when (themeMode) {
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        }
                        val themeName = when (themeMode) {
                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                        }
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = themeName,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("step_reset_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reset_steps_btn),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Permission Banner if required
            if (!hasActivityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.activity_permission_rationale),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Text(stringResource(R.string.activity_permission_btn))
                                }
                            }
                        }
                    }
                }
            }

            // Hero Circular Dial Gauge
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .testTag("step_dial_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Circular Gauge
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        val safeGreen = SafeMint

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(210.dp)
                        ) {
                            Canvas(modifier = Modifier.size(200.dp)) {
                                val strokeWidth = 18.dp.toPx()
                                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                                val arcOffset = Offset(strokeWidth / 2, strokeWidth / 2)

                                // Track Arc (270 degrees sweep)
                                drawArc(
                                    color = trackColor,
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    topLeft = arcOffset,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                // Progress Arc
                                val sweep = 270f * animatedProgress
                                drawArc(
                                    color = if (animatedProgress >= 1f) safeGreen else primaryColor,
                                    startAngle = 135f,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = arcOffset,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (animatedProgress >= 1f) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.DirectionsWalk,
                                    contentDescription = null,
                                    tint = if (animatedProgress >= 1f) SafeMint else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%,d", currentSteps),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 36.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "/ ${String.format(Locale.getDefault(), "%,d", targetSteps)} ${stringResource(R.string.steps_label).lowercase()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (animatedProgress >= 1f) SafeMint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    val goalPct = (animatedProgress * 100).roundToInt()
                                    Text(
                                        text = tr("$goalPct% Goal", "$goalPct% લક્ષ્ય", "$goalPct% लक्ष्य"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (animatedProgress >= 1f) Color(0xFF1B5E20) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sensor Status Pill & Play/Pause
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isTracking) SafeMint.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { viewModel.toggleStepTracking() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(if (isTracking) SafeMint else Color.Gray, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isTracking) stringResource(R.string.sensor_active_label) else stringResource(R.string.sensor_paused_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isTracking) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (sensorDesc.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sensorDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // 3 Stat Cards Row: Calories, Distance, Active Time
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FitnessMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = Color(0xFFE65100),
                        label = stringResource(R.string.calories_burned_label),
                        value = "${calories.roundToInt()}",
                        unit = tr("kcal", "કેલરી", "कैलोरी")
                    )
                    FitnessMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Map,
                        iconTint = PrimaryTeal,
                        label = stringResource(R.string.distance_walked_label),
                        value = String.format(Locale.getDefault(), "%.2f", distanceKm),
                        unit = tr("km", "કિમી", "किमी")
                    )
                    FitnessMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Timer,
                        iconTint = Color(0xFF2E7D32),
                        label = stringResource(R.string.active_time_label),
                        value = "$activeMinutes",
                        unit = tr("min", "મિનિટ", "मिनट")
                    )
                }
            }

            // Quick Step Increment Buttons (Simulate/Log Walks)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.add_steps_btn),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr("Indoor / Manual Log", "ઇન્ડોર / મેન્યુઅલ લોગ", "इनडोर / मैन्युअल लॉग"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(100, 250, 500, 1000).forEach { inc ->
                                OutlinedButton(
                                    onClick = { viewModel.addManualSteps(inc) },
                                    modifier = Modifier.weight(1f).testTag("quick_add_step_$inc"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                                ) {
                                    Text("+$inc", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Daily Step Goal Settings Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.step_goal_label),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        "Current: $targetSteps steps/day",
                                        "હાલનું લક્ષ્ય: $targetSteps પગલાં/દિવસ",
                                        "वर्तमान लक्ष्य: $targetSteps कदम/दिन"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                customGoalInput = targetSteps.toString()
                                showGoalDialog = true
                            }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = tr("Edit Goal", "લક્ષ્ય બદલો", "लक्ष्य बदलें"),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(6000, 8000, 10000, 12000).forEach { goal ->
                                FilterChip(
                                    selected = targetSteps == goal,
                                    onClick = { viewModel.setDailyStepGoal(goal) },
                                    label = { Text("${goal / 1000}k", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 7-Day Activity Trend Bar Chart
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.weekly_movement_history),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        WeeklyStepsBarChart(
                            records = weeklyRecords,
                            todaySteps = currentSteps,
                            targetSteps = targetSteps
                        )
                    }
                }
            }

            // Health & Movement Insights Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = EmergencyRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.fitness_tips_header),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tr(
                                "• 8,000 to 10,000 steps daily helps support heart health and reduce cardiovascular risk.\n" +
                                    "• Drink 250ml of water for every 2,500 steps to stay properly hydrated.\n" +
                                    "• Walking after meals aids digestion and helps maintain steady blood sugar levels.",
                                "• દરરોજ ૮,૦૦૦ થી ૧૦,૦૦૦ પગલાં ચાલવાથી હૃદય મજબૂત બને છે અને રોગોનું જોખમ ઘટે છે.\n" +
                                    "• દર ૨,૫૦૦ પગલે ૨૫૦ મિલી પાણી પીવું સ્વાસ્થ્ય માટે ફાયદાકારક છે.\n" +
                                    "• જમ્યા પછી હળવું ચાલવાથી પાચન સારું રહે છે અને બ્લડ સુગર નિયંત્રણમાં રહે છે.",
                                "• रोज 8,000 से 10,000 कदम चलने से हृदय स्वास्थ्य बेहतर होता है और रोगों का खतरा कम होता है।\n" +
                                    "• हर 2,500 कदम के बाद 250 मिली पानी पीना शरीर को हाइड्रेटेड रखता है।\n" +
                                    "• भोजन के बाद टहलने से पाचन सुधरता है और ब्लड शुगर नियंत्रित रहता है।"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.setScreen(AppScreen.WATER_REMINDER) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.nav_water), fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.sendMessage(
                                        trLang(
                                            appLanguage,
                                            "How many daily steps are ideal for my age and health profile?",
                                            "મારી ઉંમર અને આરોગ્ય પ્રમાણે રોજ કેટલા પગલાં ચાલવું ઉત્તમ છે?",
                                            "मेरी उम्र और स्वास्थ्य के अनुसार रोज कितने कदम चलना आदर्श है?"
                                        )
                                    )
                                    viewModel.setScreen(AppScreen.TRIAGE_CHAT)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    tr("Ask AI Doctor", "AI ડૉક્ટરને પૂછો", "AI डॉक्टर से पूछें"),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Set Goal Dialog
    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text(stringResource(R.string.set_goal_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = customGoalInput,
                    onValueChange = { customGoalInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.step_goal_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = customGoalInput.toIntOrNull()
                        if (parsed != null && parsed in 1000..50000) {
                            viewModel.setDailyStepGoal(parsed)
                        }
                        showGoalDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Reset Steps Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_steps_btn)) },
            text = {
                Text(
                    tr(
                        "Reset today's step count back to 0?",
                        "શું આજના પગલાંની ગણતરી ફરીથી ૦ કરવી છે?",
                        "क्या आज के कदमों की गिनती को फिर से 0 पर रीसेट करें?"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDailySteps()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text(stringResource(R.string.reset_steps_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun FitnessMetricCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    unit: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun WeeklyStepsBarChart(
    records: List<StepRecordEntity>,
    todaySteps: Int,
    targetSteps: Int
) {
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayLabel = tr("Today", "આજે", "आज")

    // Generate last 7 days entries
    val calendar = java.util.Calendar.getInstance()
    val past7Days = (6 downTo 0).map { daysAgo ->
        calendar.time = java.util.Date()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
        val dStr = dateFormat.format(calendar.time)
        val dLabel = if (daysAgo == 0) todayLabel else dayFormat.format(calendar.time)
        val stepVal = if (daysAgo == 0) todaySteps else records.find { it.date == dStr }?.steps ?: 0
        Triple(dLabel, stepVal, targetSteps)
    }

    val maxSteps = (past7Days.maxOfOrNull { it.second } ?: targetSteps).coerceAtLeast(targetSteps).coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        past7Days.forEach { (label, steps, target) ->
            val fraction = (steps.toFloat() / maxSteps.toFloat()).coerceIn(0.05f, 1f)
            val isTargetReached = steps >= target
            val isToday = label == todayLabel

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (steps > 0) "${(steps / 1000f * 10).roundToInt() / 10f}k" else "0",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height((100 * fraction).dp)
                        .background(
                            color = if (isTargetReached) SafeMint else MaterialTheme.colorScheme.primary.copy(alpha = if (steps > 0) 0.85f else 0.2f),
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

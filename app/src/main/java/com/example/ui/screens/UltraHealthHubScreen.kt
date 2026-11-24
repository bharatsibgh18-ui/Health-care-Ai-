package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AnimatedHealthScoreRing
import com.example.ui.components.UltraGlassCard
import com.example.ui.components.UltraGradientHeroCard
import com.example.ui.components.UltraMetricPill
import com.example.ui.components.UltraSectionHeader
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.UltraAmberStart
import com.example.ui.theme.UltraCyanEnd
import com.example.ui.theme.UltraEmeraldStart
import com.example.ui.theme.UltraIndigoStart
import com.example.ui.theme.UltraMintEnd
import com.example.ui.theme.UltraTealStart
import com.example.ui.theme.UltraVioletEnd
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import com.example.ui.viewmodel.UltraHealthTab
import com.example.utils.DailyHealthScoreReport
import com.example.utils.HealthPassportPdfGenerator
import com.example.utils.HealthScoreTier
import com.example.utils.InsightPriority
import com.example.utils.MedicationStreakEngine
import com.example.utils.NutritionPresetCatalog
import com.example.utils.UltraHealthCoachEngine
import com.example.utils.UltraHealthScoreEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltraHealthHubScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.ultraHealthTab.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val authSession by viewModel.authSession.collectAsState()
    val devAppConfig by viewModel.devAppConfig.collectAsState()
    val todayWaterMl by viewModel.todayTotalWaterMl.collectAsState()
    val waterTargetMl by viewModel.waterTargetMl.collectAsState()
    val todayStepRecord by viewModel.todayStepRecord.collectAsState()
    val medicines by viewModel.medicines.collectAsState()
    val allAdherenceLogs by viewModel.allAdherenceLogs.collectAsState()
    val sleepLogs by viewModel.sleepWellnessLogs.collectAsState()
    val vitalLogs by viewModel.vitalLogs.collectAsState()
    val scannedReports by viewModel.scannedReports.collectAsState()
    val mealNutritionLogs by viewModel.mealNutritionLogs.collectAsState()
    val dailyCalorieTarget by viewModel.dailyCalorieTarget.collectAsState()
    val completedCoachTaskIds by viewModel.completedCoachTaskIds.collectAsState()
    val aiCoachBrief by viewModel.aiCoachBrief.collectAsState()
    val isAiCoachFromGemini by viewModel.isAiCoachFromGemini.collectAsState()
    val isGeneratingCoachPlan by viewModel.isGeneratingCoachPlan.collectAsState()
    val shakeToSosEnabled by viewModel.shakeToSosEnabled.collectAsState()
    val context = LocalContext.current

    val todaySteps = todayStepRecord?.steps ?: 0
    val stepTarget = todayStepRecord?.targetSteps ?: 8000
    val activeMeds = remember(medicines) { medicines.filter { it.isActive } }
    val todayDateKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    val scoreReport = remember(
        todayWaterMl, waterTargetMl, todaySteps, stepTarget, activeMeds, allAdherenceLogs, sleepLogs, vitalLogs
    ) {
        UltraHealthScoreEngine.calculate(
            todayWaterMl = todayWaterMl,
            waterTargetMl = waterTargetMl,
            todaySteps = todaySteps,
            stepTarget = stepTarget,
            activeMedicines = activeMeds,
            adherenceLogs = allAdherenceLogs,
            sleepLogs = sleepLogs,
            vitalLogs = vitalLogs
        )
    }

    val streakAnalytics = remember(activeMeds, allAdherenceLogs) {
        MedicationStreakEngine.analyze(medicines = activeMeds, adherenceLogs = allAdherenceLogs)
    }

    val todayMealLogs = remember(mealNutritionLogs, todayDateKey) {
        mealNutritionLogs.filter { it.dateKey == todayDateKey }
    }

    val nutritionSummary = remember(mealNutritionLogs, todayDateKey, dailyCalorieTarget) {
        NutritionPresetCatalog.calculateDailySummary(
            logs = mealNutritionLogs,
            dateKey = todayDateKey,
            targetCalories = dailyCalorieTarget
        )
    }

    val coachPlan = remember(
        todayDateKey,
        scoreReport.overallScore,
        todayWaterMl,
        waterTargetMl,
        todaySteps,
        stepTarget,
        nutritionSummary.totalCalories,
        dailyCalorieTarget,
        activeMeds,
        sleepLogs,
        vitalLogs,
        selectedLanguage,
        aiCoachBrief,
        isAiCoachFromGemini
    ) {
        UltraHealthCoachEngine.buildDailyPlan(
            dateKey = todayDateKey,
            healthScore = scoreReport.overallScore,
            todayWaterMl = todayWaterMl,
            waterTargetMl = waterTargetMl,
            todaySteps = todaySteps,
            stepTarget = stepTarget,
            todayCalories = nutritionSummary.totalCalories,
            calorieTarget = dailyCalorieTarget,
            activeMedicines = activeMeds,
            sleepLogs = sleepLogs,
            vitalLogs = vitalLogs,
            language = selectedLanguage,
            aiBriefOverride = aiCoachBrief,
            isAiGenerated = isAiCoachFromGemini
        )
    }

    val passportSnapshot = remember(
        userProfile,
        authSession.displayName,
        scoreReport,
        todaySteps,
        todayWaterMl,
        nutritionSummary.totalCalories,
        activeMeds,
        vitalLogs
    ) {
        HealthPassportPdfGenerator.buildSnapshot(
            profile = userProfile,
            fallbackName = authSession.displayName,
            scoreReport = scoreReport,
            todaySteps = todaySteps,
            todayWaterMl = todayWaterMl,
            todayCalories = nutritionSummary.totalCalories,
            activeMedicines = activeMeds,
            latestVitals = vitalLogs
        )
    }

    // Keep Home Screen Widget synchronized with latest computed score
    LaunchedEffect(scoreReport.overallScore, todaySteps, todayWaterMl, streakAnalytics.currentStreakDays) {
        viewModel.syncHealthSummaryWidget()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(UltraTealStart, UltraCyanEnd, UltraIndigoStart)))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = tr("Ultra Health Hub", "અલ્ટ્રા હેલ્થ હબ", "अल्ट्रा हेल्थ हब"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = tr(
                                    "Score • AI Coach • Body Map • Nutrition • SOS PDF",
                                    "સ્કોર • AI કોચ • બોડી મેપ • પોષણ • SOS PDF",
                                    "स्कोर • AI कोच • बॉडी मैप • पोषण • SOS PDF"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.TRIAGE_CHAT) },
                        modifier = Modifier.testTag("ultra_hub_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "પાછા જાઓ", "वापस जाएं")
                        )
                    }
                },
                actions = {
                    com.example.ui.components.LanguageSelector(viewModel = viewModel, showRefreshButton = false)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.10f)
                        )
                    )
                )
        ) {
            // Pill Tab Bar for all 9 Ultra Clinical Modules
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UltraTabChip(
                    selected = activeTab == UltraHealthTab.SCORE,
                    label = tr("Score", "હેલ્થ સ્કોર", "हेल्थ स्कोर"),
                    icon = Icons.Default.MonitorHeart,
                    tag = "ultra_tab_score",
                    onClick = { viewModel.selectUltraHealthTab(UltraHealthTab.SCORE) }
                )
                UltraTabChip(
                    selected = activeTab == UltraHealthTab.AI_COACH,
                    label = tr("AI Coach", "AI કોચ", "AI कोच"),
                    icon = Icons.Default.AutoAwesome,
                    tag = "ultra_tab_ai_coach",
                    onClick = { viewModel.selectUltraHealthTab(UltraHealthTab.AI_COACH) }
                )
                UltraTabChip(
                    selected = activeTab == UltraHealthTab.BODY_MAP,
                    label = tr("Body Map", "બોડી મેપ", "बॉडी मैप"),
                    icon = Icons.Default.AccessibilityNew,
                    tag = "ultra_tab_body_map",
                    onClick = { viewModel.selectUltraHealthTab(UltraHealthTab.BODY_MAP) }
                )
                UltraTabChip(
                    selected = activeTab == UltraHealthTab.NUTRITION,
                    label = tr("Nutrition", "પોષણ", "पोषण"),
                    icon = Icons.Default.Restaurant,
                    tag = "ultra_tab_nutrition",
                    onClick = { viewModel.selectUltraHealthTab(UltraHealthTab.NUTRITION) }
                )
                UltraTabChip(
                    selected = activeTab == UltraHealthTab.SOS_PASSPORT,
                    label = tr("SOS & PDF", "SOS અને PDF", "SOS व PDF"),
                    icon = Icons.Default.Emergency,
                    tag = "ultra_tab_sos_passport",
                    onClick = { viewModel.selectUltraHealthTab(UltraHealthTab.SOS_PASSPORT) }
                )
                UltraTabChip(
                    selected = activeTab == UltraHealthTab.STREAKS,
                    label = tr("Streaks", "દવા સ્ટ્રીક", "दवा स्ट्रीक"),
                    icon = Icons.Default.LocalFireDepartment,
                    tag = "ultra_tab_streaks",
                    onClick = { viewModel.selectUltraHealthTab(UltraHealthTab.STREAKS) }
                )
                UltraTabChip(
                    selected = activeTab == UltraHealthTab.MEDICAL_ID,
                    label = tr("QR Medical ID", "QR મેડિકલ ID", "QR मेडिकल ID"),
                    icon = Icons.Default.Badge,
                    tag = "ultra_tab_medical_id",
                    onClick = { viewModel.selectUltraHealthTab(UltraHealthTab.MEDICAL_ID) }
                )
                UltraTabChip(
                    selected = activeTab == UltraHealthTab.WELLNESS,
                    label = tr("BMI & Sleep", "BMI અને ઊંઘ", "BMI व नींद"),
                    icon = Icons.Default.Nightlight,
                    tag = "ultra_tab_wellness",
                    onClick = { viewModel.selectUltraHealthTab(UltraHealthTab.WELLNESS) }
                )
                UltraTabChip(
                    selected = activeTab == UltraHealthTab.LAB_TRENDS,
                    label = tr("Lab Trends", "લેબ ટ્રેન્ડ", "लैब ट्रेंड"),
                    icon = Icons.Default.Biotech,
                    tag = "ultra_tab_lab_trends",
                    onClick = { viewModel.selectUltraHealthTab(UltraHealthTab.LAB_TRENDS) }
                )
            }

            // Tab Body with smooth Ultra Clinical Glass fade transitions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
                    },
                    label = "ultra_tab_transition"
                ) { tab ->
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        when (tab) {
                            UltraHealthTab.SCORE -> UltraScoreDashboardTab(
                                scoreReport = scoreReport,
                                todaySteps = todaySteps,
                                todayWaterMl = todayWaterMl,
                                streakDays = streakAnalytics.currentStreakDays,
                                selectedLanguage = selectedLanguage,
                                onQuickWater = {
                                    viewModel.logWater(250)
                                    viewModel.syncHealthSummaryWidget()
                                },
                                onOpenTab = { viewModel.selectUltraHealthTab(it) },
                                onOpenSteps = { viewModel.setScreen(AppScreen.STEP_COUNTER) },
                                onSyncWidget = {
                                    viewModel.syncHealthSummaryWidget()
                                    android.widget.Toast.makeText(
                                        context,
                                        trLang(
                                            selectedLanguage,
                                            "Home Screen Widget synced! Long-press your home screen -> Widgets -> Health Care AI Daily Summary.",
                                            "હોમ સ્ક્રીન વિજેટ અપડેટ થયું! હોમ સ્ક્રીન પર લાંબું દબાવી Widgets માંથી Health Care AI ઉમેરો.",
                                            "होम स्क्रीन विजेट सिंक हो गया! होम स्क्रीन पर लॉन्ग-प्रेस करके Widgets से Health Care AI जोड़ें।"
                                        ),
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            )

                            UltraHealthTab.AI_COACH -> UltraAiCoachTab(
                                plan = coachPlan,
                                completedTaskIds = completedCoachTaskIds,
                                isGenerating = isGeneratingCoachPlan,
                                selectedLanguage = selectedLanguage,
                                onToggleTask = { taskId -> viewModel.toggleCoachTaskCompleted(taskId) },
                                onRefreshAiCoach = { viewModel.refreshAiHealthCoachPlan() }
                            )

                            UltraHealthTab.BODY_MAP -> UltraBodyMapTab(
                                selectedLanguage = selectedLanguage,
                                onAnalyzeWithAiTriage = { zoneTitle, symptoms, severity, duration, notes ->
                                    viewModel.launchBodyMapTriage(
                                        zoneTitle = zoneTitle,
                                        symptoms = symptoms,
                                        severity = severity,
                                        duration = duration,
                                        additionalNotes = notes
                                    )
                                }
                            )

                            UltraHealthTab.NUTRITION -> UltraNutritionTab(
                                summary = nutritionSummary,
                                todayLogs = todayMealLogs,
                                selectedLanguage = selectedLanguage,
                                onLogMeal = { mealType, foodName, calories, protein, carbs, fiber ->
                                    viewModel.logMealNutrition(
                                        mealType = mealType,
                                        foodName = foodName,
                                        calories = calories,
                                        proteinGrams = protein,
                                        carbsGrams = carbs,
                                        fiberGrams = fiber
                                    )
                                },
                                onDeleteMeal = { meal -> viewModel.deleteMealNutrition(meal) },
                                onUpdateTargetCalories = { target -> viewModel.updateDailyCalorieTarget(target) }
                            )

                            UltraHealthTab.SOS_PASSPORT -> UltraSosPassportTab(
                                snapshot = passportSnapshot,
                                emergencyHelpline = devAppConfig.emergencyHelplineOverride
                                    .ifBlank { userProfile?.emergencyContactPhone ?: "112" },
                                shakeToSosEnabled = shakeToSosEnabled,
                                selectedLanguage = selectedLanguage,
                                onToggleShakeToSos = { enabled -> viewModel.setShakeToSosEnabled(enabled) }
                            )

                            UltraHealthTab.STREAKS -> UltraMedicationStreaksTab(
                                analytics = streakAnalytics,
                                medicines = activeMeds,
                                onMarkTaken = { med ->
                                    viewModel.recordMedicineAdherence(med, "TAKEN", "Logged from Ultra Hub")
                                    viewModel.syncHealthSummaryWidget()
                                },
                                onOpenMedicineReminders = { viewModel.setScreen(AppScreen.MEDICINE_REMINDER) }
                            )

                            UltraHealthTab.MEDICAL_ID -> UltraEmergencyMedicalIdTab(
                                profile = userProfile,
                                selectedLanguage = selectedLanguage,
                                onEditProfile = { viewModel.setScreen(AppScreen.PROFILE) },
                                onOpenEmergencyGuide = { viewModel.setScreen(AppScreen.EMERGENCY_GUIDE) }
                            )

                            UltraHealthTab.WELLNESS -> UltraWellnessAndSleepTab(
                                userProfile = userProfile,
                                sleepLogs = sleepLogs,
                                onSaveSleep = { hours, quality, bed, wake, weight, height, notes ->
                                    viewModel.logSleepWellness(hours, quality, bed, wake, weight, height, notes)
                                },
                                onDeleteSleep = { viewModel.deleteSleepWellnessLog(it) }
                            )

                            UltraHealthTab.LAB_TRENDS -> UltraLabTrendsTab(
                                scannedReports = scannedReports,
                                vitalLogs = vitalLogs,
                                onOpenScanner = { viewModel.setScreen(AppScreen.REPORT_SCANNER) },
                                onOpenVitals = { viewModel.setScreen(AppScreen.VITALS_TRACKER) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun UltraTabChip(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun UltraScoreDashboardTab(
    scoreReport: DailyHealthScoreReport,
    todaySteps: Int,
    todayWaterMl: Int,
    streakDays: Int,
    selectedLanguage: String,
    onQuickWater: () -> Unit,
    onOpenTab: (UltraHealthTab) -> Unit,
    onOpenSteps: () -> Unit,
    onSyncWidget: () -> Unit
) {
    val tierLabel = when (scoreReport.tier) {
        HealthScoreTier.ULTRA_OPTIMAL -> tr("Ultra Optimal", "અલ્ટ્રા શ્રેષ્ઠ", "अल्ट्रा उत्तम")
        HealthScoreTier.STRONG -> tr("Strong Health", "મજબૂત સ્વાસ્થ્ય", "मज़बूत स्वास्थ्य")
        HealthScoreTier.STEADY -> tr("Steady Progress", "સારી પ્રગતિ", "स्थिर प्रगति")
        HealthScoreTier.NEEDS_ATTENTION -> tr("Boost Needed", "ધ્યાન જરૂરી", "ध्यान आवश्यक")
    }

    // Hero Gradient Health Score Card
    UltraGradientHeroCard(
        colors = listOf(UltraTealStart, UltraCyanEnd, UltraIndigoStart),
        modifier = Modifier.testTag("ultra_health_score_hero_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedHealthScoreRing(
                score = scoreReport.overallScore,
                tierLabel = tierLabel,
                ringSize = 136.dp,
                strokeWidth = 13.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tr("Daily Health Score", "દૈનિક હેલ્થ સ્કોર", "दैनिक हेल्थ स्कोर"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                UltraMetricPill(
                    label = tr("Steps", "સ્ટેપ્સ", "कदम"),
                    value = "$todaySteps",
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    modifier = Modifier.fillMaxWidth()
                )
                UltraMetricPill(
                    label = tr("Hydration", "પાણી", "पानी"),
                    value = "$todayWaterMl ml",
                    icon = Icons.Default.WaterDrop,
                    modifier = Modifier.fillMaxWidth()
                )
                UltraMetricPill(
                    label = tr("Med Streak", "દવા સ્ટ્રીક", "दवा स्ट्रीक"),
                    value = "$streakDays d",
                    icon = Icons.Default.LocalFireDepartment,
                    tint = Color(0xFFFDE68A),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onQuickWater,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.22f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("ultra_quick_water_btn")
            ) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("+250 ml", "+250 ml", "+250 ml"), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onOpenTab(UltraHealthTab.MEDICAL_ID) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = UltraTealStart
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("ultra_quick_qr_id_btn")
            ) {
                Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("QR Medical ID", "QR કાર્ડ", "QR कार्ड"), fontWeight = FontWeight.Bold)
            }
        }
    }

    // 5 Clinical Pillars Glass Breakdown Card
    UltraGlassCard(
        accentStart = UltraTealStart,
        accentEnd = UltraEmeraldStart,
        modifier = Modifier.testTag("ultra_pillars_card")
    ) {
        UltraSectionHeader(
            title = tr("5-Pillar Health Breakdown", "૫-સ્તંભ આરોગ્ય વિશ્લેષણ", "5-स्तंभ स्वास्थ्य विश्लेषण"),
            subtitle = tr("Weighted live across your daily habits", "તમારી દૈનિક આદતોનું જીવંત મૂલ્યાંકન", "आपकी दैनिक आदतों का लाइव मूल्यांकन"),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            accentColor = UltraTealStart
        )
        Spacer(modifier = Modifier.height(12.dp))
        scoreReport.pillars.forEach { pillar ->
            val label = trLang(selectedLanguage, pillar.labelEn, pillar.labelGu, pillar.labelHi)
            val barColor = when {
                pillar.score >= 80 -> SafeMint
                pillar.score >= 55 -> UltraCyanEnd
                else -> UltraAmberStart
            }
            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$label (${pillar.weightPercent}%)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${pillar.metricSummary} • ${pillar.score}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { (pillar.score / 100f).coerceIn(0f, 1f) },
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }

    // Smart Daily Insights Card
    UltraGlassCard(
        accentStart = UltraIndigoStart,
        accentEnd = UltraVioletEnd,
        modifier = Modifier.testTag("ultra_smart_insights_card")
    ) {
        UltraSectionHeader(
            title = tr("Smart Daily Insights", "સ્માર્ટ દૈનિક સૂચનો", "स्मार्ट दैनिक सुझाव"),
            subtitle = tr("Actionable guidance tailored to today's stats", "આજના ડેટા પર આધારિત વ્યક્તિગત માર્ગદર્શન", "आज के आंकड़ों पर आधारित व्यक्तिगत सलाह"),
            icon = Icons.Default.TipsAndUpdates,
            accentColor = UltraIndigoStart
        )
        Spacer(modifier = Modifier.height(10.dp))
        scoreReport.insights.forEach { insight ->
            val (badgeColor, iconVec) = when (insight.priority) {
                InsightPriority.POSITIVE -> SafeMint to Icons.Default.CheckCircle
                InsightPriority.SUGGESTION -> UltraIndigoStart to Icons.Default.TipsAndUpdates
                InsightPriority.ACTION_NEEDED -> EmergencyRed to Icons.Default.PriorityHigh
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = badgeColor.copy(alpha = 0.10f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = iconVec,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trLang(selectedLanguage, insight.titleEn, insight.titleGu, insight.titleHi),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = trLang(selectedLanguage, insight.detailEn, insight.detailGu, insight.detailHi),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Home Screen Widget Sync & Quick Launch Card
    UltraGlassCard(
        accentStart = UltraEmeraldStart,
        accentEnd = UltraMintEnd,
        modifier = Modifier.testTag("ultra_widget_sync_card")
    ) {
        UltraSectionHeader(
            title = tr("Android Home Screen Widget", "હોમ સ્ક્રીન હેલ્થ વિજેટ", "होम स्क्रीन हेल्थ विजेट"),
            subtitle = tr(
                "Shows live Score, Steps, Water, Medicine & 1-tap SOS right on your home screen",
                "હોમ સ્ક્રીન પર લાઇવ સ્કોર, સ્ટેપ્સ, પાણી, દવા અને SOS બટન જુઓ",
                "होम स्क्रीन पर लाइव स्कोर, कदम, पानी, दवा और SOS बटन देखें"
            ),
            icon = Icons.Default.Widgets,
            accentColor = UltraEmeraldStart
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onSyncWidget,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("ultra_sync_widget_btn")
            ) {
                Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Sync Widget", "વિજેટ સિંક કરો", "विजेट सिंक करें"))
            }
            OutlinedButton(
                onClick = onOpenSteps,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(tr("Step Tracker", "સ્ટેપ ટ્રેકર", "स्टेप ट्रैकर"))
            }
        }
    }
}

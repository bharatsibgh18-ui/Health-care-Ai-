package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.screens.AiMedicineAdvisorScreen
import com.example.ui.screens.AiVideoCallScreen
import com.example.ui.screens.AnimatedSplashScreen
import com.example.ui.screens.AppInfoScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatTriageScreen
import com.example.ui.screens.ClinicalIntelligenceScreen
import com.example.ui.screens.DailySymptomLoggerScreen
import com.example.ui.screens.DeveloperControlPanelScreen
import com.example.ui.screens.DeveloperLogsScreen
import com.example.ui.screens.EmergencyGuideScreen
import kotlinx.coroutines.delay
import com.example.ui.screens.FamilyCareScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MedicalReportScannerScreen
import com.example.ui.screens.MedicineReminderScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SmartHubScreen
import com.example.ui.screens.StepCounterScreen
import com.example.ui.screens.UltraHealthHubScreen
import com.example.ui.screens.VitalsTrackerScreen
import com.example.ui.screens.WaterReminderScreen
import com.example.ui.theme.AppLanguages
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.MediAssistTheme
import com.example.ui.theme.tr
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_SCREEN = "extra_open_screen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { enableEdgeToEdge() }
        val initialScreenExtra = runCatching { intent?.getStringExtra(EXTRA_OPEN_SCREEN) }.getOrNull()
        setContent {
            val appFactory = remember(application) {
                androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(application)
            }
            val viewModel: MediAssistViewModel = viewModel(factory = appFactory)
            val themeMode by viewModel.themeMode.collectAsState()
            val selectedLanguage by viewModel.selectedLanguage.collectAsState()
            val context = LocalContext.current

            val languageInfo = remember(selectedLanguage) {
                AppLanguages.findByName(selectedLanguage)
            }
            val locale = remember(languageInfo) { languageInfo.toLocale() }
            val layoutDirection = if (languageInfo.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            // Sync Android resources locale outside composition only when language actually differs
            LaunchedEffect(locale) {
                try {
                    val resLocales = resources.configuration.locales
                    val resLocale = if (!resLocales.isEmpty) resLocales[0] else null
                    val targetLang = locale.language.lowercase(Locale.US)
                    val currentLang = resLocale?.language?.lowercase(Locale.US)
                    if (currentLang != targetLang) {
                        Locale.setDefault(locale)
                        val config = Configuration(resources.configuration)
                        config.setLocale(locale)
                        @Suppress("DEPRECATION")
                        resources.updateConfiguration(config, resources.displayMetrics)
                    }
                } catch (_: Throwable) {
                }
            }

            // Initialize notification channels and handle notification/widget tap deep-link
            LaunchedEffect(initialScreenExtra) {
                try {
                    when (initialScreenExtra) {
                        "MEDICINE_REMINDER" -> viewModel.setScreen(AppScreen.MEDICINE_REMINDER)
                        "WATER_REMINDER" -> viewModel.setScreen(AppScreen.WATER_REMINDER)
                        "ULTRA_HEALTH_HUB" -> viewModel.openUltraHealthHub()
                        "EMERGENCY_GUIDE" -> viewModel.setScreen(AppScreen.EMERGENCY_GUIDE)
                        "TRIAGE_CHAT" -> viewModel.setScreen(AppScreen.TRIAGE_CHAT)
                    }
                } catch (_: Throwable) {}
                try {
                    viewModel.syncHealthSummaryWidget()
                } catch (_: Throwable) {}
                try {
                    com.example.utils.WaterReminderNotificationHelper.createNotificationChannel(context)
                    com.example.utils.MedicineReminderWorker.createNotificationChannel(context)
                } catch (_: Throwable) {}
            }

            val currentConfig = LocalConfiguration.current
            val localizedConfiguration = remember(currentConfig, locale) {
                Configuration(currentConfig).apply {
                    setLocale(locale)
                }
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfiguration,
                LocalAppLanguage provides languageInfo.id,
                LocalLayoutDirection provides layoutDirection
            ) {
                MediAssistTheme(themeMode = themeMode) {
                    var showSplash by rememberSaveable { mutableStateOf(initialScreenExtra == null) }
                    if (showSplash) {
                        AnimatedSplashScreen(
                            onSplashFinished = { showSplash = false }
                        )
                    } else {
                        MediAssistApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MediAssistApp(viewModel: MediAssistViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val authSession by viewModel.authSession.collectAsState()
    val pendingInAppAlert by viewModel.pendingInAppAlert.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    pendingInAppAlert?.let { alert ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissInAppAlert() },
            title = { Text(text = alert.title) },
            text = {
                Column {
                    Text(text = alert.message)
                    if (!alert.scheduledIsoTime.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tr(
                                "Scheduled (ISO): ${alert.scheduledIsoTime}",
                                "શેડ્યૂલ સમય (ISO): ${alert.scheduledIsoTime}",
                                "शेड्यूल समय (ISO): ${alert.scheduledIsoTime}"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = alert.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissInAppAlert() },
                    modifier = Modifier.testTag("dismiss_offline_in_app_alert_btn")
                ) {
                    Text(tr("OK", "બરાબર", "ठीक है"))
                }
            },
            modifier = Modifier.testTag("offline_in_app_alert_dialog")
        )
    }

    // Sync public doctors/config and accumulate foreground online duration every 30s
    LaunchedEffect(authSession.isLoggedIn, authSession.username) {
        if (authSession.isLoggedIn) {
            viewModel.syncPublicDataFromCloud()
            viewModel.recordOnlineSessionHeartbeat(0L, isOnline = true)
            while (true) {
                delay(30_000L)
                viewModel.recordOnlineSessionHeartbeat(30L, isOnline = true)
            }
        }
    }

    DisposableEffect(lifecycleOwner, authSession.isLoggedIn, authSession.username) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (authSession.isLoggedIn) {
                        viewModel.recordOnlineSessionHeartbeat(0L, isOnline = true)
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    if (authSession.isLoggedIn) {
                        viewModel.markCurrentSessionOffline()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navigateTo: (AppScreen) -> Unit = { targetScreen ->
        if (targetScreen != currentScreen) {
            viewModel.setScreen(targetScreen)
        }
    }

    if (!authSession.isLoggedIn || currentScreen == AppScreen.AUTH_LOGIN) {
        if (currentScreen == AppScreen.AUTH_LOGIN && authSession.isLoggedIn) {
            BackHandler {
                viewModel.setScreen(AppScreen.TRIAGE_CHAT)
            }
        }
        AuthScreen(
            viewModel = viewModel,
            onAuthSuccess = {
                viewModel.setScreen(AppScreen.TRIAGE_CHAT)
            }
        )
        return
    }

    // Handle back button on secondary screens
    if (currentScreen != AppScreen.TRIAGE_CHAT) {
        BackHandler {
            viewModel.setScreen(AppScreen.TRIAGE_CHAT)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedWindow = maxWidth >= 600.dp

        if (currentScreen == AppScreen.AI_VIDEO_CALL) {
            AiVideoCallScreen(viewModel = viewModel)
        } else if (isExpandedWindow) {
            // Adaptive Window / Tablet / Desktop layout with NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.testTag("main_nav_rail"),
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HealthAndSafety,
                                contentDescription = "Health Care AI",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = tr("Health AI", "હેલ્થ AI", "हेल्थ AI"),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationRailItem(
                        selected = currentScreen == AppScreen.TRIAGE_CHAT,
                        onClick = { navigateTo(AppScreen.TRIAGE_CHAT) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppScreen.TRIAGE_CHAT) Icons.Filled.HealthAndSafety else Icons.Outlined.HealthAndSafety,
                                contentDescription = stringResource(R.string.nav_triage)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_triage), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_triage_tab")
                    )

                    NavigationRailItem(
                        selected = currentScreen == AppScreen.WATER_REMINDER,
                        onClick = { navigateTo(AppScreen.WATER_REMINDER) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppScreen.WATER_REMINDER) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop,
                                contentDescription = stringResource(R.string.nav_water)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_water), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_water_tab")
                    )

                    NavigationRailItem(
                        selected = currentScreen == AppScreen.STEP_COUNTER,
                        onClick = { navigateTo(AppScreen.STEP_COUNTER) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppScreen.STEP_COUNTER) Icons.AutoMirrored.Filled.DirectionsRun else Icons.AutoMirrored.Outlined.DirectionsRun,
                                contentDescription = stringResource(R.string.step_counter_title)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_steps), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_steps_rail_tab")
                    )

                    NavigationRailItem(
                        selected = currentScreen == AppScreen.EMERGENCY_GUIDE,
                        onClick = { navigateTo(AppScreen.EMERGENCY_GUIDE) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppScreen.EMERGENCY_GUIDE) Icons.Filled.Emergency else Icons.Outlined.Emergency,
                                contentDescription = stringResource(R.string.nav_emergency),
                                tint = if (currentScreen == AppScreen.EMERGENCY_GUIDE) EmergencyRed else LocalContentColor.current
                            )
                        },
                        label = { Text(stringResource(R.string.nav_emergency), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_emergency_tab")
                    )

                    NavigationRailItem(
                        selected = currentScreen == AppScreen.HISTORY,
                        onClick = { navigateTo(AppScreen.HISTORY) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppScreen.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                contentDescription = stringResource(R.string.nav_history)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_history), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_history_tab")
                    )

                    NavigationRailItem(
                        selected = currentScreen == AppScreen.PROFILE,
                        onClick = { navigateTo(AppScreen.PROFILE) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == AppScreen.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = stringResource(R.string.nav_profile)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_profile), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_profile_tab")
                    )
                }

                // Window content container with responsive layout
                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1000.dp)
                        ) {
                            AppScreenContent(currentScreen = currentScreen, viewModel = viewModel)
                        }
                    }
                }
            }
        } else {
            // Handheld / Compact Screen layout (5 clean NavigationBar destinations, single WindowInsets pass)
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar(modifier = Modifier.testTag("main_bottom_nav")) {
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.TRIAGE_CHAT,
                            onClick = { navigateTo(AppScreen.TRIAGE_CHAT) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.TRIAGE_CHAT) Icons.Filled.HealthAndSafety else Icons.Outlined.HealthAndSafety,
                                    contentDescription = stringResource(R.string.nav_triage)
                                )
                            },
                            label = {
                                Text(
                                    stringResource(R.string.nav_triage),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_triage_tab")
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.WATER_REMINDER,
                            onClick = { navigateTo(AppScreen.WATER_REMINDER) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.WATER_REMINDER) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop,
                                    contentDescription = stringResource(R.string.nav_water)
                                )
                            },
                            label = {
                                Text(
                                    stringResource(R.string.nav_water),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_water_tab")
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.EMERGENCY_GUIDE,
                            onClick = { navigateTo(AppScreen.EMERGENCY_GUIDE) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.EMERGENCY_GUIDE) Icons.Filled.Emergency else Icons.Outlined.Emergency,
                                    contentDescription = stringResource(R.string.nav_emergency),
                                    tint = if (currentScreen == AppScreen.EMERGENCY_GUIDE) EmergencyRed else LocalContentColor.current
                                )
                            },
                            label = {
                                Text(
                                    stringResource(R.string.nav_emergency),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_emergency_tab")
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.HISTORY,
                            onClick = { navigateTo(AppScreen.HISTORY) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = stringResource(R.string.nav_history)
                                )
                            },
                            label = {
                                Text(
                                    stringResource(R.string.nav_history),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_history_tab")
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.PROFILE,
                            onClick = { navigateTo(AppScreen.PROFILE) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                    contentDescription = stringResource(R.string.nav_profile)
                                )
                            },
                            label = {
                                Text(
                                    stringResource(R.string.nav_profile),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_profile_tab")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                ) {
                    AppScreenContent(currentScreen = currentScreen, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun AppScreenContent(
    currentScreen: AppScreen,
    viewModel: MediAssistViewModel
) {
    when (currentScreen) {
        AppScreen.TRIAGE_CHAT -> ChatTriageScreen(viewModel = viewModel)
        AppScreen.WATER_REMINDER -> WaterReminderScreen(viewModel = viewModel)
        AppScreen.MEDICINE_REMINDER -> MedicineReminderScreen(viewModel = viewModel)
        AppScreen.EMERGENCY_GUIDE -> EmergencyGuideScreen(viewModel = viewModel)
        AppScreen.HISTORY -> HistoryScreen(viewModel = viewModel)
        AppScreen.PROFILE -> ProfileScreen(viewModel = viewModel)
        AppScreen.APP_INFO -> AppInfoScreen(viewModel = viewModel)
        AppScreen.REPORT_SCANNER -> MedicalReportScannerScreen(viewModel = viewModel)
        AppScreen.STEP_COUNTER -> StepCounterScreen(viewModel = viewModel)
        AppScreen.VITALS_TRACKER -> VitalsTrackerScreen(viewModel = viewModel)
        AppScreen.FAMILY_CARE -> FamilyCareScreen(viewModel = viewModel)
        AppScreen.SMART_HUB -> SmartHubScreen(viewModel = viewModel)
        AppScreen.CLINICAL_AI_SUITE -> ClinicalIntelligenceScreen(viewModel = viewModel)
        AppScreen.AI_VIDEO_CALL -> AiVideoCallScreen(viewModel = viewModel)
        AppScreen.DAILY_SYMPTOM_LOGGER -> DailySymptomLoggerScreen(viewModel = viewModel)
        AppScreen.DEVELOPER_LOGS -> {
            val authSession by viewModel.authSession.collectAsState()
            if (authSession.isDeveloper) {
                DeveloperLogsScreen(viewModel = viewModel)
            } else {
                AuthScreen(
                    viewModel = viewModel,
                    initialTab = 3,
                    onAuthSuccess = {
                        viewModel.setScreen(
                            if (viewModel.authSession.value.isDeveloper) AppScreen.DEVELOPER_LOGS
                            else AppScreen.TRIAGE_CHAT
                        )
                    }
                )
            }
        }
        AppScreen.DEVELOPER_CONSOLE -> {
            val authSession by viewModel.authSession.collectAsState()
            if (authSession.isDeveloper) {
                DeveloperControlPanelScreen(viewModel = viewModel)
            } else {
                AuthScreen(
                    viewModel = viewModel,
                    initialTab = 3,
                    onAuthSuccess = {
                        viewModel.setScreen(
                            if (viewModel.authSession.value.isDeveloper) AppScreen.DEVELOPER_CONSOLE
                            else AppScreen.TRIAGE_CHAT
                        )
                    }
                )
            }
        }
        AppScreen.AI_MEDICINE_ADVISOR -> AiMedicineAdvisorScreen(viewModel = viewModel)
        AppScreen.ULTRA_HEALTH_HUB -> UltraHealthHubScreen(viewModel = viewModel)
        AppScreen.AUTH_LOGIN -> AuthScreen(
            viewModel = viewModel,
            onAuthSuccess = { viewModel.setScreen(AppScreen.TRIAGE_CHAT) }
        )
    }
}


package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ChatTriageScreen
import com.example.ui.screens.EmergencyGuideScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WaterReminderScreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.MediAssistTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediAssistTheme {
                val viewModel: MediAssistViewModel = viewModel()
                MediAssistApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MediAssistApp(viewModel: MediAssistViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Handle back button on secondary screens
    if (currentScreen != AppScreen.TRIAGE_CHAT) {
        BackHandler {
            viewModel.setScreen(AppScreen.TRIAGE_CHAT)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("main_bottom_nav")) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.TRIAGE_CHAT,
                    onClick = { viewModel.setScreen(AppScreen.TRIAGE_CHAT) },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == AppScreen.TRIAGE_CHAT) Icons.Filled.HealthAndSafety else Icons.Outlined.HealthAndSafety,
                            contentDescription = "Triage"
                        )
                    },
                    label = { Text("Triage") },
                    modifier = Modifier.testTag("nav_triage_tab")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.WATER_REMINDER,
                    onClick = { viewModel.setScreen(AppScreen.WATER_REMINDER) },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == AppScreen.WATER_REMINDER) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop,
                            contentDescription = "Water Reminder (પાણી રિમાઇન્ડર)"
                        )
                    },
                    label = { Text("Water") },
                    modifier = Modifier.testTag("nav_water_tab")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.EMERGENCY_GUIDE,
                    onClick = { viewModel.setScreen(AppScreen.EMERGENCY_GUIDE) },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == AppScreen.EMERGENCY_GUIDE) Icons.Filled.Emergency else Icons.Outlined.Emergency,
                            contentDescription = "Emergency",
                            tint = if (currentScreen == AppScreen.EMERGENCY_GUIDE) EmergencyRed else androidx.compose.material3.LocalContentColor.current
                        )
                    },
                    label = { Text("Emergency") },
                    modifier = Modifier.testTag("nav_emergency_tab")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.HISTORY,
                    onClick = { viewModel.setScreen(AppScreen.HISTORY) },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == AppScreen.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "Health Log"
                        )
                    },
                    label = { Text("Health Log") },
                    modifier = Modifier.testTag("nav_history_tab")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.PROFILE,
                    onClick = { viewModel.setScreen(AppScreen.PROFILE) },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == AppScreen.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile") },
                    modifier = Modifier.testTag("nav_profile_tab")
                )
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.TRIAGE_CHAT -> ChatTriageScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppScreen.WATER_REMINDER -> WaterReminderScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppScreen.EMERGENCY_GUIDE -> EmergencyGuideScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppScreen.HISTORY -> HistoryScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppScreen.PROFILE -> ProfileScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

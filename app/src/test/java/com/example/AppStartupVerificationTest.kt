package com.example

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import com.example.ui.viewmodel.UltraHealthTab
import com.example.utils.AuthRole
import com.example.utils.AuthSecurityHelper
import com.example.utils.AuthSessionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppStartupVerificationTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun `MainActivity Compose UI renders cleanly when logged out AND logged in across Home and all 5 Ultra tabs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("app_language_prefs", Context.MODE_PRIVATE)
            .edit().putString("selected_language", "English").commit()

        // 1. Logged out startup (AnimatedSplashScreen -> AuthScreen full Compose render)
        AuthSecurityHelper.clearSession(context)
        composeTestRule.mainClock.autoAdvance = false
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeTestRule.mainClock.advanceTimeBy(3000L)
            composeTestRule.onNodeWithTag("animated_splash_screen").assertIsDisplayed()
            composeTestRule.onNodeWithTag("splash_app_logo").assertIsDisplayed()
            composeTestRule.onNodeWithText("Created by Shailesh and Ruturaj").assertIsDisplayed()
            composeTestRule.onNodeWithText("Model School Sayla").assertIsDisplayed()
            composeTestRule.onNodeWithTag("animated_splash_screen").performClick()
            composeTestRule.mainClock.autoAdvance = true
            composeTestRule.waitForIdle()
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }

        // 2. Logged in startup (AnimatedSplashScreen auto-finishes -> ChatTriageScreen + Ultra Hero Card + NEAR PMJAY button + all 5 Ultra tabs)
        AuthSecurityHelper.saveSession(
            context,
            AuthSessionState(
                isLoggedIn = true,
                username = "testpatient",
                displayName = "Test Patient",
                role = AuthRole.PATIENT,
                authMethod = "PASSWORD"
            )
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("home_ultra_hub_hero_card").assertIsDisplayed()
            composeTestRule.onNodeWithTag("home_near_pmjay_hospital_btn").performScrollTo().assertIsDisplayed()

            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity)[MediAssistViewModel::class.java]
                assertEquals(AppScreen.TRIAGE_CHAT, vm.currentScreen.value)
                vm.openUltraHealthHub(UltraHealthTab.SCORE)
            }
            composeTestRule.waitForIdle()

            for (tab in UltraHealthTab.entries) {
                scenario.onActivity { activity ->
                    val vm = ViewModelProvider(activity)[MediAssistViewModel::class.java]
                    vm.selectUltraHealthTab(tab)
                }
                composeTestRule.waitForIdle()
            }

            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity)[MediAssistViewModel::class.java]
                vm.setScreen(AppScreen.SMART_HUB)
            }
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun `MainActivity opens cleanly in Gujarati and RTL Urdu locales without configuration crash`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AuthSecurityHelper.saveSession(
            context,
            AuthSessionState(
                isLoggedIn = true,
                username = "localeuser",
                displayName = "Locale User",
                role = AuthRole.PATIENT,
                authMethod = "PASSWORD"
            )
        )

        for (lang in listOf("Gujarati", "Urdu", "English")) {
            context.getSharedPreferences("app_language_prefs", Context.MODE_PRIVATE)
                .edit().putString("selected_language", lang).commit()

            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                composeTestRule.waitForIdle()
                scenario.onActivity { activity ->
                    val vm = ViewModelProvider(activity)[MediAssistViewModel::class.java]
                    assertEquals(lang, vm.selectedLanguage.value)
                }
            }
        }
    }

    @Test
    fun `PmjayPortalHelper builds direct ACTION_VIEW browser intent for hem nha gov in search`() {
        val intent = com.example.utils.PmjayPortalHelper.buildSearchIntent()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://hem.nha.gov.in/search", intent.data?.toString())
        assertTrue(intent.categories?.contains(Intent.CATEGORY_BROWSABLE) == true)
    }

    @Test
    fun `AppUpgradeDownloadHelper normalizes download URL and builds browsable ACTION_VIEW intent`() {
        assertEquals(
            "https://github.com/example/healthcare-ai/releases",
            com.example.utils.AppUpgradeDownloadHelper.normalizeDownloadUrl("github.com/example/healthcare-ai/releases")
        )
        val intent = com.example.utils.AppUpgradeDownloadHelper.buildDownloadIntent(
            "https://github.com/example/healthcare-ai/releases/latest"
        )
        assertNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent!!.action)
        assertEquals("https://github.com/example/healthcare-ai/releases/latest", intent.data?.toString())
        assertTrue(intent.categories?.contains(Intent.CATEGORY_BROWSABLE) == true)
    }

    @Test
    fun `MainActivity opens cleanly from Home Screen Widget deep links`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AuthSecurityHelper.saveSession(
            context,
            AuthSessionState(
                isLoggedIn = true,
                username = "widgetuser",
                displayName = "Widget User",
                role = AuthRole.PATIENT,
                authMethod = "PASSWORD"
            )
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_SCREEN, "ULTRA_HEALTH_HUB")
        }
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            composeTestRule.waitForIdle()
            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity)[MediAssistViewModel::class.java]
                assertEquals(AppScreen.ULTRA_HEALTH_HUB, vm.currentScreen.value)
            }
        }
    }

    @Test
    fun `Room MIGRATION_15_16_17_18 upgrades existing v15 SQLite database without crashing`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_v15_to_v18.db"
        context.deleteDatabase(dbName)
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val tempV18 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        tempV18.openHelper.writableDatabase
        tempV18.close()

        val rawDb = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        rawDb.execSQL("DROP TABLE IF EXISTS `sleep_wellness_logs`")
        rawDb.execSQL("DROP TABLE IF EXISTS `meal_nutrition_logs`")
        rawDb.version = 15
        rawDb.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18
            )
            .allowMainThreadQueries()
            .build()
        try {
            val sleepLogs = migrated.triageDao().getAllSleepWellnessLogs().first()
            assertTrue(sleepLogs.isEmpty())
            val meals = migrated.triageDao().getAllMealNutritionLogs().first()
            assertTrue(meals.isEmpty())
            val cfg = migrated.triageDao().getDevAppConfigOnce()
            assertEquals(null, cfg)
        } finally {
            migrated.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `Room MIGRATION_14_15_16_17_18 is idempotent even if columns already exist`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_v14_idempotent.db"
        context.deleteDatabase(dbName)
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val tempV18 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        tempV18.openHelper.writableDatabase
        tempV18.close()

        // Simulate a v14 DB that ALREADY has columns (duplicate-column edge case)
        val rawDb = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        rawDb.execSQL("DROP TABLE IF EXISTS `sleep_wellness_logs`")
        rawDb.execSQL("DROP TABLE IF EXISTS `meal_nutrition_logs`")
        rawDb.version = 14
        rawDb.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18
            )
            .allowMainThreadQueries()
            .build()
        try {
            val sleepLogs = migrated.triageDao().getAllSleepWellnessLogs().first()
            assertTrue(sleepLogs.isEmpty())
            val meals = migrated.triageDao().getAllMealNutritionLogs().first()
            assertTrue(meals.isEmpty())
            val profile = migrated.triageDao().getUserProfile().first()
            assertEquals(null, profile)
        } finally {
            migrated.close()
            context.deleteDatabase(dbName)
        }
    }
}

package com.example

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.MealNutritionLogEntity
import com.example.data.local.MedicineEntity
import com.example.data.local.UserProfileEntity
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import com.example.ui.viewmodel.UltraHealthTab
import com.example.utils.CoachTimeSlot
import com.example.utils.DailyHealthScoreReport
import com.example.utils.HealthPassportPdfGenerator
import com.example.utils.HealthScoreTier
import com.example.utils.NutritionPresetCatalog
import com.example.utils.UltraHealthCoachEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UltraClinicalUpgradeTest {

    @Test
    fun `Room MIGRATION_17_18 creates meal_nutrition_logs and persists meal entries`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_ultra_migration_17_18.db"
        context.deleteDatabase(dbName)
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        // Create current DB then downgrade to v17 without meal_nutrition_logs
        val initDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        initDb.openHelper.writableDatabase
        initDb.close()

        val rawDb = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        rawDb.execSQL("DROP TABLE IF EXISTS `meal_nutrition_logs`")
        rawDb.version = 17
        rawDb.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_17_18)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = migrated.triageDao()
            dao.insertMealNutritionLog(
                MealNutritionLogEntity(
                    dateKey = "2026-11-24",
                    mealType = "Breakfast",
                    foodName = "Poha / Upma (1 Bowl)",
                    calories = 260,
                    proteinGrams = 6.5,
                    carbsGrams = 42.0,
                    fiberGrams = 4.0
                )
            )
            val saved = dao.getMealNutritionLogsForDate("2026-11-24").first()
            assertEquals(1, saved.size)
            assertEquals(260, saved.first().calories)
            assertEquals("Poha / Upma (1 Bowl)", saved.first().foodName)
        } finally {
            migrated.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `NutritionPresetCatalog computes daily calorie and macro summary accurately`() {
        assertTrue(NutritionPresetCatalog.presets.size >= 10)
        val logs = listOf(
            MealNutritionLogEntity(
                dateKey = "2026-11-24",
                mealType = "Breakfast",
                foodName = "Oats",
                calories = 310,
                proteinGrams = 12.0,
                carbsGrams = 44.0,
                fiberGrams = 7.0
            ),
            MealNutritionLogEntity(
                dateKey = "2026-11-24",
                mealType = "Lunch",
                foodName = "Roti Dal",
                calories = 420,
                proteinGrams = 16.0,
                carbsGrams = 64.0,
                fiberGrams = 9.0
            ),
            MealNutritionLogEntity(
                dateKey = "2026-11-23",
                mealType = "Dinner",
                foodName = "Yesterday Meal",
                calories = 999,
                proteinGrams = 30.0,
                carbsGrams = 100.0,
                fiberGrams = 10.0
            )
        )
        val summary = NutritionPresetCatalog.calculateDailySummary(
            logs = logs,
            dateKey = "2026-11-24",
            targetCalories = 2000
        )
        assertEquals(730, summary.totalCalories)
        assertEquals(2000, summary.targetCalories)
        assertEquals(0.365f, summary.progressFraction, 0.01f)
        assertEquals(28.0, summary.totalProteinGrams, 0.01)
        assertEquals(108.0, summary.totalCarbsGrams, 0.01)
        assertEquals(16.0, summary.totalFiberGrams, 0.01)
        assertEquals(2, summary.mealCount)
    }

    @Test
    fun `UltraHealthCoachEngine builds structured Morning Afternoon Evening plan and brief`() {
        val plan = UltraHealthCoachEngine.buildDailyPlan(
            dateKey = "2026-11-24",
            healthScore = 78,
            todayWaterMl = 1250,
            waterTargetMl = 2500,
            todaySteps = 4200,
            stepTarget = 8000,
            todayCalories = 980,
            calorieTarget = 2000,
            activeMedicines = listOf(
                MedicineEntity(name = "Metformin", dosage = "500mg", timeOfDay = "08:00 AM")
            ),
            sleepLogs = emptyList(),
            vitalLogs = emptyList(),
            language = "English"
        )
        assertTrue(plan.readinessHeadline.isNotBlank())
        assertTrue(plan.summaryBrief.isNotBlank())
        assertTrue(plan.tasks.any { it.slot == CoachTimeSlot.MORNING })
        assertTrue(plan.tasks.any { it.slot == CoachTimeSlot.AFTERNOON })
        assertTrue(plan.tasks.any { it.slot == CoachTimeSlot.EVENING })
        assertTrue(plan.tasks.any { it.title.contains("Metformin") })
    }

    @Test
    fun `HealthPassportPdfGenerator creates valid non-empty A4 PDF file in cache exports`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val report = DailyHealthScoreReport(
            overallScore = 88,
            tier = HealthScoreTier.ULTRA_OPTIMAL,
            pillars = emptyList(),
            insights = emptyList()
        )
        val snapshot = HealthPassportPdfGenerator.buildSnapshot(
            profile = UserProfileEntity(
                fullName = "Aarav Patel",
                age = 32,
                gender = "Male",
                bloodGroup = "O+",
                allergies = "Penicillin",
                chronicConditions = "Mild Asthma",
                emergencyContactName = "Meera Patel",
                emergencyContactPhone = "9876543210",
                pmjayCardNumber = "PMJAY-1234-5678",
                abhaCardNumber = "14-1234-5678-9012"
            ),
            fallbackName = "Aarav",
            scoreReport = report,
            todaySteps = 6500,
            todayWaterMl = 2000,
            todayCalories = 1450,
            activeMedicines = listOf(MedicineEntity(name = "Vitamin D3", dosage = "60K IU", timeOfDay = "09:00 AM")),
            latestVitals = emptyList()
        )
        val pdfFile = HealthPassportPdfGenerator.generatePdfFile(context, snapshot)
        try {
            assertTrue(pdfFile.exists())
            assertTrue(pdfFile.length() > 500L)
            val header = pdfFile.readBytes().copyOfRange(0, 5).decodeToString()
            assertEquals("%PDF-", header)
        } finally {
            pdfFile.delete()
        }
    }

    @Test
    fun `MediAssistViewModel handles Ultra Coach tasks, Shake SOS toggle, and Body Map triage navigation`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = MediAssistViewModel(app)

        vm.openUltraHealthHub(UltraHealthTab.AI_COACH)
        assertEquals(AppScreen.ULTRA_HEALTH_HUB, vm.currentScreen.value)
        assertEquals(UltraHealthTab.AI_COACH, vm.ultraHealthTab.value)

        assertFalse(vm.completedCoachTaskIds.value.contains("morning_hydration"))
        vm.toggleCoachTaskCompleted("morning_hydration")
        assertTrue(vm.completedCoachTaskIds.value.contains("morning_hydration"))
        vm.toggleCoachTaskCompleted("morning_hydration")
        assertFalse(vm.completedCoachTaskIds.value.contains("morning_hydration"))

        vm.setShakeToSosEnabled(true)
        assertTrue(vm.shakeToSosEnabled.value)
        vm.setShakeToSosEnabled(false)
        assertFalse(vm.shakeToSosEnabled.value)

        vm.launchBodyMapTriage(
            zoneTitle = "Chest & Heart",
            symptoms = listOf("Chest Tightness", "Shortness of Breath"),
            severity = "Moderate",
            duration = "Today",
            additionalNotes = "Started after climbing stairs"
        )
        assertEquals(AppScreen.TRIAGE_CHAT, vm.currentScreen.value)
    }

    @Test
    fun `All 18 added languages translate new Ultra Upgrade strings accurately`() {
        val addedLanguages = listOf(
            "Marathi", "Bengali", "Tamil", "Telugu", "Kannada", "Malayalam",
            "Punjabi", "Urdu", "Odia", "Assamese",
            "Spanish", "French", "German", "Arabic", "Portuguese", "Russian", "Japanese", "Chinese"
        )
        val ultraPhrases = listOf(
            "AI Coach",
            "Body Map",
            "Nutrition",
            "Interactive Symptom Body Map",
            "Analyze with AI Triage",
            "Emergency SOS Shield",
            "Export & Share Health Passport PDF",
            "Generate Fresh AI Plan",
            "Smart Nutrition & Calories"
        )
        for (lang in addedLanguages) {
            for (phrase in ultraPhrases) {
                val explicit = com.example.ui.theme.i18n.ExtendedLanguagesDictionary.translate(lang, phrase)
                assertTrue("Missing dictionary entry for $lang / $phrase", !explicit.isNullOrBlank())
                val translated = trLang(lang, phrase, "ગુજરાતી", "हिन्दी")
                assertTrue("Blank translation for $lang / $phrase", translated.isNotBlank())
                if (!(lang == "French" && phrase == "Nutrition")) {
                    assertNotEquals("Untranslated fallback for $lang / $phrase", phrase, translated)
                }
            }
        }
    }
}

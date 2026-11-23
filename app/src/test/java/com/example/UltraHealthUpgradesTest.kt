package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.MedicationAdherenceEntity
import com.example.data.local.MedicineEntity
import com.example.data.local.ScannedReportEntity
import com.example.data.local.SleepWellnessLogEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.VitalLogEntity
import com.example.utils.ActivityLevel
import com.example.utils.BiomarkerAlertLevel
import com.example.utils.BiomarkerTrendDirection
import com.example.utils.EmergencyQrCodeGenerator
import com.example.utils.HealthScoreTier
import com.example.utils.LabReportTrendEngine
import com.example.utils.MedicationStreakEngine
import com.example.utils.UltraHealthScoreEngine
import com.example.utils.WellnessCalculatorEngine
import com.example.widget.HealthSummaryWidgetProvider
import com.example.widget.HealthWidgetSnapshot
import com.example.widget.HealthWidgetSnapshotStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UltraHealthUpgradesTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `UltraHealthScoreEngine calculates composite score and actionable insights`() {
        val report = UltraHealthScoreEngine.calculate(
            todayWaterMl = 2500,
            waterTargetMl = 2500,
            todaySteps = 8200,
            stepTarget = 8000,
            activeMedicines = emptyList(),
            adherenceLogs = emptyList(),
            sleepLogs = listOf(
                SleepWellnessLogEntity(dateKey = "2026-11-20", sleepHours = 8.0, sleepQuality = 5)
            ),
            vitalLogs = listOf(
                VitalLogEntity(vitalType = "Blood Pressure", primaryValue = 118.0, secondaryValue = 78.0, unit = "mmHg", status = "Normal")
            )
        )

        assertEquals(100, report.overallScore)
        assertEquals(HealthScoreTier.ULTRA_OPTIMAL, report.tier)
        assertEquals(5, report.pillars.size)
        assertTrue(report.insights.isNotEmpty())
    }

    @Test
    fun `MedicationStreakEngine computes consecutive day streak and 7-day adherence`() {
        val now = 1_795_140_000_000L
        val dayMs = 86_400_000L
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val med = MedicineEntity(id = 1L, name = "Metformin", dosage = "500mg", timeOfDay = "08:00 AM", isActive = true)
        val logs = (0..3).map { d ->
            MedicationAdherenceEntity(
                medicineId = 1L,
                medicineName = "Metformin",
                dosage = "500mg",
                scheduledTime = "08:00 AM",
                scheduledDate = fmt.format(Date(now - d * dayMs)),
                status = "TAKEN",
                actionTimestamp = now - d * dayMs
            )
        }

        val analytics = MedicationStreakEngine.analyze(
            medicines = listOf(med),
            adherenceLogs = logs,
            referenceTimeMs = now
        )

        assertEquals(4, analytics.currentStreakDays)
        assertEquals(4, analytics.bestStreakDays)
        assertEquals(100, analytics.adherenceRate7d)
        assertEquals(0, analytics.pendingTodayCount)
        assertEquals(7, analytics.dailyPoints7d.size)
    }

    @Test
    fun `WellnessCalculatorEngine computes WHO and Asian-Indian BMI, BMR, HR zones, and Sleep Debt`() {
        val bmi = WellnessCalculatorEngine.calculateBmi(weightKg = 70.0, heightCm = 172.0)
        assertEquals(23.7, bmi.bmi, 0.1)
        assertEquals("Normal", bmi.whoCategory)
        assertEquals("Overweight (At Risk)", bmi.asianIndianCategory)

        val metabolic = WellnessCalculatorEngine.calculateMetabolicAndHeart(
            weightKg = 70.0,
            heightCm = 172.0,
            ageYears = 30,
            isMale = true,
            activityLevel = ActivityLevel.MODERATE
        )
        assertEquals(1630, metabolic.bmrKcal)
        assertEquals(190, metabolic.maxHeartRateBpm)
        assertTrue(metabolic.tdeeKcal > metabolic.bmrKcal)

        val sleepDebt = WellnessCalculatorEngine.analyzeSleepDebt(
            listOf(
                SleepWellnessLogEntity(dateKey = "2026-11-20", sleepHours = 6.0, sleepQuality = 3),
                SleepWellnessLogEntity(dateKey = "2026-11-19", sleepHours = 6.5, sleepQuality = 4)
            )
        )
        assertEquals(3.5, sleepDebt.weeklySleepDebtHours, 0.05)
        assertEquals(2, sleepDebt.entriesCount)
    }

    @Test
    fun `LabReportTrendEngine extracts biomarkers across reports and classifies deltas and alerts`() {
        val older = ScannedReportEntity(
            timestamp = 1_000_000L,
            title = "Lab Report Jan",
            extractedText = "Hemoglobin: 10.5 g/dL\nHbA1c: 7.4 %\nTotal Cholesterol: 245 mg/dL",
            aiExplanation = ""
        )
        val newer = ScannedReportEntity(
            timestamp = 2_000_000L,
            title = "Lab Report Apr",
            extractedText = "Hemoglobin: 12.8 g/dL\nHbA1c: 6.4 %\nTotal Cholesterol: 260 mg/dL",
            aiExplanation = ""
        )

        val trends = LabReportTrendEngine.analyzeTrends(listOf(older, newer))
        val hb = trends.firstOrNull { it.key == "hb" }
        val hba1c = trends.firstOrNull { it.key == "hba1c" }
        val chol = trends.firstOrNull { it.key == "cholesterol" }

        assertNotNull(hb)
        assertEquals(BiomarkerAlertLevel.NORMAL, hb!!.alertLevel)
        assertEquals(BiomarkerTrendDirection.IMPROVED, hb.trendDirection)

        assertNotNull(hba1c)
        assertEquals(BiomarkerTrendDirection.IMPROVED, hba1c!!.trendDirection)
        assertEquals(-1.0, hba1c.delta!!, 0.05)

        assertNotNull(chol)
        assertEquals(BiomarkerAlertLevel.HIGH_ALERT, chol!!.alertLevel)
        assertEquals(BiomarkerTrendDirection.WORSENED, chol.trendDirection)
    }

    @Test
    fun `EmergencyQrCodeGenerator formats PMJAY and ABHA cards and produces valid QR matrix`() {
        val profile = UserProfileEntity(
            fullName = "Aarav Patel",
            age = 35,
            bloodGroup = "O+",
            allergies = "Penicillin",
            emergencyContactName = "Meera",
            emergencyContactPhone = "9876543210",
            pmjayCardNumber = "PM12AB34",
            abhaCardNumber = "12345678901234"
        )
        val payload = EmergencyQrCodeGenerator.buildEmergencyPayload(profile)
        assertTrue(payload.contains("PMJAY: PM12AB34"))
        assertTrue(payload.contains("ABHA: 12-3456-7890-1234"))
        assertTrue(payload.contains("Blood: O+"))

        val matrix = EmergencyQrCodeGenerator.encode(payload)
        assertTrue(matrix.size >= 29)
        // Verify top-left, top-right, bottom-left 7x7 finder pattern corners are dark
        assertTrue(matrix.isDark(0, 0))
        assertTrue(matrix.isDark(6, 0))
        assertTrue(matrix.isDark(0, 6))
        assertTrue(matrix.isDark(3, 3))
        assertTrue(matrix.isDark(matrix.size - 1, 0))
        assertTrue(matrix.isDark(0, matrix.size - 1))
    }

    @Test
    fun `Room v16 persists SleepWellnessLogEntity and Widget snapshot builds RemoteViews cleanly`() = runBlocking {
        db.triageDao().insertSleepWellnessLog(
            SleepWellnessLogEntity(
                dateKey = "2026-11-20",
                sleepHours = 7.5,
                sleepQuality = 4,
                weightKg = 68.0,
                heightCm = 171.0
            )
        )
        val saved = db.triageDao().getAllSleepWellnessLogs().first()
        assertEquals(1, saved.size)
        assertEquals(7.5, saved.first().sleepHours, 0.01)

        val snapshot = HealthWidgetSnapshot(
            healthScore = 91,
            scoreTierLabel = "Ultra Optimal",
            stepsToday = 8450,
            stepTarget = 8000,
            waterMlToday = 2250,
            waterTargetMl = 2500,
            nextMedicineLabel = "Next Rx: Vitamin D (08:00 AM)",
            streakDays = 6
        )
        HealthWidgetSnapshotStore.saveSnapshot(context, snapshot)
        val loaded = HealthWidgetSnapshotStore.loadSnapshot(context)
        assertEquals(91, loaded.healthScore)
        assertEquals(8450, loaded.stepsToday)
        assertEquals(6, loaded.streakDays)

        val remoteViews = HealthSummaryWidgetProvider.buildRemoteViews(context, loaded)
        assertNotNull(remoteViews)
    }
}

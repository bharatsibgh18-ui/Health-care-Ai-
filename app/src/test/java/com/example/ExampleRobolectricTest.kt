package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ScannedReportEntity
import com.example.data.local.StepRecordEntity
import com.example.data.local.SymptomLogEntity
import com.example.data.local.TriageSessionEntity
import com.example.data.local.UserProfileEntity
import com.example.data.repository.TriageRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: TriageRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TriageRepository(database.triageDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Health Care AI", appName)
    }

    @Test
    fun `detect emergency red flag keywords`() {
        assertTrue(repository.isEmergencyText("Patient is showing signs of face drooping and slurred speech"))
        assertTrue(repository.isEmergencyText("Sudden thunderclap headache with severe agony"))
        assertTrue(repository.isEmergencyText("Cannot breathe and choking"))
        assertFalse(repository.isEmergencyText("I have a mild runny nose and cough"))
        assertFalse(repository.isEmergencyText("muje sir dard hai"))
        assertFalse(repository.isEmergencyText("sar dard ho raha hai"))
        assertFalse(repository.isEmergencyText("I have a mild headache"))
        assertFalse(repository.isEmergencyText("minor headache with severity 3/10"))
        assertFalse(repository.isEmergencyText("slight tension headache from screen work"))
        assertFalse(repository.isEmergencyText("halka sir dard aur thoda body ache"))
    }

    @Test
    fun `insert and retrieve triage session in room database`() = runBlocking {
        val session = TriageSessionEntity(
            primarySymptom = "Dull headache",
            severity = 4,
            duration = "2 days",
            emergencyFlagged = false,
            recommendedSpecialist = "General Physician",
            summary = "Possible tension headache"
        )

        val id = repository.saveSession(session)
        assertTrue(id > 0)

        val sessions = repository.allSessions.first()
        assertEquals(1, sessions.size)
        assertEquals("Dull headache", sessions[0].primarySymptom)
        assertEquals("General Physician", sessions[0].recommendedSpecialist)
    }

    @Test
    fun `insert and retrieve symptom log and user profile in room database`() = runBlocking {
        val symptomLog = SymptomLogEntity(
            symptomTitle = "Throbbing headache",
            severity = 6,
            bodyLocation = "Forehead",
            duration = "3 days",
            notesOrTriggers = "After working on computer",
            status = "Ongoing"
        )
        val logId = repository.saveSymptomLog(symptomLog)
        assertTrue(logId > 0)

        val logs = repository.allSymptomLogs.first()
        assertEquals(1, logs.size)
        assertEquals("Throbbing headache", logs[0].symptomTitle)
        assertEquals("Forehead", logs[0].bodyLocation)
        assertEquals("Ongoing", logs[0].status)

        val profile = UserProfileEntity(
            id = 1,
            fullName = "Jane Doe",
            age = 34,
            gender = "Female",
            bloodGroup = "O+",
            allergies = "Penicillin",
            chronicConditions = "Asthma",
            currentMedications = "Albuterol Inhaler",
            pastSurgeries = "None",
            emergencyContactName = "John Doe",
            emergencyContactPhone = "9998887776",
            preferredLanguage = "English"
        )
        repository.saveUserProfile(profile)

        val retrievedProfile = repository.userProfile.first()
        assertEquals("Jane Doe", retrievedProfile?.fullName)
        assertEquals("Asthma", retrievedProfile?.chronicConditions)
        assertEquals("Albuterol Inhaler", retrievedProfile?.currentMedications)
    }

    @Test
    fun `filter and retrieve headache history for self-monitoring`() = runBlocking {
        // Insert a headache log
        val headache = SymptomLogEntity(
            symptomTitle = "Tension headache",
            severity = 5,
            bodyLocation = "Head",
            duration = "2 Days",
            notesOrTriggers = "Late night work",
            status = "Ongoing"
        )
        repository.saveSymptomLog(headache)

        // Insert a non-headache log
        val kneePain = SymptomLogEntity(
            symptomTitle = "Knee Joint Pain",
            severity = 4,
            bodyLocation = "Joints/Limbs",
            duration = "1 Week",
            notesOrTriggers = "After running",
            status = "Improving"
        )
        repository.saveSymptomLog(kneePain)

        // Verify headache filter only returns headache entries
        val headaches = repository.headacheLogs.first()
        assertEquals(1, headaches.size)
        assertEquals("Tension headache", headaches[0].symptomTitle)
        assertEquals(5, headaches[0].severity)
        assertEquals("2 Days", headaches[0].duration)
    }

    @Test
    fun `generate clinical summary export for doctor`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.viewmodel.MediAssistViewModel(app)

        val summary = viewModel.generateDoctorSummaryText()
        assertTrue(summary.contains("PATIENT CLINICAL HEALTH SUMMARY"))
        assertTrue(summary.contains("Doctor / Physician Review"))
        assertTrue(summary.contains("RECORDED SYMPTOM HISTORY"))
    }

    @Test
    fun `log and query water intake in room database`() = runBlocking {
        val date = "2026-09-30"
        repository.logWater(250, date)
        repository.logWater(500, date)

        val logs = repository.getWaterLogsForDate(date).first()
        assertEquals(2, logs.size)

        val total = repository.getTotalWaterForDate(date).first()
        assertEquals(750, total)
    }

    @Test
    fun `send gujarati triage query and verify spoken remedy solution`() = runBlocking {
        val query = "Aama bimari nu hal boli samja tu nathi te boline samjave aevu karo"
        val result = repository.sendTriageQuery(query, emptyList(), null)
        assertTrue(result.isSuccess)
        val text = result.getOrNull() ?: ""
        assertTrue(text.isNotBlank())
    }

    @Test
    fun `verify AppScreen enum contains APP_INFO`() {
        val screens = com.example.ui.viewmodel.AppScreen.values()
        assertTrue(screens.contains(com.example.ui.viewmodel.AppScreen.APP_INFO))
        assertEquals("APP_INFO", com.example.ui.viewmodel.AppScreen.APP_INFO.name)
    }

    @Test
    fun `verify theme mode toggles in view model`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.viewmodel.MediAssistViewModel(app)

        assertEquals(com.example.ui.theme.ThemeMode.SYSTEM, viewModel.themeMode.value)
        viewModel.toggleThemeMode()
        assertEquals(com.example.ui.theme.ThemeMode.DARK, viewModel.themeMode.value)
        viewModel.toggleThemeMode()
        assertEquals(com.example.ui.theme.ThemeMode.LIGHT, viewModel.themeMode.value)
        viewModel.toggleThemeMode()
        assertEquals(com.example.ui.theme.ThemeMode.SYSTEM, viewModel.themeMode.value)
    }

    @Test
    fun `verify localized navigation strings exist`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Triage", context.getString(R.string.nav_triage))
        assertEquals("Water", context.getString(R.string.nav_water))
        assertEquals("Emergency", context.getString(R.string.nav_emergency))
        assertEquals("Health Log", context.getString(R.string.nav_history))
        assertEquals("Profile", context.getString(R.string.nav_profile))
    }

    @Test
    fun `send medical image query and verify photo analysis response`() = runBlocking {
        val query = "આ ફોટામાં શું થયું છે અને કઈ દવા કરવી?"
        val sampleBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        val result = repository.sendTriageQuery(query, emptyList(), null, imageBase64 = sampleBase64, imageMimeType = "image/png")
        assertTrue(result.isSuccess)
        val text = result.getOrNull() ?: ""
        assertTrue(text.isNotBlank())
        assertTrue(text.contains("ફોટાનું ક્લિનિકલ નિરીક્ષણ") || text.contains("Visual") || text.contains("દવા") || text.contains("કારણો"))
    }

    @Test
    fun `verify gujarati localized emergency strings`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val config = android.content.res.Configuration(baseContext.resources.configuration)
        config.setLocale(java.util.Locale.forLanguageTag("gu"))
        val guContext = baseContext.createConfigurationContext(config)

        assertEquals("ઇમરજન્સી પ્રોટોકોલ", guContext.getString(R.string.emergency_protocol_title))
        assertEquals("તાત્કાલિક ઇમરજન્સી સેવા", guContext.getString(R.string.emergency_dispatch_title))
        assertEquals("🚨 જીવલેણ ઇમરજન્સી લક્ષણો (રેડ ફ્લેગ)", guContext.getString(R.string.emergency_red_flags_header))
        assertEquals("છાતીમાં અસહ્ય દુખાવો", guContext.getString(R.string.emergency_sign_chest_title))
        assertEquals("🧠 F.A.S.T. લકવા (સ્ટ્રોક) તપાસ માર્ગદર્શિકા", guContext.getString(R.string.emergency_fast_header))
        assertEquals("🩹 તાત્કાલિક પ્રાથમિક સારવારની સૂચનાઓ", guContext.getString(R.string.emergency_first_aid_header))
    }

    @Test
    fun `adjust and update medicine reminder time in room database`() = runBlocking {
        val med = com.example.data.local.MedicineEntity(
            name = "Paracetamol",
            dosage = "500mg",
            timeOfDay = "08:00 AM",
            instruction = "After Food",
            isActive = true
        )
        val id = repository.saveMedicine(med)
        val savedMed = med.copy(id = id)

        // Adjust time to 09:30 PM and update
        val updatedMed = savedMed.copy(
            timeOfDay = "09:30 PM",
            instruction = "At Bedtime"
        )
        repository.updateMedicine(updatedMed)

        // Retrieve and verify adjusted time
        val medicines = repository.allMedicines.first()
        val updated = medicines.first { it.id == id }
        assertEquals("09:30 PM", updated.timeOfDay)
        assertEquals("At Bedtime", updated.instruction)
    }

    @Test
    fun `verify MainActivity launches successfully without crashing`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        val activity = controller.get()
        org.junit.Assert.assertNotNull(activity)
        controller.pause().stop().destroy()
    }

    @Test
    fun `verify front page ChatTriageScreen loads default state`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.viewmodel.MediAssistViewModel(app)
        assertEquals(com.example.ui.viewmodel.AppScreen.TRIAGE_CHAT, viewModel.currentScreen.value)
        assertEquals(false, viewModel.showSymptomContextSheet.value)
        assertEquals("", viewModel.inputText.value)
    }

    @Test
    fun `insert and retrieve scanned report in room database`() = runBlocking {
        val report = ScannedReportEntity(
            title = "Complete Blood Count (CBC) Lab",
            extractedText = "Hemoglobin: 10.5 g/dL (Normal: 12.0-15.5)\nWBC: 7,800 /mcL\nPlatelets: 210,000 /mcL",
            aiExplanation = "Mild anemia noted from slightly low hemoglobin levels. Consult your physician.",
            reportType = "Lab Report"
        )
        val id = repository.saveScannedReport(report)
        assertTrue(id > 0)

        val list = repository.allScannedReports.first()
        assertEquals(1, list.size)
        assertEquals("Complete Blood Count (CBC) Lab", list[0].title)
        assertEquals("Lab Report", list[0].reportType)
        assertTrue(list[0].extractedText.contains("Hemoglobin"))
    }

    @Test
    fun `explain medical report or prescription fallback generates structured explanation`() = runBlocking {
        val ocrSample = "Rx\nTab Amoxicillin 500mg 1 tablet TID x 5 days\nTab Paracetamol 650mg SOS for fever\nAdv: Drink plenty of fluids"
        val result = repository.explainMedicalReportOrPrescription(
            extractedText = ocrSample,
            reportType = "Prescription",
            language = "English"
        )
        assertTrue(result.isSuccess)
        val explanation = result.getOrNull() ?: ""
        assertTrue(explanation.isNotBlank())
        assertTrue(explanation.contains("Prescription", ignoreCase = true) || explanation.contains("Medication", ignoreCase = true))
    }

    @Test
    fun `save and update daily steps in room database`() = runBlocking {
        val testDate = "2026-11-06"
        val id = repository.saveOrUpdateSteps(date = testDate, steps = 3500, targetSteps = 8000)
        assertTrue(id > 0)

        val record = repository.getStepRecordForDate(testDate).first()
        org.junit.Assert.assertNotNull(record)
        assertEquals(3500, record?.steps)
        assertEquals(8000, record?.targetSteps)
        assertTrue((record?.caloriesBurned ?: 0.0) > 0.0)
        assertTrue((record?.distanceMeters ?: 0.0) > 0.0)

        // Add 500 steps
        repository.addSteps(date = testDate, increment = 500)
        val updatedRecord = repository.getStepRecordForDate(testDate).first()
        assertEquals(4000, updatedRecord?.steps)

        // Update goal to 10,000
        repository.updateStepGoal(date = testDate, targetSteps = 10000)
        val finalRecord = repository.getStepRecordForDate(testDate).first()
        assertEquals(10000, finalRecord?.targetSteps)
    }

    @Test
    fun `verify AppScreen enum contains STEP_COUNTER`() {
        val screens = com.example.ui.viewmodel.AppScreen.values()
        assertTrue(screens.contains(com.example.ui.viewmodel.AppScreen.STEP_COUNTER))
    }
}


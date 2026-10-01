package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
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
}

package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.UserProfileEntity
import com.example.ui.viewmodel.MediAssistViewModel
import com.example.utils.HealthCardValidator
import com.example.utils.HealthDataExportHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HealthCardProfileTest {

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
    fun `PMJAY card validator enforces exactly 8 alphanumeric uppercase characters`() {
        assertEquals("AB12CD34", HealthCardValidator.sanitizePmjayInput("ab-12 cd@3499"))
        assertTrue(HealthCardValidator.isCompletePmjay("AB12CD34"))
        assertTrue(HealthCardValidator.isValidPmjayForSave("ab12cd34"))
        assertTrue(HealthCardValidator.isValidPmjayForSave(""))
        assertFalse(HealthCardValidator.isValidPmjayForSave("AB12CD3")) // 7 chars
        assertFalse(HealthCardValidator.isValidPmjayForSave("AB12CD345")) // 9 chars
        assertFalse(HealthCardValidator.isValidPmjayForSave("AB12-D34")) // symbol
    }

    @Test
    fun `ABHA card validator enforces exactly 14 numeric digits and formats cleanly`() {
        assertEquals("12345678901234", HealthCardValidator.sanitizeAbhaInput("12-3456-7890-123499ab"))
        assertTrue(HealthCardValidator.isCompleteAbha("12345678901234"))
        assertTrue(HealthCardValidator.isValidAbhaForSave("12345678901234"))
        assertTrue(HealthCardValidator.isValidAbhaForSave(""))
        assertFalse(HealthCardValidator.isValidAbhaForSave("1234567890123")) // 13 digits
        assertFalse(HealthCardValidator.isValidAbhaForSave("1234567890123A")) // letter
        assertEquals("12-3456-7890-1234", HealthCardValidator.formatAbhaForDisplay("12345678901234"))
    }

    @Test
    fun `UserProfileEntity persists PMJAY and ABHA card numbers in Room and clinical exports`() = runBlocking {
        val profile = UserProfileEntity(
            fullName = "Ramesh Patel",
            age = 42,
            gender = "Male",
            bloodGroup = "B+",
            pmjayCardNumber = "PM89XY12",
            abhaCardNumber = "91234567890123"
        )
        db.triageDao().insertOrUpdateProfile(profile)
        val loaded = db.triageDao().getUserProfileOnce()
        assertNotNull(loaded)
        assertEquals("PM89XY12", loaded!!.pmjayCardNumber)
        assertEquals("91234567890123", loaded.abhaCardNumber)

        val csv = HealthDataExportHelper.buildClinicalExportCsv(
            profile = loaded,
            medicines = emptyList(),
            adherenceLogs = emptyList(),
            symptomLogs = emptyList(),
            symptomNotes = emptyList()
        )
        assertTrue(csv.contains("PMJAY: PM89XY12"))
        assertTrue(csv.contains("ABHA: 91-2345-6789-0123"))

        val jsonStr = HealthDataExportHelper.buildClinicalExportJson(
            profile = loaded,
            medicines = emptyList(),
            adherenceLogs = emptyList(),
            symptomLogs = emptyList(),
            symptomNotes = emptyList()
        )
        val patientJson = JSONObject(jsonStr).getJSONObject("patientProfile")
        assertEquals("PM89XY12", patientJson.getString("pmjayCardNumber"))
        assertEquals("91234567890123", patientJson.getString("abhaCardNumber"))
    }

    @Test
    fun `ViewModel saveProfile sanitizes and saves PMJAY and ABHA cards`() = runBlocking {
        val app = context.applicationContext as Application
        val viewModel = MediAssistViewModel(app)
        viewModel.saveProfile(
            name = "Kiran Shah",
            age = 34,
            gender = "Female",
            bloodGroup = "O+",
            allergies = "None",
            chronicConditions = "None",
            medications = "None",
            pastSurgeries = "None",
            emergencyName = "Family",
            emergencyPhone = "112",
            language = "Gujarati",
            pmjayCardNumber = "gj01ab23",
            abhaCardNumber = "14-2536-4758-6970"
        )

        val dao = AppDatabase.getDatabase(app).triageDao()
        var saved: UserProfileEntity? = null
        val deadline = System.currentTimeMillis() + 5000L
        while (System.currentTimeMillis() < deadline && saved?.fullName != "Kiran Shah") {
            shadowOf(Looper.getMainLooper()).idle()
            saved = dao.getUserProfile().first()
            if (saved?.fullName != "Kiran Shah") delay(50)
        }
        assertNotNull(saved)
        assertEquals("GJ01AB23", saved!!.pmjayCardNumber)
        assertEquals("14253647586970", saved.abhaCardNumber)
    }
}

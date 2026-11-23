package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SymptomLogEntity
import com.example.data.repository.TriageRepository
import com.example.ui.viewmodel.AppScreen
import com.example.utils.DevLogger
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
class DailySymptomAndDevLoggerTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: TriageRepository

    @Before
    fun setUp() {
        DevLogger.clear()
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TriageRepository(database.triageDao())
    }

    @After
    fun tearDown() {
        DevLogger.attachPersistenceListener(null)
        database.close()
    }

    @Test
    fun `daily symptom with severity and timestamped follow-up notes persists and cascades in Room`() = runBlocking {
        val logId = repository.saveSymptomLog(
            SymptomLogEntity(
                symptomTitle = "Migraine Headache",
                severity = 7,
                bodyLocation = "Head",
                duration = "Today",
                notesOrTriggers = "Started after bright screen exposure",
                status = "Ongoing",
                dateKey = "2026-11-15"
            )
        )
        assertTrue(logId > 0L)

        // Append multiple timestamped follow-up notes
        repository.addTimestampedSymptomNote(
            symptomLogId = logId,
            noteText = "Drank water and rested in dark room",
            severitySnapshot = 6,
            timestamp = 1763200000000L
        )
        repository.addTimestampedSymptomNote(
            symptomLogId = logId,
            noteText = "Pain subsided significantly",
            severitySnapshot = 3,
            timestamp = 1763203600000L
        )

        val todayLogs = repository.getSymptomLogsByDate("2026-11-15").first()
        assertEquals(1, todayLogs.size)
        assertEquals(7, todayLogs[0].severity)

        val notes = repository.getNotesForSymptom(logId).first()
        assertEquals(2, notes.size)
        assertEquals(3, notes[0].severitySnapshot) // Latest note first (ORDER BY timestamp DESC)
        assertEquals("Pain subsided significantly", notes[0].noteText)

        // Cascade delete removes notes cleanly
        repository.deleteSymptomLog(todayLogs[0])
        val remainingNotes = repository.allSymptomNotes.first()
        assertFalse(remainingNotes.any { it.symptomLogId == logId })
    }

    @Test
    fun `developer logger buffers events persists to Room and exports formatted text`() = runBlocking {
        DevLogger.d("SymptomTest", "Checking diagnostic event capture")
        DevLogger.w("NetworkWarn", "Simulated timeout check")
        DevLogger.e("DbError", "Simulated recoverable fault")

        val buffered = DevLogger.logs.value
        assertEquals(3, buffered.size)
        assertEquals("ERROR", buffered[0].level)
        assertEquals("DbError", buffered[0].tag)

        val exported = DevLogger.exportLogsAsText()
        assertTrue(exported.contains("DEBUG/SymptomTest: Checking diagnostic event capture"))
        assertTrue(exported.contains("ERROR/DbError: Simulated recoverable fault"))

        repository.saveDeveloperLogToDb("INFO", "RoomCheck", "Persisted diagnostic message")
        val dbLogs = repository.allDeveloperLogs.first()
        assertEquals(1, dbLogs.size)
        assertEquals("RoomCheck", dbLogs[0].tag)

        repository.clearAllDeveloperLogs()
        assertTrue(repository.allDeveloperLogs.first().isEmpty())
    }

    @Test
    fun `AppScreen exposes DAILY_SYMPTOM_LOGGER and DEVELOPER_LOGS routes`() {
        val screens = AppScreen.entries
        assertTrue(screens.contains(AppScreen.DAILY_SYMPTOM_LOGGER))
        assertTrue(screens.contains(AppScreen.DEVELOPER_LOGS))
    }
}

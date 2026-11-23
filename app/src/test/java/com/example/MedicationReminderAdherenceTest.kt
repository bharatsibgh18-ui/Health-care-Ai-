package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.local.MedicineEntity
import com.example.data.repository.TriageRepository
import com.example.utils.MedicineReminderWorker
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
import java.util.Calendar
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MedicationReminderAdherenceTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: TriageRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        try {
            WorkManager.initialize(
                context,
                Configuration.Builder().build()
            )
        } catch (_: IllegalStateException) {
            // Already initialized in test suite
        }
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
    fun `parseTimeOfDay accurately converts 12h and 24h formats`() {
        assertEquals(8 to 0, MedicineReminderWorker.parseTimeOfDay("08:00 AM"))
        assertEquals(0 to 15, MedicineReminderWorker.parseTimeOfDay("12:15 AM"))
        assertEquals(12 to 30, MedicineReminderWorker.parseTimeOfDay("12:30 PM"))
        assertEquals(21 to 45, MedicineReminderWorker.parseTimeOfDay("09:45 PM"))
        assertEquals(14 to 5, MedicineReminderWorker.parseTimeOfDay("14:05"))
    }

    @Test
    fun `computeInitialDelayMillis targets upcoming occurrence within interval`() {
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 15)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delayLaterToday = MedicineReminderWorker.computeInitialDelayMillis(
            timeOfDay = "01:30 PM",
            frequencyHours = 24,
            nowMillis = nowCal.timeInMillis
        )
        assertEquals(TimeUnit.MINUTES.toMillis(210), delayLaterToday)

        val delayTomorrow = MedicineReminderWorker.computeInitialDelayMillis(
            timeOfDay = "08:00 AM",
            frequencyHours = 24,
            nowMillis = nowCal.timeInMillis
        )
        assertEquals(TimeUnit.HOURS.toMillis(22), delayTomorrow)
    }

    @Test
    fun `schedule and cancel medication reminder enqueues unique WorkManager job`() {
        val medId = 42L
        MedicineReminderWorker.scheduleDailyMedicineReminder(
            context = context,
            medId = medId,
            medName = "Metformin",
            dosage = "500mg",
            instruction = "After Food",
            timeOfDay = "08:00 AM",
            frequencyHours = 12
        )

        val workManager = WorkManager.getInstance(context)
        val scheduledInfos = workManager
            .getWorkInfosForUniqueWork("${MedicineReminderWorker.WORK_NAME_PREFIX}$medId")
            .get()
        assertEquals(1, scheduledInfos.size)
        assertTrue(
            scheduledInfos[0].state == WorkInfo.State.ENQUEUED ||
                scheduledInfos[0].state == WorkInfo.State.RUNNING
        )

        MedicineReminderWorker.scheduleSnoozeReminder(
            context = context,
            medId = medId,
            medName = "Metformin",
            dosage = "500mg",
            instruction = "After Food",
            timeOfDay = "08:00 AM",
            snoozeMinutes = 15
        )
        val snoozeInfos = workManager
            .getWorkInfosForUniqueWork("${MedicineReminderWorker.SNOOZE_WORK_PREFIX}$medId")
            .get()
        assertEquals(1, snoozeInfos.size)

        MedicineReminderWorker.cancelMedicineReminder(context, medId)
        val afterCancel = workManager
            .getWorkInfosForUniqueWork("${MedicineReminderWorker.WORK_NAME_PREFIX}$medId")
            .get()
        assertEquals(WorkInfo.State.CANCELLED, afterCancel[0].state)
    }

    @Test
    fun `record update and cascade delete medication adherence history in Room`() = runBlocking {
        val med = MedicineEntity(
            name = "Amlodipine",
            dosage = "5mg",
            timeOfDay = "08:00 AM",
            instruction = "After Food",
            frequencyHours = 24,
            prescribedBy = "Dr. Satiya",
            isActive = true
        )
        val medId = repository.saveMedicine(med)
        val savedMed = med.copy(id = medId)
        val today = "2026-11-15"

        // Log SNOOZED first
        repository.recordMedicationAdherence(savedMed, "SNOOZED", today, "Snoozed 15m")
        var logsForDay = repository.getAdherenceLogsForDate(today).first()
        assertEquals(1, logsForDay.size)
        assertEquals("SNOOZED", logsForDay[0].status)

        // Update same day's adherence to TAKEN (should update existing row cleanly, no duplicate)
        repository.recordMedicationAdherence(savedMed, "TAKEN", today, "Taken with water")
        logsForDay = repository.getAdherenceLogsForDate(today).first()
        assertEquals(1, logsForDay.size)
        assertEquals("TAKEN", logsForDay[0].status)
        assertEquals("Taken with water", logsForDay[0].notes)

        // Log another day SKIPPED
        repository.recordMedicationAdherence(savedMed, "SKIPPED", "2026-11-14", "Fasting")
        val allForMed = repository.getAdherenceLogsForMedicine(medId).first()
        assertEquals(2, allForMed.size)

        // Deleting medicine cascades to adherence history rows
        repository.deleteMedicine(savedMed)
        val remaining = repository.allAdherenceLogs.first()
        assertFalse(remaining.any { it.medicineId == medId })
    }
}

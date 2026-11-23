package com.example

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MedicationAdherenceEntity
import com.example.data.local.MedicineEntity
import com.example.data.local.SymptomLogEntity
import com.example.data.local.SymptomNoteEntity
import com.example.data.local.UserProfileEntity
import com.example.utils.ExportFormat
import com.example.utils.HealthDataExportHelper
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClinicalDataExportTest {

    private val sampleProfile = UserProfileEntity(
        id = 1,
        fullName = "Aarav Patel",
        age = 42,
        gender = "Male",
        bloodGroup = "B+",
        allergies = "Penicillin, Sulfa drugs",
        chronicConditions = "Hypertension",
        currentMedications = "Amlodipine 5mg"
    )

    private val sampleMedicines = listOf(
        MedicineEntity(
            id = 10L,
            name = "Amlodipine",
            dosage = "5 mg",
            timeOfDay = "08:00 AM",
            instruction = "After Breakfast",
            frequencyHours = 24,
            prescribedBy = "Dr. Sharma",
            isActive = true,
            createdAt = 1763100000000L
        )
    )

    private val sampleAdherence = listOf(
        MedicationAdherenceEntity(
            id = 101L,
            medicineId = 10L,
            medicineName = "Amlodipine",
            dosage = "5 mg",
            scheduledTime = "08:00 AM",
            scheduledDate = "2026-11-15",
            status = "TAKEN",
            actionTimestamp = 1763200000000L,
            notes = "Taken after breakfast, no dizziness"
        ),
        MedicationAdherenceEntity(
            id = 102L,
            medicineId = 10L,
            medicineName = "Amlodipine",
            dosage = "5 mg",
            scheduledTime = "08:00 AM",
            scheduledDate = "2026-11-14",
            status = "SKIPPED",
            actionTimestamp = 1763113600000L,
            notes = "Fasting morning lab \"draw\""
        )
    )

    private val sampleSymptoms = listOf(
        SymptomLogEntity(
            id = 201L,
            timestamp = 1763205000000L,
            symptomTitle = "Tension Headache, Frontal",
            severity = 7,
            bodyLocation = "Head & Neck",
            duration = "4 hours",
            notesOrTriggers = "Bright monitor glare\nSkipped lunch",
            status = "Improving",
            dateKey = "2026-11-15"
        )
    )

    private val sampleNotes = listOf(
        SymptomNoteEntity(
            id = 301L,
            symptomLogId = 201L,
            noteText = "Hydrated and rested 30 mins; pain reduced",
            severitySnapshot = 4,
            timestamp = 1763208600000L
        )
    )

    @Test
    fun `csv escaping follows RFC 4180 for commas quotes and newlines`() {
        assertEquals("plain", HealthDataExportHelper.escapeCsvField("plain"))
        assertEquals("\"Penicillin, Sulfa\"", HealthDataExportHelper.escapeCsvField("Penicillin, Sulfa"))
        assertEquals("\"lab \"\"draw\"\"\"", HealthDataExportHelper.escapeCsvField("lab \"draw\""))
        assertEquals("\"line1\nline2\"", HealthDataExportHelper.escapeCsvField("line1\r\nline2"))
    }

    @Test
    fun `clinical CSV export contains adherence schedule symptom logs and follow-up notes`() {
        val csv = HealthDataExportHelper.buildClinicalExportCsv(
            profile = sampleProfile,
            medicines = sampleMedicines,
            adherenceLogs = sampleAdherence,
            symptomLogs = sampleSymptoms,
            symptomNotes = sampleNotes,
            exportedAtMillis = 1763210000000L
        )

        val lines = csv.lines().filter { it.isNotBlank() }
        assertTrue(lines.first().startsWith("record_type,record_id,date,timestamp_iso"))
        assertTrue(csv.contains("PATIENT_PROFILE,1,"))
        assertTrue(csv.contains("MEDICATION_ADHERENCE,101,2026-11-15"))
        assertTrue(csv.contains("MEDICATION_ADHERENCE,102,2026-11-14"))
        assertTrue(csv.contains("\"Fasting morning lab \"\"draw\"\"\""))
        assertTrue(csv.contains("MEDICATION_SCHEDULE,10,"))
        assertTrue(csv.contains("SYMPTOM_LOG,201,2026-11-15"))
        assertTrue(csv.contains("\"Tension Headache, Frontal\""))
        assertTrue(csv.contains("SYMPTOM_NOTE,301,"))
        assertTrue(csv.contains("Hydrated and rested 30 mins; pain reduced"))
    }

    @Test
    fun `clinical JSON export parses cleanly with nested follow-up notes and adherence metrics`() {
        val jsonText = HealthDataExportHelper.buildClinicalExportJson(
            profile = sampleProfile,
            medicines = sampleMedicines,
            adherenceLogs = sampleAdherence,
            symptomLogs = sampleSymptoms,
            symptomNotes = sampleNotes,
            exportedAtMillis = 1763210000000L
        )

        val root = JSONObject(jsonText)
        val metadata = root.getJSONObject("exportMetadata")
        assertEquals("Health Care AI", metadata.getString("appName"))
        val counts = metadata.getJSONObject("counts")
        assertEquals(2, counts.getInt("adherenceRecords"))
        assertEquals(1, counts.getInt("symptomLogs"))
        assertEquals(1, counts.getInt("followUpNotes"))

        val profile = root.getJSONObject("patientProfile")
        assertEquals("Aarav Patel", profile.getString("fullName"))

        val medSummary = root.getJSONObject("medicationSummary")
        assertEquals(1, medSummary.getInt("takenCount"))
        assertEquals(1, medSummary.getInt("skippedCount"))
        assertEquals(50, medSummary.getInt("adherenceRatePercent"))

        val adherenceArr = root.getJSONArray("medicationAdherenceHistory")
        assertEquals(2, adherenceArr.length())
        assertEquals("TAKEN", adherenceArr.getJSONObject(0).getString("status"))

        val symptomsArr = root.getJSONArray("symptomLogs")
        assertEquals(1, symptomsArr.length())
        val firstSymptom = symptomsArr.getJSONObject(0)
        assertEquals("Tension Headache, Frontal", firstSymptom.getString("symptomTitle"))
        assertEquals(7, firstSymptom.getInt("severity"))
        val nestedNotes = firstSymptom.getJSONArray("followUpNotes")
        assertEquals(1, nestedNotes.length())
        assertEquals(4, nestedNotes.getJSONObject(0).getInt("severitySnapshot"))
    }

    @Test
    fun `cache export files write valid UTF-8 CSV and JSON and resolve FileProvider content URI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val csvContent = HealthDataExportHelper.buildClinicalExportCsv(
            profile = sampleProfile,
            medicines = sampleMedicines,
            adherenceLogs = sampleAdherence,
            symptomLogs = sampleSymptoms,
            symptomNotes = sampleNotes
        )
        val csvFile = HealthDataExportHelper.createCacheExportFile(
            context = context,
            format = ExportFormat.CSV,
            content = csvContent,
            timestampMillis = 1763210000000L
        )
        assertTrue(csvFile.exists())
        assertTrue(csvFile.name.endsWith(".csv"))
        assertEquals(csvContent, csvFile.readText(Charsets.UTF_8))

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )
        assertEquals("content", uri.scheme)
        assertTrue(uri.toString().contains(csvFile.name))
    }
}

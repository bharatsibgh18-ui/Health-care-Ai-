package com.example

import com.example.data.local.StepRecordEntity
import com.example.data.local.SymptomLogEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.VitalLogEntity
import com.example.utils.ClinicalRiskEngine
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalRiskEngineTest {

    @Test
    fun computePredictiveRisks_flagsHighHeartAndSepsisRisks() {
        val profile = UserProfileEntity(
            fullName = "Test Patient",
            age = 62,
            chronicConditions = "Hypertension, Diabetes",
            pastSurgeries = "Cardiac Stent Surgery"
        )
        val vitals = listOf(
            VitalLogEntity(vitalType = "Blood Pressure", primaryValue = 165.0, secondaryValue = 102.0, unit = "mmHg"),
            VitalLogEntity(vitalType = "Heart Rate", primaryValue = 116.0, unit = "bpm"),
            VitalLogEntity(vitalType = "SpO2", primaryValue = 90.0, unit = "%"),
            VitalLogEntity(vitalType = "Blood Sugar", primaryValue = 225.0, unit = "mg/dL")
        )
        val symptoms = listOf(
            SymptomLogEntity(symptomTitle = "High fever with chills and chest discomfort", severity = 8)
        )
        val steps = listOf(StepRecordEntity(date = "2026-11-14", steps = 1800))

        val snapshot = ClinicalRiskEngine.computePredictiveRisks(
            profile = profile,
            vitalLogs = vitals,
            symptomLogs = symptoms,
            stepRecords = steps,
            scannedReports = emptyList(),
            language = "English"
        )

        assertTrue("Expected elevated heart risk >= 65, got ${snapshot.heartRiskScore}", snapshot.heartRiskScore >= 65)
        assertTrue("Expected elevated sepsis risk >= 65, got ${snapshot.sepsisRiskScore}", snapshot.sepsisRiskScore >= 65)
        assertTrue("Expected elevated metabolic risk >= 50, got ${snapshot.metabolicRiskScore}", snapshot.metabolicRiskScore >= 50)
        assertTrue(snapshot.immediateActions.isNotEmpty())
    }

    @Test
    fun parseClinicalTranscriptLocally_extractsSoapVitalsAndRx() {
        val transcript = """
            Doctor: Hello, what brings you in today?
            Patient: I have had high fever 101.8 and dry cough for 3 days.
            Doctor: Your BP is 138/88, SpO2 96%, HR 92.
            Please take Paracetamol 650mg after food for fever and Azithromycin 500mg before food once daily.
        """.trimIndent()

        val doc = ClinicalRiskEngine.parseClinicalTranscriptLocally(
            transcript = transcript,
            patientName = "Ramesh Patel",
            language = "English"
        )

        assertTrue(doc.detectedSymptoms.contains("Fever"))
        assertTrue(doc.detectedSymptoms.contains("Cough"))
        assertTrue(doc.detectedVitals.any { it.contains("138/88") })
        assertTrue(doc.detectedVitals.any { it.contains("96%") })
        assertTrue(doc.extractedMedicines.any { it.name.equals("Paracetamol", ignoreCase = true) && it.dosage.contains("650mg", ignoreCase = true) })
        assertTrue(doc.extractedMedicines.any { it.name.equals("Azithromycin", ignoreCase = true) })
        assertTrue(doc.soapNote.contains("Ramesh Patel"))
    }
}

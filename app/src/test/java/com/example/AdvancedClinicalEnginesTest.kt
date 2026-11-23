package com.example

import com.example.data.local.SymptomLogEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.NearbyMedicalPlace
import com.example.utils.AdvancedClinicalEngines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedClinicalEnginesTest {

    @Test
    fun parseAndOrganizeEhr_extractsCategoryVitalsAndMedications() {
        val profile = UserProfileEntity(
            fullName = "Ramesh Patel",
            age = 52,
            gender = "Male",
            bloodGroup = "B+",
            allergies = "Penicillin",
            chronicConditions = "Hypertension",
            currentMedications = "Amlodipine 5mg"
        )
        val rawNotes = "Patient has BP 148/92, dry cough for 3 days, advised Paracetamol 500mg twice daily."
        val res = AdvancedClinicalEngines.parseAndOrganizeEhr(rawNotes, "Ramesh Patel", profile, "English")

        assertTrue(res.category.contains("Cardiology", ignoreCase = true) || res.category.contains("EHR", ignoreCase = true))
        assertTrue(res.structuredSummary.contains("Penicillin"))
        assertTrue(res.structuredSummary.contains("Paracetamol"))
        assertTrue(res.diagnosisTags.isNotBlank())
    }

    @Test
    fun analyzeMentalHealthSignals_detectsCrisisAndStress() {
        val calm = AdvancedClinicalEngines.analyzeMentalHealthSignals(
            transcript = "Feeling peaceful after walking outside.",
            stressSlider = 2,
            sleepHours = 7.5f,
            anxietyDays = 0,
            hasFacialPhoto = false,
            language = "English"
        )
        assertFalse(calm.crisisDetected)
        assertTrue(calm.stressScore < 40)

        val crisis = AdvancedClinicalEngines.analyzeMentalHealthSignals(
            transcript = "I feel hopeless and want to end my life",
            stressSlider = 9,
            sleepHours = 3.5f,
            anxietyDays = 14,
            hasFacialPhoto = true,
            language = "Gujarati"
        )
        assertTrue(crisis.crisisDetected)
        assertTrue(crisis.stressScore >= 90)
        assertTrue(crisis.fallbackGuidance.contains("14416"))
    }

    @Test
    fun evaluateMolecularCandidate_computesLipinskiMetrics() {
        val aspirin = AdvancedClinicalEngines.evaluateMolecularCandidate(
            moleculeInput = "C9H8O4",
            targetDisease = "COX-2 Inflammation",
            priority = "Safety & Oral Bioavailability",
            language = "English"
        )
        assertEquals(0, aspirin.lipinskiViolations)
        assertTrue(aspirin.estimatedMolecularWeight in 175.0..185.0)
        assertTrue(aspirin.drugLikenessScore >= 80)

        val heavyMacroMolecule = AdvancedClinicalEngines.evaluateMolecularCandidate(
            moleculeInput = "C50H80N12O15",
            targetDisease = "Complex Biologic",
            priority = "Target Potency",
            language = "English"
        )
        assertTrue(heavyMacroMolecule.lipinskiViolations >= 2)
        assertTrue(heavyMacroMolecule.drugLikenessScore < aspirin.drugLikenessScore)
    }

    @Test
    fun computeEpidemicOutbreakRisk_flagsMonsoonVectorAndHighTravelRespiratoryRisk() {
        val recentFevers = listOf(
            SymptomLogEntity(symptomTitle = "High fever with chills", severity = 7, bodyLocation = "Whole Body", duration = "2d"),
            SymptomLogEntity(symptomTitle = "Cough and fever", severity = 6, bodyLocation = "Throat", duration = "3d")
        )
        val highRisk = AdvancedClinicalEngines.computeEpidemicOutbreakRisk(
            regionName = "Sayla / Surendranagar",
            tempCelsius = 29,
            humidityPercent = 82,
            travelDensity = "High",
            hospitalLoadPercent = 80,
            newsSignals = "Heavy monsoon rain, rising dengue and flu cases",
            recentSymptoms = recentFevers,
            language = "English"
        )
        assertTrue(highRisk.vectorRiskScore >= 70)
        assertTrue(highRisk.respiratoryRiskScore >= 60)
        assertTrue(highRisk.overallAlertTier.contains("Red", ignoreCase = true))
    }

    @Test
    fun matchSpecialistAndDoctors_mapsSymptomsToCardiologistAndFiltersPharmacies() {
        val places = listOf(
            NearbyMedicalPlace(id = "p1", name = "Shreeji Medical Store", type = "Pharmacy", address = "Main Rd", distanceKm = 0.4, phoneNumber = "104", latitude = 22.3, longitude = 70.8),
            NearbyMedicalPlace(id = "h1", name = "City Heart Care Hospital", type = "Hospital", address = "Station Rd", distanceKm = 1.2, phoneNumber = "108", latitude = 22.31, longitude = 70.81)
        )
        val match = AdvancedClinicalEngines.matchSpecialistAndDoctors(
            symptomQuery = "Chest discomfort and high blood pressure",
            nearbyFacilities = places,
            language = "English"
        )
        assertEquals("Cardiologist", match.recommendedSpecialty)
        assertEquals(1, match.doctorDirectory.size)
        assertEquals("City Heart Care Hospital", match.doctorDirectory.first().clinicName)
    }
}

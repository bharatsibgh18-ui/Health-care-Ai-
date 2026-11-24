package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "triage_sessions")
data class TriageSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val primarySymptom: String,
    val severity: Int = 5,
    val duration: String = "",
    val emergencyFlagged: Boolean = false,
    val recommendedSpecialist: String = "General Physician",
    val keyAdvice: String = "",
    val summary: String = ""
)

@Entity(tableName = "symptom_logs")
data class SymptomLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val symptomTitle: String,
    val severity: Int = 5, // 1 to 10 scale
    val bodyLocation: String = "General",
    val duration: String = "1-2 Days",
    val notesOrTriggers: String = "",
    val status: String = "Ongoing", // "Ongoing", "Improving", "Resolved"
    val dateKey: String = "" // "yyyy-MM-dd" for daily grouping
)

@Entity(
    tableName = "symptom_notes",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = SymptomLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["symptomLogId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["symptomLogId"]),
        androidx.room.Index(value = ["timestamp"])
    ]
)
data class SymptomNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symptomLogId: Long,
    val noteText: String,
    val severitySnapshot: Int = 5,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "developer_logs",
    indices = [
        androidx.room.Index(value = ["timestamp"]),
        androidx.room.Index(value = ["level"])
    ]
)
data class DeveloperLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val level: String, // "DEBUG", "INFO", "WARN", "ERROR"
    val tag: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long = 0,
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEmergencyAlert: Boolean = false,
    val recommendedSpecialist: String? = null,
    val imageUri: String? = null,
    val imageMimeType: String? = null
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val fullName: String = "Health Patient",
    val age: Int = 30,
    val gender: String = "Not Specified",
    val bloodGroup: String = "Not Specified",
    val allergies: String = "None Known",
    val chronicConditions: String = "None",
    val currentMedications: String = "None",
    val pastSurgeries: String = "None",
    val emergencyContactName: String = "Family Contact",
    val emergencyContactPhone: String = "112",
    val preferredLanguage: String = "English",
    val pmjayCardNumber: String = "",
    val abhaCardNumber: String = ""
)

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String = "1 Tablet",
    val timeOfDay: String = "08:00 AM", // e.g. "08:00 AM", "01:00 PM", "08:00 PM"
    val instruction: String = "After Food", // "Before Food", "After Food", "With Water"
    val frequencyHours: Int = 24, // 24 = Once daily, 12 = Twice daily, 8 = Every 8 hours
    val prescribedBy: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "medication_adherence",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["medicineId"]),
        androidx.room.Index(value = ["scheduledDate"])
    ]
)
data class MedicationAdherenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicineId: Long,
    val medicineName: String,
    val dosage: String,
    val scheduledTime: String,
    val scheduledDate: String, // "yyyy-MM-dd"
    val status: String, // "TAKEN", "SKIPPED", "SNOOZED", "MISSED"
    val actionTimestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "scanned_reports")
data class ScannedReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String = "Medical Report",
    val extractedText: String,
    val aiExplanation: String,
    val imageUri: String? = null,
    val reportType: String = "General Report" // "Prescription", "Blood Test", "Discharge Summary", "Radiology"
)

@Entity(tableName = "step_records")
data class StepRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // "yyyy-MM-dd" format
    val steps: Int = 0,
    val targetSteps: Int = 8000,
    val caloriesBurned: Double = 0.0, // in kcal
    val distanceMeters: Double = 0.0, // in meters
    val activeMinutes: Int = 0, // in minutes
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vital_logs")
data class VitalLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val vitalType: String, // "Blood Pressure", "Blood Sugar", "Heart Rate", "SpO2", "Weight", "Sleep", "Stress"
    val primaryValue: Double, // e.g. 120 (Systolic) or 95 (Sugar) or 72 (BPM) or 98 (SpO2) or 7.5 (Hours)
    val secondaryValue: Double = 0.0, // e.g. 80 (Diastolic)
    val unit: String, // "mmHg", "mg/dL", "bpm", "%", "kg", "hrs"
    val status: String = "Normal", // "Normal", "Elevated", "High", "Critical"
    val notes: String = "",
    val familyMemberName: String = "Self"
)

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val relationship: String, // "Father", "Mother", "Spouse", "Child", "Elder", "Other"
    val age: Int,
    val bloodGroup: String = "O+",
    val phone: String = "",
    val caregiverAlertEnabled: Boolean = true,
    val chronicConditions: String = "None",
    val avatarColor: Long = 0xFF26A69A
)

@Entity(tableName = "treatment_plans")
data class TreatmentPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val targetGoal: String,
    val geneticMarkers: String,
    val lifestyleSummary: String,
    val labResultsSummary: String,
    val planContent: String,
    val isAiPowered: Boolean = false
)

@Entity(tableName = "risk_assessments")
data class RiskAssessmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val heartRiskScore: Int,
    val sepsisRiskScore: Int,
    val metabolicRiskScore: Int,
    val overallRiskLevel: String,
    val contributingFactors: String,
    val aiForecast: String
)

@Entity(tableName = "clinical_notes")
data class ClinicalNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val patientName: String,
    val conversationTranscript: String,
    val soapNote: String,
    val medicalReport: String,
    val prescriptionSummary: String
)

@Entity(tableName = "ehr_records")
data class EhrRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val patientName: String,
    val category: String,
    val rawNotes: String,
    val structuredSummary: String,
    val diagnosisTags: String,
    val medicationsDetected: String
)

@Entity(tableName = "mental_health_checkins")
data class MentalHealthCheckInEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val stressScore: Int,
    val moodLabel: String,
    val spokenTranscript: String,
    val hasFacialPhoto: Boolean = false,
    val counselingGuidance: String
)

@Entity(tableName = "drug_studies")
data class DrugCandidateStudyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val targetDisease: String,
    val moleculeInput: String,
    val drugLikenessScore: Int,
    val analysisReport: String
)

@Entity(tableName = "outbreak_alerts")
data class OutbreakAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val regionName: String,
    val overallAlertTier: String,
    val vectorRiskScore: Int,
    val respiratoryRiskScore: Int,
    val alertBrief: String
)

@Entity(tableName = "doctor_appointments")
data class DoctorAppointmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val bookingRef: String,
    val doctorName: String,
    val specialty: String,
    val clinicName: String,
    val distanceKm: Double,
    val appointmentDate: String,
    val timeSlot: String,
    val consultationMode: String,
    val symptomNote: String,
    val status: String = "Confirmed"
)

@Entity(
    tableName = "user_accounts",
    indices = [
        androidx.room.Index(value = ["username"], unique = true),
        androidx.room.Index(value = ["phoneNumber"])
    ]
)
data class UserAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val displayName: String,
    val phoneNumber: String = "",
    val passwordSalt: String = "",
    val passwordHash: String = "",
    val role: String = "PATIENT", // "PATIENT" or "DEVELOPER"
    val authMethod: String = "PASSWORD", // "PASSWORD" or "MOBILE_OTP"
    val createdAt: Long = System.currentTimeMillis(),
    val totalOnlineSeconds: Long = 0L,
    val lastOnlineAt: Long = System.currentTimeMillis(),
    val isCurrentlyOnline: Boolean = false
)

/**
 * Privacy-preserving projection exposed to the Developer Console:
 * strictly username and online activity timing only (no passwords, salts, phone numbers, or clinical records).
 */
data class DeveloperUserPresenceSummary(
    val username: String,
    val authMethod: String,
    val totalOnlineSeconds: Long,
    val lastOnlineAt: Long,
    val isCurrentlyOnline: Boolean
)

@Entity(
    tableName = "public_doctors",
    indices = [
        androidx.room.Index(value = ["cloudId"], unique = true)
    ]
)
data class PublicDoctorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cloudId: String = java.util.UUID.randomUUID().toString(),
    val doctorName: String,
    val specialty: String,
    val clinicOrCity: String,
    val mobileNumber: String,
    val consultationFee: String = "₹300",
    val videoCallEnabled: Boolean = false,
    val videoCallRoomUrl: String = "",
    val isPublicActive: Boolean = true,
    val addedByDeveloper: String = "SHAILESH",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "developer_app_config")
data class DeveloperAppConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val appBannerNotice: String = "",
    val publicDoctorConsultEnabled: Boolean = true,
    val videoCallFeatureEnabled: Boolean = true,
    val medicineAdvisorFeatureEnabled: Boolean = true,
    val emergencyHelplineOverride: String = "112",
    val configVersion: Int = 1,
    val updatedBy: String = "SHAILESH",
    val updatedAt: Long = System.currentTimeMillis(),
    // Public App Upgrade Publisher (shown to all users with Download Upgrade option)
    val upgradePublished: Boolean = false,
    val upgradeTargetVersion: String = "",
    val upgradeTitle: String = "",
    val upgradeMessage: String = "",
    val upgradeDownloadUrl: String = "",
    val upgradeMandatory: Boolean = false,
    // Expanded Live Developer Controls
    val ultraHubFeatureEnabled: Boolean = true,
    val pmjayFinderEnabled: Boolean = true,
    val pmjayPortalUrlOverride: String = "https://hem.nha.gov.in/search",
    val reportScannerFeatureEnabled: Boolean = true,
    val splashDurationSeconds: Int = 5,
    val splashTopCreditText: String = "Created by Shailesh and Ruturaj",
    val splashSchoolNameText: String = "Model School Sayla",
    val maintenanceModeEnabled: Boolean = false,
    val maintenanceMessage: String = "",
    val supportContactPhone: String = ""
)

@Entity(
    tableName = "sleep_wellness_logs",
    indices = [
        androidx.room.Index(value = ["dateKey"]),
        androidx.room.Index(value = ["timestamp"])
    ]
)
data class SleepWellnessLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateKey: String, // "yyyy-MM-dd"
    val sleepHours: Double, // e.g. 7.5
    val sleepQuality: Int = 4, // 1 (Poor) to 5 (Restorative)
    val bedTime: String = "10:30 PM",
    val wakeTime: String = "06:00 AM",
    val weightKg: Double = 0.0,
    val heightCm: Double = 0.0,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "meal_nutrition_logs",
    indices = [
        androidx.room.Index(value = ["dateKey"]),
        androidx.room.Index(value = ["timestamp"])
    ]
)
data class MealNutritionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateKey: String, // "yyyy-MM-dd"
    val mealType: String = "Lunch", // Breakfast, Lunch, Dinner, Snack
    val foodName: String,
    val calories: Int,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fiberGrams: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)




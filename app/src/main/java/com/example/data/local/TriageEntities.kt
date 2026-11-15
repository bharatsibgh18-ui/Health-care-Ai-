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
    val status: String = "Ongoing" // "Ongoing", "Improving", "Resolved"
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
    val preferredLanguage: String = "English"
)

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String = "1 Tablet",
    val timeOfDay: String = "08:00 AM", // e.g. "08:00 AM", "01:00 PM", "08:00 PM"
    val instruction: String = "After Food", // "Before Food", "After Food", "With Water"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
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


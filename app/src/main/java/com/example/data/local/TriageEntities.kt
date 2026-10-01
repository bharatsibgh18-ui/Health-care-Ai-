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
    val recommendedSpecialist: String? = null
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

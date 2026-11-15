package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.local.SymptomLogEntity
import com.example.data.local.UserProfileEntity
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirebaseBackupRepository(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy {
        try {
            val databaseId = context.getString(R.string.firestore_database_id)
            if (databaseId.isNotBlank() && databaseId != "(default)") {
                Firebase.firestore(databaseId)
            } else {
                Firebase.firestore
            }
        } catch (_: Exception) {
            Firebase.firestore
        }
    }

    /**
     * Backs up User Profile and all Room Symptom Logs to Firebase Firestore.
     */
    suspend fun backupHealthDataToFirestore(
        profile: UserProfileEntity?,
        symptomLogs: List<SymptomLogEntity>,
        stepRecords: List<com.example.data.local.StepRecordEntity> = emptyList()
    ): Result<Int> {
        return try {
            val deviceId = profile?.fullName?.replace(" ", "_")?.lowercase()?.ifBlank { "patient" } ?: "patient"
            val patientDocRef = firestore.collection("health_backups").document(deviceId)

            // 1. Sync UserProfileEntity
            if (profile != null) {
                val profileMap = hashMapOf(
                    "fullName" to profile.fullName,
                    "age" to profile.age,
                    "gender" to profile.gender,
                    "bloodGroup" to profile.bloodGroup,
                    "allergies" to profile.allergies,
                    "chronicConditions" to profile.chronicConditions,
                    "currentMedications" to profile.currentMedications,
                    "pastSurgeries" to profile.pastSurgeries,
                    "emergencyContactName" to profile.emergencyContactName,
                    "emergencyContactPhone" to profile.emergencyContactPhone,
                    "preferredLanguage" to profile.preferredLanguage,
                    "lastBackupTimestamp" to System.currentTimeMillis()
                )
                patientDocRef.set(profileMap).await()
            }

            // 2. Sync Symptom Logs in subcollection
            val symptomsCollection = patientDocRef.collection("symptom_logs")
            var count = 0
            for (log in symptomLogs) {
                val logMap = hashMapOf(
                    "id" to log.id,
                    "timestamp" to log.timestamp,
                    "symptomTitle" to log.symptomTitle,
                    "severity" to log.severity,
                    "bodyLocation" to log.bodyLocation,
                    "duration" to log.duration,
                    "notesOrTriggers" to log.notesOrTriggers,
                    "status" to log.status
                )
                symptomsCollection.document(log.id.toString()).set(logMap).await()
                count++
            }

            // 3. Sync Step Records in subcollection
            val stepsCollection = patientDocRef.collection("step_records")
            for (step in stepRecords) {
                val stepMap = hashMapOf(
                    "date" to step.date,
                    "steps" to step.steps,
                    "targetSteps" to step.targetSteps,
                    "caloriesBurned" to step.caloriesBurned,
                    "distanceMeters" to step.distanceMeters,
                    "activeMinutes" to step.activeMinutes,
                    "timestamp" to step.timestamp
                )
                stepsCollection.document(step.date).set(stepMap).await()
                count++
            }

            Result.success(count)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

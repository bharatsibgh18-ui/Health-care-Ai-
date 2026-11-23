package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.local.DeveloperAppConfigEntity
import com.example.data.local.DeveloperUserPresenceSummary
import com.example.data.local.PublicDoctorEntity
import com.example.data.local.SymptomLogEntity
import com.example.data.local.UserProfileEntity
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirebaseBackupRepository(private val context: Context) {

    private fun getFirestoreOrNull(): FirebaseFirestore? {
        return try {
            val databaseId = try {
                context.getString(R.string.firestore_database_id)
            } catch (_: Throwable) {
                "(default)"
            }
            if (databaseId.isNotBlank() && databaseId != "(default)") {
                Firebase.firestore(databaseId)
            } else {
                Firebase.firestore
            }
        } catch (_: Throwable) {
            try {
                Firebase.firestore
            } catch (_: Throwable) {
                null
            }
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
            val firestore = getFirestoreOrNull()
                ?: return Result.failure(IllegalStateException("Cloud backup unavailable"))
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
                    "pmjayCardNumber" to profile.pmjayCardNumber,
                    "abhaCardNumber" to profile.abhaCardNumber,
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
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun syncPublicDoctor(doctor: PublicDoctorEntity): Result<Unit> {
        return try {
            val firestore = getFirestoreOrNull()
                ?: return Result.failure(IllegalStateException("Cloud sync unavailable"))
            val map = hashMapOf(
                "cloudId" to doctor.cloudId,
                "doctorName" to doctor.doctorName,
                "specialty" to doctor.specialty,
                "clinicOrCity" to doctor.clinicOrCity,
                "mobileNumber" to doctor.mobileNumber,
                "consultationFee" to doctor.consultationFee,
                "videoCallEnabled" to doctor.videoCallEnabled,
                "videoCallRoomUrl" to doctor.videoCallRoomUrl,
                "isPublicActive" to doctor.isPublicActive,
                "addedByDeveloper" to doctor.addedByDeveloper,
                "updatedAt" to doctor.updatedAt
            )
            firestore.collection("public_doctors").document(doctor.cloudId).set(map).await()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun deletePublicDoctorFromCloud(cloudId: String): Result<Unit> {
        return try {
            val firestore = getFirestoreOrNull()
                ?: return Result.failure(IllegalStateException("Cloud sync unavailable"))
            firestore.collection("public_doctors").document(cloudId).delete().await()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun fetchPublicDoctorsFromCloud(): Result<List<PublicDoctorEntity>> {
        return try {
            val firestore = getFirestoreOrNull()
                ?: return Result.failure(IllegalStateException("Cloud sync unavailable"))
            val snapshot = firestore.collection("public_doctors").get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("doctorName") ?: return@mapNotNull null
                PublicDoctorEntity(
                    cloudId = doc.getString("cloudId") ?: doc.id,
                    doctorName = name,
                    specialty = doc.getString("specialty") ?: "General Physician",
                    clinicOrCity = doc.getString("clinicOrCity") ?: "Public Clinic",
                    mobileNumber = doc.getString("mobileNumber") ?: "",
                    consultationFee = doc.getString("consultationFee") ?: "₹300",
                    videoCallEnabled = false,
                    videoCallRoomUrl = "",
                    isPublicActive = doc.getBoolean("isPublicActive") ?: true,
                    addedByDeveloper = doc.getString("addedByDeveloper") ?: "SHAILESH",
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }
            Result.success(list)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun syncDevAppConfig(config: DeveloperAppConfigEntity): Result<Unit> {
        return try {
            val firestore = getFirestoreOrNull()
                ?: return Result.failure(IllegalStateException("Cloud sync unavailable"))
            val map = hashMapOf(
                "appBannerNotice" to config.appBannerNotice,
                "publicDoctorConsultEnabled" to config.publicDoctorConsultEnabled,
                "videoCallFeatureEnabled" to config.videoCallFeatureEnabled,
                "medicineAdvisorFeatureEnabled" to config.medicineAdvisorFeatureEnabled,
                "emergencyHelplineOverride" to config.emergencyHelplineOverride,
                "configVersion" to config.configVersion,
                "updatedBy" to config.updatedBy,
                "updatedAt" to config.updatedAt
            )
            firestore.collection("app_config").document("global").set(map).await()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun fetchDevAppConfig(): Result<DeveloperAppConfigEntity?> {
        return try {
            val firestore = getFirestoreOrNull()
                ?: return Result.failure(IllegalStateException("Cloud sync unavailable"))
            val doc = firestore.collection("app_config").document("global").get().await()
            if (!doc.exists()) return Result.success(null)
            Result.success(
                DeveloperAppConfigEntity(
                    id = 1,
                    appBannerNotice = doc.getString("appBannerNotice") ?: "",
                    publicDoctorConsultEnabled = doc.getBoolean("publicDoctorConsultEnabled") ?: true,
                    videoCallFeatureEnabled = doc.getBoolean("videoCallFeatureEnabled") ?: true,
                    medicineAdvisorFeatureEnabled = doc.getBoolean("medicineAdvisorFeatureEnabled") ?: true,
                    emergencyHelplineOverride = doc.getString("emergencyHelplineOverride") ?: "112",
                    configVersion = (doc.getLong("configVersion") ?: 1L).toInt(),
                    updatedBy = doc.getString("updatedBy") ?: "SHAILESH",
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Syncs ONLY username and online duration/timestamp — never syncs passwords, salts, phone, or medical logs.
     */
    suspend fun syncUserPresence(summary: DeveloperUserPresenceSummary): Result<Unit> {
        return try {
            val firestore = getFirestoreOrNull()
                ?: return Result.failure(IllegalStateException("Cloud sync unavailable"))
            val cleanDocId = summary.username.trim().replace("/", "_")
            if (cleanDocId.isBlank()) return Result.success(Unit)
            val map = hashMapOf(
                "username" to summary.username,
                "authMethod" to summary.authMethod,
                "totalOnlineSeconds" to summary.totalOnlineSeconds,
                "lastOnlineAt" to summary.lastOnlineAt,
                "isCurrentlyOnline" to summary.isCurrentlyOnline
            )
            firestore.collection("user_presence").document(cleanDocId).set(map).await()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun fetchAllUserPresenceFromCloud(): Result<List<DeveloperUserPresenceSummary>> {
        return try {
            val firestore = getFirestoreOrNull()
                ?: return Result.failure(IllegalStateException("Cloud sync unavailable"))
            val snap = firestore.collection("user_presence").get().await()
            val list = snap.documents.mapNotNull { doc ->
                val username = doc.getString("username") ?: return@mapNotNull null
                DeveloperUserPresenceSummary(
                    username = username,
                    authMethod = doc.getString("authMethod") ?: "PASSWORD",
                    totalOnlineSeconds = doc.getLong("totalOnlineSeconds") ?: 0L,
                    lastOnlineAt = doc.getLong("lastOnlineAt") ?: 0L,
                    isCurrentlyOnline = doc.getBoolean("isCurrentlyOnline") ?: false
                )
            }
            Result.success(list)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}

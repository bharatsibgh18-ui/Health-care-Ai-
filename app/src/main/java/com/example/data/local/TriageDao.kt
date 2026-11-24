package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TriageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TriageSessionEntity): Long

    @Update
    suspend fun updateSession(session: TriageSessionEntity)

    @Query("SELECT * FROM triage_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<TriageSessionEntity>>

    @Query("SELECT * FROM triage_sessions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 5): List<TriageSessionEntity>

    @Query("SELECT * FROM triage_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): TriageSessionEntity?

    @Delete
    suspend fun deleteSession(session: TriageSessionEntity)

    @Query("DELETE FROM triage_sessions")
    suspend fun clearAllSessions()

    // Symptom Log queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptomLog(log: SymptomLogEntity): Long

    @Update
    suspend fun updateSymptomLog(log: SymptomLogEntity)

    @Delete
    suspend fun deleteSymptomLog(log: SymptomLogEntity)

    @Query("SELECT * FROM symptom_logs ORDER BY timestamp DESC")
    fun getAllSymptomLogs(): Flow<List<SymptomLogEntity>>

    @Query("SELECT * FROM symptom_logs WHERE LOWER(symptomTitle) LIKE '%headache%' OR LOWER(symptomTitle) LIKE '%sir dard%' OR LOWER(symptomTitle) LIKE '%sar dard%' OR LOWER(bodyLocation) = 'head' ORDER BY timestamp DESC")
    fun getHeadacheLogs(): Flow<List<SymptomLogEntity>>

    @Query("SELECT * FROM symptom_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSymptomLogs(limit: Int = 10): List<SymptomLogEntity>

    @Query("SELECT * FROM symptom_logs WHERE dateKey = :dateKey ORDER BY timestamp DESC")
    fun getSymptomLogsByDate(dateKey: String): Flow<List<SymptomLogEntity>>

    @Query("DELETE FROM symptom_logs")
    suspend fun clearAllSymptomLogs()

    // Timestamped Symptom Follow-Up Notes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptomNote(note: SymptomNoteEntity): Long

    @Delete
    suspend fun deleteSymptomNote(note: SymptomNoteEntity)

    @Query("SELECT * FROM symptom_notes ORDER BY timestamp DESC")
    fun getAllSymptomNotes(): Flow<List<SymptomNoteEntity>>

    @Query("SELECT * FROM symptom_notes WHERE symptomLogId = :symptomLogId ORDER BY timestamp DESC")
    fun getNotesForSymptom(symptomLogId: Long): Flow<List<SymptomNoteEntity>>

    // Developer Diagnostic Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeveloperLog(log: DeveloperLogEntity): Long

    @Query("SELECT * FROM developer_logs ORDER BY timestamp DESC")
    fun getAllDeveloperLogs(): Flow<List<DeveloperLogEntity>>

    @Query("SELECT * FROM developer_logs WHERE level = :level ORDER BY timestamp DESC")
    fun getDeveloperLogsByLevel(level: String): Flow<List<DeveloperLogEntity>>

    @Query("DELETE FROM developer_logs")
    suspend fun clearAllDeveloperLogs()

    @Query("DELETE FROM developer_logs WHERE id NOT IN (SELECT id FROM developer_logs ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun trimOldDeveloperLogs(keepCount: Int = 250)

    // Chat messages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    // User profile
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    // Water Log queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLogEntity): Long

    @Query("SELECT * FROM water_logs WHERE dateString = :date ORDER BY timestamp DESC")
    fun getWaterLogsForDate(date: String): Flow<List<WaterLogEntity>>

    @Query("SELECT SUM(amountMl) FROM water_logs WHERE dateString = :date")
    fun getTotalWaterForDate(date: String): Flow<Int?>

    @Delete
    suspend fun deleteWaterLog(log: WaterLogEntity)

    @Query("DELETE FROM water_logs WHERE dateString = :date")
    suspend fun clearWaterLogsForDate(date: String)

    // Medicine Reminders
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)

    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE isActive = 1")
    suspend fun getActiveMedicinesOnce(): List<MedicineEntity>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getMedicineById(id: Long): MedicineEntity?

    // Medication Adherence History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdherenceLog(log: MedicationAdherenceEntity): Long

    @Update
    suspend fun updateAdherenceLog(log: MedicationAdherenceEntity)

    @Delete
    suspend fun deleteAdherenceLog(log: MedicationAdherenceEntity)

    @Query("SELECT * FROM medication_adherence ORDER BY actionTimestamp DESC")
    fun getAllAdherenceLogs(): Flow<List<MedicationAdherenceEntity>>

    @Query("SELECT * FROM medication_adherence WHERE scheduledDate = :date ORDER BY actionTimestamp DESC")
    fun getAdherenceLogsForDate(date: String): Flow<List<MedicationAdherenceEntity>>

    @Query("SELECT * FROM medication_adherence WHERE medicineId = :medicineId ORDER BY actionTimestamp DESC")
    fun getAdherenceLogsForMedicine(medicineId: Long): Flow<List<MedicationAdherenceEntity>>

    @Query("SELECT * FROM medication_adherence WHERE medicineId = :medicineId AND scheduledDate = :date LIMIT 1")
    suspend fun getAdherenceLogForMedicineAndDate(medicineId: Long, date: String): MedicationAdherenceEntity?

    @Query("DELETE FROM medication_adherence")
    suspend fun clearAllAdherenceLogs()

    // Scanned Medical Reports & Prescriptions (OCR)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannedReport(report: ScannedReportEntity): Long

    @Query("SELECT * FROM scanned_reports ORDER BY timestamp DESC")
    fun getAllScannedReports(): Flow<List<ScannedReportEntity>>

    @Query("SELECT * FROM scanned_reports WHERE id = :id LIMIT 1")
    suspend fun getScannedReportById(id: Long): ScannedReportEntity?

    @Delete
    suspend fun deleteScannedReport(report: ScannedReportEntity)

    @Query("DELETE FROM scanned_reports")
    suspend fun clearAllScannedReports()

    // Step Counter & Fitness Tracking
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStepRecord(record: StepRecordEntity): Long

    @Query("SELECT * FROM step_records WHERE date = :date LIMIT 1")
    fun getStepRecordForDate(date: String): Flow<StepRecordEntity?>

    @Query("SELECT * FROM step_records WHERE date = :date LIMIT 1")
    suspend fun getStepRecordDirect(date: String): StepRecordEntity?

    @Query("SELECT * FROM step_records ORDER BY date DESC LIMIT :limit")
    fun getRecentStepRecords(limit: Int = 7): Flow<List<StepRecordEntity>>

    @Query("SELECT * FROM step_records ORDER BY date DESC")
    fun getAllStepRecords(): Flow<List<StepRecordEntity>>

    @Query("DELETE FROM step_records")
    suspend fun clearAllStepRecords()

    // Smart Health Vitals Tracker
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVitalLog(log: VitalLogEntity): Long

    @Delete
    suspend fun deleteVitalLog(log: VitalLogEntity)

    @Query("SELECT * FROM vital_logs ORDER BY timestamp DESC")
    fun getAllVitalLogs(): Flow<List<VitalLogEntity>>

    @Query("SELECT * FROM vital_logs WHERE vitalType = :type ORDER BY timestamp DESC")
    fun getVitalLogsByType(type: String): Flow<List<VitalLogEntity>>

    // Family Profile & Caregiver Support
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMemberEntity): Long

    @Update
    suspend fun updateFamilyMember(member: FamilyMemberEntity)

    @Delete
    suspend fun deleteFamilyMember(member: FamilyMemberEntity)

    @Query("SELECT * FROM family_members ORDER BY id ASC")
    fun getAllFamilyMembers(): Flow<List<FamilyMemberEntity>>

    // Personalized Treatment Plans
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreatmentPlan(plan: TreatmentPlanEntity): Long

    @Delete
    suspend fun deleteTreatmentPlan(plan: TreatmentPlanEntity)

    @Query("SELECT * FROM treatment_plans ORDER BY timestamp DESC")
    fun getAllTreatmentPlans(): Flow<List<TreatmentPlanEntity>>

    // Predictive Risk Assessments
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiskAssessment(assessment: RiskAssessmentEntity): Long

    @Delete
    suspend fun deleteRiskAssessment(assessment: RiskAssessmentEntity)

    @Query("SELECT * FROM risk_assessments ORDER BY timestamp DESC")
    fun getAllRiskAssessments(): Flow<List<RiskAssessmentEntity>>

    // Automated Clinical Documentation Notes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinicalNote(note: ClinicalNoteEntity): Long

    @Delete
    suspend fun deleteClinicalNote(note: ClinicalNoteEntity)

    @Query("SELECT * FROM clinical_notes ORDER BY timestamp DESC")
    fun getAllClinicalNotes(): Flow<List<ClinicalNoteEntity>>

    // EHR Automation
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEhrRecord(record: EhrRecordEntity): Long

    @Delete
    suspend fun deleteEhrRecord(record: EhrRecordEntity)

    @Query("SELECT * FROM ehr_records ORDER BY timestamp DESC")
    fun getAllEhrRecords(): Flow<List<EhrRecordEntity>>

    // Mental Health Support
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMentalHealthCheckIn(checkIn: MentalHealthCheckInEntity): Long

    @Delete
    suspend fun deleteMentalHealthCheckIn(checkIn: MentalHealthCheckInEntity)

    @Query("SELECT * FROM mental_health_checkins ORDER BY timestamp DESC")
    fun getAllMentalHealthCheckIns(): Flow<List<MentalHealthCheckInEntity>>

    // Drug Discovery Studies
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrugStudy(study: DrugCandidateStudyEntity): Long

    @Delete
    suspend fun deleteDrugStudy(study: DrugCandidateStudyEntity)

    @Query("SELECT * FROM drug_studies ORDER BY timestamp DESC")
    fun getAllDrugStudies(): Flow<List<DrugCandidateStudyEntity>>

    // Epidemic Outbreak Alerts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutbreakAlert(alert: OutbreakAlertEntity): Long

    @Delete
    suspend fun deleteOutbreakAlert(alert: OutbreakAlertEntity)

    @Query("SELECT * FROM outbreak_alerts ORDER BY timestamp DESC")
    fun getAllOutbreakAlerts(): Flow<List<OutbreakAlertEntity>>

    // Smart Doctor Finder & Appointments
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorAppointment(appointment: DoctorAppointmentEntity): Long

    @Delete
    suspend fun deleteDoctorAppointment(appointment: DoctorAppointmentEntity)

    @Query("SELECT * FROM doctor_appointments ORDER BY timestamp DESC")
    fun getAllDoctorAppointments(): Flow<List<DoctorAppointmentEntity>>

    // User Accounts (Username/Password + Mobile OTP)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUserAccount(account: UserAccountEntity): Long

    @Query("SELECT * FROM user_accounts WHERE username = :username LIMIT 1")
    suspend fun getUserAccountByUsername(username: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserAccountByUsernameCaseInsensitive(username: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getUserAccountByPhone(phoneNumber: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts ORDER BY createdAt DESC")
    suspend fun getAllUserAccounts(): List<UserAccountEntity>

    @Query("UPDATE user_accounts SET totalOnlineSeconds = totalOnlineSeconds + :deltaSeconds, lastOnlineAt = :timestamp, isCurrentlyOnline = :isOnline WHERE username = :username")
    suspend fun recordUserOnlineHeartbeat(username: String, deltaSeconds: Long, timestamp: Long, isOnline: Boolean)

    @Query("UPDATE user_accounts SET isCurrentlyOnline = 0, lastOnlineAt = :timestamp WHERE username = :username")
    suspend fun markUserOffline(username: String, timestamp: Long)

    @Query("SELECT username, authMethod, totalOnlineSeconds, lastOnlineAt, isCurrentlyOnline FROM user_accounts ORDER BY isCurrentlyOnline DESC, lastOnlineAt DESC")
    fun getDeveloperUserPresenceFlow(): Flow<List<DeveloperUserPresenceSummary>>

    // Developer-Managed Public Doctors
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePublicDoctor(doctor: PublicDoctorEntity): Long

    @Delete
    suspend fun deletePublicDoctor(doctor: PublicDoctorEntity)

    @Query("SELECT * FROM public_doctors ORDER BY updatedAt DESC")
    fun getAllPublicDoctors(): Flow<List<PublicDoctorEntity>>

    @Query("SELECT * FROM public_doctors WHERE isPublicActive = 1 ORDER BY updatedAt DESC")
    fun getActivePublicDoctors(): Flow<List<PublicDoctorEntity>>

    @Query("SELECT * FROM public_doctors WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getPublicDoctorByCloudId(cloudId: String): PublicDoctorEntity?

    // Developer Live App Configuration & Feature Upgrades
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevAppConfig(config: DeveloperAppConfigEntity)

    @Query("SELECT * FROM developer_app_config WHERE id = 1 LIMIT 1")
    fun getDevAppConfigFlow(): Flow<DeveloperAppConfigEntity?>

    @Query("SELECT * FROM developer_app_config WHERE id = 1 LIMIT 1")
    suspend fun getDevAppConfigOnce(): DeveloperAppConfigEntity?

    // Ultra Sleep & Wellness Logs (Room v16)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepWellnessLog(log: SleepWellnessLogEntity): Long

    @Delete
    suspend fun deleteSleepWellnessLog(log: SleepWellnessLogEntity)

    @Query("SELECT * FROM sleep_wellness_logs ORDER BY timestamp DESC")
    fun getAllSleepWellnessLogs(): Flow<List<SleepWellnessLogEntity>>

    @Query("SELECT * FROM sleep_wellness_logs WHERE dateKey = :dateKey ORDER BY timestamp DESC LIMIT 1")
    suspend fun getSleepLogForDate(dateKey: String): SleepWellnessLogEntity?

    // Ultra Meal & Calorie Nutrition Logs (Room v18)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealNutritionLog(log: MealNutritionLogEntity): Long

    @Delete
    suspend fun deleteMealNutritionLog(log: MealNutritionLogEntity)

    @Query("SELECT * FROM meal_nutrition_logs ORDER BY timestamp DESC")
    fun getAllMealNutritionLogs(): Flow<List<MealNutritionLogEntity>>

    @Query("SELECT * FROM meal_nutrition_logs WHERE dateKey = :dateKey ORDER BY timestamp DESC")
    fun getMealNutritionLogsForDate(dateKey: String): Flow<List<MealNutritionLogEntity>>
}

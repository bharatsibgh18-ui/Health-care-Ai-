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

    @Query("DELETE FROM symptom_logs")
    suspend fun clearAllSymptomLogs()

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
}

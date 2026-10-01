package com.example.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.SymptomLogEntity
import com.example.data.local.TriageSessionEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WaterLogEntity
import com.example.data.repository.TriageRepository
import com.example.utils.WaterReminderNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    TRIAGE_CHAT,
    WATER_REMINDER,
    EMERGENCY_GUIDE,
    HISTORY,
    PROFILE
}

data class SymptomDetails(
    val severity: Int = 5,
    val duration: String = "1-2 Days",
    val location: String = "General",
    val associatedSymptoms: String = ""
)

class MediAssistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TriageRepository
    private var textToSpeech: TextToSpeech? = null
    private val _isTtsReady = MutableStateFlow(false)
    private val _speakingMessageId = MutableStateFlow<Long?>(null)
    val speakingMessageId: StateFlow<Long?> = _speakingMessageId.asStateFlow()

    private val _autoSpeakEnabled = MutableStateFlow(true)
    val autoSpeakEnabled: StateFlow<Boolean> = _autoSpeakEnabled.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TriageRepository(database.triageDao())

        textToSpeech = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.setSpeechRate(0.95f)
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _speakingMessageId.value = null
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _speakingMessageId.value = null
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        _speakingMessageId.value = null
                    }
                })
                _isTtsReady.value = true
            }
        }
    }

    val sessions: StateFlow<List<TriageSessionEntity>> = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val symptomLogs: StateFlow<List<SymptomLogEntity>> = repository.allSymptomLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val headacheLogs: StateFlow<List<SymptomLogEntity>> = repository.headacheLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _currentScreen = MutableStateFlow(AppScreen.TRIAGE_CHAT)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _emergencyAlertTriggered = MutableStateFlow(false)
    val emergencyAlertTriggered: StateFlow<Boolean> = _emergencyAlertTriggered.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _showSymptomContextSheet = MutableStateFlow(false)
    val showSymptomContextSheet: StateFlow<Boolean> = _showSymptomContextSheet.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun updateInputText(newText: String) {
        _inputText.value = newText
    }

    fun setSelectedLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    fun toggleSymptomContextSheet(show: Boolean) {
        _showSymptomContextSheet.value = show
    }

    fun dismissEmergencyAlert() {
        _emergencyAlertTriggered.value = false
    }

    fun triggerEmergencyDirect() {
        _emergencyAlertTriggered.value = true
    }

    fun sendMessage(
        overrideText: String? = null,
        symptomDetails: SymptomDetails? = null
    ) {
        val messageText = overrideText ?: _inputText.value.trim()
        if (messageText.isBlank()) return

        val formattedMessage = if (symptomDetails != null) {
            """
                $messageText
                [Clinical Context Attached: Severity: ${symptomDetails.severity}/10, Duration: ${symptomDetails.duration}, Location: ${symptomDetails.location}, Associated: ${if (symptomDetails.associatedSymptoms.isNotBlank()) symptomDetails.associatedSymptoms else "None"}]
            """.trimIndent()
        } else {
            messageText
        }

        val isEmergency = repository.isEmergencyText(messageText)
        if (isEmergency) {
            _emergencyAlertTriggered.value = true
        }

        val userMessageEntity = ChatMessageEntity(
            sender = "USER",
            text = formattedMessage,
            isEmergencyAlert = isEmergency
        )

        _inputText.value = ""
        _isLoading.value = true

        viewModelScope.launch {
            repository.saveMessage(userMessageEntity)

            val currentHistory = messages.value
            val currentProf = userProfile.value

            val result = repository.sendTriageQuery(
                userMessage = formattedMessage,
                recentHistory = currentHistory,
                userProfile = currentProf
            )

            result.onSuccess { responseText ->
                val responseIsEmergency = isEmergency || repository.isEmergencyResponse(responseText)
                if (responseIsEmergency) {
                    _emergencyAlertTriggered.value = true
                }

                val aiMessageEntity = ChatMessageEntity(
                    sender = "AI",
                    text = responseText,
                    isEmergencyAlert = responseIsEmergency
                )
                val savedMessageId = repository.saveMessage(aiMessageEntity)

                if (_autoSpeakEnabled.value) {
                    explainRemedyAloud(aiMessageEntity.copy(id = savedMessageId))
                }

                // Save or update as a triage session entry in Room
                val specialist = extractSpecialist(responseText)
                val newSession = TriageSessionEntity(
                    primarySymptom = messageText.take(100),
                    severity = symptomDetails?.severity ?: 5,
                    duration = symptomDetails?.duration ?: "Recent",
                    emergencyFlagged = responseIsEmergency,
                    recommendedSpecialist = specialist,
                    summary = responseText.take(200) + "..."
                )
                repository.saveSession(newSession)

                // Also persist symptom into Room database for long-term health tracking
                val symptomLog = SymptomLogEntity(
                    symptomTitle = messageText.take(80),
                    severity = symptomDetails?.severity ?: 5,
                    bodyLocation = symptomDetails?.location ?: "General",
                    duration = symptomDetails?.duration ?: "Recent",
                    notesOrTriggers = symptomDetails?.associatedSymptoms ?: "",
                    status = "Ongoing"
                )
                repository.saveSymptomLog(symptomLog)
            }.onFailure { err ->
                val fallbackMessage = ChatMessageEntity(
                    sender = "AI",
                    text = "⚠️ An error occurred while contacting triage: ${err.message}. Please consult a local healthcare professional or call emergency services if symptoms are severe.",
                    isEmergencyAlert = false
                )
                repository.saveMessage(fallbackMessage)
            }

            _isLoading.value = false
        }
    }

    private fun extractSpecialist(text: String): String {
        val specialistSection = text.substringAfter("Recommended Healthcare Specialist", "")
            .substringAfter("अनुशंसित", "")
            .ifBlank { text }

        return when {
            specialistSection.contains("Cardiologist", ignoreCase = true) || specialistSection.contains("कार्डियोलॉजिस्ट", ignoreCase = true) -> "Cardiologist"
            specialistSection.contains("Neurologist", ignoreCase = true) || specialistSection.contains("न्यूरोलॉजिस्ट", ignoreCase = true) -> "Neurologist"
            specialistSection.contains("Orthopedic", ignoreCase = true) || specialistSection.contains("हड्डी", ignoreCase = true) -> "Orthopedic Specialist"
            specialistSection.contains("ENT", ignoreCase = true) || specialistSection.contains("कान, नाक", ignoreCase = true) -> "ENT Specialist"
            specialistSection.contains("Dermatologist", ignoreCase = true) || specialistSection.contains("त्वचा", ignoreCase = true) -> "Dermatologist"
            specialistSection.contains("Gastroenterologist", ignoreCase = true) || specialistSection.contains("पेट रोग", ignoreCase = true) -> "Gastroenterologist"
            specialistSection.contains("Pulmonologist", ignoreCase = true) || specialistSection.contains("फेफड़े", ignoreCase = true) -> "Pulmonologist"
            specialistSection.contains("Emergency Department", ignoreCase = true) || specialistSection.contains("आपातकालीन विभाग", ignoreCase = true) -> "Emergency Department"
            else -> "General Physician"
        }
    }

    fun logNewSymptom(
        title: String,
        severity: Int,
        location: String,
        duration: String,
        notes: String,
        status: String = "Ongoing"
    ) {
        viewModelScope.launch {
            val log = SymptomLogEntity(
                symptomTitle = title,
                severity = severity,
                bodyLocation = location,
                duration = duration,
                notesOrTriggers = notes,
                status = status
            )
            repository.saveSymptomLog(log)
        }
    }

    fun updateSymptomStatus(log: SymptomLogEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updateSymptomLog(log.copy(status = newStatus))
        }
    }

    fun deleteSymptomLog(log: SymptomLogEntity) {
        viewModelScope.launch {
            repository.deleteSymptomLog(log)
        }
    }

    fun toggleAutoSpeak() {
        _autoSpeakEnabled.value = !_autoSpeakEnabled.value
    }

    fun toggleSpeech(text: String, messageId: Long? = null) {
        if (_isSpeaking.value && (_speakingMessageId.value == messageId || messageId == null)) {
            stopSpeaking()
            return
        }
        speakText(text, messageId)
    }

    fun speakText(text: String, messageId: Long? = null) {
        stopSpeaking()

        val cleaned = cleanTextForSpeech(text)
        if (cleaned.isBlank()) return

        val locale = detectLocaleForText(cleaned)
        try {
            val avail = textToSpeech?.isLanguageAvailable(locale)
            if (avail != null && avail >= TextToSpeech.LANG_AVAILABLE) {
                textToSpeech?.language = locale
            } else {
                val hiLocale = Locale.forLanguageTag("hi-IN")
                val hiAvail = textToSpeech?.isLanguageAvailable(hiLocale)
                if (hiAvail != null && hiAvail >= TextToSpeech.LANG_AVAILABLE) {
                    textToSpeech?.language = hiLocale
                } else {
                    textToSpeech?.language = Locale.getDefault()
                }
            }
        } catch (_: Exception) {
            textToSpeech?.language = Locale.getDefault()
        }

        _speakingMessageId.value = messageId
        val utteranceId = "msg_${messageId ?: System.currentTimeMillis()}"
        val result = textToSpeech?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.SUCCESS) {
            _isSpeaking.value = true
        } else {
            _speakingMessageId.value = null
            _isSpeaking.value = false
        }
    }

    fun explainRemedyAloud(message: ChatMessageEntity) {
        val remedyText = extractRemedyForSpeech(message.text)
        speakText(remedyText, message.id)
    }

    private fun detectLocaleForText(text: String): Locale {
        val hasGujarati = text.any { it in '\u0A80'..'\u0AFF' }
        if (hasGujarati) {
            val guLocale = Locale.forLanguageTag("gu-IN")
            val available = textToSpeech?.isLanguageAvailable(guLocale)
            if (available != null && available >= TextToSpeech.LANG_AVAILABLE) {
                return guLocale
            }
            return Locale.forLanguageTag("hi-IN")
        }

        val hasHindi = text.any { it in '\u0900'..'\u097F' }
        if (hasHindi) {
            return Locale.forLanguageTag("hi-IN")
        }

        return Locale.forLanguageTag("en-IN")
    }

    private fun cleanTextForSpeech(text: String): String {
        return text
            .replace(Regex("[#*`_~]"), " ")
            .replace("🚨", " ")
            .replace("⚠️", " ")
            .replace("•", ". ")
            .replace("- ", ". ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractRemedyForSpeech(fullText: String): String {
        return cleanTextForSpeech(fullText)
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
        _speakingMessageId.value = null
    }

    // -------------------------------------------------------------
    // WATER REMINDER & HYDRATION TRACKING (પાણી રિમાઇન્ડર)
    // -------------------------------------------------------------
    val todayDateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val todayWaterLogs: StateFlow<List<WaterLogEntity>> = repository.getWaterLogsForDate(todayDateString).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _todayTotalWater = MutableStateFlow(0)
    val todayTotalWaterMl: StateFlow<Int> = _todayTotalWater.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getTotalWaterForDate(todayDateString).collect { total ->
                _todayTotalWater.value = total ?: 0
            }
        }
    }

    private val _waterTargetMl = MutableStateFlow(2500)
    val waterTargetMl: StateFlow<Int> = _waterTargetMl.asStateFlow()

    private val _reminderIntervalHours = MutableStateFlow(2)
    val reminderIntervalHours: StateFlow<Int> = _reminderIntervalHours.asStateFlow()

    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            repository.logWater(amountMl, todayDateString)
        }
    }

    fun deleteWaterLog(log: WaterLogEntity) {
        viewModelScope.launch {
            repository.deleteWaterLog(log)
        }
    }

    fun clearTodayWater() {
        viewModelScope.launch {
            repository.clearWaterLogsForDate(todayDateString)
        }
    }

    fun setWaterTarget(targetMl: Int) {
        _waterTargetMl.value = targetMl
    }

    fun setReminderInterval(hours: Int) {
        _reminderIntervalHours.value = hours
    }

    fun triggerWaterReminderNotification() {
        WaterReminderNotificationHelper.showWaterReminderNotification(
            getApplication(),
            title = "💧 Time to Drink Water! (પાણી પીવાનો સમય થયો છે)",
            message = "Stay hydrated! Drinking water boosts energy, improves digestion, and prevents headaches. Take a glass of water now!"
        )
    }

    fun saveProfile(
        name: String,
        age: Int,
        gender: String,
        bloodGroup: String,
        allergies: String,
        chronicConditions: String,
        medications: String,
        pastSurgeries: String,
        emergencyName: String,
        emergencyPhone: String,
        language: String
    ) {
        viewModelScope.launch {
            val entity = UserProfileEntity(
                id = 1,
                fullName = name,
                age = age,
                gender = gender,
                bloodGroup = bloodGroup,
                allergies = allergies,
                chronicConditions = chronicConditions,
                currentMedications = medications,
                pastSurgeries = pastSurgeries,
                emergencyContactName = emergencyName,
                emergencyContactPhone = emergencyPhone,
                preferredLanguage = language
            )
            repository.saveUserProfile(entity)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteSession(session: TriageSessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun generateDoctorSummaryText(): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val profile = userProfile.value
        val logs = symptomLogs.value
        val pastSessions = sessions.value

        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append("PATIENT CLINICAL HEALTH SUMMARY\n")
        sb.append("Prepared for Doctor / Physician Review\n")
        sb.append("Date of Export: $currentDate\n")
        sb.append("========================================\n\n")

        // Patient Background
        sb.append("--- PATIENT BACKGROUND ---\n")
        if (profile != null) {
            sb.append("• Name: ${profile.fullName}\n")
            sb.append("• Age: ${profile.age} | Gender: ${profile.gender} | Blood Group: ${profile.bloodGroup}\n")
            sb.append("• Known Allergies: ${profile.allergies}\n")
            sb.append("• Chronic Conditions: ${profile.chronicConditions}\n")
            sb.append("• Current Medications: ${profile.currentMedications}\n")
            sb.append("• Medical/Surgical History: ${profile.pastSurgeries}\n")
            sb.append("• Emergency Contact: ${profile.emergencyContactName} (${profile.emergencyContactPhone})\n")
        } else {
            sb.append("• Profile: Not yet configured\n")
        }
        sb.append("\n")

        // Recorded Symptom History
        sb.append("--- RECORDED SYMPTOM HISTORY (${logs.size} entries) ---\n")
        if (logs.isEmpty()) {
            sb.append("No symptoms recorded in diary yet.\n")
        } else {
            logs.forEachIndexed { index, log ->
                val dateStr = dateTimeFormat.format(Date(log.timestamp))
                val severityCategory = when {
                    log.severity <= 3 -> "Mild"
                    log.severity <= 6 -> "Moderate"
                    log.severity <= 8 -> "Severe"
                    else -> "Very Severe"
                }
                sb.append("${index + 1}. $dateStr — ${log.symptomTitle}\n")
                sb.append("   • Severity: ${log.severity}/10 ($severityCategory)\n")
                sb.append("   • Anatomical Location: ${log.bodyLocation}\n")
                sb.append("   • Duration: ${log.duration}\n")
                sb.append("   • Current Status: ${log.status}\n")
                if (log.notesOrTriggers.isNotBlank()) {
                    sb.append("   • Triggers / Notes: ${log.notesOrTriggers}\n")
                }
                sb.append("\n")
            }
        }

        // Triage Consultations
        if (pastSessions.isNotEmpty()) {
            sb.append("--- RECENT AI TRIAGE SESSIONS (${pastSessions.size}) ---\n")
            pastSessions.take(5).forEachIndexed { idx, s ->
                val dateStr = dateTimeFormat.format(Date(s.timestamp))
                sb.append("${idx + 1}. [$dateStr] ${s.primarySymptom}\n")
                sb.append("   • Severity: ${s.severity}/10 | Duration: ${s.duration}\n")
                sb.append("   • Recommended Doctor: ${s.recommendedSpecialist}\n")
                if (s.summary.isNotBlank()) {
                    sb.append("   • Triage Summary: ${s.summary.take(160)}\n")
                }
                sb.append("\n")
            }
        }

        sb.append("========================================\n")
        sb.append("Disclaimer: This patient-reported symptom history is compiled via MediAssist for clinical consultation with a licensed healthcare provider.\n")
        return sb.toString()
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}

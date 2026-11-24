package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.trLang

enum class AppScreen {
    TRIAGE_CHAT,
    WATER_REMINDER,
    MEDICINE_REMINDER,
    EMERGENCY_GUIDE,
    HISTORY,
    PROFILE,
    APP_INFO,
    REPORT_SCANNER,
    STEP_COUNTER,
    VITALS_TRACKER,
    FAMILY_CARE,
    SMART_HUB,
    CLINICAL_AI_SUITE,
    AI_VIDEO_CALL,
    DAILY_SYMPTOM_LOGGER,
    DEVELOPER_LOGS,
    DEVELOPER_CONSOLE,
    AI_MEDICINE_ADVISOR,
    AUTH_LOGIN,
    ULTRA_HEALTH_HUB
}

enum class UltraHealthTab {
    SCORE,
    AI_COACH,
    BODY_MAP,
    NUTRITION,
    SOS_PASSPORT,
    STREAKS,
    MEDICAL_ID,
    WELLNESS,
    LAB_TRENDS
}

enum class ClinicalAiTab {
    TREATMENT_PLAN,
    PREDICTIVE_ANALYTICS,
    CLINICAL_SCRIBE,
    REMOTE_MONITORING,
    EHR_AUTOMATION,
    MENTAL_HEALTH,
    DRUG_DISCOVERY,
    EPIDEMIC_PREDICTION,
    DOCTOR_FINDER
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
    private var pendingSpeechRequest: Pair<String, Long?>? = null
    private val _speakingMessageId = MutableStateFlow<Long?>(null)
    val speakingMessageId: StateFlow<Long?> = _speakingMessageId.asStateFlow()

    private val _autoSpeakEnabled = MutableStateFlow(true)
    val autoSpeakEnabled: StateFlow<Boolean> = _autoSpeakEnabled.asStateFlow()

    private val _retryableQuery = MutableStateFlow<Pair<String, SymptomDetails?>?>(null)
    val retryableQuery: StateFlow<Pair<String, SymptomDetails?>?> = _retryableQuery.asStateFlow()

    private fun <T> Flow<T>.safeStateIn(initialValue: T): StateFlow<T> =
        this.catch { emit(initialValue) }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = initialValue
        )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TriageRepository(database.triageDao())

        com.example.utils.DevLogger.attachPersistenceListener { level, tag, message, ts ->
            viewModelScope.launch {
                try {
                    repository.saveDeveloperLogToDb(level, tag, message, ts)
                } catch (_: Throwable) {
                }
            }
        }
        com.example.utils.DevLogger.i(
            "AppLifecycle",
            "MediAssistViewModel initialized (Room v17, Live Public Upgrades + Exact Alarms + WorkManager ready)"
        )

        startLivePublicConfigObserver()

        viewModelScope.launch {
            try {
                kotlinx.coroutines.yield()
                initTextToSpeech(application)
            } catch (_: Throwable) {
            }
        }
    }

    // -------------------------------------------------------------
    // AUTHENTICATION: DEVELOPER (SHAILESH), CUSTOM ACCOUNT & MOBILE OTP
    // -------------------------------------------------------------
    private val _authSession = MutableStateFlow(
        runCatching { com.example.utils.AuthSecurityHelper.loadSession(application) }
            .getOrElse { com.example.utils.AuthSessionState() }
    )
    val authSession: StateFlow<com.example.utils.AuthSessionState> = _authSession.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authInfoMessage = MutableStateFlow<String?>(null)
    val authInfoMessage: StateFlow<String?> = _authInfoMessage.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private var pendingOtpPhone: String = ""
    private var pendingOtpCode: String = ""
    private var pendingOtpExpiresAt: Long = 0L

    private val _otpSentToPhone = MutableStateFlow<String?>(null)
    val otpSentToPhone: StateFlow<String?> = _otpSentToPhone.asStateFlow()

    fun clearAuthFeedback() {
        _authError.value = null
        _authInfoMessage.value = null
    }

    fun loginAsDeveloper(username: String, password: String): Boolean {
        _authError.value = null
        _authInfoMessage.value = null
        val ok = com.example.utils.AuthSecurityHelper.verifyDeveloperCredentials(username, password)
        if (!ok) {
            _authError.value = trLang(
                _selectedLanguage.value,
                "Invalid Developer credentials. Only authorized developer username & password can access.",
                "અમાન્ય ડેવલપર વિગતો! માત્ર અધિકૃત ડેવલપર યુઝરનેમ અને પાસવર્ડથી જ લોગિન થઈ શકે છે.",
                "अमान्य डेवलपर विवरण! केवल अधिकृत डेवलपर यूज़रनेम और पासवर्ड से ही लॉगिन हो सकता है।"
            )
            com.example.utils.DevLogger.w("Auth", "Rejected unauthorized developer login attempt for '${username.trim()}'")
            return false
        }
        val session = com.example.utils.AuthSessionState(
            isLoggedIn = true,
            username = com.example.utils.AuthSecurityHelper.DEVELOPER_USERNAME,
            displayName = "Shailesh Satiya (Developer)",
            role = com.example.utils.AuthRole.DEVELOPER,
            authMethod = "DEVELOPER"
        )
        com.example.utils.AuthSecurityHelper.saveSession(getApplication(), session)
        _authSession.value = session
        if (_currentScreen.value == AppScreen.AUTH_LOGIN) {
            _currentScreen.value = AppScreen.TRIAGE_CHAT
        }
        com.example.utils.DevLogger.i("Auth", "Developer SHAILESH logged in successfully")
        recordOnlineSessionHeartbeat(0L, isOnline = true)
        syncPublicDataFromCloud()
        return true
    }

    fun registerWithUsernamePassword(
        username: String,
        displayName: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit = {}
    ) {
        if (_authLoading.value) return
        _authError.value = null
        _authInfoMessage.value = null
        val cleanUser = username.trim()
        if (cleanUser.length < 3) {
            _authError.value = trLang(
                _selectedLanguage.value,
                "Username must be at least 3 characters.",
                "યુઝરનેમ ઓછામાં ઓછું 3 અક્ષરનું રાખો.",
                "यूज़रनेम कम से कम 3 अक्षरों का होना चाहिए।"
            )
            return
        }
        if (password.length < 6) {
            _authError.value = trLang(
                _selectedLanguage.value,
                "Password must be at least 6 characters.",
                "પાસવર્ડ ઓછામાં ઓછો 6 અક્ષરનો રાખો.",
                "पासवर्ड कम से कम 6 अक्षरों का होना चाहिए।"
            )
            return
        }
        if (password != confirmPassword) {
            _authError.value = trLang(
                _selectedLanguage.value,
                "Passwords do not match.",
                "બંને પાસવર્ડ સરખા નથી.",
                "दोनों पासवर्ड मेल नहीं खाते।"
            )
            return
        }
        _authLoading.value = true
        viewModelScope.launch {
            val res = repository.registerCustomUserAccount(cleanUser, displayName, password)
            _authLoading.value = false
            res.onSuccess { account ->
                val session = com.example.utils.AuthSessionState(
                    isLoggedIn = true,
                    username = account.username,
                    displayName = account.displayName,
                    role = com.example.utils.AuthRole.PATIENT,
                    authMethod = account.authMethod
                )
                com.example.utils.AuthSecurityHelper.saveSession(getApplication(), session)
                _authSession.value = session
                _authError.value = null
                _authInfoMessage.value = null
                _currentScreen.value = AppScreen.TRIAGE_CHAT
                recordOnlineSessionHeartbeat(0L, isOnline = true)
                syncPublicDataFromCloud()
                onSuccess()
            }.onFailure { err ->
                _authError.value = err.message ?: trLang(
                    _selectedLanguage.value,
                    "Sign up failed. Please try another username.",
                    "સાઇન અપ નિષ્ફળ ગયું. કૃપા કરીને બીજું યુઝરનેમ અજમાવો અથવા લોગિન કરો.",
                    "साइन अप विफल रहा। कृपया दूसरा यूज़रनेम चुनें या लॉगिन करें।"
                )
            }
        }
    }

    fun loginWithUsernamePassword(
        username: String,
        password: String,
        onSuccess: () -> Unit = {}
    ) {
        if (_authLoading.value) return
        _authError.value = null
        _authInfoMessage.value = null
        val cleanUser = username.trim()
        if (cleanUser.isBlank() || password.isBlank()) {
            _authError.value = trLang(
                _selectedLanguage.value,
                "Enter both username and password.",
                "યુઝરનેમ અને પાસવર્ડ બંને દાખલ કરો.",
                "यूज़रनेम और पासवर्ड दोनों दर्ज करें।"
            )
            return
        }
        _authLoading.value = true
        viewModelScope.launch {
            val res = repository.loginCustomUserAccount(cleanUser, password)
            _authLoading.value = false
            res.onSuccess { account ->
                val session = com.example.utils.AuthSessionState(
                    isLoggedIn = true,
                    username = account.username,
                    displayName = account.displayName,
                    phoneNumber = account.phoneNumber,
                    role = com.example.utils.AuthRole.PATIENT,
                    authMethod = account.authMethod
                )
                com.example.utils.AuthSecurityHelper.saveSession(getApplication(), session)
                _authSession.value = session
                _authError.value = null
                _authInfoMessage.value = null
                _currentScreen.value = AppScreen.TRIAGE_CHAT
                recordOnlineSessionHeartbeat(0L, isOnline = true)
                syncPublicDataFromCloud()
                onSuccess()
            }.onFailure { err ->
                _authError.value = err.message ?: trLang(
                    _selectedLanguage.value,
                    "Login failed. Check your username and password.",
                    "લોગિન નિષ્ફળ ગયું. યુઝરનેમ અને પાસવર્ડ તપાસો.",
                    "लॉगिन विफल। कृपया यूज़रनेम और पासवर्ड जांचें।"
                )
            }
        }
    }

    fun sendLoginOtp(rawPhone: String): String? {
        _authError.value = null
        val normalized = com.example.utils.AuthSecurityHelper.normalizePhoneNumber(rawPhone)
        if (normalized.length < 10) {
            _authError.value = trLang(
                _selectedLanguage.value,
                "Please enter a valid 10-digit mobile number.",
                "કૃપા કરીને માન્ય 10-અંકનો મોબાઇલ નંબર નાખો.",
                "कृपया मान्य 10-अंकों का मोबाइल नंबर दर्ज करें।"
            )
            return null
        }
        val code = com.example.utils.AuthSecurityHelper.generateOtpCode()
        pendingOtpPhone = normalized
        pendingOtpCode = code
        pendingOtpExpiresAt = System.currentTimeMillis() + 5 * 60_000L
        _otpSentToPhone.value = normalized
        val smsSent = com.example.utils.AuthSecurityHelper.dispatchMobileOtp(getApplication(), normalized, code)
        _authInfoMessage.value = if (smsSent) {
            trLang(
                _selectedLanguage.value,
                "OTP sent via SMS & system notification to +91 $normalized",
                "+91 $normalized પર SMS અને નોટિફિકેશન દ્વારા OTP મોકલ્યો છે",
                "+91 $normalized पर SMS और नोटिफिकेशन द्वारा OTP भेजा गया है"
            )
        } else {
            trLang(
                _selectedLanguage.value,
                "OTP notification sent for +91 $normalized (Code: $code)",
                "+91 $normalized માટે OTP નોટિફિકેશન મોકલ્યો છે (કોડ: $code)",
                "+91 $normalized के लिए OTP नोटिफिकेशन भेजा गया है (कोड: $code)"
            )
        }
        return code
    }

    fun verifyLoginOtp(
        rawPhone: String,
        enteredCode: String,
        onSuccess: () -> Unit = {}
    ) {
        if (_authLoading.value) return
        _authError.value = null
        val normalized = com.example.utils.AuthSecurityHelper.normalizePhoneNumber(rawPhone)
        val cleanOtp = enteredCode.trim()
        if (pendingOtpCode.isBlank() || pendingOtpPhone != normalized) {
            _authError.value = trLang(
                _selectedLanguage.value,
                "Please request a new OTP for this mobile number.",
                "પહેલા આ મોબાઇલ નંબર માટે નવો OTP મંગાવો.",
                "पहले इस मोबाइल नंबर के लिए नया OTP मंगाएं।"
            )
            return
        }
        if (System.currentTimeMillis() > pendingOtpExpiresAt) {
            _authError.value = trLang(
                _selectedLanguage.value,
                "OTP expired. Please resend a fresh OTP.",
                "OTP નો સમય સમાપ્ત થયો છે. ફરી મોકલો.",
                "OTP समाप्त हो गया है। कृपया पुनः भेजें।"
            )
            return
        }
        if (cleanOtp != pendingOtpCode) {
            _authError.value = trLang(
                _selectedLanguage.value,
                "Incorrect OTP code. Please check notification/SMS and try again.",
                "ખોટો OTP કોડ! નોટિફિકેશન/SMS તપાસી ફરી દાખલ કરો.",
                "गलत OTP कोड! नोटिफिकेशन/SMS जांच कर पुनः प्रयास करें।"
            )
            return
        }

        _authLoading.value = true
        viewModelScope.launch {
            val res = repository.findOrCreateOtpAccount(normalized)
            _authLoading.value = false
            res.onSuccess { account ->
                pendingOtpCode = ""
                _otpSentToPhone.value = null
                val session = com.example.utils.AuthSessionState(
                    isLoggedIn = true,
                    username = account.username,
                    displayName = account.displayName,
                    phoneNumber = account.phoneNumber,
                    role = com.example.utils.AuthRole.PATIENT,
                    authMethod = "MOBILE_OTP"
                )
                com.example.utils.AuthSecurityHelper.saveSession(getApplication(), session)
                _authSession.value = session
                _authError.value = null
                _authInfoMessage.value = null
                _currentScreen.value = AppScreen.TRIAGE_CHAT
                recordOnlineSessionHeartbeat(0L, isOnline = true)
                syncPublicDataFromCloud()
                onSuccess()
            }.onFailure { err ->
                _authError.value = err.message ?: "OTP verification failed"
            }
        }
    }

    fun logoutAccount() {
        val currentUser = _authSession.value.username
        if (currentUser.isNotBlank()) {
            markCurrentSessionOffline(currentUser, _authSession.value.authMethod)
        }
        com.example.utils.AuthSecurityHelper.clearSession(getApplication())
        _authSession.value = com.example.utils.AuthSessionState()
        _otpSentToPhone.value = null
        _authLoading.value = false
        _currentScreen.value = AppScreen.TRIAGE_CHAT
        clearAuthFeedback()
        com.example.utils.DevLogger.i("Auth", "User signed out")
    }

    // -------------------------------------------------------------
    // AI SYMPTOM & SEVERITY MEDICINE ADVISOR
    // -------------------------------------------------------------
    private val _medicineAdvisorResult = MutableStateFlow<com.example.utils.MedicineAdvisorResult?>(null)
    val medicineAdvisorResult: StateFlow<com.example.utils.MedicineAdvisorResult?> = _medicineAdvisorResult.asStateFlow()

    private val _isAnalyzingMedicineAdvisor = MutableStateFlow(false)
    val isAnalyzingMedicineAdvisor: StateFlow<Boolean> = _isAnalyzingMedicineAdvisor.asStateFlow()

    fun evaluateMedicineAdvisor(
        symptomOrIllness: String,
        severity: Int,
        duration: String
    ) {
        val clean = symptomOrIllness.trim()
        if (clean.isBlank()) return
        _isAnalyzingMedicineAdvisor.value = true
        viewModelScope.launch {
            try {
                val res = repository.evaluateMedicineBySymptoms(
                    symptomOrIllness = clean,
                    severity = severity,
                    duration = duration,
                    language = _selectedLanguage.value,
                    userProfile = userProfile.value
                )
                _medicineAdvisorResult.value = res
                com.example.utils.DevLogger.i(
                    "MedicineAdvisor",
                    "Analyzed '$clean' (severity=$severity, tier=${res.tier}, meds=${res.medicines.size})"
                )
            } catch (e: Exception) {
                com.example.utils.DevLogger.e("MedicineAdvisor", "Advisor evaluation failed: ${e.message}")
            } finally {
                _isAnalyzingMedicineAdvisor.value = false
            }
        }
    }

    fun clearMedicineAdvisor() {
        _medicineAdvisorResult.value = null
    }

    private fun initTextToSpeech(application: Application) {
        if (textToSpeech != null) return
        try {
            textToSpeech = TextToSpeech(application) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        textToSpeech?.setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        textToSpeech?.setSpeechRate(0.95f)
                        textToSpeech?.setPitch(1.04f)
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
                        val queued = pendingSpeechRequest
                        pendingSpeechRequest = null
                        if (queued != null && _autoSpeakEnabled.value) {
                            speakText(queued.first, queued.second)
                        }
                    } catch (_: Exception) {
                        _isTtsReady.value = false
                    }
                }
            }
        } catch (_: Exception) {
            textToSpeech = null
            _isTtsReady.value = false
        }
    }

    val sessions: StateFlow<List<TriageSessionEntity>> = repository.allSessions.safeStateIn(emptyList())

    val symptomLogs: StateFlow<List<SymptomLogEntity>> = repository.allSymptomLogs.safeStateIn(emptyList())

    val symptomNotes: StateFlow<List<com.example.data.local.SymptomNoteEntity>> =
        repository.allSymptomNotes.safeStateIn(emptyList())

    val persistedDeveloperLogs: StateFlow<List<com.example.data.local.DeveloperLogEntity>> =
        repository.allDeveloperLogs.safeStateIn(emptyList())

    val liveDeveloperLogs: StateFlow<List<com.example.utils.DevLogEntry>> =
        com.example.utils.DevLogger.logs

    val headacheLogs: StateFlow<List<SymptomLogEntity>> = repository.headacheLogs.safeStateIn(emptyList())

    val medicines: StateFlow<List<com.example.data.local.MedicineEntity>> = repository.allMedicines.safeStateIn(emptyList())

    val allAdherenceLogs: StateFlow<List<com.example.data.local.MedicationAdherenceEntity>> =
        repository.allAdherenceLogs.safeStateIn(emptyList())

    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages.safeStateIn(emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile.safeStateIn(null)

    val vitalLogs: StateFlow<List<com.example.data.local.VitalLogEntity>> = repository.allVitalLogs.safeStateIn(emptyList())

    val familyMembers: StateFlow<List<com.example.data.local.FamilyMemberEntity>> = repository.allFamilyMembers.safeStateIn(emptyList())

    val treatmentPlans: StateFlow<List<com.example.data.local.TreatmentPlanEntity>> = repository.allTreatmentPlans.safeStateIn(emptyList())

    val riskAssessments: StateFlow<List<com.example.data.local.RiskAssessmentEntity>> = repository.allRiskAssessments.safeStateIn(emptyList())

    val clinicalNotes: StateFlow<List<com.example.data.local.ClinicalNoteEntity>> = repository.allClinicalNotes.safeStateIn(emptyList())

    val ehrRecords: StateFlow<List<com.example.data.local.EhrRecordEntity>> = repository.allEhrRecords.safeStateIn(emptyList())

    val mentalHealthCheckIns: StateFlow<List<com.example.data.local.MentalHealthCheckInEntity>> = repository.allMentalHealthCheckIns.safeStateIn(emptyList())

    val drugStudies: StateFlow<List<com.example.data.local.DrugCandidateStudyEntity>> = repository.allDrugStudies.safeStateIn(emptyList())

    val outbreakAlerts: StateFlow<List<com.example.data.local.OutbreakAlertEntity>> = repository.allOutbreakAlerts.safeStateIn(emptyList())

    val doctorAppointments: StateFlow<List<com.example.data.local.DoctorAppointmentEntity>> = repository.allDoctorAppointments.safeStateIn(emptyList())

    private val _currentScreen = MutableStateFlow(AppScreen.TRIAGE_CHAT)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedClinicalTab = MutableStateFlow(ClinicalAiTab.TREATMENT_PLAN)
    val selectedClinicalTab: StateFlow<ClinicalAiTab> = _selectedClinicalTab.asStateFlow()

    fun setClinicalTab(tab: ClinicalAiTab) {
        _selectedClinicalTab.value = tab
    }

    fun openClinicalAiSuite(tab: ClinicalAiTab) {
        _selectedClinicalTab.value = tab
        _currentScreen.value = AppScreen.CLINICAL_AI_SUITE
    }

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

    private val languagePrefs by lazy {
        getApplication<Application>().getSharedPreferences("app_language_prefs", Context.MODE_PRIVATE)
    }

    private val _selectedLanguage = MutableStateFlow(
        runCatching {
            com.example.ui.theme.AppLanguages.findByName(
                application.getSharedPreferences("app_language_prefs", Context.MODE_PRIVATE)
                    .getString("selected_language", "English")
            ).id
        }.getOrDefault("English")
    )
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun toggleThemeMode() {
        _themeMode.value = when (_themeMode.value) {
            ThemeMode.SYSTEM -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.SYSTEM
        }
    }

    private val _selectedImageUri = MutableStateFlow<String?>(null)
    val selectedImageUri: StateFlow<String?> = _selectedImageUri.asStateFlow()

    private val _selectedImageBase64 = MutableStateFlow<String?>(null)
    val selectedImageBase64: StateFlow<String?> = _selectedImageBase64.asStateFlow()

    private val _selectedImageMimeType = MutableStateFlow<String?>("image/jpeg")
    val selectedImageMimeType: StateFlow<String?> = _selectedImageMimeType.asStateFlow()

    // Scanned Medical Reports & Prescriptions (OCR)
    val scannedReports: StateFlow<List<com.example.data.local.ScannedReportEntity>> =
        repository.allScannedReports.safeStateIn(emptyList())

    private val _isOcrScanning = MutableStateFlow(false)
    val isOcrScanning: StateFlow<Boolean> = _isOcrScanning.asStateFlow()

    private val _ocrExtractedText = MutableStateFlow<String?>(null)
    val ocrExtractedText: StateFlow<String?> = _ocrExtractedText.asStateFlow()

    private val _ocrAiExplanation = MutableStateFlow<String?>(null)
    val ocrAiExplanation: StateFlow<String?> = _ocrAiExplanation.asStateFlow()

    private val _ocrSelectedImageUri = MutableStateFlow<String?>(null)
    val ocrSelectedImageUri: StateFlow<String?> = _ocrSelectedImageUri.asStateFlow()

    private val _ocrErrorMessage = MutableStateFlow<String?>(null)
    val ocrErrorMessage: StateFlow<String?> = _ocrErrorMessage.asStateFlow()

    // Step Counter & Fitness Tracking State
    val todayDateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val todayAdherenceLogs: StateFlow<List<com.example.data.local.MedicationAdherenceEntity>> =
        repository.getAdherenceLogsForDate(todayDateString).safeStateIn(emptyList())

    val todayStepRecord: StateFlow<com.example.data.local.StepRecordEntity?> =
        repository.getStepRecordForDate(todayDateString).safeStateIn(null)

    val weeklyStepRecords: StateFlow<List<com.example.data.local.StepRecordEntity>> =
        repository.recentStepRecords.safeStateIn(emptyList())

    private val _isStepTrackingActive = MutableStateFlow(true)
    val isStepTrackingActive: StateFlow<Boolean> = _isStepTrackingActive.asStateFlow()

    private val _sensorHardwareDescription = MutableStateFlow("")
    val sensorHardwareDescription: StateFlow<String> = _sensorHardwareDescription.asStateFlow()

    private val _isSensorAvailable = MutableStateFlow(false)
    val isSensorAvailable: StateFlow<Boolean> = _isSensorAvailable.asStateFlow()

    private var stepSensorTracker: com.example.utils.StepSensorTracker? = null

    init {
        try {
            val tracker = com.example.utils.StepSensorTracker(application) { stepDelta ->
                onSensorStepDetected(stepDelta)
            }
            stepSensorTracker = tracker
            _isSensorAvailable.value = tracker.isSensorAvailable
            _sensorHardwareDescription.value = tracker.sensorTypeDescription
            val hasActivityPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    application,
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            if (hasActivityPermission && _isStepTrackingActive.value) {
                tracker.startTracking()
            } else if (!hasActivityPermission) {
                _isStepTrackingActive.value = false
            }
        } catch (_: Throwable) {
            _sensorHardwareDescription.value = "Sensor Standby / Manual Mode"
            _isStepTrackingActive.value = false
        }
    }

    // Nearby Hospitals & Medical Places (Maps)
    private val _nearbyMedicalPlaces = MutableStateFlow<List<com.example.data.model.NearbyMedicalPlace>>(emptyList())
    val nearbyMedicalPlaces: StateFlow<List<com.example.data.model.NearbyMedicalPlace>> = _nearbyMedicalPlaces.asStateFlow()

    private val _isLocatingHospitals = MutableStateFlow(false)
    val isLocatingHospitals: StateFlow<Boolean> = _isLocatingHospitals.asStateFlow()

    private val _userCurrentGpsLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userCurrentGpsLocation: StateFlow<Pair<Double, Double>?> = _userCurrentGpsLocation.asStateFlow()

    private val _nearbyPlacesFilter = MutableStateFlow("All")
    val nearbyPlacesFilter: StateFlow<String> = _nearbyPlacesFilter.asStateFlow()

    fun setNearbyPlacesFilter(filter: String) {
        _nearbyPlacesFilter.value = filter
    }

    fun locateNearbyHospitals(context: android.content.Context) {
        _isLocatingHospitals.value = true
        viewModelScope.launch {
            try {
                val coords = com.example.utils.EmergencySosLocationHelper.getCurrentLocation(context)
                val lat = coords?.first ?: 22.3039 // Default fallback regional GPS
                val lon = coords?.second ?: 70.8022
                _userCurrentGpsLocation.value = Pair(lat, lon)
                val places = com.example.utils.NearbyHospitalsService.fetchRealNearbyFacilities(lat, lon)
                _nearbyMedicalPlaces.value = places
            } catch (_: Exception) {
                // Fallback to sample regional hospitals if GPS retrieval errors
                val lat = 22.3039
                val lon = 70.8022
                _userCurrentGpsLocation.value = Pair(lat, lon)
                _nearbyMedicalPlaces.value = com.example.utils.NearbyHospitalsService.getNearbyFacilities(lat, lon)
            } finally {
                _isLocatingHospitals.value = false
            }
        }
    }

    fun scanAndExplainReport(
        context: android.content.Context,
        bitmap: android.graphics.Bitmap? = null,
        uri: android.net.Uri? = null,
        reportType: String = "Medical Document"
    ) {
        _isOcrScanning.value = true
        _ocrErrorMessage.value = null
        _ocrExtractedText.value = null
        _ocrAiExplanation.value = null
        if (uri != null) {
            _ocrSelectedImageUri.value = uri.toString()
        }

        viewModelScope.launch {
            val fileName = if (uri != null) {
                com.example.utils.MedicalReportOcrScanner.getFileName(context, uri)
            } else {
                null
            }
            val isPdf = uri != null && com.example.utils.MedicalReportOcrScanner.isPdfUri(context, uri)

            val ocrResult = if (bitmap != null) {
                com.example.utils.MedicalReportOcrScanner.extractTextFromBitmap(bitmap)
            } else if (uri != null) {
                com.example.utils.MedicalReportOcrScanner.extractTextFromUri(context, uri)
            } else {
                Result.failure(Exception("No image or PDF provided"))
            }

            if (ocrResult.isFailure) {
                _isOcrScanning.value = false
                _ocrErrorMessage.value = ocrResult.exceptionOrNull()?.message ?: "Failed to read text from report."
                return@launch
            }

            val text = ocrResult.getOrNull() ?: ""
            _ocrExtractedText.value = text

            val effectiveType = if (isPdf && reportType.isBlank()) "Lab & Blood Report (PDF)" else reportType
            val explainResult = repository.explainMedicalReportOrPrescription(
                extractedText = text,
                reportType = effectiveType,
                language = _selectedLanguage.value
            )

            val explanation = explainResult.getOrNull() ?: ""
            _ocrAiExplanation.value = explanation
            _isOcrScanning.value = false

            val savedTitle = when {
                !fileName.isNullOrBlank() && effectiveType.isNotBlank() -> "$effectiveType • $fileName"
                !fileName.isNullOrBlank() -> fileName
                effectiveType.isNotBlank() -> effectiveType
                else -> "Scanned Medical Report"
            }

            // Automatically persist to Room Database
            repository.saveScannedReport(
                com.example.data.local.ScannedReportEntity(
                    title = savedTitle,
                    extractedText = text,
                    aiExplanation = explanation,
                    imageUri = _ocrSelectedImageUri.value,
                    reportType = if (isPdf) "Lab Report (PDF)" else effectiveType
                )
            )
        }
    }

    fun analyzePdfReportInChat(
        context: android.content.Context,
        pdfUri: android.net.Uri
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            val fileName = com.example.utils.MedicalReportOcrScanner.getFileName(context, pdfUri) ?: "Lab_Report.pdf"
            val ocrResult = com.example.utils.MedicalReportOcrScanner.extractTextFromPdfUri(context, pdfUri)
            val lang = _selectedLanguage.value

            if (ocrResult.isFailure) {
                val errMsg = ocrResult.exceptionOrNull()?.message ?: "Could not read PDF lab report."
                repository.saveMessage(
                    ChatMessageEntity(
                        sender = "AI",
                        text = trLang(
                            lang,
                            "⚠️ Unable to read PDF report ($fileName): $errMsg",
                            "⚠️ PDF રિપોર્ટ ($fileName) વાંચી શકાયો નહીં: $errMsg",
                            "⚠️ PDF रिपोर्ट ($fileName) पढ़ी नहीं जा सकी: $errMsg"
                        )
                    )
                )
                _isLoading.value = false
                return@launch
            }

            val extractedText = ocrResult.getOrNull() ?: ""

            // Persist to Scanned Reports vault as well
            val explainResult = repository.explainMedicalReportOrPrescription(
                extractedText = extractedText,
                reportType = "Lab & Blood Report (PDF)",
                language = lang
            )
            val explanationText = explainResult.getOrNull()
            if (!explanationText.isNullOrBlank()) {
                repository.saveScannedReport(
                    com.example.data.local.ScannedReportEntity(
                        title = "Lab PDF • $fileName",
                        extractedText = extractedText,
                        aiExplanation = explanationText,
                        imageUri = pdfUri.toString(),
                        reportType = "Lab Report (PDF)"
                    )
                )
            }

            // Show user's PDF upload in chat and trigger full AI analysis response
            val promptForChat = trLang(
                lang,
                "📄 Uploaded Lab Report PDF: $fileName\n\nExtracted Lab Values:\n${extractedText.take(3000)}\n\nPlease explain these lab report results clearly, highlight any abnormal values, and suggest next steps.",
                "📄 PDF લેબ રિપોર્ટ અપલોડ કર્યો છે: $fileName\n\nરિપોર્ટની વિગત:\n${extractedText.take(3000)}\n\nકૃપા કરીને આ લેબ રિપોર્ટના બધા આંકડા (નોર્મલ કે વધઘટ) સરળ ગુજરાતી ભાષામાં સમજાવો અને આગળ શું કરવું તે જણાવો.",
                "📄 PDF लैब रिपोर्ट अपलोड की गई है: $fileName\n\nरिपोर्ट का विवरण:\n${extractedText.take(3000)}\n\nकृपया इस लैब रिपोर्ट के सभी परिणाम (सामान्य या असामान्य मान) सरल हिंदी में समझाएं और आगे के सुझाव दें।"
            )

            _isLoading.value = false
            sendMessage(promptForChat)
        }
    }

    fun clearOcrScan() {
        _ocrExtractedText.value = null
        _ocrAiExplanation.value = null
        _ocrSelectedImageUri.value = null
        _ocrErrorMessage.value = null
        _isOcrScanning.value = false
    }

    fun deleteScannedReport(report: com.example.data.local.ScannedReportEntity) {
        viewModelScope.launch {
            repository.deleteScannedReport(report)
        }
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
        com.example.utils.DevLogger.d("Navigation", "Screen changed -> ${screen.name}")
    }

    fun updateInputText(newText: String) {
        _inputText.value = newText
    }

    fun setSelectedLanguage(lang: String) {
        val normalized = com.example.ui.theme.AppLanguages.findByName(lang).id
        _selectedLanguage.value = normalized
        try {
            languagePrefs.edit().putString("selected_language", normalized).apply()
        } catch (_: Exception) {}
        applyAppLocale(normalized)
    }

    fun refreshAppLanguage() {
        val currentLang = _selectedLanguage.value
        applyAppLocale(currentLang)
    }

    private fun applyAppLocale(lang: String) {
        try {
            val locale = com.example.ui.theme.AppLanguages.localeFor(lang)
            Locale.setDefault(locale)
            val app = getApplication<Application>()
            val config = Configuration(app.resources.configuration)
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            app.resources.updateConfiguration(config, app.resources.displayMetrics)
        } catch (_: Throwable) {}
    }

    fun setSelectedImage(uriString: String?, base64Data: String?, mimeType: String? = "image/jpeg") {
        _selectedImageUri.value = uriString
        _selectedImageBase64.value = base64Data
        _selectedImageMimeType.value = mimeType ?: "image/jpeg"
    }

    fun clearSelectedImage() {
        _selectedImageUri.value = null
        _selectedImageBase64.value = null
        _selectedImageMimeType.value = null
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
        val currentImageUri = _selectedImageUri.value
        val currentImageBase64 = _selectedImageBase64.value
        val currentImageMime = _selectedImageMimeType.value ?: "image/jpeg"

        // Allow sending if text is present OR an image is attached
        if (messageText.isBlank() && currentImageBase64.isNullOrBlank()) return

        val lang = _selectedLanguage.value
        val userPrompt = if (messageText.isBlank() && !currentImageBase64.isNullOrBlank()) {
            trLang(
                lang,
                "Please visually evaluate and clinically analyze this attached medical/symptom image.",
                "કૃપા કરીને આ જોડેલા મેડિકલ/લક્ષણના ફોટાનું ધ્યાનપૂર્વક વિશ્લેષણ કરો.",
                "कृपया इस संलग्न मेडिकल/लक्षण फ़ोटो का ध्यानपूर्वक विश्लेषण करें।"
            )
        } else {
            messageText
        }

        val formattedMessage = if (symptomDetails != null) {
            """
                $userPrompt
                [Clinical Context Attached: Severity: ${symptomDetails.severity}/10, Duration: ${symptomDetails.duration}, Location: ${symptomDetails.location}, Associated: ${if (symptomDetails.associatedSymptoms.isNotBlank()) symptomDetails.associatedSymptoms else "None"}]
            """.trimIndent()
        } else {
            userPrompt
        }

        val isEmergency = repository.isEmergencyText(userPrompt)
        if (isEmergency) {
            _emergencyAlertTriggered.value = true
        }

        val userMessageEntity = ChatMessageEntity(
            sender = "USER",
            text = formattedMessage,
            isEmergencyAlert = isEmergency,
            imageUri = currentImageUri,
            imageMimeType = if (currentImageUri != null) currentImageMime else null
        )

        _inputText.value = ""
        clearSelectedImage()
        _retryableQuery.value = null
        _isLoading.value = true

        viewModelScope.launch {
            repository.saveMessage(userMessageEntity)

            val currentHistory = messages.value
            val currentProf = userProfile.value

            val result = repository.sendTriageQuery(
                userMessage = formattedMessage,
                recentHistory = currentHistory,
                userProfile = currentProf,
                imageBase64 = currentImageBase64,
                imageMimeType = currentImageMime
            )

            result.onSuccess { responseText ->
                _retryableQuery.value = null
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
            }.onFailure { _ ->
                _retryableQuery.value = formattedMessage to symptomDetails
                val fallbackMessage = ChatMessageEntity(
                    sender = "AI",
                    text = trLang(
                        _selectedLanguage.value,
                        "⏳ Internet connection appears slow or unstable, so no guessed answer was generated. Please check your network and tap 'Retry' below.",
                        "⏳ ઇન્ટરનેટ ખૂબ ધીમું અથવા બંધ હોવાથી AI જવાબ મેળવી શકાયો નથી (ખોટો અંદાજિત જવાબ આપ્યો નથી). કૃપા કરીને નેટવર્ક ચકાસી નીચે 'ફરી પ્રયાસ કરો (Retry)' દબાવો.",
                        "⏳ इंटरनेट धीमा या बंद होने के कारण सही AI उत्तर नहीं मिल सका (गलत अनुमानित जवाब नहीं दिया गया है)। कृपया नीचे 'पुनः प्रयास करें (Retry)' दबाएं।"
                    ),
                    isEmergencyAlert = false
                )
                repository.saveMessage(fallbackMessage)
            }

            _isLoading.value = false
        }
    }

    fun retryLastQuery() {
        val pending = _retryableQuery.value ?: return
        if (_isLoading.value) return
        _retryableQuery.value = null
        sendMessage(overrideText = pending.first, symptomDetails = pending.second)
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
            val now = System.currentTimeMillis()
            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
            val log = SymptomLogEntity(
                timestamp = now,
                symptomTitle = title.trim(),
                severity = severity.coerceIn(1, 10),
                bodyLocation = location,
                duration = duration,
                notesOrTriggers = notes.trim(),
                status = status,
                dateKey = dateKey
            )
            val symptomId = repository.saveSymptomLog(log)
            if (notes.isNotBlank() && symptomId > 0L) {
                repository.addTimestampedSymptomNote(
                    symptomLogId = symptomId,
                    noteText = notes.trim(),
                    severitySnapshot = severity.coerceIn(1, 10),
                    timestamp = now
                )
            }
        }
    }

    fun addTimestampedSymptomNote(
        symptomLogId: Long,
        noteText: String,
        severitySnapshot: Int
    ) {
        if (noteText.isBlank()) return
        viewModelScope.launch {
            repository.addTimestampedSymptomNote(
                symptomLogId = symptomLogId,
                noteText = noteText.trim(),
                severitySnapshot = severitySnapshot.coerceIn(1, 10)
            )
        }
    }

    fun deleteSymptomNote(note: com.example.data.local.SymptomNoteEntity) {
        viewModelScope.launch {
            repository.deleteSymptomNote(note)
        }
    }

    fun emitDeveloperLog(level: String, tag: String, message: String) {
        when (level.uppercase()) {
            "INFO" -> com.example.utils.DevLogger.i(tag, message)
            "WARN" -> com.example.utils.DevLogger.w(tag, message)
            "ERROR" -> com.example.utils.DevLogger.e(tag, message)
            else -> com.example.utils.DevLogger.d(tag, message)
        }
    }

    fun clearDeveloperLogs() {
        com.example.utils.DevLogger.clear()
        viewModelScope.launch {
            repository.clearAllDeveloperLogs()
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

    fun boostCallAudioVolumeIfTooLow() {
        try {
            val am = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val targetVol = (maxVol * 0.85f).toInt().coerceIn(1, maxVol)
            if (currVol < (maxVol * 0.65f).toInt()) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            }
        } catch (_: Exception) {}
    }

    private fun selectBestVoiceForLocale(tts: TextToSpeech, targetLocale: Locale) {
        try {
            val installedVoices = tts.voices ?: return
            val matching = installedVoices.filter { voice ->
                voice.locale != null &&
                    voice.locale.language.equals(targetLocale.language, ignoreCase = true) &&
                    !voice.features.orEmpty().contains("notInstalled")
            }
            val best = matching
                .sortedWith(
                    compareByDescending<Voice> { it.locale?.country.equals(targetLocale.country, ignoreCase = true) }
                        .thenByDescending { it.quality }
                        .thenBy { it.isNetworkConnectionRequired } // Prefer crystal-clear fast local neural voices first, else best quality
                        .thenByDescending { it.name.contains("local", ignoreCase = true) || it.name.contains("neural", ignoreCase = true) }
                )
                .firstOrNull()
            if (best != null) {
                tts.voice = best
            }
        } catch (_: Exception) {}
    }

    fun speakText(text: String, messageId: Long? = null) {
        stopSpeaking()
        initTextToSpeech(getApplication())

        val cleaned = cleanTextForSpeech(text)
        if (cleaned.isBlank()) return

        val tts = textToSpeech
        if (tts == null || !_isTtsReady.value) {
            pendingSpeechRequest = text to messageId
            _speakingMessageId.value = null
            _isSpeaking.value = false
            return
        }

        val locale = detectLocaleForText(cleaned)
        var resolvedLocale = locale
        try {
            val avail = tts.isLanguageAvailable(locale)
            if (avail >= TextToSpeech.LANG_AVAILABLE) {
                tts.language = locale
            } else {
                val hiLocale = Locale.forLanguageTag("hi-IN")
                val hiAvail = tts.isLanguageAvailable(hiLocale)
                if (hiAvail >= TextToSpeech.LANG_AVAILABLE) {
                    tts.language = hiLocale
                    resolvedLocale = hiLocale
                } else {
                    resolvedLocale = Locale.getDefault()
                    tts.language = resolvedLocale
                }
            }
            selectBestVoiceForLocale(tts, resolvedLocale)
            tts.setSpeechRate(if (resolvedLocale.language == "en") 0.98f else 0.94f)
            tts.setPitch(1.04f)
            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        } catch (_: Exception) {
            try {
                tts.language = Locale.getDefault()
            } catch (_: Exception) {}
        }

        _speakingMessageId.value = messageId
        val utteranceId = "msg_${messageId ?: System.currentTimeMillis()}"
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        try {
            val result = tts.speak(cleaned, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (result == TextToSpeech.SUCCESS) {
                _isSpeaking.value = true
            } else {
                _speakingMessageId.value = null
                _isSpeaking.value = false
            }
        } catch (_: Exception) {
            _speakingMessageId.value = null
            _isSpeaking.value = false
        }
    }

    fun explainRemedyAloud(message: ChatMessageEntity) {
        val remedyText = extractRemedyForSpeech(message.text)
        speakText(remedyText, message.id)
    }

    private fun detectLocaleForText(text: String): Locale {
        val currentInfo = com.example.ui.theme.AppLanguages.findByName(_selectedLanguage.value)
        fun pickIfAvailable(tag: String, fallbackTag: String = "en-IN"): Locale {
            val candidate = Locale.forLanguageTag(tag)
            val available = try {
                textToSpeech?.isLanguageAvailable(candidate)
            } catch (_: Exception) {
                null
            }
            return if (available != null && available >= TextToSpeech.LANG_AVAILABLE) {
                candidate
            } else {
                Locale.forLanguageTag(fallbackTag)
            }
        }

        return when {
            text.any { it in '\u0A80'..'\u0AFF' } -> pickIfAvailable("gu-IN", "hi-IN")
            text.any { it in '\u0900'..'\u097F' } -> {
                if (currentInfo.id == "Marathi") pickIfAvailable("mr-IN", "hi-IN")
                else Locale.forLanguageTag("hi-IN")
            }
            text.any { it in '\u0980'..'\u09FF' } -> {
                if (currentInfo.id == "Assamese") pickIfAvailable("as-IN", "bn-IN")
                else pickIfAvailable("bn-IN", "hi-IN")
            }
            text.any { it in '\u0B80'..'\u0BFF' } -> pickIfAvailable("ta-IN")
            text.any { it in '\u0C00'..'\u0C7F' } -> pickIfAvailable("te-IN")
            text.any { it in '\u0C80'..'\u0CFF' } -> pickIfAvailable("kn-IN")
            text.any { it in '\u0D00'..'\u0D7F' } -> pickIfAvailable("ml-IN")
            text.any { it in '\u0A00'..'\u0A7F' } -> pickIfAvailable("pa-IN", "hi-IN")
            text.any { it in '\u0B00'..'\u0B7F' } -> pickIfAvailable("or-IN", "hi-IN")
            text.any { it in '\u0600'..'\u06FF' } -> {
                if (currentInfo.id == "Urdu") pickIfAvailable("ur-IN", "ar-SA")
                else pickIfAvailable("ar-SA")
            }
            text.any { it in '\u0400'..'\u04FF' } -> pickIfAvailable("ru-RU")
            text.any { it in '\u3040'..'\u30FF' } -> pickIfAvailable("ja-JP")
            text.any { it in '\u4E00'..'\u9FFF' } -> {
                if (currentInfo.id == "Japanese") pickIfAvailable("ja-JP")
                else pickIfAvailable("zh-CN")
            }
            else -> Locale.forLanguageTag(currentInfo.speechTag)
        }
    }

    private fun cleanTextForSpeech(text: String): String {
        return text
            .replace(Regex("[#*`_~|]"), " ")
            .replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), " ")
            .replace("⚠", " ")
            .replace("⏳", " ")
            .replace("🩺", " ")
            .replace("•", ", ")
            .replace("- ", ", ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractRemedyForSpeech(fullText: String): String {
        return cleanTextForSpeech(fullText)
    }

    fun stopSpeaking() {
        pendingSpeechRequest = null
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
        _speakingMessageId.value = null
    }

    // -------------------------------------------------------------
    // WATER REMINDER & HYDRATION TRACKING (પાણી રિમાઇન્ડર)
    // -------------------------------------------------------------
    val todayWaterLogs: StateFlow<List<WaterLogEntity>> =
        repository.getWaterLogsForDate(todayDateString).safeStateIn(emptyList())

    private val _todayTotalWater = MutableStateFlow(0)
    val todayTotalWaterMl: StateFlow<Int> = _todayTotalWater.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.getTotalWaterForDate(todayDateString)
                    .catch { emit(0) }
                    .collect { total ->
                        _todayTotalWater.value = total ?: 0
                    }
            } catch (_: Throwable) {
                _todayTotalWater.value = 0
            }
        }
    }

    private val _waterTargetMl = MutableStateFlow(2500)
    val waterTargetMl: StateFlow<Int> = _waterTargetMl.asStateFlow()

    private val _reminderIntervalHours = MutableStateFlow(
        runCatching { WaterReminderNotificationHelper.getSavedIntervalHours(application) }.getOrDefault(2)
    )
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
        WaterReminderNotificationHelper.scheduleWaterReminder(getApplication(), hours)
    }

    private val _oneSignalConfig = MutableStateFlow(
        runCatching { com.example.utils.OneSignalNotificationService.loadConfig(getApplication()) }
            .getOrElse { com.example.utils.OneSignalConfig(appId = "", restApiKey = "") }
    )
    val oneSignalConfig: StateFlow<com.example.utils.OneSignalConfig> = _oneSignalConfig.asStateFlow()

    val pendingInAppAlert: StateFlow<com.example.utils.InAppFallbackAlert?> =
        com.example.utils.OneSignalNotificationService.pendingInAppAlert

    val oneSignalLastStatus: StateFlow<String?> =
        com.example.utils.OneSignalNotificationService.lastSyncStatus

    fun dismissInAppAlert() {
        com.example.utils.OneSignalNotificationService.dismissInAppAlert()
    }

    fun saveOneSignalSettings(
        appId: String,
        restApiKey: String,
        targetExternalUserId: String = "",
        enabled: Boolean = true
    ) {
        val updated = com.example.utils.OneSignalNotificationService.saveConfig(
            context = getApplication(),
            appId = appId,
            restApiKey = restApiKey,
            targetExternalUserId = targetExternalUserId,
            enabled = enabled
        )
        _oneSignalConfig.value = updated
    }

    fun sendOneSignalTestScheduledPush(
        delayMinutes: Int = 1,
        onResult: (String) -> Unit = {}
    ) {
        val app = getApplication<Application>()
        val lang = _selectedLanguage.value
        val scheduledAt = System.currentTimeMillis() + delayMinutes.coerceAtLeast(1) * 60_000L
        val isoTime = com.example.utils.OneSignalNotificationService.formatIso8601Utc(scheduledAt)
        val title = trLang(
            lang,
            "🔔 Health Care AI Scheduled Reminder",
            "🔔 Health Care AI શેડ્યૂલ રિમાઇન્ડર",
            "🔔 Health Care AI शेड्यूल रिमाइंडर"
        )
        val body = trLang(
            lang,
            "Background push scheduled via OneSignal for $isoTime.",
            "OneSignal દ્વારા $isoTime માટે બેકગ્રાઉન્ડ રિમાઇન્ડર સેટ થયું છે.",
            "OneSignal द्वारा $isoTime के लिए बैकग्राउंड रिमाइंडर सेट किया गया है।"
        )
        viewModelScope.launch {
            val res = com.example.utils.OneSignalNotificationService.scheduleOrSendPush(
                context = app,
                title = title,
                message = body,
                scheduledAtMillis = scheduledAt,
                extraData = mapOf("reminder_type" to "test_scheduled", "screen" to "PROFILE"),
                showInAppFallbackWhenUnconfigured = true
            )
            val statusText = when (res) {
                is com.example.utils.OneSignalScheduleResult.Scheduled ->
                    trLang(
                        lang,
                        "OneSignal push scheduled for ${res.scheduledIsoTime ?: isoTime}!",
                        "OneSignal પુશ નોટિફિકેશન ${res.scheduledIsoTime ?: isoTime} માટે શેડ્યૂલ થયું!",
                        "OneSignal पुश नोटिफिकेशन ${res.scheduledIsoTime ?: isoTime} के लिए शेड्यूल हुआ!"
                    )
                is com.example.utils.OneSignalScheduleResult.OfflineFallback ->
                    trLang(
                        lang,
                        "Offline detected — in-app fallback alert shown.",
                        "ઓફલાઇન મોડ — ઇન-એપ એલર્ટ બતાવવામાં આવ્યું છે.",
                        "ऑफ़लाइन मोड — इन-ऐप अलर्ट दिखाया गया है।"
                    )
                is com.example.utils.OneSignalScheduleResult.NotConfiguredFallback ->
                    trLang(
                        lang,
                        "Enter ONESIGNAL_APP_ID & ONESIGNAL_REST_API_KEY to enable cloud push (in-app alert shown).",
                        "ક્લાઉડ પુશ માટે ONESIGNAL_APP_ID અને ONESIGNAL_REST_API_KEY દાખલ કરો.",
                        "क्लाउड पुश के लिए ONESIGNAL_APP_ID और ONESIGNAL_REST_API_KEY दर्ज करें।"
                    )
                is com.example.utils.OneSignalScheduleResult.ErrorFallback ->
                    trLang(
                        lang,
                        "OneSignal fallback alert shown (${res.errorMessage}).",
                        "OneSignal ફોલબેક એલર્ટ બતાવ્યું (${res.errorMessage}).",
                        "OneSignal फ़ॉलबैक अलर्ट दिखाया गया (${res.errorMessage})।"
                    )
            }
            onResult(statusText)
        }
    }

    fun triggerWaterReminderNotification() {
        val app = getApplication<Application>()
        val lang = _selectedLanguage.value
        val title = trLang(
            lang,
            "💧 Time to Drink Water!",
            "💧 પાણી પીવાનો સમય થયો છે!",
            "💧 पानी पीने का समय हो गया है!"
        )
        val msg = trLang(
            lang,
            "Stay hydrated! Drinking water boosts energy, improves digestion, and prevents headaches. Take a glass of water now!",
            "હાઇડ્રેટેડ રહો! પાણી પીવાથી ઊર્જા વધે છે, પાચન સુધરે છે અને માથાનો દુખાવો અટકે છે. અત્યારે એક ગ્લાસ પાણી પીઓ!",
            "हाइड्रेटेड रहें! पानी पीने से ऊर्जा बढ़ती है, पाचन सुधरता है और सिरदर्द से बचाव होता है। अभी एक गिलास पानी पिएं!"
        )
        WaterReminderNotificationHelper.showWaterReminderNotification(
            app,
            title = title,
            message = msg
        )
        com.example.utils.OneSignalNotificationService.schedulePushAsync(
            context = app,
            title = title,
            message = msg,
            scheduledAtMillis = null,
            extraData = mapOf("reminder_type" to "water_immediate", "screen" to "WATER_REMINDER")
        )
    }

    fun triggerMedicineTestNotification() {
        val app = getApplication<Application>()
        val lang = _selectedLanguage.value
        val firstMed = medicines.value.firstOrNull()
        val name = firstMed?.name ?: "Paracetamol 500mg"
        val dosage = firstMed?.dosage ?: trLang(lang, "1 Tablet", "૧ ગોળી", "1 गोली")
        val instruction = firstMed?.instruction ?: trLang(lang, "After food", "જમ્યા પછી", "खाने के बाद")
        val timeOfDay = firstMed?.timeOfDay ?: "08:00 AM"
        com.example.utils.MedicineReminderWorker.showImmediateNotification(
            context = app,
            medId = firstMed?.id ?: 0L,
            medName = name,
            dosage = dosage,
            instruction = instruction,
            timeOfDay = timeOfDay
        )
        com.example.utils.OneSignalNotificationService.schedulePushAsync(
            context = app,
            title = "💊 Medication Reminder: $name",
            message = "$dosage • $instruction ($timeOfDay)",
            scheduledAtMillis = null,
            extraData = mapOf("reminder_type" to "medicine_immediate", "screen" to "MEDICINE_REMINDER")
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
        language: String,
        pmjayCardNumber: String = "",
        abhaCardNumber: String = ""
    ) {
        val cleanPmjay = com.example.utils.HealthCardValidator.sanitizePmjayInput(pmjayCardNumber)
        val cleanAbha = com.example.utils.HealthCardValidator.sanitizeAbhaInput(abhaCardNumber)
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
                preferredLanguage = language,
                pmjayCardNumber = cleanPmjay,
                abhaCardNumber = cleanAbha
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

    fun exportDoctorReportPdf(context: Context): String? {
        val pdf = com.example.utils.DoctorReportPdfGenerator.createDoctorReportPdf(
            context = context,
            profile = userProfile.value,
            symptomLogs = symptomLogs.value,
            triageSessions = sessions.value
        )
        return com.example.utils.DoctorReportPdfGenerator.savePdfToDownloads(context, pdf)
    }

    fun shareDoctorReportPdf(context: Context) {
        val pdf = com.example.utils.DoctorReportPdfGenerator.createDoctorReportPdf(
            context = context,
            profile = userProfile.value,
            symptomLogs = symptomLogs.value,
            triageSessions = sessions.value
        )
        com.example.utils.DoctorReportPdfGenerator.shareDoctorReportPdf(context, pdf)
    }

    fun generateClinicalDataExport(format: com.example.utils.ExportFormat): String {
        val profile = userProfile.value
        val medList = medicines.value
        val adherence = allAdherenceLogs.value
        val symptoms = symptomLogs.value
        val notes = symptomNotes.value
        return when (format) {
            com.example.utils.ExportFormat.CSV ->
                com.example.utils.HealthDataExportHelper.buildClinicalExportCsv(
                    profile = profile,
                    medicines = medList,
                    adherenceLogs = adherence,
                    symptomLogs = symptoms,
                    symptomNotes = notes
                )
            com.example.utils.ExportFormat.JSON ->
                com.example.utils.HealthDataExportHelper.buildClinicalExportJson(
                    profile = profile,
                    medicines = medList,
                    adherenceLogs = adherence,
                    symptomLogs = symptoms,
                    symptomNotes = notes
                )
        }
    }

    fun saveClinicalDataExport(
        context: Context,
        format: com.example.utils.ExportFormat
    ): String? {
        val content = generateClinicalDataExport(format)
        return com.example.utils.HealthDataExportHelper.saveExportFileToDownloads(
            context = context,
            format = format,
            content = content
        )
    }

    fun shareClinicalDataExport(
        context: Context,
        format: com.example.utils.ExportFormat
    ): Boolean {
        val content = generateClinicalDataExport(format)
        return com.example.utils.HealthDataExportHelper.shareExportFile(
            context = context,
            format = format,
            content = content,
            patientName = userProfile.value?.fullName
        )
    }

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val firebaseBackupRepo by lazy {
        com.example.data.firebase.FirebaseBackupRepository(getApplication())
    }

    fun backupToCloud(onComplete: (Boolean, String) -> Unit) {
        _isBackingUp.value = true
        val lang = _selectedLanguage.value
        viewModelScope.launch {
            val prof = userProfile.value
            val logs = symptomLogs.value
            val steps = weeklyStepRecords.value
            val result = firebaseBackupRepo.backupHealthDataToFirestore(prof, logs, steps)
            _isBackingUp.value = false
            result.onSuccess { count ->
                onComplete(
                    true,
                    trLang(
                        lang,
                        "Successfully backed up profile, symptoms, and fitness records ($count items) to Firebase Firestore!",
                        "પ્રોફાઇલ, લક્ષણો અને ફિટનેસ રેકોર્ડ ($count વિગતો) સફળતાપૂર્વક Firebase Firestore માં બેકઅપ થઈ ગયા છે!",
                        "प्रोफ़ाइल, लक्षण और फ़िटनेस रिकॉर्ड ($count आइटम) सफलतापूर्वक Firebase Firestore में बैकअप हो गए हैं!"
                    )
                )
            }.onFailure { err ->
                onComplete(
                    false,
                    trLang(
                        lang,
                        "Backup failed: ${err.localizedMessage ?: "Unknown error"}",
                        "બેકઅપ નિષ્ફળ ગયું: ${err.localizedMessage ?: "અજ્ઞાત ભૂલ"}",
                        "बैकअप विफल रहा: ${err.localizedMessage ?: "अज्ञात त्रुटि"}"
                    )
                )
            }
        }
    }

    fun triggerEmergencySosWithLocation(context: Context, onResult: (Boolean, String) -> Unit) {
        _emergencyAlertTriggered.value = true
        val lang = _selectedLanguage.value
        viewModelScope.launch {
            val prof = userProfile.value
            val contactPhone = prof?.emergencyContactPhone?.ifBlank { "112" } ?: "112"
            val patientName = prof?.fullName ?: trLang(lang, "Patient", "દર્દી", "मरीज़")

            // 1. Fetch live GPS coordinates
            val coords = com.example.utils.EmergencySosLocationHelper.getCurrentLocation(context)
            val mapsLink = coords?.let { (lat, lon) ->
                com.example.utils.EmergencySosLocationHelper.buildGoogleMapsLink(lat, lon)
            }

            // 2. Dispatch SMS
            val sent = com.example.utils.EmergencySosLocationHelper.sendSosSms(
                context = context,
                phoneNumber = contactPhone,
                patientName = patientName,
                mapsLink = mapsLink
            )

            if (sent) {
                onResult(
                    true,
                    trLang(
                        lang,
                        "🚨 SOS SMS with GPS location sent to $contactPhone!",
                        "🚨 GPS લોકેશન સાથે SOS SMS $contactPhone પર મોકલી દેવાયો છે!",
                        "🚨 GPS लोकेशन के साथ SOS SMS $contactPhone पर भेज दिया गया है!"
                    )
                )
            } else {
                onResult(
                    false,
                    trLang(
                        lang,
                        "SOS SMS initiated for $contactPhone",
                        "$contactPhone માટે SOS SMS શરૂ કરવામાં આવ્યો છે",
                        "$contactPhone के लिए SOS SMS शुरू किया गया है"
                    )
                )
            }
        }
    }

    fun addMedicine(
        name: String,
        dosage: String,
        timeOfDay: String,
        instruction: String,
        frequencyHours: Int = 24,
        prescribedBy: String = ""
    ) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val medEntity = com.example.data.local.MedicineEntity(
                name = name.trim(),
                dosage = dosage.trim().ifBlank { "1 Tablet" },
                timeOfDay = timeOfDay,
                instruction = instruction,
                frequencyHours = frequencyHours.coerceIn(1, 24),
                prescribedBy = prescribedBy.trim(),
                isActive = true
            )
            val insertedId = repository.saveMedicine(medEntity)
            com.example.utils.MedicineReminderWorker.scheduleDailyMedicineReminder(
                context = app,
                medId = insertedId,
                medName = medEntity.name,
                dosage = medEntity.dosage,
                instruction = medEntity.instruction,
                timeOfDay = medEntity.timeOfDay,
                frequencyHours = medEntity.frequencyHours
            )
        }
    }

    fun toggleMedicineActive(medicine: com.example.data.local.MedicineEntity) {
        val app = getApplication<Application>()
        val updated = medicine.copy(isActive = !medicine.isActive)
        viewModelScope.launch {
            repository.updateMedicine(updated)
            if (updated.isActive) {
                com.example.utils.MedicineReminderWorker.scheduleDailyMedicineReminder(
                    context = app,
                    medId = updated.id,
                    medName = updated.name,
                    dosage = updated.dosage,
                    instruction = updated.instruction,
                    timeOfDay = updated.timeOfDay,
                    frequencyHours = updated.frequencyHours
                )
            } else {
                com.example.utils.MedicineReminderWorker.cancelMedicineReminder(app, updated.id)
            }
        }
    }

    fun updateMedicineTimeAndDetails(
        medicine: com.example.data.local.MedicineEntity,
        newTime: String,
        newName: String? = null,
        newDosage: String? = null,
        newInstruction: String? = null,
        newFrequencyHours: Int? = null,
        newPrescribedBy: String? = null
    ) {
        val app = getApplication<Application>()
        val updated = medicine.copy(
            name = (newName ?: medicine.name).trim(),
            dosage = (newDosage ?: medicine.dosage).trim(),
            timeOfDay = newTime.trim(),
            instruction = (newInstruction ?: medicine.instruction).trim(),
            frequencyHours = (newFrequencyHours ?: medicine.frequencyHours).coerceIn(1, 24),
            prescribedBy = (newPrescribedBy ?: medicine.prescribedBy).trim()
        )
        viewModelScope.launch {
            repository.updateMedicine(updated)
            if (updated.isActive) {
                com.example.utils.MedicineReminderWorker.scheduleDailyMedicineReminder(
                    context = app,
                    medId = updated.id,
                    medName = updated.name,
                    dosage = updated.dosage,
                    instruction = updated.instruction,
                    timeOfDay = updated.timeOfDay,
                    frequencyHours = updated.frequencyHours
                )
            } else {
                com.example.utils.MedicineReminderWorker.cancelMedicineReminder(app, updated.id)
            }
        }
    }

    fun deleteMedicine(medicine: com.example.data.local.MedicineEntity) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            repository.deleteMedicine(medicine)
            com.example.utils.MedicineReminderWorker.cancelMedicineReminder(app, medicine.id)
        }
    }

    fun recordMedicineAdherence(
        medicine: com.example.data.local.MedicineEntity,
        status: String,
        notes: String = ""
    ) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.recordMedicationAdherence(
                medicine = medicine,
                status = status,
                dateString = today,
                notes = notes
            )
            when (status) {
                "TAKEN", "SKIPPED" -> {
                    com.example.utils.MedicineReminderWorker.cancelSnoozeReminder(app, medicine.id)
                    val nm = app.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                    nm?.cancel((3000 + medicine.id).toInt())
                }
                "SNOOZED" -> {
                    com.example.utils.MedicineReminderWorker.scheduleSnoozeReminder(
                        context = app,
                        medId = medicine.id,
                        medName = medicine.name,
                        dosage = medicine.dosage,
                        instruction = medicine.instruction,
                        timeOfDay = medicine.timeOfDay,
                        snoozeMinutes = 15
                    )
                }
            }
        }
    }

    fun deleteAdherenceLog(log: com.example.data.local.MedicationAdherenceEntity) {
        viewModelScope.launch {
            repository.deleteAdherenceLog(log)
        }
    }

    fun clearAllAdherenceHistory() {
        viewModelScope.launch {
            repository.clearAllAdherenceLogs()
        }
    }

    // Step Counter & Fitness Actions
    fun onSensorStepDetected(delta: Int) {
        viewModelScope.launch {
            repository.addSteps(todayDateString, delta)
        }
    }

    fun addManualSteps(amount: Int) {
        viewModelScope.launch {
            repository.addSteps(todayDateString, amount)
        }
    }

    fun setDailyStepGoal(newGoal: Int) {
        viewModelScope.launch {
            repository.updateStepGoal(todayDateString, newGoal)
        }
    }

    fun resetDailySteps() {
        viewModelScope.launch {
            repository.resetSteps(todayDateString)
        }
    }

    fun toggleStepTracking() {
        val nextState = !_isStepTrackingActive.value
        _isStepTrackingActive.value = nextState
        if (nextState) {
            stepSensorTracker?.startTracking()
        } else {
            stepSensorTracker?.stopTracking()
        }
    }

    // Smart Health Vitals Management
    fun recordVitalLog(
        type: String,
        primaryValue: Double,
        secondaryValue: Double = 0.0,
        unit: String,
        status: String,
        notes: String = "",
        familyMemberName: String = "Self"
    ) {
        viewModelScope.launch {
            val log = com.example.data.local.VitalLogEntity(
                vitalType = type,
                primaryValue = primaryValue,
                secondaryValue = secondaryValue,
                unit = unit,
                status = status,
                notes = notes,
                familyMemberName = familyMemberName
            )
            repository.saveVitalLog(log)
        }
    }

    fun deleteVitalLog(log: com.example.data.local.VitalLogEntity) {
        viewModelScope.launch {
            repository.deleteVitalLog(log)
        }
    }

    // Family Profile & Caregiver Alert System
    fun addFamilyMember(
        name: String,
        relationship: String,
        age: Int,
        bloodGroup: String,
        phone: String,
        conditions: String = "None"
    ) {
        viewModelScope.launch {
            val member = com.example.data.local.FamilyMemberEntity(
                name = name.trim(),
                relationship = relationship,
                age = age,
                bloodGroup = bloodGroup,
                phone = phone.trim(),
                caregiverAlertEnabled = true,
                chronicConditions = conditions.trim()
            )
            repository.saveFamilyMember(member)
        }
    }

    fun deleteFamilyMember(member: com.example.data.local.FamilyMemberEntity) {
        viewModelScope.launch {
            repository.deleteFamilyMember(member)
        }
    }

    fun triggerCaregiverEmergencyAlert(
        context: android.content.Context,
        elderName: String,
        medName: String,
        caregiverPhone: String
    ) {
        val message = "🚨 Caregiver Alert from Health Care AI: $elderName has missed scheduled medication: $medName. Please check in with them immediately."
        com.example.utils.EmergencySosLocationHelper.sendSosSms(
            context = context,
            phoneNumber = caregiverPhone,
            patientName = elderName,
            mapsLink = null
        )
    }

    // -------------------------------------------------------------------------
    // 1. Personalized Treatment Plans
    // -------------------------------------------------------------------------
    private val _isGeneratingTreatmentPlan = MutableStateFlow(false)
    val isGeneratingTreatmentPlan: StateFlow<Boolean> = _isGeneratingTreatmentPlan.asStateFlow()

    private val _activeTreatmentPlan = MutableStateFlow<String?>(null)
    val activeTreatmentPlan: StateFlow<String?> = _activeTreatmentPlan.asStateFlow()

    private val _treatmentPlanIsAi = MutableStateFlow(false)
    val treatmentPlanIsAi: StateFlow<Boolean> = _treatmentPlanIsAi.asStateFlow()

    fun generatePersonalizedTreatmentPlan(
        targetGoal: String,
        geneticMarkers: String,
        lifestyleSummary: String,
        labResultsSummary: String
    ) {
        if (_isGeneratingTreatmentPlan.value) return
        _isGeneratingTreatmentPlan.value = true
        viewModelScope.launch {
            try {
                val (planText, aiPowered) = repository.generatePersonalizedTreatmentPlan(
                    profile = userProfile.value,
                    targetGoal = targetGoal.ifBlank { "General Wellness & Preventive Care" },
                    geneticsMarkers = geneticMarkers.ifBlank { "None specified" },
                    lifestyleSummary = lifestyleSummary.ifBlank { "Balanced daily routine" },
                    labResultsSummary = labResultsSummary.ifBlank { "No custom labs entered" },
                    vitalLogs = vitalLogs.value,
                    language = _selectedLanguage.value
                )
                _activeTreatmentPlan.value = planText
                _treatmentPlanIsAi.value = aiPowered

                repository.saveTreatmentPlan(
                    com.example.data.local.TreatmentPlanEntity(
                        targetGoal = targetGoal.ifBlank { "General Wellness" },
                        geneticMarkers = geneticMarkers.ifBlank { "None" },
                        lifestyleSummary = lifestyleSummary,
                        labResultsSummary = labResultsSummary,
                        planContent = planText,
                        isAiPowered = aiPowered
                    )
                )
            } finally {
                _isGeneratingTreatmentPlan.value = false
            }
        }
    }

    fun deleteTreatmentPlan(plan: com.example.data.local.TreatmentPlanEntity) {
        viewModelScope.launch {
            repository.deleteTreatmentPlan(plan)
        }
    }

    // -------------------------------------------------------------------------
    // 2. Predictive Analytics & Early Disease Detection
    // -------------------------------------------------------------------------
    val predictiveRiskSnapshot: StateFlow<com.example.utils.PredictiveRiskSnapshot> = combine(
        userProfile,
        vitalLogs,
        symptomLogs,
        weeklyStepRecords,
        scannedReports,
        selectedLanguage
    ) { arr ->
        val prof = arr[0] as? UserProfileEntity
        @Suppress("UNCHECKED_CAST")
        val vitals = arr[1] as List<com.example.data.local.VitalLogEntity>
        @Suppress("UNCHECKED_CAST")
        val symptoms = arr[2] as List<SymptomLogEntity>
        @Suppress("UNCHECKED_CAST")
        val steps = arr[3] as List<com.example.data.local.StepRecordEntity>
        @Suppress("UNCHECKED_CAST")
        val reports = arr[4] as List<com.example.data.local.ScannedReportEntity>
        val lang = arr[5] as String

        com.example.utils.ClinicalRiskEngine.computePredictiveRisks(
            profile = prof,
            vitalLogs = vitals,
            symptomLogs = symptoms,
            stepRecords = steps,
            scannedReports = reports,
            language = lang
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.example.utils.ClinicalRiskEngine.computePredictiveRisks(
            profile = null,
            vitalLogs = emptyList(),
            symptomLogs = emptyList(),
            stepRecords = emptyList(),
            scannedReports = emptyList(),
            language = "English"
        )
    )

    private val _isGeneratingRiskForecast = MutableStateFlow(false)
    val isGeneratingRiskForecast: StateFlow<Boolean> = _isGeneratingRiskForecast.asStateFlow()

    private val _activeRiskForecast = MutableStateFlow<String?>(null)
    val activeRiskForecast: StateFlow<String?> = _activeRiskForecast.asStateFlow()

    fun generatePredictiveRiskForecast() {
        if (_isGeneratingRiskForecast.value) return
        _isGeneratingRiskForecast.value = true
        viewModelScope.launch {
            try {
                val snapshot = predictiveRiskSnapshot.value
                val (forecastText, _) = repository.generatePredictiveRiskForecast(
                    profile = userProfile.value,
                    riskSnapshot = snapshot,
                    vitalLogs = vitalLogs.value,
                    symptomLogs = symptomLogs.value,
                    language = _selectedLanguage.value
                )
                _activeRiskForecast.value = forecastText
                repository.saveRiskAssessment(
                    com.example.data.local.RiskAssessmentEntity(
                        heartRiskScore = snapshot.heartRiskScore,
                        sepsisRiskScore = snapshot.sepsisRiskScore,
                        metabolicRiskScore = snapshot.metabolicRiskScore,
                        overallRiskLevel = snapshot.overallRiskLevel,
                        contributingFactors = (snapshot.heartFactors + snapshot.sepsisFactors).distinct().joinToString(" • "),
                        aiForecast = forecastText
                    )
                )
            } finally {
                _isGeneratingRiskForecast.value = false
            }
        }
    }

    fun deleteRiskAssessment(assessment: com.example.data.local.RiskAssessmentEntity) {
        viewModelScope.launch {
            repository.deleteRiskAssessment(assessment)
        }
    }

    // -------------------------------------------------------------------------
    // 3. Automated Clinical Documentation (NLP Clinical Scribe)
    // -------------------------------------------------------------------------
    private val _isGeneratingClinicalDoc = MutableStateFlow(false)
    val isGeneratingClinicalDoc: StateFlow<Boolean> = _isGeneratingClinicalDoc.asStateFlow()

    private val _latestParsedClinicalDoc = MutableStateFlow<com.example.utils.ParsedClinicalDocument?>(null)
    val latestParsedClinicalDoc: StateFlow<com.example.utils.ParsedClinicalDocument?> = _latestParsedClinicalDoc.asStateFlow()

    private val _clinicalDocIsAi = MutableStateFlow(false)
    val clinicalDocIsAi: StateFlow<Boolean> = _clinicalDocIsAi.asStateFlow()

    fun generateClinicalDocumentation(patientName: String, conversationTranscript: String) {
        if (conversationTranscript.isBlank() || _isGeneratingClinicalDoc.value) return
        _isGeneratingClinicalDoc.value = true
        viewModelScope.launch {
            try {
                val cleanName = patientName.trim().ifBlank {
                    userProfile.value?.fullName ?: "Patient"
                }
                val (parsedDoc, aiPowered) = repository.generateClinicalDocumentation(
                    transcript = conversationTranscript,
                    patientName = cleanName,
                    profile = userProfile.value,
                    language = _selectedLanguage.value
                )
                _latestParsedClinicalDoc.value = parsedDoc
                _clinicalDocIsAi.value = aiPowered

                repository.saveClinicalNote(
                    com.example.data.local.ClinicalNoteEntity(
                        patientName = cleanName,
                        conversationTranscript = conversationTranscript.trim(),
                        soapNote = parsedDoc.soapNote,
                        medicalReport = parsedDoc.medicalReport,
                        prescriptionSummary = parsedDoc.prescriptionSummary
                    )
                )
            } finally {
                _isGeneratingClinicalDoc.value = false
            }
        }
    }

    fun deleteClinicalNote(note: com.example.data.local.ClinicalNoteEntity) {
        viewModelScope.launch {
            repository.deleteClinicalNote(note)
        }
    }

    fun addExtractedMedicinesToReminders(items: List<com.example.utils.ExtractedRxItem>) {
        items.forEach { rx ->
            addMedicine(
                name = rx.name,
                dosage = rx.dosage,
                timeOfDay = rx.timeOfDay,
                instruction = rx.instruction
            )
        }
    }

    // -------------------------------------------------------------------------
    // 4. Remote Patient Monitoring (BLE Smartwatches & Virtual Wards)
    // -------------------------------------------------------------------------
    private val bleWearableManager by lazy {
        com.example.utils.BleWearableManager(getApplication())
    }

    val bleWearableState: StateFlow<com.example.utils.BleWearableState>
        get() = bleWearableManager.state

    fun bleRequiredPermissions(): Array<String> = bleWearableManager.requiredPermissions()

    fun refreshBleStatus() {
        bleWearableManager.refreshHardwareStatus()
    }

    fun startWearableScan() {
        bleWearableManager.startScan()
    }

    fun stopWearableScan() {
        bleWearableManager.stopScan()
    }

    fun connectWearableDevice(address: String, name: String) {
        bleWearableManager.connectToDevice(address, name)
    }

    fun disconnectWearableDevice() {
        bleWearableManager.disconnect()
    }

    fun syncWearableVitalsToLog() {
        val ble = bleWearableState.value
        val deviceTag = ble.connectedDeviceName ?: "BLE Wearable"
        ble.liveHeartRateBpm?.let { bpm ->
            val status = when {
                bpm > 110 || bpm < 50 -> "Alert"
                bpm > 100 -> "Elevated"
                else -> "Normal"
            }
            recordVitalLog(
                type = "Heart Rate",
                primaryValue = bpm.toDouble(),
                unit = "bpm",
                status = status,
                notes = "Synced from $deviceTag"
            )
        }
        ble.liveSpO2Percent?.let { spo2 ->
            val status = when {
                spo2 < 90 -> "Critical"
                spo2 < 95 -> "Low"
                else -> "Normal"
            }
            recordVitalLog(
                type = "SpO2",
                primaryValue = spo2.toDouble(),
                unit = "%",
                status = status,
                notes = "Synced from $deviceTag"
            )
        }
    }

    // -------------------------------------------------------------------------
    // 5. Electronic Health Records (EHR) Automation
    // -------------------------------------------------------------------------
    private val _isOrganizingEhr = MutableStateFlow(false)
    val isOrganizingEhr: StateFlow<Boolean> = _isOrganizingEhr.asStateFlow()

    private val _activeEhrRecord = MutableStateFlow<com.example.data.local.EhrRecordEntity?>(null)
    val activeEhrRecord: StateFlow<com.example.data.local.EhrRecordEntity?> = _activeEhrRecord.asStateFlow()

    fun organizeAndSaveEhr(patientName: String, rawNotes: String) {
        if (rawNotes.isBlank() || _isOrganizingEhr.value) return
        _isOrganizingEhr.value = true
        viewModelScope.launch {
            try {
                val cleanName = patientName.trim().ifBlank { userProfile.value?.fullName ?: "Patient" }
                val lang = _selectedLanguage.value
                val parsed = com.example.utils.AdvancedClinicalEngines.parseAndOrganizeEhr(
                    rawNotes = rawNotes,
                    patientName = cleanName,
                    profile = userProfile.value,
                    language = lang
                )
                val (summary, _) = repository.generateAutomatedEhrSummary(
                    rawNotes = rawNotes,
                    patientName = cleanName,
                    profile = userProfile.value,
                    parsed = parsed,
                    language = lang
                )
                val record = com.example.data.local.EhrRecordEntity(
                    patientName = cleanName,
                    category = parsed.category,
                    rawNotes = rawNotes.trim(),
                    structuredSummary = summary,
                    diagnosisTags = parsed.diagnosisTags,
                    medicationsDetected = parsed.medicationsDetected
                )
                val id = repository.saveEhrRecord(record)
                _activeEhrRecord.value = record.copy(id = id)
            } finally {
                _isOrganizingEhr.value = false
            }
        }
    }

    fun deleteEhrRecord(record: com.example.data.local.EhrRecordEntity) {
        viewModelScope.launch {
            repository.deleteEhrRecord(record)
            if (_activeEhrRecord.value?.id == record.id) _activeEhrRecord.value = null
        }
    }

    // -------------------------------------------------------------------------
    // 6. Mental Health Support & Primary Counseling
    // -------------------------------------------------------------------------
    private val _isAnalyzingMentalHealth = MutableStateFlow(false)
    val isAnalyzingMentalHealth: StateFlow<Boolean> = _isAnalyzingMentalHealth.asStateFlow()

    private val _activeMentalHealthCheckIn = MutableStateFlow<com.example.data.local.MentalHealthCheckInEntity?>(null)
    val activeMentalHealthCheckIn: StateFlow<com.example.data.local.MentalHealthCheckInEntity?> = _activeMentalHealthCheckIn.asStateFlow()

    private val _activeMentalHealthCrisis = MutableStateFlow(false)
    val activeMentalHealthCrisis: StateFlow<Boolean> = _activeMentalHealthCrisis.asStateFlow()

    private fun readCompressedImageBase64(context: Context, uri: android.net.Uri): Pair<String, String>? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val rawBitmap = android.graphics.BitmapFactory.decodeStream(stream)
            stream.close()
            if (rawBitmap == null) return null

            val maxDim = 1024
            val scaled = if (rawBitmap.width > maxDim || rawBitmap.height > maxDim) {
                val ratio = maxDim.toFloat() / maxOf(rawBitmap.width, rawBitmap.height).toFloat()
                android.graphics.Bitmap.createScaledBitmap(
                    rawBitmap,
                    (rawBitmap.width * ratio).toInt().coerceAtLeast(1),
                    (rawBitmap.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                rawBitmap
            }
            val out = java.io.ByteArrayOutputStream()
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, out)
            val bytes = out.toByteArray()
            val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            b64 to "image/jpeg"
        } catch (_: Exception) {
            null
        }
    }

    fun analyzeAndSaveMentalHealth(
        context: Context,
        transcript: String,
        stressSlider: Int,
        sleepHours: Float,
        anxietyDays: Int,
        facialPhotoUri: android.net.Uri?
    ) {
        if (_isAnalyzingMentalHealth.value) return
        _isAnalyzingMentalHealth.value = true
        viewModelScope.launch {
            try {
                val lang = _selectedLanguage.value
                val imgData = if (facialPhotoUri != null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        readCompressedImageBase64(context, facialPhotoUri)
                    }
                } else null

                val screening = com.example.utils.AdvancedClinicalEngines.analyzeMentalHealthSignals(
                    transcript = transcript,
                    stressSlider = stressSlider,
                    sleepHours = sleepHours,
                    anxietyDays = anxietyDays,
                    hasFacialPhoto = imgData != null || facialPhotoUri != null,
                    language = lang
                )
                _activeMentalHealthCrisis.value = screening.crisisDetected || screening.stressScore >= 72

                val (guidance, _) = repository.generateMentalHealthCounseling(
                    transcript = transcript,
                    screening = screening,
                    stressSlider = stressSlider,
                    sleepHours = sleepHours,
                    anxietyDays = anxietyDays,
                    imageBase64 = imgData?.first,
                    imageMimeType = imgData?.second,
                    language = lang
                )
                val checkIn = com.example.data.local.MentalHealthCheckInEntity(
                    stressScore = screening.stressScore,
                    moodLabel = screening.moodLabel,
                    spokenTranscript = transcript.trim().ifBlank { "Mood slider: $stressSlider/10 • Sleep: ${sleepHours}h" },
                    hasFacialPhoto = facialPhotoUri != null,
                    counselingGuidance = guidance
                )
                val id = repository.saveMentalHealthCheckIn(checkIn)
                _activeMentalHealthCheckIn.value = checkIn.copy(id = id)
            } finally {
                _isAnalyzingMentalHealth.value = false
            }
        }
    }

    fun deleteMentalHealthCheckIn(checkIn: com.example.data.local.MentalHealthCheckInEntity) {
        viewModelScope.launch {
            repository.deleteMentalHealthCheckIn(checkIn)
            if (_activeMentalHealthCheckIn.value?.id == checkIn.id) _activeMentalHealthCheckIn.value = null
        }
    }

    // -------------------------------------------------------------------------
    // 7. Drug Discovery & Development Candidate Screening
    // -------------------------------------------------------------------------
    private val _isScreeningDrug = MutableStateFlow(false)
    val isScreeningDrug: StateFlow<Boolean> = _isScreeningDrug.asStateFlow()

    private val _activeDrugMetrics = MutableStateFlow<com.example.utils.MoleculeScreeningResult?>(null)
    val activeDrugMetrics: StateFlow<com.example.utils.MoleculeScreeningResult?> = _activeDrugMetrics.asStateFlow()

    private val _activeDrugReport = MutableStateFlow<String?>(null)
    val activeDrugReport: StateFlow<String?> = _activeDrugReport.asStateFlow()

    fun runDrugDiscoveryStudy(
        targetDisease: String,
        moleculeInput: String,
        priority: String
    ) {
        if (targetDisease.isBlank() || moleculeInput.isBlank() || _isScreeningDrug.value) return
        _isScreeningDrug.value = true
        viewModelScope.launch {
            try {
                val lang = _selectedLanguage.value
                val metrics = com.example.utils.AdvancedClinicalEngines.evaluateMolecularCandidate(
                    moleculeInput = moleculeInput,
                    targetDisease = targetDisease,
                    priority = priority,
                    language = lang
                )
                _activeDrugMetrics.value = metrics
                val (report, _) = repository.generateDrugDiscoveryAnalysis(
                    targetDisease = targetDisease,
                    moleculeInput = moleculeInput,
                    priority = priority,
                    screening = metrics,
                    language = lang
                )
                _activeDrugReport.value = report
                repository.saveDrugStudy(
                    com.example.data.local.DrugCandidateStudyEntity(
                        targetDisease = targetDisease.trim(),
                        moleculeInput = moleculeInput.trim(),
                        drugLikenessScore = metrics.drugLikenessScore,
                        analysisReport = report
                    )
                )
            } finally {
                _isScreeningDrug.value = false
            }
        }
    }

    fun deleteDrugStudy(study: com.example.data.local.DrugCandidateStudyEntity) {
        viewModelScope.launch {
            repository.deleteDrugStudy(study)
        }
    }

    // -------------------------------------------------------------------------
    // 8. Epidemic Outbreak Prediction & Early Alert
    // -------------------------------------------------------------------------
    private val _isPredictingOutbreak = MutableStateFlow(false)
    val isPredictingOutbreak: StateFlow<Boolean> = _isPredictingOutbreak.asStateFlow()

    private val _activeOutbreakResult = MutableStateFlow<com.example.utils.OutbreakPredictionResult?>(null)
    val activeOutbreakResult: StateFlow<com.example.utils.OutbreakPredictionResult?> = _activeOutbreakResult.asStateFlow()

    private val _activeOutbreakBrief = MutableStateFlow<String?>(null)
    val activeOutbreakBrief: StateFlow<String?> = _activeOutbreakBrief.asStateFlow()

    fun predictEpidemicOutbreak(
        regionName: String,
        tempCelsius: Int,
        humidityPercent: Int,
        travelDensity: String,
        hospitalLoadPercent: Int,
        newsSignals: String
    ) {
        if (_isPredictingOutbreak.value) return
        _isPredictingOutbreak.value = true
        viewModelScope.launch {
            try {
                val cleanRegion = regionName.trim().ifBlank { "Regional Health District" }
                val lang = _selectedLanguage.value
                val prediction = com.example.utils.AdvancedClinicalEngines.computeEpidemicOutbreakRisk(
                    regionName = cleanRegion,
                    tempCelsius = tempCelsius,
                    humidityPercent = humidityPercent,
                    travelDensity = travelDensity,
                    hospitalLoadPercent = hospitalLoadPercent,
                    newsSignals = newsSignals,
                    recentSymptoms = symptomLogs.value,
                    language = lang
                )
                _activeOutbreakResult.value = prediction
                val (brief, _) = repository.generateEpidemicAlertBrief(
                    regionName = cleanRegion,
                    tempCelsius = tempCelsius,
                    humidityPercent = humidityPercent,
                    travelDensity = travelDensity,
                    hospitalLoadPercent = hospitalLoadPercent,
                    newsSignals = newsSignals,
                    prediction = prediction,
                    language = lang
                )
                _activeOutbreakBrief.value = brief
                repository.saveOutbreakAlert(
                    com.example.data.local.OutbreakAlertEntity(
                        regionName = cleanRegion,
                        overallAlertTier = prediction.overallAlertTier,
                        vectorRiskScore = prediction.vectorRiskScore,
                        respiratoryRiskScore = prediction.respiratoryRiskScore,
                        alertBrief = brief
                    )
                )
            } finally {
                _isPredictingOutbreak.value = false
            }
        }
    }

    fun deleteOutbreakAlert(alert: com.example.data.local.OutbreakAlertEntity) {
        viewModelScope.launch {
            repository.deleteOutbreakAlert(alert)
        }
    }

    // -------------------------------------------------------------------------
    // 9. Smart Doctor Finder & Online Appointment Booking
    // -------------------------------------------------------------------------
    private val _isSearchingDoctors = MutableStateFlow(false)
    val isSearchingDoctors: StateFlow<Boolean> = _isSearchingDoctors.asStateFlow()

    private val _matchedDoctorRecommendation = MutableStateFlow<com.example.utils.MatchedDoctorRecommendation?>(null)
    val matchedDoctorRecommendation: StateFlow<com.example.utils.MatchedDoctorRecommendation?> = _matchedDoctorRecommendation.asStateFlow()

    fun findSpecialistsNearby(context: Context, symptomQuery: String) {
        if (symptomQuery.isBlank() || _isSearchingDoctors.value) return
        _isSearchingDoctors.value = true
        viewModelScope.launch {
            try {
                val coords = try {
                    com.example.utils.EmergencySosLocationHelper.getCurrentLocation(context)
                } catch (_: Exception) {
                    null
                }
                val lat = coords?.first ?: _userCurrentGpsLocation.value?.first ?: 22.3039
                val lon = coords?.second ?: _userCurrentGpsLocation.value?.second ?: 70.8022
                _userCurrentGpsLocation.value = lat to lon

                val places = try {
                    com.example.utils.NearbyHospitalsService.fetchRealNearbyFacilities(lat, lon)
                } catch (_: Exception) {
                    com.example.utils.NearbyHospitalsService.getNearbyFacilities(lat, lon)
                }
                _nearbyMedicalPlaces.value = places

                val match = com.example.utils.AdvancedClinicalEngines.matchSpecialistAndDoctors(
                    symptomQuery = symptomQuery,
                    nearbyFacilities = places,
                    language = _selectedLanguage.value
                )
                _matchedDoctorRecommendation.value = match
            } finally {
                _isSearchingDoctors.value = false
            }
        }
    }

    fun bookDoctorAppointment(
        listing: com.example.utils.MatchedDoctorListing,
        appointmentDate: String,
        timeSlot: String,
        consultationMode: String,
        symptomNote: String
    ) {
        viewModelScope.launch {
            val ref = "HCAI-${(1000..9999).random()}"
            repository.saveDoctorAppointment(
                com.example.data.local.DoctorAppointmentEntity(
                    bookingRef = ref,
                    doctorName = listing.doctorName,
                    specialty = listing.specialty,
                    clinicName = listing.clinicName,
                    distanceKm = listing.distanceKm,
                    appointmentDate = appointmentDate,
                    timeSlot = timeSlot,
                    consultationMode = consultationMode,
                    symptomNote = symptomNote.trim(),
                    status = "Confirmed"
                )
            )
        }
    }

    fun cancelDoctorAppointment(appointment: com.example.data.local.DoctorAppointmentEntity) {
        viewModelScope.launch {
            repository.deleteDoctorAppointment(appointment)
        }
    }

    // -------------------------------------------------------------------------
    // 10. Live AI Doctor Video Call Consultation (Camera + Voice + TTS)
    // -------------------------------------------------------------------------
    data class VideoCallTurn(
        val id: Long = System.nanoTime(),
        val speaker: String, // "PATIENT" or "AI"
        val text: String,
        val hadCameraSnapshot: Boolean = false,
        val isEmergency: Boolean = false
    )

    private val _videoCallTurns = MutableStateFlow<List<VideoCallTurn>>(emptyList())
    val videoCallTurns: StateFlow<List<VideoCallTurn>> = _videoCallTurns.asStateFlow()

    private val _isVideoAiResponding = MutableStateFlow(false)
    val isVideoAiResponding: StateFlow<Boolean> = _isVideoAiResponding.asStateFlow()

    private val _videoCallAutoListen = MutableStateFlow(true)
    val videoCallAutoListen: StateFlow<Boolean> = _videoCallAutoListen.asStateFlow()

    fun setVideoCallAutoListen(enabled: Boolean) {
        _videoCallAutoListen.value = enabled
    }

    fun startVideoCallGreeting() {
        boostCallAudioVolumeIfTooLow()
        if (_videoCallTurns.value.isNotEmpty()) return
        val lang = _selectedLanguage.value
        val patientFirstName = userProfile.value?.fullName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
        val greeting = when {
            lang.equals("Gujarati", ignoreCase = true) ->
                "નમસ્તે ${patientFirstName ?: "મિત્ર"}! હું Health Care AI ડૉક્ટર છું. માઇક દબાવ્યા વગર સીધું બોલો, હું તમને સાંભળું છું."
            lang.equals("Hindi", ignoreCase = true) ->
                "नमस्ते ${patientFirstName ?: "मित्र"}! मैं Health Care AI डॉक्टर हूँ। बिना माइक दबाए सीधे बोलें, मैं आपको सुन रहा हूँ।"
            else ->
                "Hello ${patientFirstName ?: "there"}! I am your Health Care AI Assistant on video. Speak hands-free right away without tapping the mic, or show your symptom on camera."
        }
        val aiTurn = VideoCallTurn(speaker = "AI", text = greeting)
        _videoCallTurns.value = listOf(aiTurn)
        if (_autoSpeakEnabled.value) {
            speakText(greeting, aiTurn.id)
        }
    }

    fun sendVideoCallMessage(spokenText: String, cameraFrameBase64: String? = null) {
        val cleanText = spokenText.trim()
        if ((cleanText.isBlank() && cameraFrameBase64.isNullOrBlank()) || _isVideoAiResponding.value) return

        val lang = _selectedLanguage.value
        val effectivePatientPrompt = cleanText.ifBlank {
            trLang(
                lang,
                "Please look at my live camera video frame and tell me what you observe and how I should care for it.",
                "કૃપા કરીને મારા લાઇવ વિડિયો કૅમેરામાં દેખાતી તકલીફ જોઈને તેના વિશે યોગ્ય સલાહ આપો.",
                "कृपया मेरे लाइव वीडियो कैमरे में दिख रही समस्या को देखकर उचित सलाह दें।"
            )
        }

        val patientTurn = VideoCallTurn(
            speaker = "PATIENT",
            text = effectivePatientPrompt,
            hadCameraSnapshot = !cameraFrameBase64.isNullOrBlank()
        )
        _videoCallTurns.value = _videoCallTurns.value + patientTurn
        _isVideoAiResponding.value = true

        viewModelScope.launch {
            try {
                val recentPairs = _videoCallTurns.value.takeLast(6).map {
                    (if (it.speaker == "PATIENT") "Patient" else "AI Doctor") to it.text
                }
                val result = repository.sendVideoConsultationTurn(
                    spokenMessage = effectivePatientPrompt,
                    recentCallTurns = recentPairs,
                    userProfile = userProfile.value,
                    cameraFrameBase64 = cameraFrameBase64,
                    language = lang
                )
                result.onSuccess { (reply, isEmerg) ->
                    if (isEmerg) {
                        _emergencyAlertTriggered.value = true
                    }
                    val aiTurn = VideoCallTurn(
                        speaker = "AI",
                        text = reply,
                        isEmergency = isEmerg
                    )
                    _videoCallTurns.value = _videoCallTurns.value + aiTurn
                    if (_autoSpeakEnabled.value) {
                        speakText(reply, aiTurn.id)
                    }
                }.onFailure {
                    val errMsg = trLang(
                        lang,
                        "Your internet is slow right now, so I did not guess an answer. Please wait a moment and speak again.",
                        "હાલમાં ઇન્ટરનેટ ધીમું હોવાથી ખોટો જવાબ આપ્યો નથી. કૃપા કરીને એક ક્ષણ પછી ફરી બોલો.",
                        "अभी इंटरनेट धीमा है, इसलिए मैंने गलत अनुमान नहीं लगाया। कृपया एक पल बाद फिर बोलें।"
                    )
                    val errTurn = VideoCallTurn(speaker = "AI", text = errMsg)
                    _videoCallTurns.value = _videoCallTurns.value + errTurn
                    if (_autoSpeakEnabled.value) {
                        speakText(errMsg, errTurn.id)
                    }
                }
            } finally {
                _isVideoAiResponding.value = false
            }
        }
    }

    fun endAndSaveVideoCallToEhr() {
        stopSpeaking()
        val turns = _videoCallTurns.value
        val patientUtterances = turns.filter { it.speaker == "PATIENT" }
        if (patientUtterances.isNotEmpty()) {
            val transcript = turns.joinToString("\n") { "${it.speaker}: ${it.text}" }
            val cleanName = userProfile.value?.fullName ?: "Patient"
            organizeAndSaveEhr(cleanName, transcript)
        }
        _videoCallTurns.value = emptyList()
        _currentScreen.value = AppScreen.TRIAGE_CHAT
    }

    // -------------------------------------------------------------
    // DEVELOPER LIVE APP UPGRADES, PUBLIC DOCTORS & USER ONLINE TIME
    // -------------------------------------------------------------
    val allPublicDoctors: StateFlow<List<com.example.data.local.PublicDoctorEntity>> =
        repository.allPublicDoctors.safeStateIn(emptyList())

    val activePublicDoctors: StateFlow<List<com.example.data.local.PublicDoctorEntity>> =
        repository.activePublicDoctors.safeStateIn(emptyList())

    val devAppConfig: StateFlow<com.example.data.local.DeveloperAppConfigEntity> =
        combine(
            repository.devAppConfig.catch { emit(null) },
            MutableStateFlow(Unit)
        ) { entity, _ ->
            entity ?: com.example.data.local.DeveloperAppConfigEntity()
        }.safeStateIn(com.example.data.local.DeveloperAppConfigEntity())

    private var devConfigListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val upgradePrefs by lazy {
        getApplication<Application>().getSharedPreferences("app_upgrade_prefs", android.content.Context.MODE_PRIVATE)
    }

    private val _dismissedUpgradeConfigVersion = MutableStateFlow(
        runCatching { upgradePrefs.getInt("dismissed_upgrade_config_version", 0) }.getOrDefault(0)
    )
    val dismissedUpgradeConfigVersion: StateFlow<Int> = _dismissedUpgradeConfigVersion.asStateFlow()

    fun dismissUpgradeDialog(configVersion: Int) {
        _dismissedUpgradeConfigVersion.value = configVersion
        runCatching {
            upgradePrefs.edit().putInt("dismissed_upgrade_config_version", configVersion).apply()
        }
    }

    fun startLivePublicConfigObserver() {
        if (devConfigListener != null) return
        try {
            devConfigListener = firebaseBackupRepo.observeDevAppConfig { remoteConfig ->
                viewModelScope.launch {
                    try {
                        val current = devAppConfig.value
                        if (remoteConfig.configVersion >= current.configVersion ||
                            remoteConfig.updatedAt > current.updatedAt
                        ) {
                            repository.saveDevAppConfig(remoteConfig)
                        }
                    } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {}
    }

    private val _cloudUserPresence =
        MutableStateFlow<List<com.example.data.local.DeveloperUserPresenceSummary>>(emptyList())

    val developerUserPresenceList: StateFlow<List<com.example.data.local.DeveloperUserPresenceSummary>> =
        combine(
            repository.developerUserPresenceList.catch { emit(emptyList()) },
            _cloudUserPresence
        ) { localList, cloudList ->
            val merged = linkedMapOf<String, com.example.data.local.DeveloperUserPresenceSummary>()
            (localList + cloudList).forEach { entry ->
                val existing = merged[entry.username]
                if (existing == null ||
                    entry.totalOnlineSeconds > existing.totalOnlineSeconds ||
                    entry.lastOnlineAt > existing.lastOnlineAt
                ) {
                    merged[entry.username] = entry
                }
            }
            merged.values.sortedWith(
                compareByDescending<com.example.data.local.DeveloperUserPresenceSummary> { it.isCurrentlyOnline }
                    .thenByDescending { it.lastOnlineAt }
            )
        }.safeStateIn(emptyList())

    private val _devUpgradeStatus = MutableStateFlow<String?>(null)
    val devUpgradeStatus: StateFlow<String?> = _devUpgradeStatus.asStateFlow()

    fun clearDevUpgradeStatus() {
        _devUpgradeStatus.value = null
    }

    fun recordOnlineSessionHeartbeat(deltaSeconds: Long = 30L, isOnline: Boolean = true) {
        val session = _authSession.value
        if (!session.isLoggedIn || session.username.isBlank()) return
        viewModelScope.launch {
            try {
                val updated = repository.recordUserHeartbeat(
                    username = session.username,
                    authMethod = session.authMethod,
                    deltaSeconds = deltaSeconds,
                    isOnline = isOnline
                )
                if (updated != null) {
                    firebaseBackupRepo.syncUserPresence(updated)
                }
            } catch (_: Throwable) {}
        }
    }

    fun markCurrentSessionOffline(
        explicitUsername: String? = null,
        explicitAuthMethod: String? = null
    ) {
        val session = _authSession.value
        val targetUser = explicitUsername ?: session.username
        if (targetUser.isBlank()) return
        viewModelScope.launch {
            try {
                repository.markUserOffline(targetUser)
                val summary = repository.recordUserHeartbeat(
                    username = targetUser,
                    authMethod = explicitAuthMethod ?: session.authMethod,
                    deltaSeconds = 0L,
                    isOnline = false
                )
                if (summary != null) {
                    firebaseBackupRepo.syncUserPresence(summary)
                }
            } catch (_: Throwable) {}
        }
    }

    fun savePublicDoctor(
        doctorId: Long = 0L,
        cloudId: String = "",
        doctorName: String,
        specialty: String,
        clinicOrCity: String,
        mobileNumber: String,
        consultationFee: String,
        videoCallEnabled: Boolean = false,
        videoCallRoomUrl: String = "",
        isPublicActive: Boolean = true
    ): Boolean {
        if (!_authSession.value.isDeveloper) {
            _devUpgradeStatus.value = "Unauthorized: Developer login required."
            return false
        }
        val cleanName = doctorName.trim()
        val cleanPhone = mobileNumber.trim()
        if (cleanName.isBlank() || cleanPhone.isBlank()) {
            _devUpgradeStatus.value = "Doctor name and mobile number are required."
            return false
        }
        val entity = com.example.data.local.PublicDoctorEntity(
            id = doctorId,
            cloudId = cloudId.ifBlank { java.util.UUID.randomUUID().toString() },
            doctorName = cleanName,
            specialty = specialty.trim().ifBlank { "General Physician" },
            clinicOrCity = clinicOrCity.trim().ifBlank { "Public Clinic" },
            mobileNumber = cleanPhone,
            consultationFee = consultationFee.trim().ifBlank { "₹300" },
            videoCallEnabled = false,
            videoCallRoomUrl = "",
            isPublicActive = isPublicActive,
            addedByDeveloper = _authSession.value.username,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.upsertPublicDoctor(entity)
            val cloudRes = firebaseBackupRepo.syncPublicDoctor(entity)
            com.example.utils.DevLogger.i(
                "DevConsole",
                "Developer saved public doctor '${entity.doctorName}' (cloudSync=${cloudRes.isSuccess})"
            )
            _devUpgradeStatus.value = if (cloudRes.isSuccess) {
                "Published '${entity.doctorName}' to Public Directory & Cloud."
            } else {
                "Saved '${entity.doctorName}' on device (Cloud sync queued)."
            }
        }
        return true
    }

    fun togglePublicDoctorActive(doctor: com.example.data.local.PublicDoctorEntity) {
        if (!_authSession.value.isDeveloper) return
        val updated = doctor.copy(
            isPublicActive = !doctor.isPublicActive,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.upsertPublicDoctor(updated)
            firebaseBackupRepo.syncPublicDoctor(updated)
        }
    }

    fun deletePublicDoctor(doctor: com.example.data.local.PublicDoctorEntity) {
        if (!_authSession.value.isDeveloper) return
        viewModelScope.launch {
            repository.deletePublicDoctor(doctor)
            firebaseBackupRepo.deletePublicDoctorFromCloud(doctor.cloudId)
            com.example.utils.DevLogger.w("DevConsole", "Removed public doctor '${doctor.doctorName}'")
        }
    }

    fun saveDeveloperAppConfig(
        appBannerNotice: String,
        publicDoctorConsultEnabled: Boolean,
        videoCallFeatureEnabled: Boolean,
        medicineAdvisorFeatureEnabled: Boolean,
        emergencyHelplineOverride: String,
        ultraHubFeatureEnabled: Boolean = devAppConfig.value.ultraHubFeatureEnabled,
        pmjayFinderEnabled: Boolean = devAppConfig.value.pmjayFinderEnabled,
        pmjayPortalUrlOverride: String = devAppConfig.value.pmjayPortalUrlOverride,
        reportScannerFeatureEnabled: Boolean = devAppConfig.value.reportScannerFeatureEnabled,
        splashDurationSeconds: Int = devAppConfig.value.splashDurationSeconds,
        splashTopCreditText: String = devAppConfig.value.splashTopCreditText,
        splashSchoolNameText: String = devAppConfig.value.splashSchoolNameText,
        maintenanceModeEnabled: Boolean = devAppConfig.value.maintenanceModeEnabled,
        maintenanceMessage: String = devAppConfig.value.maintenanceMessage,
        supportContactPhone: String = devAppConfig.value.supportContactPhone
    ): Boolean {
        if (!_authSession.value.isDeveloper) {
            _devUpgradeStatus.value = "Unauthorized: Developer login required."
            return false
        }
        val current = devAppConfig.value
        val updated = current.copy(
            id = 1,
            appBannerNotice = appBannerNotice.trim(),
            publicDoctorConsultEnabled = publicDoctorConsultEnabled,
            videoCallFeatureEnabled = videoCallFeatureEnabled,
            medicineAdvisorFeatureEnabled = medicineAdvisorFeatureEnabled,
            emergencyHelplineOverride = emergencyHelplineOverride.trim().ifBlank { "112" },
            ultraHubFeatureEnabled = ultraHubFeatureEnabled,
            pmjayFinderEnabled = pmjayFinderEnabled,
            pmjayPortalUrlOverride = pmjayPortalUrlOverride.trim().ifBlank { com.example.utils.PmjayPortalHelper.PMJAY_HOSPITAL_SEARCH_URL },
            reportScannerFeatureEnabled = reportScannerFeatureEnabled,
            splashDurationSeconds = splashDurationSeconds.coerceIn(2, 10),
            splashTopCreditText = splashTopCreditText.trim().ifBlank { "Created by Shailesh and Ruturaj" },
            splashSchoolNameText = splashSchoolNameText.trim().ifBlank { "Model School Sayla" },
            maintenanceModeEnabled = maintenanceModeEnabled,
            maintenanceMessage = maintenanceMessage.trim(),
            supportContactPhone = supportContactPhone.trim(),
            configVersion = current.configVersion + 1,
            updatedBy = _authSession.value.username,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveDevAppConfig(updated)
            val cloudRes = firebaseBackupRepo.syncDevAppConfig(updated)
            com.example.utils.DevLogger.i(
                "DevConsole",
                "Developer published live app config v${updated.configVersion} (cloud=${cloudRes.isSuccess})"
            )
            _devUpgradeStatus.value = if (cloudRes.isSuccess) {
                "Live App Controls v${updated.configVersion} published to all users!"
            } else {
                "Saved Live App Controls v${updated.configVersion} locally."
            }
        }
        return true
    }

    fun publishPublicAppUpgrade(
        targetVersion: String,
        title: String,
        message: String,
        downloadUrl: String,
        mandatory: Boolean = false
    ): Boolean {
        if (!_authSession.value.isDeveloper) {
            _devUpgradeStatus.value = "Unauthorized: Developer login required."
            return false
        }
        val cleanVersion = targetVersion.trim().ifBlank { "${com.example.BuildConfig.VERSION_NAME}.1" }
        val cleanTitle = title.trim().ifBlank { "New Health Care AI Upgrade v$cleanVersion" }
        val cleanMessage = message.trim()
        if (cleanMessage.isBlank()) {
            _devUpgradeStatus.value = "Please enter an upgrade message for users."
            return false
        }
        val cleanUrl = com.example.utils.AppUpgradeDownloadHelper.normalizeDownloadUrl(downloadUrl) ?: downloadUrl.trim()
        val current = devAppConfig.value
        val now = System.currentTimeMillis()
        val updated = current.copy(
            id = 1,
            upgradePublished = true,
            upgradeTargetVersion = cleanVersion,
            upgradeTitle = cleanTitle,
            upgradeMessage = cleanMessage,
            upgradeDownloadUrl = cleanUrl,
            upgradeMandatory = mandatory,
            configVersion = current.configVersion + 1,
            updatedBy = _authSession.value.username,
            updatedAt = now
        )
        viewModelScope.launch {
            repository.saveDevAppConfig(updated)
            val cloudRes = firebaseBackupRepo.syncDevAppConfig(updated)
            com.example.utils.DevLogger.i(
                "DevConsole",
                "Developer published PUBLIC APP UPGRADE v$cleanVersion (url=$cleanUrl, mandatory=$mandatory, cloud=${cloudRes.isSuccess})"
            )
            _devUpgradeStatus.value = if (cloudRes.isSuccess) {
                "Public App Upgrade v$cleanVersion sent to ALL users with Download Upgrade!"
            } else {
                "Published Public App Upgrade v$cleanVersion locally (Cloud sync queued)."
            }
        }
        return true
    }

    fun clearPublishedAppUpgrade(): Boolean {
        if (!_authSession.value.isDeveloper) {
            _devUpgradeStatus.value = "Unauthorized: Developer login required."
            return false
        }
        val current = devAppConfig.value
        val updated = current.copy(
            id = 1,
            upgradePublished = false,
            configVersion = current.configVersion + 1,
            updatedBy = _authSession.value.username,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveDevAppConfig(updated)
            val cloudRes = firebaseBackupRepo.syncDevAppConfig(updated)
            com.example.utils.DevLogger.i(
                "DevConsole",
                "Developer unpublished app upgrade banner (cloud=${cloudRes.isSuccess})"
            )
            _devUpgradeStatus.value = "Public App Upgrade notification turned off."
        }
        return true
    }

    fun syncPublicDataFromCloud() {
        viewModelScope.launch {
            try {
                firebaseBackupRepo.fetchPublicDoctorsFromCloud().onSuccess { cloudDocs ->
                    cloudDocs.forEach { doc -> repository.upsertPublicDoctor(doc) }
                }
                firebaseBackupRepo.fetchDevAppConfig().onSuccess { cfg ->
                    if (cfg != null) repository.saveDevAppConfig(cfg)
                }
            } catch (_: Throwable) {}
        }
    }

    fun refreshDeveloperCloudConsole() {
        if (!_authSession.value.isDeveloper) return
        viewModelScope.launch {
            try {
                syncPublicDataFromCloud()
                firebaseBackupRepo.fetchAllUserPresenceFromCloud().onSuccess { cloudUsers ->
                    _cloudUserPresence.value = cloudUsers
                }
                _devUpgradeStatus.value = "Developer Console synced with latest cloud user activity."
            } catch (_: Throwable) {}
        }
    }

    // -------------------------------------------------------------
    // ULTRA HEALTH COMMAND CENTER (SCORE, STREAKS, SLEEP, WIDGET)
    // -------------------------------------------------------------
    val sleepWellnessLogs: StateFlow<List<com.example.data.local.SleepWellnessLogEntity>> =
        repository.allSleepWellnessLogs.safeStateIn(emptyList())

    private val _ultraHealthTab = MutableStateFlow(UltraHealthTab.SCORE)
    val ultraHealthTab: StateFlow<UltraHealthTab> = _ultraHealthTab.asStateFlow()

    fun openUltraHealthHub(tab: UltraHealthTab = UltraHealthTab.SCORE) {
        _ultraHealthTab.value = tab
        _currentScreen.value = AppScreen.ULTRA_HEALTH_HUB
        syncHealthSummaryWidget()
    }

    fun selectUltraHealthTab(tab: UltraHealthTab) {
        _ultraHealthTab.value = tab
    }

    fun logSleepWellness(
        sleepHours: Double,
        sleepQuality: Int,
        bedTime: String = "10:30 PM",
        wakeTime: String = "06:00 AM",
        weightKg: Double = 0.0,
        heightCm: Double = 0.0,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.saveSleepWellnessLog(
                com.example.data.local.SleepWellnessLogEntity(
                    dateKey = today,
                    sleepHours = sleepHours.coerceIn(1.0, 18.0),
                    sleepQuality = sleepQuality.coerceIn(1, 5),
                    bedTime = bedTime.ifBlank { "10:30 PM" },
                    wakeTime = wakeTime.ifBlank { "06:00 AM" },
                    weightKg = weightKg.coerceAtLeast(0.0),
                    heightCm = heightCm.coerceAtLeast(0.0),
                    notes = notes.trim(),
                    timestamp = System.currentTimeMillis()
                )
            )
            syncHealthSummaryWidget()
        }
    }

    fun deleteSleepWellnessLog(log: com.example.data.local.SleepWellnessLogEntity) {
        viewModelScope.launch {
            repository.deleteSleepWellnessLog(log)
            syncHealthSummaryWidget()
        }
    }

    // -------------------------------------------------------------
    // ULTRA CLINICAL GLASS UPGRADE: AI COACH, BODY MAP, NUTRITION, SOS
    // -------------------------------------------------------------
    private val ultraUpgradePrefs by lazy {
        getApplication<Application>().getSharedPreferences("ultra_upgrade_prefs", android.content.Context.MODE_PRIVATE)
    }

    private fun todayDateKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val mealNutritionLogs: StateFlow<List<com.example.data.local.MealNutritionLogEntity>> =
        repository.allMealNutritionLogs.safeStateIn(emptyList())

    private val _dailyCalorieTarget = MutableStateFlow(
        runCatching { ultraUpgradePrefs.getInt("daily_calorie_target", 2000) }.getOrDefault(2000)
    )
    val dailyCalorieTarget: StateFlow<Int> = _dailyCalorieTarget.asStateFlow()

    private val _completedCoachTaskIds = MutableStateFlow(loadTodayCompletedCoachTasks())
    val completedCoachTaskIds: StateFlow<Set<String>> = _completedCoachTaskIds.asStateFlow()

    private val _aiCoachBrief = MutableStateFlow<String?>(null)
    val aiCoachBrief: StateFlow<String?> = _aiCoachBrief.asStateFlow()

    private val _isAiCoachFromGemini = MutableStateFlow(false)
    val isAiCoachFromGemini: StateFlow<Boolean> = _isAiCoachFromGemini.asStateFlow()

    private val _isGeneratingCoachPlan = MutableStateFlow(false)
    val isGeneratingCoachPlan: StateFlow<Boolean> = _isGeneratingCoachPlan.asStateFlow()

    private val _shakeToSosEnabled = MutableStateFlow(
        runCatching { ultraUpgradePrefs.getBoolean("shake_to_sos_enabled", false) }.getOrDefault(false)
    )
    val shakeToSosEnabled: StateFlow<Boolean> = _shakeToSosEnabled.asStateFlow()

    private fun loadTodayCompletedCoachTasks(): Set<String> {
        return runCatching {
            val savedDate = ultraUpgradePrefs.getString("coach_tasks_date", "")
            if (savedDate == todayDateKey()) {
                ultraUpgradePrefs.getStringSet("coach_tasks_done", emptySet())?.toSet() ?: emptySet()
            } else {
                emptySet()
            }
        }.getOrDefault(emptySet())
    }

    fun toggleCoachTaskCompleted(taskId: String) {
        val updated = _completedCoachTaskIds.value.toMutableSet().apply {
            if (!add(taskId)) remove(taskId)
        }
        _completedCoachTaskIds.value = updated
        runCatching {
            ultraUpgradePrefs.edit()
                .putString("coach_tasks_date", todayDateKey())
                .putStringSet("coach_tasks_done", updated)
                .apply()
        }
    }

    fun refreshAiHealthCoachPlan() {
        if (_isGeneratingCoachPlan.value) return
        viewModelScope.launch {
            _isGeneratingCoachPlan.value = true
            try {
                val today = todayDateKey()
                val waterMl = todayTotalWaterMl.value
                val waterGoal = waterTargetMl.value
                val stepRec = todayStepRecord.value
                val steps = stepRec?.steps ?: 0
                val stepGoal = stepRec?.targetSteps ?: 8000
                val activeMeds = medicines.value.filter { it.isActive }
                val adherence = allAdherenceLogs.value
                val sleep = sleepWellnessLogs.value
                val vitals = vitalLogs.value
                val score = com.example.utils.UltraHealthScoreEngine.calculate(
                    todayWaterMl = waterMl,
                    waterTargetMl = waterGoal,
                    todaySteps = steps,
                    stepTarget = stepGoal,
                    activeMedicines = activeMeds,
                    adherenceLogs = adherence,
                    sleepLogs = sleep,
                    vitalLogs = vitals
                ).overallScore
                val nutrition = com.example.utils.NutritionPresetCatalog.calculateDailySummary(
                    dateKey = today,
                    logs = mealNutritionLogs.value,
                    targetCalories = _dailyCalorieTarget.value
                )
                val (brief, isGemini) = repository.generateAiHealthCoachAdvice(
                    healthScore = score,
                    todayWaterMl = waterMl,
                    waterTargetMl = waterGoal,
                    todaySteps = steps,
                    stepTarget = stepGoal,
                    todayCalories = nutrition.totalCalories,
                    calorieTarget = nutrition.targetCalories,
                    proteinGrams = nutrition.totalProteinGrams,
                    activeMedicinesCount = activeMeds.size,
                    latestSleepHours = sleep.firstOrNull()?.sleepHours,
                    profile = userProfile.value,
                    language = _selectedLanguage.value
                )
                _aiCoachBrief.value = brief
                _isAiCoachFromGemini.value = isGemini
            } catch (_: Throwable) {
            } finally {
                _isGeneratingCoachPlan.value = false
            }
        }
    }

    fun logMealNutrition(
        mealType: String,
        foodName: String,
        calories: Int,
        proteinGrams: Double = 0.0,
        carbsGrams: Double = 0.0,
        fiberGrams: Double = 0.0
    ) {
        val cleanName = foodName.trim()
        if (cleanName.isBlank() || calories <= 0) return
        viewModelScope.launch {
            repository.saveMealNutritionLog(
                com.example.data.local.MealNutritionLogEntity(
                    dateKey = todayDateKey(),
                    mealType = mealType.ifBlank { "Lunch" },
                    foodName = cleanName,
                    calories = calories.coerceIn(1, 5000),
                    proteinGrams = proteinGrams.coerceAtLeast(0.0),
                    carbsGrams = carbsGrams.coerceAtLeast(0.0),
                    fiberGrams = fiberGrams.coerceAtLeast(0.0),
                    timestamp = System.currentTimeMillis()
                )
            )
            syncHealthSummaryWidget()
        }
    }

    fun deleteMealNutrition(log: com.example.data.local.MealNutritionLogEntity) {
        viewModelScope.launch {
            repository.deleteMealNutritionLog(log)
            syncHealthSummaryWidget()
        }
    }

    fun updateDailyCalorieTarget(kcal: Int) {
        val safe = kcal.coerceIn(1200, 4000)
        _dailyCalorieTarget.value = safe
        runCatching {
            ultraUpgradePrefs.edit().putInt("daily_calorie_target", safe).apply()
        }
    }

    fun setShakeToSosEnabled(enabled: Boolean) {
        _shakeToSosEnabled.value = enabled
        runCatching {
            ultraUpgradePrefs.edit().putBoolean("shake_to_sos_enabled", enabled).apply()
        }
    }

    fun launchBodyMapTriage(
        zoneTitle: String,
        symptoms: List<String>,
        severity: String,
        duration: String,
        additionalNotes: String = ""
    ) {
        val symptomListText = symptoms.filter { it.isNotBlank() }.joinToString(", ")
            .ifBlank { "localized discomfort" }
        val notesPart = additionalNotes.trim().takeIf { it.isNotBlank() }?.let { " Additional details: $it." } ?: ""
        val structuredPrompt =
            "Body Map Visual Triage — Area: $zoneTitle. Symptoms: $symptomListText. Severity: $severity. Duration: $duration.$notesPart Please provide structured clinical triage guidance, red flags, safe self-care, and recommended specialist."
        _currentScreen.value = AppScreen.TRIAGE_CHAT
        sendMessage(structuredPrompt)
    }

    fun syncHealthSummaryWidget() {
        try {
            val app = getApplication<Application>()
            val waterMl = todayTotalWaterMl.value
            val waterGoal = waterTargetMl.value
            val stepRec = todayStepRecord.value
            val steps = stepRec?.steps ?: 0
            val stepGoal = stepRec?.targetSteps ?: 8000
            val activeMeds = medicines.value.filter { it.isActive }
            val adherence = allAdherenceLogs.value
            val sleep = sleepWellnessLogs.value
            val vitals = vitalLogs.value

            val scoreReport = com.example.utils.UltraHealthScoreEngine.calculate(
                todayWaterMl = waterMl,
                waterTargetMl = waterGoal,
                todaySteps = steps,
                stepTarget = stepGoal,
                activeMedicines = activeMeds,
                adherenceLogs = adherence,
                sleepLogs = sleep,
                vitalLogs = vitals
            )
            val streak = com.example.utils.MedicationStreakEngine.analyze(
                medicines = activeMeds,
                adherenceLogs = adherence
            )
            val nextPendingMed = streak.perMedicineStats.firstOrNull { !it.takenToday }
            val nextMedText = when {
                nextPendingMed != null -> "Next Rx: ${nextPendingMed.medicineName} (${nextPendingMed.timeOfDay})"
                activeMeds.isNotEmpty() -> "All ${activeMeds.size} doses taken today!"
                else -> "No medicines scheduled"
            }
            val tierLabel = when (scoreReport.tier) {
                com.example.utils.HealthScoreTier.ULTRA_OPTIMAL -> "Ultra Optimal"
                com.example.utils.HealthScoreTier.STRONG -> "Strong"
                com.example.utils.HealthScoreTier.STEADY -> "Steady"
                com.example.utils.HealthScoreTier.NEEDS_ATTENTION -> "Boost Needed"
            }

            com.example.widget.HealthWidgetSnapshotStore.updateAndRefreshWidgets(
                context = app,
                snapshot = com.example.widget.HealthWidgetSnapshot(
                    healthScore = scoreReport.overallScore,
                    scoreTierLabel = tierLabel,
                    stepsToday = steps,
                    stepTarget = stepGoal,
                    waterMlToday = waterMl,
                    waterTargetMl = waterGoal,
                    nextMedicineLabel = nextMedText,
                    streakDays = streak.currentStreakDays
                )
            )
        } catch (_: Throwable) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            devConfigListener?.remove()
            devConfigListener = null
        } catch (_: Exception) {}
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
        stepSensorTracker?.stopTracking()
        try {
            bleWearableManager.cleanup()
        } catch (_: Exception) {}
    }
}

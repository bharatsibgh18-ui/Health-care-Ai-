package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.res.Configuration
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
    SMART_HUB
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

        viewModelScope.launch {
            kotlinx.coroutines.yield()
            initTextToSpeech(application)
        }
    }

    private fun initTextToSpeech(application: Application) {
        if (textToSpeech != null) return
        try {
            textToSpeech = TextToSpeech(application) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
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

    val medicines: StateFlow<List<com.example.data.local.MedicineEntity>> = repository.allMedicines.stateIn(
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

    val vitalLogs: StateFlow<List<com.example.data.local.VitalLogEntity>> = repository.allVitalLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val familyMembers: StateFlow<List<com.example.data.local.FamilyMemberEntity>> = repository.allFamilyMembers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
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
        repository.allScannedReports.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
    val todayDateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val todayStepRecord: StateFlow<com.example.data.local.StepRecordEntity?> =
        repository.getStepRecordForDate(todayDateString).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val weeklyStepRecords: StateFlow<List<com.example.data.local.StepRecordEntity>> =
        repository.recentStepRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
        } catch (_: Exception) {
            _sensorHardwareDescription.value = "Sensor Standby / Manual Mode"
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
    }

    fun updateInputText(newText: String) {
        _inputText.value = newText
    }

    fun setSelectedLanguage(lang: String) {
        _selectedLanguage.value = lang
        applyAppLocale(lang)
    }

    fun refreshAppLanguage() {
        val currentLang = _selectedLanguage.value
        applyAppLocale(currentLang)
    }

    private fun applyAppLocale(lang: String) {
        try {
            val locale = when (lang) {
                "Gujarati" -> Locale.forLanguageTag("gu")
                "Hindi" -> Locale.forLanguageTag("hi")
                else -> Locale.ENGLISH
            }
            Locale.setDefault(locale)
            val app = getApplication<Application>()
            val config = Configuration(app.resources.configuration)
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            app.resources.updateConfiguration(config, app.resources.displayMetrics)
        } catch (_: Exception) {}
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
                    text = trLang(
                        _selectedLanguage.value,
                        "⚠️ An error occurred while contacting triage: ${err.message}. Please consult a local healthcare professional or call emergency services if symptoms are severe.",
                        "⚠️ તપાસ કરતી વખતે ભૂલ આવી: ${err.message}. જો લક્ષણો ગંભીર હોય તો કૃપા કરીને નજીકના ડોક્ટરનો સંપર્ક કરો અથવા ઇમરજન્સી સેવા (108/112) પર કૉલ કરો.",
                        "⚠️ जांच के दौरान त्रुटि हुई: ${err.message}. यदि लक्षण गंभीर हों तो कृपया नजदीकी डॉक्टर से संपर्क करें या आपातकालीन सेवा (108/112) पर कॉल करें।"
                    ),
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
        initTextToSpeech(getApplication())

        val cleaned = cleanTextForSpeech(text)
        if (cleaned.isBlank()) return

        val tts = textToSpeech
        if (tts == null || !_isTtsReady.value) {
            _speakingMessageId.value = null
            _isSpeaking.value = false
            return
        }

        val locale = detectLocaleForText(cleaned)
        try {
            val avail = tts.isLanguageAvailable(locale)
            if (avail >= TextToSpeech.LANG_AVAILABLE) {
                tts.language = locale
            } else {
                val hiLocale = Locale.forLanguageTag("hi-IN")
                val hiAvail = tts.isLanguageAvailable(hiLocale)
                if (hiAvail >= TextToSpeech.LANG_AVAILABLE) {
                    tts.language = hiLocale
                } else {
                    tts.language = Locale.getDefault()
                }
            }
        } catch (_: Exception) {
            try {
                tts.language = Locale.getDefault()
            } catch (_: Exception) {}
        }

        _speakingMessageId.value = messageId
        val utteranceId = "msg_${messageId ?: System.currentTimeMillis()}"
        try {
            val result = tts.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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
        val hasGujarati = text.any { it in '\u0A80'..'\u0AFF' }
        if (hasGujarati) {
            val guLocale = Locale.forLanguageTag("gu-IN")
            val available = try {
                textToSpeech?.isLanguageAvailable(guLocale)
            } catch (_: Exception) {
                null
            }
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
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
        _speakingMessageId.value = null
    }

    // -------------------------------------------------------------
    // WATER REMINDER & HYDRATION TRACKING (પાણી રિમાઇન્ડર)
    // -------------------------------------------------------------
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
        val lang = _selectedLanguage.value
        WaterReminderNotificationHelper.showWaterReminderNotification(
            getApplication(),
            title = trLang(
                lang,
                "💧 Time to Drink Water!",
                "💧 પાણી પીવાનો સમય થયો છે!",
                "💧 पानी पीने का समय हो गया है!"
            ),
            message = trLang(
                lang,
                "Stay hydrated! Drinking water boosts energy, improves digestion, and prevents headaches. Take a glass of water now!",
                "હાઇડ્રેટેડ રહો! પાણી પીવાથી ઊર્જા વધે છે, પાચન સુધરે છે અને માથાનો દુખાવો અટકે છે. અત્યારે એક ગ્લાસ પાણી પીઓ!",
                "हाइड्रेटेड रहें! पानी पीने से ऊर्जा बढ़ती है, पाचन सुधरता है और सिरदर्द से बचाव होता है। अभी एक गिलास पानी पिएं!"
            )
        )
    }

    fun triggerMedicineTestNotification() {
        val app = getApplication<Application>()
        val lang = _selectedLanguage.value
        val firstMed = medicines.value.firstOrNull()
        val name = firstMed?.name ?: "Paracetamol 500mg"
        val dosage = firstMed?.dosage ?: trLang(lang, "1 Tablet", "૧ ગોળી", "1 गोली")
        val instruction = firstMed?.instruction ?: trLang(lang, "After food", "જમ્યા પછી", "खाने के बाद")
        com.example.utils.MedicineReminderWorker.showImmediateNotification(
            context = app,
            medId = firstMed?.id ?: 999L,
            medName = name,
            dosage = dosage,
            instruction = instruction
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
        instruction: String
    ) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val medEntity = com.example.data.local.MedicineEntity(
                name = name.trim(),
                dosage = dosage.trim().ifBlank { "1 Tablet" },
                timeOfDay = timeOfDay,
                instruction = instruction,
                isActive = true
            )
            val insertedId = repository.saveMedicine(medEntity)
            com.example.utils.MedicineReminderWorker.scheduleDailyMedicineReminder(
                context = app,
                medId = insertedId,
                medName = medEntity.name,
                dosage = medEntity.dosage,
                instruction = medEntity.instruction
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
                    instruction = updated.instruction
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
        newInstruction: String? = null
    ) {
        val app = getApplication<Application>()
        val updated = medicine.copy(
            name = (newName ?: medicine.name).trim(),
            dosage = (newDosage ?: medicine.dosage).trim(),
            timeOfDay = newTime.trim(),
            instruction = (newInstruction ?: medicine.instruction).trim()
        )
        viewModelScope.launch {
            repository.updateMedicine(updated)
            if (updated.isActive) {
                com.example.utils.MedicineReminderWorker.scheduleDailyMedicineReminder(
                    context = app,
                    medId = updated.id,
                    medName = updated.name,
                    dosage = updated.dosage,
                    instruction = updated.instruction
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

    override fun onCleared() {
        super.onCleared()
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
        stepSensorTracker?.stopTracking()
    }
}

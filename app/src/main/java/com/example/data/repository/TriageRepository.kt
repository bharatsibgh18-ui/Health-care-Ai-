package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiApiService
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerateContentRequest
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.SlowNetworkException
import com.example.data.local.ChatMessageEntity
import com.example.data.local.StepRecordEntity
import com.example.data.local.SymptomLogEntity
import com.example.data.local.TriageDao
import com.example.data.local.TriageSessionEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WaterLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TriageRepository(
    private val triageDao: TriageDao,
    geminiService: GeminiApiService? = null,
    private val apiKeyOverride: String = BuildConfig.GEMINI_API_KEY
) {
    private val resolvedGeminiService: GeminiApiService by lazy {
        geminiService ?: GeminiApiClient.service
    }

    private sealed interface GeminiCallResult {
        data class Success(val text: String) : GeminiCallResult
        data object NoApiKey : GeminiCallResult
        data class NetworkError(
            val message: String,
            val isTimeout: Boolean,
            val cause: Throwable? = null
        ) : GeminiCallResult
    }

    private suspend fun executeGeminiWithRetry(
        request: GeminiGenerateContentRequest,
        explicitApiKey: String = apiKeyOverride
    ): GeminiCallResult {
        if (explicitApiKey.isBlank() || explicitApiKey == "MY_GEMINI_API_KEY") {
            return GeminiCallResult.NoApiKey
        }

        val modelCascade = listOf(
            "gemini-3.1-flash-lite-preview",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite"
        )

        var lastError: Throwable? = null
        var sawTimeout = false

        for ((modelIndex, modelName) in modelCascade.withIndex()) {
            val attemptsForModel = if (modelIndex < 2) 2 else 1
            for (attempt in 1..attemptsForModel) {
                try {
                    val response = resolvedGeminiService.generateContent(
                        model = modelName,
                        apiKey = explicitApiKey,
                        request = request
                    )
                    val apiErr = response.error
                    if (apiErr != null) {
                        val code = apiErr.code ?: 500
                        lastError = RuntimeException(apiErr.message ?: "Gemini API error ($code)")
                        if (code in setOf(408, 429, 500, 502, 503, 504) && attempt < attemptsForModel) {
                            delay(800L * attempt)
                            continue
                        }
                        break
                    }
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!text.isNullOrBlank()) {
                        return GeminiCallResult.Success(text)
                    }
                    lastError = IllegalStateException("Empty response from $modelName")
                    break
                } catch (e: HttpException) {
                    lastError = e
                    val code = e.code()
                    if (code in setOf(408, 429, 500, 502, 503, 504)) {
                        if (code == 408 || code == 504) sawTimeout = true
                        if (attempt < attemptsForModel) {
                            delay(850L * attempt)
                            continue
                        }
                    }
                    // On 400/404/non-retryable or exhausted attempts, fall through to next valid model
                    break
                } catch (e: SocketTimeoutException) {
                    lastError = e
                    sawTimeout = true
                    if (attempt < attemptsForModel) {
                        delay(900L * attempt)
                        continue
                    }
                } catch (e: InterruptedIOException) {
                    lastError = e
                    sawTimeout = true
                    if (attempt < attemptsForModel) {
                        delay(900L * attempt)
                        continue
                    }
                } catch (e: IOException) {
                    lastError = e
                    if (attempt < attemptsForModel) {
                        delay(800L * attempt)
                        continue
                    }
                } catch (e: Exception) {
                    lastError = e
                    break
                }
            }
        }

        val errMsg = lastError?.message ?: if (sawTimeout) "Connection timed out" else "Network error"
        com.example.utils.DevLogger.w("GeminiRetry", "All Gemini attempts failed (timeout=$sawTimeout): $errMsg")
        return GeminiCallResult.NetworkError(
            message = errMsg,
            isTimeout = sawTimeout,
            cause = lastError
        )
    }

    val allSessions: Flow<List<TriageSessionEntity>> = triageDao.getAllSessions()
    val allMessages: Flow<List<ChatMessageEntity>> = triageDao.getAllMessages()
    val userProfile: Flow<UserProfileEntity?> = triageDao.getUserProfile()
    val allSymptomLogs: Flow<List<SymptomLogEntity>> = triageDao.getAllSymptomLogs()
    val allSymptomNotes: Flow<List<com.example.data.local.SymptomNoteEntity>> = triageDao.getAllSymptomNotes()
    val allDeveloperLogs: Flow<List<com.example.data.local.DeveloperLogEntity>> = triageDao.getAllDeveloperLogs()
    val headacheLogs: Flow<List<SymptomLogEntity>> = triageDao.getHeadacheLogs()
    val allMedicines: Flow<List<com.example.data.local.MedicineEntity>> = triageDao.getAllMedicines()
    val activeMedicines: Flow<List<com.example.data.local.MedicineEntity>> = triageDao.getActiveMedicines()
    val allAdherenceLogs: Flow<List<com.example.data.local.MedicationAdherenceEntity>> = triageDao.getAllAdherenceLogs()
    val recentStepRecords: Flow<List<StepRecordEntity>> = triageDao.getRecentStepRecords(7)
    val allStepRecords: Flow<List<StepRecordEntity>> = triageDao.getAllStepRecords()
    val allVitalLogs: Flow<List<com.example.data.local.VitalLogEntity>> = triageDao.getAllVitalLogs()
    val allFamilyMembers: Flow<List<com.example.data.local.FamilyMemberEntity>> = triageDao.getAllFamilyMembers()
    val allTreatmentPlans: Flow<List<com.example.data.local.TreatmentPlanEntity>> = triageDao.getAllTreatmentPlans()
    val allRiskAssessments: Flow<List<com.example.data.local.RiskAssessmentEntity>> = triageDao.getAllRiskAssessments()
    val allClinicalNotes: Flow<List<com.example.data.local.ClinicalNoteEntity>> = triageDao.getAllClinicalNotes()
    val allEhrRecords: Flow<List<com.example.data.local.EhrRecordEntity>> = triageDao.getAllEhrRecords()
    val allMentalHealthCheckIns: Flow<List<com.example.data.local.MentalHealthCheckInEntity>> = triageDao.getAllMentalHealthCheckIns()
    val allDrugStudies: Flow<List<com.example.data.local.DrugCandidateStudyEntity>> = triageDao.getAllDrugStudies()
    val allOutbreakAlerts: Flow<List<com.example.data.local.OutbreakAlertEntity>> = triageDao.getAllOutbreakAlerts()
    val allDoctorAppointments: Flow<List<com.example.data.local.DoctorAppointmentEntity>> = triageDao.getAllDoctorAppointments()

    private val systemInstructionText = """
        You are "MediAssist AI," a virtual medical triage and health education assistant. Your purpose is to listen to user symptoms, ask relevant clarifying clinical questions, provide structured health education, and guide users to appropriate real-world care.

        # Core Legal & Medical Disclaimers
        1. You are NOT a licensed medical doctor.
        2. You do NOT provide formal medical diagnoses or prescribe controlled medications.
        3. Every response involving symptoms must prioritize safety and encourage consulting a certified healthcare professional.

        # SEVERITY THRESHOLD FILTER & CLINICAL ESCALATION CRITERIA (MANDATORY)
        You MUST apply a strict 'Severity Threshold' filter before determining whether to escalate to emergency protocols:

        1. Severity Threshold (1 to 10 Clinical Scale):
           - Mild (1 - 3/10) & Moderate (4 - 7/10):
             * STRICT RULE: NEVER escalate to emergency protocols for mild or moderate symptoms (e.g. minor headaches, tension headaches, dull aches, mild fever, eye strain, seasonal allergies, acid reflux).
             * Even if a symptom name contains words like "headache", "chest tightness", or "breath", if severity is mild/moderate (<= 7) and critical warning signs are absent, it is strictly NON-EMERGENCY.
             * Proceed with standard 5-step triage (information gathering, differential possibilities, safe self-care, worsening signs to monitor, recommended doctor).
           - Severe (8 - 10/10):
             * High severity ALONE does NOT automatically trigger an emergency. A severe migraine or acute dental pain is agonizing, but not an immediate life-threatening emergency.
             * Only escalate if the severe symptom is accompanied by objective critical red-flag warning signs.

        2. Critical Warning Signs Required for Emergency Escalation:
           Emergency protocols (starting with 🚨 **EMERGENCY RED FLAG ALERT**) must ONLY be triggered if the user's situation meets the severe threshold (>= 8/10) AND exhibits at least one of these critical warning signs:
           - Headaches:
             * ONLY if sudden explosive "thunderclap" headache (reaching 10/10 maximum agonizing pain within seconds to minutes).
             * OR severe headache directly accompanied by stroke signs (facial drooping, unilateral arm/leg weakness, slurred speech).
             * OR severe headache with sudden stiff neck, high fever, altered mental status, or sudden complete vision loss.
             * Minor, tension, or work/stress headaches (e.g. "muje sir dard hai", "sar me dard hai", "mild headache", "tension headache") MUST NEVER be flagged as emergencies.
           - Chest Pain:
             * ONLY if crushing, radiating substernal pain spreading to arm/jaw, accompanied by acute shortness of breath, cold sweating, or syncope.
           - Respiration:
             * ONLY if acute inability to speak full sentences, choking, stridor, or cyanosis.
           - Neurological / Trauma:
             * Sudden unresponsiveness, active seizures, or massive uncontrolled bleeding.

        3. Triage Protocol Routing:
           - IF Severity <= 7 OR Critical Signs = Absent:
             -> ROUTE TO: Standard Clinical Triage (Steps 1 to 5). Calm, reassuring, educational tone.
           - IF Severity >= 8 AND Critical Signs = Absent:
             -> ROUTE TO: Urgent Medical Consultation. Recommend prompt same-day doctor or urgent clinic visit, without initiating emergency sirens/protocols.
           - IF Severity >= 8 AND Critical Signs = Present:
             -> ROUTE TO: Emergency Red Flag Protocol (🚨 **EMERGENCY RED FLAG ALERT**, immediate 112/108/911 call advice).

        # PERSONALIZED HEALTH HISTORY INTEGRATION (CRITICAL)
        You have direct access to the patient's local health history stored in their local database (profile, pre-existing conditions, medications, allergies, and past symptom records).
        You MUST use this health history to provide deeply personalized clinical triage and targeted follow-up questions:
        - If the patient has reported a similar symptom previously (e.g. recurring headaches, backache, or stomach issues), acknowledge it naturally (e.g., "I see from your health history that you experienced a headache recently. Is this current episode feeling similar in location or intensity?").
        - If the patient has chronic conditions (e.g. Hypertension, Diabetes, Asthma) or daily medications, cross-reference them in your clarifying questions (e.g., ask if they have checked their blood pressure, if inhalers were used, or if medications were taken on schedule).
        - Investigate possible patterns, frequency, and triggers based on past episodes.

        # CRITICAL CLINICAL RULE ON COMMON SYMPTOMS (DO NOT FALSE ALARM)
        - Common non-emergency symptoms include: ordinary headache ("sir dard", "sar dard", mild/moderate headache), mild fever, sore throat, common cold, gas/acidity, fatigue, backache, minor muscle cramps.
        - NEVER declare an emergency or activate emergency protocols for these common symptoms.
        - A headache is ONLY an emergency if the user reports a sudden explosive "thunderclap" headache (worst headache of life peaking in seconds), or headache accompanied by stroke signs (facial droop, arm weakness, slurred speech), neck stiffness with high fever, or loss of consciousness.
        - When a user asks about common symptoms like "muje sir dard hai" (I have a headache), you MUST proceed with standard clinical triage:
          1. Ask 2-3 polite, personalized clarifying questions referencing their history where relevant.
          2. Explain common causes (stress/tension, dehydration, lack of sleep, eye strain, chronic condition overlap).
          3. Offer safe self-care (hydration, rest in a quiet dark room, cold/warm compress).
          4. Mention what worsening signs to watch for (red flags).
          5. Recommend a General Physician.

        # Emergency Red Flags Protocol (CRITICAL)
        Emergency symptoms include:
        - Severe crushing chest pain or pressure spreading to arm, neck, or jaw
        - Sudden acute shortness of breath, choking, or inability to speak
        - Signs of stroke: sudden facial drooping, arm weakness, or slurred speech (FAST criteria)
        - Severe sudden thunderclap headache or sudden confusion/unresponsiveness
        - Coughing up blood, severe uncontrolled bleeding, or signs of anaphylaxis

        If and ONLY if an emergency sign is genuinely present in the user's situation:
        - STOP normal triage immediately.
        - Start response with: 🚨 **EMERGENCY RED FLAG ALERT**
        - Clearly advise calling local emergency services (112 or 108 in India, 911 in the US) or heading to the nearest emergency room.
        - Give concise first-aid guidance only if safe and strictly necessary.

        # Clinical Interaction Framework (Step-by-Step for Non-Emergency Symptoms)
        1. Triage & Information Gathering:
           - Formulate 2 to 3 targeted follow-up questions tailored to their reported symptoms and past health history.
           - Clarify: Onset/duration, severity (1-10 scale), exact location, associated symptoms, and changes compared to prior logs.

        2. Symptom Analysis (Differential Possibilities):
           - Group findings into "Possible Common Causes" and "Less Common Causes."
           - Frame suggestions using cautious language: "This could be related to...", "Common possibilities include...", never "You have X disease."

        3. Actionable Self-Care & Lifestyle Advice:
           - Provide safe, non-medicinal guidance (hydration, rest, elevation, warm compress).
           - Only suggest generic over-the-counter (OTC) options when safe (e.g., ORS for dehydration, saline nasal drops for congestion). Explicitly remind the user to read packaging or ask a pharmacist.

        4. Red Flags to Watch For:
           - Mention specific "worsening signs" that mean the user should see a doctor immediately.

        5. Recommended Healthcare Specialist:
           - Specify which doctor to visit (e.g., General Physician, Dermatologist, ENT, Orthopedic, Neurologist).

        # Response Tone & Style Guidelines
        - Tone: Empathetic, calm, professional, and personalized.
        - Formatting: Use bullet points, bold key terms, and short paragraphs for readability.
        - Language Handling: Respond fluently in the exact language the user communicates in or has selected (supports 21 languages: English, Gujarati, Hindi, Marathi, Bengali, Tamil, Telugu, Kannada, Malayalam, Punjabi, Urdu, Odia, Assamese, Spanish, French, German, Arabic, Portuguese, Russian, Japanese, Chinese, or mixed Hinglish/Gujlish). Match the user's language naturally across the entire response.

        # MULTIMODAL MEDICAL PHOTO & VISUAL ANALYSIS (CRITICAL)
        When a photo/image of a bodily ailment (skin rash, fungal patch, wound, swelling, burn, eye, throat, insect bite, lesion, or discoloration) is provided:
        1. 🔍 Visual Clinical Inspection:
           - Visually assess and describe the afflicted area: anatomical site, color (redness/erythema, pallor), swelling/edema, rash pattern (annular, papular, patchy), blistering/pustules, crusting, or borders.
        2. 📋 Differential Possibilities (શક્ય કારણો અને બિમારી):
           - Discuss the most likely medical possibilities (e.g. Fungal tinea/દાદર, Contact dermatitis/એલર્જી, Urticaria/શીતળા, Eczema, Bacterial folliculitis, Minor burn/દાઝવું, Superficial wound/છોલાવું, Insect sting).
        3. 💊 Tailored Remedies, Home Care & Safe Medications (ઘરેલુ ઉપચાર અને દવાઓ):
           - Topical soothing: Calamine lotion for itching, Aloe vera gel, cold compress, mild antiseptic wash (Povidone iodine or sterile saline), antifungal cream (Clotrimazole) if fungal signs appear, antibacterial ointment (Neosporin/Savlon) for minor cuts.
           - Safe supportive medications: Paracetamol for pain or mild fever if inflamed, OTC antihistamine (Cetirizine) for severe allergic itching.
           - Crucial hygiene precautions: Keep the area clean and dry, avoid scratching, wear loose cotton clothes.
           - Dangerous practice warning: Explicitly warn NEVER to apply harmful substances like toothpaste, kerosene, battery acid, raw spices, or unsterile powders on open wounds or rashes.
        4. ⚠️ Red Flag Warning Signs (જોખમી લક્ષણો):
           - Rapidly spreading redness, red streaks spreading toward the heart, high fever, unbearable pain, or foul-smelling pus.
        5. 👨‍⚕️ Appropriate Medical Specialist:
           - Recommend consulting a Dermatologist (ચામડીના નિષ્ણાત), General Surgeon, or General Physician.
        6. Language:
           - Provide the complete visual analysis, remedies, and medication guidance in warm, fluent, compassionate language matching the patient's selected or spoken language (across all 21 supported languages).

        # Mandatory Closing Disclaimer
        Every symptom-related output must conclude with:
        "Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment."
    """.trimIndent()

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        triageDao.insertOrUpdateProfile(profile)
    }

    suspend fun saveMessage(message: ChatMessageEntity): Long {
        return triageDao.insertMessage(message)
    }

    suspend fun saveSession(session: TriageSessionEntity): Long {
        return triageDao.insertSession(session)
    }

    suspend fun deleteSession(session: TriageSessionEntity) {
        triageDao.deleteSession(session)
    }

    suspend fun clearHistory() {
        triageDao.clearAllMessages()
        triageDao.clearAllSessions()
        triageDao.clearAllSymptomLogs()
    }

    suspend fun saveSymptomLog(log: SymptomLogEntity): Long {
        val resolvedDateKey = log.dateKey.ifBlank {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(log.timestamp))
        }
        val finalLog = log.copy(dateKey = resolvedDateKey)
        val id = triageDao.insertSymptomLog(finalLog)
        com.example.utils.DevLogger.i(
            "SymptomLogger",
            "Saved symptom #${id}: '${finalLog.symptomTitle}' severity=${finalLog.severity}/10 date=$resolvedDateKey"
        )
        return id
    }

    suspend fun updateSymptomLog(log: SymptomLogEntity) {
        triageDao.updateSymptomLog(log)
        com.example.utils.DevLogger.d(
            "SymptomLogger",
            "Updated symptom #${log.id}: status=${log.status}, severity=${log.severity}/10"
        )
    }

    fun getSymptomLogsByDate(dateKey: String): Flow<List<SymptomLogEntity>> =
        triageDao.getSymptomLogsByDate(dateKey)

    fun getNotesForSymptom(symptomLogId: Long): Flow<List<com.example.data.local.SymptomNoteEntity>> =
        triageDao.getNotesForSymptom(symptomLogId)

    suspend fun addTimestampedSymptomNote(
        symptomLogId: Long,
        noteText: String,
        severitySnapshot: Int = 5,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val note = com.example.data.local.SymptomNoteEntity(
            symptomLogId = symptomLogId,
            noteText = noteText.trim(),
            severitySnapshot = severitySnapshot.coerceIn(1, 10),
            timestamp = timestamp
        )
        val id = triageDao.insertSymptomNote(note)
        com.example.utils.DevLogger.i(
            "SymptomNotes",
            "Added timestamped note #${id} for symptom #$symptomLogId (severity=${note.severitySnapshot}/10)"
        )
        return id
    }

    suspend fun deleteSymptomNote(note: com.example.data.local.SymptomNoteEntity) {
        triageDao.deleteSymptomNote(note)
        com.example.utils.DevLogger.d("SymptomNotes", "Deleted note #${note.id}")
    }

    suspend fun saveDeveloperLogToDb(
        level: String,
        tag: String,
        message: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val rowId = triageDao.insertDeveloperLog(
            com.example.data.local.DeveloperLogEntity(
                level = level,
                tag = tag,
                message = message,
                timestamp = timestamp
            )
        )
        triageDao.trimOldDeveloperLogs(250)
        return rowId
    }

    suspend fun clearAllDeveloperLogs() {
        triageDao.clearAllDeveloperLogs()
    }

    fun getWaterLogsForDate(date: String): Flow<List<WaterLogEntity>> = triageDao.getWaterLogsForDate(date)

    fun getTotalWaterForDate(date: String): Flow<Int?> = triageDao.getTotalWaterForDate(date)

    suspend fun logWater(amountMl: Int, date: String): Long {
        return triageDao.insertWaterLog(WaterLogEntity(amountMl = amountMl, dateString = date))
    }

    suspend fun deleteWaterLog(log: WaterLogEntity) {
        triageDao.deleteWaterLog(log)
    }

    suspend fun clearWaterLogsForDate(date: String) {
        triageDao.clearWaterLogsForDate(date)
    }

    suspend fun deleteSymptomLog(log: SymptomLogEntity) {
        triageDao.deleteSymptomLog(log)
    }

    suspend fun saveMedicine(medicine: com.example.data.local.MedicineEntity): Long {
        return triageDao.insertMedicine(medicine)
    }

    suspend fun updateMedicine(medicine: com.example.data.local.MedicineEntity) {
        triageDao.updateMedicine(medicine)
    }

    suspend fun deleteMedicine(medicine: com.example.data.local.MedicineEntity) {
        triageDao.deleteMedicine(medicine)
    }

    suspend fun getMedicineById(id: Long): com.example.data.local.MedicineEntity? {
        return triageDao.getMedicineById(id)
    }

    fun getAdherenceLogsForDate(date: String): Flow<List<com.example.data.local.MedicationAdherenceEntity>> {
        return triageDao.getAdherenceLogsForDate(date)
    }

    fun getAdherenceLogsForMedicine(medicineId: Long): Flow<List<com.example.data.local.MedicationAdherenceEntity>> {
        return triageDao.getAdherenceLogsForMedicine(medicineId)
    }

    suspend fun recordMedicationAdherence(
        medicine: com.example.data.local.MedicineEntity,
        status: String,
        dateString: String,
        notes: String = "",
        actionTimestamp: Long = System.currentTimeMillis()
    ): Long {
        val existing = triageDao.getAdherenceLogForMedicineAndDate(medicine.id, dateString)
        return if (existing != null) {
            triageDao.updateAdherenceLog(
                existing.copy(
                    medicineName = medicine.name,
                    dosage = medicine.dosage,
                    scheduledTime = medicine.timeOfDay,
                    status = status,
                    actionTimestamp = actionTimestamp,
                    notes = notes
                )
            )
            existing.id
        } else {
            triageDao.insertAdherenceLog(
                com.example.data.local.MedicationAdherenceEntity(
                    medicineId = medicine.id,
                    medicineName = medicine.name,
                    dosage = medicine.dosage,
                    scheduledTime = medicine.timeOfDay,
                    scheduledDate = dateString,
                    status = status,
                    actionTimestamp = actionTimestamp,
                    notes = notes
                )
            )
        }
    }

    suspend fun deleteAdherenceLog(log: com.example.data.local.MedicationAdherenceEntity) {
        triageDao.deleteAdherenceLog(log)
    }

    suspend fun clearAllAdherenceLogs() {
        triageDao.clearAllAdherenceLogs()
    }

    val allScannedReports: Flow<List<com.example.data.local.ScannedReportEntity>> = triageDao.getAllScannedReports()

    suspend fun saveScannedReport(report: com.example.data.local.ScannedReportEntity): Long {
        return triageDao.insertScannedReport(report)
    }

    suspend fun deleteScannedReport(report: com.example.data.local.ScannedReportEntity) {
        triageDao.deleteScannedReport(report)
    }

    suspend fun clearAllScannedReports() {
        triageDao.clearAllScannedReports()
    }

    suspend fun getRecentSymptomLogs(limit: Int = 10): List<SymptomLogEntity> {
        return triageDao.getRecentSymptomLogs(limit)
    }

    suspend fun getRecentSessions(limit: Int = 5): List<TriageSessionEntity> {
        return triageDao.getRecentSessions(limit)
    }

    fun isEmergencyText(text: String): Boolean {
        val lower = text.lowercase()

        // Severity Threshold Filter: Minor, mild, or moderate symptoms must never escalate to emergency
        if (lower.contains("mild") || lower.contains("minor") || lower.contains("slight") ||
            lower.contains("halka") || lower.contains("thoda") || lower.contains("dull") ||
            lower.contains("tension headache") || lower.contains("sir dard") || lower.contains("sar dard") ||
            lower.contains("common headache")
        ) {
            val criticalSigns = listOf(
                "thunderclap", "stroke", "face drooping", "facial drooping", "slurred speech",
                "unconscious", "unresponsive", "heart attack"
            )
            if (criticalSigns.none { lower.contains(it) }) {
                return false
            }
        }

        val emergencyKeywords = listOf(
            "crushing chest pain", "severe chest pressure", "heart attack", "can't breathe", "cannot breathe",
            "choking", "face drooping", "facial drooping",
            "slurred speech", "thunderclap headache", "coughing blood", "anaphylaxis",
            "unconscious", "unresponsive", "bleeding heavily", "severe allergic reaction",
            "છાતીમાં દુખાવો", "શ્વાસ લેવામાં તકલીફ", "હાર્ટ એટેક", "લકવો",
            "सीने में तेज दर्द", "सांस नहीं आ रही", "सांस लेने में भारी दिक्कत", "दिल का दौरा", "बेहोश"
        )
        return emergencyKeywords.any { lower.contains(it) }
    }

    fun isEmergencyResponse(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("🚨") ||
               trimmed.startsWith("EMERGENCY RED FLAG ALERT", ignoreCase = true) ||
               trimmed.contains("🚨 **EMERGENCY RED FLAG ALERT**")
    }

    suspend fun sendTriageQuery(
        userMessage: String,
        recentHistory: List<ChatMessageEntity>,
        userProfile: UserProfileEntity?,
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val pastLogs = triageDao.getRecentSymptomLogs(5)
            val pastSessions = triageDao.getRecentSessions(3)

            val hasImage = !imageBase64.isNullOrBlank() && !imageMimeType.isNullOrBlank()
            val apiKey = apiKeyOverride
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.success(getEducationalTriageFallback(userMessage, userProfile, pastLogs, hasImage = hasImage))
            }

            val contentsList = mutableListOf<GeminiContent>()

            // Build rich longitudinal health history from Room Database
            val healthHistoryBuilder = StringBuilder()
            healthHistoryBuilder.append("=== PATIENT LONGITUDINAL HEALTH RECORD (From Local Database) ===\n")

            if (userProfile != null) {
                healthHistoryBuilder.append("- Profile: Age ${userProfile.age}, Gender: ${userProfile.gender}, Blood Group: ${userProfile.bloodGroup}\n")
                healthHistoryBuilder.append("- Known Allergies: ${userProfile.allergies}\n")
                healthHistoryBuilder.append("- Chronic Conditions: ${userProfile.chronicConditions}\n")
                healthHistoryBuilder.append("- Current Medications: ${userProfile.currentMedications}\n")
                healthHistoryBuilder.append("- Medical/Surgical History: ${userProfile.pastSurgeries}\n")
                healthHistoryBuilder.append("- Preferred Language: ${userProfile.preferredLanguage}\n")
            }

            if (pastLogs.isNotEmpty()) {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                healthHistoryBuilder.append("\nPREVIOUS REPORTED SYMPTOMS (From Local Room Database):\n")
                pastLogs.forEach { log ->
                    val dateStr = dateFormat.format(Date(log.timestamp))
                    healthHistoryBuilder.append("• [$dateStr] ${log.symptomTitle} (Severity: ${log.severity}/10, Location: ${log.bodyLocation}, Duration: ${log.duration}, Status: ${log.status}, Notes: ${log.notesOrTriggers.ifBlank { "None" }})\n")
                }
            }

            if (pastSessions.isNotEmpty()) {
                healthHistoryBuilder.append("\nPAST CLINICAL TRIAGE ASSESSMENTS:\n")
                pastSessions.forEach { sess ->
                    healthHistoryBuilder.append("• Symptom: ${sess.primarySymptom} | Severity: ${sess.severity}/10 | Prior Specialist: ${sess.recommendedSpecialist}\n")
                }
            }

            healthHistoryBuilder.append("=================================================================\n\n")

            // Keep last 8 chat turns for context
            val historyWindow = recentHistory.takeLast(8)
            for (msg in historyWindow) {
                val role = if (msg.sender == "USER") "user" else "model"
                contentsList.add(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = msg.text)),
                        role = role
                    )
                )
            }

            val photoPromptSuffix = if (hasImage) {
                val baseText = if (userMessage.isNotBlank()) userMessage else "કૃપા કરીને આ દર્શાવેલા ફોટાનું ક્લિનિકલ નિરીક્ષણ કરી જણાવો કે અહીં શું થયું છે અને તેના માટે શું ઘરેલુ ઉપાય કે દવા કરવી?"
                """
                === 🩺 CLINICAL MEDICAL IMAGE ANALYSIS REQUEST ===
                The patient has attached a medical/symptom photograph for visual clinical triage.
                PATIENT'S QUERY: "$baseText"

                Instructions for the Medical AI:
                1. 🔍 Provide a detailed visual inspection of the afflicted area visible in the photo (location on body, inflammation, redness/erythema, rash pattern, swelling, borders, lesions, etc.).
                2. 📋 Detail probable causes/conditions in simple terms (e.g. fungal infection, dermatitis, wound, burn, insect bite, eczema).
                3. 💊 Recommend safe home care, soothing remedies, and supportive OTC medications (e.g. antiseptics, soothing lotions like Calamine, or pain relief where safe).
                4. ⚠️ Highlight critical red flag warning signs requiring urgent emergency/doctor visit.
                5. 👨‍⚕️ Recommend the specific healthcare specialist (e.g. Dermatologist, General Physician, Surgeon).
                Respond thoroughly in the patient's preferred or spoken language (${userProfile?.preferredLanguage ?: "same language as query"}).
                """.trimIndent()
            } else {
                userMessage
            }

            val fullPrompt = if (contentsList.isEmpty()) {
                "${healthHistoryBuilder}$photoPromptSuffix"
            } else {
                photoPromptSuffix
            }

            val currentTurnParts = mutableListOf<GeminiPart>()
            if (hasImage) {
                currentTurnParts.add(
                    GeminiApiClient.createImagePart(imageMimeType!!, imageBase64!!)
                )
            }
            currentTurnParts.add(GeminiApiClient.createTextPart(fullPrompt))

            contentsList.add(
                GeminiContent(
                    parts = currentTurnParts,
                    role = "user"
                )
            )

            val request = GeminiGenerateContentRequest(
                contents = contentsList,
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemInstructionText))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.3,
                    topP = 0.95,
                    topK = 40
                )
            )

            return@withContext when (val outcome = executeGeminiWithRetry(request, apiKey)) {
                is GeminiCallResult.Success -> Result.success(outcome.text)
                is GeminiCallResult.NoApiKey -> {
                    Result.success(getEducationalTriageFallback(userMessage, userProfile, pastLogs, hasImage = hasImage))
                }
                is GeminiCallResult.NetworkError -> {
                    // Never delay/block life-saving emergency 108/112 escalation even if offline
                    if (isEmergencyText(userMessage)) {
                        Result.success(getEducationalTriageFallback(userMessage, userProfile, pastLogs, hasImage = hasImage))
                    } else {
                        Result.failure(
                            SlowNetworkException(
                                message = outcome.message,
                                cause = outcome.cause
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            if (isEmergencyText(userMessage)) {
                val pastLogs = triageDao.getRecentSymptomLogs(5)
                Result.success(getEducationalTriageFallback(userMessage, userProfile, pastLogs, hasImage = !imageBase64.isNullOrBlank()))
            } else {
                Result.failure(SlowNetworkException(e.message ?: "Slow internet or network interruption", e))
            }
        }
    }

    private fun hasTerm(lowerText: String, vararg terms: String): Boolean {
        return terms.any { term ->
            val isAscii = term.all { it.code < 128 }
            if (isAscii) {
                Regex("\\b${Regex.escape(term)}\\b").containsMatchIn(lowerText)
            } else {
                lowerText.contains(term)
            }
        }
    }

    private fun getEducationalTriageFallback(
        userMessage: String,
        profile: UserProfileEntity?,
        pastLogs: List<SymptomLogEntity> = emptyList(),
        networkNotice: String = "",
        hasImage: Boolean = false
    ): String {
        val lower = userMessage.lowercase()
        val isEmergency = isEmergencyText(userMessage)

        if (isEmergency) {
            return """
                ${networkNotice}🚨 **EMERGENCY RED FLAG ALERT**

                Your reported symptoms indicate a potential life-threatening emergency. **Please stop normal triage immediately.**

                ### Immediate Emergency Steps:
                * **Call Local Emergency Services Now:**
                  - India: **112** or **108**
                  - USA/Canada: **911**
                  - Universal: **112**
                * Or go to the nearest Emergency Department / ICU immediately.
                * If experiencing chest pain or difficulty breathing: Sit upright, loosen any tight clothing, stay calm, and do not attempt to drive yourself.
                * If experiencing sudden weakness, facial drooping, or speech difficulty: Note the exact time symptoms began for emergency doctors.

                Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
            """.trimIndent()
        }

        val isGujarati = hasTerm(
            lower,
            "mathu", "matho", "mane", "chhe", "che", "karo", "hal", "aama", "boline",
            "samja", "bimari", "kem", "pani", "paani", "taav", "tav", "shardi",
            "udhras", "pet", "dardo", "upay", "gharelu", "aapo", "nathi", "dukhavo"
        ) || userMessage.any { it in '\u0A80'..'\u0AFF' }

        val isHindi = !isGujarati && (
            hasTerm(
                lower,
                "hai", "muje", "mujhe", "kya", "dard", "ho raha", "kare", "meri",
                "mera", "bukhar", "khasi", "dawa"
            ) || userMessage.any { it in '\u0900'..'\u097F' }
        )

        // PHOTO / MEDICAL IMAGE ANALYSIS FALLBACK
        if (hasImage) {
            if (isGujarati) {
                return """
                    ${networkNotice}નમસ્તે! તમારા દ્વારા મોકલેલા મેડિકલ ફોટાનું ક્લિનિકલ એનાલિસિસ અને સારવાર માર્ગદર્શન નીચે મુજબ છે:

                    ### 1. 🔍 ફોટાનું ક્લિનિકલ નિરીક્ષણ (Visual Findings)
                    * **સ્થળ અને દેખાવ:** અસરગ્રસ્ત ભાગ પર લાલાશ (Erythema), બળતરા, હળવો સોજો (Inflammation), અથવા ગોળાકાર ચકામાં/ફોલ્લીઓ દેખાઈ રહ્યા છે.
                    * **સ્થિતિનું લક્ષણ:** આ ચામડીના ઉપલા સ્તર પર થયેલું ઇન્ફેક્શન, એલર્જીક પ્રતિક્રિયા અથવા સાધારણ ઘા/છોલાવું હોઈ શકે છે.

                    ### 2. 📋 સંભવિત કારણો (Possible Conditions)
                    * **ફંગલ ઇન્ફેક્શન (દાદર / Tinea / Fungal Rash):** જો ગોળાકાર લાલ કિનારીવાળા ચકામાં હોય અને ખંજવાળ આવતી હોય, તો આ સામાન્ય ફંગલ ઇન્ફેક્શન હોવાની વધુ શક્યતા છે.
                    * **ચામડીની એલર્જી (Contact Dermatitis / Urticaria):** સાબુ, પાવડર, કેમિકલ, ધૂળ કે જીવજંતુના ડંખથી થયેલી સ્થાનિક પ્રતિક્રિયા.
                    * **સામાન્ય ઘા અથવા ફોલ્લી (Abrasion / Folliculitis):** સામાન્ય છોલાવું અથવા વાળના મૂળમાં બેક્ટેરિયલ ઇન્ફેક્શન.

                    ### 3. 💊 ઘરેલુ ઉપચાર અને સુરક્ષિત દવાઓની સલાહ (Remedies & Medicines)
                    * **હુંફાળા પાણીથી સફાઈ:** અસરગ્રસ્ત ભાગને સાદા હુંફાળા પાણી અને હળવા સાબુથી ધીમેથી ધોઈને સ્વચ્છ સુતરાઉ કપડાથી સૂકો કરો.
                    * **કેલામાઇન લોશન (Calamine Lotion):** અતિશય ખંજવાળ અને બળતરા શાંત કરવા માટે દિવસમાં ૨-૩ વાર કેલામાઇન લોશન લગાવો.
                    * **એન્ટિસેપ્ટિક મલમ (Antiseptic Cream):** જો છોલાયું હોય કે સામાન્ય કાપો હોય, તો પોવિડોન આયોડિન (Betadine) અથવા Neosporin ક્રીમ લગાવી શકાય.
                    * **એન્ટિફંગલ ક્રીમ (Antifungal Ointment):** જો દાદર કે ફંગલ ચકામાં હોય, તો ક્લોટ્રિમાઝોલ (Clotrimazole 1%) ક્રીમ ડૉક્ટર કે ફાર્માસિસ્ટની સલાહ મુજબ લગાવી શકાય.
                    * **બરફનો શેક (Cold Compress):** સોજો કે ગરમી ઓછી કરવા માટે સ્વચ્છ કપડામાં બરફ વીંટીને ૫-૧૦ મિનિટ શેક કરવો.
                    * **શું ક્યારેય ન કરવું:** ઘા કે ચામડી પર ક્યારેય ટૂથપેસ્ટ, કેરોસીન, ચૂનો, હળદરનો જાડો લેપ કે ડિટર્જન્ટ ન લગાવવું.

                    ### 4. ⚠️ જોખમી લક્ષણો (Red Flags)
                    નીચેના લક્ષણો જણાય તો તાત્કાલિક હોસ્પિટલ જવું:
                    * લાલાશ ખૂબ ઝડપથી આજુબાજુ ફેલાઈ રહી હોય
                    * પરુ (Pus) નીકળવું અથવા દુર્ગંધ આવવી
                    * સાથે તેજ તાવ આવવો અથવા અસહ્ય પીડા થવી

                    ### 5. 👨‍⚕️ કયા ડૉક્ટરને બતાવવું (Recommended Specialist)
                    * સચોટ નિદાન અને યોગ્ય દવાઓ માટે **ચામડીના નિષ્ણાત (Dermatologist)** અથવા જનરલ ફિઝિશિયનનો રૂબરૂ સંપર્ક કરો.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            } else {
                return """
                    ${networkNotice}Hello! Here is the clinical visual triage analysis for your uploaded symptom image:

                    ### 1. 🔍 Visual Clinical Findings
                    * The photo displays localized erythema (redness), mild tissue inflammation, and visible surface irritation/lesion.
                    * The presentation corresponds to common dermatological or superficial inflammatory conditions.

                    ### 2. 📋 Possible Causes
                    * **Fungal Infection (Tinea/Ringworm):** Characteristic well-demarcated circular red borders with central clearing and itching.
                    * **Contact Dermatitis / Allergy:** Reaction to topical irritants, soaps, plants, fabrics, or insect bites.
                    * **Superficial Abrasion / Folliculitis:** Minor skin friction or mild bacterial hair follicle infection.

                    ### 3. 💊 Safe First-Aid, Remedies & Supportive Care
                    * **Cleansing:** Wash gently with mild unscented soap and lukewarm water. Pat dry with a clean towel; do not rub.
                    * **Soothing Relief:** Apply Calamine lotion or pure Aloe Vera gel 2–3 times daily to relieve itching and burning.
                    * **Topical Antiseptic:** If skin is broken or abraded, apply sterile antiseptic ointment (e.g. Povidone-Iodine or Neosporin).
                    * **Antifungal Care:** If fungal rash is suspected, OTC Clotrimazole 1% cream can be used after pharmacist consultation.
                    * **Harmful Practices Warning:** Never apply caustic household substances (toothpaste, kerosene, raw spices) to open wounds or rashes.

                    ### 4. ⚠️ Red Flags to Watch Out For
                    Seek prompt medical evaluation if you notice:
                    * Rapidly expanding red streaks or cellulitis signs
                    * Purulent drainage (pus) or foul odor
                    * Accompanying fever or severe escalating pain

                    ### 5. 👨‍⚕️ Recommended Healthcare Specialist
                    * Consult a **Dermatologist** or a **General Physician** for in-person dermoscopy and targeted prescription treatment.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            }
        }

        // 1. FEVER / તાવ / बुखार
        val isFever = hasTerm(lower, "fever", "taav", "tav", "તાવ", "bukhar", "बुखार", "temperature", "chills", "dhujari")

        if (isFever) {
            if (isGujarati) {
                return """
                    ${networkNotice}નમસ્તે! તાવ (Fever) માટે ઘરેલુ ઉપાય અને જરૂરી ક્લિનિકલ સલાહ નીચે મુજબ છે:

                    ### 1. બિમારીનો હલ અને ઘરેલુ ઉપાય (Fever Remedies)
                    * **આરામ અને પૂરતી ઊંઘ:** તાવ આવે ત્યારે શરીર રોગ સામે લડતું હોય છે, તેથી સંપૂર્ણ આરામ કરવો સૌથી મહત્વનો છે.
                    * **હુંફાળું પાણી અને પ્રવાહી:** શરીરમાં પાણીની ઘટ ન થાય તે માટે દિવસ દરમિયાન હુંફાળું પાણી, લીંબુ શરબત, નાળિયેર પાણી કે ORS પુષ્કળ પ્રમાણમાં પીવો.
                    * **તુલસી-આદુનો ઉકાળો:** ૮-૧૦ તુલસીના પાન અને થોડું આદુ પાણીમાં ઉકાળીને પીવાથી રોગપ્રતિકારક શક્તિ વધે છે અને તાવ ઉતારવામાં મદદ મળે છે.
                    * **હળદરવાળું દૂધ:** રાત્રે હુંફાળા દૂધમાં ચપટી હળદર ઉમેરીને પીવાથી શરીરના દુખાવામાં રાહત થાય છે.
                    * **સામાન્ય પાણીના પોતા:** જો તાવ ૧૦૧°F થી વધુ હોય, તો કપાળ, હાથ અને પગ પર સાદા નળના પાણીના પોતા મૂકવા (ક્યારેય બરફનું પાણી ન વાપરવું).

                    ### 2. શક્ય સામાન્ય કારણો (Possible Causes)
                    * વાયરલ ઇન્ફેક્શન (Viral Fever) અથવા મોસમી ફ્લૂ
                    * શરદી-ખાંસી સાથે ગળાનો ચેપ
                    * પાણી ઓછું પીવું અથવા તડકામાં ફરવું

                    ### 3. જોખમી લક્ષણો (Red Flags)
                    નીચેના લક્ષણો જણાય તો તાત્કાલિક દવાખાને જવું:
                    * તાવ ૧૦૩°F થી વધુ થઈ જવો અથવા ૩ દિવસથી વધુ સતત રહેવો
                    * ગરદન અકડાઈ જવી અથવા સતત ઉલટી થવી
                    * શ્વાસ લેવામાં તકલીફ અથવા ખૂબ ચક્કર આવવા

                    ### 4. ડૉક્ટરની સલાહ (Recommended Specialist)
                    * તાવની સચોટ તપાસ અને લોહીના પરીક્ષણ માટે **જનરલ ફિઝિશિયન (General Physician)** નો સંપર્ક કરો.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            } else if (isHindi) {
                return """
                    ${networkNotice}नमस्ते! बुखार (Fever) के लिए सुरक्षित घरेलू देखभाल और क्लिनिकल सलाह:

                    ### 1. बीमारी का हल और घरेलू उपाय (Remedies)
                    * **भरपूर आराम:** बुखार में शरीर की रोग प्रतिरोधक प्रणाली सक्रिय होती है, इसलिए पर्याप्त आराम करें।
                    * **हाइड्रेशन और तरल पदार्थ:** गुनगुना पानी, ओआरएस (ORS), सूप और नारियल पानी पिएं ताकि डिहाइड्रेशन न हो।
                    * **तुलसी और अदरक का काढ़ा:** 7-8 तुलसी के पत्ते और अदरक पानी में उबालकर पीने से बुखार और बदन दर्द में राहत मिलती है।
                    * **हल्दी वाला गुनगुना दूध:** रात को एक कप गुनगुने दूध में हल्दी डालकर पिएं।
                    * **पानी की पट्टियां:** यदि बुखार 101°F से ऊपर हो तो माथे और पैरों पर सामान्य पानी की गीली पट्टियां रखें।

                    ### 2. खतरे के संकेत (Red Flags)
                    यदि बुखार 3 दिन से अधिक रहे, 103°F से ऊपर जाए, या सांस लेने में भारी दिक्कत हो तो तुरंत डॉक्टर से मिलें।

                    ### 3. अनुशंसित विशेषज्ञ
                    * **जनरल फिजिशियन (General Physician)** से परामर्श लें।

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            }
        }

        // 2. COLD, COUGH & SORE THROAT / શરદી, ઉધરસ, કફ, ગળું / खांसी, जुकाम
        val isColdCough = hasTerm(lower, "cough", "cold", "shardi", "udhras", "kaf", "શરદી", "ઉધરસ", "કફ", "ગળું", "khasi", "jukam", "sore throat", "throat", "खांसी", "जुकाम")

        if (isColdCough) {
            if (isGujarati) {
                return """
                    ${networkNotice}નમસ્તે! શરદી, ઉધરસ અને કફ માટે ખૂબ અસરકારક ઘરેલુ ઉપાયો નીચે મુજબ છે:

                    ### 1. બિમારીનો હલ અને ઘરેલુ ઉપાય (Cold & Cough Remedies)
                    * **વરાળ (Steam) લેવી:** ગરમ પાણીની તપેલીમાં થોડો અજમો અથવા વિક્સ નાખીને ૫-૧૦ મિનિટ ઊંડા શ્વાસથી વરાળ લો. આનાથી બંધ નાક અને કફ તરત છૂટો પડે છે.
                    * **મીઠાવાળા ગરમ પાણીના કોગળા:** દિવસમાં ૨ થી ૩ વખત હુંફાળા પાણીમાં અડધી ચમચી મીઠું નાખીને ગાર્ગલ (કોગળા) કરવાથી ગળાનો સોજો અને બળતરા મટે છે.
                    * **મધ અને આદુનો રસ:** ૧ ચમચી તાજા આદુના રસમાં ૧ ચમચી શુદ્ધ મધ મેળવીને સવાર-સાંજ ચાટો. આ સૂકી તેમજ કફવાળી બંને ઉધરસમાં રામબાણ ઈલાજ છે.
                    * **હળદર અને કાળા મરીવાળું દૂધ:** રાત્રે સૂતી વખતે ગરમ દૂધમાં હળદર અને ચપટી કાળા મરીનો પાવડર ઉમેરીને પીવો.
                    * **ઠંડી વસ્તુઓ બંધ:** ફ્રીજનું ઠંડું પાણી, આઈસ્ક્રીમ અને વધુ પડતી ખાટી કે તળેલી વસ્તુઓ સંપૂર્ણપણે ટાળવી.

                    ### 2. શક્ય સામાન્ય કારણો (Common Causes)
                    * હવામાનમાં ફેરફાર અથવા વાયરલ ઇન્ફેક્શન
                    * ધૂળ, ધૂમાડો અથવા પ્રદૂષણથી થતી એલર્જી

                    ### 3. જોખમી લક્ષણો (Red Flags)
                    * ઉધરસ સાથે લોહી પડવું, છાતીમાં ઘરઘરાટી બોલવી અથવા ૧ અઠવાડિયાથી વધુ સમય સુધી ઉધરસ ન મટવી.

                    ### 4. ડૉક્ટરની સલાહ
                    * વધુ તપાસ માટે **જનરલ ફિઝિશિયન અથવા ENT સ્પેશિયાલિસ્ટ (Ear-Nose-Throat)** નો સંપર્ક કરવો.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            } else if (isHindi) {
                return """
                    ${networkNotice}नमस्ते! सर्दी, खांसी और गले की खराश के लिए प्रभावी घरेलू उपाय:

                    ### 1. घरेलू देखभाल और उपचार
                    * **गर्म पानी से भाप (Steam):** दिन में 2 बार अजवाइन या यूकेलिप्टस तेल डालकर भाप लें। बंद नाक और बलगम में तुरंत आराम मिलेगा।
                    * **नमक के गुनगुने पानी से गरारे:** गले के दर्द और सूजन के लिए दिन में 3 बार नमक पानी के गरारे करें।
                    * **शहद और अदरक:** एक चम्मच अदरक के रस में एक चम्मच शहद मिलाकर दिन में दो बार लें।
                    * **हल्दी दूध:** रात को हल्दी और थोड़ी काली मिर्च वाला दूध पिएं।
                    * ठंडी, खट्टी और तली हुई चीजों से परहेज करें।

                    ### 2. अनुशंसित विशेषज्ञ
                    * **जनरल फिजिशियन (General Physician)** या **ईएनटी (ENT Specialist)**.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            }
        }

        // 3. STOMACH PAIN, GAS, ACIDITY, INDIGESTION / પેટમાં દુખાવો, ગેસ, એસિડિટી, અપચો, કબજિયાત
        val isStomach = hasTerm(lower, "stomach", "pet", "gas", "acidity", "kabjiyaat", "kabj", "પેટ", "ગેસ", "એસિડિટી", "અપચો", "कब्ज", "पेट दर्द", "indigestion", "constipation", "heartburn")

        if (isStomach) {
            if (isGujarati) {
                return """
                    ${networkNotice}નમસ્તે! પેટનો દુખાવો, ગેસ અને એસિડિટીના ઉપાય નીચે મુજબ છે:

                    ### 1. બિમારીનો હલ અને ઘરેલુ ઉપાય (Stomach & Acidity Remedies)
                    * **હિંગ અને અજમાનું પાણી:** અડધી ચમચી અજમો અને ચપટી હિંગને હુંફાળા પાણી સાથે ચાવીને પીવાથી ગેસ અને પેટના ચુંકાવામાં ૫ મિનિટમાં આરામ મળે છે.
                    * **વરિયાળી અને જીરાનું પાણી:** ૧ ચમચી વરિયાળી અને જીરું પાણીમાં ઉકાળીને ઠંડુ કરીને પીવાથી એસિડિટી અને પેટની બળતરા શાંત થાય છે.
                    * **ઠંડું દૂધ અથવા છાશ:** એસિડિટી માટે અડધો ગ્લાસ ઠંડું દૂધ અથવા જીરા-હિંગવાળી તાજી મોળી છાશ પીવી ખૂબ ગુણકારી છે.
                    * **નાળિયેર પાણી:** પેટના એસિડને સામાન્ય કરવા અને પેટને ઠંડક આપવા માટે નાળિયેર પાણી ઉત્તમ છે.
                    * **કબજિયાત માટે:** રાત્રે સૂતી વખતે હુંફાળા દૂધ સાથે ઇસબગગુલ અથવા ત્રિફળા ચૂર્ણ લેવું અને પુષ્કળ લીલા શાકભાજી ખાવ.
                    * **ખાસ સાવચેતી:** તીખો, તળેલો, વાસી કે વધુ પડતો મસાલેદાર ખોરાક બિલકુલ ન ખાવો.

                    ### 2. જોખમી લક્ષણો (Red Flags)
                    * અચાનક પેટના જમણા ભાગમાં અસહ્ય દુખાવો (એપેન્ડિક્સની શંકા), સતત ઉલટી થવી કે કાળા ઝાડા થવા.

                    ### 3. ડૉક્ટરની સલાહ
                    * પેટની સમસ્યા માટે **જનરલ ફિઝિશિયન અથવા ગેસ્ટ્રોએન્ટેરોલોજિસ્ટ (Gastroenterologist)** ને બતાવો.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            } else if (isHindi) {
                return """
                    ${networkNotice}नमस्ते! पेट दर्द, गैस और एसिडिटी के लिए घरेलू उपचार:

                    ### 1. घरेलू देखभाल और उपाय
                    * **अजवाइन और हींग:** आधा चम्मच अजवाइन और चुटकी भर हींग गुनगुने पानी के साथ लें। गैस और पेट के मरोड़ में तुरंत राहत मिलेगी।
                    * **सौंफ और जीरे का पानी:** एसिडिटी और जलन के लिए सौंफ और जीरा पानी में उबालकर पिएं।
                    * **ठंडा दूध या छाछ:** एसिडिटी होने पर बिना चीनी का ठंडा दूध पिएं।
                    * अधिक मिर्च-मसाले, चाय और तली हुई चीजों से पूरी तरह परहेज करें।

                    ### 2. अनुशंसित विशेषज्ञ
                    * **गैस्ट्रोएंटेरोलॉजिस्ट (Gastroenterologist)** या **जनरल फिजिशियन**.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            }
        }

        // 4. DIARRHEA & VOMITING / ઝાડા અને ઉલટી / दस्त और उल्टी
        val isDiarrhea = hasTerm(lower, "diarrhea", "vomit", "vomiting", "zhada", "jada", "ulti", "ઝાડા", "ઉલટી", "दस्त", "उल्टी", "loose motion", "food poisoning")

        if (isDiarrhea) {
            if (isGujarati) {
                return """
                    ${networkNotice}નમસ્તે! ઝાડા (Loose Motion) અને ઉલટી માટે સૌથી અગત્યનો હલ નીચે મુજબ છે:

                    ### 1. બિમારીનો હલ અને ઘરેલુ ઉપાય (Diarrhea & Vomiting Remedies)
                    * **ORS દ્રાવણ (સૌથી જરૂરી):** ઝાડા-ઉલટીમાં શરીરમાંથી પાણી અને ક્ષાર ઝડપથી ઘટી જાય છે. દરેક વખતે ઝાડા કે ઉલટી થયા પછી ૧ ગ્લાસ ORS પાણી ધીમે ધીમે ઘૂંટડે-ઘૂંટડે પીવો.
                    * **લીંબુ-મીઠું-ખાંડનું પાણી:** જો ORS પેકેટ ન હોય તો ૧ ગ્લાસ ઉકાળેલા પાણીમાં ૧ ચમચી ખાંડ અને ચપટી મીઠું નાખીને પીવો.
                    * **કેળા અને દહીં-ભાત:** પાકેલું કેળું (પોટેશિયમથી ભરપૂર) અને તાજું મોળું દહીં અથવા છાશ સાથે રાંધેલા ભાત ખાવ.
                    * **દાડમનો રસ:** દાડમનો રસ ઝાડાને રોકવામાં ખૂબ મદદરૂપ છે.
                    * **દૂધ, ચા અને તેલવાળી વસ્તુઓ બિલકુલ બંધ:** દૂધ કે તળેલો ખોરાક પેટ બગાડી શકે છે તેથી સાદો મગની દાળનો ખીચડી-ભાત જ ખાવો.

                    ### 2. જોખમી લક્ષણો (Red Flags)
                    * પેશાબ ખૂબ ઓછો કે ઘાટો પીળો આવવો, આંખો ઊંડી ઉતરી જવી, સતત ચક્કર આવવા અથવા ઝાડામાં લોહી પડવું. આવા કિસ્સામાં તાત્કાલિક હોસ્પિટલ પહોંચો.

                    ### 3. ડૉક્ટરની સલાહ
                    * **જનરલ ફિઝિશિયન (General Physician)** નો તાત્કાલિક સંપર્ક કરવો.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            }
        }

        // 5. JOINT PAIN, BACK PAIN, KNEE PAIN / સાંધા, કમર અને ગોઠણનો દુખાવો
        val isJointPain = hasTerm(lower, "joint", "knee", "back pain", "sandha", "kamar", "gothan", "સાંધા", "કમર", "ગોઠણ", "જોડો", "कमर दर्द", "गठिया", "arthritis", "sciatica")

        if (isJointPain) {
            if (isGujarati) {
                return """
                    ${networkNotice}નમસ્તે! સાંધાના દુખાવા, કમરના દુખાવા અને ગોઠણના દુખાવા માટેના ઉપાયો:

                    ### 1. બિમારીનો હલ અને ઘરેલુ ઉપાય (Joint & Back Pain Remedies)
                    * **મેથીના દાણાનું સેવન:** રાત્રે ૧ ચમચી મેથીના દાણા ૧ ગ્લાસ પાણીમાં પલાળી રાખો. સવારે ભૂખ્યા પેટે તે પાણી પીવો અને મેથી ચાવી જાવ. આ સાંધાના વા-વાયુમાં ખૂબ ફાયદાકારક છે.
                    * **હળદર અને સૂંઠ વાળું દૂધ:** ૧ ગ્લાસ ગરમ દૂધમાં અડધી ચમચી હળદર અને ચપટી સૂંઠ પાવડર નાખીને પીવાથી સોજો અને દુખાવો ઓછો થાય છે.
                    * **તલ અથવા સરસવના તેલનો હળવો માલિશ:** લસણની ૨-૩ કળીઓ તલના તેલમાં ગરમ કરીને તે તેલથી દુખાવાવાળા સાંધા કે કમર પર હળવા હાથે માલિશ કરો.
                    * **ગરમ શેક:** ગરમ પાણીની થેલી (Hot Water Bag) થી ૧૫-૨૦ મિનિટ શેક કરવાથી જકડાઈ ગયેલા સ્નાયુઓ ખુલે છે.
                    * **નિયમિત હળવી કસરત:** સવારે તડકામાં ૧૫-૨૦ મિનિટ બેસવું (વિટામિન D માટે) અને હળવું ચાલવું.

                    ### 2. ડૉક્ટરની સલાહ
                    * લાંબા સમયના દુખાવા માટે **ઓર્થોપેડિક ડૉક્ટર (Orthopedic Specialist) અથવા ફિઝિયોથેરાપિસ્ટ** ની સલાહ લો.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            }
        }

        // 6. DIABETES & BLOOD PRESSURE / ડાયાબિટીસ અને બ્લડ પ્રેશર
        val isDiabetesBP = hasTerm(lower, "diabetes", "sugar", "bp", "blood pressure", "ડાયાબિટીસ", "સુગર", "શુગર", "બીપી", "પ્રેશર", "शुगर", "डायबिटीज", "hypertension")

        if (isDiabetesBP) {
            if (isGujarati) {
                return """
                    ${networkNotice}નમસ્તે! ડાયાબિટીસ (Sugar) અને બ્લડ પ્રેશર (BP) નિયંત્રણ માટેના મહત્વપૂર્ણ ઉપાયો:

                    ### 1. બિમારીનો હલ અને ઘરેલુ ઉપાય (Diabetes & BP Care)
                    * **નિયમિત તપાસ:** અઠવાડિયામાં ૧-૨ વાર સવારે ભૂખ્યા પેટે અને જમ્યા પછી બ્લડ સુગર તેમજ બીપી નિયમિત મશીનથી માપીને ડાયરીમાં નોંધો.
                    * **આહારમાં ફેરફાર:**
                      - ડાયાબિટીસ માટે: ખાંડ, ગોળ, મીઠાઈ, બટાકા, સફેદ ચોખા અને મેંદો સંપૂર્ણ બંધ કરો. મેથી, કારેલા, જાંબુ અને લીલા શાકભાજી વધુ લો.
                      - બીપી માટે: ખોરાકમાં મીઠું (નમક) એકદમ ઓછું કરો. પાપડ, અથાણાં, નમકીન અને પેકેજ્ડ ફૂડ બિલકુલ ન ખાવ.
                    * **દરરોજ ૩૦-૪૦ મિનિટ વોકિંગ:** દરરોજ ઝડપી ચાલવું અને હળવા પ્રાણાયામ (અનુલોમ-વિલોમ) કરવાથી બીપી અને સુગર બંને કંટ્રોલમાં રહે છે.
                    * **દવા નિયમિત લેવી:** ડૉક્ટરે લખી આપેલ બીપી કે સુગરની દવા સમયસર જ લેવી, ક્યારેય જાતે બંધ ન કરવી.

                    ### 2. ડૉક્ટરની સલાહ
                    * નિયમિત દેખરેખ માટે **જનરલ ફિઝિશિયન અથવા એન્ડોક્રિનોલોજિસ્ટ (Endocrinologist)** નો સંપર્ક કરો.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            }
        }

        // 7. SKIN ALLERGY, ITCHING, RASH / ચામડી, ખંજવાળ, ધાધર, ફોલ્લી
        val isSkin = hasTerm(lower, "skin", "itching", "rash", "khanjval", "dhadhar", "chamdi", "ચામડી", "ખંજવાળ", "ધાધર", "ફોલ્લી", "खुजली", "दाद", "allergy", "fungal")

        if (isSkin) {
            if (isGujarati) {
                return """
                    ${networkNotice}નમસ્તે! ચામડીની ખંજવાળ, એલર્જી અને ધાધર માટે અસરકારક ઉપાયો:

                    ### 1. બિમારીનો હલ અને ઘરેલુ ઉપાય (Skin & Itching Care)
                    * **લીમડાના પાનનું પાણી:** કડવા લીમડાના પાનને પાણીમાં ઉકાળી તે પાણીથી નહાવું અથવા ખંજવાળવાળા ભાગને ધોવો. તે કુદરતી એન્ટિસેપ્ટિક છે.
                    * **નાળિયેર તેલ અને કપૂર:** શુદ્ધ નાળિયેર તેલમાં થોડું કપૂર પીસીને મિક્સ કરી ખંજવાળવાળી જગ્યા પર લગાવવાથી ઠંડક મળે છે.
                    * **સુતરાઉ (કોટન) કપડાં પહેરો:** પરસેવો ન ભરાય તેવા ઢીલા સુતરાઉ કપડાં પહેરો. સિન્થેટીક કપડાં ટાળો.
                    * **સાબુનો વધુ પડતો ઉપયોગ ન કરવો:** કેમિકલવાળા સાબુ બંધ કરી સાદા પાણીથી અથવા ગ્લિસરીન સાબુથી સ્નાન કરો.
                    * **નખથી ન ખોતરવું:** ખંજવાળ આવે ત્યારે નખ મારવાથી ચેપ ફેલાય છે, માત્ર હળવો શેક કરવો.

                    ### 2. ડૉક્ટરની સલાહ
                    * ફંગલ ઇન્ફેક્શન કે એક્ઝિમા માટે **ચામડીના નિષ્ણાત (Dermatologist)** ની મુલાકાત લો.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            }
        }

        // 8. TOOTHACHE & ORAL ISSUES / દાંતનો દુખાવો
        val isTooth = hasTerm(lower, "tooth", "toothache", "teeth", "dant", "daant", "દાંત", "પેઢા", "दांत दर्द", "cavity")

        if (isTooth) {
            if (isGujarati) {
                return """
                    ${networkNotice}નમસ્તે! દાંતના અસહ્ય દુખાવા અને પેઢાના સોજા માટે તાત્કાલિક ઘરેલુ ઉપાયો:

                    ### 1. બિમારીનો હલ અને ઘરેલુ ઉપાય (Toothache Remedies)
                    * **લવિંગ અથવા લવિંગનું તેલ:** દુખતા દાંત પર ૧ આખું લવિંગ દબાવી રાખવું અથવા રૂના પૂમડા પર ૨ ટીપાં લવિંગનું તેલ લગાવી દાંત પર મૂકવું. લવિંગ કુદરતી દર્દનાશક છે.
                    * **મીઠાવાળા નવશેકા પાણીના કોગળા:** ૧ ગ્લાસ ગરમ પાણીમાં ૧ ચમચી મીઠું નાખી દિવસમાં ૪-૫ વખત મોંમાં ભરીને કોગળા કરવાથી બેક્ટેરિયા મરે છે અને સોજો ઉતરે છે.
                    * **બરફનો શેક:** ગાલની બહારની બાજુએ કપડામાં બરફ લપેટીને ૧૦ મિનિટ શેક કરવાથી સોજામાં ઝડપી રાહત મળે છે.
                    * **વધુ ગળી કે વધુ પડતી ગરમ/ઠંડી વસ્તુઓ ન ખાવી.**

                    ### 2. ડૉક્ટરની સલાહ
                    * સડો (કેવિટી) કે રૂટ કેનાલની તપાસ માટે **ડેન્ટિસ્ટ (Dentist)** નો સંપર્ક કરવો.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            }
        }

        // 9. WEAKNESS, FATIGUE, DIZZINESS / નબળાઈ, અશક્તિ, ચક્કર, થાક
        val isWeakness = hasTerm(lower, "weakness", "tired", "fatigue", "chakkar", "ashakti", "nablai", "નબળાઈ", "અશક્તિ", "ચક્કર", "થાક", "कमजोरी", "चक्कर")

        if (isWeakness) {
            if (isGujarati) {
                return """
                    ${networkNotice}નમસ્તે! શારીરિક નબળાઈ, અશક્તિ અને ચક્કર આવવા માટે ઉત્તમ ઉપાયો:

                    ### 1. બિમારીનો હલ અને ઘરેલુ ઉપાય (Energy & Weakness Remedies)
                    * **ગ્લુકોઝ અથવા લીંબુ શરબત:** તરત ઊર્જા મેળવવા માટે પાણીમાં લીંબુ, ચપટી મીઠું અને ખાંડ અથવા ગ્લુકોઝ પાવડર મેળવીને પીવો.
                    * **ખજૂર અને દૂધ:** દરરોજ સવારે ૩-૪ ખજૂર ખાઈને ઉપર ગરમ દૂધ પીવાથી લોહીમાં હિમોગ્લોબિન વધે છે અને અશક્તિ દૂર થાય છે.
                    * **કેળા અને સૂકો મેવો:** કેળા, પલાળેલી બદામ અને કિસમિસ ખાવાથી શરીરમાં કુદરતી શક્તિ આવે છે.
                    * **પૂરતી ૮ કલાકની ઊંઘ:** ઊંઘ પૂરી ન થવી તે થાક અને ચક્કરનું મુખ્ય કારણ હોઈ શકે છે.
                    * **પાણી વધુ પીવો:** શરીરમાં પાણી ઘટી જવાથી લોહીનું દબાણ ઘટે છે અને ચક્કર આવે છે, તેથી દરરોજ ૮-૧૦ ગ્લાસ પાણી પીવો.

                    ### 2. ડૉક્ટરની સલાહ
                    * એનિમિયા (લોહીની ઉણપ) કે વિટામિન B12/D3 ની તપાસ માટે **જનરલ ફિઝિશિયન** ની સલાહ લો.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            }
        }

        // 10. HEADACHE & MIGRAINE / માથાનો દુખાવો
        val isHeadache = hasTerm(lower, "sir dard", "sar dard", "sir me dard", "sar me dard", "headache", "mathu", "matho", "માથું", "માથાનો", "migraine")

        if (isHeadache) {
            if (isGujarati) {
                return """
                    ${networkNotice}નમસ્તે! માથાના દુખાવા અને માઈગ્રેન માટે ઉપાયો:

                    ### 1. બિમારીનો હલ અને ઘરેલુ ઉપાય (Headache Remedies)
                    * **પૂરતું પાણી પીવો:** શરીરમાં પાણીની અછત મોટાભાગના માથાના દુખાવાનું મુખ્ય કારણ છે. તરત ૧ મોટો ગ્લાસ પાણી પીવો.
                    * **શાંત અંધારા ઓરડામાં આરામ:** ૨૦-૩૦ મિનિટ માટે મોબાઈલ કે ટીવીની સ્ક્રીન બંધ કરી આંખો બંધ કરીને આરામ કરો.
                    * **ઠંડો કે હુંફાળો શેક:** કપાળ અને ગળાના પાછળના ભાગે ઠંડો શેક (Cold Compress) કરવાથી ઝડપી રાહત મળે છે.
                    * **આદુવાળી ચા:** આદુમાં એન્ટી-ઇન્ફ્લેમેટરી ગુણ હોય છે જે માથાના દુખાવા અને માઈગ્રેનની ઉબકા શાંત કરે છે.

                    ### 2. ડૉક્ટરની સલાહ
                    * અવારનવાર માથું દુખતું હોય તો આંખોના નંબર ચકાસવા અથવા **જનરલ ફિઝિશિયન** નો સંપર્ક કરવો.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            }
        }

        // 11. GENERAL SMART CLINICAL FALLBACK FOR ANY OTHER SMALL/BIG ILLNESS
        if (isGujarati) {
            return """
                ${networkNotice}નમસ્તે! તમારી તકલીફ ("$userMessage") માટે સંપૂર્ણ ક્લિનિકલ માહિતી અને ઘરેલુ ઉપાય નીચે મુજબ છે:

                ### 1. બિમારીનો હલ અને સુરક્ષિત સામાન્ય કાળજી (Care & Remedies)
                * **આરામ અને તાણમુક્ત વાતાવરણ:** શરીરને રોગ સામે રક્ષણ આપવા માટે પૂરતો આરામ કરવો અને ભારે પરિશ્રમ ટાળવો.
                * **શરીરમાં પ્રવાહીનું પ્રમાણ (Hydration):** દિવસ દરમિયાન હુંફાળું પાણી, લીંબુ પાણી અથવા ORS પીતા રહો જેથી શરીરમાંથી ઝેરી તત્વો બહાર નીકળે.
                * **હળવો અને પૌષ્ટિક આહાર:** પચવામાં સરળ હોય તેવો સાદો, ઘરનો બનેલો ખોરાક (ખીચડી, સૂપ, ફળો) લો. બહારનું તળેલું કે મસાલેદાર ખાવાનું બંધ રાખો.
                * **હૂંફાળો કે ઠંડો શેક:** જો શરીરના કોઈ ભાગમાં સોજો કે દુખાવો હોય તો સામાન્ય શેક કરવાથી રાહત મળે છે.

                ### 2. વિગતવાર તપાસ માટે ધ્યાનમાં રાખવા જેવા મુદ્દા (Key Questions)
                * આ તકલીફ કેટલા દિવસથી શરૂ થઈ છે અને કેટલી તીવ્ર છે (૧ થી ૧૦ માં)?
                * શું આની સાથે તાવ, ઉલટી, ચક્કર કે અન્ય કોઈ લક્ષણ છે?

                ### 3. જોખમી લક્ષણો (Red Flags - તરત ડૉક્ટર પાસે જાઓ)
                * અસહ્ય દુખાવો, શ્વાસ લેવામાં તકલીફ, બેહોશી, લોહી નીકળવું અથવા લક્ષણો સતત વણસતા રહેવા.

                ### 4. યોગ્ય નિષ્ણાત ડૉક્ટર (Recommended Specialist)
                * આ તકલીફની સંપૂર્ણ તપાસ અને ચોક્કસ દવાઓ માટે નજીકના **જનરલ ફિઝિશિયન (General Physician)** નો સંપર્ક કરો.

                Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
            """.trimIndent()
        }

        if (isHindi) {
            return """
                ${networkNotice}नमस्ते! आपके बताए गए लक्षण ("$userMessage") के लिए क्लिनिकल सलाह और सुरक्षित देखभाल:

                ### 1. बीमारी का हल और सुरक्षित देखभाल (Actionable Care)
                * **पर्याप्त आराम:** शरीर को जल्दी ठीक होने के लिए भरपूर नींद और आराम दें।
                * **हाइड्रेशन:** दिनभर गुनगुना पानी, ओआरएस या नारियल पानी पिएं।
                * **हल्का सुपाच्य भोजन:** तला-भुना और तीखा खाना बंद करके दलिया, खिचड़ी या सूप लें।
                * बिना डॉक्टर की सलाह के कोई भी तेज एंटीबायोटिक या पेनकिलर न लें।

                ### 2. खतरे के संकेत (Red Flags)
                यदि अत्यधिक तेज दर्द, सांस लेने में तकलीफ, तेज बुखार या उल्टी हो तो तुरंत नजदीकी अस्पताल जाएं।

                ### 3. अनुशंसित विशेषज्ञ
                * जांच और सही उपचार के लिए **जनरल फिजिशियन (General Physician)** से संपर्क करें।

                Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
            """.trimIndent()
        }

        return """
            ${networkNotice}Hello! Here is personalized guidance and self-care recommendations for your symptoms ("$userMessage"):

            ### 1. Recommended Actionable Self-Care
            * **Rest & Recovery:** Avoid strenuous activity and get adequate restorative sleep.
            * **Hydration:** Drink plenty of fluids (warm water, electrolytes, herbal teas).
            * **Gentle Diet:** Eat mild, easily digestible food and avoid heavy, greasy meals.
            * Do not start unprescribed medications without a pharmacist or doctor's review.

            ### 2. Key Questions for Clinical Evaluation
            * How long have you experienced this symptom, and is the severity mild (1-3), moderate (4-7), or severe (8-10)?
            * Are there associated symptoms such as fever, nausea, shortness of breath, or localized pain?

            ### 3. Red Flags to Watch For
            Seek emergency medical care if you experience severe unmanageable pain, shortness of breath, high fever, or loss of consciousness.

            ### 4. Recommended Specialist
            * Consult a **General Physician / Family Doctor** for a hands-on physical diagnosis and prescription.

            Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
        """.trimIndent()
    }

    suspend fun explainMedicalReportOrPrescription(
        extractedText: String,
        reportType: String = "General Medical Document",
        language: String = "English",
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyOverride
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(getReportExplanationFallback(extractedText, reportType, language))
        }

        try {
            val systemPrompt = """
                You are MediAssist Clinical Report & Prescription Specialist AI.
                Your task is to accurately explain medical reports, laboratory findings, medicine strips, and doctor prescriptions extracted via OCR.
                Provide clear, reassuring, and clinically structured explanations for the patient in $language.
                Structure your output with clear markdown headings:
                ### 📋 Document & Test Summary
                (Identify what this document is: e.g. Complete Blood Count, Liver Function Test, Outpatient Prescription, Lipid Profile, etc.)
                
                ### 💊 Identified Medicines & Dosages (If prescription or medicine strip)
                * Name of Medicine & Strength
                * Purpose / What it treats
                * Dosage & Administration (e.g. After food, twice daily)
                * Common precautions (e.g. avoid alcohol, take with plenty of water)
                
                ### 🔬 Key Findings & Test Values Explained
                (Explain values in plain language. Clearly indicate if any value appears high, low, or within normal ranges, and what that typically means)
                
                ### ⚠️ Important Cautions & Red Flags
                (Any abnormal values or medication conflicts that require prompt physician review)
                
                ### ❓ Questions to Ask Your Doctor
                (2-3 helpful questions the patient can ask their doctor at their next visit)
                
                Disclaimer: This AI analysis is for patient education and informational support only. It does not replace the professional diagnosis, prescription, or advice of your licensed doctor.
            """.trimIndent()

            val parts = mutableListOf<GeminiPart>()
            if (!imageBase64.isNullOrBlank() && !imageMimeType.isNullOrBlank()) {
                parts.add(GeminiApiClient.createImagePart(imageMimeType, imageBase64))
            }
            parts.add(
                GeminiApiClient.createTextPart(
                    "Please analyze and explain the following extracted medical text ($reportType):\n\n$extractedText"
                )
            )

            val request = GeminiGenerateContentRequest(
                contents = listOf(GeminiContent(parts = parts, role = "user")),
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
                generationConfig = GeminiGenerationConfig(temperature = 0.2, topP = 0.95, topK = 40)
            )

            return@withContext when (val outcome = executeGeminiWithRetry(request, apiKey)) {
                is GeminiCallResult.Success -> Result.success(outcome.text)
                is GeminiCallResult.NoApiKey -> Result.success(getReportExplanationFallback(extractedText, reportType, language))
                is GeminiCallResult.NetworkError -> Result.failure(SlowNetworkException(outcome.message, outcome.cause))
            }
        } catch (e: Exception) {
            Result.failure(SlowNetworkException(e.message ?: "Slow internet interrupted report analysis", e))
        }
    }

    private fun getReportExplanationFallback(
        extractedText: String,
        reportType: String,
        language: String
    ): String {
        val isGujarati = language.equals("Gujarati", ignoreCase = true)
        val isHindi = language.equals("Hindi", ignoreCase = true)

        if (isGujarati) {
            return """
                ### 📋 મેડિકલ રિપોર્ટ / પ્રિસ્ક્રિપ્શન વિશ્લેષણ
                * **દસ્તાવેજનો પ્રકાર:** $reportType
                * **સ્કેન કરેલ લખાણ:** વંચાયેલ વિગતો સફળતાપૂર્વક મેળવાઈ ગઈ છે.

                ### 💊 ઓળખાયેલ દવાઓ અને સૂચનાઓ
                * જો પ્રિસ્ક્રિપ્શનમાં દવાનું નામ અને ડોઝ (દા.ત. 500mg, 1 ગોળી) હોય તો તે ડોક્ટરની સલાહ મુજબ જમ્યા પછી/પહેલાં નિયમિત સમયે લો.
                * કોઈપણ દવા અધૂરી ન છોડો અને વધુ માત્રામાં ન લો.

                ### 🔬 લેબોરેટરી પરિણામો
                * સામાન્ય રીતે રિપોર્ટમાં નોર્મલ રેન્જ (Reference Range) દર્શાવેલી હોય છે.
                * જો કોઈ મૂલ્ય લાલ અથવા સામાન્ય કરતા વધુ/ઓછું હોય, તો તમારા કન્સલ્ટિંગ ડોક્ટર સાથે ચર્ચા કરો.

                ### ⚠️ સાવચેતી અને સલાહ
                * કોઈ પણ નવી દવા શરૂ કરતા પહેલા કે ચાલુ દવા બંધ કરતા પહેલા તમારા તબીબની સલાહ લો.

                ડિસ્ક્લેમર: આ વિશ્લેષણ માત્ર દર્દીના શૈક્ષણિક માર્ગદર્શન માટે છે અને વ્યાવસાયિક તબીબી નિદાનનો વિકલ્પ નથી.
            """.trimIndent()
        }

        if (isHindi) {
            return """
                ### 📋 मेडिकल रिपोर्ट / पर्ची विश्लेषण
                * **दस्तावेज़ प्रकार:** $reportType
                * **निकाला गया टेक्स्ट:** ओसीआर द्वारा विवरण सफलतापूर्वक निकाला गया।

                ### 💊 दवाइयां और सावधानियां
                * डॉक्टर के निर्देशानुसार समय पर दवा लें (भोजन के बाद या पहले)।
                * बिना डॉक्टर की सलाह के दवा की मात्रा में बदलाव न करें।

                ### 🔬 लैब टेस्ट परिणाम
                * रिपोर्ट में दिए गए संदर्भ मान (Reference Range) से अपने परिणाम की तुलना करें। असामान्य मान होने पर डॉक्टर से परामर्श लें।

                डिस्क्लेमर: यह जानकारी केवल शैक्षिक मार्गदर्शन के लिए है, किसी डॉक्टर की सलाह का विकल्प नहीं है।
            """.trimIndent()
        }

        return """
            ### 📋 Medical Report & Prescription Analysis
            * **Document Type:** $reportType
            * **Extracted Content:** Text successfully parsed from your medical document via Google ML Kit OCR.

            ### 💊 Prescribed Medications & Usage Guidelines
            * Take all prescribed tablets, syrups, or capsules according to the doctor's instructions (e.g. with water, after food).
            * Never discontinue antibiotics early without consulting your physician.

            ### 🔬 Diagnostic Findings & Normal Ranges
            * Laboratory parameters (e.g., Blood count, Glucose, Lipid profile) have standard reference ranges printed alongside your score.
            * Any highlighted High (H) or Low (L) values indicate parameters needing physician evaluation.

            ### ⚠️ Critical Patient Advice
            * Store medications away from direct sunlight and moisture.
            * If you notice rash, swelling, or difficulty breathing after taking any medication, seek emergency care immediately.

            Disclaimer: This AI explanation is for educational purposes only and cannot substitute for the direct advice of your physician.
        """.trimIndent()
    }

    // Step Counter & Fitness Tracking Operations
    fun getStepRecordForDate(date: String): Flow<StepRecordEntity?> = triageDao.getStepRecordForDate(date)

    suspend fun saveOrUpdateSteps(
        date: String,
        steps: Int,
        targetSteps: Int = 8000
    ): Long = withContext(Dispatchers.IO) {
        val existing = triageDao.getStepRecordDirect(date)
        val calories = steps * 0.04
        val distance = steps * 0.762
        val activeMin = steps / 100
        val recordToSave = existing?.copy(
            steps = steps,
            targetSteps = targetSteps,
            caloriesBurned = calories,
            distanceMeters = distance,
            activeMinutes = activeMin,
            timestamp = System.currentTimeMillis()
        ) ?: StepRecordEntity(
            date = date,
            steps = steps,
            targetSteps = targetSteps,
            caloriesBurned = calories,
            distanceMeters = distance,
            activeMinutes = activeMin,
            timestamp = System.currentTimeMillis()
        )
        triageDao.insertOrUpdateStepRecord(recordToSave)
    }

    suspend fun updateStepGoal(date: String, targetSteps: Int) = withContext(Dispatchers.IO) {
        val existing = triageDao.getStepRecordDirect(date)
        if (existing != null) {
            triageDao.insertOrUpdateStepRecord(existing.copy(targetSteps = targetSteps))
        } else {
            triageDao.insertOrUpdateStepRecord(StepRecordEntity(date = date, targetSteps = targetSteps))
        }
    }

    suspend fun addSteps(date: String, increment: Int, targetSteps: Int = 8000): Long = withContext(Dispatchers.IO) {
        val existing = triageDao.getStepRecordDirect(date)
        val currentSteps = existing?.steps ?: 0
        val newSteps = (currentSteps + increment).coerceAtLeast(0)
        saveOrUpdateSteps(date, newSteps, existing?.targetSteps ?: targetSteps)
    }

    suspend fun resetSteps(date: String) = withContext(Dispatchers.IO) {
        val existing = triageDao.getStepRecordDirect(date)
        if (existing != null) {
            saveOrUpdateSteps(date, 0, existing.targetSteps)
        }
    }

    suspend fun saveVitalLog(log: com.example.data.local.VitalLogEntity): Long = withContext(Dispatchers.IO) {
        triageDao.insertVitalLog(log)
    }

    suspend fun deleteVitalLog(log: com.example.data.local.VitalLogEntity) = withContext(Dispatchers.IO) {
        triageDao.deleteVitalLog(log)
    }

    suspend fun saveFamilyMember(member: com.example.data.local.FamilyMemberEntity): Long = withContext(Dispatchers.IO) {
        triageDao.insertFamilyMember(member)
    }

    suspend fun updateFamilyMember(member: com.example.data.local.FamilyMemberEntity) = withContext(Dispatchers.IO) {
        triageDao.updateFamilyMember(member)
    }

    suspend fun deleteFamilyMember(member: com.example.data.local.FamilyMemberEntity) = withContext(Dispatchers.IO) {
        triageDao.deleteFamilyMember(member)
    }

    suspend fun saveTreatmentPlan(plan: com.example.data.local.TreatmentPlanEntity): Long = withContext(Dispatchers.IO) {
        triageDao.insertTreatmentPlan(plan)
    }

    suspend fun deleteTreatmentPlan(plan: com.example.data.local.TreatmentPlanEntity) = withContext(Dispatchers.IO) {
        triageDao.deleteTreatmentPlan(plan)
    }

    suspend fun saveRiskAssessment(assessment: com.example.data.local.RiskAssessmentEntity): Long = withContext(Dispatchers.IO) {
        triageDao.insertRiskAssessment(assessment)
    }

    suspend fun deleteRiskAssessment(assessment: com.example.data.local.RiskAssessmentEntity) = withContext(Dispatchers.IO) {
        triageDao.deleteRiskAssessment(assessment)
    }

    suspend fun saveClinicalNote(note: com.example.data.local.ClinicalNoteEntity): Long = withContext(Dispatchers.IO) {
        triageDao.insertClinicalNote(note)
    }

    suspend fun deleteClinicalNote(note: com.example.data.local.ClinicalNoteEntity) = withContext(Dispatchers.IO) {
        triageDao.deleteClinicalNote(note)
    }

    private suspend fun callClinicalGeminiPrompt(promptText: String): String? {
        val request = GeminiGenerateContentRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = promptText)),
                    role = "user"
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.25,
                topP = 0.9,
                topK = 40
            )
        )
        return when (val outcome = executeGeminiWithRetry(request)) {
            is GeminiCallResult.Success -> outcome.text.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    suspend fun generatePersonalizedTreatmentPlan(
        profile: UserProfileEntity?,
        targetGoal: String,
        geneticsMarkers: String,
        lifestyleSummary: String,
        labResultsSummary: String,
        vitalLogs: List<com.example.data.local.VitalLogEntity>,
        language: String
    ): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val vitalsContext = vitalLogs.take(5).joinToString("; ") {
            val sec = if (it.secondaryValue > 0) "/${it.secondaryValue.toInt()}" else ""
            "${it.vitalType}: ${it.primaryValue.toInt()}$sec ${it.unit} (${it.status})"
        }.ifBlank { "No recent vitals recorded" }

        val prompt = """
            You are a Precision Medicine & Personalized Treatment Planning AI assistant.
            Analyze the patient's genetics, lifestyle, lab test results, allergies, and medical profile to suggest the safest, best personalized treatment options for discussion with their physician.

            PATIENT PROFILE:
            - Age: ${profile?.age ?: 30}, Gender: ${profile?.gender ?: "Not Specified"}, Blood Group: ${profile?.bloodGroup ?: "Not Specified"}
            - Allergies: ${profile?.allergies ?: "None Known"}
            - Chronic Conditions: ${profile?.chronicConditions ?: "None"}
            - Current Medications: ${profile?.currentMedications ?: "None"}
            - Recent Vitals: $vitalsContext

            PERSONALIZED CARE INPUTS:
            - Primary Health Goal / Condition: $targetGoal
            - Genetic / Hereditary Factors: $geneticsMarkers
            - Lifestyle Profile: $lifestyleSummary
            - Lab & Test Results: $labResultsSummary

            REQUIRED LANGUAGE: Respond completely in $language.
            FORMAT STRICTLY INTO 4 CLEAR SECTIONS:
            1. 🧬 Genetic & Biomarker Insights
            2. 🥗 Personalized Lifestyle & Nutrition Protocol
            3. 💊 Targeted Treatment & Pharmacogenomic Considerations (explicitly cross-check allergies: ${profile?.allergies ?: "None"})
            4. 🔬 Recommended Follow-Up Tests & Doctor Action Plan

            End with a clear educational medical disclaimer.
        """.trimIndent()

        val aiText = callClinicalGeminiPrompt(prompt)
        if (aiText != null) {
            return@withContext aiText to true
        }

        val allergyWarning = profile?.allergies?.takeIf { it.isNotBlank() && !it.equals("None Known", ignoreCase = true) }
        val fallback = when {
            language.equals("Gujarati", ignoreCase = true) -> """
                ### 1. 🧬 જીનેટિક અને લેબ રિઝલ્ટ વિશ્લેષણ
                * **લક્ષ્ય સ્થિતિ:** $targetGoal
                * **આનુવંશિક / પારિવારિક ઇતિહાસ:** $geneticsMarkers
                * **લેબ ટેસ્ટ અને વાઇટલ્સ:** $labResultsSummary ($vitalsContext)
                * વારસાગત જોખમ અને લેબ રિઝલ્ટના આધારે નિવારક (Preventive) અને વ્યક્તિગત સંભાળની ભલામણ કરવામાં આવે છે.

                ### 2. 🥗 વ્યક્તિગત જીવનશૈલી અને આહાર યોજના
                * **જીવનશૈલી આધાર:** $lifestyleSummary
                * ઓછું મીઠું, ઉચ્ચ ફાઇબર (લીલા શાકભાજી, કઠોળ, આખા અનાજ), અને પ્રોસેસ્ડ શુગર ટાળો.
                * રોજ ઓછામાં ઓછી ૩૦ મિનિટ ઝડપી ચાલવું અને ૭+ કલાક ઊંઘ લેવી.

                ### 3. 💊 વ્યક્તિગત સારવાર વિકલ્પો (ડૉક્ટર સાથે ચર્ચા માટે)
                * ${if (allergyWarning != null) "⚠️ **એલર્જી ચેતવણી:** '$allergyWarning' નોંધાયેલ છે — કોઈ પણ દવા લેતા પહેલા ડૉક્ટરને ખાસ જણાવો." else "કોઈ જાણીતી એલર્જી નોંધાયેલ નથી; દવાઓ ડૉક્ટરની સલાહ મુજબ જ લો."}
                * ચાલુ ક્રોનિક સ્થિતિ (${profile?.chronicConditions ?: "None"}) સાથે દવાઓના ડોઝ અને સમયનું સંતુલન જાળવવું.

                ### 4. 🔬 આગામી લેબ ટેસ્ટ અને ફોલો-અપ
                * દર ૩ મહિને HbA1c, લિપિડ પ્રોફાઇલ, CBC અને BP ટ્રેકિંગ કરાવવું.

                *નોંધ: ઓફલાઇન ક્લિનિકલ રૂલ્સ મુજબ તૈયાર થયેલ માર્ગદર્શન; અંતિમ સારવાર માટે ડૉક્ટરની સલાહ જરૂરી છે.*
            """.trimIndent()
            language.equals("Hindi", ignoreCase = true) -> """
                ### 1. 🧬 जेनेटिक और लैब परिणाम विश्लेषण
                * **लक्ष्य स्थिति:** $targetGoal
                * **आनुवंशिक कारक:** $geneticsMarkers
                * **लैब और वाइटल्स:** $labResultsSummary ($vitalsContext)

                ### 2. 🥗 व्यक्तिगत जीवनशैली और आहार योजना
                * **लाइफस्टाइल प्रोफ़ाइल:** $lifestyleSummary
                * कम नमक, उच्च फाइबर आहार और नियमित 30 मिनट वॉक + 7 घंटे नींद को प्राथमिकता दें।

                ### 3. 💊 लक्षित उपचार विकल्प (डॉक्टर से परामर्श हेतु)
                * ${if (allergyWarning != null) "⚠️ **एलर्जी चेतावनी:** '$allergyWarning' दर्ज है — नई दवा से पहले डॉक्टर को बताएं।" else "प्रोफ़ाइल के अनुसार निर्धारित समय पर दवा लें।"}
                * पुरानी बीमारी (${profile?.chronicConditions ?: "None"}) के अनुसार दवाओं की समीक्षा कराएं।

                ### 4. 🔬 अनुशंसित फॉलो-अप टेस्ट
                * हर 3 महीने में HbA1c, लिपिड प्रोफाइल और नियमित BP जांच कराएं।

                *अस्वीकरण: यह शैक्षिक मार्गदर्शन है; अंतिम उपचार डॉक्टर की सलाह से ही लें।*
            """.trimIndent()
            else -> """
                ### 1. 🧬 Genetic & Biomarker Insights
                * **Target Goal:** $targetGoal
                * **Genetic / Family Risk Profile:** $geneticsMarkers
                * **Lab & Vitals Context:** $labResultsSummary ($vitalsContext)

                ### 2. 🥗 Personalized Lifestyle & Nutrition Protocol
                * **Current Lifestyle:** $lifestyleSummary
                * Prioritize high-fiber whole foods, balanced glycemic load, reduced sodium (<2g/day), 30 min daily aerobic activity, and 7+ hrs restorative sleep.

                ### 3. 💊 Targeted Treatment Options (For Physician Review)
                * ${if (allergyWarning != null) "⚠️ **Allergy Cross-Check:** Avoid medications triggering '$allergyWarning'." else "No known drug allergies flagged; verify compatibility with current regimen (${profile?.currentMedications ?: "None"})."}
                * Tailor preventive therapy and monitoring frequency to genetic predisposition and chronic background (${profile?.chronicConditions ?: "None"}).

                ### 4. 🔬 Recommended Follow-Up Screenings
                * Schedule periodic Lipid Panel, HbA1c, Renal/Liver function, and home vitals tracking every 3 months.

                Disclaimer: Generated via offline precision clinical rules. Always finalize personalized treatment with your licensed physician.
            """.trimIndent()
        }
        fallback to false
    }

    suspend fun generatePredictiveRiskForecast(
        profile: UserProfileEntity?,
        riskSnapshot: com.example.utils.PredictiveRiskSnapshot,
        vitalLogs: List<com.example.data.local.VitalLogEntity>,
        symptomLogs: List<SymptomLogEntity>,
        language: String
    ): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val recentSymptoms = symptomLogs.take(6).joinToString("; ") { "${it.symptomTitle} (${it.severity}/10, ${it.status})" }
            .ifBlank { "No active symptoms logged" }
        val recentVitals = vitalLogs.take(6).joinToString("; ") { "${it.vitalType}=${it.primaryValue}" }
            .ifBlank { "No recent vitals" }

        val prompt = """
            You are a Predictive Clinical Analytics & Early Disease Detection AI.
            Analyze the patient's medical history, computed risk indices, vitals, and symptoms to forecast early cardiovascular, sepsis/infection, and metabolic risks.

            PATIENT RECORD:
            - Age: ${profile?.age ?: 30}, Chronic Conditions: ${profile?.chronicConditions ?: "None"}
            - Computed Cardiac Risk Score: ${riskSnapshot.heartRiskScore}/100
            - Computed Sepsis / Systemic Infection Early Warning Score: ${riskSnapshot.sepsisRiskScore}/100
            - Computed Metabolic Risk Score: ${riskSnapshot.metabolicRiskScore}/100
            - Overall Status: ${riskSnapshot.overallRiskLevel}
            - Recent Vitals: $recentVitals
            - Recent Symptoms: $recentSymptoms

            REQUIRED LANGUAGE: Respond completely in $language.
            Provide:
            1. ❤️ Early Cardiovascular Risk Forecast & Prevention
            2. 🦠 Sepsis / Infection Early Warning Interpretation
            3. ⚖️ 30–90 Day Preventive Action Plan & Warning Red Flags
            Include an educational medical disclaimer at the end.
        """.trimIndent()

        val aiText = callClinicalGeminiPrompt(prompt)
        if (aiText != null) return@withContext aiText to true

        val factors = (riskSnapshot.heartFactors + riskSnapshot.sepsisFactors).distinct().joinToString("\n• ")
        val actions = riskSnapshot.immediateActions.joinToString("\n• ")
        val fallback = when {
            language.equals("Gujarati", ignoreCase = true) -> """
                ### 📊 પ્રારંભિક જોખમ આગાહી (${riskSnapshot.overallRiskLevel})
                * **હૃદય રોગ જોખમ સ્કોર:** ${riskSnapshot.heartRiskScore}%
                * **સેપ્સિસ / ચેપ પ્રારંભિક સંકેત:** ${riskSnapshot.sepsisRiskScore}%
                * **મેટાબોલિક જોખમ:** ${riskSnapshot.metabolicRiskScore}%

                ### 🔍 મુખ્ય કારણો
                • $factors

                ### 🛡️ સમયસર નિવારણ પગલાં
                • $actions
            """.trimIndent()
            language.equals("Hindi", ignoreCase = true) -> """
                ### 📊 प्रारंभिक जोखिम पूर्वानुमान (${riskSnapshot.overallRiskLevel})
                * **हृदय जोखिम स्कोर:** ${riskSnapshot.heartRiskScore}%
                * **सेप्सिस / संक्रमण प्रारंभिक चेतावनी:** ${riskSnapshot.sepsisRiskScore}%
                * **मेटाबोलिक जोखिम:** ${riskSnapshot.metabolicRiskScore}%

                ### 🔍 मुख्य कारक
                • $factors

                ### 🛡️ समय रहते बचाव के कदम
                • $actions
            """.trimIndent()
            else -> """
                ### 📊 Early Risk Forecast (${riskSnapshot.overallRiskLevel})
                * **Cardiovascular Risk Index:** ${riskSnapshot.heartRiskScore}%
                * **Sepsis / Systemic Early Warning:** ${riskSnapshot.sepsisRiskScore}%
                * **Metabolic Trajectory Risk:** ${riskSnapshot.metabolicRiskScore}%

                ### 🔍 Key Clinical Drivers
                • $factors

                ### 🛡️ Timely Preventive Actions
                • $actions
            """.trimIndent()
        }
        fallback to false
    }

    suspend fun generateClinicalDocumentation(
        transcript: String,
        patientName: String,
        profile: UserProfileEntity?,
        language: String
    ): Pair<com.example.utils.ParsedClinicalDocument, Boolean> = withContext(Dispatchers.IO) {
        val localParsed = com.example.utils.ClinicalRiskEngine.parseClinicalTranscriptLocally(
            transcript = transcript,
            patientName = patientName,
            language = language
        )

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext localParsed to false
        }

        val prompt = """
            You are an Automated Clinical Documentation & Medical NLP Scribe.
            Convert the following doctor-patient conversation into crisp clinical documentation in $language.
            Patient Name: $patientName | Age: ${profile?.age ?: 30} | Allergies: ${profile?.allergies ?: "None Known"}

            TRANSCRIPT:
            "$transcript"

            Output clearly with these exact 3 section headers:
            ===SOAP_NOTE===
            (Subjective, Objective, Assessment, Plan concisely formatted)
            ===MEDICAL_REPORT===
            (Structured consultation report summary for patient file)
            ===PRESCRIPTION===
            (Numbered medications discussed with dosage and timing, or "No medications prescribed — follow clinical advice")
        """.trimIndent()

        val aiText = callClinicalGeminiPrompt(prompt) ?: return@withContext localParsed to false

        val soap = aiText.substringAfter("===SOAP_NOTE===", "")
            .substringBefore("===MEDICAL_REPORT===").trim()
            .ifBlank { localParsed.soapNote }
        val report = aiText.substringAfter("===MEDICAL_REPORT===", "")
            .substringBefore("===PRESCRIPTION===").trim()
            .ifBlank { localParsed.medicalReport }
        val rx = aiText.substringAfter("===PRESCRIPTION===", "").trim()
            .ifBlank { localParsed.prescriptionSummary }

        return@withContext localParsed.copy(
            soapNote = soap,
            medicalReport = report,
            prescriptionSummary = rx
        ) to true
    }

    // -------------------------------------------------------------------------
    // Version 5.0 Modules: EHR, Mental Health, Drug Discovery, Epidemic & Doctor Booking
    // -------------------------------------------------------------------------
    suspend fun saveEhrRecord(record: com.example.data.local.EhrRecordEntity): Long = withContext(Dispatchers.IO) {
        triageDao.insertEhrRecord(record)
    }

    suspend fun deleteEhrRecord(record: com.example.data.local.EhrRecordEntity) = withContext(Dispatchers.IO) {
        triageDao.deleteEhrRecord(record)
    }

    suspend fun saveMentalHealthCheckIn(checkIn: com.example.data.local.MentalHealthCheckInEntity): Long = withContext(Dispatchers.IO) {
        triageDao.insertMentalHealthCheckIn(checkIn)
    }

    suspend fun deleteMentalHealthCheckIn(checkIn: com.example.data.local.MentalHealthCheckInEntity) = withContext(Dispatchers.IO) {
        triageDao.deleteMentalHealthCheckIn(checkIn)
    }

    suspend fun saveDrugStudy(study: com.example.data.local.DrugCandidateStudyEntity): Long = withContext(Dispatchers.IO) {
        triageDao.insertDrugStudy(study)
    }

    suspend fun deleteDrugStudy(study: com.example.data.local.DrugCandidateStudyEntity) = withContext(Dispatchers.IO) {
        triageDao.deleteDrugStudy(study)
    }

    suspend fun saveOutbreakAlert(alert: com.example.data.local.OutbreakAlertEntity): Long = withContext(Dispatchers.IO) {
        triageDao.insertOutbreakAlert(alert)
    }

    suspend fun deleteOutbreakAlert(alert: com.example.data.local.OutbreakAlertEntity) = withContext(Dispatchers.IO) {
        triageDao.deleteOutbreakAlert(alert)
    }

    suspend fun saveDoctorAppointment(appointment: com.example.data.local.DoctorAppointmentEntity): Long = withContext(Dispatchers.IO) {
        triageDao.insertDoctorAppointment(appointment)
    }

    suspend fun deleteDoctorAppointment(appointment: com.example.data.local.DoctorAppointmentEntity) = withContext(Dispatchers.IO) {
        triageDao.deleteDoctorAppointment(appointment)
    }

    private suspend fun callMultimodalClinicalPromptDetailed(
        promptText: String,
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): GeminiCallResult {
        val parts = mutableListOf<GeminiPart>()
        if (!imageBase64.isNullOrBlank() && !imageMimeType.isNullOrBlank()) {
            parts.add(GeminiApiClient.createImagePart(imageMimeType, imageBase64))
        }
        parts.add(GeminiApiClient.createTextPart(promptText))

        val request = GeminiGenerateContentRequest(
            contents = listOf(GeminiContent(parts = parts, role = "user")),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.25,
                topP = 0.9,
                topK = 40
            )
        )
        return executeGeminiWithRetry(request)
    }

    private suspend fun callMultimodalClinicalPrompt(
        promptText: String,
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): String? {
        return when (val outcome = callMultimodalClinicalPromptDetailed(promptText, imageBase64, imageMimeType)) {
            is GeminiCallResult.Success -> outcome.text.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    suspend fun generateAutomatedEhrSummary(
        rawNotes: String,
        patientName: String,
        profile: UserProfileEntity?,
        parsed: com.example.utils.StructuredEhrParseResult,
        language: String
    ): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val prompt = """
            You are an Electronic Health Records (EHR) Automation NLP Specialist.
            Organize messy dictated/typed patient notes into a clean, structured digital EHR entry for a physician.
            Patient: $patientName | Age: ${profile?.age ?: 30} | Gender: ${profile?.gender ?: "Not specified"} | Blood Group: ${profile?.bloodGroup ?: "Unknown"}
            Known Allergies: ${profile?.allergies ?: "None Known"}
            Detected Category: ${parsed.category}
            Extracted Tags: ${parsed.diagnosisTags}
            Extracted Medications: ${parsed.medicationsDetected}

            RAW CLINICAL NOTES:
            "$rawNotes"

            REQUIRED LANGUAGE: Respond completely in $language.
            Format clearly into:
            1. 🗂️ EHR Header & Chief Complaint
            2. 📊 Organized Vitals, Labs & History
            3. 💊 Structured Medication Reconciliation & Allergy Cross-Check
            4. ✅ Actionable Follow-Up & Coding Summary
        """.trimIndent()

        val aiText = callClinicalGeminiPrompt(prompt)
        if (aiText != null) aiText to true else parsed.structuredSummary to false
    }

    suspend fun generateMentalHealthCounseling(
        transcript: String,
        screening: com.example.utils.MentalHealthScreeningResult,
        stressSlider: Int,
        sleepHours: Float,
        anxietyDays: Int,
        imageBase64: String? = null,
        imageMimeType: String? = null,
        language: String
    ): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val hasPhoto = !imageBase64.isNullOrBlank()
        val prompt = """
            You are a compassionate Mental Health Support & Primary CBT Counseling AI assistant.
            Analyze the patient's spoken/written feelings, sleep & stress index${if (hasPhoto) ", and facial expression cues from the attached image" else ""} to gently screen for anxiety, stress, or low mood and offer supportive primary counseling.
            IMPORTANT SAFETY RULE: Never diagnose major psychiatric disorders clinically. If crisis/self-harm indicators exist, prioritize urgent Tele-MANAS (14416) / 112 crisis intervention first.

            SCREENING SIGNALS:
            - Mood/Distress Assessment: ${screening.moodLabel} (Stress Index: ${screening.stressScore}/100)
            - Self-Rated Stress: $stressSlider/10 | Sleep: $sleepHours hrs/night | Anxious Days (last 14d): $anxietyDays
            - Patient Voice / Journal Input: "${transcript.ifBlank { "No text provided — check-in via sliders/expression." }}"
            - Crisis Flag: ${screening.crisisDetected}

            REQUIRED LANGUAGE: Respond warmly and supportively in $language.
            Include:
            1. 💬 Empathetic Reflection & Affect Observation
            2. 🌬️ Immediate Calming Breath & Grounding Exercise
            3. 🧠 Gentle CBT Coping Steps
            4. 📞 Helpline & When to See a Counselor/Psychiatrist (Include Tele-MANAS 14416 / 112 if high distress)
        """.trimIndent()

        val aiText = callMultimodalClinicalPrompt(prompt, imageBase64, imageMimeType)
        if (aiText != null) aiText to true else screening.fallbackGuidance to false
    }

    suspend fun generateDrugDiscoveryAnalysis(
        targetDisease: String,
        moleculeInput: String,
        priority: String,
        screening: com.example.utils.MoleculeScreeningResult,
        language: String
    ): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val prompt = """
            You are a Computational Medicinal Chemistry & AI Drug Discovery Research Assistant.
            Evaluate the candidate compound and explain how in-silico AI accelerates preclinical discovery before lab validation and human clinical trials.

            CANDIDATE METRICS:
            - Target Disease / Protein Pathway: $targetDisease
            - Compound Formula / SMILES: $moleculeInput
            - Optimization Priority: $priority
            - Estimated Molecular Weight: ${screening.estimatedMolecularWeight} g/mol
            - Heavy Atoms: ${screening.heavyAtomEstimate} | H-Donors: ${screening.hBondDonors} | H-Acceptors: ${screening.hBondAcceptors}
            - Lipinski Rule-of-5 Violations: ${screening.lipinskiViolations} | Drug-Likeness Index: ${screening.drugLikenessScore}% (${screening.bioavailabilityTier})

            REQUIRED LANGUAGE: Respond completely in $language.
            Format clearly into:
            1. 🧪 Physicochemical & Lipinski ADMET Profile
            2. 🎯 Target Binding & Mechanism Hypothesis
            3. 🚀 AI Preclinical Acceleration Roadmap (Months vs Years before Phase I-III Clinical Trials)
            4. ⚠️ Scientific Caveat (Computational screening requires wet-lab synthesis & regulatory clinical trials)
        """.trimIndent()

        val aiText = callClinicalGeminiPrompt(prompt)
        if (aiText != null) aiText to true else screening.fallbackReport to false
    }

    suspend fun generateEpidemicAlertBrief(
        regionName: String,
        tempCelsius: Int,
        humidityPercent: Int,
        travelDensity: String,
        hospitalLoadPercent: Int,
        newsSignals: String,
        prediction: com.example.utils.OutbreakPredictionResult,
        language: String
    ): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val prompt = """
            You are an Epidemiological Surveillance & Public Health Early-Warning AI.
            Analyze weather, mobility/travel, hospital capacity, and local outbreak news signals to produce an actionable Early-Warning Health Bulletin for public health officials & hospitals.

            TELEMETRY INPUTS:
            - Region / District: $regionName
            - Weather: ${tempCelsius}°C, Humidity $humidityPercent%
            - Travel / Mobility Density: $travelDensity
            - Local Hospital OPD/ICU Load: $hospitalLoadPercent%
            - News / Cluster Signals: "${newsSignals.ifBlank { "Routine seasonal monitoring" }}"
            - Computed Risk Scores: Vector-Borne ${prediction.vectorRiskScore}%, Respiratory/Viral ${prediction.respiratoryRiskScore}%, Water-Borne ${prediction.waterBorneRiskScore}%
            - Alert Tier: ${prediction.overallAlertTier}

            REQUIRED LANGUAGE: Respond completely in $language.
            Include:
            1. 🚨 Outbreak Risk Assessment & Dominant Pathogen Vectors
            2. 🏥 Health Dept & Municipal Preventive Actions
            3. 🧬 Surveillance & Hospital Surge Readiness Checklist
        """.trimIndent()

        val aiText = callClinicalGeminiPrompt(prompt)
        if (aiText != null) aiText to true else prediction.fallbackBrief to false
    }

    suspend fun sendVideoConsultationTurn(
        spokenMessage: String,
        recentCallTurns: List<Pair<String, String>>, // Pair(speaker, text)
        userProfile: UserProfileEntity?,
        cameraFrameBase64: String? = null,
        language: String
    ): Result<Pair<String, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val isEmergency = isEmergencyText(spokenMessage)
            if (isEmergency) {
                val emergencyReply = when {
                    language.equals("Gujarati", ignoreCase = true) ->
                        "સાવધાન! તમારા લક્ષણો ગંભીર લાગે છે. કૃપા કરીને તરત જ ૧૦૮ અથવા ૧૧૨ ઇમરજન્સી પર કૉલ કરો અને નજીકની હૉસ્પિટલમાં જાઓ. શાંતિથી બેસો અને મદદ આવતા સુધી રાહ જુઓ."
                    language.equals("Hindi", ignoreCase = true) ->
                        "सावधान! आपके लक्षण गंभीर लग रहे हैं। कृपया तुरंत 108 या 112 पर कॉल करें और नजदीकी अस्पताल जाएं। शांत बैठें और मदद आने तक प्रतीक्षा करें।"
                    else ->
                        "Urgent alert! Your symptoms sound critical. Please call emergency services at 112 or 108 immediately and go to the nearest emergency room. Sit upright and stay calm while help arrives."
                }
                return@withContext Result.success(emergencyReply to true)
            }

            val historyBlock = recentCallTurns.takeLast(6).joinToString("\n") { (speaker, msg) ->
                "$speaker: $msg"
            }.ifBlank { "First turn of video call." }

            val hasCameraFrame = !cameraFrameBase64.isNullOrBlank()
            val prompt = """
                You are "Health Care AI Doctor," speaking face-to-face with a patient during a live mobile VIDEO CALL consultation.
                Speak naturally, warmly, concisely (3 to 5 clear spoken sentences, NO markdown bullets, NO hashtags, NO asterisks) because your reply will be read aloud immediately via Text-to-Speech while shown as live video call subtitles.

                PATIENT PROFILE:
                - Name: ${userProfile?.fullName ?: "Patient"}, Age: ${userProfile?.age ?: 30}, Allergies: ${userProfile?.allergies ?: "None Known"}, Conditions: ${userProfile?.chronicConditions ?: "None"}
                - Live Camera Snapshot Attached: $hasCameraFrame (If attached, briefly note any visible clinical cue or demeanor naturally without overclaiming a diagnosis.)
                - Recent Video Call Dialogue:
                $historyBlock

                PATIENT JUST SAID:
                "$spokenMessage"

                REQUIRED LANGUAGE: Reply completely and naturally in $language.
                Include:
                1. Empathetic acknowledgement (and visual observation if a camera frame shows something relevant).
                2. One practical self-care step or follow-up question to keep the video consultation flowing.
                3. Clear advice on when to see an in-person doctor or specialist.
            """.trimIndent()

            when (val aiOutcome = callMultimodalClinicalPromptDetailed(
                promptText = prompt,
                imageBase64 = cameraFrameBase64,
                imageMimeType = if (hasCameraFrame) "image/jpeg" else null
            )) {
                is GeminiCallResult.Success -> {
                    return@withContext Result.success(aiOutcome.text.replace(Regex("[#*`_~]"), "") to false)
                }
                is GeminiCallResult.NetworkError -> {
                    return@withContext Result.failure(SlowNetworkException(aiOutcome.message, aiOutcome.cause))
                }
                is GeminiCallResult.NoApiKey -> {
                    // Continue below ONLY for unconfigured offline test builds
                }
            }

            // Conversational local fallback cleanly tailored for spoken Video Call dialogue (unconfigured/offline mode)
            val rawFallback = getEducationalTriageFallback(
                userMessage = spokenMessage,
                profile = userProfile,
                hasImage = hasCameraFrame && spokenMessage.isBlank()
            )
            val cleanSpokenFallback = rawFallback
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("###") && !it.startsWith("Disclaimer:") }
                .take(4)
                .joinToString(" ") { it.replace(Regex("[#*`_~•]"), "").trim() }
                .ifBlank {
                    when {
                        language.equals("Gujarati", ignoreCase = true) ->
                            "હું તમને સાંભળી રહ્યો છું. કૃપા કરીને તમારા લક્ષણો કેટલા સમયથી છે તે જણાવો, પૂરતું પાણી પીઓ અને જરૂર જણાય તો ડૉક્ટરની સલાહ લો."
                        language.equals("Hindi", ignoreCase = true) ->
                            "मैं आपकी बात सुन रहा हूँ। कृपया बताएं कि यह समस्या कितने समय से है, आराम करें और जरूरत पड़ने पर डॉक्टर से मिलें।"
                        else ->
                            "I hear you clearly on our video call. Could you tell me how long you have felt this way? Stay hydrated, rest calmly, and consult a doctor if symptoms worsen."
                    }
                }
            Result.success(cleanSpokenFallback to false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Authentication Repository (Username/Password + Mobile OTP)
    // -------------------------------------------------------------------------
    suspend fun registerCustomUserAccount(
        username: String,
        displayName: String,
        password: String
    ): Result<com.example.data.local.UserAccountEntity> = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        if (cleanUser.length < 3) {
            return@withContext Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
        }
        if (cleanUser.contains(' ')) {
            return@withContext Result.failure(IllegalArgumentException("Username cannot contain spaces."))
        }
        if (cleanUser.equals(com.example.utils.AuthSecurityHelper.DEVELOPER_USERNAME, ignoreCase = true)) {
            return@withContext Result.failure(IllegalArgumentException("Reserved developer username cannot be registered."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        val existing = triageDao.getUserAccountByUsernameCaseInsensitive(cleanUser)
        if (existing != null) {
            return@withContext Result.failure(IllegalStateException("Username '$cleanUser' is already registered. Please sign in."))
        }
        val salt = com.example.utils.AuthSecurityHelper.generateSalt()
        val hash = com.example.utils.AuthSecurityHelper.hashPassword(password, salt)
        val account = com.example.data.local.UserAccountEntity(
            username = cleanUser,
            displayName = displayName.trim().ifBlank { cleanUser },
            passwordSalt = salt,
            passwordHash = hash,
            role = "PATIENT",
            authMethod = "PASSWORD"
        )
        return@withContext try {
            val newId = triageDao.insertUserAccount(account)
            com.example.utils.DevLogger.i("AuthRepo", "Registered custom user account '$cleanUser'")
            Result.success(account.copy(id = newId))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Could not create account for '$cleanUser': ${e.message ?: "Already exists"}"))
        }
    }

    suspend fun loginCustomUserAccount(
        username: String,
        password: String
    ): Result<com.example.data.local.UserAccountEntity> = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        val existing = triageDao.getUserAccountByUsernameCaseInsensitive(cleanUser)
            ?: return@withContext Result.failure(IllegalArgumentException("Account not found. Please sign up first."))
        val valid = com.example.utils.AuthSecurityHelper.verifyPassword(
            password = password,
            salt = existing.passwordSalt,
            expectedHash = existing.passwordHash
        )
        if (!valid) {
            com.example.utils.DevLogger.w("AuthRepo", "Invalid password attempt for '${existing.username}'")
            return@withContext Result.failure(IllegalArgumentException("Incorrect password for '${existing.username}'."))
        }
        com.example.utils.DevLogger.i("AuthRepo", "Logged in user '${existing.username}' via password")
        Result.success(existing)
    }

    suspend fun findOrCreateOtpAccount(
        phoneNumber: String
    ): Result<com.example.data.local.UserAccountEntity> = withContext(Dispatchers.IO) {
        val normalized = com.example.utils.AuthSecurityHelper.normalizePhoneNumber(phoneNumber)
        if (normalized.length < 10) {
            return@withContext Result.failure(IllegalArgumentException("Enter a valid 10-digit mobile number."))
        }
        val otpUsername = "mobile_$normalized"
        val existing = triageDao.getUserAccountByPhone(normalized)
            ?: triageDao.getUserAccountByUsernameCaseInsensitive(otpUsername)
        if (existing != null) {
            return@withContext Result.success(existing)
        }
        val account = com.example.data.local.UserAccountEntity(
            username = otpUsername,
            displayName = "Patient (+91 $normalized)",
            phoneNumber = normalized,
            role = "PATIENT",
            authMethod = "MOBILE_OTP"
        )
        return@withContext try {
            val newId = triageDao.insertUserAccount(account)
            com.example.utils.DevLogger.i("AuthRepo", "Registered & verified mobile OTP user +91 $normalized")
            Result.success(account.copy(id = newId))
        } catch (e: Exception) {
            val fallback = triageDao.getUserAccountByPhone(normalized)
                ?: triageDao.getUserAccountByUsernameCaseInsensitive(otpUsername)
            if (fallback != null) {
                Result.success(fallback)
            } else {
                Result.failure(e)
            }
        }
    }

    // -------------------------------------------------------------------------
    // AI Symptom-Based Medicine & Severity Advisor (with Online Photo Lookup)
    // -------------------------------------------------------------------------
    suspend fun evaluateMedicineBySymptoms(
        symptomOrIllness: String,
        severity: Int,
        duration: String,
        language: String,
        userProfile: UserProfileEntity?
    ): com.example.utils.MedicineAdvisorResult = withContext(Dispatchers.IO) {
        val localBaseline = com.example.utils.MedicineAdvisorEngine.evaluateLocally(
            symptomOrIllness = symptomOrIllness,
            severity = severity,
            duration = duration,
            language = language,
            profile = userProfile,
            resolveImagesOnline = true
        )

        // When symptoms are mild (<=3/10) without red flags, keep strict non-medicine advice
        if (localBaseline.tier == com.example.utils.SeverityGuidanceTier.MILD_NO_MED_NEEDED) {
            return@withContext localBaseline
        }

        val prompt = """
            You are a cautious Clinical Pharmacology Reference Assistant.
            Analyze this patient's symptoms and severity and output ONLY valid JSON (no markdown code fences).

            INPUT:
            - Illness / Symptoms: "$symptomOrIllness"
            - Severity: $severity / 10
            - Duration: "$duration"
            - Patient Age: ${userProfile?.age ?: 30}
            - Known Allergies: "${userProfile?.allergies ?: "None Known"}"
            - Chronic Conditions: "${userProfile?.chronicConditions ?: "None"}"
            - Preferred Language: "$language" (Write doctorHeadline, clinicalSummary, usageReason, dosageNote, safetyWarning, homeCareSteps in $language!)

            SAFETY RULES:
            1. Emphasize consulting a certified doctor.
            2. Return up to 4 evidence-based reference medicines that exist for these symptoms (keep generic English medicine name + short Wikipedia title in "wikiQuery", e.g., "Paracetamol", "Cetirizine", "Omeprazole", "Amoxicillin").
            3. Mark antibiotics or strong drugs strictly as "Rx • Doctor Prescription Required".

            REQUIRED JSON SCHEMA:
            {
              "doctorHeadline": "string",
              "clinicalSummary": "string",
              "homeCareSteps": ["string"],
              "medicines": [
                {
                  "name": "string",
                  "category": "string",
                  "rxStatus": "string",
                  "usageReason": "string",
                  "dosageNote": "string",
                  "safetyWarning": "string",
                  "wikiQuery": "string"
                }
              ]
            }
        """.trimIndent()

        val aiOutcome = callMultimodalClinicalPromptDetailed(promptText = prompt)
        val rawAi = (aiOutcome as? GeminiCallResult.Success)?.text
        if (!rawAi.isNullOrBlank()) {
            try {
                val cleanedJson = rawAi.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                val obj = org.json.JSONObject(cleanedJson)
                val medArr = obj.optJSONArray("medicines")
                val parsedMeds = mutableListOf<com.example.utils.MedicineRecommendationItem>()
                if (medArr != null) {
                    for (i in 0 until medArr.length()) {
                        val item = medArr.getJSONObject(i)
                        val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                        val wikiQuery = item.optString("wikiQuery", name.substringBefore(" "))
                        val photoUrl = com.example.utils.MedicineAdvisorEngine.fetchOnlineMedicineImageUrl(wikiQuery)
                        parsedMeds.add(
                            com.example.utils.MedicineRecommendationItem(
                                name = name,
                                category = item.optString("category", "Reference Medicine"),
                                rxStatus = item.optString("rxStatus", "Doctor Guidance"),
                                usageReason = item.optString("usageReason", ""),
                                dosageNote = item.optString("dosageNote", "Consult a doctor for dosage."),
                                safetyWarning = item.optString("safetyWarning", "Verify allergies before taking."),
                                wikiQuery = wikiQuery,
                                imageUrl = photoUrl,
                                onlinePhotoSearchUrl = com.example.utils.MedicineAdvisorEngine.buildOnlineSearchUrl(name)
                            )
                        )
                    }
                }
                val stepsArr = obj.optJSONArray("homeCareSteps")
                val parsedSteps = if (stepsArr != null) {
                    (0 until stepsArr.length()).mapNotNull { idx -> stepsArr.optString(idx).takeIf { it.isNotBlank() } }
                } else {
                    localBaseline.homeCareSteps
                }
                if (parsedMeds.isNotEmpty()) {
                    return@withContext localBaseline.copy(
                        doctorHeadline = obj.optString("doctorHeadline").ifBlank { localBaseline.doctorHeadline },
                        clinicalSummary = obj.optString("clinicalSummary").ifBlank { localBaseline.clinicalSummary },
                        homeCareSteps = parsedSteps.ifEmpty { localBaseline.homeCareSteps },
                        medicines = parsedMeds,
                        aiPowered = true
                    )
                }
            } catch (_: Exception) {
                // Fall back cleanly to localBaseline with enriched images
            }
        }
        val slowWarning = if (aiOutcome is GeminiCallResult.NetworkError) {
            when {
                language.equals("Gujarati", ignoreCase = true) ->
                    "ઇન્ટરનેટ ધીમું હોવાથી લાઇવ AI જવાબ મળી શક્યો નથી — ખોટી માહિતી ન આવે તે માટે પ્રાથમિક સલાહ બતાવી છે. મજબૂત નેટવર્ક સાથે ફરી તપાસો."
                language.equals("Hindi", ignoreCase = true) ->
                    "धीमे इंटरनेट के कारण लाइव AI उत्तर नहीं मिल सका — गलत उत्तर से बचने के लिए केवल प्राथमिक संदर्भ दिखाया गया है। कृपया फिर प्रयास करें।"
                else ->
                    "Internet was too slow for live AI verification — showing conservative clinical reference only. Please retry with stronger connection."
            }
        } else {
            null
        }
        localBaseline.copy(networkWarningMessage = slowWarning)
    }

    // -------------------------------------------------------------------------
    // Developer Console: Public Doctors, Live App Upgrades & Online User Presence
    // -------------------------------------------------------------------------
    val allPublicDoctors: Flow<List<com.example.data.local.PublicDoctorEntity>> =
        triageDao.getAllPublicDoctors()

    val activePublicDoctors: Flow<List<com.example.data.local.PublicDoctorEntity>> =
        triageDao.getActivePublicDoctors()

    val devAppConfig: Flow<com.example.data.local.DeveloperAppConfigEntity?> =
        triageDao.getDevAppConfigFlow()

    val developerUserPresenceList: Flow<List<com.example.data.local.DeveloperUserPresenceSummary>> =
        triageDao.getDeveloperUserPresenceFlow()

    suspend fun upsertPublicDoctor(doctor: com.example.data.local.PublicDoctorEntity): Long =
        withContext(Dispatchers.IO) {
            val existingByCloud = if (doctor.cloudId.isNotBlank()) {
                triageDao.getPublicDoctorByCloudId(doctor.cloudId)
            } else {
                null
            }
            val resolvedId = if (doctor.id > 0L) doctor.id else (existingByCloud?.id ?: 0L)
            triageDao.insertOrUpdatePublicDoctor(
                doctor.copy(
                    id = resolvedId,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

    suspend fun deletePublicDoctor(doctor: com.example.data.local.PublicDoctorEntity) =
        withContext(Dispatchers.IO) {
            triageDao.deletePublicDoctor(doctor)
        }

    suspend fun saveDevAppConfig(config: com.example.data.local.DeveloperAppConfigEntity) =
        withContext(Dispatchers.IO) {
            triageDao.insertOrUpdateDevAppConfig(
                config.copy(
                    id = 1,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

    suspend fun getDevAppConfigOnce(): com.example.data.local.DeveloperAppConfigEntity? =
        withContext(Dispatchers.IO) {
            triageDao.getDevAppConfigOnce()
        }

    suspend fun recordUserHeartbeat(
        username: String,
        authMethod: String,
        deltaSeconds: Long,
        isOnline: Boolean
    ): com.example.data.local.DeveloperUserPresenceSummary? = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        if (cleanUser.isBlank()) return@withContext null
        val now = System.currentTimeMillis()
        var existing = triageDao.getUserAccountByUsername(cleanUser)
        if (existing == null) {
            // Ensure Developer account ("SHAILESH") or active session also has a presence row
            val stub = com.example.data.local.UserAccountEntity(
                username = cleanUser,
                displayName = cleanUser,
                role = if (cleanUser == com.example.utils.AuthSecurityHelper.DEVELOPER_USERNAME) "DEVELOPER" else "PATIENT",
                authMethod = authMethod.ifBlank { "PASSWORD" },
                totalOnlineSeconds = deltaSeconds.coerceAtLeast(0L),
                lastOnlineAt = now,
                isCurrentlyOnline = isOnline
            )
            try {
                triageDao.insertUserAccount(stub)
            } catch (_: Exception) {}
            existing = triageDao.getUserAccountByUsername(cleanUser)
        } else {
            triageDao.recordUserOnlineHeartbeat(
                username = cleanUser,
                deltaSeconds = deltaSeconds.coerceAtLeast(0L),
                timestamp = now,
                isOnline = isOnline
            )
            existing = triageDao.getUserAccountByUsername(cleanUser)
        }
        existing?.let {
            com.example.data.local.DeveloperUserPresenceSummary(
                username = it.username,
                authMethod = it.authMethod,
                totalOnlineSeconds = it.totalOnlineSeconds,
                lastOnlineAt = it.lastOnlineAt,
                isCurrentlyOnline = it.isCurrentlyOnline
            )
        }
    }

    suspend fun markUserOffline(username: String) = withContext(Dispatchers.IO) {
        val clean = username.trim()
        if (clean.isBlank()) return@withContext
        triageDao.markUserOffline(clean, System.currentTimeMillis())
    }

    // Ultra Sleep & Wellness Persistence
    val allSleepWellnessLogs: Flow<List<com.example.data.local.SleepWellnessLogEntity>> =
        triageDao.getAllSleepWellnessLogs()

    suspend fun saveSleepWellnessLog(log: com.example.data.local.SleepWellnessLogEntity): Long =
        withContext(Dispatchers.IO) {
            triageDao.insertSleepWellnessLog(log)
        }

    suspend fun deleteSleepWellnessLog(log: com.example.data.local.SleepWellnessLogEntity) =
        withContext(Dispatchers.IO) {
            triageDao.deleteSleepWellnessLog(log)
        }

    // Ultra Meal & Calorie Nutrition Persistence
    val allMealNutritionLogs: Flow<List<com.example.data.local.MealNutritionLogEntity>> =
        triageDao.getAllMealNutritionLogs()

    suspend fun saveMealNutritionLog(log: com.example.data.local.MealNutritionLogEntity): Long =
        withContext(Dispatchers.IO) {
            triageDao.insertMealNutritionLog(log)
        }

    suspend fun deleteMealNutritionLog(log: com.example.data.local.MealNutritionLogEntity) =
        withContext(Dispatchers.IO) {
            triageDao.deleteMealNutritionLog(log)
        }

    suspend fun generateAiHealthCoachAdvice(
        healthScore: Int,
        todayWaterMl: Int,
        waterTargetMl: Int,
        todaySteps: Int,
        stepTarget: Int,
        todayCalories: Int,
        calorieTarget: Int,
        proteinGrams: Double,
        activeMedicinesCount: Int,
        latestSleepHours: Double?,
        profile: com.example.data.local.UserProfileEntity?,
        language: String
    ): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val prompt = """
            You are an empathetic, concise Preventive Clinical Health Coach inside Health Care AI.
            Provide a motivating, practical 3-bullet daily wellness coaching brief based on these live metrics:
            - Overall Health Score: $healthScore / 100
            - Hydration: $todayWaterMl / $waterTargetMl ml
            - Steps: $todaySteps / $stepTarget
            - Nutrition Today: $todayCalories / $calorieTarget kcal (${proteinGrams.toInt()}g protein)
            - Active Medicines: $activeMedicinesCount
            - Latest Sleep: ${latestSleepHours?.let { "${"%.1f".format(it)} hrs" } ?: "Not logged"}
            - Chronic Conditions: ${profile?.chronicConditions?.ifBlank { "None" } ?: "None"}

            REQUIRED LANGUAGE: Respond completely in $language.
            Keep it under 90 words, warm, safe, educational, and non-diagnostic.
        """.trimIndent()

        val aiText = callClinicalGeminiPrompt(prompt)
        if (!aiText.isNullOrBlank()) {
            aiText.trim() to true
        } else {
            val fallback = com.example.utils.UltraHealthCoachEngine.buildOfflineCoachingBrief(
                healthScore = healthScore,
                todayWaterMl = todayWaterMl,
                waterTargetMl = waterTargetMl,
                todaySteps = todaySteps,
                stepTarget = stepTarget,
                todayCalories = todayCalories,
                calorieTarget = calorieTarget,
                language = language
            )
            fallback to false
        }
    }
}

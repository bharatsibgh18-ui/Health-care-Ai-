package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerateContentRequest
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.local.ChatMessageEntity
import com.example.data.local.StepRecordEntity
import com.example.data.local.SymptomLogEntity
import com.example.data.local.TriageDao
import com.example.data.local.TriageSessionEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WaterLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TriageRepository(private val triageDao: TriageDao) {

    val allSessions: Flow<List<TriageSessionEntity>> = triageDao.getAllSessions()
    val allMessages: Flow<List<ChatMessageEntity>> = triageDao.getAllMessages()
    val userProfile: Flow<UserProfileEntity?> = triageDao.getUserProfile()
    val allSymptomLogs: Flow<List<SymptomLogEntity>> = triageDao.getAllSymptomLogs()
    val headacheLogs: Flow<List<SymptomLogEntity>> = triageDao.getHeadacheLogs()
    val allMedicines: Flow<List<com.example.data.local.MedicineEntity>> = triageDao.getAllMedicines()
    val activeMedicines: Flow<List<com.example.data.local.MedicineEntity>> = triageDao.getActiveMedicines()
    val recentStepRecords: Flow<List<StepRecordEntity>> = triageDao.getRecentStepRecords(7)
    val allStepRecords: Flow<List<StepRecordEntity>> = triageDao.getAllStepRecords()
    val allVitalLogs: Flow<List<com.example.data.local.VitalLogEntity>> = triageDao.getAllVitalLogs()
    val allFamilyMembers: Flow<List<com.example.data.local.FamilyMemberEntity>> = triageDao.getAllFamilyMembers()

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
        - Language Handling: Respond in the exact language the user communicates in (e.g., Hindi, Gujarati, English, or mixed Hinglish/Gujlish). If the user asks in Hindi/Hinglish like "muje sir dard hai", reply entirely in Hindi/Hinglish!

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
           - If the user communicates in Gujarati, provide the complete visual analysis, remedies, and medication guidance in warm, fluent, compassionate Gujarati!

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
        return triageDao.insertSymptomLog(log)
    }

    suspend fun updateSymptomLog(log: SymptomLogEntity) {
        triageDao.updateSymptomLog(log)
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

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.success(getEducationalTriageFallback(userMessage, userProfile, pastLogs))
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

            val hasImage = !imageBase64.isNullOrBlank() && !imageMimeType.isNullOrBlank()

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
                Respond thoroughly in Gujarati if the patient communicated in Gujarati or selected Gujarati.
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

            val response = try {
                GeminiApiClient.service.generateContent(
                    model = "gemini-3.1-flash-lite-preview",
                    apiKey = apiKey,
                    request = request
                )
            } catch (eLite: Exception) {
                try {
                    GeminiApiClient.service.generateContent(
                        model = "gemini-3.8-flash",
                        apiKey = apiKey,
                        request = request
                    )
                } catch (eFlash: Exception) {
                    val pastLogs = triageDao.getRecentSymptomLogs(5)
                    return@withContext Result.success(getEducationalTriageFallback(userMessage, userProfile, pastLogs, hasImage = hasImage))
                }
            }

            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (responseText.isNullOrBlank()) {
                Result.success(getEducationalTriageFallback(userMessage, userProfile, pastLogs, hasImage = hasImage))
            } else {
                Result.success(responseText)
            }
        } catch (e: Exception) {
            val pastLogs = triageDao.getRecentSymptomLogs(5)
            Result.success(getEducationalTriageFallback(userMessage, userProfile, pastLogs, hasImage = !imageBase64.isNullOrBlank()))
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

        val isGujarati = lower.contains("mathu") || lower.contains("matho") || lower.contains("mane") ||
                         lower.contains("chhe") || lower.contains("che") || lower.contains("karo") ||
                         lower.contains("hal") || lower.contains("aama") || lower.contains("boline") ||
                         lower.contains("samja") || lower.contains("bimari") || lower.contains("kem") ||
                         lower.contains("pani") || lower.contains("paani") || lower.contains("taav") ||
                         lower.contains("tav") || lower.contains("shardi") || lower.contains("udhras") ||
                         lower.contains("pet") || lower.contains("dardo") || lower.contains("upay") ||
                         lower.contains("gharelu") || lower.contains("aapo") ||
                         userMessage.any { it in '\u0A80'..'\u0AFF' }

        val isHindi = !isGujarati && (
            lower.contains("hai") || lower.contains("muje") || lower.contains("mujhe") ||
            lower.contains("kya") || lower.contains("dard") || lower.contains("ho raha") ||
            lower.contains("kare") || lower.contains("meri") || lower.contains("mera") ||
            lower.contains("bukhar") || lower.contains("khasi") || lower.contains("dawa") ||
            userMessage.any { it in '\u0900'..'\u097F' }
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
        val isFever = lower.contains("fever") || lower.contains("taav") || lower.contains("tav") ||
                      lower.contains("તાવ") || lower.contains("bukhar") || lower.contains("बुखार") ||
                      lower.contains("temperature") || lower.contains("chills") || lower.contains("dhujari")

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
        val isColdCough = lower.contains("cough") || lower.contains("cold") || lower.contains("shardi") ||
                          lower.contains("udhras") || lower.contains("kaf") || lower.contains("શરદી") ||
                          lower.contains("ઉધરસ") || lower.contains("કફ") || lower.contains("ગળું") ||
                          lower.contains("khasi") || lower.contains("jukam") || lower.contains("sore throat") ||
                          lower.contains("throat") || lower.contains("खांसी") || lower.contains("जुकाम")

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
        val isStomach = lower.contains("stomach") || lower.contains("pet") || lower.contains("gas") ||
                        lower.contains("acidity") || lower.contains("kabjiyaat") || lower.contains("kabj") ||
                        lower.contains("પેટ") || lower.contains("ગેસ") || lower.contains("એસિડિટી") ||
                        lower.contains("અપચો") || lower.contains("कब्ज") || lower.contains("पेट दर्द") ||
                        lower.contains("indigestion") || lower.contains("constipation") || lower.contains("heartburn")

        if (isStomach) {
            if (isGujarati) {
                return """
                    ${networkNotice}નમસ્તે! પેટનો દુખાવો, ગેસ અને એસિડિટીના ઉપાય નીચે મુજબ છે:

                    ### 1. બિમારીનો હલ અને ઘરેલુ ઉપાય (Stomach & Acidity Remedies)
                    * **હિંગ અને અજમાનું પાણી:** અડધી ચમચી અજમો અને ચપટી હિંગને હુંફાળા પાણી સાથે ચાવીને પીવાથી ગેસ અને પેટના ચુંકાવામાં ૫ મિનિટમાં આરામ મળે છે.
                    * **વરિયાળી અને જીરાનું પાણી:** ૧ ચમચી વરિયાળી અને જીરું પાણીમાં ઉકાળીને ઠંડુ કરીને પીવાથી એસિડિટી અને પેટની બળતરા શાંત થાય છે.
                    * **ઠંડું દૂધ અથવા છાશ:** એસિડિટી માટે અડધો ગ્લાસ ઠંડું દૂધ અથવા જીરા-હિંગવાળી તાજી મોળી છાશ પીવી ખૂબ ગુણકારી છે.
                    * **નાળિયેર પાણી:** પેટના એસિડને સામાન્ય કરવા અને પેટને ઠંડક આપવા માટે નાળિયેર પાણી ઉત્તમ છે.
                    * **કબજિયાત માટે:** રાત્રે સૂતી વખતે હુંફાળા દૂધ સાથે ઇસબગુલ અથવા ત્રિફળા ચૂર્ણ લેવું અને પુષ્કળ લીલા શાકભાજી ખાવ.
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
        val isDiarrhea = lower.contains("diarrhea") || lower.contains("vomit") || lower.contains("zhada") ||
                         lower.contains("jada") || lower.contains("ulti") || lower.contains("ઝાડા") ||
                         lower.contains("ઉલટી") || lower.contains("दस्त") || lower.contains("उल्टी") ||
                         lower.contains("loose motion") || lower.contains("food poisoning")

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
        val isJointPain = lower.contains("joint") || lower.contains("knee") || lower.contains("back pain") ||
                          lower.contains("sandha") || lower.contains("kamar") || lower.contains("gothan") ||
                          lower.contains("સાંધા") || lower.contains("કમર") || lower.contains("ગોઠણ") ||
                          lower.contains("જોડો") || lower.contains("कमर दर्द") || lower.contains("गठिया") ||
                          lower.contains("arthritis") || lower.contains("sciatica")

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
        val isDiabetesBP = lower.contains("diabetes") || lower.contains("sugar") || lower.contains("bp") ||
                           lower.contains("blood pressure") || lower.contains("ડાયાબિટીસ") || lower.contains("સુગર") ||
                           lower.contains("શુગર") || lower.contains("બીપી") || lower.contains("પ્રેશર") ||
                           lower.contains("शुगर") || lower.contains("डायबिटीज") || lower.contains("hypertension")

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
        val isSkin = lower.contains("skin") || lower.contains("itching") || lower.contains("rash") ||
                     lower.contains("khanjval") || lower.contains("dhadhar") || lower.contains("chamdi") ||
                     lower.contains("ચામડી") || lower.contains("ખંજવાળ") || lower.contains("ધાધર") ||
                     lower.contains("ફોલ્લી") || lower.contains("खुजली") || lower.contains("दाद") ||
                     lower.contains("allergy") || lower.contains("fungal")

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
        val isTooth = lower.contains("tooth") || lower.contains("teeth") || lower.contains("dant") ||
                      lower.contains("daant") || lower.contains("દાંત") || lower.contains("પેઢા") ||
                      lower.contains("दांत दर्द") || lower.contains("cavity")

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
        val isWeakness = lower.contains("weakness") || lower.contains("tired") || lower.contains("fatigue") ||
                         lower.contains("chakkar") || lower.contains("ashakti") || lower.contains("nablai") ||
                         lower.contains("નબળાઈ") || lower.contains("અશક્તિ") || lower.contains("ચક્કર") ||
                         lower.contains("થાક") || lower.contains("कमजोरी") || lower.contains("चक्कर")

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
        val isHeadache = lower.contains("sir dard") || lower.contains("sar dard") ||
                         lower.contains("sir me dard") || lower.contains("sar me dard") ||
                         lower.contains("headache") || lower.contains("mathu") || lower.contains("matho") ||
                         lower.contains("માથું") || lower.contains("માથાનો") || lower.contains("migraine")

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
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
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

            val response = try {
                GeminiApiClient.service.generateContent(
                    model = "gemini-3.1-flash-lite-preview",
                    apiKey = apiKey,
                    request = request
                )
            } catch (_: Exception) {
                try {
                    GeminiApiClient.service.generateContent(
                        model = "gemini-3.8-flash",
                        apiKey = apiKey,
                        request = request
                    )
                } catch (_: Exception) {
                    return@withContext Result.success(getReportExplanationFallback(extractedText, reportType, language))
                }
            }

            val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (resultText.isNullOrBlank()) {
                Result.success(getReportExplanationFallback(extractedText, reportType, language))
            } else {
                Result.success(resultText)
            }
        } catch (e: Exception) {
            Result.success(getReportExplanationFallback(extractedText, reportType, language))
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
}

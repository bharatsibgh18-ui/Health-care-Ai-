package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerateContentRequest
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.local.ChatMessageEntity
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
        userProfile: UserProfileEntity?
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

            val fullPrompt = if (contentsList.isEmpty()) {
                "${healthHistoryBuilder}$userMessage"
            } else {
                userMessage
            }

            contentsList.add(
                GeminiContent(
                    parts = listOf(GeminiPart(text = fullPrompt)),
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

            val response = GeminiApiClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (responseText.isNullOrBlank()) {
                Result.success(getEducationalTriageFallback(userMessage, userProfile, pastLogs))
            } else {
                Result.success(responseText)
            }
        } catch (e: Exception) {
            val pastLogs = triageDao.getRecentSymptomLogs(5)
            Result.success(getEducationalTriageFallback(userMessage, userProfile, pastLogs))
        }
    }

    private fun getEducationalTriageFallback(
        userMessage: String,
        profile: UserProfileEntity?,
        pastLogs: List<SymptomLogEntity> = emptyList(),
        networkNotice: String = ""
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

        val isHeadache = lower.contains("sir dard") || lower.contains("sar dard") ||
                         lower.contains("sir me dard") || lower.contains("sar me dard") ||
                         lower.contains("headache") || lower.contains("mathu") || lower.contains("matho")

        val isHindi = lower.contains("hai") || lower.contains("muje") || lower.contains("mujhe") ||
                      lower.contains("kya") || lower.contains("dard") || lower.contains("ho raha") ||
                      lower.contains("kare") || lower.contains("meri") || lower.contains("mera")

        // Personalization note based on Room database
        val previousHeadache = pastLogs.firstOrNull {
            it.symptomTitle.contains("headache", ignoreCase = true) ||
            it.symptomTitle.contains("sir dard", ignoreCase = true) ||
            it.symptomTitle.contains("sar dard", ignoreCase = true)
        }

        val conditionNoteHindi = if (profile != null && profile.chronicConditions != "None") {
            "• आपके प्रोफाइल में दर्ज पुरानी स्थिति (${profile.chronicConditions}) और दवाओं (${profile.currentMedications}) को ध्यान में रखते हुए,\n"
        } else ""

        val historyNoteHindi = if (previousHeadache != null) {
            "• आपके स्वास्थ्य रिकॉर्ड के अनुसार आपको पहले भी सिरदर्द दर्ज हुआ था (गंभीरता: ${previousHeadache.severity}/10, स्थान: ${previousHeadache.bodyLocation})। क्या यह दर्द पहले जैसा ही लग रहा है?\n"
        } else ""

        val previousHeadacheEng = pastLogs.firstOrNull { it.symptomTitle.contains("headache", ignoreCase = true) }
        val historyNoteEng = if (previousHeadacheEng != null) {
            "• I note from your health history that you previously recorded a headache (Severity: ${previousHeadacheEng.severity}/10, Location: ${previousHeadacheEng.bodyLocation}). Does this feel similar to your previous episode?\n"
        } else ""

        val conditionNoteEng = if (profile != null && profile.chronicConditions != "None") {
            "• Considering your recorded profile conditions (${profile.chronicConditions}) and medications (${profile.currentMedications}):\n"
        } else ""

        val isGujarati = lower.contains("mathu") || lower.contains("matho") || lower.contains("mane") ||
                         lower.contains("chhe") || lower.contains("che") || lower.contains("karo") ||
                         lower.contains("hal") || lower.contains("aama") || lower.contains("boline") ||
                         lower.contains("samja") || lower.contains("bimari") || lower.contains("kem") ||
                         lower.contains("pani") || lower.contains("paani") ||
                         userMessage.any { it in '\u0A80'..'\u0AFF' }

        if (isGujarati) {
            return """
                ${networkNotice}નમસ્તે! બિમારીનો હલ અને ઘરેલુ ઉપાય નીચે મુજબ છે:

                ### 1. બિમારીનો હલ અને ઘરેલુ ઉપાય (Solution & Remedies)
                * **પૂરતું પાણી પીવો:** શરીરમાં પાણીની અછત (Dehydration) મોટાભાગના માથાના દુખાવા અને અશક્તિનું મુખ્ય કારણ હોય છે. તરત જ એક મોટો ગ્લાસ પાણી પીવો.
                * **આરામ કરો:** શાંત અને થોડા અંધારા વાળા ઓરડામાં 20-30 મિનિટ આંખો બંધ કરીને આરામ કરો.
                * **ઠંડો શેક કરો:** કપાળ અને ગરદન પર હળવો ઠંડો શેક (cold compress) મૂકવાથી ઝડપી રાહત મળે છે.
                * **સ્ક્રીનથી દૂર રહો:** મોબાઈલ કે ટીવીની સ્ક્રીન જોવાનું થોડીવાર માટે બંધ રાખો.

                ### 2. શક્ય સામાન્ય કારણો (Possible Causes)
                * માનસિક તાણ અથવા વધુ પડતો થાક (Tension Headache)
                * પાણી ઓછું પીવું અથવા ભૂખ્યા રહેવું
                * અધૂરી ઊંઘ અથવા આંખોમાં થાક

                ### 3. જોખમી લક્ષણો (Red Flags to Watch For)
                નીચે મુજબના લક્ષણો જણાય તો તરત જ દવાખાને જવું:
                * અચાનક વીજળીના આંચકા જેવો અત્યંત અસહ્ય દુખાવો
                * તાવ સાથે ગરદન અકડાઈ જવી
                * ચક્કર આવવા, બોલવામાં તકલીફ અથવા બેહોશી

                ### 4. ડૉક્ટરની સલાહ (Recommended Doctor)
                * વધુ તપાસ માટે **જનરલ ફિઝિશિયન (General Physician)** નો સંપર્ક કરવો.

                Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
            """.trimIndent()
        }

        if (isHeadache) {
            if (isHindi) {
                return """
                    ${networkNotice}नमस्ते! सिरदर्द एक बहुत ही आम लक्षण है और अधिकांश मामलों में यह किसी गंभीर बीमारी का संकेत नहीं होता है। आपके स्वास्थ्य इतिहास के आधार पर व्यक्तिगत क्लिनिकल सलाह:

                    ### 1. व्यक्तिगत क्लिनिकल सवाल (Personalized Follow-up Questions)
                    $historyNoteHindi$conditionNoteHindi* **अवधि (Duration):** यह सिरदर्द कब से शुरू हुआ है और क्या यह लगातार है या आ-जा रहा है?
                    * **गंभीरता (Severity):** 1 से 10 के पैमाने पर दर्द कितना तेज है?
                    * **स्थान (Location):** दर्द सिर के किस हिस्से में है — माथे पर, दोनों तरफ, या सिर्फ एक तरफ?
                    * **संभावित ट्रिगर:** क्या हाल ही में पानी कम पिया था, नींद अधूरी रही, या मोबाइल/कंप्यूटर पर ज्यादा समय बीता?

                    ### 2. संभावित सामान्य कारण (Differential Possibilities)
                    * **संभावित सामान्य कारण:** तनाव या मानसिक थकान (Tension headache), पानी की कमी (Dehydration), अपर्याप्त नींद, या आंखों पर तनाव।
                    * **कम सामान्य कारण:** माइग्रेन (Migraine), साइनसाइटिस (Sinusitis), या मौसम में अचानक बदलाव।

                    ### 3. सुरक्षित घरेलू देखभाल (Actionable Self-Care Advice)
                    * पर्याप्त मात्रा में सादा पानी पिएं।
                    * शांत और हल्के अंधेरे कमरे में आराम करें।
                    * माथे पर हल्का ठंडा या गुनगुना सेंक करें।
                    * स्क्रीन और तेज रोशनी से कुछ समय के लिए दूरी बनाएं।
                    * यदि आवश्यक हो तो डॉक्टर या फार्मासिस्ट की सलाह से ही कोई सामान्य दवा लें।

                    ### 4. ध्यान देने योग्य खतरे के संकेत (Red Flags to Watch For)
                    यदि इनमें से कोई लक्षण दिखे तो तुरंत डॉक्टर के पास जाएं:
                    * अचानक बिजली के झटके जैसा असहनीय सिरदर्द (Thunderclap headache)।
                    * सिरदर्द के साथ तेज बुखार और गर्दन का अकड़ना।
                    * बोलने में लड़खड़ाहट, कमजोरी या बेहोशी।

                    ### 5. अनुशंसित स्वास्थ्य विशेषज्ञ (Recommended Specialist)
                    * **जनरल फिजिशियन (General Physician)** से व्यक्तिगत जांच कराएं।

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            } else {
                return """
                    ${networkNotice}Hello! A headache is a very common symptom and is usually manageable with conservative care. Here is your personalized triage evaluation:

                    ### 1. Personalized Follow-up Questions
                    $historyNoteEng$conditionNoteEng* **Onset & Duration:** How long has this headache lasted, and is it constant or pulsing?
                    * **Severity (1-10):** On a scale of 1 to 10, how intense is the discomfort right now?
                    * **Location:** Is the pain across your forehead, temples, or localized to one side?
                    * **Triggers:** Did this begin after prolonged screen work, dehydration, or poor sleep?

                    ### 2. Symptom Analysis (Differential Possibilities)
                    * **Possible Common Causes:** Tension headache, dehydration, eye strain, or lack of restorative sleep.
                    * **Less Common Causes:** Migraine, sinus inflammation, or cervical muscle tension.

                    ### 3. Actionable Self-Care & Lifestyle Advice
                    * Drink 1-2 glasses of water to ensure hydration.
                    * Rest in a quiet, dark room with eyes closed for 20-30 minutes.
                    * Place a cool compress on your forehead or temples.
                    * Avoid digital screens until feeling better.

                    ### 4. Red Flags to Watch For
                    Seek immediate medical attention if you experience:
                    * Sudden explosive 'thunderclap' headache (worst headache of your life).
                    * High fever with neck stiffness or confusion.
                    * Neurological deficits such as numbness or slurred speech.

                    ### 5. Recommended Healthcare Specialist
                    * **General Physician / Family Doctor** for physical examination.

                    Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
                """.trimIndent()
            }
        }

        // General Hindi response
        if (isHindi) {
            return """
                ${networkNotice}### 1. व्यक्तिगत क्लिनिकल सवाल (Personalized Follow-up Questions)
                ${conditionNoteHindi}अपने स्वास्थ्य लक्षण साझा करने के लिए धन्यवाद। बेहतर स्वास्थ्य शिक्षा के लिए कृपया कुछ बातें स्पष्ट करें:
                * **अवधि (Duration):** यह समस्या कब से शुरू हुई है और क्या यह लगातार बनी हुई है?
                * **गंभीरता (Severity):** 1 से 10 के पैमाने पर आपको कितना दर्द या असहजता महसूस हो रही है?
                * **स्थान व अन्य लक्षण:** क्या इसके साथ बुखार, उल्टी, कमजोरी या कोई अन्य परेशानी है?

                ### 2. संभावित सामान्य कारण (Differential Possibilities)
                * **संभावित सामान्य कारण:** मौसम में बदलाव, खान-पान में अनियमितता, तनाव, हल्की वायरल समस्या या पर्याप्त आराम न मिलना।
                * **कम सामान्य कारण:** अंदरूनी सूजन, एलर्जी या संक्रमण।

                ### 3. सुरक्षित घरेलू देखभाल (Actionable Self-Care Advice)
                * पर्याप्त मात्रा में पानी पिएं और शरीर को हाइड्रेटेड रखें।
                * भारी काम से बचें और भरपूर आराम करें।
                * हल्का और सुपाच्य भोजन लें।
                * कोई भी दवा लेने से पहले डॉक्टर/फार्मासिस्ट से परामर्श करें।

                ### 4. ध्यान देने योग्य खतरे के संकेत (Red Flags to Watch For)
                यदि तेज बुखार, सांस लेने में भारी तकलीफ, असहनीय दर्द या बेहोशी जैसे लक्षण हों तो तुरंत अस्पताल जाएं।

                ### 5. अनुशंसित विशेषज्ञ (Recommended Specialist)
                * **जनरल फिजिशियन (General Physician)** से व्यक्तिगत जांच कराएं।

                Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
            """.trimIndent()
        }

        return """
            ${networkNotice}### 1. Personalized Follow-up Questions
            ${conditionNoteEng}Thank you for sharing your symptoms. To provide accurate clinical guidance:
            * **Onset & Duration:** When did this symptom start, and is it constant or intermittent?
            * **Severity (1-10):** How severe is the discomfort on a scale of 1 to 10?
            * **Associated Signs:** Are you experiencing fever, nausea, dizziness, or localized swelling?

            ### 2. Symptom Analysis (Differential Possibilities)
            * **Possible Common Causes:** Mild viral infections, tension or stress, mild muscle strain, or dehydration depending on daily routine.
            * **Less Common Causes:** Underlying localized inflammation or allergic sensitivity.

            ### 3. Actionable Self-Care & Lifestyle Advice
            * Rest in a comfortable position and stay well-hydrated.
            * Avoid heavy exertion while feeling unwell.
            * For mild discomfort, conservative non-medicinal remedies are recommended.
            * Always consult a pharmacist before taking any OTC remedies.

            ### 4. Red Flags to Watch For
            Seek immediate medical attention if you develop high fever, shortness of breath, chest tightness, or severe worsening pain.

            ### 5. Recommended Healthcare Specialist
            * **General Physician / Family Doctor** for comprehensive initial physical examination.

            Disclaimer: This guidance is for educational purposes only and cannot replace professional medical judgment. Please consult a qualified doctor for personalized diagnosis and treatment.
        """.trimIndent()
    }
}

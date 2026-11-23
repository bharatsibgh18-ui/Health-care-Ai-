package com.example.utils

import com.example.data.local.ScannedReportEntity
import com.example.data.local.StepRecordEntity
import com.example.data.local.SymptomLogEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.VitalLogEntity
import com.example.ui.theme.trLang

data class PredictiveRiskSnapshot(
    val heartRiskScore: Int,
    val sepsisRiskScore: Int,
    val metabolicRiskScore: Int,
    val overallRiskLevel: String,
    val heartFactors: List<String>,
    val sepsisFactors: List<String>,
    val metabolicFactors: List<String>,
    val immediateActions: List<String>,
    val dataPointsAnalyzed: Int
)

data class ExtractedRxItem(
    val name: String,
    val dosage: String,
    val timeOfDay: String,
    val instruction: String
)

data class ParsedClinicalDocument(
    val soapNote: String,
    val medicalReport: String,
    val prescriptionSummary: String,
    val extractedMedicines: List<ExtractedRxItem>,
    val detectedSymptoms: List<String>,
    val detectedVitals: List<String>
)

object ClinicalRiskEngine {

    fun computePredictiveRisks(
        profile: UserProfileEntity?,
        vitalLogs: List<VitalLogEntity>,
        symptomLogs: List<SymptomLogEntity>,
        stepRecords: List<StepRecordEntity>,
        scannedReports: List<ScannedReportEntity>,
        language: String
    ): PredictiveRiskSnapshot {
        val age = profile?.age ?: 30
        val conditions = profile?.chronicConditions?.lowercase().orEmpty()
        val surgeries = profile?.pastSurgeries?.lowercase().orEmpty()

        // Latest vitals
        val latestBp = vitalLogs.firstOrNull { it.vitalType.equals("Blood Pressure", ignoreCase = true) }
        val latestSugar = vitalLogs.firstOrNull { it.vitalType.equals("Blood Sugar", ignoreCase = true) }
        val latestHr = vitalLogs.firstOrNull { it.vitalType.equals("Heart Rate", ignoreCase = true) }
        val latestSpo2 = vitalLogs.firstOrNull { it.vitalType.equals("SpO2", ignoreCase = true) }
        val latestSleep = vitalLogs.firstOrNull { it.vitalType.equals("Sleep", ignoreCase = true) }
        val avgSteps = if (stepRecords.isNotEmpty()) stepRecords.map { it.steps }.average().toInt() else -1

        // 1. Cardiovascular Risk Score (0-100)
        var heartScore = 10
        val heartFactors = mutableListOf<String>()

        if (age >= 60) {
            heartScore += 20
            heartFactors += trLang(language, "Age >= 60 years ($age)", "ઉંમર ૬૦+ વર્ષ ($age)", "आयु 60+ वर्ष ($age)")
        } else if (age >= 45) {
            heartScore += 10
            heartFactors += trLang(language, "Age >= 45 years ($age)", "ઉંમર ૪૫+ વર્ષ ($age)", "आयु 45+ वर्ष ($age)")
        }

        if (conditions.contains("hypertension") || conditions.contains("bp") || conditions.contains("heart") || conditions.contains("cardiac") || conditions.contains("cholesterol")) {
            heartScore += 22
            heartFactors += trLang(language, "Chronic cardiac/BP history in profile", "પ્રોફાઇલમાં BP/હૃદય રોગનો ઇતિહાસ", "प्रोफ़ाइल में BP/हृदय रोग इतिहास")
        }
        if (conditions.contains("diabetes") || conditions.contains("sugar")) {
            heartScore += 12
            heartFactors += trLang(language, "Diabetes elevates cardiovascular strain", "ડાયાબિટીસથી હૃદય પર જોખમ વધે", "डायबिटीज से हृदय जोखिम बढ़ता है")
        }

        if (latestBp != null) {
            val sys = latestBp.primaryValue
            val dia = latestBp.secondaryValue
            when {
                sys >= 160 || dia >= 100 -> {
                    heartScore += 30
                    heartFactors += trLang(language, "Stage 2 High BP (${sys.toInt()}/${dia.toInt()} mmHg)", "ખૂબ ઊંચું BP (${sys.toInt()}/${dia.toInt()} mmHg)", "बहुत उच्च BP (${sys.toInt()}/${dia.toInt()} mmHg)")
                }
                sys >= 140 || dia >= 90 -> {
                    heartScore += 20
                    heartFactors += trLang(language, "Elevated BP (${sys.toInt()}/${dia.toInt()} mmHg)", "ઊંચું BP (${sys.toInt()}/${dia.toInt()} mmHg)", "उच्च BP (${sys.toInt()}/${dia.toInt()} mmHg)")
                }
                sys >= 130 -> {
                    heartScore += 10
                    heartFactors += trLang(language, "Borderline BP (${sys.toInt()}/${dia.toInt()} mmHg)", "બોર્ડરલાઇન BP (${sys.toInt()}/${dia.toInt()} mmHg)", "बॉर्डरलाइन BP (${sys.toInt()}/${dia.toInt()} mmHg)")
                }
            }
        }

        if (latestHr != null && (latestHr.primaryValue >= 100 || latestHr.primaryValue < 50)) {
            heartScore += 14
            heartFactors += trLang(language, "Abnormal resting HR (${latestHr.primaryValue.toInt()} bpm)", "અસામાન્ય હાર્ટ રેટ (${latestHr.primaryValue.toInt()} bpm)", "असामान्य हार्ट रेट (${latestHr.primaryValue.toInt()} bpm)")
        }

        if (avgSteps in 0..3499) {
            heartScore += 10
            heartFactors += trLang(language, "Low daily activity (~$avgSteps steps/day)", "ઓછી શારીરિક પ્રવૃત્તિ (~$avgSteps પગલાં/દિવસ)", "कम शारीरिक गतिविधि (~$avgSteps कदम/दिन)")
        }

        val chestSymptoms = symptomLogs.take(10).any {
            val s = it.symptomTitle.lowercase()
            s.contains("chest") || s.contains("palpitation") || s.contains("breath") || s.contains("છાતી") || s.contains("સીને")
        }
        if (chestSymptoms) {
            heartScore += 15
            heartFactors += trLang(language, "Recent chest/breathing symptom logged", "તાજેતરમાં છાતી/શ્વાસના લક્ષણો નોંધાયા છે", "हाल ही में सीने/सांस के लक्षण दर्ज हुए")
        }

        if (heartFactors.isEmpty()) {
            heartFactors += trLang(language, "No major cardiac risk triggers found in current logs", "હાલના રેકોર્ડમાં હૃદય રોગનું મોટું જોખમ નથી", "वर्तमान रिकॉर्ड में कोई बड़ा हृदय जोखिम नहीं मिला")
        }

        // 2. Sepsis & Systemic Infection Early Warning Score (qSOFA / SIRS-inspired, 0-100)
        var sepsisScore = 6
        val sepsisFactors = mutableListOf<String>()

        val recentFeverLogs = symptomLogs.take(10).filter {
            val t = (it.symptomTitle + " " + it.notesOrTriggers).lowercase()
            t.contains("fever") || t.contains("chills") || t.contains("infection") ||
                t.contains("taav") || t.contains("તાવ") || t.contains("bukhar") || t.contains("बुखार") ||
                t.contains("sepsis") || t.contains("confusion")
        }
        if (recentFeverLogs.isNotEmpty()) {
            val maxSev = recentFeverLogs.maxOf { it.severity }
            sepsisScore += if (maxSev >= 7) 26 else 14
            sepsisFactors += trLang(language, "Recent febrile/infection symptom (severity $maxSev/10)", "તાજેતરમાં તાવ/ચેપના લક્ષણો (તીવ્રતા $maxSev/10)", "हाल में बुखार/संक्रमण लक्षण (तीव्रता $maxSev/10)")
        }

        if (latestHr != null && latestHr.primaryValue >= 95) {
            sepsisScore += if (latestHr.primaryValue >= 110) 24 else 14
            sepsisFactors += trLang(language, "Tachycardia signal: HR ${latestHr.primaryValue.toInt()} bpm", "વધેલો હાર્ટ રેટ: ${latestHr.primaryValue.toInt()} bpm", "तेज़ हार्ट रेट: ${latestHr.primaryValue.toInt()} bpm")
        }

        if (latestSpo2 != null && latestSpo2.primaryValue <= 94) {
            sepsisScore += if (latestSpo2.primaryValue <= 90) 32 else 18
            sepsisFactors += trLang(language, "Low Blood Oxygen (SpO2 ${latestSpo2.primaryValue.toInt()}%)", "ઓછું ઓક્સિજન સ્તર (SpO2 ${latestSpo2.primaryValue.toInt()}%)", "कम ऑक्सीजन स्तर (SpO2 ${latestSpo2.primaryValue.toInt()}%)")
        }

        if (latestBp != null && latestBp.primaryValue <= 95) {
            sepsisScore += 24
            sepsisFactors += trLang(language, "Low Systolic BP (${latestBp.primaryValue.toInt()} mmHg)", "ઓછું BP (${latestBp.primaryValue.toInt()} mmHg)", "कम सिस्टोलिक BP (${latestBp.primaryValue.toInt()} mmHg)")
        }

        if (surgeries.contains("surgery") || surgeries.contains("implant") || conditions.contains("kidney") || conditions.contains("immune")) {
            sepsisScore += 10
            sepsisFactors += trLang(language, "Surgical / immune sensitivity context", "સર્જરી / ઇમ્યુનિટી સંવેદનશીલતા", "सर्जरी / इम्युनिटी संवेदनशीलता")
        }

        if (sepsisFactors.isEmpty()) {
            sepsisFactors += trLang(language, "SIRS/qSOFA baseline markers appear stable", "તાવ, SpO2 અને ચેપના પ્રારંભિક સંકેતો સામાન્ય છે", "बुखार, SpO2 और संक्रमण के शुरुआती संकेत सामान्य हैं")
        }

        // 3. Metabolic & Lifestyle Trajectory Risk (0-100)
        var metabolicScore = 10
        val metabolicFactors = mutableListOf<String>()

        if (latestSugar != null) {
            when {
                latestSugar.primaryValue >= 200 -> {
                    metabolicScore += 36
                    metabolicFactors += trLang(language, "High Blood Glucose (${latestSugar.primaryValue.toInt()} mg/dL)", "ખૂબ ઊંચું સુગર (${latestSugar.primaryValue.toInt()} mg/dL)", "बहुत उच्च शुगर (${latestSugar.primaryValue.toInt()} mg/dL)")
                }
                latestSugar.primaryValue >= 140 -> {
                    metabolicScore += 22
                    metabolicFactors += trLang(language, "Elevated Blood Glucose (${latestSugar.primaryValue.toInt()} mg/dL)", "ઊંચું બ્લડ સુગર (${latestSugar.primaryValue.toInt()} mg/dL)", "उच्च ब्लड शुगर (${latestSugar.primaryValue.toInt()} mg/dL)")
                }
                latestSugar.primaryValue < 70 -> {
                    metabolicScore += 24
                    metabolicFactors += trLang(language, "Hypoglycemia Alert (${latestSugar.primaryValue.toInt()} mg/dL)", "લો સુગર ચેતવણી (${latestSugar.primaryValue.toInt()} mg/dL)", "लो शुगर चेतावनी (${latestSugar.primaryValue.toInt()} mg/dL)")
                }
            }
        }
        if (conditions.contains("diabetes") || conditions.contains("thyroid") || conditions.contains("pcos") || conditions.contains("obesity")) {
            metabolicScore += 22
            metabolicFactors += trLang(language, "Metabolic/Endocrine history recorded", "મેટાબોલિક/ડાયાબિટીસ/થાઇરોઇડ ઇતિહાસ", "मेटाबोलिक/थायरॉइड/शुगर इतिहास")
        }
        if (latestSleep != null && latestSleep.primaryValue < 6.0) {
            metabolicScore += 14
            metabolicFactors += trLang(language, "Sleep deficit (${latestSleep.primaryValue} hrs/night)", "ઊંઘની અછત (${latestSleep.primaryValue} કલાક)", "नींद की कमी (${latestSleep.primaryValue} घंटे)")
        }
        if (scannedReports.any { it.extractedText.lowercase().contains("hba1c") || it.extractedText.lowercase().contains("cholesterol") }) {
            metabolicScore += 8
            metabolicFactors += trLang(language, "Lab markers referenced in scanned reports", "સ્કેન કરેલા રિપોર્ટમાં લેબ માર્કર મળ્યા", "स्कैन की गई रिपोर्ट में लैब मार्कर मिले")
        }
        if (metabolicFactors.isEmpty()) {
            metabolicFactors += trLang(language, "Glucose, sleep, and metabolic baseline within range", "સુગર અને ઊંઘના આંકડા સામાન્ય રેન્જમાં છે", "शुगर और नींद के आंकड़े सामान्य सीमा में हैं")
        }

        val clampedHeart = heartScore.coerceIn(5, 96)
        val clampedSepsis = sepsisScore.coerceIn(5, 96)
        val clampedMetabolic = metabolicScore.coerceIn(5, 96)
        val maxRisk = maxOf(clampedHeart, clampedSepsis, clampedMetabolic)

        val overallLevel = when {
            maxRisk >= 70 -> trLang(language, "High Alert", "ઉચ્ચ ચેતવણી", "उच्च चेतावनी")
            maxRisk >= 45 -> trLang(language, "Elevated", "વધેલું જોખમ", "बढ़ा हुआ जोखिम")
            maxRisk >= 25 -> trLang(language, "Moderate", "મધ્યમ", "मध्यम")
            else -> trLang(language, "Low Risk", "ઓછું જોખમ", "कम जोखिम")
        }

        val actions = mutableListOf<String>()
        if (clampedSepsis >= 45) {
            actions += trLang(
                language,
                "Monitor temperature, SpO2 (<94% alert), and pulse every 2-4 hours; consult a doctor promptly if fever persists.",
                "દર ૨-૪ કલાકે તાપમાન, SpO2 (<94%) અને નાડી તપાસો; તાવ રહે તો તાત્કાલિક ડૉક્ટરને બતાવો.",
                "हर 2-4 घंटे में तापमान, SpO2 (<94%) और पल्स जांचें; बुखार रहने पर तुरंत डॉक्टर से मिलें।"
            )
        }
        if (clampedHeart >= 45) {
            actions += trLang(
                language,
                "Track morning & evening Blood Pressure, limit sodium intake, and schedule a Lipid Profile / ECG checkup.",
                "સવાર-સાંજ BP માપો, મીઠું ઓછું કરો અને ECG / Lipid Profile તપાસ કરાવો.",
                "सुबह-शाम BP मापें, नमक कम करें और ECG / लिपिड प्रोफाइल जांच कराएं।"
            )
        }
        if (clampedMetabolic >= 45) {
            actions += trLang(
                language,
                "Log fasting & post-meal blood sugar and aim for 7+ hrs sleep with 6,000-8,000 daily steps.",
                "ખાલી પેટે અને જમ્યા પછી સુગર તપાસો, ૭+ કલાક ઊંઘ અને રોજ ચાલવાનું રાખો.",
                "खाली पेट और भोजन के बाद शुगर जांचें, 7+ घंटे नींद और रोज़ टहलना जारी रखें।"
            )
        }
        if (actions.isEmpty()) {
            actions += trLang(
                language,
                "Continue weekly vitals logging and wearable sync to maintain accurate early-detection baselines.",
                "સચોટ પ્રારંભિક ચેતવણી માટે દર અઠવાડિયે વાઇટલ્સ અને વેરેબલ ડેટા લોગ કરતા રહો.",
                "सटीक प्रारंभिक पहचान के लिए हर हफ्ते वाइटल्स और वियरेबल डेटा लॉग करते रहें।"
            )
        }

        val dataPoints = (if (profile != null) 1 else 0) + vitalLogs.size + symptomLogs.size + stepRecords.size + scannedReports.size

        return PredictiveRiskSnapshot(
            heartRiskScore = clampedHeart,
            sepsisRiskScore = clampedSepsis,
            metabolicRiskScore = clampedMetabolic,
            overallRiskLevel = overallLevel,
            heartFactors = heartFactors,
            sepsisFactors = sepsisFactors,
            metabolicFactors = metabolicFactors,
            immediateActions = actions,
            dataPointsAnalyzed = dataPoints
        )
    }

    fun parseClinicalTranscriptLocally(
        transcript: String,
        patientName: String,
        language: String
    ): ParsedClinicalDocument {
        val clean = transcript.trim()
        val lower = clean.lowercase()

        // Extract spoken vitals via regex
        val vitalsFound = mutableListOf<String>()
        Regex("""(?:bp|blood pressure)\s*[:\-is]*\s*(\d{2,3}\s*/\s*\d{2,3})""", RegexOption.IGNORE_CASE)
            .findAll(clean).forEach { vitalsFound += "BP: ${it.groupValues[1].replace(" ", "")} mmHg" }
        Regex("""(?:spo2|oxygen|o2)\s*[:\-is]*\s*(\d{2,3})\s*%?""", RegexOption.IGNORE_CASE)
            .findAll(clean).forEach { vitalsFound += "SpO2: ${it.groupValues[1]}%" }
        Regex("""(?:hr|heart rate|pulse)\s*[:\-is]*\s*(\d{2,3})""", RegexOption.IGNORE_CASE)
            .findAll(clean).forEach { vitalsFound += "Pulse: ${it.groupValues[1]} bpm" }
        Regex("""(?:temp|temperature|fever)\s*[:\-is]*\s*(\d{2,3}(?:\.\d)?)\s*(?:f|c|°)?""", RegexOption.IGNORE_CASE)
            .findAll(clean).forEach { vitalsFound += "Temp: ${it.groupValues[1]}°" }
        Regex("""(?:sugar|glucose|rbs|fbs)\s*[:\-is]*\s*(\d{2,3})""", RegexOption.IGNORE_CASE)
            .findAll(clean).forEach { vitalsFound += "Blood Sugar: ${it.groupValues[1]} mg/dL" }

        // Detect key symptoms
        val symptomLexicon = listOf(
            "fever" to "Fever", "taav" to "Fever", "bukhar" to "Fever", "તાવ" to "Fever", "बुखार" to "Fever",
            "cough" to "Cough", "khasi" to "Cough", "udhras" to "Cough", "ઉધરસ" to "Cough", "खांसी" to "Cough",
            "headache" to "Headache", "sir dard" to "Headache", "માથું" to "Headache", "सिर दर्द" to "Headache",
            "chest pain" to "Chest Discomfort", "chest" to "Chest Discomfort",
            "shortness of breath" to "Dyspnea", "breathless" to "Dyspnea", "શ્વાસ" to "Dyspnea",
            "sore throat" to "Sore Throat", "throat" to "Sore Throat", "ગળું" to "Sore Throat",
            "stomach" to "Abdominal Pain", "acidity" to "Acidity/Gastritis", "vomiting" to "Vomiting",
            "dizziness" to "Dizziness", "fatigue" to "Fatigue", "weakness" to "Weakness",
            "joint pain" to "Arthralgia", "back pain" to "Back Pain", "rash" to "Skin Rash"
        )
        val detectedSymptoms = symptomLexicon
            .filter { lower.contains(it.first) }
            .map { it.second }
            .distinct()
            .ifEmpty { listOf(trLang(language, "General Clinical Consultation", "સામાન્ય તબીબી પરામર્શ", "सामान्य चिकित्सा परामर्श")) }

        // Extract medications using known clinical patterns
        val knownDrugs = listOf(
            "Paracetamol", "Dolo", "Crocin", "Ibuprofen", "Amoxicillin", "Azithromycin",
            "Augmentin", "Cefixime", "Pantoprazole", "Omeprazole", "Cetirizine", "Levocetirizine",
            "Metformin", "Amlodipine", "Telmisartan", "Atorvastatin", "Aspirin", "ORS",
            "Montelukast", "Vitamin D3", "Vitamin C", "Zinc", "Multivitamin"
        )

        val extractedMedicines = mutableListOf<ExtractedRxItem>()
        for (drug in knownDrugs) {
            val drugIndex = lower.indexOf(drug.lowercase())
            if (drugIndex >= 0) {
                val window = clean.substring(drugIndex, (drugIndex + 75).coerceAtMost(clean.length))
                val dosageMatch = Regex("""(\d+(?:\.\d+)?\s*(?:mg|mcg|ml|g|tablet|tab|cap|sachet)s?)""", RegexOption.IGNORE_CASE).find(window)
                val dosage = dosageMatch?.groupValues?.get(1) ?: "1 Tablet"
                val wLower = window.lowercase()
                val timeOfDay = when {
                    wLower.contains("night") || wLower.contains("hs") || wLower.contains("bedtime") -> "09:00 PM"
                    wLower.contains("noon") || wLower.contains("afternoon") -> "01:00 PM"
                    wLower.contains("evening") -> "06:00 PM"
                    else -> "08:00 AM"
                }
                val instruction = when {
                    wLower.contains("before food") || wLower.contains("empty stomach") -> "Before Food"
                    wLower.contains("bedtime") -> "At Bedtime"
                    wLower.contains("with water") -> "With Water"
                    else -> "After Food"
                }
                extractedMedicines += ExtractedRxItem(
                    name = drug,
                    dosage = dosage,
                    timeOfDay = timeOfDay,
                    instruction = instruction
                )
            }
        }

        val symptomsText = detectedSymptoms.joinToString(", ")
        val vitalsText = if (vitalsFound.isEmpty()) {
            trLang(language, "Not verbally recorded — check vitals monitor", "વાતચીતમાં નોંધાયેલ નથી — વાઇટલ્સ તપાસો", "बातचीत में दर्ज नहीं — वाइटल्स जांचें")
        } else {
            vitalsFound.distinct().joinToString(" | ")
        }

        val rxLines = if (extractedMedicines.isEmpty()) {
            trLang(
                language,
                "• No explicit Rx drug names detected in transcript. Add medicines manually or review with doctor.",
                "• વાતચીતમાં સ્પષ્ટ દવાના નામ મળ્યા નથી. ડૉક્ટરની સલાહ મુજબ દવા ઉમેરો.",
                "• बातचीत में स्पष्ट दवा का नाम नहीं मिला। डॉक्टर की सलाह अनुसार दवा जोड़ें।"
            )
        } else {
            extractedMedicines.mapIndexed { idx, rx ->
                "${idx + 1}. ${rx.name} — ${rx.dosage} (${rx.timeOfDay}, ${rx.instruction})"
            }.joinToString("\n")
        }

        val soapNote = when {
            language.equals("Gujarati", ignoreCase = true) -> """
                📋 ઓટોમેટેડ SOAP ક્લિનિકલ નોટ — દર્દી: $patientName
                
                • S (Subjective / દર્દીની ફરિયાદ):
                  મુખ્ય લક્ષણો: $symptomsText
                  વાતચીત સારાંશ: "${clean.take(260)}${if (clean.length > 260) "..." else ""}"
                
                • O (Objective / નોંધાયેલા વાઇટલ્સ):
                  $vitalsText
                
                • A (Assessment / પ્રાથમિક મૂલ્યાંકન):
                  $symptomsText ના લક્ષણોનું મૂલ્યાંકન કરેલ છે; ડૉક્ટર દ્વારા ક્લિનિકલ પુષ્ટિ જરૂરી છે.
                
                • P (Plan / સારવાર યોજના):
                  સૂચવેલ દવાઓ, પૂરતો આરામ, પ્રવાહી અને જો લક્ષણો વધે તો ફોલો-અપ વિઝિટ.
            """.trimIndent()
            language.equals("Hindi", ignoreCase = true) -> """
                📋 ऑटोमेटेड SOAP क्लिनिकल नोट — मरीज: $patientName
                
                • S (Subjective / मरीज की शिकायत):
                  मुख्य लक्षण: $symptomsText
                  बातचीत सारांश: "${clean.take(260)}${if (clean.length > 260) "..." else ""}"
                
                • O (Objective / दर्ज वाइटल्स):
                  $vitalsText
                
                • A (Assessment / प्रारंभिक आकलन):
                  $symptomsText से संबंधित लक्षण दर्ज किए गए; डॉक्टर द्वारा जांच आवश्यक।
                
                • P (Plan / उपचार योजना):
                  दवाइयों का नियमित पालन, पर्याप्त आराम, हाइड्रेशन और सुधार न होने पर फॉलो-अप।
            """.trimIndent()
            else -> """
                📋 AUTOMATED SOAP CLINICAL NOTE — Patient: $patientName
                
                • S (Subjective):
                  Chief Symptoms: $symptomsText
                  Encounter Excerpt: "${clean.take(260)}${if (clean.length > 260) "..." else ""}"
                
                • O (Objective):
                  Recorded Vitals: $vitalsText
                
                • A (Assessment):
                  Clinical presentation consistent with $symptomsText (pending licensed physician confirmation).
                
                • P (Plan):
                  Follow draft prescription instructions, maintain hydration/rest, and schedule follow-up if red flags occur.
            """.trimIndent()
        }

        val reportText = trLang(
            language,
            "Patient $patientName presented with $symptomsText.\nVitals captured: $vitalsText.\nRecommended next step: Doctor verification & routine monitoring.",
            "દર્દી $patientName ને $symptomsText ની ફરિયાદ છે.\nનોંધાયેલ વાઇટલ્સ: $vitalsText.\nઆગળનું પગલું: ડૉક્ટર દ્વારા ચકાસણી અને નિયમિત દેખરેખ.",
            "मरीज $patientName ने $symptomsText के लक्षण बताए।\nदर्ज वाइटल्स: $vitalsText.\nअगला कदम: डॉक्टर द्वारा सत्यापन और नियमित निगरानी।"
        )

        return ParsedClinicalDocument(
            soapNote = soapNote,
            medicalReport = reportText,
            prescriptionSummary = rxLines,
            extractedMedicines = extractedMedicines,
            detectedSymptoms = detectedSymptoms,
            detectedVitals = vitalsFound.distinct()
        )
    }
}

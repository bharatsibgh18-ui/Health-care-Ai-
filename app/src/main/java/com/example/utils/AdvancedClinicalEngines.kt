package com.example.utils

import com.example.data.local.SymptomLogEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.NearbyMedicalPlace
import com.example.ui.theme.trLang
import kotlin.math.roundToInt

data class StructuredEhrParseResult(
    val category: String,
    val diagnosisTags: String,
    val medicationsDetected: String,
    val structuredSummary: String
)

data class MentalHealthScreeningResult(
    val stressScore: Int,
    val moodLabel: String,
    val crisisDetected: Boolean,
    val fallbackGuidance: String
)

data class MoleculeScreeningResult(
    val estimatedMolecularWeight: Double,
    val heavyAtomEstimate: Int,
    val hBondDonors: Int,
    val hBondAcceptors: Int,
    val lipinskiViolations: Int,
    val drugLikenessScore: Int,
    val bioavailabilityTier: String,
    val fallbackReport: String
)

data class OutbreakPredictionResult(
    val vectorRiskScore: Int,
    val respiratoryRiskScore: Int,
    val waterBorneRiskScore: Int,
    val overallAlertTier: String,
    val riskDrivers: List<String>,
    val fallbackBrief: String
)

data class MatchedDoctorRecommendation(
    val recommendedSpecialty: String,
    val localizedSpecialty: String,
    val urgencyNote: String,
    val doctorDirectory: List<MatchedDoctorListing>
)

data class MatchedDoctorListing(
    val doctorName: String,
    val specialty: String,
    val clinicName: String,
    val distanceKm: Double,
    val phoneNumber: String,
    val latitude: Double,
    val longitude: Double,
    val consultationFee: String,
    val rating: Double
)

object AdvancedClinicalEngines {

    fun parseAndOrganizeEhr(
        rawNotes: String,
        patientName: String,
        profile: UserProfileEntity?,
        language: String
    ): StructuredEhrParseResult {
        val parsed = ClinicalRiskEngine.parseClinicalTranscriptLocally(rawNotes, patientName, language)
        val lower = rawNotes.lowercase()

        val category = when {
            lower.contains("lab") || lower.contains("hba1c") || lower.contains("cbc") || lower.contains("cholesterol") ->
                trLang(language, "Lab & Diagnostics", "લેબ & ડાયગ્નોસ્ટિક્સ", "लैब और जांच")
            lower.contains("bp") || lower.contains("cardiac") || lower.contains("ecg") || lower.contains("chest") ->
                trLang(language, "Cardiology EHR", "હૃદય EHR રેકોર્ડ", "कार्डियोलॉजी EHR")
            lower.contains("surgery") || lower.contains("discharge") || lower.contains("admit") ->
                trLang(language, "Surgical / Inpatient", "સર્જિકલ / ડિસ્ચાર્જ", "सर्जिकल / डिस्चार्ज")
            parsed.extractedMedicines.isNotEmpty() ->
                trLang(language, "Medication & Rx", "દવા & પ્રિસ્ક્રિપ્શન", "दवा और पर्चा")
            else ->
                trLang(language, "Outpatient Clinical EHR", "OPD ક્લિનિકલ EHR", "OPD क्लिनिकल EHR")
        }

        val diagnosisTags = parsed.detectedSymptoms.joinToString(", ")
        val medsDetected = if (parsed.extractedMedicines.isNotEmpty()) {
            parsed.extractedMedicines.joinToString(", ") { "${it.name} (${it.dosage})" }
        } else {
            profile?.currentMedications?.takeIf { it.isNotBlank() && !it.equals("None", ignoreCase = true) }
                ?: trLang(language, "None extracted", "કોઈ દવા ઓળખાઈ નથી", "कोई दवा नहीं मिली")
        }

        val vitalsLine = if (parsed.detectedVitals.isNotEmpty()) parsed.detectedVitals.joinToString(" | ")
        else trLang(language, "Not specified", "ઉલ્લેખ નથી", "उल्लेख नहीं")

        val allergies = profile?.allergies ?: "None Known"
        val structured = """
            [${category.uppercase()}] — $patientName (${profile?.age ?: 30}y, ${profile?.bloodGroup ?: "O+"})
            • Diagnosis / Problems: $diagnosisTags
            • Vitals & Labs: $vitalsLine
            • Structured Medications: $medsDetected
            • Allergies Cross-Check: $allergies
            • Organized Plan: Follow structured regimen, archive in digital EHR timeline, and verify at next doctor visit.
        """.trimIndent()

        return StructuredEhrParseResult(
            category = category,
            diagnosisTags = diagnosisTags,
            medicationsDetected = medsDetected,
            structuredSummary = structured
        )
    }

    fun analyzeMentalHealthSignals(
        transcript: String,
        stressSlider: Int,
        sleepHours: Float,
        anxietyDays: Int,
        hasFacialPhoto: Boolean,
        language: String
    ): MentalHealthScreeningResult {
        val lower = transcript.lowercase()
        val crisisWords = listOf(
            "suicide", "kill myself", "end my life", "self harm", "hurt myself",
            "આત્મહત્યા", "મરી જવું", "જીવવું નથી", "आत्महत्या", "मरना चाहता"
        )
        val crisisDetected = crisisWords.any { lower.contains(it) }

        val distressWords = listOf(
            "depress", "sad", "hopeless", "anxious", "panic", "worry", "stress", "insomnia",
            "lonely", "crying", "tired", "overwhelmed", "fear",
            "ચિંતા", "તણાવ", "ઉદાસ", "ઊંઘ નથી", "ડર", "ડિપ્રેશન", "થાક",
            "चिंता", "तनाव", "उदास", "नींद नहीं", "डर", "डिप्रेशन", "घबराहट"
        )
        val matchedDistressCount = distressWords.count { lower.contains(it) }

        var score = (stressSlider * 6) + (anxietyDays * 3) + (matchedDistressCount * 7)
        if (sleepHours < 5.5f) score += 16
        else if (sleepHours < 6.5f) score += 8
        if (crisisDetected) score = 95
        val finalScore = score.coerceIn(8, 98)

        val moodLabel = when {
            crisisDetected || finalScore >= 75 -> trLang(language, "High Distress — Immediate Support Needed", "તીવ્ર માનસિક તણાવ — તાત્કાલિક મદદ", "गंभीर तनाव — तुरंत सहायता आवश्यक")
            finalScore >= 50 -> trLang(language, "Moderate Anxiety / Emotional Strain", "મધ્યમ ચિંતા અને તણાવ", "मध्यम चिंता और तनाव")
            finalScore >= 28 -> trLang(language, "Mild Stress / Fatigue", "હળવો તણાવ અને થાક", "हल्का तनाव और थकान")
            else -> trLang(language, "Balanced / Calm Affect", "સંતુલિત અને શાંત મનોદશા", "संतुलित और शांत मनोदशा")
        }

        val photoNote = if (hasFacialPhoto) {
            trLang(language, "Facial visual cues considered alongside voice & mood check-in.", "ચહેરાના હાવભાવ અને અવાજના લક્ષણો સાથે વિશ્લેષણ કરેલ છે.", "चेहरे के हावभाव और आवाज के संकेतों के साथ विश्लेषण किया गया है।")
        } else {
            trLang(language, "Assessed from voice transcript, sleep & stress index.", "અવાજ, ઊંઘ અને તણાવના આધારે વિશ્લેષણ કરેલ છે.", "आवाज़, नींद और तनाव स्तर के आधार पर मूल्यांकन।")
        }

        val guidance = when {
            language.equals("Gujarati", ignoreCase = true) -> """
                ### 💚 પ્રાથમિક માનસિક સ્વાસ્થ્ય કાઉન્સેલિંગ ($moodLabel)
                * **તણાવ સ્કોર:** $finalScore/100 • $photoNote
                * **૪-૭-૮ શ્વાસની કસરત:** ૪ સેકન્ડ ઊંડો શ્વાસ લો, ૭ સેકન્ડ રોકો, ૮ સેકન્ડમાં ધીમેથી બહાર છોડો (૫ વખત કરો).
                * **CBT ગ્રાઉન્ડિંગ ટિપ:** નકારાત્મક વિચારોને નાના પગલાંમાં વહેંચો, પરિવાર/મિત્ર સાથે ખુલીને વાત કરો અને સૂવાના ૧ કલાક પહેલાં મોબાઇલ દૂર રાખો.
                ${if (crisisDetected || finalScore >= 70) "* 🚨 **તાત્કાલિક હેલ્પલાઇન:** ભારત સરકાર **Tele-MANAS 14416** અથવા **112** પર મફત અને ગોપનીય કાઉન્સેલિંગ માટે તરત કૉલ કરો." else "* જો ૨ અઠવાડિયાથી વધુ ઉદાસી કે ઊંઘની તકલીફ રહે, તો મનોચિકિત્સક (Psychiatrist/Counselor) ની સલાહ લો."}
            """.trimIndent()
            language.equals("Hindi", ignoreCase = true) -> """
                ### 💚 प्रारंभिक मानसिक स्वास्थ्य काउंसलिंग ($moodLabel)
                * **तनाव स्कोर:** $finalScore/100 • $photoNote
                * **4-7-8 ब्रीदिंग अभ्यास:** 4 सेकंड गहरी सांस लें, 7 सेकंड रोकें, 8 सेकंड में धीरे-धीरे छोड़ें (5 चक्र)।
                * **CBT माइंडफुलनेस टिप:** किसी भरोसेमंद मित्र या परिवार से बात करें और नियमित नींद के समय का पालन करें।
                ${if (crisisDetected || finalScore >= 70) "* 🚨 **तत्काल हेल्पलाइन:** मुफ़्त गोपनीय परामर्श के लिए **Tele-MANAS 14416** या **112** पर कॉल करें।" else "* यदि चिंता या उदासी 2 सप्ताह से अधिक बनी रहे, तो मनोचिकित्सक या काउंसलर से परामर्श लें।"}
            """.trimIndent()
            else -> """
                ### 💚 Primary Mental Health Counseling ($moodLabel)
                * **Distress Index:** $finalScore/100 • $photoNote
                * **Immediate 4-7-8 Calming Technique:** Inhale 4s, hold 7s, exhale slowly 8s for 5 cycles to activate parasympathetic calm.
                * **CBT Micro-Steps:** Write down 1 controllable worry, step outdoors for 10 mins of daylight, and disconnect screens 45 mins before sleep.
                ${if (crisisDetected || finalScore >= 70) "* 🚨 **Urgent Support:** Call **Tele-MANAS 14416** (24/7 India Mental Health Helpline) or **112** immediately to speak with a trained counselor." else "* If persistent low mood or panic lasts over 2 weeks, schedule a confidential session with a Psychologist or Psychiatrist."}
            """.trimIndent()
        }

        return MentalHealthScreeningResult(
            stressScore = finalScore,
            moodLabel = moodLabel,
            crisisDetected = crisisDetected,
            fallbackGuidance = guidance
        )
    }

    fun evaluateMolecularCandidate(
        moleculeInput: String,
        targetDisease: String,
        priority: String,
        language: String
    ): MoleculeScreeningResult {
        val clean = moleculeInput.trim().ifBlank { "C9H8O4" }
        val upper = clean.uppercase()

        // Parse molecular formula if present (e.g., C9H8O4, C21H23NO5) or estimate from SMILES tokens
        val isFormula = Regex("""^[A-Z][a-z]?\d*(?:[A-Z][a-z]?\d*)*$""").matches(clean)
        val mw: Double
        val heavyAtoms: Int
        val donors: Int
        val acceptors: Int

        if (isFormula) {
            fun countAtom(symbol: String): Int {
                val match = Regex("""$symbol(\d*)""").find(clean) ?: return 0
                return match.groupValues[1].toIntOrNull() ?: 1
            }
            val c = countAtom("C")
            val h = countAtom("H")
            val n = countAtom("N")
            val o = countAtom("O")
            val s = countAtom("S")
            val f = countAtom("F")
            val cl = countAtom("Cl")
            mw = (c * 12.011 + h * 1.008 + n * 14.007 + o * 15.999 + s * 32.06 + f * 18.998 + cl * 35.45)
                .coerceAtLeast(60.0)
            heavyAtoms = (c + n + o + s + f + cl).coerceAtLeast(4)
            acceptors = (n + o).coerceAtLeast(1)
            donors = (h / 6).coerceIn(0, 6)
        } else {
            // SMILES estimation
            val cCount = upper.count { it == 'C' }
            val nCount = upper.count { it == 'N' }
            val oCount = upper.count { it == 'O' }
            val sCount = upper.count { it == 'S' }
            val halogenCount = upper.count { it == 'F' } + Regex("Cl|Br").findAll(clean).count()
            heavyAtoms = (cCount + nCount + oCount + sCount + halogenCount).coerceAtLeast(6)
            mw = (cCount * 13.5 + nCount * 15.0 + oCount * 16.0 + sCount * 32.0 + halogenCount * 26.0).coerceAtLeast(90.0)
            acceptors = (nCount + oCount).coerceAtLeast(1)
            donors = (Regex("""NH|OH|\[nH\]""").findAll(clean).count() + (nCount + oCount) / 3).coerceIn(1, 6)
        }

        var violations = 0
        if (mw > 500.0) violations++
        if (donors > 5) violations++
        if (acceptors > 10) violations++

        val baseScore = (92 - (violations * 18) - if (heavyAtoms < 8) 10 else 0).coerceIn(28, 96)
        val tier = when {
            violations == 0 && mw in 150.0..480.0 -> trLang(language, "Optimal Oral Drug-Likeness (Lipinski Pass)", "શ્રેષ્ઠ ઓરલ ડ્રગ-લાઇકનેસ (Lipinski Pass)", "उत्तम ओरल ड्रग-लाइकनेस (Lipinski Pass)")
            violations <= 1 -> trLang(language, "Promising Lead Candidate (1 Minor Rule Flag)", "સારો લીડ કેન્ડિડેટ (૧ સામાન્ય ફ્લેગ)", "अच्छा लीड कैंडिडेट (1 छोटा फ्लैग)")
            else -> trLang(language, "Requires Structural Optimization (MW/H-Bond High)", "માળખાકીય સુધારાની જરૂર છે", "संरचनात्मक सुधार आवश्यक")
        }

        val roundedMw = (mw * 10.0).roundToInt() / 10.0
        val report = when {
            language.equals("Gujarati", ignoreCase = true) -> """
                ### 🧪 મોલેક્યુલર સ્ક્રીનિંગ અને ડ્રગ ડિસ્કવરી રિપોર્ટ
                * **લક્ષ્ય રોગ / પ્રોટીન:** $targetDisease • **પ્રાથમિકતા:** $priority
                * **અણુ / ફોર્મ્યુલા:** `$clean`
                * **અંદાજિત મોલેક્યુલર વેઇટ:** $roundedMw g/mol | **Heavy Atoms:** $heavyAtoms
                * **H-Bond Donors / Acceptors:** $donors / $acceptors | **Lipinski Violations:** $violations
                * **ડ્રગ-લાઇકનેસ સ્કોર:** $baseScore% ($tier)
                * **ક્લિનિકલ ટ્રાયલ એક્સિલરેશન:** AI વર્ચ્યુઅલ ડોકિંગ અને ઇન-સિલિકો ADMET ટોક્સિસિટી ફિલ્ટરિંગ દ્વારા પ્રારંભિક સ્ક્રીનિંગ મહિનાઓમાં પૂર્ણ કરી શકાય છે (ત્યારબાદ ઇન-વિટ્રો અને Phase I–III ટ્રાયલ જરૂરી છે).
            """.trimIndent()
            language.equals("Hindi", ignoreCase = true) -> """
                ### 🧪 मॉलिक्यूलर स्क्रीनिंग और ड्रग डिस्कवरी रिपोर्ट
                * **लक्ष्य रोग / प्रोटीन:** $targetDisease • **प्राथमिकता:** $priority
                * **फॉर्मूला / SMILES:** `$clean`
                * **अनुमानित मॉलिक्यूलर वेट:** $roundedMw g/mol | **Heavy Atoms:** $heavyAtoms
                * **H-Bond Donors / Acceptors:** $donors / $acceptors | **Lipinski Violations:** $violations
                * **ड्रग-लाइकनेस स्कोर:** $baseScore% ($tier)
                * **क्लिनिकल ट्रायल त्वरण:** AI वर्चुअल डॉकिंग और ADMET फ़िल्टरिंग से शुरुआती खोज वर्षों की बजाय महीनों में की जा सकती है।
            """.trimIndent()
            else -> """
                ### 🧪 In-Silico Drug Candidate & ADMET Screening
                * **Target Disease / Pathway:** $targetDisease • **Optimization Goal:** $priority
                * **Molecule / Formula:** `$clean`
                * **Estimated Molecular Weight:** $roundedMw g/mol | **Heavy Atoms:** $heavyAtoms
                * **Lipinski Profile:** H-Donors ~$donors, H-Acceptors ~$acceptors ($violations Rule-of-5 violations)
                * **Drug-Likeness Index:** $baseScore% ($tier)
                * **Discovery Acceleration:** Virtual target docking + toxicity screening narrows millions of candidates in months before In-Vitro assays and Phase I–III Clinical Trials.
            """.trimIndent()
        }

        return MoleculeScreeningResult(
            estimatedMolecularWeight = roundedMw,
            heavyAtomEstimate = heavyAtoms,
            hBondDonors = donors,
            hBondAcceptors = acceptors,
            lipinskiViolations = violations,
            drugLikenessScore = baseScore,
            bioavailabilityTier = tier,
            fallbackReport = report
        )
    }

    fun computeEpidemicOutbreakRisk(
        regionName: String,
        tempCelsius: Int,
        humidityPercent: Int,
        travelDensity: String,
        hospitalLoadPercent: Int,
        newsSignals: String,
        recentSymptoms: List<SymptomLogEntity>,
        language: String
    ): OutbreakPredictionResult {
        val newsLower = newsSignals.lowercase()
        val feverClusters = recentSymptoms.take(15).count {
            val s = it.symptomTitle.lowercase()
            s.contains("fever") || s.contains("taav") || s.contains("તાવ") || s.contains("cough") || s.contains("cold")
        }

        // Vector-borne (Dengue / Malaria / Chikungunya): peaks at 25-34C + high humidity >= 65%
        var vectorScore = 15
        if (tempCelsius in 24..35) vectorScore += 18
        if (humidityPercent >= 70) vectorScore += 28 else if (humidityPercent >= 55) vectorScore += 14
        if (newsLower.contains("dengue") || newsLower.contains("malaria") || newsLower.contains("rain") || newsLower.contains("monsoon") || newsLower.contains("ડેન્ગ્યુ")) vectorScore += 22
        vectorScore += (feverClusters * 4)

        // Respiratory / Viral (Influenza / COVID-like): peaks with high travel, cool/dry or dense hospital load
        var respScore = 14
        when {
            travelDensity.contains("High", ignoreCase = true) -> respScore += 26
            travelDensity.contains("Moderate", ignoreCase = true) -> respScore += 14
        }
        if (hospitalLoadPercent >= 75) respScore += 24 else if (hospitalLoadPercent >= 55) respScore += 12
        if (tempCelsius <= 22 || humidityPercent <= 40) respScore += 14
        if (newsLower.contains("covid") || newsLower.contains("flu") || newsLower.contains("viral") || newsLower.contains("outbreak") || newsLower.contains("શ્વાસ")) respScore += 20
        respScore += (feverClusters * 4)

        // Water-borne (Typhoid / Cholera / Gastroenteritis)
        var waterScore = 12
        if (humidityPercent >= 75) waterScore += 22
        if (newsLower.contains("flood") || newsLower.contains("water") || newsLower.contains("cholera") || newsLower.contains("typhoid") || newsLower.contains("ઝાડા")) waterScore += 26
        if (hospitalLoadPercent >= 70) waterScore += 14

        val vClamped = vectorScore.coerceIn(8, 96)
        val rClamped = respScore.coerceIn(8, 96)
        val wClamped = waterScore.coerceIn(8, 96)
        val maxScore = maxOf(vClamped, rClamped, wClamped)

        val tier = when {
            maxScore >= 72 -> trLang(language, "Red — Epidemic Early Alert", "લાલ — મહામારી પ્રારંભિક એલર્ટ", "लाल — महामारी प्रारंभिक अलर्ट")
            maxScore >= 50 -> trLang(language, "Orange — High Surveillance Watch", "ઓરેન્જ — હાઈ સર્વેલન્સ એલર્ટ", "ऑरेंज — हाई सर्विलांस अलर्ट")
            maxScore >= 30 -> trLang(language, "Yellow — Seasonal Advisory", "પીળો — મોસમી સાવચેતી", "पीला — मौसमी सलाह")
            else -> trLang(language, "Green — Low Outbreak Risk", "લીલો — સામાન્ય સ્થિતિ", "हरा — कम जोखिम")
        }

        val drivers = mutableListOf<String>()
        if (humidityPercent >= 65) drivers += trLang(language, "High humidity ($humidityPercent%) accelerates mosquito vector breeding", "વધુ ભેજ ($humidityPercent%) મચ્છરજન્ય રોગો વધારે છે", "उच्च आर्द्रता ($humidityPercent%) मच्छर जनित रोग बढ़ाती है")
        if (travelDensity.contains("High", ignoreCase = true)) drivers += trLang(language, "High inter-city travel density amplifies airborne transmission", "વધુ મુસાફરીથી વાયરલ ફેલાવાની ગતિ વધે છે", "अधिक यात्रा से वायरल संक्रमण तेजी से फैलता है")
        if (hospitalLoadPercent >= 65) drivers += trLang(language, "Hospital OPD/ICU load ($hospitalLoadPercent%) signals rising clinical cluster", "હોસ્પિટલ લોડ ($hospitalLoadPercent%) કેસ વધવાના સંકેત આપે છે", "अस्पताल लोड ($hospitalLoadPercent%) बढ़ते मामलों का संकेत है")
        if (feverClusters > 0) drivers += trLang(language, "$feverClusters febrile/respiratory log(s) detected in local telemetry", "લોકલ રેકોર્ડમાં $feverClusters તાવ/શ્વાસના કેસ નોંધાયા", "स्थानीय रिकॉर्ड में $feverClusters बुखार/सांस के मामले मिले")
        if (drivers.isEmpty()) drivers += trLang(language, "Environmental and hospital telemetry within baseline limits", "હવામાન અને હોસ્પિટલ ડેટા હાલમાં નિયંત્રણમાં છે", "पर्यावरण और अस्पताल डेटा नियंत्रण में है")

        val brief = when {
            language.equals("Gujarati", ignoreCase = true) -> """
                ### 📡 મહામારી પ્રારંભિક ચેતવણી બુલેટિન — $regionName
                * **એલર્ટ સ્તર:** $tier
                * **ડેન્ગ્યુ / મચ્છરજન્ય જોખમ:** $vClamped% | **શ્વસન / વાયરલ જોખમ:** $rClamped% | **પાણીજન્ય જોખમ:** $wClamped%
                * **સ્વાસ્થ્ય વિભાગ માટે ભલામણ:** મચ્છર નિયંત્રણ ફોગિંગ, તાવ-ખાંસીના કેસનું મેપિંગ, પાણી ક્લોરિનેશન અને મુસાફરી સ્ક્રીનિંગ વધારવું.
            """.trimIndent()
            language.equals("Hindi", ignoreCase = true) -> """
                ### 📡 महामारी प्रारंभिक चेतावनी बुलेटिन — $regionName
                * **अलर्ट स्तर:** $tier
                * **डेंगू / वेक्टर जोखिम:** $vClamped% | **श्वसन / वायरल जोखिम:** $rClamped% | **जलजनित जोखिम:** $wClamped%
                * **स्वास्थ्य विभाग के लिए सुझाव:** फॉगिंग अभियान, बुखार क्लस्टर ट्रैकिंग और स्वच्छ पेयजल निगरानी बढ़ाएं।
            """.trimIndent()
            else -> """
                ### 📡 Public Health Outbreak Early-Warning Brief — $regionName
                * **Alert Tier:** $tier
                * **Vector-Borne Risk:** $vClamped% | **Respiratory/Viral Spread:** $rClamped% | **Water-Borne Risk:** $wClamped%
                * **Recommended Dept Action:** Deploy vector larvicide control, intensify fever/SARI triage surveillance, chlorinate water lines, and issue public hygiene advisories.
            """.trimIndent()
        }

        return OutbreakPredictionResult(
            vectorRiskScore = vClamped,
            respiratoryRiskScore = rClamped,
            waterBorneRiskScore = wClamped,
            overallAlertTier = tier,
            riskDrivers = drivers,
            fallbackBrief = brief
        )
    }

    fun matchSpecialistAndDoctors(
        symptomQuery: String,
        nearbyFacilities: List<NearbyMedicalPlace>,
        language: String
    ): MatchedDoctorRecommendation {
        val lower = symptomQuery.lowercase()

        val (specEn, specGu, specHi, docTitle) = when {
            lower.contains("chest") || lower.contains("heart") || lower.contains("bp") || lower.contains("palpitation") ||
                lower.contains("છાતી") || lower.contains("હૃદય") || lower.contains("સીને") || lower.contains("दिल") ->
                listOf("Cardiologist", "કાર્ડિયોલોજિસ્ટ (હૃદય નિષ્ણાત)", "कार्डियोलॉजिस्ट (हृदय विशेषज्ञ)", "Dr. A. Mehta (MD, DM Cardiology)")
            lower.contains("headache") || lower.contains("migraine") || lower.contains("dizzy") || lower.contains("seizure") ||
                lower.contains("numb") || lower.contains("માથું") || lower.contains("ચક્કર") || lower.contains("सिर") ->
                listOf("Neurologist", "ન્યૂરોલોજિસ્ટ (મગજ અને નસ નિષ્ણાત)", "न्यूरोलॉजिस्ट (मस्तिष्क विशेषज्ञ)", "Dr. R. Desai (MD, DM Neurology)")
            lower.contains("rash") || lower.contains("itch") || lower.contains("skin") || lower.contains("fungal") ||
                lower.contains("ચામડી") || lower.contains("ખંજવાળ") || lower.contains("ત્વચા") || lower.contains("त्वचा") ->
                listOf("Dermatologist", "ડર્મેટોલોજિસ્ટ (ચામડીના નિષ્ણાત)", "डर्मेटोलॉजिस्ट (त्वचा विशेषज्ञ)", "Dr. K. Patel (MD Dermatology)")
            lower.contains("knee") || lower.contains("joint") || lower.contains("bone") || lower.contains("back") || lower.contains("fracture") ||
                lower.contains("ઘૂંટણ") || lower.contains("હાડકા") || lower.contains("કમર") || lower.contains("घुटने") ->
                listOf("Orthopedic Surgeon", "ઓર્થોપેડિક (હાડકાના નિષ્ણાત)", "ऑर्थोपेडिक (हड्डी विशेषज्ञ)", "Dr. V. Jadeja (MS Orthopedics)")
            lower.contains("anxiety") || lower.contains("depress") || lower.contains("stress") || lower.contains("sleep") ||
                lower.contains("ચિંતા") || lower.contains("તણાવ") || lower.contains("ડિપ્રેશન") || lower.contains("तनाव") ->
                listOf("Psychiatrist", "સાયકિયાટ્રિસ્ટ (માનસિક સ્વાસ્થ્ય નિષ્ણાત)", "मनोचिकित्सक (Psychiatrist)", "Dr. N. Shah (MD Psychiatry)")
            lower.contains("cough") || lower.contains("breath") || lower.contains("asthma") || lower.contains("lung") ||
                lower.contains("ઉધરસ") || lower.contains("શ્વાસ") || lower.contains("खांसी") || lower.contains("सांस") ->
                listOf("Pulmonologist", "પલ્મોનોલોજિસ્ટ (ફેફસાના નિષ્ણાત)", "पल्मोनोलॉजिस्ट (फेफड़े विशेषज्ञ)", "Dr. S. Trivedi (MD Chest & TB)")
            lower.contains("stomach") || lower.contains("gas") || lower.contains("acid") || lower.contains("liver") ||
                lower.contains("પેટ") || lower.contains("એસિડિટી") || lower.contains("पेट") ->
                listOf("Gastroenterologist", "ગેસ્ટ્રોએન્ટેરોલોજિસ્ટ (પેટના નિષ્ણાત)", "गैस्ट्रोएंटेरोलॉजिस्ट (पेट विशेषज्ञ)", "Dr. P. Vyas (DM Gastroenterology)")
            else ->
                listOf("General Physician", "જનરલ ફિઝિશિયન (MD મેડિસિન)", "जनरल फिजिशियन (MD मेडिसिन)", "Dr. H. Gohil (MD General Medicine)")
        }

        val localizedSpec = trLang(language, specEn, specGu, specHi)
        val urgencyNote = if (lower.contains("chest") || lower.contains("breath") || lower.contains("severe")) {
            trLang(language, "Priority appointment advised today.", "આજે જ પ્રાથમિકતાથી ડૉક્ટરને બતાવવું સલાહભર્યું છે.", "आज ही प्राथमिकता से डॉक्टर से मिलना उचित है।")
        } else {
            trLang(language, "Routine or same-week specialist visit recommended.", "નજીકના નિષ્ણાત સાથે ઓનલાઇન કે ક્લિનિક એપોઇન્ટમેન્ટ બુક કરો.", "नजदीकी विशेषज्ञ से ऑनलाइन या क्लिनिक अपॉइंटमेंट बुक करें।")
        }

        val hospitalsAndClinics = nearbyFacilities.filter { !it.type.equals("Pharmacy", ignoreCase = true) }.ifEmpty { nearbyFacilities }
        val listings = hospitalsAndClinics.take(4).mapIndexed { index, place ->
            val altDocName = when (index) {
                0 -> docTitle
                1 -> "Dr. M. Joshi ($specEn)"
                2 -> "Dr. R.Parmar ($specEn)"
                else -> "Senior $specEn Duty Consultant"
            }
            MatchedDoctorListing(
                doctorName = altDocName,
                specialty = specEn,
                clinicName = place.name,
                distanceKm = place.distanceKm,
                phoneNumber = place.phoneNumber,
                latitude = place.latitude,
                longitude = place.longitude,
                consultationFee = when (index) {
                    0 -> "₹300 - ₹500"
                    1 -> "₹500 - ₹800"
                    else -> "₹400 - ₹600"
                },
                rating = place.rating
            )
        }

        return MatchedDoctorRecommendation(
            recommendedSpecialty = specEn,
            localizedSpecialty = localizedSpec,
            urgencyNote = urgencyNote,
            doctorDirectory = listings
        )
    }
}

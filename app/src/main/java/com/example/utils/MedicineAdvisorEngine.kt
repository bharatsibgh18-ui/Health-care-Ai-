package com.example.utils

import com.example.data.local.UserProfileEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class SeverityGuidanceTier {
    MILD_NO_MED_NEEDED,
    MODERATE_SUPPORTIVE_CARE,
    SEVERE_DOCTOR_URGENT
}

data class MedicineRecommendationItem(
    val name: String,
    val category: String,
    val rxStatus: String, // "OTC Reference" or "Rx • Doctor Prescription Required"
    val usageReason: String,
    val dosageNote: String,
    val safetyWarning: String,
    val wikiQuery: String,
    val imageUrl: String? = null,
    val onlinePhotoSearchUrl: String = ""
)

data class MedicineAdvisorResult(
    val conditionQuery: String,
    val severity: Int,
    val tier: SeverityGuidanceTier,
    val doctorHeadline: String,
    val clinicalSummary: String,
    val homeCareSteps: List<String>,
    val redFlagWarnings: List<String>,
    val medicines: List<MedicineRecommendationItem>,
    val allergyAlert: String? = null,
    val aiPowered: Boolean = false,
    val networkWarningMessage: String? = null
)

object MedicineAdvisorEngine {

    private val imageUrlCache = ConcurrentHashMap<String, String>()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Queries Wikimedia/Wikipedia REST summary API for real online medicine packaging/tablet/compound photos.
     * No API key needed, HTTPS-only, cached in memory.
     */
    fun fetchOnlineMedicineImageUrl(wikiQuery: String): String? {
        val clean = wikiQuery.trim().replace(" ", "_")
        if (clean.isBlank()) return null
        imageUrlCache[clean]?.let { return it }

        return try {
            val encoded = URLEncoder.encode(clean, "UTF-8").replace("+", "_")
            val req = Request.Builder()
                .url("https://en.wikipedia.org/api/rest_v1/page/summary/$encoded")
                .header("User-Agent", "HealthCareAI-Android/${com.example.BuildConfig.VERSION_NAME}")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return null
                val json = JSONObject(body)
                val thumbUrl = json.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.startsWith("https://") }
                    ?: json.optJSONObject("originalimage")?.optString("source")?.takeIf { it.startsWith("https://") }
                if (!thumbUrl.isNullOrBlank()) {
                    imageUrlCache[clean] = thumbUrl
                }
                thumbUrl
            }
        } catch (_: Exception) {
            null
        }
    }

    fun buildOnlineSearchUrl(medicineName: String): String {
        val q = URLEncoder.encode("$medicineName medicine tablet photo", "UTF-8")
        return "https://commons.wikimedia.org/w/index.php?search=$q"
    }

    fun isRedFlagSymptom(text: String): Boolean {
        val lower = text.lowercase()
        val keywords = listOf(
            "chest pain", "breath", "choking", "unconscious", "seizure", "paralysis",
            "stroke", "heavy bleeding", "blood in stool", "vomiting blood", "high fever",
            "104", "105", "heart attack", "severe allergic", "anaphylaxis",
            "છાતીમાં દુખાવો", "શ્વાસ", "બેભાન", "सीने में दर्द", "सांस"
        )
        return keywords.any { lower.contains(it) }
    }

    fun evaluateLocally(
        symptomOrIllness: String,
        severity: Int,
        duration: String,
        language: String,
        profile: UserProfileEntity?,
        resolveImagesOnline: Boolean = true
    ): MedicineAdvisorResult {
        val clampedSeverity = severity.coerceIn(1, 10)
        val lower = symptomOrIllness.lowercase()
        val redFlag = isRedFlagSymptom(lower)

        val tier = when {
            redFlag || clampedSeverity >= 8 -> SeverityGuidanceTier.SEVERE_DOCTOR_URGENT
            clampedSeverity <= 3 -> SeverityGuidanceTier.MILD_NO_MED_NEEDED
            else -> SeverityGuidanceTier.MODERATE_SUPPORTIVE_CARE
        }

        val allergyAlert = checkAllergyRisk(profile?.allergies.orEmpty(), lower)
        val matchedCatalog = matchMedicinesFromCatalog(lower, language)

        val enrichedMedicines = if (tier == SeverityGuidanceTier.MILD_NO_MED_NEEDED) {
            // Even in mild cases, keep medicine reference minimal / optional and emphasize non-drug recovery
            emptyList()
        } else {
            matchedCatalog.map { item ->
                val fetchedImage = if (resolveImagesOnline) {
                    fetchOnlineMedicineImageUrl(item.wikiQuery)
                } else {
                    null
                }
                item.copy(
                    imageUrl = fetchedImage ?: item.imageUrl,
                    onlinePhotoSearchUrl = buildOnlineSearchUrl(item.name)
                )
            }
        }

        val (headline, summary, homeCare, warnings) = buildLocalizedGuidance(
            query = symptomOrIllness,
            severity = clampedSeverity,
            duration = duration,
            tier = tier,
            language = language,
            profile = profile
        )

        return MedicineAdvisorResult(
            conditionQuery = symptomOrIllness,
            severity = clampedSeverity,
            tier = tier,
            doctorHeadline = headline,
            clinicalSummary = summary,
            homeCareSteps = homeCare,
            redFlagWarnings = warnings,
            medicines = enrichedMedicines,
            allergyAlert = allergyAlert,
            aiPowered = false
        )
    }

    private fun checkAllergyRisk(allergies: String, symptomQuery: String): String? {
        val cleanAllergies = allergies.trim()
        if (cleanAllergies.isBlank() || cleanAllergies.equals("None Known", ignoreCase = true) || cleanAllergies.equals("None", ignoreCase = true)) {
            return null
        }
        return "Recorded Patient Allergies: $cleanAllergies — verify every ingredient before taking any medicine (even mild symptoms)."
    }

    private fun matchMedicinesFromCatalog(query: String, language: String): List<MedicineRecommendationItem> {
        val catalog = mutableListOf<MedicineRecommendationItem>()

        fun matchesKeyword(text: String, keyword: String): Boolean {
            val isAscii = keyword.all { it.code < 128 }
            return if (isAscii) {
                Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
            } else {
                text.contains(keyword)
            }
        }

        fun addIfMatches(keywords: List<String>, item: MedicineRecommendationItem) {
            if (keywords.any { matchesKeyword(query, it) }) {
                if (catalog.none { it.name == item.name }) {
                    catalog.add(item)
                }
            }
        }

        // Fever / Body Ache / Pain / Headache / Migraine / Toothache
        addIfMatches(
            listOf("fever", "tap", "bukhar", "headache", "migraine", "pain", "body ache", "tooth", "sore", "દુખાવો", "તાવ", "માથા", "बुखार", "सिरदर्द", "दर्द"),
            MedicineRecommendationItem(
                name = "Paracetamol (Acetaminophen)",
                category = "Antipyretic & Mild Analgesic",
                rxStatus = "OTC Reference",
                usageReason = trText(language, "Helps reduce mild-to-moderate fever, headache, and body aches.", "તાવ, માથાનો દુખાવો અને શરીરના દુખાવામાં રાહત આપે છે.", "बुखार, सिरदर्द और शरीर दर्द कम करने में मददगार।"),
                dosageNote = trText(language, "Discuss adult/pediatric mg dose with a doctor or pharmacist; do not exceed daily limit.", "ઉંમર અને વજન પ્રમાણે યોગ્ય ડોઝ ડૉક્ટર/ફાર્માસિસ્ટ પાસે જાણીને જ લો.", "उम्र और वजन के अनुसार डॉक्टर/फार्मासिस्ट से सही खुराक पूछें।"),
                safetyWarning = trText(language, "Avoid extra doses if liver disease exists; never combine with other paracetamol brands.", "લિવરની તકલીફ હોય તો ડૉક્ટર વગર ન લો.", "लिवर की बीमारी में डॉक्टर की सलाह के बिना न लें।"),
                wikiQuery = "Paracetamol"
            )
        )

        addIfMatches(
            listOf("inflammation", "joint", "swelling", "sprain", "tooth", "muscle", "back pain", "knee", "migraine", "સાંધા", "સોજો", "જકડન", "सूजन", "जोड़ों"),
            MedicineRecommendationItem(
                name = "Ibuprofen",
                category = "NSAID Anti-inflammatory",
                rxStatus = "OTC / Pharmacist Guidance",
                usageReason = trText(language, "Reduces inflammatory joint pain, toothache, and muscle sprains.", "સાંધાનો સોજો, દાંતનો દુખાવો અને સ્નાયુઓની મચકોડમાં ઉપયોગી.", "जोड़ों की सूजन, दांत दर्द और मांसपेशियों के खिंचाव में उपयोगी।"),
                dosageNote = trText(language, "Always take after food with a full glass of water under clinician guidance.", "હંમેશા જમ્યા પછી અને ડૉક્ટરની સલાહથી જ લો.", "हमेशा भोजन के बाद और डॉक्टर की सलाह से लें।"),
                safetyWarning = trText(language, "Avoid in stomach ulcers, kidney disease, dehydration, or pregnancy.", "પેટમાં અલ્સર, કિડનીની તકલીફ કે ગર્ભાવસ્થામાં ટાળો.", "पेट के अल्सर, किडनी समस्या या गर्भावस्था में बचें।"),
                wikiQuery = "Ibuprofen"
            )
        )

        // Allergy / Cold / Sneezing / Runny Nose / Hives / Skin Itch
        addIfMatches(
            listOf("cold", "sneeze", "runny nose", "allergy", "itch", "hives", "rash", "rhinitis", "sinus", "શરદી", "છીંક", "ખંજવાળ", "એલર્જી", "जुकाम", "छींक", "एलर्जी", "खुजली"),
            MedicineRecommendationItem(
                name = "Cetirizine Hydrochloride",
                category = "Second-Generation Antihistamine",
                rxStatus = "OTC Reference",
                usageReason = trText(language, "Relieves sneezing, watery eyes, runny nose, hives, and allergic itching.", "છીંક, નાક વહેવું, આંખમાં પાણી અને એલર્જીની ખંજવાળ ઘટાડે છે.", "छींक, बहती नाक, आंखों से पानी और एलर्जी की खुजली से राहत।"),
                dosageNote = trText(language, "Typically once daily in evening; verify with doctor if taking other sedatives.", "સામાન્ય રીતે રાત્રે એક વાર — ડૉક્ટર કે ફાર્માસિસ્ટને પૂછીને લો.", "आमतौर पर शाम को एक बार — डॉक्टर से पुष्टि करें।"),
                safetyWarning = trText(language, "May cause mild drowsiness; avoid driving until you know how it affects you.", "થોડી ઊંઘ કે સુસ્તી આવી શકે છે.", "हल्की नींद या सुस्ती आ सकती है।"),
                wikiQuery = "Cetirizine"
            )
        )

        // Acidity / Gas / Heartburn / Gastritis
        addIfMatches(
            listOf("acidity", "gas", "heartburn", "reflux", "gerd", "stomach burn", "indigestion", "ulcer", "એસિડિટી", "ગેસ", "બળતરા", "एसिडिटी", "गैस", "जलन"),
            MedicineRecommendationItem(
                name = "Pantoprazole / Omeprazole",
                category = "Proton Pump Inhibitor (Antacid)",
                rxStatus = "OTC / Doctor Review for Long Use",
                usageReason = trText(language, "Calms excess stomach acid, heartburn, and acid reflux irritation.", "પેટની વધારાની એસિડિટી, છાતીમાં બળતરા અને ગેસમાં રાહત આપે છે.", "पेट के अतिरिक्त एसिड, सीने की जलन और रिफ्लक्स में राहत।"),
                dosageNote = trText(language, "Usually taken 30 minutes before breakfast if advised by a physician.", "ડૉક્ટરની સલાહ મુજબ સવારે ખાલી પેટે નાસ્તાના 30 મિનિટ પહેલા.", "डॉक्टर की सलाह से सुबह नाश्ते से 30 मिनट पहले।"),
                safetyWarning = trText(language, "Persistent stomach pain or black stools require prompt doctor evaluation.", "સતત પેટમાં દુખાવો રહે તો તરત ડૉક્ટરને બતાવો.", "लगातार पेट दर्द हो तो तुरंत डॉक्टर को दिखाएं।"),
                wikiQuery = "Pantoprazole"
            )
        )

        // Diarrhea / Loose Motions / Dehydration / Heat Exhaustion / Vomiting
        addIfMatches(
            listOf("diarrhea", "loose motion", "dehydration", "vomit", "vomiting", "cholera", "heat", "dizziness", "ઝાડા", "ઉલટી", "ચક્કર", "दस्त", "उल्टी", "चक्कर"),
            MedicineRecommendationItem(
                name = "Oral Rehydration Salts (ORS)",
                category = "WHO Electrolyte Rehydration Solution",
                rxStatus = "First-Line OTC Support",
                usageReason = trText(language, "Restores essential fluids, sodium, and potassium lost from diarrhea, vomiting, or heat.", "ઝાડા, ઉલટી કે ગરમીને કારણે ઘટેલું પાણી અને ક્ષાર તરત પૂરા પાડે છે.", "दस्त, उल्टी या गर्मी से हुई पानी और इलेक्ट्रोलाइट की कमी पूरी करता है।"),
                dosageNote = trText(language, "Mix packet cleanly in boiled/safe drinking water exactly as printed on sachet.", "પેકેટ પર લખેલા પ્રમાણે ચોખ્ખા પાણીમાં ઓગાળીને થોડું-થોડું પીઓ.", "पैकेट पर दिए निर्देशानुसार साफ पानी में घोलकर पिएं।"),
                safetyWarning = trText(language, "Seek urgent clinic care if unable to keep fluids down or very little urine passes.", "જો પાણી ન ટકે કે પેશાબ ખૂબ ઓછો આવે તો તરત હૉસ્પિટલ જાઓ.", "यदि तरल पदार्थ न पचें या पेशाब कम हो तो तुरंत डॉक्टर के पास जाएं।"),
                wikiQuery = "Oral rehydration therapy"
            )
        )

        // Cough (Dry / Wet / Phlegm) & Throat
        addIfMatches(
            listOf("cough", "khansi", "phlegm", "throat", "mucus", "bronchitis", "ઉધરસ", "કફ", "ગળામાં", "खांसी", "कफ", "गले"),
            MedicineRecommendationItem(
                name = "Dextromethorphan / Ambroxol Syrup",
                category = "Cough Suppressant / Mucolytic Reference",
                rxStatus = "Pharmacist / Doctor Guidance",
                usageReason = trText(language, "Dextromethorphan calms dry cough; Ambroxol helps thin thick chest phlegm.", "સૂકી ઉધરસમાં રાહત અથવા જાડા કફને પાતળો કરવામાં મદદ કરે છે.", "सूखी खांसी को शांत करने या गाढ़े कफ को पतला करने में सहायक।"),
                dosageNote = trText(language, "Ask doctor whether cough is dry or productive before selecting syrup formula.", "સૂકી કે કફવાળી ઉધરસ મુજબ ડૉક્ટર પાસે યોગ્ય સિરપ નક્કી કરાવો.", "खांसी सूखी है या बलगम वाली, इसके अनुसार डॉक्टर से सिरप चुनें।"),
                safetyWarning = trText(language, "Do not give adult cough syrups to young children without a pediatrician.", "નાના બાળકોને બાળરોગ નિષ્ણાત (Pediatrician) વગર કફ સિરપ ન આપો.", "छोटे बच्चों को बाल रोग विशेषज्ञ के बिना खांसी की दवा न दें।"),
                wikiQuery = "Dextromethorphan"
            )
        )

        // Fungal / Ringworm / Itchy Skin Patch / Candidiasis
        addIfMatches(
            listOf("fungal", "ringworm", "tinea", "dadar", "skin patch", "athlete foot", "દાદર", "ફંગલ", "દાद", "फंगल"),
            MedicineRecommendationItem(
                name = "Clotrimazole 1% Topical Cream",
                category = "Topical Antifungal Cream",
                rxStatus = "OTC / Dermatologist Reference",
                usageReason = trText(language, "Targets superficial fungal skin infections and ringworm patches.", "ચામડી પરના દાદર અને ફંગલ ઇન્ફેક્શનમાં ઉપયોગી ક્રીમ.", "त्वचा के फंगल संक्रमण और दाद के इलाज में उपयोगी क्रीम।"),
                dosageNote = trText(language, "Apply thin layer on clean dry area as advised by skin doctor; complete course.", "સાફ અને સૂકી ચામડી પર ડૉક્ટરની સલાહ મુજબ પાતળો થર લગાવો.", "साफ और सूखी त्वचा पर डॉक्टर की सलाह से पतली परत लगाएं।"),
                safetyWarning = trText(language, "Avoid random steroid creams on fungal rashes as they worsen infection.", "ફંગલ ઇન્ફેક્શન પર સ્ટેરોઇડ ક્રીમ બિલકુલ ન લગાવો.", "फंगल संक्रमण पर बिना सलाह स्टेरॉयड क्रीम न लगाएं।"),
                wikiQuery = "Clotrimazole"
            )
        )

        // Cuts, Scrapes, Minor Burns
        addIfMatches(
            listOf("wound", "cut", "scrape", "burn", "injury", "antiseptic", "ઘા", "દાઝ", "કાપો", "घाव", "जलन", "चोट"),
            MedicineRecommendationItem(
                name = "Povidone-Iodine Solution",
                category = "Topical Antiseptic",
                rxStatus = "First-Aid OTC",
                usageReason = trText(language, "Disinfects minor cuts and surface scrapes to prevent wound infection.", "નાના ઘા અને ઉઝરડા સાફ કરી ઇન્ફેક્શન અટકાવે છે.", "छोटे घाव और खरोंच को साफ कर संक्रमण से बचाता है।"),
                dosageNote = trText(language, "Clean wound gently with clean water first before antiseptic care.", "પહેલા ચોખ્ખા પાણીથી ઘા સાફ કરી પછી ડૉક્ટર/ફર્સ્ટ-એઇડ મુજબ લગાવો.", "पहले साफ पानी से घाव धोएं, फिर प्राथमिक उपचार करें।"),
                safetyWarning = trText(language, "Deep, gaping, rusty-object, or bleeding wounds require immediate doctor stitching/tetanus check.", "ઊંડો ઘા કે લોહી ન બંધ થાય તો તરત ડૉક્ટર પાસે ટાંકા/ટીટેનસ ચેક કરાવો.", "गहरे घाव या लगातार खून बहने पर तुरंत डॉक्टर से जांच कराएं।"),
                wikiQuery = "Povidone-iodine"
            )
        )

        // Diabetes / BP / Infection / UTI / Chronic Rx References (Strictly Doctor Prescribed)
        addIfMatches(
            listOf("infection", "antibiotic", "uti", "urine", "bacteria", "tonsils", "પેશાબ", "ચેપ", "ઇન્ફેક્શન", "यूरिन", "संक्रमण"),
            MedicineRecommendationItem(
                name = "Amoxicillin / Ciprofloxacin (Prescription Reference)",
                category = "Prescription Antibiotic Class",
                rxStatus = "Rx • Doctor Prescription Required",
                usageReason = trText(language, "Doctors evaluate urine/throat cultures before choosing an antibiotic for bacterial infections.", "બેક્ટેરિયલ ચેપ (જેમ કે UTI કે કાકડા) માટે ડૉક્ટર તપાસ પછી યોગ્ય એન્ટિબાયોટિક લખી આપે છે.", "बैक्टीरियल संक्रमण (जैसे UTI) के लिए डॉक्टर जांच के बाद उचित एंटीबायोटिक लिखते हैं।"),
                dosageNote = trText(language, "NEVER self-start antibiotics — doctor must confirm bacterial cause and dose.", "ડૉક્ટરના પ્રિસ્ક્રિપ્શન વગર ક્યારેય એન્ટિબાયોટિક દવા ન લો.", "बिना डॉक्टर की पर्ची के कभी भी एंटीबायोटिक न लें।"),
                safetyWarning = trText(language, "Improper antibiotic use causes severe drug resistance and allergic shock.", "ખોટી એન્ટિબાયોટિકથી ગંભીર એલર્જી અને દવાની અસર ઘટી શકે છે.", "गलत एंटीबायोटिक से गंभीर एलर्जी और दवा प्रतिरोध हो सकता है।"),
                wikiQuery = "Amoxicillin"
            )
        )

        addIfMatches(
            listOf("diabetes", "sugar", "blood pressure", "bp", "hypertension", "asthma", "ડાયાબિટીસ", "બીપી", "અસ્થમા", "डायबिटीज", "शुगर", "अस्थमा"),
            MedicineRecommendationItem(
                name = "Metformin / Amlodipine (Chronic Care Reference)",
                category = "Chronic Condition Therapy",
                rxStatus = "Rx • Doctor Supervision Essential",
                usageReason = trText(language, "Chronic blood sugar, blood pressure, or asthma control relies on physician-titrated maintenance therapy.", "બ્લડ સુગર, BP અને અસ્થમા માટે ડૉક્ટર દ્વારા નક્કી કરેલી નિયમિત દવા જ ચાલુ રાખવી જરૂરી છે.", "शुगर, बीपी और अस्थमा नियंत्रण के लिए डॉक्टर द्वारा तय नियमित दवा ही लें।"),
                dosageNote = trText(language, "Never change or skip prescribed chronic doses without consulting your physician.", "તમારા નિયમિત ડૉક્ટરને પૂછ્યા વગર ડોઝમાં ફેરફાર ન કરો.", "डॉक्टर से पूछे बिना नियमित दवा की खुराक न बदलें।"),
                safetyWarning = trText(language, "Check BP / blood glucose readings regularly and carry emergency rescue meds if prescribed.", "નિયમિત BP / સુગર માપતા રહો અને તાત્કાલિક લક્ષણોમાં ડૉક્ટરનો સંપર્ક કરો.", "नियमित बीपी/शुगर जांचें और आपातकाल में तुरंत डॉक्टर से मिलें।"),
                wikiQuery = "Metformin"
            )
        )

        // General Vitamin / Weakness / Recovery Support if nothing specific matched
        if (catalog.isEmpty()) {
            catalog.add(
                MedicineRecommendationItem(
                    name = "Multivitamin & Oral Hydration Care",
                    category = "Supportive Recovery Nutrition",
                    rxStatus = "General Supportive Reference",
                    usageReason = trText(language, "Supports hydration and recovery while a doctor evaluates non-specific illness symptoms.", "સામાન્ય નબળાઈ કે અસ્પષ્ટ લક્ષણોમાં ડૉક્ટરની તપાસ દરમિયાન શરીરની તાકાત જાળવવામાં મદદ.", "सामान्य कमजोरी या लक्षणों की डॉक्टर द्वारा जांच के दौरान सहायक देखभाल।"),
                    dosageNote = trText(language, "Targeted medicine depends on exact clinical diagnosis — consult a doctor first.", "ચોક્કસ નિદાન અને દવા માટે નજીકના ડૉક્ટરની સલાહ લેવી શ્રેષ્ઠ છે.", "सटीक जांच और दवा के लिए नजदीकी डॉक्टर से सलाह लें।"),
                    safetyWarning = trText(language, "Do not take unknown tablets suggested by friends without proper examination.", "તપાસ વગર કોઈ અજાણી ગોળી ન લો.", "बिना जांच के कोई भी अनजान दवा न लें।"),
                    wikiQuery = "Multivitamin"
                )
            )
            catalog.add(
                MedicineRecommendationItem(
                    name = "Paracetamol (Acetaminophen)",
                    category = "Fever / Discomfort Reference",
                    rxStatus = "OTC Reference",
                    usageReason = trText(language, "General reference option if mild fever or body ache accompanies your symptoms.", "જો સાથે સામાન્ય તાવ કે દુખાવો હોય તો ડૉક્ટરની સલાહથી વિચારી શકાય.", "यदि लक्षणों के साथ हल्का बुखार या दर्द हो तो डॉक्टर की सलाह से विचार करें।"),
                    dosageNote = trText(language, "Only use when clinically needed and approved by physician/pharmacist.", "જરૂર હોય ત્યારે જ ડૉક્ટર/ફાર્માસિસ્ટને પૂછીને લો.", "केवल आवश्यकता होने पर डॉक्टर/फार्मासिस्ट की सलाह से लें।"),
                    safetyWarning = trText(language, "Check total daily limit and existing allergies.", "દૈનિક મર્યાદા અને એલર્જીની ખાતરી કરો.", "दैनिक सीमा और एलर्जी की जांच अवश्य करें।"),
                    wikiQuery = "Paracetamol"
                )
            )
        }

        return catalog
    }

    private fun buildLocalizedGuidance(
        query: String,
        severity: Int,
        duration: String,
        tier: SeverityGuidanceTier,
        language: String,
        profile: UserProfileEntity?
    ): GuidanceBundle {
        val patientAge = profile?.age?.let { "$it yrs" } ?: "adult"
        return when (tier) {
            SeverityGuidanceTier.MILD_NO_MED_NEEDED -> GuidanceBundle(
                headline = trText(
                    language,
                    "Mild Severity ($severity/10) — No Medicine Needed Right Now; Consult a Doctor if Symptoms Continue",
                    "સામાન્ય લક્ષણો ($severity/10) — હાલ દવા લેવાની કોઈ જરૂર નથી; જરૂર પડે તો ડૉક્ટરની સલાહ લો",
                    "हल्के लक्षण ($severity/10) — अभी दवा लेने की आवश्यकता नहीं है; बढ़ने पर डॉक्टर से सलाह लें"
                ),
                summary = trText(
                    language,
                    "Based on your reported symptom severity ($severity/10 for \"$query\", duration: $duration), this appears mild and may settle naturally with rest and hydration. Avoid unnecessary self-medication and visit a certified doctor if symptoms last more than 48 hours or intensify.",
                    "તમારા જણાવેલ લક્ષણોની ગંભીરતા ($severity/10 — \"$query\", સમય: $duration) ખૂબ ઓછી હોવાથી તાત્કાલિક કોઈ દવા લેવાની જરૂર નથી. આરામ, પૂરતું પાણી અને હળવા ખોરાકથી સુધારો થઈ શકે છે. જો 24-48 કલાકમાં રાહત ન થાય તો ડૉક્ટરની સલાહ અવશ્ય લો.",
                    "आपके द्वारा बताए गए लक्षणों की गंभीरता ($severity/10 — \"$query\", अवधि: $duration) कम है, इसलिए अभी तुरंत किसी दवा की जरूरत नहीं है। आराम और पर्याप्त पानी से सुधार संभव है। यदि 48 घंटे में आराम न मिले तो डॉक्टर को दिखाएं।"
                ),
                homeCare = listOf(
                    trText(language, "Prioritize complete rest and sleep (7–8 hours).", "પૂરતો આરામ અને ઊંઘ લો.", "पूरा आराम और नींद लें।"),
                    trText(language, "Drink clean water, warm soup, or coconut water regularly.", "નિયમિત ચોખ્ખું પાણી, ગરમ સૂપ કે નાળિયેર પાણી પીઓ.", "साफ पानी, सूप या नारियल पानी नियमित पिएं।"),
                    trText(language, "Track severity again in 12–24 hours in Daily Symptom Logger.", "12-24 કલાક પછી ફરી લક્ષણની ગંભીરતા તપાસો.", "12-24 घंटे बाद फिर से लक्षण की गंभीरता दर्ज करें。")
                ),
                warnings = listOf(
                    trText(language, "Consult a licensed physician if fever spikes, pain worsens, or new symptoms develop.", "જો તાવ વધે કે દુખાવો વધી જાય તો તરત ડૉક્ટરને બતાવો.", "यदि बुखार बढ़े या दर्द तेज हो तो तुरंत डॉक्टर से मिलें।")
                )
            )
            SeverityGuidanceTier.MODERATE_SUPPORTIVE_CARE -> GuidanceBundle(
                headline = trText(
                    language,
                    "Moderate Severity ($severity/10) — Doctor Consultation Advised + Medicine References Below",
                    "મધ્યમ ગંભીરતા ($severity/10) — ડૉક્ટરની સલાહ લો + ઉપલબ્ધ દવાઓની માહિતી",
                    "मध्यम गंभीरता ($severity/10) — डॉक्टर से परामर्श लें + उपलब्ध दवाओं की जानकारी"
                ),
                summary = trText(
                    language,
                    "For \"$query\" (Severity $severity/10, Patient age: $patientAge), schedule an in-person or tele-doctor consultation to confirm the exact root cause. Below are standard reference medicines (names & online identification photos) commonly discussed with clinicians — confirm dose & allergies with your doctor or pharmacist before use.",
                    "\"$query\" (તીવ્રતા $severity/10) માટે ચોક્કસ કારણ જાણવા ડૉક્ટરની સલાહ લેવી હિતાવહ છે. નીચે આ લક્ષણોમાં સામાન્ય રીતે વપરાતી દવાઓના નામ અને ઓનલાઇન ફોટાની માહિતી આપેલ છે — કોઈ પણ દવા લેતા પહેલા ડૉક્ટર કે ફાર્માસિસ્ટ પાસે ડોઝ અને એલર્જીની ખાતરી અવશ્ય કરો.",
                    "\"$query\" (तीव्रता $severity/10) के सटीक कारण के लिए डॉक्टर की सलाह लें। नीचे सामान्य रूप से उपयोग होने वाली दवाओं के नाम और ऑनलाइन फोटो दिए गए हैं — लेने से पहले डॉक्टर या फार्मासिस्ट से खुराक और एलर्जी की जांच जरूर करें।"
                ),
                homeCare = listOf(
                    trText(language, "Stay hydrated and monitor temperature/vitals twice daily.", "શરીરમાં પાણી જાળવી રાખો અને દિવસમાં બે વાર તાવ/BP માપો.", "पर्याप्त पानी पिएं और दिन में दो बार तापमान/वाइटल्स जांचें।"),
                    trText(language, "Never mix two painkillers or take leftover prescription antibiotics.", "એકસાથે બે દુખાવાની ગોળી કે જૂની એન્ટિબાયોટિક ન લો.", "दो दर्द निवारक दवाएं साथ न लें और पुरानी एंटीबायोटिक न खाएं।")
                ),
                warnings = listOf(
                    trText(language, "If symptoms fail to improve in 48 hours, see a physician promptly.", "જો 48 કલાકમાં સુધારો ન થાય તો તરત ડૉક્ટરની મુલાકાત લો.", "यदि 48 घंटे में सुधार न हो तो तुरंत डॉक्टर से मिलें।")
                )
            )
            SeverityGuidanceTier.SEVERE_DOCTOR_URGENT -> GuidanceBundle(
                headline = trText(
                    language,
                    "High / Severe Warning ($severity/10) — Urgent Doctor Visit Required!",
                    "વધુ ગંભીર ચેતવણી ($severity/10) — તાત્કાલિક ડૉક્ટરને બતાવો!",
                    "उच्च गंभीरता चेतावनी ($severity/10) — तुरंत डॉक्टर से मिलें!"
                ),
                summary = trText(
                    language,
                    "Severity $severity/10 for \"$query\" is too high for home self-medication alone. Visit a nearby clinic or hospital immediately. Reference medicines below are shown solely for medical identification and doctor discussion — do NOT delay emergency care.",
                    "\"$query\" ની તીવ્રતા $severity/10 ઘણી વધારે છે, તેથી ઘરે માત્ર પોતાની રીતે દવા લેવી જોખમી બની શકે છે. તરત જ નજીકના ડૉક્ટર અથવા હૉસ્પિટલની મુલાકાત લો.",
                    "\"$query\" की गंभीरता $severity/10 बहुत अधिक है, इसलिए केवल घरेलू दवा पर निर्भर न रहें। तुरंत नजदीकी डॉक्टर या अस्पताल जाएं।"
                ),
                homeCare = listOf(
                    trText(language, "Contact your doctor or Emergency 108/112 right away.", "તાત્કાલિક ડૉક્ટર અથવા ઈમરજન્સી 108/112 પર કૉલ કરો.", "तुरंत डॉक्टर या आपातकालीन सेवा 108/112 पर कॉल करें।"),
                    trText(language, "Keep your allergy list and current medicine strip ready to show the doctor.", "ડૉક્ટરને બતાવવા માટે તમારી એલર્જી અને હાલની દવાની વિગત સાથે રાખો.", "डॉक्टर को दिखाने के लिए अपनी एलर्जी और वर्तमान दवाओं की सूची साथ रखें।")
                ),
                warnings = listOf(
                    trText(language, "Chest pain, breathing trouble, fainting, or high fever require emergency hospital care.", "છાતીમાં દુખાવો, શ્વાસની તકલીફ, બેભાન થવું કે ભારે તાવ માટે તરત હૉસ્પિટલ જાઓ.", "सीने में दर्द, सांस फूलने, बेहोशी या तेज बुखार में तुरंत अस्पताल जाएं।")
                )
            )
        }
    }

    private data class GuidanceBundle(
        val headline: String,
        val summary: String,
        val homeCare: List<String>,
        val warnings: List<String>
    )

    private fun trText(language: String, en: String, gu: String, hi: String): String =
        when (language) {
            "Gujarati" -> gu
            "Hindi" -> hi
            else -> en
        }
}

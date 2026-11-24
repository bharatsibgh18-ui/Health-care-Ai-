package com.example.utils

import com.example.data.local.MealNutritionLogEntity
import com.example.data.local.MedicineEntity
import com.example.data.local.SleepWellnessLogEntity
import com.example.data.local.VitalLogEntity
import com.example.ui.theme.trLang
import kotlin.math.roundToInt

enum class CoachTimeSlot {
    MORNING,
    AFTERNOON,
    EVENING
}

data class CoachTask(
    val id: String,
    val slot: CoachTimeSlot,
    val title: String,
    val subtitle: String,
    val metricBadge: String
)

data class DailyCoachPlan(
    val dateKey: String,
    val readinessHeadline: String,
    val summaryBrief: String,
    val isAiGenerated: Boolean,
    val tasks: List<CoachTask>
)

/**
 * Generates a structured, personalized daily wellness coaching plan from live
 * Health Score, Hydration, Steps, Nutrition, Sleep, and Medication data.
 */
object UltraHealthCoachEngine {

    fun buildDailyPlan(
        dateKey: String,
        healthScore: Int,
        todayWaterMl: Int,
        waterTargetMl: Int,
        todaySteps: Int,
        stepTarget: Int,
        todayCalories: Int,
        calorieTarget: Int,
        activeMedicines: List<MedicineEntity>,
        sleepLogs: List<SleepWellnessLogEntity>,
        vitalLogs: List<VitalLogEntity>,
        language: String,
        aiBriefOverride: String? = null,
        isAiGenerated: Boolean = false
    ): DailyCoachPlan {
        val waterPct = ((todayWaterMl.toFloat() / waterTargetMl.coerceAtLeast(500)) * 100f).roundToInt().coerceIn(0, 100)
        val stepPct = ((todaySteps.toFloat() / stepTarget.coerceAtLeast(1000)) * 100f).roundToInt().coerceIn(0, 100)
        val latestSleep = sleepLogs.firstOrNull()?.sleepHours

        val headline = when {
            healthScore >= 85 -> trLang(
                language,
                "Peak Vitality Day — Keep Your Momentum!",
                "ઉત્તમ સ્વાસ્થ્ય દિવસ — તમારી ગતિ જાળવી રાખો!",
                "उत्कृष्ट स्वास्थ्य दिवस — अपनी गति बनाए रखें!"
            )
            healthScore >= 65 -> trLang(
                language,
                "Steady Progress — 2 Quick Wins to Reach Ultra",
                "સારી પ્રગતિ — અલ્ટ્રા સ્કોર માટે 2 નાના પગલાં",
                "अच्छी प्रगति — अल्ट्रा स्कोर के लिए 2 आसान कदम"
            )
            else -> trLang(
                language,
                "Fresh Wellness Reset — Focus on Hydration & Steps",
                "નવી શરૂઆત — આજે પાણી અને ચાલવા પર ધ્યાન આપો",
                "नई शुरुआत — आज पानी और पैदल चलने पर ध्यान दें"
            )
        }

        val tasks = mutableListOf<CoachTask>()

        // Morning slot
        tasks += CoachTask(
            id = "morning_hydration",
            slot = CoachTimeSlot.MORNING,
            title = trLang(
                language,
                "Morning Hydration Boost (500 ml)",
                "સવારે 500 ml હૂંફાળું પાણી પીઓ",
                "सुबह 500 ml गुनगुना पानी पिएं"
            ),
            subtitle = trLang(
                language,
                "Hydration is at $waterPct% of your ${waterTargetMl}ml target.",
                "પાણીનું લક્ષ્ય અત્યારે $waterPct% (${todayWaterMl}/${waterTargetMl}ml) પૂર્ણ છે.",
                "हाइड्रेशन अभी $waterPct% (${todayWaterMl}/${waterTargetMl}ml) पूरा है।"
            ),
            metricBadge = "$waterPct%"
        )

        if (activeMedicines.isNotEmpty()) {
            val firstMed = activeMedicines.first()
            tasks += CoachTask(
                id = "morning_medicine",
                slot = CoachTimeSlot.MORNING,
                title = trLang(
                    language,
                    "Verify Morning Dose: ${firstMed.name}",
                    "સવારની દવા લો: ${firstMed.name}",
                    "सुबह की दवा लें: ${firstMed.name}"
                ),
                subtitle = trLang(
                    language,
                    "${activeMedicines.size} active medicine schedule(s) tracked today.",
                    "આજે કુલ ${activeMedicines.size} દવાઓનું શેડ્યૂલ સક્રિય છે.",
                    "आज कुल ${activeMedicines.size} दवाओं का शेड्यूल सक्रिय है।"
                ),
                metricBadge = "${activeMedicines.size} Rx"
            )
        } else {
            tasks += CoachTask(
                id = "morning_breathing",
                slot = CoachTimeSlot.MORNING,
                title = trLang(
                    language,
                    "5-Minute Deep Pranayama & Stretch",
                    "5 મિનિટ ઊંડા શ્વાસ (પ્રાણાયામ) અને સ્ટ્રેચિંગ",
                    "5 मिनट प्राणायाम और हल्की स्ट्रेचिंग"
                ),
                subtitle = trLang(
                    language,
                    "Lowers resting stress and supports calm heart rate.",
                    "તણાવ ઘટાડે છે અને હૃદયની ગતિ સંતુલિત રાખે છે.",
                    "तनाव कम करता है और हृदय गति संतुलित रखता है।"
                ),
                metricBadge = "5 min"
            )
        }

        // Afternoon slot
        val remainingSteps = (stepTarget - todaySteps).coerceAtLeast(0)
        tasks += CoachTask(
            id = "afternoon_walk",
            slot = CoachTimeSlot.AFTERNOON,
            title = if (remainingSteps > 0) {
                trLang(
                    language,
                    "Brisk 15-Min Walk (+${remainingSteps.coerceAtMost(2500)} Steps)",
                    "15 મિનિટ ઝડપી ચાલો (+${remainingSteps.coerceAtMost(2500)} સ્ટેપ્સ)",
                    "15 मिनट तेज़ चलें (+${remainingSteps.coerceAtMost(2500)} कदम)"
                )
            } else {
                trLang(
                    language,
                    "Daily Step Target Achieved! Gentle Mobility Stretch",
                    "આજના સ્ટેપ્સ પૂર્ણ! હળવું સ્ટ્રેચિંગ કરો",
                    "दैनिक कदम लक्ष्य पूरा! हल्की स्ट्रेचिंग करें"
                )
            },
            subtitle = trLang(
                language,
                "Current activity: $todaySteps / $stepTarget steps ($stepPct%).",
                "હાલની પ્રવૃત્તિ: $todaySteps / $stepTarget સ્ટેપ્સ ($stepPct%).",
                "वर्तमान गतिविधि: $todaySteps / $stepTarget कदम ($stepPct%)।"
            ),
            metricBadge = "$stepPct%"
        )

        val calRemaining = (calorieTarget - todayCalories).coerceAtLeast(0)
        tasks += CoachTask(
            id = "afternoon_nutrition",
            slot = CoachTimeSlot.AFTERNOON,
            title = trLang(
                language,
                "Balanced Protein & Fiber Meal Check",
                "પ્રોટીન અને ફાઇબરયુક્ત સંતુલિત ભોજન લો",
                "प्रोटीन और फाइबर युक्त संतुलित भोजन लें"
            ),
            subtitle = trLang(
                language,
                "Logged $todayCalories / $calorieTarget kcal (${calRemaining} kcal remaining).",
                "આજે $todayCalories / $calorieTarget kcal નોંધાયા (બાકી: $calRemaining kcal).",
                "आज $todayCalories / $calorieTarget kcal दर्ज (शेष: $calRemaining kcal)।"
            ),
            metricBadge = "$todayCalories kcal"
        )

        // Evening slot
        tasks += CoachTask(
            id = "evening_vitals",
            slot = CoachTimeSlot.EVENING,
            title = trLang(
                language,
                "Log Evening Vitals or Symptom Check-In",
                "સાંજના વાઇટલ્સ (BP/સુગર) અથવા લક્ષણો નોંધો",
                "शाम के वाइटल्स (BP/शुगर) या लक्षण दर्ज करें"
            ),
            subtitle = trLang(
                language,
                "${vitalLogs.size} vital record(s) saved for doctor-ready trends.",
                "ડૉક્ટર રિપોર્ટ માટે ${vitalLogs.size} વાઇટલ રેકોર્ડ સાચવેલા છે.",
                "डॉक्टर रिपोर्ट के लिए ${vitalLogs.size} वाइटल रिकॉर्ड सहेजे गए हैं।"
            ),
            metricBadge = "${vitalLogs.size} Logs"
        )

        tasks += CoachTask(
            id = "evening_sleep",
            slot = CoachTimeSlot.EVENING,
            title = trLang(
                language,
                "Wind Down Early for 7.5+ Hours Sleep",
                "7.5+ કલાકની ગાઢ ઊંઘ માટે સમયસર આરામ કરો",
                "7.5+ घंटे की अच्छी नींद के लिए समय पर सोएं"
            ),
            subtitle = if (latestSleep != null) {
                trLang(
                    language,
                    "Last logged sleep: ${"%.1f".format(latestSleep)} hrs. Keep screens off 30 min before bed.",
                    "છેલ્લી નોંધાયેલ ઊંઘ: ${"%.1f".format(latestSleep)} કલાક. સૂતા પહેલા 30 મિનિટ મોબાઇલ દૂર રાખો.",
                    "अंतिम दर्ज नींद: ${"%.1f".format(latestSleep)} घंटे। सोने से 30 मिनट पहले स्क्रीन बंद करें।"
                )
            } else {
                trLang(
                    language,
                    "Log tonight's sleep in BMI & Sleep tab to boost your Ultra Score.",
                    "અલ્ટ્રા સ્કોર વધારવા માટે આજની ઊંઘ BMI અને ઊંઘ ટેબમાં નોંધો.",
                    "अल्ट्रा स्कोर बढ़ाने के लिए आज की नींद BMI व नींद टैब में दर्ज करें।"
                )
            },
            metricBadge = latestSleep?.let { "${"%.1f".format(it)}h" } ?: "7.5h"
        )

        val brief = aiBriefOverride?.takeIf { it.isNotBlank() } ?: buildOfflineCoachingBrief(
            healthScore = healthScore,
            todayWaterMl = todayWaterMl,
            waterTargetMl = waterTargetMl,
            todaySteps = todaySteps,
            stepTarget = stepTarget,
            todayCalories = todayCalories,
            calorieTarget = calorieTarget,
            language = language
        )

        return DailyCoachPlan(
            dateKey = dateKey,
            readinessHeadline = headline,
            summaryBrief = brief,
            isAiGenerated = isAiGenerated && !aiBriefOverride.isNullOrBlank(),
            tasks = tasks
        )
    }

    fun buildOfflineCoachingBrief(
        healthScore: Int,
        todayWaterMl: Int,
        waterTargetMl: Int,
        todaySteps: Int,
        stepTarget: Int,
        todayCalories: Int,
        calorieTarget: Int,
        language: String
    ): String {
        val waterGap = (waterTargetMl - todayWaterMl).coerceAtLeast(0)
        val stepGap = (stepTarget - todaySteps).coerceAtLeast(0)
        return trLang(
            language,
            "• Health Score: $healthScore/100. Drink ${if (waterGap > 0) "${waterGap}ml more water" else "steady sips"} today.\n• Movement: ${if (stepGap > 0) "$stepGap steps left to hit your daily goal" else "Daily step goal achieved!"}.\n• Nutrition: $todayCalories / $calorieTarget kcal logged — prioritize dal, greens, and fresh fruit.",
            "• હેલ્થ સ્કોર: $healthScore/100. આજે ${if (waterGap > 0) "વધુ ${waterGap}ml પાણી પીવાનું" else "પાણીનું લક્ષ્ય પૂર્ણ"} રાખો.\n• ચાલવાનું લક્ષ્ય: ${if (stepGap > 0) "બાકી $stepGap સ્ટેપ્સ પૂર્ણ કરો" else "આજના સ્ટેપ્સ પૂર્ણ થઈ ગયા!"}.\n• પોષણ: $todayCalories / $calorieTarget kcal નોંધાયા — દાળ, લીલા શાકભાજી અને ફળોને પ્રાધાન્ય આપો.",
            "• हेल्थ स्कोर: $healthScore/100। आज ${if (waterGap > 0) "${waterGap}ml और पानी पिएं" else "पानी का लक्ष्य पूरा है"}।\n• गतिविधि: ${if (stepGap > 0) "लक्ष्य के लिए $stepGap कदम शेष हैं" else "दैनिक कदम लक्ष्य पूरा हुआ!"}।\n• पोषण: $todayCalories / $calorieTarget kcal दर्ज — दाल, हरी सब्ज़ियाँ और ताज़े फल शामिल करें।"
        )
    }
}

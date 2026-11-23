package com.example.utils

import com.example.data.local.MedicationAdherenceEntity
import com.example.data.local.MedicineEntity
import com.example.data.local.SleepWellnessLogEntity
import com.example.data.local.VitalLogEntity
import kotlin.math.roundToInt

enum class HealthScoreTier {
    ULTRA_OPTIMAL,
    STRONG,
    STEADY,
    NEEDS_ATTENTION
}

enum class InsightPriority {
    POSITIVE,
    SUGGESTION,
    ACTION_NEEDED
}

data class HealthPillarScore(
    val key: String,
    val labelEn: String,
    val labelGu: String,
    val labelHi: String,
    val score: Int, // 0..100
    val weightPercent: Int,
    val metricSummary: String
)

data class SmartHealthInsight(
    val id: String,
    val titleEn: String,
    val titleGu: String,
    val titleHi: String,
    val detailEn: String,
    val detailGu: String,
    val detailHi: String,
    val priority: InsightPriority
)

data class DailyHealthScoreReport(
    val overallScore: Int, // 0..100
    val tier: HealthScoreTier,
    val pillars: List<HealthPillarScore>,
    val insights: List<SmartHealthInsight>
)

/**
 * Computes a live 0-100 composite Daily Health Score across 5 clinical pillars:
 * Hydration (25%), Daily Steps (25%), Medication Adherence (20%), Sleep Recovery (15%), Vitals Stability (15%).
 */
object UltraHealthScoreEngine {

    fun calculate(
        todayWaterMl: Int,
        waterTargetMl: Int,
        todaySteps: Int,
        stepTarget: Int,
        activeMedicines: List<MedicineEntity>,
        adherenceLogs: List<MedicationAdherenceEntity>,
        sleepLogs: List<SleepWellnessLogEntity>,
        vitalLogs: List<VitalLogEntity>,
        referenceTimeMs: Long = System.currentTimeMillis()
    ): DailyHealthScoreReport {
        val safeWaterTarget = waterTargetMl.coerceAtLeast(500)
        val safeStepTarget = stepTarget.coerceAtLeast(1000)

        // 1. Hydration Pillar (25%)
        val waterRatio = (todayWaterMl.toDouble() / safeWaterTarget.toDouble()).coerceIn(0.0, 1.0)
        val hydrationScore = (waterRatio * 100.0).roundToInt()

        // 2. Activity Steps Pillar (25%)
        val stepRatio = (todaySteps.toDouble() / safeStepTarget.toDouble()).coerceIn(0.0, 1.0)
        val stepsScore = (stepRatio * 100.0).roundToInt()

        // 3. Medication Adherence Pillar (20%)
        val streakSummary = MedicationStreakEngine.analyze(
            medicines = activeMedicines,
            adherenceLogs = adherenceLogs,
            referenceTimeMs = referenceTimeMs
        )
        val medScore = if (activeMedicines.isEmpty()) {
            100
        } else if (adherenceLogs.isEmpty()) {
            65
        } else {
            streakSummary.adherenceRate7d.coerceIn(0, 100)
        }

        // 4. Sleep Recovery Pillar (15%)
        val latestSleep = sleepLogs.maxByOrNull { it.timestamp }
        val sleepScore = if (latestSleep == null) {
            70
        } else {
            val hoursScore = when {
                latestSleep.sleepHours in 7.0..9.0 -> 100
                latestSleep.sleepHours in 6.0..6.99 || latestSleep.sleepHours in 9.01..10.0 -> 82
                latestSleep.sleepHours in 5.0..5.99 -> 60
                else -> 40
            }
            val qualityScore = (latestSleep.sleepQuality.coerceIn(1, 5) * 20)
            ((hoursScore * 0.7) + (qualityScore * 0.3)).roundToInt().coerceIn(0, 100)
        }

        // 5. Vitals Stability Pillar (15%)
        val recentVitals = vitalLogs.sortedByDescending { it.timestamp }.take(6)
        val vitalsScore = if (recentVitals.isEmpty()) {
            75
        } else {
            val normalCount = recentVitals.count {
                it.status.equals("Normal", ignoreCase = true)
            }
            val elevatedCount = recentVitals.count {
                it.status.equals("Elevated", ignoreCase = true)
            }
            val weighted = (normalCount * 100.0 + elevatedCount * 65.0 +
                (recentVitals.size - normalCount - elevatedCount) * 35.0) / recentVitals.size
            weighted.roundToInt().coerceIn(25, 100)
        }

        val overall = (
            hydrationScore * 0.25 +
                stepsScore * 0.25 +
                medScore * 0.20 +
                sleepScore * 0.15 +
                vitalsScore * 0.15
            ).roundToInt().coerceIn(0, 100)

        val tier = when {
            overall >= 85 -> HealthScoreTier.ULTRA_OPTIMAL
            overall >= 68 -> HealthScoreTier.STRONG
            overall >= 48 -> HealthScoreTier.STEADY
            else -> HealthScoreTier.NEEDS_ATTENTION
        }

        val pillars = listOf(
            HealthPillarScore(
                key = "hydration",
                labelEn = "Hydration",
                labelGu = "પાણી (હાઇડ્રેશન)",
                labelHi = "हाइड्रेशन",
                score = hydrationScore,
                weightPercent = 25,
                metricSummary = "$todayWaterMl / $safeWaterTarget ml"
            ),
            HealthPillarScore(
                key = "steps",
                labelEn = "Activity",
                labelGu = "ચાલવું (સ્ટેપ્સ)",
                labelHi = "गतिविधि (कदम)",
                score = stepsScore,
                weightPercent = 25,
                metricSummary = "$todaySteps / $safeStepTarget"
            ),
            HealthPillarScore(
                key = "meds",
                labelEn = "Medicines",
                labelGu = "દવા નિયમિતતા",
                labelHi = "दवा नियमितता",
                score = medScore,
                weightPercent = 20,
                metricSummary = if (activeMedicines.isEmpty()) "No active Rx" else "$medScore% (7d)"
            ),
            HealthPillarScore(
                key = "sleep",
                labelEn = "Sleep",
                labelGu = "ઊંઘ & રિકવરી",
                labelHi = "नींद व रिकवरी",
                score = sleepScore,
                weightPercent = 15,
                metricSummary = if (latestSleep == null) "Not logged" else String.format("%.1fh (%d/5)", latestSleep.sleepHours, latestSleep.sleepQuality)
            ),
            HealthPillarScore(
                key = "vitals",
                labelEn = "Vitals",
                labelGu = "વાઇટલ્સ સ્થિરતા",
                labelHi = "वाइटल्स स्थिरता",
                score = vitalsScore,
                weightPercent = 15,
                metricSummary = if (recentVitals.isEmpty()) "Baseline" else "${recentVitals.count { it.status.equals("Normal", true) }}/${recentVitals.size} normal"
            )
        )

        val insights = buildInsights(
            todayWaterMl = todayWaterMl,
            waterTargetMl = safeWaterTarget,
            todaySteps = todaySteps,
            stepTarget = safeStepTarget,
            activeMedicines = activeMedicines,
            medScore = medScore,
            streakDays = streakSummary.currentStreakDays,
            latestSleep = latestSleep,
            recentVitals = recentVitals
        )

        return DailyHealthScoreReport(
            overallScore = overall,
            tier = tier,
            pillars = pillars,
            insights = insights
        )
    }

    private fun buildInsights(
        todayWaterMl: Int,
        waterTargetMl: Int,
        todaySteps: Int,
        stepTarget: Int,
        activeMedicines: List<MedicineEntity>,
        medScore: Int,
        streakDays: Int,
        latestSleep: SleepWellnessLogEntity?,
        recentVitals: List<VitalLogEntity>
    ): List<SmartHealthInsight> {
        val list = mutableListOf<SmartHealthInsight>()

        val waterRemaining = (waterTargetMl - todayWaterMl).coerceAtLeast(0)
        if (waterRemaining > 500) {
            list += SmartHealthInsight(
                id = "insight_water",
                titleEn = "Hydration Boost Needed",
                titleGu = "વધુ પાણી પીવાની જરૂર છે",
                titleHi = "पानी पीने का लक्ष्य बाकी है",
                detailEn = "Drink $waterRemaining ml more today to hit your $waterTargetMl ml hydration goal.",
                detailGu = "તમારો $waterTargetMl ml ટાર્ગેટ પૂરો કરવા આજે વધુ $waterRemaining ml પાણી પીવો.",
                detailHi = "अपना $waterTargetMl ml लक्ष्य पूरा करने के लिए आज $waterRemaining ml पानी और पिएं।",
                priority = InsightPriority.ACTION_NEEDED
            )
        } else {
            list += SmartHealthInsight(
                id = "insight_water_ok",
                titleEn = "Great Hydration Balance",
                titleGu = "શ્રેષ્ઠ હાઇડ્રેશન જાળવણી",
                titleHi = "उत्कृष्ट हाइड्रेशन संतुलन",
                detailEn = "You're right on track with $todayWaterMl ml logged today.",
                detailGu = "તમે આજે $todayWaterMl ml પાણી પીને ઉત્તમ લય જાળવી છે.",
                detailHi = "आपने आज $todayWaterMl ml पानी लॉग करके बढ़िया गति बनाए रखी है।",
                priority = InsightPriority.POSITIVE
            )
        }

        val stepsRemaining = (stepTarget - todaySteps).coerceAtLeast(0)
        if (stepsRemaining > 1500) {
            val minsWalk = (stepsRemaining / 100).coerceAtLeast(5)
            list += SmartHealthInsight(
                id = "insight_steps",
                titleEn = "Quick $minsWalk-Min Walk Recommended",
                titleGu = "$minsWalk મિનિટ ચાલવાની સલાહ",
                titleHi = "$minsWalk मिनट की वॉक करें",
                detailEn = "$stepsRemaining steps left to unlock full daily cardiovascular activity points.",
                detailGu = "દૈનિક હાર્ટ ફિટનેસ ટાર્ગેટ માટે $stepsRemaining પગલાં બાકી છે.",
                detailHi = "दैनिक गतिविधि लक्ष्य पूरा करने के लिए $stepsRemaining कदम बाकी हैं।",
                priority = InsightPriority.SUGGESTION
            )
        }

        if (activeMedicines.isNotEmpty() && medScore < 80) {
            list += SmartHealthInsight(
                id = "insight_meds",
                titleEn = "Protect Your Medication Routine",
                titleGu = "દવા નિયમિત લેવાનું ધ્યાન રાખો",
                titleHi = "दवा का समय न चूकें",
                detailEn = "7-day adherence is $medScore%. Mark doses promptly to build an unbroken streak.",
                detailGu = "છેલ્લા 7 દિવસની નિયમિતતા $medScore% છે. સમયસર દવા નોંધી સ્ટ્રીક વધારો.",
                detailHi = "पिछले 7 दिनों की नियमितता $medScore% है। स्ट्रीक बनाने के लिए समय पर दवा लें।",
                priority = InsightPriority.ACTION_NEEDED
            )
        } else if (streakDays >= 3) {
            list += SmartHealthInsight(
                id = "insight_streak",
                titleEn = "$streakDays-Day Medicine Streak Active!",
                titleGu = "$streakDays દિવસની દવા સ્ટ્રીક ચાલુ!",
                titleHi = "$streakDays दिनों की दवा स्ट्रीक सक्रिय!",
                detailEn = "Consistent adherence dramatically improves long-term treatment outcomes.",
                detailGu = "સતત નિયમિત દવા લેવાથી સારવારનું પરિણામ ઘણું સારું મળે છે.",
                detailHi = "लगातार नियमित दवा लेने से स्वास्थ्य लाभ बहुत बेहतर होता है।",
                priority = InsightPriority.POSITIVE
            )
        }

        if (latestSleep == null) {
            list += SmartHealthInsight(
                id = "insight_sleep_log",
                titleEn = "Log Last Night's Sleep",
                titleGu = "ગઈકાલની ઊંઘ નોંધો",
                titleHi = "कल रात की नींद लॉग करें",
                detailEn = "Record sleep hours in the Wellness tab to track weekly sleep debt and recovery.",
                detailGu = "વેલનેસ ટેબમાં ઊંઘના કલાક નોંધી સાપ્તાહિક રિકવરી ટ્રેક કરો.",
                detailHi = "साप्ताहिक स्लीप डेट ट्रैक करने के लिए वेलनेस टैब में नींद के घंटे दर्ज करें।",
                priority = InsightPriority.SUGGESTION
            )
        } else if (latestSleep.sleepHours < 6.5) {
            list += SmartHealthInsight(
                id = "insight_sleep_low",
                titleEn = "Sleep Deficit Detected (${String.format("%.1f", latestSleep.sleepHours)}h)",
                titleGu = "ઓછી ઊંઘ નોંધાઈ (${String.format("%.1f", latestSleep.sleepHours)} કલાક)",
                titleHi = "नींद की कमी दर्ज (${String.format("%.1f", latestSleep.sleepHours)} घंटे)",
                detailEn = "Aim for 7–8 hours tonight to lower cortisol and support immune recovery.",
                detailGu = "તણાવ ઘટાડવા અને રોગપ્રતિકારક શક્તિ માટે આજે રાત્રે 7–8 કલાક ઊંઘ લો.",
                detailHi = "तनाव कम करने और इम्युनिटी बढ़ाने के लिए आज रात 7–8 घंटे सोएं।",
                priority = InsightPriority.ACTION_NEEDED
            )
        }

        val abnormalVitals = recentVitals.filter {
            it.status.equals("High", true) || it.status.equals("Critical", true)
        }
        if (abnormalVitals.isNotEmpty()) {
            val topVital = abnormalVitals.first()
            list += SmartHealthInsight(
                id = "insight_vitals",
                titleEn = "Monitor ${topVital.vitalType} (${topVital.status})",
                titleGu = "${topVital.vitalType} પર ધ્યાન આપો (${topVital.status})",
                titleHi = "${topVital.vitalType} पर ध्यान दें (${topVital.status})",
                detailEn = "Recent reading flagged ${topVital.status}. Recheck after resting or consult a doctor.",
                detailGu = "તાજેતરનું રીડિંગ ${topVital.status} છે. આરામ કરીને ફરી તપાસો અથવા ડૉક્ટરને બતાવો.",
                detailHi = "हाल की रीडिंग ${topVital.status} है। आराम के बाद दोबारा जांचें या डॉक्टर से सलाह लें।",
                priority = InsightPriority.ACTION_NEEDED
            )
        }

        return list.take(4)
    }
}

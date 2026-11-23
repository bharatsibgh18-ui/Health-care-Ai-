package com.example.utils

import com.example.data.local.SleepWellnessLogEntity
import kotlin.math.pow
import kotlin.math.roundToInt

enum class ActivityLevel(val factor: Double, val labelEn: String, val labelGu: String, val labelHi: String) {
    SEDENTARY(1.2, "Sedentary (Desk)", "ઓછી હલનચલન", "कम सक्रिय"),
    LIGHT(1.375, "Light (1-3d/wk)", "હળવી કસરત", "हल्का व्यायाम"),
    MODERATE(1.55, "Moderate (3-5d/wk)", "મધ્યમ કસરત", "मध्यम व्यायाम"),
    ACTIVE(1.725, "Active (6-7d/wk)", "સક્રિય જીવનશૈલી", "अधिक सक्रिय")
}

data class BmiResult(
    val bmi: Double,
    val whoCategory: String,
    val asianIndianCategory: String,
    val healthyWeightMinKg: Double,
    val healthyWeightMaxKg: Double,
    val isHealthyRange: Boolean
)

data class MetabolicAndHeartResult(
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val weightLossTargetKcal: Int,
    val idealBodyWeightKg: Double,
    val maxHeartRateBpm: Int,
    val fatBurnZoneBpm: IntRange,
    val aerobicZoneBpm: IntRange,
    val peakZoneBpm: IntRange
)

data class SleepDebtSummary(
    val entriesCount: Int,
    val avgSleepHours: Double,
    val avgQuality: Double,
    val weeklySleepDebtHours: Double, // positive = deficit relative to 8h/night
    val recoveryStatus: String
)

/**
 * Pure clinical wellness calculator covering BMI (WHO + Asian-Indian cutoffs),
 * Mifflin-St Jeor BMR/TDEE, Devine Ideal Body Weight, Target Heart Rate Zones,
 * and 7-day Sleep Debt & Recovery analysis.
 */
object WellnessCalculatorEngine {

    fun calculateBmi(weightKg: Double, heightCm: Double): BmiResult {
        val safeWeight = weightKg.coerceIn(15.0, 300.0)
        val safeHeightM = (heightCm.coerceIn(80.0, 250.0)) / 100.0
        val bmi = ((safeWeight / safeHeightM.pow(2)) * 10.0).roundToInt() / 10.0

        val whoCat = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }

        // Consensus Asian-Indian clinical BMI cutoffs (WHO Asia-Pacific / Indian Ministry of Health)
        val indianCat = when {
            bmi < 18.5 -> "Underweight"
            bmi < 23.0 -> "Normal (Optimal)"
            bmi < 25.0 -> "Overweight (At Risk)"
            else -> "Obese (Clinical Care)"
        }

        val minHealthy = ((18.5 * safeHeightM.pow(2)) * 10.0).roundToInt() / 10.0
        val maxHealthy = ((24.9 * safeHeightM.pow(2)) * 10.0).roundToInt() / 10.0

        return BmiResult(
            bmi = bmi,
            whoCategory = whoCat,
            asianIndianCategory = indianCat,
            healthyWeightMinKg = minHealthy,
            healthyWeightMaxKg = maxHealthy,
            isHealthyRange = bmi in 18.5..24.9
        )
    }

    fun calculateMetabolicAndHeart(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        isMale: Boolean,
        activityLevel: ActivityLevel = ActivityLevel.MODERATE
    ): MetabolicAndHeartResult {
        val w = weightKg.coerceIn(15.0, 300.0)
        val h = heightCm.coerceIn(80.0, 250.0)
        val age = ageYears.coerceIn(5, 110)

        // Mifflin-St Jeor Equation
        val bmrRaw = (10.0 * w) + (6.25 * h) - (5.0 * age) + (if (isMale) 5.0 else -161.0)
        val bmr = bmrRaw.roundToInt().coerceAtLeast(800)
        val tdee = (bmr * activityLevel.factor).roundToInt()
        val weightLoss = (tdee - 450).coerceAtLeast(1200)

        // Devine Ideal Body Weight Formula
        val inchesOverFiveFeet = ((h / 2.54) - 60.0).coerceAtLeast(0.0)
        val idealRaw = (if (isMale) 50.0 else 45.5) + (2.3 * inchesOverFiveFeet)
        val idealKg = (idealRaw * 10.0).roundToInt() / 10.0

        // Target Heart Rate Zones
        val maxHr = (220 - age).coerceIn(100, 215)
        val fatBurn = ((maxHr * 0.50).roundToInt())..((maxHr * 0.60).roundToInt())
        val aerobic = ((maxHr * 0.60).roundToInt())..((maxHr * 0.75).roundToInt())
        val peak = ((maxHr * 0.75).roundToInt())..((maxHr * 0.90).roundToInt())

        return MetabolicAndHeartResult(
            bmrKcal = bmr,
            tdeeKcal = tdee,
            weightLossTargetKcal = weightLoss,
            idealBodyWeightKg = idealKg,
            maxHeartRateBpm = maxHr,
            fatBurnZoneBpm = fatBurn,
            aerobicZoneBpm = aerobic,
            peakZoneBpm = peak
        )
    }

    fun analyzeSleepDebt(
        logs: List<SleepWellnessLogEntity>,
        targetHoursPerNight: Double = 8.0
    ): SleepDebtSummary {
        val recent = logs.sortedByDescending { it.timestamp }.take(7)
        if (recent.isEmpty()) {
            return SleepDebtSummary(
                entriesCount = 0,
                avgSleepHours = 0.0,
                avgQuality = 0.0,
                weeklySleepDebtHours = 0.0,
                recoveryStatus = "No sleep logged yet"
            )
        }
        val avgHours = ((recent.map { it.sleepHours }.average()) * 10.0).roundToInt() / 10.0
        val avgQuality = ((recent.map { it.sleepQuality }.average()) * 10.0).roundToInt() / 10.0
        val totalDebt = recent.sumOf { (targetHoursPerNight - it.sleepHours).coerceAtLeast(0.0) }
        val roundedDebt = (totalDebt * 10.0).roundToInt() / 10.0

        val status = when {
            roundedDebt <= 1.5 && avgHours >= 7.0 -> "Optimal Recovery"
            roundedDebt <= 4.5 -> "Mild Sleep Debt"
            else -> "High Sleep Debt — Prioritize Rest"
        }

        return SleepDebtSummary(
            entriesCount = recent.size,
            avgSleepHours = avgHours,
            avgQuality = avgQuality,
            weeklySleepDebtHours = roundedDebt,
            recoveryStatus = status
        )
    }
}

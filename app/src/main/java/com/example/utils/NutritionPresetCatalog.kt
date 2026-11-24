package com.example.utils

import com.example.data.local.MealNutritionLogEntity
import kotlin.math.roundToInt

data class NutritionMealPreset(
    val id: String,
    val nameEn: String,
    val nameGu: String,
    val nameHi: String,
    val defaultMealType: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fiberGrams: Double
)

data class DailyNutritionSummary(
    val dateKey: String,
    val totalCalories: Int,
    val targetCalories: Int,
    val totalProteinGrams: Double,
    val totalCarbsGrams: Double,
    val totalFiberGrams: Double,
    val mealCount: Int,
    val progressFraction: Float
)

/**
 * Curated Indian + Global healthy meal presets and macro rollup calculations
 * for the Ultra Smart Nutrition & Calorie Tracker.
 */
object NutritionPresetCatalog {

    val presets: List<NutritionMealPreset> = listOf(
        NutritionMealPreset("poha_upma", "Poha / Upma Bowl", "પૌઆ / ઉપમા", "पोहा / उपमा", "Breakfast", 260, 6.5, 44.0, 4.5),
        NutritionMealPreset("idli_sambar", "2 Idli & Sambar", "2 ઈડલી અને સંભાર", "2 इडली व सांभर", "Breakfast", 240, 8.0, 42.0, 5.0),
        NutritionMealPreset("oats_nuts", "Oats with Milk & Nuts", "દૂધ અને ડ્રાયફ્રુટ ઓટ્સ", "दूध व मेवा ओट्स", "Breakfast", 310, 11.5, 46.0, 6.0),
        NutritionMealPreset("roti_dal", "2 Roti, Dal & Sabzi", "2 રોટલી, દાળ અને શાક", "2 रोटी, दाल व सब्ज़ी", "Lunch", 420, 15.0, 62.0, 9.5),
        NutritionMealPreset("khichdi_kadhi", "Gujarati Khichdi & Kadhi", "ખીચડી અને કઢી", "खिचड़ी और कढ़ी", "Dinner", 380, 13.0, 58.0, 7.0),
        NutritionMealPreset("sprouts_bowl", "Moong Sprouts Protein Bowl", "ફણગાવેલા મગનું સલાડ", "अंकुरित मूंग प्रोटीन बाउल", "Snack", 190, 13.5, 28.0, 7.5),
        NutritionMealPreset("fresh_fruits", "Seasonal Fruit Bowl", "તાજા ફળોની પ્લેટ", "ताज़े फलों का बाउल", "Snack", 145, 2.0, 35.0, 5.5),
        NutritionMealPreset("green_salad", "Fresh Cucumber-Tomato Salad", "કાકડી-ટામેટાનું લીલું સલાડ", "ताज़ा हरा सलाद", "Lunch", 95, 3.0, 18.0, 5.0),
        NutritionMealPreset("warm_milk", "Warm Turmeric Milk / Curd", "હળદરવાળું દૂધ / દહીં", "हल्दी दूध / दही", "Dinner", 160, 8.5, 13.0, 0.5),
        NutritionMealPreset("veg_soup", "Hot Vegetable Clear Soup", "ગરમ વેજીટેબલ સૂપ", "गर्म वेजिटेबल सूप", "Dinner", 120, 4.0, 20.0, 4.5)
    )

    fun calculateDailySummary(
        dateKey: String,
        logs: List<MealNutritionLogEntity>,
        targetCalories: Int = 2000
    ): DailyNutritionSummary {
        val todayLogs = logs.filter { it.dateKey == dateKey }
        val totalCal = todayLogs.sumOf { it.calories }
        val totalProtein = todayLogs.sumOf { it.proteinGrams }
        val totalCarbs = todayLogs.sumOf { it.carbsGrams }
        val totalFiber = todayLogs.sumOf { it.fiberGrams }
        val safeTarget = targetCalories.coerceAtLeast(1000)
        val fraction = (totalCal.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1.5f)
        return DailyNutritionSummary(
            dateKey = dateKey,
            totalCalories = totalCal,
            targetCalories = safeTarget,
            totalProteinGrams = (totalProtein * 10.0).roundToInt() / 10.0,
            totalCarbsGrams = (totalCarbs * 10.0).roundToInt() / 10.0,
            totalFiberGrams = (totalFiber * 10.0).roundToInt() / 10.0,
            mealCount = todayLogs.size,
            progressFraction = fraction
        )
    }
}

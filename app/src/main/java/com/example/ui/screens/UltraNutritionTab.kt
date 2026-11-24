package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.MealNutritionLogEntity
import com.example.ui.components.UltraGlassCard
import com.example.ui.components.UltraGradientHeroCard
import com.example.ui.components.UltraSectionHeader
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.UltraAmberStart
import com.example.ui.theme.UltraCyanEnd
import com.example.ui.theme.UltraEmeraldStart
import com.example.ui.theme.UltraGoldEnd
import com.example.ui.theme.UltraMintEnd
import com.example.ui.theme.UltraTealStart
import com.example.ui.theme.trLang
import com.example.utils.DailyNutritionSummary
import com.example.utils.NutritionPresetCatalog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UltraNutritionTab(
    summary: DailyNutritionSummary,
    todayLogs: List<MealNutritionLogEntity>,
    selectedLanguage: String,
    onLogMeal: (mealType: String, foodName: String, calories: Int, protein: Double, carbs: Double, fiber: Double) -> Unit,
    onDeleteMeal: (MealNutritionLogEntity) -> Unit,
    onUpdateTargetCalories: (Int) -> Unit
) {
    val context = LocalContext.current
    var selectedMealType by remember { mutableStateOf("Lunch") }
    var showCustomDialog by remember { mutableStateOf(false) }

    val animatedCalProgress by animateFloatAsState(
        targetValue = summary.progressFraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "nutrition_cal_ring"
    )

    if (showCustomDialog) {
        CustomMealLogDialog(
            defaultMealType = selectedMealType,
            selectedLanguage = selectedLanguage,
            onDismiss = { showCustomDialog = false },
            onSave = { name, kcal, protein, carbs, fiber ->
                onLogMeal(selectedMealType, name, kcal, protein, carbs, fiber)
                showCustomDialog = false
                Toast.makeText(
                    context,
                    trLang(selectedLanguage, "Meal logged!", "ભોજન નોંધાઈ ગયું!", "भोजन दर्ज किया गया!"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // Hero Calorie & Macro Ring Card
    UltraGradientHeroCard(
        modifier = Modifier.testTag("ultra_nutrition_hero_card"),
        colors = listOf(UltraTealStart, UltraEmeraldStart, UltraCyanEnd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(102.dp)
            ) {
                Canvas(modifier = Modifier.size(102.dp)) {
                    val stroke = 9.dp.toPx()
                    drawArc(
                        color = Color.White.copy(alpha = 0.22f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color.White, UltraGoldEnd, UltraMintEnd, Color.White)),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedCalProgress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.totalCalories}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "/ ${summary.targetCalories} kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = trLang(selectedLanguage, "Smart Nutrition & Calories", "સ્માર્ટ પોષણ અને કેલરી", "स्मार्ट पोषण और कैलोरी"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                MacroMiniBar(
                    label = trLang(selectedLanguage, "Protein", "પ્રોટીન", "प्रोटीन"),
                    grams = summary.totalProteinGrams,
                    targetGrams = 60.0,
                    barColor = Color(0xFF67E8F9)
                )
                MacroMiniBar(
                    label = trLang(selectedLanguage, "Carbs", "કાર્બ્સ", "कार्ब्स"),
                    grams = summary.totalCarbsGrams,
                    targetGrams = 250.0,
                    barColor = Color(0xFFFDE047)
                )
                MacroMiniBar(
                    label = trLang(selectedLanguage, "Fiber", "ફાઇબર", "फाइबर"),
                    grams = summary.totalFiberGrams,
                    targetGrams = 30.0,
                    barColor = Color(0xFF86EFAC)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trLang(selectedLanguage, "Goal:", "લક્ષ્ય:", "लक्ष्य:"),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold
            )
            listOf(1600, 2000, 2400).forEach { goal ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (summary.targetCalories == goal) Color.White else Color.White.copy(alpha = 0.18f),
                    onClick = { onUpdateTargetCalories(goal) },
                    modifier = Modifier.testTag("ultra_nutrition_goal_$goal")
                ) {
                    Text(
                        text = "$goal kcal",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.targetCalories == goal) UltraTealStart else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }

    // Meal Type Selector + Custom Add Button
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            Triple("Breakfast", "નાસ્તો", "नाश्ता"),
            Triple("Lunch", "બપોરનું ભોજન", "दोपहर का भोजन"),
            Triple("Dinner", "રાત્રિભોજન", "रात का भोजन"),
            Triple("Snack", "હળવો નાસ્તો", "स्नैक")
        ).forEach { (en, gu, hi) ->
            FilterChip(
                selected = selectedMealType == en,
                onClick = { selectedMealType = en },
                label = { Text(trLang(selectedLanguage, en, gu, hi), fontWeight = FontWeight.Bold) }
            )
        }
    }

    // Quick-Add Indian & Global Healthy Meals
    UltraSectionHeader(
        title = trLang(selectedLanguage, "One-Tap Healthy Meals", "એક-ટેપ તંદુરસ્ત ભોજન ઉમેરો", "एक-टैप स्वस्थ भोजन जोड़ें"),
        icon = Icons.Default.Restaurant
    )

    UltraGlassCard(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NutritionPresetCatalog.presets.forEach { preset ->
                val mealTitle = trLang(selectedLanguage, preset.nameEn, preset.nameGu, preset.nameHi)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    onClick = {
                        onLogMeal(
                            selectedMealType,
                            mealTitle,
                            preset.calories,
                            preset.proteinGrams,
                            preset.carbsGrams,
                            preset.fiberGrams
                        )
                        Toast.makeText(
                            context,
                            "+${preset.calories} kcal ($mealTitle)",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.testTag("ultra_nutrition_preset_${preset.id}")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = mealTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${preset.calories} kcal • ${preset.proteinGrams.toInt()}g P",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { showCustomDialog = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ultra_nutrition_add_custom_btn")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = trLang(selectedLanguage, "Log Custom Meal", "અન્ય ભોજન જાતે ઉમેરો", "अन्य भोजन स्वयं जोड़ें"),
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Today's Logged Meals List
    UltraSectionHeader(
        title = "${trLang(selectedLanguage, "Today's Meals", "આજના નોંધાયેલા ભોજન", "आज के दर्ज भोजन")} (${todayLogs.size})",
        icon = Icons.Default.Restaurant
    )

    if (todayLogs.isEmpty()) {
        UltraGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = trLang(
                    selectedLanguage,
                    "No meals logged today yet. Tap any healthy preset above or log a custom meal to track calories and macros.",
                    "આજે હજુ કોઈ ભોજન નોંધ્યું નથી. કેલરી અને પ્રોટીન ટ્રેક કરવા ઉપરથી કોઈ પણ ભોજન પર ટેપ કરો.",
                    "आज अभी कोई भोजन दर्ज नहीं है। कैलोरी और प्रोटीन ट्रैक करने के लिए ऊपर से किसी भी भोजन पर टैप करें।"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        todayLogs.forEach { meal ->
            UltraGlassCard(
                modifier = Modifier.fillMaxWidth().testTag("ultra_meal_log_${meal.id}"),
                contentPadding = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = meal.foodName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${meal.mealType} • ${meal.calories} kcal • P:${meal.proteinGrams.toInt()}g C:${meal.carbsGrams.toInt()}g F:${meal.fiberGrams.toInt()}g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { onDeleteMeal(meal) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = trLang(selectedLanguage, "Delete", "કાઢી નાખો", "हटाएं"),
                            tint = EmergencyRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroMiniBar(
    label: String,
    grams: Double,
    targetGrams: Double,
    barColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f))
            Text(
                text = "${grams.toInt()}g / ${targetGrams.toInt()}g",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { (grams / targetGrams).toFloat().coerceIn(0f, 1f) },
            color = barColor,
            trackColor = Color.White.copy(alpha = 0.22f),
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxWidth().height(5.dp)
        )
    }
}

@Composable
private fun CustomMealLogDialog(
    defaultMealType: String,
    selectedLanguage: String,
    onDismiss: () -> Unit,
    onSave: (String, Int, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("300") }
    var protein by remember { mutableStateOf("10") }
    var carbs by remember { mutableStateOf("40") }
    var fiber by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${trLang(selectedLanguage, "Log Custom Meal", "અન્ય ભોજન ઉમેરો", "कस्टम भोजन जोड़ें")} ($defaultMealType)",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(trLang(selectedLanguage, "Food / Dish Name *", "ભોજનનું નામ *", "भोजन का नाम *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text("Calories *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fiber,
                        onValueChange = { fiber = it },
                        label = { Text("Fiber (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kcal = calories.toIntOrNull() ?: 0
                    if (name.isNotBlank() && kcal > 0) {
                        onSave(
                            name.trim(),
                            kcal,
                            protein.toDoubleOrNull() ?: 0.0,
                            carbs.toDoubleOrNull() ?: 0.0,
                            fiber.toDoubleOrNull() ?: 0.0
                        )
                    }
                }
            ) {
                Text(trLang(selectedLanguage, "Save", "સેવ કરો", "सहेजें"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(trLang(selectedLanguage, "Cancel", "રદ", "रद्द"))
            }
        }
    )
}

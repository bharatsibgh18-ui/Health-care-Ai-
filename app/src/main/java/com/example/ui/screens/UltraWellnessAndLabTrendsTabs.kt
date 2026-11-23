package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ScannedReportEntity
import com.example.data.local.SleepWellnessLogEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.VitalLogEntity
import com.example.ui.components.UltraGlassCard
import com.example.ui.components.UltraGradientHeroCard
import com.example.ui.components.UltraMetricPill
import com.example.ui.components.UltraSectionHeader
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.UltraAmberStart
import com.example.ui.theme.UltraCrimsonStart
import com.example.ui.theme.UltraCyanEnd
import com.example.ui.theme.UltraEmeraldStart
import com.example.ui.theme.UltraIndigoStart
import com.example.ui.theme.UltraTealStart
import com.example.ui.theme.UltraVioletEnd
import com.example.ui.theme.tr
import com.example.utils.ActivityLevel
import com.example.utils.BiomarkerAlertLevel
import com.example.utils.BiomarkerTrendDirection
import com.example.utils.LabReportTrendEngine
import com.example.utils.WellnessCalculatorEngine
import kotlin.math.roundToInt

@Composable
fun UltraWellnessAndSleepTab(
    userProfile: UserProfileEntity?,
    sleepLogs: List<SleepWellnessLogEntity>,
    onSaveSleep: (Double, Int, String, String, Double, Double, String) -> Unit,
    onDeleteSleep: (SleepWellnessLogEntity) -> Unit
) {
    val latestWeight = sleepLogs.firstOrNull { it.weightKg > 0.0 }?.weightKg ?: 68.0
    val latestHeight = sleepLogs.firstOrNull { it.heightCm > 0.0 }?.heightCm ?: 170.0

    var weightKg by remember { mutableFloatStateOf(latestWeight.toFloat()) }
    var heightCm by remember { mutableFloatStateOf(latestHeight.toFloat()) }
    var isMale by remember {
        mutableStateOf(!((userProfile?.gender ?: "").equals("Female", ignoreCase = true)))
    }
    var activityLevel by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var sleepHoursSlider by remember { mutableFloatStateOf(7.5f) }
    var sleepQuality by remember { mutableIntStateOf(4) }

    val age = (userProfile?.age ?: 30).coerceIn(10, 100)
    val bmi = remember(weightKg, heightCm) {
        WellnessCalculatorEngine.calculateBmi(weightKg.toDouble(), heightCm.toDouble())
    }
    val metabolic = remember(weightKg, heightCm, age, isMale, activityLevel) {
        WellnessCalculatorEngine.calculateMetabolicAndHeart(
            weightKg = weightKg.toDouble(),
            heightCm = heightCm.toDouble(),
            ageYears = age,
            isMale = isMale,
            activityLevel = activityLevel
        )
    }
    val sleepSummary = remember(sleepLogs) {
        WellnessCalculatorEngine.analyzeSleepDebt(sleepLogs)
    }

    // Hero BMI & Metabolic Gradient Card
    UltraGradientHeroCard(
        colors = listOf(UltraIndigoStart, UltraVioletEnd, UltraTealStart),
        modifier = Modifier.testTag("ultra_wellness_hero_card")
    ) {
        Text(
            text = tr("BMI, Calorie & Heart Rate Suite", "BMI, કેલરી અને હાર્ટ રેટ કેલ્ક્યુલેટર", "BMI, कैलोरी और हार्ट रेट कैलकुलेटर"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            UltraMetricPill(
                label = "BMI (${bmi.whoCategory})",
                value = "${bmi.bmi}",
                icon = Icons.Default.Calculate,
                modifier = Modifier.weight(1f)
            )
            UltraMetricPill(
                label = tr("Daily TDEE", "દૈનિક કેલરી", "दैनिक कैलोरी"),
                value = "${metabolic.tdeeKcal} kcal",
                icon = Icons.Default.Favorite,
                tint = Color(0xFFFDE68A),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Asian-Indian Cutoff: ${bmi.asianIndianCategory} • Ideal Weight: ${metabolic.idealBodyWeightKg} kg (${bmi.healthyWeightMinKg}–${bmi.healthyWeightMaxKg} kg)",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.90f)
        )
    }

    // Interactive Sliders & Target HR Zones Card
    UltraGlassCard(
        accentStart = UltraIndigoStart,
        accentEnd = UltraCyanEnd,
        modifier = Modifier.testTag("ultra_bmi_calculator_card")
    ) {
        UltraSectionHeader(
            title = tr("Interactive Clinical Calculator", "ઇન્ટરેક્ટિવ ક્લિનિકલ કેલ્ક્યુલેટર", "इंटरैक्टिव क्लिनिकल कैलकुलेटर"),
            subtitle = tr("Adjust weight, height & activity for live BMR / HR zones", "વજન, ઊંચાઈ અને એક્ટિવિટી મુજબ લાઇવ ગણતરી", "वजन, लंबाई और गतिविधि के अनुसार लाइव गणना"),
            icon = Icons.Default.Calculate,
            accentColor = UltraIndigoStart
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("Weight", "વજન", "वजन"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text("${weightKg.roundToInt()} kg", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = weightKg,
            onValueChange = { weightKg = it },
            valueRange = 30f..150f,
            modifier = Modifier.testTag("ultra_weight_slider")
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("Height", "ઊંચાઈ", "लंबाई"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text("${heightCm.roundToInt()} cm", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = heightCm,
            onValueChange = { heightCm = it },
            valueRange = 120f..215f,
            modifier = Modifier.testTag("ultra_height_slider")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = isMale,
                onClick = { isMale = true },
                label = { Text(tr("Male", "પુરુષ", "पुरुष")) }
            )
            FilterChip(
                selected = !isMale,
                onClick = { isMale = false },
                label = { Text(tr("Female", "સ્ત્રી", "महिला")) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ActivityLevel.entries.forEach { lvl ->
                FilterChip(
                    selected = activityLevel == lvl,
                    onClick = { activityLevel = lvl },
                    label = { Text(lvl.name.take(3), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "BMR: ${metabolic.bmrKcal} kcal/day • Weight-Loss Target: ${metabolic.weightLossTargetKcal} kcal/day",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Max HR: ${metabolic.maxHeartRateBpm} bpm | Fat Burn: ${metabolic.fatBurnZoneBpm.first}-${metabolic.fatBurnZoneBpm.last} | Cardio: ${metabolic.aerobicZoneBpm.first}-${metabolic.aerobicZoneBpm.last} bpm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Daily Sleep & Weekly Sleep Debt Logger (Persisted in Room v16)
    UltraGlassCard(
        accentStart = UltraVioletEnd,
        accentEnd = UltraIndigoStart,
        modifier = Modifier.testTag("ultra_sleep_logger_card")
    ) {
        UltraSectionHeader(
            title = tr("Sleep Recovery & Sleep Debt Tracker", "ઊંઘ રિકવરી અને સ્લીપ ડેટ ટ્રેકર", "नींद रिकवरी और स्लीप डेट ट्रैकर"),
            subtitle = if (sleepSummary.entriesCount > 0) {
                "Avg ${sleepSummary.avgSleepHours}h • Debt: ${sleepSummary.weeklySleepDebtHours}h • ${sleepSummary.recoveryStatus}"
            } else {
                tr("Log last night's sleep to track recovery", "ગઈકાલની ઊંઘ નોંધો", "कल रात की नींद दर्ज करें")
            },
            icon = Icons.Default.Bedtime,
            accentColor = UltraVioletEnd
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("Sleep Duration", "ઊંઘના કલાક", "नींद के घंटे"), style = MaterialTheme.typography.labelLarge)
            Text(
                text = String.format("%.1f hrs", sleepHoursSlider),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = UltraVioletEnd
            )
        }
        Slider(
            value = sleepHoursSlider,
            onValueChange = { sleepHoursSlider = (it * 2f).roundToInt() / 2f },
            valueRange = 3f..12f,
            steps = 17,
            modifier = Modifier.testTag("ultra_sleep_hours_slider")
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(tr("Quality:", "ગુણવત્તા:", "गुणवत्ता:"), style = MaterialTheme.typography.labelMedium)
            (1..5).forEach { q ->
                FilterChip(
                    selected = sleepQuality == q,
                    onClick = { sleepQuality = q },
                    label = { Text("$q★") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onSaveSleep(
                    sleepHoursSlider.toDouble(),
                    sleepQuality,
                    "10:30 PM",
                    "06:00 AM",
                    weightKg.toDouble(),
                    heightCm.toDouble(),
                    "Logged via Ultra Wellness"
                )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ultra_save_sleep_btn")
        ) {
            Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(tr("Save Sleep & Body Snapshot", "ઊંઘ અને BMI સેવ કરો", "नींद और BMI सहेजें"))
        }

        if (sleepLogs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            sleepLogs.take(5).forEach { entry ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${entry.dateKey} • ${String.format("%.1f", entry.sleepHours)} hrs (${entry.sleepQuality}/5★)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (entry.weightKg > 0.0) {
                                Text(
                                    text = "Weight: ${entry.weightKg.roundToInt()} kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteSleep(entry) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = tr("Delete sleep log", "કાઢી નાખો", "हटाएं"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UltraLabTrendsTab(
    scannedReports: List<ScannedReportEntity>,
    vitalLogs: List<VitalLogEntity>,
    onOpenScanner: () -> Unit,
    onOpenVitals: () -> Unit
) {
    val trends = remember(scannedReports, vitalLogs) {
        LabReportTrendEngine.analyzeTrends(reports = scannedReports, vitalLogs = vitalLogs)
    }
    val alertCount = remember(trends) {
        trends.count { it.alertLevel != BiomarkerAlertLevel.NORMAL }
    }

    // Lab Trends Hero Card
    UltraGradientHeroCard(
        colors = if (alertCount > 0) {
            listOf(UltraCrimsonStart, UltraAmberStart, UltraTealStart)
        } else {
            listOf(UltraTealStart, UltraEmeraldStart, UltraIndigoStart)
        },
        modifier = Modifier.testTag("ultra_lab_trends_hero")
    ) {
        Text(
            text = tr("Lab Report Trend Intelligence", "લેબ રિપોર્ટ ટ્રેન્ડ વિશ્લેષણ", "लैब रिपोर्ट ट्रेंड विश्लेषण"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = tr(
                "Analyzed ${scannedReports.size} scanned reports & ${vitalLogs.size} vital logs • ${trends.size} biomarkers tracked ($alertCount alert${if (alertCount == 1) "" else "s"}).",
                "${scannedReports.size} સ્કેન કરેલા રિપોર્ટ અને ${vitalLogs.size} વાઇટલ્સમાંથી ${trends.size} બાયોમાર્કર ટ્રેક થયા ($alertCount એલર્ટ).",
                "${scannedReports.size} स्कैन रिपोर्ट और ${vitalLogs.size} वाइटल्स से ${trends.size} बायोमार्कर ट्रैक हुए ($alertCount अलर्ट)।"
            ),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.90f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onOpenScanner,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Scan Report", "રિપોર્ટ સ્કેન", "रिपोर्ट स्कैन"))
            }
            OutlinedButton(
                onClick = onOpenVitals,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Log Vitals", "વાઇટલ્સ નોંધો", "वाइटल्स लॉग"), color = Color.White)
            }
        }
    }

    if (trends.isEmpty()) {
        UltraGlassCard(
            accentStart = UltraTealStart,
            accentEnd = UltraIndigoStart,
            modifier = Modifier.testTag("ultra_lab_trends_empty")
        ) {
            UltraSectionHeader(
                title = tr("No Lab Biomarkers Detected Yet", "હજુ કોઈ લેબ બાયોમાર્કર મળ્યા નથી", "अभी कोई लैब बायोमार्कर नहीं मिला"),
                subtitle = tr(
                    "Scan blood tests (Hb, HbA1c, Fasting Sugar, Cholesterol, TSH, Creatinine) or log BP/Sugar vitals to unlock side-by-side trend alerts.",
                    "બ્લડ રિપોર્ટ (Hb, HbA1c, Sugar, Cholesterol, TSH) સ્કેન કરો અથવા BP/Sugar નોંધો જેથી ઓટોમેટિક ટ્રેન્ડ એલર્ટ દેખાય.",
                    "ब्लड टेस्ट (Hb, HbA1c, शुगर, कोलेस्ट्रॉल, TSH) स्कैन करें या BP/शुगर लॉग करें ताकि ट्रेंड अलर्ट मिल सकें।"
                ),
                icon = Icons.Default.Biotech,
                accentColor = UltraTealStart
            )
        }
    } else {
        trends.forEach { card ->
            val accent = when (card.alertLevel) {
                BiomarkerAlertLevel.HIGH_ALERT -> EmergencyRed
                BiomarkerAlertLevel.BORDERLINE -> UltraAmberStart
                BiomarkerAlertLevel.NORMAL -> SafeMint
            }
            val trendIcon = when (card.trendDirection) {
                BiomarkerTrendDirection.IMPROVED -> Icons.AutoMirrored.Filled.TrendingDown
                BiomarkerTrendDirection.WORSENED -> Icons.AutoMirrored.Filled.TrendingUp
                else -> Icons.AutoMirrored.Filled.TrendingFlat
            }
            UltraGlassCard(
                accentStart = accent,
                accentEnd = UltraCyanEnd,
                modifier = Modifier.testTag("lab_trend_card_${card.key}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Normal: ${card.normalRangeLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accent.copy(alpha = 0.16f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (card.alertLevel == BiomarkerAlertLevel.NORMAL) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${card.latestValue} ${card.unit}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (card.previousValue != null && card.delta != null) {
                    val deltaSign = if (card.delta > 0) "+" else ""
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = trendIcon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Previous: ${card.previousValue} ${card.unit}  •  Change: $deltaSign${card.delta} (${card.trendDirection.name})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = card.alertMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

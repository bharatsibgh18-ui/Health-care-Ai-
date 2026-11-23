package com.example.ui.screens.clinical

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.viewmodel.MediAssistViewModel
import kotlin.math.roundToInt

@Composable
fun EpidemicPredictionPane(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPredicting by viewModel.isPredictingOutbreak.collectAsState()
    val result by viewModel.activeOutbreakResult.collectAsState()
    val brief by viewModel.activeOutbreakBrief.collectAsState()
    val savedAlerts by viewModel.outbreakAlerts.collectAsState()

    var regionName by remember { mutableStateOf("Surendranagar / Saurashtra") }
    var tempCelsius by remember { mutableIntStateOf(31) }
    var humidityPercent by remember { mutableIntStateOf(74) }
    var travelDensity by remember { mutableStateOf("Moderate") }
    var hospitalLoad by remember { mutableIntStateOf(62) }
    var newsSignals by remember { mutableStateOf("Monsoon waterlogging, rising viral fever & dengue OPD visits") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            "Epidemic Outbreak Prediction & Early Alert",
                            "મહામારીની આગાહી & સરકારી એલર્ટ સિસ્ટમ",
                            "महामारी पूर्वानुमान और स्वास्थ्य विभाग अलर्ट"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Analyze weather, travel mobility, news & hospital telemetry to warn Health Dept before outbreaks spread",
                            "હવામાન, મુસાફરી, ન્યૂઝ અને હૉસ્પિટલ ડેટા પરથી કોરોના/ડેન્ગ્યુ જેવી મહામારીનો અગાઉથી એલર્ટ આપે છે",
                            "मौसम, यात्रा, समाचार और अस्पताल डेटा से महामारी फैलने से पहले स्वास्थ्य विभाग को अलर्ट"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Inputs Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = regionName,
                    onValueChange = { regionName = it },
                    label = { Text(tr("City / District / Region", "શહેર / જિલ્લો / વિસ્તાર", "शहर / जिला / क्षेत्र")) },
                    modifier = Modifier.fillMaxWidth().testTag("epidemic_region_input")
                )

                Text(tr("Temperature: ${tempCelsius}°C", "તાપમાન: ${tempCelsius}°C", "तापमान: ${tempCelsius}°C"), style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = tempCelsius.toFloat(),
                    onValueChange = { tempCelsius = it.roundToInt() },
                    valueRange = 10f..48f
                )

                Text(tr("Humidity: $humidityPercent%", "ભેજ: $humidityPercent%", "आर्द्रता: $humidityPercent%"), style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = humidityPercent.toFloat(),
                    onValueChange = { humidityPercent = it.roundToInt() },
                    valueRange = 15f..100f
                )

                Text(tr("Travel & Mobility Density", "મુસાફરી / અવરજવરનું પ્રમાણ", "यात्रा और आवागमन घनत्व"), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Low", "Moderate", "High").forEach { tier ->
                        FilterChip(
                            selected = travelDensity == tier,
                            onClick = { travelDensity = tier },
                            label = { Text(tier) }
                        )
                    }
                }

                Text(tr("Hospital OPD / Bed Load: $hospitalLoad%", "હૉસ્પિટલ OPD / બેડ લોડ: $hospitalLoad%", "अस्पताल OPD / बेड लोड: $hospitalLoad%"), style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = hospitalLoad.toFloat(),
                    onValueChange = { hospitalLoad = it.roundToInt() },
                    valueRange = 10f..100f
                )

                OutlinedTextField(
                    value = newsSignals,
                    onValueChange = { newsSignals = it },
                    label = { Text(tr("Local News & Symptom Signals", "ન્યૂઝ અને રોગચાળાના સંકેતો", "स्थानीय समाचार और लक्षण संकेत")) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("epidemic_news_input")
                )

                Button(
                    onClick = {
                        viewModel.predictEpidemicOutbreak(
                            regionName = regionName,
                            tempCelsius = tempCelsius,
                            humidityPercent = humidityPercent,
                            travelDensity = travelDensity,
                            hospitalLoadPercent = hospitalLoad,
                            newsSignals = newsSignals
                        )
                    },
                    enabled = !isPredicting,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("epidemic_predict_btn")
                ) {
                    if (isPredicting) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(tr("Run Outbreak Forecast & Alert", "મહામારી એલર્ટ જનરેટ કરો", "महामारी अलर्ट जनरेट करें"))
                }
            }
        }

        // Prediction Output Card
        result?.let { pred ->
            val maxRisk = maxOf(pred.vectorRiskScore, pred.respiratoryRiskScore, pred.waterBorneRiskScore)
            val cardColor = if (maxRisk >= 70) EmergencyRed.copy(alpha = 0.12f) else SafeMint.copy(alpha = 0.12f)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier.fillMaxWidth().testTag("epidemic_result_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pred.overallAlertTier,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (maxRisk >= 70) EmergencyRed else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        brief?.let { textToShare ->
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Health Dept Outbreak Alert - $regionName")
                                        putExtra(Intent.EXTRA_TEXT, textToShare)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, regionName))
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = tr("Share Health Dept Alert", "સ્વાસ્થ્ય વિભાગને શેર કરો", "स्वास्थ्य विभाग को शेयर करें"), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    RiskGaugeRow(tr("Vector-Borne (Dengue/Malaria)", "મચ્છરજન્ય (ડેન્ગ્યુ/મેલેરિયા)", "मच्छर जनित (डेंगू/मलेरिया)"), pred.vectorRiskScore)
                    RiskGaugeRow(tr("Respiratory / Viral (Flu/COVID)", "શ્વસન / વાયરલ (ફ્લૂ/કોરોના)", "श्वसन / वायरल (फ्लू/कोविड)"), pred.respiratoryRiskScore)
                    RiskGaugeRow(tr("Water-Borne (Typhoid/Cholera)", "પાણીજન્ય રોગો", "जलजनित रोग"), pred.waterBorneRiskScore)

                    pred.riskDrivers.forEach { driver ->
                        Text("• $driver", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    brief?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Saved Alerts Archive
        if (savedAlerts.isNotEmpty()) {
            Text(
                text = tr("Recent Outbreak Bulletins (${savedAlerts.size})", "સાચવેલા મહામારી એલર્ટ (${savedAlerts.size})", "सहेजे गए महामारी बुलेटिन (${savedAlerts.size})"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            savedAlerts.take(5).forEach { alert ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${alert.regionName} • ${alert.overallAlertTier}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Vector: ${alert.vectorRiskScore}% | Respiratory: ${alert.respiratoryRiskScore}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.deleteOutbreakAlert(alert) }) {
                            Icon(Icons.Default.Delete, contentDescription = tr("Delete", "કાઢો", "हटाएं"), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RiskGaugeRow(label: String, score: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("$score%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp))
        )
    }
}

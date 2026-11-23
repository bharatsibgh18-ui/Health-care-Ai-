package com.example.ui.screens.clinical

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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PredictiveAnalyticsPane(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val riskSnapshot by viewModel.predictiveRiskSnapshot.collectAsState()
    val isGenerating by viewModel.isGeneratingRiskForecast.collectAsState()
    val activeForecast by viewModel.activeRiskForecast.collectAsState()
    val savedAssessments by viewModel.riskAssessments.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Overview Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            "Predictive Analytics & Early Detection",
                            "પ્રિડિક્ટિવ એનાલિટિક્સ & પ્રારંભિક નિદાન",
                            "प्रेडिक्टिव एनालिटिक्स और प्रारंभिक पहचान"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Analyzed ${riskSnapshot.dataPointsAnalyzed} records • Overall Status: ${riskSnapshot.overallRiskLevel}",
                            "${riskSnapshot.dataPointsAnalyzed} આરોગ્ય રેકોર્ડનું વિશ્લેષણ • સ્થિતિ: ${riskSnapshot.overallRiskLevel}",
                            "${riskSnapshot.dataPointsAnalyzed} स्वास्थ्य रिकॉर्ड का विश्लेषण • स्थिति: ${riskSnapshot.overallRiskLevel}"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Three Clinical Risk Index Cards
        RiskScoreCard(
            title = tr("Cardiovascular / Heart Risk", "હૃદય રોગ (Cardiac Risk)", "हृदय रोग जोखिम"),
            score = riskSnapshot.heartRiskScore,
            icon = Icons.Default.Favorite,
            factors = riskSnapshot.heartFactors,
            tag = "risk_card_heart"
        )

        RiskScoreCard(
            title = tr("Sepsis & Systemic Infection Early Warning", "સેપ્સિસ અને ચેપ પ્રારંભિક ચેતવણી", "सेप्सिस और संक्रमण प्रारंभिक चेतावनी"),
            score = riskSnapshot.sepsisRiskScore,
            icon = Icons.Default.Coronavirus,
            factors = riskSnapshot.sepsisFactors,
            tag = "risk_card_sepsis"
        )

        RiskScoreCard(
            title = tr("Metabolic & Glycemic Trajectory", "મેટાબોલિક અને સુગર જોખમ", "मेटाबोलिक और शुगर जोखिम"),
            score = riskSnapshot.metabolicRiskScore,
            icon = Icons.Default.MonitorHeart,
            factors = riskSnapshot.metabolicFactors,
            tag = "risk_card_metabolic"
        )

        // Immediate Preventive Actions Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Timely Preventive Actions", "સમયસર લેવાના પગલાં", "समय पर बचाव के कदम"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                riskSnapshot.immediateActions.forEach { action ->
                    Text("• $action", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { viewModel.setScreen(AppScreen.VITALS_TRACKER) },
                modifier = Modifier.weight(1f)
            ) {
                Text(tr("Add Vitals", "વાઇટલ્સ ઉમેરો", "वाइटल्स जोड़ें"))
            }
            Button(
                onClick = { viewModel.generatePredictiveRiskForecast() },
                enabled = !isGenerating,
                modifier = Modifier.weight(1.4f).testTag("run_predictive_ai_btn")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    tr("Run AI Deep Risk Forecast", "AI ઊંડાણપૂર્વક જોખમ આગાહી કરો", "AI गहन जोखिम पूर्वानुमान चलाएं")
                )
            }
        }

        if (!activeForecast.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().testTag("predictive_forecast_result_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tr("AI Early Detection Report", "AI પ્રારંભિક ચેતવણી રિપોર્ટ", "AI प्रारंभिक पहचान रिपोर्ट"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(activeForecast!!, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (savedAssessments.isNotEmpty()) {
            Text(
                text = tr("Past Risk Snapshots (${savedAssessments.size})", "જૂના જોખમ રિપોર્ટ (${savedAssessments.size})", "पिछले जोखिम रिकॉर्ड (${savedAssessments.size})"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            val fmt = remember { SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()) }
            savedAssessments.take(4).forEach { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${item.overallRiskLevel} • Heart ${item.heartRiskScore}% | Sepsis ${item.sepsisRiskScore}% | Metabolic ${item.metabolicRiskScore}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                fmt.format(Date(item.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.deleteRiskAssessment(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = tr("Delete", "કાઢી નાખો", "हटाएं"), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RiskScoreCard(
    title: String,
    score: Int,
    icon: ImageVector,
    factors: List<String>,
    tag: String
) {
    val riskColor = when {
        score >= 70 -> EmergencyRed
        score >= 45 -> Color(0xFFE65100)
        score >= 25 -> Color(0xFFF9A825)
        else -> SafeMint
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = riskColor, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "$score%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = riskColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (score / 100f).coerceIn(0.03f, 1f) },
                color = riskColor,
                trackColor = riskColor.copy(alpha = 0.16f),
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )

            factors.forEach { f ->
                Text(
                    text = "• $f",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

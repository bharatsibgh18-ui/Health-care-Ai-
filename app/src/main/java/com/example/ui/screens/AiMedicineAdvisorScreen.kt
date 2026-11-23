package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ClinicalAiTab
import com.example.ui.viewmodel.MediAssistViewModel
import com.example.utils.MedicineRecommendationItem
import com.example.utils.SeverityGuidanceTier

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiMedicineAdvisorScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val result by viewModel.medicineAdvisorResult.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzingMedicineAdvisor.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var symptomInput by remember { mutableStateOf("") }
    var severityValue by remember { mutableFloatStateOf(4f) }
    var selectedDuration by remember { mutableStateOf("1-2 Days") }

    val severityInt = severityValue.toInt().coerceIn(1, 10)
    val severityBadgeColor = when {
        severityInt <= 3 -> SafeMint
        severityInt <= 7 -> PrimaryTeal
        else -> EmergencyRed
    }
    val severityLabel = when {
        severityInt <= 3 -> tr(
            "Mild ($severityInt/10) • No Medicine Needed",
            "સામાન્ય ($severityInt/10) • દવાની જરૂર નથી",
            "हल्का ($severityInt/10) • दवा की जरूरत नहीं"
        )
        severityInt <= 7 -> tr(
            "Moderate ($severityInt/10) • Doctor + Medicine Ref",
            "મધ્યમ ($severityInt/10) • ડૉક્ટર + દવાની માહિતી",
            "मध्यम ($severityInt/10) • डॉक्टर + दवा जानकारी"
        )
        else -> tr(
            "Severe ($severityInt/10) • Urgent Doctor Care",
            "ગંભીર ($severityInt/10) • તાત્કાલિક ડૉક્ટર પાસે જાઓ",
            "गंभीर ($severityInt/10) • तुरंत डॉक्टर को दिखाएं"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = tr(
                                    "AI Medicine & Illness Advisor",
                                    "AI દવા અને રોગ સલાહકાર",
                                    "AI दवा और रोग सलाहकार"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr(
                                    "Severity Check • Online Medicine Photos • Safety First",
                                    "ગંભીરતા તપાસ • દવાનો ઓનલાઇન ફોટો • ડૉક્ટર સલાહ",
                                    "गंभीरता जांच • दवा की ऑनलाइन फोटो • डॉक्टर सलाह"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.TRIAGE_CHAT) },
                        modifier = Modifier.testTag("med_advisor_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "પાછા જાઓ", "वापस जाएं")
                        )
                    }
                },
                actions = {
                    LanguageSelector(
                        viewModel = viewModel,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Symptom + Severity Input Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = tr(
                                "Enter Any Illness or Symptoms",
                                "તમારી કોઈ પણ બીમારી કે લક્ષણો લખો",
                                "कोई भी बीमारी या लक्षण दर्ज करें"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = symptomInput,
                            onValueChange = { symptomInput = it },
                            label = {
                                Text(
                                    tr(
                                        "e.g. Fever & body pain, Acidity, Allergy, Cough...",
                                        "દા.ત. તાવ અને દુખાવો, એસિડિટી, એલર્જી, ઉધરસ, દાદર...",
                                        "जैसे बुखार और बदन दर्द, एसिडिटी, एलर्जी, खांसी..."
                                    )
                                )
                            },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("med_advisor_symptom_input")
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "Fever & Headache" to tr("Fever & Headache", "તાવ અને માથું", "बुखार और सिरदर्द"),
                                "Cold & Allergy" to tr("Cold & Allergy", "શરદી અને એલર્જી", "जुकाम और एलर्जी"),
                                "Acidity & Gas" to tr("Acidity & Gas", "એસિડિટી અને ગેસ", "एसिडिटी और गैस"),
                                "Cough & Sore Throat" to tr("Cough & Throat", "ઉધરસ અને ગળું", "खांसी और गला"),
                                "Diarrhea & Vomiting" to tr("Diarrhea", "ઝાડા-ઉલટી", "दस्त और उल्टी"),
                                "Fungal Skin Rash" to tr("Skin Rash", "ચામડી / દાદર", "त्वचा / दाद"),
                                "Joint & Muscle Pain" to tr("Joint Pain", "સાંધાનો દુખાવો", "जोड़ों का दर्द")
                            ).forEach { (queryVal, chipLabel) ->
                                SuggestionChip(
                                    onClick = { symptomInput = queryVal },
                                    label = { Text(chipLabel, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Severity Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("Symptom Severity", "લક્ષણોની ગંભીરતા", "लक्षणों की गंभीरता"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = severityBadgeColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = severityLabel,
                                    color = severityBadgeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Slider(
                            value = severityValue,
                            onValueChange = { severityValue = it },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = severityBadgeColor,
                                activeTrackColor = severityBadgeColor
                            ),
                            modifier = Modifier.testTag("med_advisor_severity_slider")
                        )

                        // Duration chips
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Today", "1-2 Days", "3+ Days").forEach { dur ->
                                FilterChip(
                                    selected = selectedDuration == dur,
                                    onClick = { selectedDuration = dur },
                                    label = { Text(dur, fontSize = 12.sp) },
                                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.evaluateMedicineAdvisor(
                                    symptomOrIllness = symptomInput.ifBlank { "Fever & Headache" },
                                    severity = severityInt,
                                    duration = selectedDuration
                                )
                            },
                            enabled = !isAnalyzing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("med_advisor_analyze_btn")
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tr("Analyzing Symptoms...", "તપાસ ચાલુ છે...", "लक्षणों की जांच हो रही है..."))
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr(
                                        "Check Severity & Medicines",
                                        "ગંભીરતા અને દવા તપાસો",
                                        "गंभीरता और दवा जांचें"
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            result?.let { advice ->
                // Clinical Severity Decision Banner
                item {
                    val (cardColor, accentColor, iconVec) = when (advice.tier) {
                        SeverityGuidanceTier.MILD_NO_MED_NEEDED -> Triple(
                            SafeMint.copy(alpha = 0.16f),
                            PrimaryTeal,
                            Icons.Default.CheckCircle
                        )
                        SeverityGuidanceTier.MODERATE_SUPPORTIVE_CARE -> Triple(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.primary,
                            Icons.Default.LocalHospital
                        )
                        SeverityGuidanceTier.SEVERE_DOCTOR_URGENT -> Triple(
                            EmergencyRed.copy(alpha = 0.14f),
                            EmergencyRed,
                            Icons.Default.Warning
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("med_advisor_result_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = iconVec,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = advice.doctorHeadline,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor
                                )
                            }

                            advice.networkWarningMessage?.let { slowWarn ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
                                ) {
                                    Text(
                                        text = "⏳ $slowWarn",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Text(
                                text = advice.clinicalSummary,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            advice.allergyAlert?.let { alert ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = EmergencyRed.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = alert,
                                        color = EmergencyRed,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            if (advice.homeCareSteps.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tr("Safe Care Steps:", "સલામત સંભાળ પગલાં:", "सुरक्षित देखभाल कदम:"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                advice.homeCareSteps.forEach { step ->
                                    Text(
                                        text = "• $step",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            // Direct Doctor & Video Call shortcuts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.openClinicalAiSuite(ClinicalAiTab.DOCTOR_FINDER)
                                    },
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tr("Find Doctor", "ડૉક્ટર શોધો", "डॉक्टर खोजें"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.setScreen(AppScreen.AI_VIDEO_CALL) },
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tr("AI Video Call", "AI વિડિયો કૉલ", "AI वीडियो कॉल"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Medicines List with Online Photos (shown when Moderate/Severe)
                if (advice.medicines.isNotEmpty()) {
                    item {
                        Text(
                            text = tr(
                                "Available Medicine References & Online Photos",
                                "ઉપલબ્ધ દવાઓના નામ અને ઓનલાઇન ફોટા",
                                "उपलब्ध दवाओं के नाम और ऑनलाइन फोटो"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(advice.medicines, key = { it.name }) { med ->
                        MedicineReferenceCard(
                            medicine = med,
                            onViewOnlinePhoto = {
                                val url = med.onlinePhotoSearchUrl
                                if (url.isNotBlank()) {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                }
                            }
                        )
                    }
                }

                // Safety & Legal Disclaimer Footer
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(
                                    "Medical Disclaimer: Always confirm exact dosage and allergies with a licensed doctor or pharmacist before taking any medication.",
                                    "તબીબી સૂચના: કોઈ પણ દવા લેતા પહેલા પ્રમાણિત ડૉક્ટર અથવા ફાર્માસિસ્ટની સલાહ અને ડોઝ અવશ્ય પૂછી લો.",
                                    "चिकित्सा अस्वीकरण: कोई भी दवा लेने से पहले हमेशा प्रमाणित डॉक्टर या फार्मासिस्ट से सलाह लें।"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicineReferenceCard(
    medicine: MedicineRecommendationItem,
    onViewOnlinePhoto: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("med_advisor_item_${medicine.name}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Online Photo Thumbnail (via Coil AsyncImage + Wikipedia REST image)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                    if (!medicine.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = medicine.imageUrl,
                            contentDescription = "${medicine.name} photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = medicine.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val isRx = medicine.rxStatus.contains("Rx", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isRx) EmergencyRed.copy(alpha = 0.14f) else SafeMint.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = medicine.rxStatus,
                            color = if (isRx) EmergencyRed else PrimaryTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (medicine.usageReason.isNotBlank()) {
                Text(
                    text = medicine.usageReason,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (medicine.dosageNote.isNotBlank()) {
                Text(
                    text = "Dosage Note: ${medicine.dosageNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (medicine.safetyWarning.isNotBlank()) {
                Text(
                    text = "⚠ ${medicine.safetyWarning}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmergencyRed,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onViewOnlinePhoto,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (medicine.imageUrl.isNullOrBlank()) {
                        tr("View Online Medicine Photo", "ઓનલાઇન દવાનો ફોટો જુઓ", "ऑनलाइन दवा की फोटो देखें")
                    } else {
                        tr("Open More Photos Online", "વધુ ઓનલાઇન ફોટા જુઓ", "ऑनलाइन और फोटो देखें")
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

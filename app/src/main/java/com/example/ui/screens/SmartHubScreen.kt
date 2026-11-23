package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ClinicalAiTab
import com.example.ui.viewmodel.MediAssistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartHubScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val context = LocalContext.current

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
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = tr(
                                    "Smart Health Ecosystem & AI",
                                    "સ્માર્ટ હેલ્થ ઇકોસિસ્ટમ & AI",
                                    "स्मार्ट हेल्थ इकोसिस्टम & AI"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Diet Scanner, Lab Booking & Tele-Consult",
                                    "ડાયેટ સ્કેનર, લેબ બુકિંગ અને ડૉક્ટર સલાહ",
                                    "डाइट स्कैनर, लैब बुकिंग और डॉक्टर परामर्श"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.TRIAGE_CHAT) },
                        modifier = Modifier.testTag("hub_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "પાછા જાઓ", "वापस जाएं")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ultra Health Command Center Hero Banner
            com.example.ui.components.UltraGradientHeroCard(
                modifier = Modifier.testTag("hub_ultra_command_center_card")
            ) {
                Text(
                    text = tr(
                        "✨ Ultra Health Command Center (v${com.example.BuildConfig.VERSION_NAME})",
                        "✨ અલ્ટ્રા હેલ્થ કમાન્ડ સેન્ટર (v${com.example.BuildConfig.VERSION_NAME})",
                        "✨ अल्ट्रा हेल्थ कमांड सेंटर (v${com.example.BuildConfig.VERSION_NAME})"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tr(
                        "Live 0–100 Health Score, Medication Streaks, Paramedic QR Medical ID (PMJAY/ABHA), BMI & Sleep Debt, and Lab Biomarker Trend Alerts.",
                        "લાઇવ 0–100 હેલ્થ સ્કોર, દવા સ્ટ્રીક, QR મેડિકલ ID (PMJAY/ABHA), BMI અને ઊંઘ ટ્રેકર, અને લેબ રિપોર્ટ ટ્રેન્ડ એલર્ટ.",
                        "लाइव 0–100 हेल्थ स्कोर, दवा स्ट्रीक, QR मेडिकल ID (PMJAY/ABHA), BMI और नींद ट्रैकर, और लैब रिपोर्ट ट्रेंड अलर्ट।"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.90f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.openUltraHealthHub(com.example.ui.viewmodel.UltraHealthTab.SCORE) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = com.example.ui.theme.UltraTealStart
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("hub_btn_ultra_score")
                    ) {
                        Text(tr("Health Score", "હેલ્થ સ્કોર", "हेल्थ स्कोर"), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.openUltraHealthHub(com.example.ui.viewmodel.UltraHealthTab.MEDICAL_ID) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.22f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("hub_btn_ultra_qr_id")
                    ) {
                        Text(tr("QR Medical ID", "QR કાર્ડ", "QR कार्ड"), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.openUltraHealthHub(com.example.ui.viewmodel.UltraHealthTab.WELLNESS) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.20f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("hub_btn_ultra_wellness")
                    ) {
                        Text(tr("BMI & Sleep", "BMI અને ઊંઘ", "BMI व नींद"), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { viewModel.openUltraHealthHub(com.example.ui.viewmodel.UltraHealthTab.LAB_TRENDS) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.20f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("hub_btn_ultra_labs")
                    ) {
                        Text(tr("Lab Trends", "લેબ ટ્રેન્ડ", "लैब ट्रेंड"), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Direct Official NHA PMJAY Hospital Finder Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth().testTag("hub_pmjay_hospital_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalHospital,
                                    contentDescription = "NEAR PMJAY HOSPITAL FIND",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NEAR PMJAY HOSPITAL FIND",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = tr(
                                    "Search nearest Ayushman Bharat PM-JAY empaneled hospitals directly on hem.nha.gov.in",
                                    "નજીકની આયુષ્માન ભારત PM-JAY હોસ્પિટલ સીધી hem.nha.gov.in પર શોધો",
                                    "नजदीकी आयुष्मान भारत PM-JAY अस्पताल सीधे hem.nha.gov.in पर खोजें"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = { com.example.utils.PmjayPortalHelper.openNearPmjayHospitalSearch(context) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("hub_near_pmjay_hospital_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NEAR PMJAY HOSPITAL FIND",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Live AI Doctor Video Call Highlight Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SafeMint.copy(alpha = 0.16f)),
                modifier = Modifier.fillMaxWidth().testTag("hub_ai_video_call_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.VideoCall, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    "📹 Live AI Doctor Video Call",
                                    "📹 AI ડૉક્ટર સાથે લાઇવ વિડિયો કૉલ",
                                    "📹 AI डॉक्टर के साथ लाइव वीडियो कॉल"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Speak face-to-face on video with AI & show symptoms directly through your camera",
                                    "વિડિયો કૉલ પર અવાજ અને કૅમેરા દ્વારા AI સાથે સીધી વાતચીત કરી તાત્કાલિક સલાહ મેળવો",
                                    "वीडियो कॉल पर आवाज़ और कैमरे के ज़रिए AI डॉक्टर से आमने-सामने बात करें"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.setScreen(AppScreen.AI_VIDEO_CALL) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("hub_btn_start_ai_video_call")
                    ) {
                        Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Start AI Video Call Now", "વિડિયો કૉલ શરૂ કરો", "वीडियो कॉल शुरू करें"))
                    }
                }
            }

            // AI Symptom-Based Medicine & Severity Advisor Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth().testTag("hub_medicine_advisor_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Medication, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    "💊 AI Medicine & Severity Advisor",
                                    "💊 AI દવા અને ગંભીરતા સલાહકાર",
                                    "💊 AI दवा और गंभीरता सलाहकार"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Checks severity: advises doctor/no unnecessary medicine when mild, or shows available medicine names & online photos.",
                                    "લક્ષણોની ગંભીરતા તપાસે: હળવા હોય તો દવા વગર ડૉક્ટરની સલાહ સૂચવે, નહિતર દવાના નામ અને ઓનલાઇન ફોટો બતાવે.",
                                    "लक्षण की गंभीरता जांचें: हल्के होने पर बिना दवा डॉक्टर सलाह, वरना उपलब्ध दवा नाम व ऑनलाइन फोटो देखें।"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.setScreen(AppScreen.AI_MEDICINE_ADVISOR) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("hub_btn_open_medicine_advisor")
                    ) {
                        Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Open AI Medicine Advisor", "AI દવા સલાહકાર ખોલો", "AI दवा सलाहकार खोलें"))
                    }
                }
            }

            // Clinical AI Suite Highlight Card (9 Advanced Modules)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth().testTag("clinical_ai_suite_hub_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = tr(
                            "🧬 Clinical AI & Virtual Ward Suite",
                            "🧬 ક્લિનિકલ AI & વર્ચ્યુઅલ વોર્ડ સ્યુટ",
                            "🧬 क्लिनिकल AI और वर्चुअल वार्ड सुइट"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = tr(
                            "Personalized treatments, early heart/sepsis prediction, NLP clinical notes & BLE smartwatch monitoring.",
                            "વ્યક્તિગત સારવાર, હૃદય/સેપ્સિસ જોખમ આગાહી, NLP મેડિકલ નોટ્સ અને સ્માર્ટવોચ મોનિટરિંગ.",
                            "व्यक्तिगत उपचार, हृदय/सेप्सिस जोखिम अनुमान, NLP नोट्स और स्मार्टवॉच मॉनिटरिंग।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.openClinicalAiSuite(ClinicalAiTab.TREATMENT_PLAN) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("hub_btn_treatment_ai")
                        ) {
                            Icon(Icons.Default.Biotech, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Treatment AI", "સારવાર AI", "उपचार AI"))
                        }
                        Button(
                            onClick = { viewModel.openClinicalAiSuite(ClinicalAiTab.PREDICTIVE_ANALYTICS) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("hub_btn_risk_predict")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Risk Predict", "જોખમ આગાહી", "जोखिम अनुमान"))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { viewModel.openClinicalAiSuite(ClinicalAiTab.CLINICAL_SCRIBE) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("hub_btn_clinical_scribe")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Clinical NLP", "ક્લિનિકલ NLP", "क्लिनिकल NLP"))
                        }
                        OutlinedButton(
                            onClick = { viewModel.openClinicalAiSuite(ClinicalAiTab.REMOTE_MONITORING) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("hub_btn_remote_ward")
                        ) {
                            Icon(Icons.Default.Watch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Virtual Ward", "વર્ચ્યુઅલ વોર્ડ", "वर्चुअल वार्ड"))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.openClinicalAiSuite(ClinicalAiTab.EHR_AUTOMATION) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("hub_btn_ehr_auto")
                        ) {
                            Icon(Icons.Default.FolderShared, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("EHR Auto", "EHR ઓટો", "EHR ऑटो"))
                        }
                        Button(
                            onClick = { viewModel.openClinicalAiSuite(ClinicalAiTab.MENTAL_HEALTH) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("hub_btn_mental_health")
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Mental Health", "માનસિક સપોર્ટ", "मानसिक मदद"))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { viewModel.openClinicalAiSuite(ClinicalAiTab.DRUG_DISCOVERY) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("hub_btn_drug_discovery")
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Drug R&D", "દવા શોધ", "ड्रग R&D"))
                        }
                        OutlinedButton(
                            onClick = { viewModel.openClinicalAiSuite(ClinicalAiTab.EPIDEMIC_PREDICTION) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("hub_btn_epidemic_alert")
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Outbreak AI", "મહામારી AI", "महामारी AI"))
                        }
                    }
                    Button(
                        onClick = { viewModel.openClinicalAiSuite(ClinicalAiTab.DOCTOR_FINDER) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("hub_btn_doctor_finder")
                    ) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Smart Doctor Finder & Booking", "સ્માર્ટ ડૉક્ટર ફાઇન્ડર & બુકિંગ", "स्मार्ट डॉक्टर फ़ाइंडर और बुकिंग"))
                    }
                }
            }

            // Section 1: Diet & Nutrition AI Scanner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("diet_nutrition_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SafeMint.copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color(0xFF1B5E20), modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    "🍲 Diet & Nutrition AI Scanner",
                                    "🍲 ડાયેટ અને ન્યુટ્રિશન AI સ્કેનર",
                                    "🍲 डाइट और पोषण AI स्कैनर"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Scan your food plate to estimate calories, carbs, & protein",
                                    "ખોરાકનો ફોટો પાડી કેલરી, પ્રોટીન અને કાર્બ્સ તપાસો",
                                    "भोजन की थाली से कैलोरी, प्रोटीन और कार्ब्स की जानकारी लें"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.sendMessage(
                                trLang(
                                    selectedLanguage,
                                    "Please act as an AI Nutritionist. What are healthy balanced lunch options for maintaining stable blood sugar and energy?",
                                    "કૃપા કરીને AI ન્યુટ્રિશનિસ્ટ તરીકે બ્લડ સુગર અને ઊર્જા જાળવી રાખવા માટે આરોગ્યપ્રદ ગુજરાતી ભોજનના વિકલ્પો જણાવો.",
                                    "कृपया एक AI पोषण विशेषज्ञ के रूप में ब्लड शुगर और ऊर्जा बनाए रखने के लिए स्वस्थ भारतीय भोजन के विकल्प बताएं।"
                                )
                            )
                            viewModel.setScreen(AppScreen.TRIAGE_CHAT)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            tr(
                                "Ask AI Nutritionist & Diet Advice",
                                "AI ન્યુટ્રિશન ડાયેટ સલાહ મેળવો",
                                "AI पोषण और डाइट सलाह लें"
                            )
                        )
                    }
                }
            }

            // Section 2: Certified Doctor Tele-Consultation Booking
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("teleconsult_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.VideoCall, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    "👨‍⚕️ Certified Doctor Video Consult",
                                    "👨‍⚕️ સર્ટિફાઇડ ડૉક્ટર વિડીયો કન્સલ્ટેશન",
                                    "👨‍⚕️ प्रमाणित डॉक्टर वीडियो परामर्श"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Book one-on-one virtual appointments with licensed specialists",
                                    "AI ચેટથી આગળ વધી વાસ્તવિક લાયસન્સ ડૉક્ટર સાથે ઓનલાઈન કન્સલ્ટેશન",
                                    "लाइसेंस प्राप्त विशेषज्ञ डॉक्टरों के साथ ऑनलाइन परामर्श बुक करें"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.openClinicalAiSuite(ClinicalAiTab.DOCTOR_FINDER)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr("Book Doctor", "એપોઇન્ટમેન્ટ બુક કરો", "अपॉइंटमेंट बुक करें"))
                        }
                        Button(
                            onClick = {
                                viewModel.setScreen(AppScreen.EMERGENCY_GUIDE)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr("Nearby Hospitals", "નજીકના દવાખાના", "नजदीकी अस्पताल"))
                        }
                    }
                }
            }

            // Section 3: Lab Test Booking & Home Sample Collection
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("lab_booking_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    "🧪 Lab Test & Report Analyzer",
                                    "🧪 લેબ ટેસ્ટ અને રિપોર્ટ એનાલાઇઝર",
                                    "🧪 लैब टेस्ट और रिपोर्ट विश्लेषक"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Upload lab PDFs or scan reports for instant AI explanation",
                                    "ઘરેથી લેબ રિપોર્ટ PDF કે ફોટો અપલોડ કરી તરત સમજૂતી મેળવો",
                                    "लैब रिपोर्ट PDF या फ़ोटो अपलोड करके तुरंत आसान विश्लेषण पाएं"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.setScreen(AppScreen.REPORT_SCANNER)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            tr(
                                "Open Report Vault & Scanner",
                                "લેબ રિપોર્ટ સ્કેનર અને હિસ્ટ્રી",
                                "रिपोर्ट स्कैनर और रिकॉर्ड खोलें"
                            )
                        )
                    }
                }
            }

            // Section 4: Daily Health Streak & AI Coach Challenge
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("ai_coach_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFB300).copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    "🏆 AI Health Coach & 7-Day Streak",
                                    "🏆 AI હેલ્થ કોચ & ૭ દિવસ સ્ટ્રીક ચેલેન્જ",
                                    "🏆 AI हेल्थ कोच और 7-दिन स्ट्रीक"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Complete 8,000 daily steps & 2L water to maintain your health streak",
                                    "રોજ ૮,૦૦૦ પગલાં અને ૨ લિટર પાણીનો ટાર્ગેટ પૂરો કરી સ્વાસ્થ્ય જાળવો",
                                    "रोज़ाना 8,000 कदम और 2 लीटर पानी का लक्ष्य पूरा कर स्वस्थ रहें"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { viewModel.setScreen(AppScreen.STEP_COUNTER) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr("View Steps", "સ્ટેપ્સ જુઓ", "कदम देखें"))
                        }
                        Button(
                            onClick = { viewModel.setScreen(AppScreen.WATER_REMINDER) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr("Water Tracker", "પાણી રિમાઇન્ડર", "पानी रिमाइंडर"))
                        }
                    }
                }
            }

            // Section 5: Medical Insurance & Expense Tracker
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("insurance_expense_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    "💳 Health Profile & Medical Records",
                                    "💳 હેલ્થ પ્રોફાઇલ અને મેડિકલ રેકોર્ડ્સ",
                                    "💳 हेल्थ प्रोफ़ाइल और मेडिकल रिकॉर्ड"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Manage your medical background, allergies, medications & cloud backup",
                                    "તમારી મેડિકલ વિગતો, એલર્જી, દવાઓ અને ક્લાઉડ બેકઅપ સાચવો",
                                    "अपनी मेडिकल जानकारी, एलर्जी, दवाइयां और क्लाउड बैकअप संभालें"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.setScreen(AppScreen.PROFILE)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            tr(
                                "View in Health Profile",
                                "પ્રોફાઇલમાં હેલ્થ ડેટા જુઓ",
                                "हेल्थ प्रोफ़ाइल देखें"
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

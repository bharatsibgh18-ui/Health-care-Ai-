package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.screens.clinical.ClinicalDocumentationPane
import com.example.ui.screens.clinical.DoctorFinderPane
import com.example.ui.screens.clinical.DrugDiscoveryPane
import com.example.ui.screens.clinical.EhrAutomationPane
import com.example.ui.screens.clinical.EpidemicPredictionPane
import com.example.ui.screens.clinical.MentalHealthPane
import com.example.ui.screens.clinical.PersonalizedTreatmentPane
import com.example.ui.screens.clinical.PredictiveAnalyticsPane
import com.example.ui.screens.clinical.RemoteMonitoringPane
import com.example.ui.theme.tr
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ClinicalAiTab
import com.example.ui.viewmodel.MediAssistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalIntelligenceScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedClinicalTab.collectAsState()

    BackHandler {
        viewModel.setScreen(AppScreen.TRIAGE_CHAT)
    }

    val tabs = listOf(
        Triple(ClinicalAiTab.EHR_AUTOMATION, Icons.Default.FolderShared, tr("EHR Auto", "EHR ઓટોમેશન", "EHR ऑटो")),
        Triple(ClinicalAiTab.MENTAL_HEALTH, Icons.Default.Psychology, tr("Mental Health", "માનસિક સ્વાસ્થ્ય", "मानसिक स्वास्थ्य")),
        Triple(ClinicalAiTab.DRUG_DISCOVERY, Icons.Default.Science, tr("Drug Discovery", "દવાની શોધ", "ड्रग डिस्कवरी")),
        Triple(ClinicalAiTab.EPIDEMIC_PREDICTION, Icons.Default.Public, tr("Epidemic Alert", "મહામારી આગાહી", "महामारी अलर्ट")),
        Triple(ClinicalAiTab.DOCTOR_FINDER, Icons.Default.MedicalServices, tr("Find Doctor", "ડૉક્ટર બુકિંગ", "डॉक्टर बुकिंग")),
        Triple(ClinicalAiTab.TREATMENT_PLAN, Icons.Default.Biotech, tr("Treatment AI", "સારવાર AI", "उपचार AI")),
        Triple(ClinicalAiTab.PREDICTIVE_ANALYTICS, Icons.AutoMirrored.Filled.TrendingUp, tr("Risk Predict", "જોખમ આગાહી", "जोखिम अनुमान")),
        Triple(ClinicalAiTab.CLINICAL_SCRIBE, Icons.Default.Description, tr("Clinical NLP", "ક્લિનિકલ નોટ્સ", "क्लिनिकल NLP")),
        Triple(ClinicalAiTab.REMOTE_MONITORING, Icons.Default.Watch, tr("Virtual Ward", "વર્ચ્યુઅલ વોર્ડ", "वर्चुअल वार्ड"))
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                                    imageVector = Icons.Default.HealthAndSafety,
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
                                    "Clinical AI Suite",
                                    "ક્લિનિકલ AI સ્યુટ",
                                    "क्लिनिकल AI सुइट"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr(
                                    "Precision Care, Risk Prediction, NLP & Wearables",
                                    "વ્યક્તિગત સારવાર, જોખમ આગાહી, NLP & સ્માર્ટવોચ",
                                    "व्यक्तिगत उपचार, जोखिम पूर्वानुमान, NLP और वियरेबल्स"
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
                        modifier = Modifier.testTag("clinical_suite_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "પાછા જાઓ", "वापस जाएं")
                        )
                    }
                },
                actions = {
                    com.example.ui.components.LanguageSelector(viewModel = viewModel, showRefreshButton = false)
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
        ) {
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0),
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEach { (tab, icon, label) ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.setClinicalTab(tab) },
                        text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("clinical_tab_${tab.name.lowercase()}")
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    ClinicalAiTab.TREATMENT_PLAN -> PersonalizedTreatmentPane(viewModel = viewModel)
                    ClinicalAiTab.PREDICTIVE_ANALYTICS -> PredictiveAnalyticsPane(viewModel = viewModel)
                    ClinicalAiTab.CLINICAL_SCRIBE -> ClinicalDocumentationPane(viewModel = viewModel)
                    ClinicalAiTab.REMOTE_MONITORING -> RemoteMonitoringPane(viewModel = viewModel)
                    ClinicalAiTab.EHR_AUTOMATION -> EhrAutomationPane(viewModel = viewModel)
                    ClinicalAiTab.MENTAL_HEALTH -> MentalHealthPane(viewModel = viewModel)
                    ClinicalAiTab.DRUG_DISCOVERY -> DrugDiscoveryPane(viewModel = viewModel)
                    ClinicalAiTab.EPIDEMIC_PREDICTION -> EpidemicPredictionPane(viewModel = viewModel)
                    ClinicalAiTab.DOCTOR_FINDER -> DoctorFinderPane(viewModel = viewModel)
                }
            }
        }
    }
}

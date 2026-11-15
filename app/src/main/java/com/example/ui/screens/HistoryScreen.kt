package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.ThemeMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScannedReportEntity
import com.example.data.local.SymptomLogEntity
import com.example.data.local.TriageSessionEntity
import com.example.ui.components.ExportSummaryDialog
import com.example.ui.components.HeadacheHistoryListView
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsState()
    val symptomLogs by viewModel.symptomLogs.collectAsState()
    val headacheLogs by viewModel.headacheLogs.collectAsState()
    val scannedReports by viewModel.scannedReports.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Headaches, 1: All Logs, 2: Triage, 3: Reports
    var showClearDialog by remember { mutableStateOf(false) }
    var showAddSymptomDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var dialogInitialTitle by remember { mutableStateOf("") }
    var dialogInitialLocation by remember { mutableStateOf("Head") }
    var selectedReportDetail by remember { mutableStateOf<ScannedReportEntity?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val appLanguage = LocalAppLanguage.current

    if (showExportDialog) {
        ExportSummaryDialog(
            summaryText = viewModel.generateDoctorSummaryText(),
            onDownloadPdf = {
                val savedPath = viewModel.exportDoctorReportPdf(context)
                if (savedPath != null) {
                    android.widget.Toast.makeText(
                        context,
                        trLang(
                            appLanguage,
                            "Doctor report saved to $savedPath",
                            "ડૉક્ટર રિપોર્ટ સેવ થયો: $savedPath",
                            "डॉक्टर रिपोर्ट सहेजी गई: $savedPath"
                        ),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        context,
                        trLang(
                            appLanguage,
                            "Failed to save PDF",
                            "PDF સેવ કરવામાં નિષ્ફળ",
                            "PDF सहेजने में विफल"
                        ),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onSharePdf = {
                viewModel.shareDoctorReportPdf(context)
            },
            onDismiss = { showExportDialog = false }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(tr("Clear All Records?", "બધા રેકોર્ડ કાઢી નાખવા?", "सभी रिकॉर्ड हटाएं?"))
            },
            text = {
                Text(
                    tr(
                        "This will permanently remove all past symptoms, headache logs, and triage history from the local database on this device.",
                        "આનાથી તમારા ફોનમાં સાચવેલા બધા જૂના લક્ષણો, માથાના દુખાવાના લોગ અને તપાસનો ઇતિહાસ કાયમ માટે દૂર થઈ જશે.",
                        "इससे इस डिवाइस में सहेजे गए सभी पुराने लक्षण, सिरदर्द लॉग और जांच इतिहास स्थायी रूप से हट जाएंगे।"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text(tr("Clear All", "બધું સાફ કરો", "सब हटाएं"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showAddSymptomDialog) {
        AddSymptomLogDialog(
            initialTitle = dialogInitialTitle,
            initialLocation = dialogInitialLocation,
            onDismiss = { showAddSymptomDialog = false },
            onSave = { title, severity, location, duration, notes, status ->
                viewModel.logNewSymptom(title, severity, location, duration, notes, status)
                showAddSymptomDialog = false
            }
        )
    }

    selectedReportDetail?.let { report ->
        AlertDialog(
            onDismissRequest = { selectedReportDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(report.title, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = report.reportType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = report.aiExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tr("Raw OCR Text:", "સ્કેન કરેલ મૂળ લખાણ (OCR):", "स्कैन किया गया मूल पाठ (OCR):"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = report.extractedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { selectedReportDetail = null }) {
                    Text(stringResource(R.string.action_close))
                }
            }
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
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = tr("Health History", "આરોગ્ય ઇતિહાસ", "स्वास्थ्य इतिहास"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr(
                                    "Saved Health Records",
                                    "સાચવેલા આરોગ્ય રેકોર્ડ",
                                    "सहेजे गए स्वास्थ्य रिकॉर्ड"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    com.example.ui.components.LanguageSelector(
                        viewModel = viewModel,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(
                        onClick = { viewModel.toggleThemeMode() },
                        modifier = Modifier.testTag("history_theme_toggle_button")
                    ) {
                        val (themeIcon, themeName) = when (themeMode) {
                            ThemeMode.DARK -> Icons.Default.DarkMode to stringResource(R.string.theme_dark)
                            ThemeMode.LIGHT -> Icons.Default.LightMode to stringResource(R.string.theme_light)
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto to stringResource(R.string.theme_system)
                        }
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = themeName,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (sessions.isNotEmpty() || symptomLogs.isNotEmpty()) {
                        IconButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.testTag("export_for_doctor_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = tr("Export for Doctor", "ડૉક્ટર માટે રિપોર્ટ", "डॉक्टर के लिए निर्यात"),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = tr("Clear History", "ઇતિહાસ સાફ કરો", "इतिहास हटाएं"),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) {
                            dialogInitialTitle = "Headache (सिरदर्द)"
                            dialogInitialLocation = "Head"
                        } else {
                            dialogInitialTitle = ""
                            dialogInitialLocation = "General"
                        }
                        showAddSymptomDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_symptom_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (selectedTab == 0)
                                tr("Log Headache", "માથાનો દુખાવો ઉમેરો", "सिरदर्द जोड़ें")
                            else
                                tr("Log Symptom", "લક્ષણ ઉમેરો", "लक्षण जोड़ें"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Selector: Headache Monitor vs Symptom Diary vs Triage Sessions
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            tr(
                                "Headaches (${headacheLogs.size})",
                                "માથાનો દુખાવો (${headacheLogs.size})",
                                "सिरदर्द (${headacheLogs.size})"
                            ),
                            maxLines = 1
                        )
                    },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_headaches")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            tr(
                                "All Logs (${symptomLogs.size})",
                                "બધા લોગ (${symptomLogs.size})",
                                "सभी लॉग (${symptomLogs.size})"
                            ),
                            maxLines = 1
                        )
                    },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_symptom_diary")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            tr(
                                "Triage (${sessions.size})",
                                "તપાસ (${sessions.size})",
                                "जांच (${sessions.size})"
                            ),
                            maxLines = 1
                        )
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_triage_sessions")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            tr(
                                "Reports (${scannedReports.size})",
                                "રિપોર્ટ (${scannedReports.size})",
                                "रिपोर्ट (${scannedReports.size})"
                            ),
                            maxLines = 1
                        )
                    },
                    icon = { Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_scanned_reports")
                )
            }

            // AI Memory Indicator & Clinical Export Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = tr("AI Memory", "AI મેમરી", "AI मेमोरी"),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(
                                    "Smart Health Memory Active",
                                    "સ્માર્ટ આરોગ્ય મેમરી ચાલુ છે",
                                    "स्मार्ट स्वास्थ्य मेमोरी सक्रिय है"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("export_summary_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                tr("Export for Doctor", "ડૉક્ટર રિપોર્ટ", "डॉक्टर रिपोर्ट"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr(
                            "Health Care AI uses your saved records to personalize advice. Tap 'Export for Doctor' to download or share a summary for clinic visits.",
                            "એપ તમારા સાચવેલા રેકોર્ડના આધારે સચોટ સલાહ આપે છે. ક્લિનિકમાં ડૉક્ટરને બતાવવા 'ડૉક્ટર રિપોર્ટ' બટન દબાવો.",
                            "ऐप आपके सहेजे गए रिकॉर्ड के आधार पर सही सलाह देता है। डॉक्टर को दिखाने के लिए 'डॉक्टर रिपोर्ट' बटन दबाएं।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Headache Monitor Tab: Visual Line Chart at top + List View for Self-Monitoring
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (headacheLogs.isNotEmpty()) {
                            com.example.ui.components.SymptomSeverityLineChart(
                                symptomLogs = headacheLogs,
                                title = tr(
                                    "Headache Severity Trend (1-10 Scale)",
                                    "માથાના દુખાવાની તીવ્રતા (૧-૧૦)",
                                    "सिरदर्द की तीव्रता (1-10)"
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        HeadacheHistoryListView(
                            headacheLogs = headacheLogs,
                            onAddHeadacheClick = {
                                dialogInitialTitle = "Headache (सिरदर्द)"
                                dialogInitialLocation = "Head"
                                showAddSymptomDialog = true
                            },
                            onStatusChange = { item, newStatus ->
                                viewModel.updateSymptomStatus(item, newStatus)
                            },
                            onDelete = { item ->
                                viewModel.deleteSymptomLog(item)
                            },
                            onDiscussWithAi = { item ->
                                viewModel.sendMessage(
                                    overrideText = trLang(
                                        appLanguage,
                                        "I want to evaluate my recurring headache from my health history: ${item.symptomTitle} (Severity: ${item.severity}/10, Location: ${item.bodyLocation}, Duration: ${item.duration}).",
                                        "મારા આરોગ્ય ઇતિહાસમાં નોંધાયેલા વારંવાર થતા માથાના દુખાવા વિશે સમજાવો: ${item.symptomTitle} (તીવ્રતા: ${item.severity}/10, સ્થાન: ${item.bodyLocation}, સમય: ${item.duration}).",
                                        "मेरे स्वास्थ्य इतिहास में दर्ज बार-बार होने वाले सिरदर्द का मूल्यांकन करें: ${item.symptomTitle} (तीव्रता: ${item.severity}/10, स्थान: ${item.bodyLocation}, अवधि: ${item.duration})।"
                                    )
                                )
                                viewModel.setScreen(AppScreen.TRIAGE_CHAT)
                            },
                            onExportClick = { showExportDialog = true }
                        )
                    }
                }
                1 -> {
                    // All Symptoms Tab
                    if (symptomLogs.isEmpty()) {
                        EmptyHistoryView(
                            title = tr("No Symptoms Logged Yet", "હજુ સુધી કોઈ લક્ષણ નોંધાયેલ નથી", "अभी तक कोई लक्षण दर्ज नहीं है"),
                            description = tr(
                                "Log your symptoms (e.g. headaches, stomach discomfort, aches) or chat with Health Care AI to automatically record them.",
                                "તમારા લક્ષણો (જેમ કે માથાનો દુખાવો, પેટમાં દુખાવો) નોંધો અથવા AI સાથે વાત કરો જેથી રેકોર્ડ આપોઆપ સચવાય.",
                                "अपने लक्षण (जैसे सिरदर्द, पेट दर्द) दर्ज करें या AI से बात करें ताकि वे अपने आप सहेजे जाएं।"
                            ),
                            buttonText = tr("Log First Symptom", "પ્રથમ લક્ષણ નોંધો", "पहला लक्षण दर्ज करें"),
                            onButtonClick = {
                                dialogInitialTitle = ""
                                dialogInitialLocation = "General"
                                showAddSymptomDialog = true
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Graphical Progress Chart for all symptoms
                            item {
                                com.example.ui.components.SymptomSeverityLineChart(
                                    symptomLogs = symptomLogs,
                                    title = tr(
                                        "Overall Symptom Severity Trend",
                                        "લક્ષણોની તીવ્રતાનો ગ્રાફ",
                                        "लक्षणों की तीव्रता का ग्राफ"
                                    ),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            items(symptomLogs, key = { it.id }) { log ->
                                SymptomLogCard(
                                    log = log,
                                    onStatusChange = { newStatus ->
                                        viewModel.updateSymptomStatus(log, newStatus)
                                    },
                                    onDelete = { viewModel.deleteSymptomLog(log) },
                                    onDiscuss = {
                                        viewModel.sendMessage(
                                            overrideText = trLang(
                                                appLanguage,
                                                "I want to follow up on my reported symptom: ${log.symptomTitle} (Severity: ${log.severity}/10, Location: ${log.bodyLocation}).",
                                                "મારા નોંધાયેલા લક્ષણ વિશે વધુ સલાહ આપો: ${log.symptomTitle} (તીવ્રતા: ${log.severity}/10, સ્થાન: ${log.bodyLocation}).",
                                                "मेरे दर्ज किए गए लक्षण पर आगे की सलाह दें: ${log.symptomTitle} (तीव्रता: ${log.severity}/10, स्थान: ${log.bodyLocation})।"
                                            )
                                        )
                                        viewModel.setScreen(AppScreen.TRIAGE_CHAT)
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
                2 -> {
                    // Triage Sessions Tab
                    if (sessions.isEmpty()) {
                        EmptyHistoryView(
                            title = tr("No Triage Consultations Yet", "હજુ સુધી કોઈ તપાસ ઇતિહાસ નથી", "अभी तक कोई जांच इतिहास नहीं है"),
                            description = tr(
                                "Start a consultation with Health Care AI to evaluate symptoms and view recommendations here.",
                                "લક્ષણોની તપાસ કરવા અને અહીં સલાહ જોવા માટે AI સાથે વાતચીત શરૂ કરો.",
                                "लक्षणों की जांच करने और यहां सलाह देखने के लिए AI के साथ परामर्श शुरू करें।"
                            ),
                            buttonText = tr("Start Health Assessment", "તપાસ શરૂ કરો", "जांच शुरू करें"),
                            onButtonClick = { viewModel.setScreen(AppScreen.TRIAGE_CHAT) }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(sessions, key = { it.id }) { session ->
                                TriageSessionItemCard(
                                    session = session,
                                    onDelete = { viewModel.deleteSession(session) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
                3 -> {
                    // Scanned Medical Reports & Prescriptions Tab
                    if (scannedReports.isEmpty()) {
                        EmptyHistoryView(
                            title = tr("No Scanned Reports Yet", "હજુ સુધી કોઈ સ્કેન કરેલ રિપોર્ટ નથી", "अभी तक कोई स्कैन की गई रिपोर्ट नहीं है"),
                            description = tr(
                                "Upload PDF lab reports or scan prescriptions & medicine strips to get simple AI explanations.",
                                "PDF લેબ રિપોર્ટ અપલોડ કરો અથવા પ્રિસ્ક્રિપ્શન અને દવાના પત્તા સ્કેન કરીને AI પાસેથી સરળ સમજૂતી મેળવો.",
                                "PDF लैब रिपोर्ट अपलोड करें या पर्चा और दवाई के पत्ते स्कैन करके सरल AI व्याख्या प्राप्त करें।"
                            ),
                            buttonText = tr("Open Report Scanner", "રિપોર્ટ સ્કેનર ખોલો", "रिपोर्ट स्कैनर खोलें"),
                            onButtonClick = { viewModel.setScreen(AppScreen.REPORT_SCANNER) }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.setScreen(AppScreen.REPORT_SCANNER) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        tr(
                                            "Scan New Report or Prescription",
                                            "નવો રિપોર્ટ કે પ્રિસ્ક્રિપ્શન સ્કેન કરો",
                                            "नई रिपोर्ट या पर्चा स्कैन करें"
                                        ),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            items(scannedReports, key = { it.id }) { report ->
                                ScannedReportCard(
                                    report = report,
                                    onClick = { selectedReportDetail = report },
                                    onDelete = { viewModel.deleteScannedReport(report) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SymptomLogCard(
    log: SymptomLogEntity,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit,
    onDiscuss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(log.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().testTag("symptom_card_${log.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusDisplay = when (log.status) {
                        "Resolved" -> tr("Resolved", "સારું થઈ ગયું", "ठीक हो गया")
                        "Improving" -> tr("Improving", "સુધારો છે", "सुधार हो रहा है")
                        else -> tr("Ongoing", "ચાલુ છે", "जारी है")
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (log.status) {
                            "Resolved" -> SafeMint.copy(alpha = 0.15f)
                            "Improving" -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    ) {
                        Text(
                            text = statusDisplay,
                            color = when (log.status) {
                                "Resolved" -> SafeMint
                                "Improving" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onTertiaryContainer
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).padding(start = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = tr("Delete", "કાઢી નાખો", "हटाएं"),
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.symptomTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        log.severity >= 8 -> EmergencyRed
                        log.severity >= 5 -> MaterialTheme.colorScheme.tertiary
                        else -> SafeMint
                    }
                ) {
                    Text(
                        text = tr(
                            "Severity: ${log.severity}/10",
                            "તીવ્રતા: ${log.severity}/10",
                            "तीव्रता: ${log.severity}/10"
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "📍 ${log.bodyLocation}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "⏱️ ${log.duration}",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (log.notesOrTriggers.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tr(
                        "Notes / Triggers: ${log.notesOrTriggers}",
                        "નોંધ / કારણો: ${log.notesOrTriggers}",
                        "नोट्स / कारण: ${log.notesOrTriggers}"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Ongoing", "Improving", "Resolved").forEach { statusOption ->
                        if (statusOption != log.status) {
                            val optionLabel = when (statusOption) {
                                "Resolved" -> tr("Resolved", "સારું થયું", "ठीक हुआ")
                                "Improving" -> tr("Improving", "સુધારો", "सुधार")
                                else -> tr("Ongoing", "ચાલુ", "जारी")
                            }
                            TextButton(
                                onClick = { onStatusChange(statusOption) },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    optionLabel,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDiscuss,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        tr("Ask AI", "AI ને પૂછો", "AI से पूछें"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSymptomLogDialog(
    initialTitle: String = "",
    initialLocation: String = "Head",
    onDismiss: () -> Unit,
    onSave: (String, Int, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var severity by remember { mutableFloatStateOf(5f) }
    var location by remember { mutableStateOf(initialLocation) }
    var duration by remember { mutableStateOf("1-2 Days") }
    var notes by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ongoing") }

    val locationOptions = listOf("Head", "Throat", "Chest", "Stomach")
    val durationOptions = listOf("Hours", "1-2 Days", "3-7 Days")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("Log Health Symptom", "આરોગ્ય લક્ષણ નોંધો", "स्वास्थ्य लक्षण दर्ज करें"))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text(
                            tr(
                                "Symptom Name (e.g. Headache, Sore throat)",
                                "લક્ષણનું નામ (દા.ત. માથાનો દુખાવો, ગળામાં દુખાવો)",
                                "लक्षण का नाम (जैसे सिरदर्द, गले में दर्द)"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_symptom_title_field")
                )

                Text(
                    text = tr(
                        "Severity (1 - 10): ${severity.toInt()} / 10",
                        "તીવ્રતા (૧ - ૧૦): ${severity.toInt()} / 10",
                        "तीव्रता (1 - 10): ${severity.toInt()} / 10"
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (severity >= 8) EmergencyRed else MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = severity,
                    onValueChange = { severity = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = if (severity >= 8) EmergencyRed else PrimaryTeal,
                        activeTrackColor = if (severity >= 8) EmergencyRed else PrimaryTeal
                    )
                )

                Text(
                    tr("Body Location:", "શરીરનો ભાગ:", "शरीर का स्थान:"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    locationOptions.forEach { loc ->
                        val locLabel = when (loc) {
                            "Head" -> tr("Head", "માથું", "सिर")
                            "Throat" -> tr("Throat", "ગળું", "गला")
                            "Chest" -> tr("Chest", "છાતી", "छाती")
                            "Stomach" -> tr("Stomach", "પેટ", "पेट")
                            else -> loc
                        }
                        FilterChip(
                            selected = location == loc,
                            onClick = { location = loc },
                            label = { Text(locLabel, fontSize = 11.sp) }
                        )
                    }
                }

                Text(
                    tr("Duration:", "સમયગાળો:", "अवधि:"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    durationOptions.forEach { dur ->
                        val durLabel = when (dur) {
                            "Hours" -> tr("Hours", "કલાકો", "कुछ घंटे")
                            "1-2 Days" -> tr("1-2 Days", "૧-૨ દિવસ", "1-2 दिन")
                            "3-7 Days" -> tr("3-7 Days", "૩-૭ દિવસ", "3-7 दिन")
                            else -> dur
                        }
                        FilterChip(
                            selected = duration == dur,
                            onClick = { duration = dur },
                            label = { Text(durLabel, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = {
                        Text(
                            tr(
                                "Notes / Triggers (optional)",
                                "નોંધ / કારણો (વૈકલ્પિક)",
                                "नोट्स / कारण (वैकल्पिक)"
                            )
                        )
                    },
                    placeholder = {
                        Text(
                            tr(
                                "e.g. Occurred after screen time or stress",
                                "દા.ત. મોબાઇલ વધુ વાપરવાથી કે તણાવથી થયું",
                                "जैसे स्क्रीन देखने या तनाव के बाद हुआ"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, severity.toInt(), location, duration, notes, status)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_dialog_symptom_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun EmptyHistoryView(
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onButtonClick,
            modifier = Modifier.testTag("empty_history_action_button")
        ) {
            Text(buttonText)
        }
    }
}

@Composable
fun TriageSessionItemCard(
    session: TriageSessionEntity,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(session.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (session.emergencyFlagged) EmergencyRedContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().testTag("session_card_${session.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = tr("Delete", "કાઢી નાખો", "हटाएं"),
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = session.primarySymptom,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        session.severity >= 8 || session.emergencyFlagged -> EmergencyRed
                        session.severity >= 5 -> MaterialTheme.colorScheme.tertiary
                        else -> SafeMint
                    }
                ) {
                    Text(
                        text = tr(
                            "Severity: ${session.severity}/10",
                            "તીવ્રતા: ${session.severity}/10",
                            "तीव्रता: ${session.severity}/10"
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = session.recommendedSpecialist,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (session.emergencyFlagged) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = EmergencyRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr(
                            "Emergency red flags noted in triage",
                            "તપાસમાં ગંભીર ઇમરજન્સી ચિહ્નો નોંધાયા છે",
                            "जांच में गंभीर आपातकालीन संकेत पाए गए हैं"
                        ),
                        color = EmergencyRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

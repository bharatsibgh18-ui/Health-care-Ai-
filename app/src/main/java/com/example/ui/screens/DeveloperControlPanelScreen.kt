package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PublicDoctorEntity
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperControlPanelScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authSession by viewModel.authSession.collectAsState()
    val allPublicDoctors by viewModel.allPublicDoctors.collectAsState()
    val devAppConfig by viewModel.devAppConfig.collectAsState()
    val userPresenceList by viewModel.developerUserPresenceList.collectAsState()
    val devUpgradeStatus by viewModel.devUpgradeStatus.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val devScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.refreshDeveloperCloudConsole()
    }

    LaunchedEffect(devUpgradeStatus) {
        if (!devUpgradeStatus.isNullOrBlank()) {
            Toast.makeText(context, devUpgradeStatus, Toast.LENGTH_SHORT).show()
            viewModel.clearDevUpgradeStatus()
        }
    }

    // Doctor form state
    var editingDoctorId by remember { mutableStateOf(0L) }
    var editingCloudId by remember { mutableStateOf("") }
    var doctorName by remember { mutableStateOf("") }
    var doctorSpecialty by remember { mutableStateOf("General Physician") }
    var doctorClinic by remember { mutableStateOf("") }
    var doctorMobile by remember { mutableStateOf("") }
    var doctorFee by remember { mutableStateOf("₹300") }

    // App config state synced with persisted config
    var bannerNotice by remember(devAppConfig.configVersion) { mutableStateOf(devAppConfig.appBannerNotice) }
    var publicDocsEnabled by remember(devAppConfig.configVersion) { mutableStateOf(devAppConfig.publicDoctorConsultEnabled) }
    var videoCallEnabled by remember(devAppConfig.configVersion) { mutableStateOf(devAppConfig.videoCallFeatureEnabled) }
    var medicineAdvisorEnabled by remember(devAppConfig.configVersion) { mutableStateOf(devAppConfig.medicineAdvisorFeatureEnabled) }
    var emergencyHelpline by remember(devAppConfig.configVersion) { mutableStateOf(devAppConfig.emergencyHelplineOverride) }

    val timeFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

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
                                    imageVector = Icons.Default.AdminPanelSettings,
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
                                    "Developer Control & Upgrades",
                                    "ડેવલપર કંટ્રોલ અને અપગ્રેડ",
                                    "डेवलपर कंट्रोल और अपग्रेड"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Logged in: ${authSession.username} • v${devAppConfig.configVersion}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.TRIAGE_CHAT) },
                        modifier = Modifier.testTag("dev_console_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "પાછા", "वापस")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshDeveloperCloudConsole() },
                        modifier = Modifier.testTag("dev_console_refresh_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Cloud")
                    }
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.DEVELOPER_LOGS) },
                        modifier = Modifier.testTag("dev_console_open_logs_btn")
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = "Developer Logs")
                    }
                    IconButton(
                        onClick = { viewModel.logoutAccount() },
                        modifier = Modifier.testTag("dev_console_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = EmergencyRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            tr("Public Doctors", "પબ્લિક ડૉક્ટર્સ", "पब्लिक डॉक्टर"),
                            maxLines = 1,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("dev_console_tab_doctors")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            tr("App Upgrades", "એપ અપગ્રેડ", "ऐप अपग्रेड"),
                            maxLines = 1,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("dev_console_tab_upgrades")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            tr("Users (${userPresenceList.size})", "યુઝર્સ (${userPresenceList.size})", "यूज़र्स (${userPresenceList.size})"),
                            maxLines = 1,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("dev_console_tab_users")
                )
            }

            when (selectedTab) {
                // ---------------------------------------------------------
                // TAB 0: PUBLIC DOCTORS + MOBILE NUMBER + VIDEO CALL CONFIG
                // ---------------------------------------------------------
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = if (editingDoctorId == 0L) {
                                            tr(
                                                "Add Public Doctor (Mobile Number & Booking)",
                                                "નવા પબ્લિક ડૉક્ટર ઉમેરો (મોબાઇલ નંબર અને બુકિંગ)",
                                                "नए पब्लिक डॉक्टर जोड़ें (मोबाइल नंबर और बुकिंग)"
                                            )
                                        } else {
                                            tr("Edit Public Doctor", "પબ્લિક ડૉક્ટર સુધારો", "पब्लिक डॉक्टर संपादित करें")
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    OutlinedTextField(
                                        value = doctorName,
                                        onValueChange = { doctorName = it },
                                        label = { Text(tr("Doctor Full Name *", "ડૉક્ટરનું પૂરું નામ *", "डॉक्टर का पूरा नाम *")) },
                                        placeholder = { Text("Dr. K. Jadeja (MD)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("dev_doc_name_input")
                                    )

                                    OutlinedTextField(
                                        value = doctorSpecialty,
                                        onValueChange = { doctorSpecialty = it },
                                        label = { Text(tr("Specialty", "નિષ્ણાત વિભાગ", "विशेषज्ञता")) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("dev_doc_specialty_input")
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            "General Physician",
                                            "Cardiologist",
                                            "Neurologist",
                                            "Pediatrician",
                                            "Dermatologist",
                                            "Orthopedic"
                                        ).forEach { spec ->
                                            SuggestionChip(
                                                onClick = { doctorSpecialty = spec },
                                                label = { Text(spec, fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = doctorMobile,
                                            onValueChange = { doctorMobile = it },
                                            label = { Text(tr("Mobile No *", "મોબાઇલ નંબર *", "मोबाइल नंबर *")) },
                                            placeholder = { Text("9876543210") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            singleLine = true,
                                            modifier = Modifier.weight(1.3f).testTag("dev_doc_mobile_input")
                                        )
                                        OutlinedTextField(
                                            value = doctorFee,
                                            onValueChange = { doctorFee = it },
                                            label = { Text(tr("Fee", "ફી", "शुल्क")) },
                                            singleLine = true,
                                            modifier = Modifier.weight(0.8f).testTag("dev_doc_fee_input")
                                        )
                                    }

                                    OutlinedTextField(
                                        value = doctorClinic,
                                        onValueChange = { doctorClinic = it },
                                        label = { Text(tr("Clinic / Hospital & City", "દવાખાનું / શહેર", "क्लिनिक / शहर")) },
                                        placeholder = { Text("Sayla Civil Care Clinic, Surendranagar") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("dev_doc_clinic_input")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (editingDoctorId != 0L) {
                                            OutlinedButton(
                                                onClick = {
                                                    editingDoctorId = 0L
                                                    editingCloudId = ""
                                                    doctorName = ""
                                                    doctorClinic = ""
                                                    doctorMobile = ""
                                                },
                                                modifier = Modifier.weight(1f).height(48.dp)
                                            ) {
                                                Text(tr("Cancel", "રદ", "रद्द"))
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                val ok = viewModel.savePublicDoctor(
                                                    doctorId = editingDoctorId,
                                                    cloudId = editingCloudId,
                                                    doctorName = doctorName,
                                                    specialty = doctorSpecialty,
                                                    clinicOrCity = doctorClinic,
                                                    mobileNumber = doctorMobile,
                                                    consultationFee = doctorFee,
                                                    videoCallEnabled = false,
                                                    videoCallRoomUrl = "",
                                                    isPublicActive = true
                                                )
                                                if (ok) {
                                                    editingDoctorId = 0L
                                                    editingCloudId = ""
                                                    doctorName = ""
                                                    doctorClinic = ""
                                                    doctorMobile = ""
                                                }
                                            },
                                            modifier = Modifier.weight(1.6f).height(48.dp).testTag("dev_doc_save_btn")
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                if (editingDoctorId == 0L) {
                                                    tr("Add Public Doctor", "પબ્લિક ડૉક્ટર ઉમેરો", "पब्लिक डॉक्टर जोड़ें")
                                                } else {
                                                    tr("Update Doctor", "સુધારો સેવ કરો", "अपडेट सहेजें")
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = tr(
                                    "Published Public Doctors (${allPublicDoctors.size})",
                                    "ઉમેરેલા પબ્લિક ડૉક્ટર્સ (${allPublicDoctors.size})",
                                    "जारी किए गए पब्लिक डॉक्टर (${allPublicDoctors.size})"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (allPublicDoctors.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = tr(
                                            "No custom public doctors added yet. Add a doctor above with their mobile number for direct calling & appointment booking.",
                                            "હજુ સુધી કોઈ પબ્લિક ડૉક્ટર ઉમેર્યા નથી. ઉપરથી નવા ડૉક્ટર અને તેમનો મોબાઇલ નંબર ઉમેરો.",
                                            "अभी कोई पब्लिक डॉक्टर नहीं जोड़ा गया है। ऊपर से डॉक्टर और मोबाइल नंबर जोड़ें।"
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(allPublicDoctors, key = { it.cloudId }) { doc ->
                                PublicDoctorAdminCard(
                                    doctor = doc,
                                    onToggleActive = { viewModel.togglePublicDoctorActive(doc) },
                                    onCall = {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${doc.mobileNumber}")))
                                        } catch (_: Exception) {}
                                    },
                                    onEdit = {
                                        editingDoctorId = doc.id
                                        editingCloudId = doc.cloudId
                                        doctorName = doc.doctorName
                                        doctorSpecialty = doc.specialty
                                        doctorClinic = doc.clinicOrCity
                                        doctorMobile = doc.mobileNumber
                                        doctorFee = doc.consultationFee
                                    },
                                    onDelete = { viewModel.deletePublicDoctor(doc) }
                                )
                            }
                        }
                    }
                }

                // ---------------------------------------------------------
                // TAB 1: LIVE MANUAL APP UPGRADES & FEATURE TOGGLES
                // ---------------------------------------------------------
                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = tr(
                                            "Manual Live App Upgrades (Push to All Users)",
                                            "લાઇવ એપ અપગ્રેડ અને મેન્યુઅલ ફેરફારો",
                                            "लाइव ऐप अपग्रेड और मैनुअल बदलाव"
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    OutlinedTextField(
                                        value = bannerNotice,
                                        onValueChange = { bannerNotice = it },
                                        label = {
                                            Text(
                                                tr(
                                                    "Public Announcement / App Upgrade Banner",
                                                    "દર્દીઓ માટે લાઇવ સૂચના / અપગ્રેડ મેસેજ",
                                                    "मरीजों के लिए लाइव सूचना / अपग्रेड संदेश"
                                                )
                                            )
                                        },
                                        placeholder = { Text("New evening tele-consultation camp open!") },
                                        modifier = Modifier.fillMaxWidth().testTag("dev_cfg_banner_input")
                                    )

                                    OutlinedTextField(
                                        value = emergencyHelpline,
                                        onValueChange = { emergencyHelpline = it },
                                        label = { Text(tr("Emergency Helpline Number", "ઇમરજન્સી હેલ્પલાઇન નંબર", "आपातकालीन हेल्पलाइन नंबर")) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("dev_cfg_helpline_input")
                                    )

                                    FeatureToggleRow(
                                        title = tr("Public Doctor Directory & Booking", "પબ્લિક ડૉક્ટર ડાયરેક્ટરી", "पब्लिक डॉक्टर डायरेक्टरी"),
                                        checked = publicDocsEnabled,
                                        onCheckedChange = { publicDocsEnabled = it },
                                        tag = "dev_cfg_public_docs_switch"
                                    )
                                    FeatureToggleRow(
                                        title = tr("Live AI Video Consultation", "લાઇવ AI વિડિયો કન્સલ્ટેશન", "लाइव AI वीडियो परामर्श"),
                                        checked = videoCallEnabled,
                                        onCheckedChange = { videoCallEnabled = it },
                                        tag = "dev_cfg_video_call_switch"
                                    )
                                    FeatureToggleRow(
                                        title = tr("AI Symptom & Medicine Advisor", "AI દવા અને ગંભીરતા સલાહકાર", "AI दवा और गंभीरता सलाहकार"),
                                        checked = medicineAdvisorEnabled,
                                        onCheckedChange = { medicineAdvisorEnabled = it },
                                        tag = "dev_cfg_med_advisor_switch"
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.saveDeveloperAppConfig(
                                                appBannerNotice = bannerNotice,
                                                publicDoctorConsultEnabled = publicDocsEnabled,
                                                videoCallFeatureEnabled = videoCallEnabled,
                                                medicineAdvisorFeatureEnabled = medicineAdvisorEnabled,
                                                emergencyHelplineOverride = emergencyHelpline
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dev_cfg_save_btn")
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            tr("Publish Live App Upgrade", "અપગ્રેડ લાઇવ પબ્લિશ કરો", "लाइव अपग्रेड पब्लिश करें"),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.setScreen(AppScreen.DEVELOPER_LOGS) },
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dev_open_logs_btn")
                                    ) {
                                        Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tr("Open System Developer Logs", "સિસ્ટમ ડેવલપર લોગ્સ ખોલો", "सिस्टम डेवलपर लॉग्स खोलें"))
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            devScope.launch {
                                                com.example.utils.ApkShareHelper.shareInstalledApk(context, selectedLanguage)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dev_share_apk_btn")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            tr(
                                                "Share Upgraded HealthCareAI_v${com.example.BuildConfig.VERSION_NAME}.apk",
                                                "અપગ્રેડેડ HealthCareAI_v${com.example.BuildConfig.VERSION_NAME}.apk શેર કરો",
                                                "अपग्रेडेड HealthCareAI_v${com.example.BuildConfig.VERSION_NAME}.apk शेयर करें"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ---------------------------------------------------------
                // TAB 2: PRIVACY-PRESERVING USER USERNAMES & ONLINE TIME
                // ---------------------------------------------------------
                else -> {
                    val onlineCount = userPresenceList.count { it.isCurrentlyOnline }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SafeMint.copy(alpha = 0.14f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tr(
                                                "Privacy-Protected Activity View (${userPresenceList.size} Users • $onlineCount Online)",
                                                "સુરક્ષિત યુઝર એક્ટિવિટી (${userPresenceList.size} યુઝર્સ • $onlineCount ઓનલાઇન)",
                                                "गोपनीयता-सुरक्षित गतिविधि (${userPresenceList.size} यूज़र्स • $onlineCount ऑनलाइन)"
                                            ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = tr(
                                                "Shows ONLY username and online duration/last active time. Passwords, chats & personal medical logs are never shown.",
                                                "અહીં માત્ર યુઝરનું નામ (Username) અને ઓનલાઇન રહેવાનો સમય જ દેખાય છે. પાસવર્ડ કે અંગત આરોગ્ય ડેટા દેખાતો નથી.",
                                                "यहाँ केवल यूज़रनेम और ऑनलाइन समय दिखाई देता है। पासवर्ड या निजी मेडिकल डेटा नहीं दिखता।"
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (userPresenceList.isEmpty()) {
                            item {
                                Text(
                                    text = tr(
                                        "No signed-in users recorded yet.",
                                        "હજુ સુધી કોઈ યુઝર નોંધાયેલ નથી.",
                                        "अभी तक कोई उपयोगकर्ता दर्ज नहीं है।"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        } else {
                            items(userPresenceList, key = { it.username }) { user ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("dev_user_presence_${user.username}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (user.isCurrentlyOnline) SafeMint else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(12.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "@${user.username}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = tr(
                                                    "Online Time: ${formatOnlineDuration(user.totalOnlineSeconds)} • ${user.authMethod}",
                                                    "ઓનલાઇન સમય: ${formatOnlineDuration(user.totalOnlineSeconds)} • ${user.authMethod}",
                                                    "ऑनलाइन समय: ${formatOnlineDuration(user.totalOnlineSeconds)} • ${user.authMethod}"
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = tr(
                                                    "Last Online: ${timeFmt.format(Date(user.lastOnlineAt))}",
                                                    "છેલ્લે ઓનલાઇન: ${timeFmt.format(Date(user.lastOnlineAt))}",
                                                    "अंतिम ऑनलाइन: ${timeFmt.format(Date(user.lastOnlineAt))}"
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (user.isCurrentlyOnline) {
                                                SafeMint.copy(alpha = 0.2f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ) {
                                            Text(
                                                text = if (user.isCurrentlyOnline) "ONLINE" else "OFFLINE",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (user.isCurrentlyOnline) SafeMint else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun PublicDoctorAdminCard(
    doctor: PublicDoctorEntity,
    onToggleActive: () -> Unit,
    onCall: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth().testTag("dev_public_doc_card_${doctor.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(doctor.doctorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${doctor.specialty} • ${doctor.clinicOrCity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "📞 ${doctor.mobileNumber} • ${doctor.consultationFee}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Switch(
                    checked = doctor.isPublicActive,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.testTag("dev_doc_toggle_${doctor.id}")
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onCall,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Call", "કૉલ", "कॉल"), fontSize = 12.sp)
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(48.dp).testTag("dev_doc_edit_${doctor.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Doctor", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp).testTag("dev_doc_delete_${doctor.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Doctor", tint = EmergencyRed)
                }
            }
        }
    }
}

fun formatOnlineDuration(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val hrs = safe / 3600L
    val mins = (safe % 3600L) / 60L
    val secs = safe % 60L
    return when {
        hrs > 0 -> "${hrs}h ${mins}m"
        mins > 0 -> "${mins}m ${secs}s"
        else -> "${secs}s"
    }
}

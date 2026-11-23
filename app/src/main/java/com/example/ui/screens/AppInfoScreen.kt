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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.ThemeMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.utils.ApkShareHelper
import kotlinx.coroutines.launch
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.tr
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tr("App Info", "એપ માહિતી", "ऐप जानकारी"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.TRIAGE_CHAT) },
                        modifier = Modifier.testTag("app_info_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back", "પાછા જાઓ", "वापस जाएं")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleThemeMode() },
                        modifier = Modifier.testTag("app_info_theme_toggle_button")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Hero Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_info_hero_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = "Health Care AI",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = tr("Health Care AI", "હેલ્થ કેર AI", "हेल्थ केयर AI"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = tr(
                            "Health Care AI • Version ${com.example.BuildConfig.VERSION_NAME}",
                            "હેલ્થ કેર એઆઈ • વર્ઝન ${com.example.BuildConfig.VERSION_NAME}",
                            "हेल्थ केयर AI • वर्ज़न ${com.example.BuildConfig.VERSION_NAME}"
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = tr(
                            "Your Personal AI Health Companion",
                            "તમારો પર્સનલ AI આરોગ્ય સહાયક",
                            "आपका व्यक्तिगत AI स्वास्थ्य सहायक"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Organization / School Card (કંપની / મોડેલ સ્કૂલ સાયલા)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_info_school_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Model School Sayla",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = tr(
                                    "Institution / Inspiring School",
                                    "સંસ્થા / પ્રેરક સ્કૂલ",
                                    "संस्था / प्रेरक स्कूल"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Model School Sayla",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr("Model School Sayla", "મોડેલ સ્કૂલ સાયલા", "मॉडल स्कूल सायला"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = tr("Location", "સ્થળ", "स्थान"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(
                                "Sayla, District: Surendranagar, Gujarat",
                                "સાયલા, જિલ્લો: સુરેન્દ્રનગર, ગુજરાત",
                                "सायला, जिला: सुरेंद्रनगर, गुजरात"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = tr(
                            "This health application was developed under the educational and health awareness initiative of Model School Sayla to provide accessible health guidance to students and the community.",
                            "આ આરોગ્ય એપ્લિકેશન મોડેલ સ્કૂલ સાયલાના શૈક્ષણિક અને આરોગ્ય જાગૃતિ અભિયાન અંતર્ગત વિદ્યાર્થીઓ અને સમાજના લોકોના સ્વાસ્થ્ય માર્ગદર્શન માટે તૈયાર કરવામાં આવી છે.",
                            "यह स्वास्थ्य एप्लिकेशन मॉडल स्कूल सायला के शैक्षणिक और स्वास्थ्य जागरूकता अभियान के अंतर्गत छात्रों और समुदाय के स्वास्थ्य मार्गदर्शन के लिए तैयार किया गया है।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // Creator / Developer Card (નિર્માતા: શૈલેષ સતિયા)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_info_creator_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Shailesh Satiya",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = tr(
                                    "Creator / Application Developer",
                                    "નિર્માતા / એપ્લિકેશન ડેવલપર",
                                    "निर्माता / एप्लिकेशन डेवलपर"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Shailesh Satiya",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr("Shailesh Satiya", "શૈલેષ સતિયા", "शैलेश सतिया"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Text(
                        text = tr(
                            "This 'Health Care AI' application was designed and built by Shailesh Satiya, combining Google Gemini AI and Android technology so people can easily get guidance and home remedies in English, Gujarati, and Hindi.",
                            "શૈલેષ સતિયા દ્વારા આ 'Health Care AI' એપ્લિકેશનનું સંપૂર્ણ નિર્માણ અને પ્રોગ્રામિંગ કરવામાં આવ્યું છે. આર્ટિફિશિયલ ઇન્ટેલિજન્સ (Google Gemini AI) અને એન્ડ્રોઇડ ટેક્નોલોજીના સમન્વયથી લોકો પોતાની ભાષામાં તમામ નાની-મોટી બિમારીઓનો સચોટ હલ અને ઘરેલુ ઉપાય સરળતાથી જાણી શકે તેવા ઉમદા હેતુથી આ એપ તૈયાર કરી છે.",
                            "शैलेश सतिया द्वारा इस 'Health Care AI' एप्लिकेशन का निर्माण और प्रोग्रामिंग की गई है। Google Gemini AI और एंड्रॉइड तकनीक के समन्वय से लोग अपनी भाषा में सामान्य बीमारियों की जानकारी और घरेलू उपाय आसानी से जान सकें, इसी उद्देश्य से यह ऐप बनाया गया है।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.setScreen(AppScreen.DEVELOPER_LOGS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_info_open_dev_logs_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            tr(
                                "Open Developer Logs Console",
                                "ડેવલપર લોગ્સ કન્સોલ ખોલો",
                                "डेवलपर लॉग्स कंसोल खोलें"
                            )
                        )
                    }
                }
            }

            // Key Highlights & Technologies
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = tr(
                            "Key Features",
                            "એપ્લિકેશનની મુખ્ય વિશેષતાઓ",
                            "एप्लिकेशन की मुख्य विशेषताएं"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FeatureBullet(
                        icon = Icons.Default.HealthAndSafety,
                        title = tr(
                            "Google Gemini AI Support",
                            "Google Gemini AI સપોર્ટ",
                            "Google Gemini AI सहायता"
                        ),
                        description = tr(
                            "Smart health guidance and safe home care tips for fever, cold, stomach pain, joint pain, diabetes, BP, and lab reports.",
                            "તમામ નાની-મોટી બિમારીઓ (તાવ, શરદી, પેટનો દુખાવો, ઝાડા-ઉલટી, સાંધાનો દુખાવો, ડાયાબિટીસ, બીપી વગેરે) અને લેબ રિપોર્ટ માટે સચોટ માર્ગદર્શન.",
                            "बुखार, सर्दी, पेट दर्द, जोड़ों का दर्द, डायबिटीज़, बीपी और लैब रिपोर्ट के लिए सटीक स्वास्थ्य मार्गदर्शन और घरेलू देखभाल।"
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FeatureBullet(
                        icon = Icons.Default.RecordVoiceOver,
                        title = tr(
                            "Voice Explanation",
                            "બોલીને સમજાવવાની સુવિધા",
                            "बोलकर समझाने की सुविधा"
                        ),
                        description = tr(
                            "Listen to medical guidance aloud in clear English, Gujarati, or Hindi instead of reading long text.",
                            "જવાબ વાંચવાને બદલે શુદ્ધ ગુજરાતી, હિન્દી અને અંગ્રેજી ભાષામાં અવાજ દ્વારા સાંભળી શકો છો.",
                            "जवाब पढ़ने के बजाय स्पष्ट हिंदी, गुजराती और अंग्रेजी में आवाज़ के माध्यम से सुन सकते हैं।"
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FeatureBullet(
                        icon = Icons.Default.WaterDrop,
                        title = tr(
                            "Water & Medicine Reminders",
                            "વોટર અને દવા રિમાઇન્ડર",
                            "पानी और दवा रिमाइंडर"
                        ),
                        description = tr(
                            "Track daily hydration, medicine schedules, vitals, and daily walking steps in one place.",
                            "રોજિંદા પૂરતા પાણીનું ટ્રેકિંગ, દવાના રિમાઇન્ડર અને શરીરને હાઇડ્રેટેડ રાખવા માટે સ્માર્ટ સુવિધા.",
                            "दैनिक पानी, दवाइयों के समय, वाइटल्स और कदमों की गिनती को एक साथ ट्रैक करें।"
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FeatureBullet(
                        icon = Icons.Default.Security,
                        title = tr(
                            "Local Privacy & Security",
                            "સુરક્ષિત લોકલ ડેટાબેઝ",
                            "सुरक्षित स्थानीय डेटाबेस"
                        ),
                        description = tr(
                            "Your health history and personal profile stay safely stored locally on your device.",
                            "તમારો આરોગ્ય ઇતિહાસ અને પ્રોફાઇલ ડેટા સંપૂર્ણ સુરક્ષિત રીતે તમારા ફોનમાં જ સચવાય છે.",
                            "आपका स्वास्थ्य इतिहास और प्रोफ़ाइल डेटा पूरी तरह सुरक्षित रूप से आपके फ़ोन में ही सहेजा जाता है।"
                        )
                    )
                }
            }

            // Full-Featured APK Export & Share Card
            val apkSizeLabel = ApkShareHelper.getInstalledApkSizeLabel(context)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_info_export_apk_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val ver = com.example.BuildConfig.VERSION_NAME
                    val versionedApk = "HealthCareAI_v${ver}.apk"
                    val versionedAab = "HealthCareAI_v${ver}.aab"
                    Text(
                        text = tr(
                            "📤 $versionedApk & $versionedAab",
                            "📤 $versionedApk અને $versionedAab વિગતો",
                            "📤 $versionedApk और $versionedAab विवरण"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = tr(
                            "Package: ${com.example.BuildConfig.APPLICATION_ID}\nVersion: $ver (Code ${com.example.BuildConfig.VERSION_CODE})${apkSizeLabel?.let { " • APK Size: $it" } ?: ""}\nFiles: $versionedApk & $versionedAab Ready.",
                            "પેકેજ: ${com.example.BuildConfig.APPLICATION_ID}\nવર્ઝન: $ver (કોડ ${com.example.BuildConfig.VERSION_CODE})${apkSizeLabel?.let { " • APK સાઇઝ: $it" } ?: ""}\nફાઇલો: $versionedApk અને $versionedAab તૈયાર છે.",
                            "पैकेज: ${com.example.BuildConfig.APPLICATION_ID}\nवर्ज़न: $ver (कोड ${com.example.BuildConfig.VERSION_CODE})${apkSizeLabel?.let { " • APK साइज: $it" } ?: ""}\nफ़ाइलें: $versionedApk व $versionedAab तैयार हैं।"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tr(
                            "Share the Health Care AI app (.apk) directly with family and friends via WhatsApp, Bluetooth, or other sharing apps.",
                            "આ Health Care AI એપ્લિકેશન (.apk) તમારા પરિવાર કે મિત્રોને WhatsApp અથવા અન્ય શેરિંગ એપ દ્વારા સરળતાથી મોકલો.",
                            "इस Health Care AI ऐप (.apk) को परिवार और मित्रों के साथ WhatsApp या अन्य शेयरिंग ऐप्स के माध्यम से साझा करें।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                ApkShareHelper.shareInstalledApk(context, selectedLanguage)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("app_info_share_apk_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            tr(
                                "Share $versionedApk",
                                "$versionedApk શેર કરો",
                                "$versionedApk शेयर करें"
                            )
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                ApkShareHelper.saveInstalledApkToDownloads(context, selectedLanguage)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("app_info_save_apk_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            tr(
                                "Save $versionedApk to Downloads",
                                "$versionedApk ડાઉનલોડ્સમાં સેવ કરો",
                                "$versionedApk डाउनलोड में सेव करें"
                            )
                        )
                    }
                }
            }

            // Medical Disclaimer Notice
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = tr("Disclaimer", "સૂચના", "अस्वीकरण"),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = tr(
                            "Disclaimer: This application is for educational and preliminary guidance only. It is not a substitute for a doctor or hospital. Always consult a licensed physician in serious conditions.",
                            "ડિસ્ક્લેમર: આ એપ્લિકેશન માત્ર શૈક્ષણિક અને પ્રાથમિક માર્ગદર્શન માટે છે. તે કોઈપણ ડૉક્ટર કે હોસ્પિટલનો વિકલ્પ નથી. કોઈપણ ગંભીર પરિસ્થિતિમાં હંમેશા લાયસન્સ ધરાવતા ડૉક્ટરની સલાહ લેવી.",
                            "अस्वीकरण: यह एप्लिकेशन केवल शैक्षिक और प्रारंभिक मार्गदर्शन के लिए है। यह किसी डॉक्टर या अस्पताल का विकल्प नहीं है। किसी भी गंभीर स्थिति में हमेशा योग्य डॉक्टर से सलाह लें।"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeatureBullet(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

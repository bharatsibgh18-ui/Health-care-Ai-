package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.ThemeMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.MediAssistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val userProfile by viewModel.userProfile.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var name by remember { mutableStateOf(userProfile?.fullName ?: "Health Patient") }
    var ageText by remember { mutableStateOf(userProfile?.age?.toString() ?: "30") }
    var gender by remember { mutableStateOf(userProfile?.gender ?: "Not Specified") }
    var bloodGroup by remember { mutableStateOf(userProfile?.bloodGroup ?: "Not Specified") }
    var allergies by remember { mutableStateOf(userProfile?.allergies ?: "None Known") }
    var chronicConditions by remember { mutableStateOf(userProfile?.chronicConditions ?: "None") }
    var medications by remember { mutableStateOf(userProfile?.currentMedications ?: "None") }
    var pastSurgeries by remember { mutableStateOf(userProfile?.pastSurgeries ?: "None") }
    var emergencyName by remember { mutableStateOf(userProfile?.emergencyContactName ?: "Family Contact") }
    var emergencyPhone by remember { mutableStateOf(userProfile?.emergencyContactPhone ?: "112") }
    var preferredLanguage by remember { mutableStateOf(userProfile?.preferredLanguage ?: "English") }

    LaunchedEffect(userProfile) {
        userProfile?.let {
            name = it.fullName
            ageText = it.age.toString()
            gender = it.gender
            bloodGroup = it.bloodGroup
            allergies = it.allergies
            chronicConditions = it.chronicConditions
            medications = it.currentMedications
            pastSurgeries = it.pastSurgeries
            emergencyName = it.emergencyContactName
            emergencyPhone = it.emergencyContactPhone
            preferredLanguage = it.preferredLanguage
        }
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
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tr("Health Profile", "આરોગ્ય પ્રોફાઇલ", "स्वास्थ्य प्रोफ़ाइल"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    com.example.ui.components.LanguageSelector(
                        viewModel = viewModel,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(
                        onClick = { viewModel.toggleThemeMode() },
                        modifier = Modifier.testTag("profile_theme_toggle_button")
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = tr("Patient Basic Details", "દર્દીની પ્રાથમિક માહિતી", "मरीज का बुनियादी विवरण"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(tr("Full Name", "પૂરું નામ", "पूरा नाम")) },
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it },
                            label = { Text(tr("Age", "ઉંમર", "आयु")) },
                            modifier = Modifier.weight(1f).testTag("profile_age_field")
                        )

                        OutlinedTextField(
                            value = gender,
                            onValueChange = { gender = it },
                            label = { Text(tr("Gender", "જાતિ", "लिंग")) },
                            modifier = Modifier.weight(1.2f).testTag("profile_gender_field")
                        )

                        OutlinedTextField(
                            value = bloodGroup,
                            onValueChange = { bloodGroup = it },
                            label = { Text(tr("Blood Group", "બ્લડ ગ્રુપ", "ब्लड ग्रुप")) },
                            modifier = Modifier.weight(1.2f).testTag("profile_blood_field")
                        )
                    }
                }
            }

            // Clinical Background Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = tr(
                            "Clinical Background Context",
                            "મેડિકલ ઇતિહાસની માહિતી",
                            "चिकित्सा पृष्ठभूमि जानकारी"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = tr(
                            "This medical context is saved safely on your device and used automatically to personalize health guidance.",
                            "આ મેડિકલ વિગતો તમારા ફોનમાં સુરક્ષિત રહે છે અને તેના આધારે AI વધુ સચોટ સલાહ આપે છે.",
                            "यह चिकित्सा विवरण आपके फोन में सुरक्षित रहता है और इसके आधार पर AI अधिक सटीक सलाह देता है।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = allergies,
                        onValueChange = { allergies = it },
                        label = {
                            Text(
                                tr(
                                    "Known Allergies (meds, foods, environment)",
                                    "જાણીતી એલર્જી (દવા, ખોરાક વગેરે)",
                                    "ज्ञात एलर्जी (दवा, भोजन आदि)"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("profile_allergies_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = chronicConditions,
                        onValueChange = { chronicConditions = it },
                        label = {
                            Text(
                                tr(
                                    "Chronic Conditions (e.g. Asthma, Diabetes, BP)",
                                    "લાંબા ગાળાની બીમારી (દા.ત. ડાયાબિટીસ, BP, અસ્થમા)",
                                    "दीर्घकालिक बीमारी (जैसे मधुमेह, BP, अस्थमा)"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("profile_chronic_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = medications,
                        onValueChange = { medications = it },
                        label = {
                            Text(
                                tr(
                                    "Current Daily Medications & Supplements",
                                    "હાલની નિયમિત દવાઓ અને વિટામિન્સ",
                                    "वर्तमान दैनिक दवाएं और सप्लीमेंट्स"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("profile_meds_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = pastSurgeries,
                        onValueChange = { pastSurgeries = it },
                        label = {
                            Text(
                                tr(
                                    "Past Surgeries / Major Illness History",
                                    "અગાઉના ઓપરેશન કે મોટી બીમારીનો ઇતિહાસ",
                                    "पिछली सर्जरी या गंभीर बीमारी का इतिहास"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("profile_surgeries_field")
                    )
                }
            }

            // Emergency Contact Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = tr(
                            "Personal Emergency Contact",
                            "અંગત ઇમરજન્સી સંપર્ક",
                            "व्यक्तिगत आपातकालीन संपर्क"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmergencyRed
                    )
                    Text(
                        text = tr(
                            "Quickly callable with 1 tap from the Emergency Banner and SOS mode.",
                            "ઇમરજન્સી બેનર અને SOS મોડમાંથી એક જ ટેપમાં તરત કૉલ કરી શકાય છે.",
                            "आपातकालीन बैनर और SOS मोड से एक ही टैप में तुरंत कॉल किया जा सकता है।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = emergencyName,
                        onValueChange = { emergencyName = it },
                        label = {
                            Text(
                                tr(
                                    "Contact Name (e.g. Mother, Spouse, Doctor)",
                                    "સંપર્કનું નામ (દા.ત. માતા, પિતા, ડૉક્ટર)",
                                    "संपर्क नाम (जैसे माता, पिता, डॉक्टर)"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("profile_emergency_name_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = emergencyPhone,
                        onValueChange = { emergencyPhone = it },
                        label = { Text(tr("Phone Number", "ફોન નંબર", "फ़ोन नंबर")) },
                        modifier = Modifier.fillMaxWidth().testTag("profile_emergency_phone_field")
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    val parsedAge = ageText.toIntOrNull() ?: 30
                    viewModel.saveProfile(
                        name = name,
                        age = parsedAge,
                        gender = gender,
                        bloodGroup = bloodGroup,
                        allergies = allergies,
                        chronicConditions = chronicConditions,
                        medications = medications,
                        pastSurgeries = pastSurgeries,
                        emergencyName = emergencyName,
                        emergencyPhone = emergencyPhone,
                        language = preferredLanguage
                    )
                    Toast.makeText(
                        context,
                        trLang(
                            appLanguage,
                            "Health profile saved locally!",
                            "આરોગ્ય પ્રોફાઇલ ફોનમાં સેવ થઈ ગઈ!",
                            "स्वास्थ्य प्रोफ़ाइल डिवाइस में सहेजी गई!"
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    tr("Save Health Profile", "આરોગ્ય પ્રોફાઇલ સેવ કરો", "स्वास्थ्य प्रोफ़ाइल सहेजें"),
                    fontWeight = FontWeight.Bold
                )
            }

            // Firebase Cloud Backup Button
            val isBackingUp by viewModel.isBackingUp.collectAsState()
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    viewModel.backupToCloud { _, message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                },
                enabled = !isBackingUp,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryTeal
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backup_to_cloud_button")
            ) {
                if (isBackingUp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tr("Syncing with Cloud...", "ક્લાઉડમાં સેવ થઈ રહ્યું છે...", "क्लाउड से सिंक हो रहा है..."),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tr(
                            "Backup to Cloud (Firebase)",
                            "ક્લાઉડમાં બેકઅપ લો (Firebase)",
                            "क्लाउड पर बैकअप लें (Firebase)"
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Medicine Reminders Card
            Card(
                onClick = { viewModel.setScreen(com.example.ui.viewmodel.AppScreen.MEDICINE_REMINDER) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_medicine_reminders_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = tr("Medicine Reminders", "દવા રિમાઇન્ડર", "दवा रिमाइंडर"),
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Medicine Reminders", "દવા રિમાઇન્ડર", "दवा रिमाइंडर"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                "Daily prescriptions • Automated alerts",
                                "રોજની દવાઓ • સમયસર આપોઆપ યાદ",
                                "दैनिक दवाएं • समय पर स्वचालित अलर्ट"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = tr("Open Reminders", "રિમાઇન્ડર ખોલો", "रिमाइंडर खोलें"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // App Info & Developer Credits Card
            Card(
                onClick = { viewModel.setScreen(com.example.ui.viewmodel.AppScreen.APP_INFO) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_app_info_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = tr("App Info", "એપ માહિતી", "ऐप जानकारी"),
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("App Info", "એપ માહિતી", "ऐप जानकारी"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                "Model School Sayla • Shailesh Satiya",
                                "મોડેલ સ્કૂલ સાયલા • શૈલેષ સતિયા",
                                "मॉडल स्कूल सायला • शैलेष सतिया"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr(
                                "Developer details, APK download & school info",
                                "મોડેલ સ્કૂલ સાયલા અને શૈલેષ સતિયાની તમામ વિગતો",
                                "डेवलपर विवरण, APK डाउनलोड और स्कूल की जानकारी"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = tr("View Details", "વિગતો જુઓ", "विवरण देखें"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Legal & Medical Disclaimers
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(
                                "Core Legal & Medical Disclaimers",
                                "મહત્વપૂર્ણ કાનૂની અને તબીબી સૂચનાઓ",
                                "महत्वपूर्ण कानूनी और चिकित्सा सूचनाएं"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tr(
                            "1. Health Care AI is NOT a licensed medical doctor.",
                            "૧. Health Care AI લાઇસન્સ પ્રાપ્ત ડૉક્ટર નથી.",
                            "1. Health Care AI कोई लाइसेंस प्राप्त डॉक्टर नहीं है।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = tr(
                            "2. It does NOT provide formal medical diagnoses or prescribe controlled medications.",
                            "૨. આ એપ સત્તાવાર નિદાન કે પ્રતિબંધિત દવાઓ લખી આપતી નથી.",
                            "2. यह औपचारिक चिकित्सा निदान या नियंत्रित दवाएं नहीं लिखता है।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = tr(
                            "3. Every response prioritizes safety and encourages consulting a certified doctor.",
                            "૩. દરેક સલાહમાં સલામતીને પ્રાથમિકતા અપાય છે અને પ્રમાણિત ડૉક્ટરને બતાવવાની ભલામણ કરાય છે.",
                            "3. हर सलाह में सुरक्षा को प्राथमिकता दी जाती है और प्रमाणित डॉक्टर से मिलने की सलाह दी जाती है।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = tr(
                            "4. For life-threatening symptoms (chest pain, stroke signs, breathing distress), call 112 or 108 immediately.",
                            "૪. જીવલેણ લક્ષણો (છાતીમાં દુખાવો, લકવાના ચિહ્નો, શ્વાસ લેવામાં તકલીફ) હોય તો તરત જ ૧૧૨ અથવા ૧૦૮ પર કૉલ કરો.",
                            "4. जानलेवा लक्षण (सीने में दर्द, लकवे के संकेत, सांस लेने में तकलीफ) होने पर तुरंत 112 या 108 पर कॉल करें।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = EmergencyRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

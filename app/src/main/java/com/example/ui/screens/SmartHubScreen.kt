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
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.VideoCall
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
                                Toast.makeText(
                                    context,
                                    trLang(
                                        selectedLanguage,
                                        "Tele-consult booking service online. Connecting to verified specialists...",
                                        "ટેલિ-કન્સલ્ટ બુકિંગ સેવા ઓનલાઈન છે. નિષ્ણાત ડૉક્ટર સાથે જોડાઈ રહ્યું છે...",
                                        "टेली-परामर्श बुकिंग सेवा ऑनलाइन है। विशेषज्ञ डॉक्टरों से जोड़ा जा रहा है..."
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr("Book Tele-Consult", "એપોઇન્ટમેન્ટ બુક કરો", "परामर्श बुक करें"))
                        }
                        Button(
                            onClick = {
                                viewModel.setScreen(AppScreen.EMERGENCY_GUIDE)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr("Nearby Doctors", "નજીકના દવાખાના", "नजदीकी डॉक्टर"))
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

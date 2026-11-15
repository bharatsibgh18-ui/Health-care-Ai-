package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.OnEmergencyRed
import com.example.ui.theme.OnEmergencyRedContainer
import com.example.ui.theme.tr
import com.example.ui.theme.trLang

@Composable
fun EmergencyHeaderBanner(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    emergencyNumber: String = "112"
) {
    val context = LocalContext.current
    val currentLanguage = LocalAppLanguage.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("emergency_header_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = EmergencyRedContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Emergency Warning",
                            tint = EmergencyRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tr(
                                "EMERGENCY RED FLAG ALERT",
                                "ગંભીર ઇમરજન્સી ચેતવણી",
                                "गंभीर आपातकालीन चेतावनी"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmergencyRed
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("emergency_banner_dismiss")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = tr("Dismiss", "બંધ કરો", "बंद करें"),
                            tint = OnEmergencyRedContainer
                        )
                    }
                }

                Text(
                    text = tr(
                        "Potential life-threatening emergency detected. Normal triage is suspended. Please contact emergency services immediately or head to the nearest Emergency Room.",
                        "ગંભીર જોખમી લક્ષણો જણાયા છે. કૃપા કરીને તાત્કાલિક ઇમરજન્સી સેવાનો સંપર્ક કરો અથવા નજીકની હોસ્પિટલના ઇમરજન્સી વિભાગમાં જાઓ.",
                        "गंभीर आपातकालीन स्थिति के संकेत मिले हैं। कृपया तुरंत आपातकालीन सेवाओं से संपर्क करें या नजदीकी अस्पताल के इमरजेंसी विभाग में जाएं।"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnEmergencyRedContainer,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { dialNumber(context, emergencyNumber) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("call_emergency_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmergencyRed,
                            contentColor = OnEmergencyRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            tr(
                                "Call $emergencyNumber",
                                "$emergencyNumber કૉલ",
                                "$emergencyNumber कॉल"
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { dialNumber(context, "108") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = EmergencyRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            tr("Call 108", "108 કૉલ", "108 कॉल"),
                            color = EmergencyRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Direct SOS SMS Dispatch
                var isSendingBannerSos by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

                Button(
                    onClick = {
                        isSendingBannerSos = true
                        coroutineScope.launch {
                            val coords = com.example.utils.EmergencySosLocationHelper.getCurrentLocation(context)
                            val mapsLink = coords?.let { (lat, lon) ->
                                com.example.utils.EmergencySosLocationHelper.buildGoogleMapsLink(lat, lon)
                            }
                            val sent = com.example.utils.EmergencySosLocationHelper.sendSosSms(
                                context = context,
                                phoneNumber = emergencyNumber,
                                patientName = trLang(currentLanguage, "Patient", "દર્દી", "मरीज़"),
                                mapsLink = mapsLink
                            )
                            isSendingBannerSos = false
                            val msg = if (sent) {
                                trLang(
                                    currentLanguage,
                                    "🚨 SOS SMS with GPS Location Sent to $emergencyNumber!",
                                    "🚨 GPS લોકેશન સાથે SOS SMS $emergencyNumber પર મોકલી દેવાયો!",
                                    "🚨 GPS लोकेशन के साथ SOS SMS $emergencyNumber पर भेज दिया गया!"
                                )
                            } else {
                                trLang(
                                    currentLanguage,
                                    "SOS message initiated",
                                    "SOS મેસેજ શરૂ કરવામાં આવ્યો",
                                    "SOS संदेश शुरू किया गया"
                                )
                            }
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isSendingBannerSos,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sos_location_banner_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OnEmergencyRedContainer,
                        contentColor = Color.White
                    )
                ) {
                    if (isSendingBannerSos) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            tr("Locating & Sending...", "લોકેશન મેળવી મોકલી રહ્યું છે...", "लोकेशन ढूंढकर भेजा जा रहा है..."),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            tr(
                                "🚨 Send Live GPS SOS SMS to $emergencyNumber",
                                "🚨 $emergencyNumber પર લાઇવ GPS SOS SMS મોકલો",
                                "🚨 $emergencyNumber पर लाइव GPS SOS SMS भेजें"
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun dialNumber(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

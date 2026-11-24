package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.UltraGlassCard
import com.example.ui.components.UltraGradientHeroCard
import com.example.ui.components.UltraSectionHeader
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.UltraCrimsonStart
import com.example.ui.theme.UltraRoseEnd
import com.example.ui.theme.trLang
import com.example.utils.HealthPassportPdfGenerator
import com.example.utils.HealthPassportSnapshot
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@Composable
fun UltraSosPassportTab(
    snapshot: HealthPassportSnapshot,
    emergencyHelpline: String,
    shakeToSosEnabled: Boolean,
    selectedLanguage: String,
    onToggleShakeToSos: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val safeHelpline = emergencyHelpline.ifBlank { "112" }

    var showSosCountdown by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(3) }

    fun dialEmergency() {
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$safeHelpline")))
        } catch (_: Throwable) {
            Toast.makeText(context, "Dial $safeHelpline", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendCaregiverSms() {
        val phone = snapshot.emergencyContactPhone.takeIf { it.isNotBlank() && it != "Not Set" } ?: safeHelpline
        val body = "EMERGENCY SOS — ${snapshot.patientName} needs urgent assistance! Blood Group: ${snapshot.bloodGroup}, Allergies: ${snapshot.allergies}, PM-JAY: ${snapshot.pmjayCardNumber}."
        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
                putExtra("sms_body", body)
            }
            context.startActivity(smsIntent)
        } catch (_: Throwable) {
            Toast.makeText(context, body, Toast.LENGTH_LONG).show()
        }
    }

    // Optional Accelerometer Shake-to-SOS Guard (opens 3-second cancelable countdown)
    DisposableEffect(shakeToSosEnabled, context) {
        if (!shakeToSosEnabled) {
            onDispose { }
        } else {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            var lastShakeTime = 0L
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val values = event?.values ?: return
                    val gForce = sqrt(
                        (values[0] * values[0] + values[1] * values[1] + values[2] * values[2]).toDouble()
                    ) / SensorManager.GRAVITY_EARTH
                    val now = System.currentTimeMillis()
                    if (gForce > 2.7 && (now - lastShakeTime) > 3000L && !showSosCountdown) {
                        lastShakeTime = now
                        countdownSeconds = 3
                        showSosCountdown = true
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            if (accel != null) {
                sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
            }
            onDispose {
                sensorManager?.unregisterListener(listener)
            }
        }
    }

    LaunchedEffect(showSosCountdown) {
        if (showSosCountdown) {
            for (sec in 3 downTo 1) {
                countdownSeconds = sec
                delay(1000L)
            }
            if (showSosCountdown) {
                showSosCountdown = false
                dialEmergency()
            }
        }
    }

    if (showSosCountdown) {
        AlertDialog(
            onDismissRequest = { showSosCountdown = false },
            icon = { Icon(Icons.Default.Emergency, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    text = trLang(
                        selectedLanguage,
                        "SOS Triggered — Calling in ${countdownSeconds}s",
                        "SOS શરૂ થયું — ${countdownSeconds} સેકન્ડમાં કૉલ થશે",
                        "SOS सक्रिय — ${countdownSeconds} सेकंड में कॉल होगा"
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = EmergencyRed
                )
            },
            text = {
                Text(
                    text = trLang(
                        selectedLanguage,
                        "Opening emergency dialer for $safeHelpline. Tap Cancel if this was accidental.",
                        "ઇમરજન્સી નંબર $safeHelpline ડાયલ થઈ રહ્યો છે. ભૂલથી થયું હોય તો રદ કરો.",
                        "आपातकालीन नंबर $safeHelpline डायल किया जा रहा है। गलती से हुआ हो तो रद्द करें।"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosCountdown = false
                        dialEmergency()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text(trLang(selectedLanguage, "Call Now", "હમણાં કૉલ કરો", "अभी कॉल करें"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSosCountdown = false }) {
                    Text(trLang(selectedLanguage, "Cancel", "રદ", "रद्द"))
                }
            }
        )
    }

    // 1. Emergency SOS Quick-Shield Hero Card
    UltraGradientHeroCard(
        modifier = Modifier.testTag("ultra_sos_shield_card"),
        colors = listOf(UltraCrimsonStart, UltraRoseEnd)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Emergency,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trLang(selectedLanguage, "Emergency SOS Shield", "ઇમરજન્સી SOS શીલ્ડ", "आपातकालीन SOS शील्ड"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = trLang(
                        selectedLanguage,
                        "Instant emergency dial ($safeHelpline) & caregiver medical alert.",
                        "તાત્કાલિક ઇમરજન્સી કૉલ ($safeHelpline) અને પરિવારને મેસેજ.",
                        "तुरंत आपातकालीन कॉल ($safeHelpline) और परिवार को संदेश।"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { dialEmergency() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = EmergencyRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("ultra_sos_call_btn")
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SOS $safeHelpline", fontWeight = FontWeight.ExtraBold)
            }
            OutlinedButton(
                onClick = { sendCaregiverSms() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("ultra_sos_sms_btn")
            ) {
                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = trLang(selectedLanguage, "Alert Family", "પરિવાર એલર્ટ", "परिवार अलर्ट"),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.16f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Vibration, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trLang(selectedLanguage, "Shake-to-SOS Guard", "ફોન હલાવીને SOS ગાર્ડ", "शेक-टू-SOS गार्ड"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = trLang(
                            selectedLanguage,
                            "Vigorous shake opens a safe 3s countdown before calling.",
                            "જોરથી ફોન હલાવતા કૉલ પહેલા 3 સેકન્ડનો સુરક્ષિત ટાઇમર ખુલશે.",
                            "ज़ोर से फ़ोन हिलाने पर कॉल से पहले 3 सेकंड का सुरक्षित टाइमर खुलेगा।"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Switch(
                    checked = shakeToSosEnabled,
                    onCheckedChange = onToggleShakeToSos,
                    modifier = Modifier.testTag("ultra_sos_shake_switch")
                )
            }
        }
    }

    // 2. Doctor-Ready Live Clinical Health Passport PDF Card
    UltraSectionHeader(
        title = trLang(selectedLanguage, "Clinical Health Passport PDF", "ક્લિનિકલ હેલ્થ પાસપોર્ટ PDF", "क्लिनिकल हेल्थ पासपोर्ट PDF"),
        icon = Icons.Default.Description
    )

    UltraGlassCard(modifier = Modifier.fillMaxWidth().testTag("ultra_health_passport_card")) {
        Text(
            text = trLang(
                selectedLanguage,
                "Generate an official A4 clinical summary PDF with your profile, blood group, allergies, PM-JAY / ABHA IDs, active medicines, vitals, and Ultra Health Score.",
                "તમારી પ્રોફાઇલ, બ્લડ ગ્રુપ, એલર્જી, PM-JAY / ABHA ID, દવાઓ અને વાઇટલ્સ સાથેનો ડૉક્ટર-રેડી A4 હેલ્થ પાસપોર્ટ PDF બનાવો અને શેર કરો.",
                "अपनी प्रोफ़ाइल, ब्लड ग्रुप, एलर्जी, PM-JAY / ABHA ID, दवाओं और वाइटल्स का डॉक्टर-रेडी A4 हेल्थ पासपोर्ट PDF बनाएं और शेयर करें।"
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        PassportInfoLine(trLang(selectedLanguage, "Patient", "દર્દી", "मरीज"), snapshot.patientName)
        PassportInfoLine(trLang(selectedLanguage, "Blood Group", "બ્લડ ગ્રુપ", "ब्लड ग्रुप"), snapshot.bloodGroup)
        PassportInfoLine(trLang(selectedLanguage, "Allergies", "એલર્જી", "एलर्जी"), snapshot.allergies)
        PassportInfoLine("PM-JAY / ABHA", "${snapshot.pmjayCardNumber} • ${snapshot.abhaCardNumber}")
        PassportInfoLine(
            trLang(selectedLanguage, "Clinical Summary", "ક્લિનિકલ સારાંશ", "क्लिनिकल सारांश"),
            "Score ${snapshot.healthScore}/100 • ${snapshot.activeMedicines.size} Rx • ${snapshot.latestVitals.size} Vitals"
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                HealthPassportPdfGenerator.exportAndSharePassportPdf(
                    context = context,
                    snapshot = snapshot,
                    language = selectedLanguage
                )
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("ultra_passport_export_pdf_btn")
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = trLang(
                    selectedLanguage,
                    "Export & Share Health Passport PDF",
                    "હેલ્થ પાસપોર્ટ PDF બનાવો અને શેર કરો",
                    "हेल्थ पासपोर्ट PDF एक्सपोर्ट और शेयर करें"
                ),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PassportInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

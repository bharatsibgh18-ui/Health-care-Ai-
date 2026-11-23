package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.MedicineEntity
import com.example.data.local.UserProfileEntity
import com.example.ui.components.ScannableQrCodeCanvas
import com.example.ui.components.UltraGlassCard
import com.example.ui.components.UltraGradientHeroCard
import com.example.ui.components.UltraMetricPill
import com.example.ui.components.UltraSectionHeader
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.UltraAmberStart
import com.example.ui.theme.UltraCrimsonStart
import com.example.ui.theme.UltraCyanEnd
import com.example.ui.theme.UltraEmeraldStart
import com.example.ui.theme.UltraGoldEnd
import com.example.ui.theme.UltraMintEnd
import com.example.ui.theme.UltraRoseEnd
import com.example.ui.theme.UltraTealStart
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.utils.EmergencyQrCodeGenerator
import com.example.utils.HealthCardValidator
import com.example.utils.MedicationStreakAnalytics

@Composable
fun UltraMedicationStreaksTab(
    analytics: MedicationStreakAnalytics,
    medicines: List<MedicineEntity>,
    onMarkTaken: (MedicineEntity) -> Unit,
    onOpenMedicineReminders: () -> Unit
) {
    // Streak Hero Gradient Card
    UltraGradientHeroCard(
        colors = listOf(UltraEmeraldStart, UltraTealStart, UltraCyanEnd),
        modifier = Modifier.testTag("ultra_streaks_hero_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(UltraAmberStart, UltraGoldEnd)))
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${analytics.currentStreakDays} " + tr("Day Streak", "દિવસની સ્ટ્રીક", "दिन की स्ट्रीक"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = tr(
                        "Best Streak: ${analytics.bestStreakDays} days • Total Taken: ${analytics.totalTakenAllTime}",
                        "શ્રેષ્ઠ સ્ટ્રીક: ${analytics.bestStreakDays} દિવસ • કુલ લીધેલી: ${analytics.totalTakenAllTime}",
                        "सर्वश्रेष्ठ स्ट्रीक: ${analytics.bestStreakDays} दिन • कुल ली गईं: ${analytics.totalTakenAllTime}"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            UltraMetricPill(
                label = tr("7-Day Rate", "૭ દિવસ %", "7-दिन दर"),
                value = "${analytics.adherenceRate7d}%",
                icon = Icons.Default.EmojiEvents,
                tint = Color(0xFFFDE68A),
                modifier = Modifier.weight(1f)
            )
            UltraMetricPill(
                label = tr("30-Day Rate", "૩૦ દિવસ %", "30-दिन दर"),
                value = "${analytics.adherenceRate30d}%",
                icon = Icons.Default.CheckCircle,
                tint = UltraMintEnd,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // 7-Day Visual Adherence Bar Chart
    UltraGlassCard(
        accentStart = UltraEmeraldStart,
        accentEnd = UltraCyanEnd,
        modifier = Modifier.testTag("ultra_7day_adherence_chart")
    ) {
        UltraSectionHeader(
            title = tr("7-Day Adherence Analytics", "૭ દિવસની દવા નિયમિતતા", "7-दिन दवा नियमितता चार्ट"),
            subtitle = tr("Daily dose completion ratio", "દૈનિક દવા પૂર્ણતાનો ગ્રાફ", "दैनिक खुराक पूर्णता ग्राफ"),
            icon = Icons.Default.Medication,
            accentColor = UltraEmeraldStart
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            analytics.dailyPoints7d.forEach { day ->
                val barHeight = (16f + (day.ratio * 76f)).dp
                val barBrush = when {
                    day.ratio >= 0.85f -> Brush.verticalGradient(listOf(UltraMintEnd, UltraEmeraldStart))
                    day.ratio >= 0.45f -> Brush.verticalGradient(listOf(UltraCyanEnd, UltraTealStart))
                    else -> Brush.verticalGradient(listOf(Color.Gray.copy(alpha = 0.45f), Color.Gray.copy(alpha = 0.25f)))
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = if (day.takenCount > 0) "${day.takenCount}" else "-",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(barBrush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = day.dayShortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Per-Medicine Accountability & 1-Tap Mark Taken
    UltraGlassCard(
        accentStart = UltraTealStart,
        accentEnd = UltraCyanEnd,
        modifier = Modifier.testTag("ultra_per_medicine_list")
    ) {
        UltraSectionHeader(
            title = tr("Active Medicines & Today's Status", "ચાલુ દવાઓ અને આજની સ્થિતિ", "सक्रिय दवाएं और आज की स्थिति"),
            subtitle = if (analytics.pendingTodayCount > 0) {
                tr("${analytics.pendingTodayCount} pending today", "આજે ${analytics.pendingTodayCount} દવા બાકી", "आज ${analytics.pendingTodayCount} खुराक बाकी")
            } else {
                tr("All active medicines accounted for", "બધી દવાઓ પૂર્ણ", "सभी दवाएं पूर्ण")
            },
            icon = Icons.Default.CheckCircle,
            accentColor = UltraTealStart
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (analytics.perMedicineStats.isEmpty()) {
            Text(
                text = tr(
                    "No active medicines scheduled yet. Add your prescriptions to build adherence streaks.",
                    "હજુ કોઈ દવા ઉમેરેલી નથી. સ્ટ્રીક બનાવવા માટે દવા રિમાઇન્ડર ઉમેરો.",
                    "अभी कोई सक्रिय दवा नहीं है। स्ट्रीक बनाने के लिए दवा रिमाइंडर जोड़ें।"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onOpenMedicineReminders,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(tr("Add Medicine Reminder", "દવા રિમાઇન્ડર ઉમેરો", "दवा रिमाइंडर जोड़ें"))
            }
        } else {
            analytics.perMedicineStats.forEach { stat ->
                val medEntity = medicines.firstOrNull { it.id == stat.medicineId }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${stat.medicineName} (${stat.dosage})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${stat.timeOfDay} • ${stat.adherencePercent}% " + tr("on-time", "નિયમિત", "नियमित"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (stat.takenToday) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SafeMint.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SafeMint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tr("Taken", "લીધી", "ली गई"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SafeMint
                                    )
                                }
                            }
                        } else if (medEntity != null) {
                            Button(
                                onClick = { onMarkTaken(medEntity) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text(tr("Mark Taken", "લીધી નોંધો", "ली गई"))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenMedicineReminders,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(tr("Manage All Medicine Reminders", "બધી દવા રિમાઇન્ડર મેનેજ કરો", "सभी दवा रिमाइंडर प्रबंधित करें"))
            }
        }
    }
}

@Composable
fun UltraEmergencyMedicalIdTab(
    profile: UserProfileEntity?,
    selectedLanguage: String,
    onEditProfile: () -> Unit,
    onOpenEmergencyGuide: () -> Unit
) {
    val context = LocalContext.current
    val qrPayload = remember(profile) { EmergencyQrCodeGenerator.buildEmergencyPayload(profile) }
    val qrMatrix = remember(qrPayload) { EmergencyQrCodeGenerator.encode(qrPayload) }
    val formattedAbha = remember(profile?.abhaCardNumber) {
        HealthCardValidator.formatAbhaForDisplay(profile?.abhaCardNumber.orEmpty())
    }
    val pmjay = profile?.pmjayCardNumber?.trim().orEmpty()

    // Paramedic High-Contrast Emergency Medical ID Gradient Card
    UltraGradientHeroCard(
        colors = listOf(UltraCrimsonStart, UltraRoseEnd, Color(0xFF881337)),
        modifier = Modifier.testTag("ultra_emergency_medical_id_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
            ) {
                Icon(
                    imageVector = Icons.Default.Emergency,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("EMERGENCY MEDICAL ID", "ઇમરજન્સી મેડિકલ ID", "आपातकालीन मेडिकल ID"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFE4E6)
                )
                Text(
                    text = profile?.fullName?.ifBlank { "Health Patient" } ?: "Health Patient",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "${profile?.age ?: 30} yrs • ${profile?.gender ?: "Not Specified"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bloodtype,
                        contentDescription = null,
                        tint = EmergencyRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = profile?.bloodGroup?.ifBlank { "N/A" } ?: "N/A",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmergencyRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Critical clinical strip
        MedicalIdDetailRow(
            label = tr("Allergies", "એલર્જી", "एलर्जी"),
            value = profile?.allergies?.ifBlank { "None Known" } ?: "None Known"
        )
        MedicalIdDetailRow(
            label = tr("Conditions", "જૂની બીમારી", "पुरानी बीमारी"),
            value = profile?.chronicConditions?.ifBlank { "None" } ?: "None"
        )
        MedicalIdDetailRow(
            label = tr("Medications", "ચાલુ દવાઓ", "वर्तमान दवाएं"),
            value = profile?.currentMedications?.ifBlank { "None" } ?: "None"
        )
        MedicalIdDetailRow(
            label = tr("ICE Contact", "ઇમરજન્સી સંપર્ક", "आपातकालीन संपर्क"),
            value = "${profile?.emergencyContactName ?: "Family"} • ${profile?.emergencyContactPhone ?: "112"}"
        )

        if (pmjay.isNotBlank() || formattedAbha.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (pmjay.isNotBlank()) {
                    UltraMetricPill(
                        label = "PMJAY (8-Char)",
                        value = pmjay,
                        icon = Icons.Default.Badge,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (formattedAbha.isNotBlank()) {
                    UltraMetricPill(
                        label = "ABHA (14-Digit)",
                        value = formattedAbha,
                        icon = Icons.Default.Badge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Scannable Offline QR Health Card
    UltraGlassCard(
        accentStart = UltraCrimsonStart,
        accentEnd = UltraTealStart,
        modifier = Modifier.testTag("ultra_qr_health_card")
    ) {
        UltraSectionHeader(
            title = tr("Scannable Offline QR Health Card", "ઓફલાઇન સ્કેનેબલ QR હેલ્થ કાર્ડ", "ऑफ़लाइन स्कैनेबल QR हेल्थ कार्ड"),
            subtitle = tr(
                "Paramedics & ER doctors can scan this code offline for instant blood group, allergies, PMJAY & ABHA details",
                "ડૉક્ટર કે ઇમરજન્સી સ્ટાફ આ QR ઓફલાઇન સ્કેન કરી તરત માહિતી મેળવી શકે છે",
                "डॉक्टर और इमरजेंसी स्टाफ इसे ऑफ़लाइन स्कैन करके तुरंत जानकारी देख सकते हैं"
            ),
            icon = Icons.Default.QrCode2,
            accentColor = UltraTealStart
        )
        Spacer(modifier = Modifier.height(14.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            ScannableQrCodeCanvas(
                matrix = qrMatrix,
                modifier = Modifier
                    .size(220.dp)
                    .testTag("emergency_qr_code_canvas")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = tr(
                "ISO/IEC 18004 QR • Matrix ${qrMatrix.size}×${qrMatrix.size} • 100% On-Device",
                "ISO/IEC 18004 QR • ${qrMatrix.size}×${qrMatrix.size} • 100% ઓફલાઇન",
                "ISO/IEC 18004 QR • ${qrMatrix.size}×${qrMatrix.size} • 100% ऑफ़लाइन"
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Emergency Medical ID")
                        putExtra(Intent.EXTRA_TEXT, qrPayload)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Medical ID"))
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("ultra_share_medical_id_btn")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Share ID", "ID શેર કરો", "ID शेयर करें"))
            }
            OutlinedButton(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("Emergency Medical ID", qrPayload))
                    Toast.makeText(
                        context,
                        trLang(selectedLanguage, "Medical ID copied!", "મેડિકલ ID કોપી થઈ ગઈ!", "मेडिकल ID कॉपी हो गई!"),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("ultra_copy_medical_id_btn")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Copy", "કોપી", "कॉपी"))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onEditProfile,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Edit Profile", "પ્રોફાઇલ સુધારો", "प्रोफ़ाइल बदलें"))
            }
            Button(
                onClick = onOpenEmergencyGuide,
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("SOS Guide", "SOS ગાઇડ", "SOS गाइड"))
            }
        }
    }
}

@Composable
private fun MedicalIdDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFFFE4E6)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.End
        )
    }
}

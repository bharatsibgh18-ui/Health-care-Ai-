package com.example.ui.screens.clinical

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import com.example.utils.WearableConnectionStatus
import kotlin.math.sin

@Composable
fun RemoteMonitoringPane(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val bleState by viewModel.bleWearableState.collectAsState()
    val vitalLogs by viewModel.vitalLogs.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshBleStatus()
    }

    val blePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.refreshBleStatus()
        if (results.values.all { it }) {
            viewModel.startWearableScan()
        } else {
            Toast.makeText(
                context,
                trLang(
                    selectedLanguage,
                    "Bluetooth permissions required to scan smartwatches",
                    "સ્માર્ટવોચ સ્કેન કરવા માટે Bluetooth પરવાનગી જરૂરી છે",
                    "स्मार्टवॉच स्कैन करने के लिए Bluetooth अनुमति आवश्यक है"
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val latestHrLog = vitalLogs.firstOrNull { it.vitalType.equals("Heart Rate", ignoreCase = true) }
    val latestSpo2Log = vitalLogs.firstOrNull { it.vitalType.equals("SpO2", ignoreCase = true) }

    val displayedHr = bleState.liveHeartRateBpm ?: latestHrLog?.primaryValue?.toInt()
    val displayedSpo2 = bleState.liveSpO2Percent ?: latestSpo2Log?.primaryValue?.toInt()
    val isLiveWearable = bleState.connectionStatus == WearableConnectionStatus.CONNECTED &&
        (bleState.liveHeartRateBpm != null || bleState.liveSpO2Percent != null)

    val isWardAlert = (displayedHr != null && (displayedHr > 110 || displayedHr < 50)) ||
        (displayedSpo2 != null && displayedSpo2 < 92)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Watch, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            "Remote Monitoring & Virtual Ward",
                            "રિમોટ મોનિટરિંગ & વર્ચ્યુઅલ વોર્ડ",
                            "रिमोट मॉनिटरिंग और वर्चुअल वार्ड"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Connect BLE smartwatches & pulse oximeters for real-time HR and SpO2 home ward care",
                            "સ્માર્ટવોચ અને વેરેબલ્સ સાથે જોડાઈને હાર્ટ રેટ, ઓક્સિજન અને વાઇટલ્સનું લાઈવ મોનિટરિંગ",
                            "स्मार्टवॉच और वियरेबल्स से जुड़कर हार्ट रेट और ऑक्सीजन का लाइव मॉनिटरिंग"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Virtual Ward Alert Banner when threshold crossed
        if (isWardAlert) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = EmergencyRedContainer),
                modifier = Modifier.fillMaxWidth().testTag("virtual_ward_alert_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(
                                "Virtual Ward Threshold Alert!",
                                "વર્ચ્યુઅલ વોર્ડ તાત્કાલિક ચેતવણી!",
                                "वर्चुअल वार्ड चेतावनी!"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmergencyRed
                        )
                    }
                    Text(
                        text = tr(
                            "Monitored HR/SpO2 is outside safe home-ward limits. Contact a physician or trigger Caregiver SOS.",
                            "હાર્ટ રેટ કે ઓક્સિજન સામાન્ય રેન્જ બહાર છે. તરત ડૉક્ટર કે પરિવારને એલર્ટ કરો.",
                            "हार्ट रेट या ऑक्सीजन सामान्य सीमा से बाहर है। तुरंत डॉक्टर या परिवार को सूचित करें।"
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { viewModel.setScreen(AppScreen.EMERGENCY_GUIDE) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                    ) {
                        Text(tr("Open Emergency & SOS", "ઇમરજન્સી SOS ખોલો", "आपातकालीन SOS खोलें"))
                    }
                }
            }
        }

        // Live Virtual Ward Vitals Cards
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                modifier = Modifier.weight(1f).testTag("ward_hr_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Heart Rate", "હાર્ટ રેટ", "हार्ट रेट"), style = MaterialTheme.typography.labelLarge)
                    }
                    Text(
                        text = displayedHr?.let { "$it bpm" } ?: "-- bpm",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (bleState.liveHeartRateBpm != null) tr("Live Wearable", "લાઈવ સ્માર્ટવોચ", "लाइव वियरेबल")
                        else if (latestHrLog != null) tr("Last Saved Log", "છેલ્લો સેવ ડેટા", "अंतिम सहेजा गया")
                        else tr("Awaiting device", "ડિવાઇસ કનેક્ટ કરો", "डिवाइस कनेक्ट करें"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLiveWearable) SafeMint else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                modifier = Modifier.weight(1f).testTag("ward_spo2_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Air, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Oxygen (SpO2)", "ઓક્સિજન (SpO2)", "ऑक्सीजन (SpO2)"), style = MaterialTheme.typography.labelLarge)
                    }
                    Text(
                        text = displayedSpo2?.let { "$it%" } ?: "--%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (bleState.liveSpO2Percent != null) tr("Live Oximeter", "લાઈવ ઓક્સિમીટર", "लाइव ऑक्सीमीटर")
                        else if (latestSpo2Log != null) tr("Last Saved Log", "છેલ્લો સેવ ડેટા", "अंतिम सहेजा गया")
                        else tr("Awaiting device", "ડિવાઇસ કનેક્ટ કરો", "डिवाइस कनेक्ट करें"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLiveWearable) SafeMint else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Animated Real-Time Ward Waveform Canvas (phase rate scales with actual BPM when connected)
        WardWaveformCard(isConnected = bleState.connectionStatus == WearableConnectionStatus.CONNECTED, bpm = displayedHr ?: 72)

        // BLE Smartwatch / Wearable Scanner & Connection Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth().testTag("ble_wearable_card")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (bleState.connectionStatus) {
                            WearableConnectionStatus.CONNECTED -> Icons.Default.BluetoothConnected
                            WearableConnectionStatus.CONNECTING -> Icons.AutoMirrored.Filled.BluetoothSearching
                            WearableConnectionStatus.DISCONNECTED -> Icons.Default.Bluetooth
                        },
                        contentDescription = null,
                        tint = if (bleState.connectionStatus == WearableConnectionStatus.CONNECTED) SafeMint else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bleState.connectedDeviceName
                                ?: tr("BLE Smartwatch & Sensor Hub", "BLE સ્માર્ટવોચ અને સેન્સર હબ", "BLE स्मार्टवॉच और सेंसर हब"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (bleState.statusNote.isNotBlank()) {
                            Text(
                                text = bleState.statusNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    bleState.batteryPercent?.let { bat ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = SafeMint, modifier = Modifier.size(16.dp))
                            Text("$bat%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (!bleState.hasPermission) {
                        Button(
                            onClick = { blePermissionLauncher.launch(viewModel.bleRequiredPermissions()) },
                            modifier = Modifier.fillMaxWidth().testTag("ble_grant_perm_btn")
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Allow Bluetooth Wearable Access", "સ્માર્ટવોચ માટે Bluetooth પરવાનગી આપો", "स्मार्टवॉच के लिए Bluetooth अनुमति दें"))
                        }
                    } else if (!bleState.isBluetoothEnabled) {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (_: Exception) {
                                    Toast.makeText(context, trLang(selectedLanguage, "Enable Bluetooth in Settings", "સેટિંગ્સમાં Bluetooth ચાલુ કરો", "सेटिंग्स में Bluetooth चालू करें"), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("ble_enable_bt_btn")
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Enable Bluetooth", "Bluetooth ચાલુ કરો", "Bluetooth चालू करें"))
                        }
                    } else {
                        Button(
                            onClick = {
                                if (bleState.isScanning) viewModel.stopWearableScan() else viewModel.startWearableScan()
                            },
                            modifier = Modifier.weight(1f).testTag("ble_scan_btn")
                        ) {
                            if (bleState.isScanning) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("Stop Scan", "સ્કેન રોકો", "स्कैन रोकें"))
                            } else {
                                Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("Scan Wearables", "સ્માર્ટવોચ શોધો", "वियरेबल खोजें"))
                            }
                        }

                        if (bleState.connectionStatus == WearableConnectionStatus.CONNECTED) {
                            OutlinedButton(
                                onClick = { viewModel.disconnectWearableDevice() },
                                modifier = Modifier.weight(1f).testTag("ble_disconnect_btn")
                            ) {
                                Text(tr("Disconnect", "ડિસ્કનેક્ટ કરો", "डिस्कनेक्ट करें"))
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.setScreen(AppScreen.VITALS_TRACKER) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(tr("Manual Log", "મેન્યુઅલ લોગ", "मैन्युअल लॉग"))
                            }
                        }
                    }
                }

                if (bleState.liveHeartRateBpm != null || bleState.liveSpO2Percent != null) {
                    Button(
                        onClick = {
                            viewModel.syncWearableVitalsToLog()
                            Toast.makeText(
                                context,
                                trLang(selectedLanguage, "Synced live wearable vitals to Health Log!", "વેરેબલના વાઇટલ્સ હેલ્થ લોગમાં સેવ થયા!", "वियरेबल वाइटल्स हेल्थ लॉग में सहेजे गए!"),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("ble_sync_vitals_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Save Live Reading to Health Log", "લાઈવ રીડિંગ હેલ્થ લોગમાં સેવ કરો", "लाइव रीडिंग हेल्थ लॉग में सहेजें"))
                    }
                }
            }
        }

        // Discovered BLE Devices List
        if (bleState.discoveredDevices.isNotEmpty()) {
            Text(
                text = tr("Nearby Bluetooth Devices (${bleState.discoveredDevices.size})", "નજીકના Bluetooth ડિવાઇસ (${bleState.discoveredDevices.size})", "नजदीकी Bluetooth डिवाइस (${bleState.discoveredDevices.size})"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            bleState.discoveredDevices.forEach { device ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Watch, contentDescription = null, tint = if (device.supportsHealthProfile) SafeMint else MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${device.address} • ${device.rssi} dBm" + if (device.supportsHealthProfile) " • Health GATT" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { viewModel.connectWearableDevice(device.address, device.name) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(tr("Connect", "જોડો", "कनेक्ट"))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WardWaveformCard(
    isConnected: Boolean,
    bpm: Int
) {
    val transition = rememberInfiniteTransition(label = "ward_pulse")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (60_000 / bpm.coerceIn(45, 160)).coerceIn(400, 1400), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_phase"
    )
    val lineColor = if (isConnected) SafeMint else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = tr("Virtual Ward Telemetry Monitor", "વર્ચ્યુઅલ વોર્ડ ટેલિમેટ્રી મોનિટર", "वर्चुअल वार्ड टेलीमेट्री मॉनिटर"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isConnected) "● LIVE" else "STANDBY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) SafeMint else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                val midY = size.height / 2f
                drawLine(
                    color = Color.Gray.copy(alpha = 0.25f),
                    start = Offset(0f, midY),
                    end = Offset(size.width, midY),
                    strokeWidth = 1.5f
                )
                val path = Path()
                val steps = 100
                for (i in 0..steps) {
                    val x = (i / steps.toFloat()) * size.width
                    val norm = (i / steps.toFloat()) * 12f + phase
                    val amp = if (isConnected) size.height * 0.32f else size.height * 0.08f
                    val y = midY - (sin(norm) * amp * 0.45f) - (if (sin(norm * 2f) > 0.85f) amp * 0.55f else 0f)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path = path, color = lineColor, style = Stroke(width = 3.5f))
            }
        }
    }
}

package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.filled.ScheduleSend
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.MediAssistViewModel

@Composable
fun OneSignalSettingsCard(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val config by viewModel.oneSignalConfig.collectAsState()
    val lastStatus by viewModel.oneSignalLastStatus.collectAsState()

    var appIdInput by remember { mutableStateOf(config.appId) }
    var restKeyInput by remember { mutableStateOf(config.restApiKey) }
    var externalUserIdInput by remember { mutableStateOf(config.targetExternalUserId) }
    var enabledSwitch by remember { mutableStateOf(config.enabled) }
    var showApiKey by remember { mutableStateOf(false) }

    LaunchedEffect(config) {
        if (appIdInput.isBlank() && config.appId.isNotBlank()) appIdInput = config.appId
        if (restKeyInput.isBlank() && config.restApiKey.isNotBlank()) restKeyInput = config.restApiKey
        if (externalUserIdInput.isBlank() && config.targetExternalUserId.isNotBlank()) {
            externalUserIdInput = config.targetExternalUserId
        }
        enabledSwitch = config.enabled
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth().testTag("onesignal_settings_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tr(
                                "OneSignal Background Push Settings",
                                "OneSignal બેકગ્રાઉન્ડ પુશ સેટિંગ્સ",
                                "OneSignal बैकग्राउंड पुश सेटिंग्स"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (config.isConfigured && config.enabled) {
                                tr(
                                    "Cloud Push + Local Background Alarms Active",
                                    "ક્લાઉડ પુશ + લોકલ બેકગ્રાઉન્ડ એલાર્મ સક્રિય",
                                    "क्लाउड पुश + लोकल बैकग्राउंड अलार्म सक्रिय"
                                )
                            } else {
                                tr(
                                    "Local Background Alarms + Offline In-App Fallback",
                                    "લોકલ બેકગ્રાઉન્ડ એલાર્મ + ઓફલાઇન ઇન-એપ એલર્ટ",
                                    "लोकल बैकग्राउंड अलार्म + ऑफ़लाइन इन-ऐप अलर्ट"
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (config.isConfigured && config.enabled) SafeMint else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = enabledSwitch,
                    onCheckedChange = { checked ->
                        enabledSwitch = checked
                        viewModel.saveOneSignalSettings(
                            appId = appIdInput,
                            restApiKey = restKeyInput,
                            targetExternalUserId = externalUserIdInput,
                            enabled = checked
                        )
                    },
                    modifier = Modifier.testTag("onesignal_enable_switch")
                )
            }

            Text(
                text = tr(
                    "Schedules background push notifications via OneSignal REST API (send_after ISO-8601) when reminders are created, with automatic in-app alert fallback if offline.",
                    "રિમાઇન્ડર સેટ કરતાં OneSignal REST API (send_after ISO સમય) દ્વારા બેકગ્રાઉન્ડ પુશ નોટિફિકેશન શેડ્યૂલ થાય છે, અને ઓફલાઇન હોય તો ઇન-એપ એલર્ટ દેખાય છે.",
                    "रिमाइंडर सेट करने पर OneSignal REST API (send_after ISO समय) से बैकग्राउंड पुश शेड्यूल होता है, और ऑफ़लाइन होने पर इन-ऐप अलर्ट दिखता है।"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = appIdInput,
                onValueChange = { appIdInput = it },
                label = { Text("ONESIGNAL_APP_ID") },
                placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("onesignal_app_id_input")
            )

            OutlinedTextField(
                value = restKeyInput,
                onValueChange = { restKeyInput = it },
                label = { Text("ONESIGNAL_REST_API_KEY") },
                placeholder = { Text("os_v2_app_...") },
                singleLine = true,
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = tr("Toggle Key Visibility", "કી બતાવો/છુપાવો", "की दिखाएं/छुपाएं")
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("onesignal_rest_key_input")
            )

            OutlinedTextField(
                value = externalUserIdInput,
                onValueChange = { externalUserIdInput = it },
                label = {
                    Text(
                        tr(
                            "Target External User ID (Optional — blank targets Subscribed Users)",
                            "ટાર્ગેટ યુઝર ID (વૈકલ્પિક — ખાલી રાખવાથી બધા સબ્સ્ક્રાઇબ્ડ ડિવાઇસ)",
                            "टारगेट यूज़र ID (वैकल्पिक — खाली छोड़ने पर Subscribed Users)"
                        )
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("onesignal_external_id_input")
            )

            if (!lastStatus.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lastStatus!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveOneSignalSettings(
                            appId = appIdInput,
                            restApiKey = restKeyInput,
                            targetExternalUserId = externalUserIdInput,
                            enabled = enabledSwitch
                        )
                        Toast.makeText(
                            context,
                            trLang(
                                appLanguage,
                                "OneSignal settings saved!",
                                "OneSignal સેટિંગ્સ સેવ થઈ ગયા!",
                                "OneSignal सेटिंग्स सहेजी गईं!"
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("save_onesignal_config_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Save Keys", "કી સેવ કરો", "सेव करें"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.saveOneSignalSettings(
                            appId = appIdInput,
                            restApiKey = restKeyInput,
                            targetExternalUserId = externalUserIdInput,
                            enabled = enabledSwitch
                        )
                        viewModel.sendOneSignalTestScheduledPush(delayMinutes = 1) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("test_onesignal_push_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ScheduleSend, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Test +1m Push", "ટેસ્ટ +1m પુશ", "टेस्ट +1m पुश"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

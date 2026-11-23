package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.ThemeMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterReminderScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val totalWaterMl by viewModel.todayTotalWaterMl.collectAsState()
    val targetMl by viewModel.waterTargetMl.collectAsState()
    val reminderIntervalHours by viewModel.reminderIntervalHours.collectAsState()
    val waterLogs by viewModel.todayWaterLogs.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val context = LocalContext.current

    val waterNotifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val msg = if (granted) {
            "Water notifications enabled outside the app!"
        } else {
            "Enable notification permission to receive water reminders outside the app."
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    val progress = (totalWaterMl.toFloat() / targetMl.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "waterProgress")
    val glassesCount = totalWaterMl / 250
    val targetGlasses = targetMl / 250

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

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
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = tr("Water Reminder", "પાણી રિમાઇન્ડર", "पानी रिमाइंडर"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr(
                                    "Daily Hydration & Headache Care",
                                    "રોજિંદા પાણી અને માથાના દુખાવાની સંભાળ",
                                    "दैनिक जल स्तर और सिरदर्द देखभाल"
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
                        modifier = Modifier.testTag("water_theme_toggle_button")
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
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.STEP_COUNTER) },
                        modifier = Modifier.testTag("water_open_step_counter_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = stringResource(R.string.step_counter_title),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                waterNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.triggerWaterReminderNotification()
                            }
                        },
                        modifier = Modifier.testTag("test_water_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = tr("Test Notification", "ટેસ્ટ નોટિફિકેશન", "टेस्ट नोटिफिकेशन"),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shortcut to Step Counter & Fitness
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setScreen(AppScreen.STEP_COUNTER) }
                        .testTag("water_to_step_counter_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.step_counter_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        "Count steps & calories via hardware sensor",
                                        "હાર્ડવેર સેન્સર વડે પગલાં અને કેલરી ગણો",
                                        "हार्डवेयर सेंसर से कदम और कैलोरी गिनें"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Text(
                            text = tr("Open 🏃", "ખોલો 🏃", "खोलें 🏃"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Main Progress Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("water_progress_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = tr("Today's Intake", "આજનું પાણી", "आज का पानी"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$totalWaterMl / $targetMl ml",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalDrink,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tr(
                                            "$glassesCount / $targetGlasses Glasses",
                                            "$glassesCount / $targetGlasses ગ્લાસ",
                                            "$glassesCount / $targetGlasses गिलास"
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tr(
                                    "${(progress * 100).toInt()}% Completed",
                                    "${(progress * 100).toInt()}% પૂર્ણ",
                                    "${(progress * 100).toInt()}% पूर्ण"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (totalWaterMl >= targetMl) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SafeMint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tr("Goal Achieved!", "લક્ષ્ય પૂરું થયું!", "लक्ष्य पूरा हुआ!"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SafeMint
                                    )
                                }
                            } else {
                                Text(
                                    text = tr(
                                        "Remaining: ${targetMl - totalWaterMl} ml",
                                        "બાકી: ${targetMl - totalWaterMl} ml",
                                        "शेष: ${targetMl - totalWaterMl} ml"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Quick Add Action Buttons
            item {
                Column {
                    Text(
                        text = tr("Quick Log Water", "ઝડપથી પાણી ઉમેરો", "त्वरित पानी जोड़ें"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.logWater(250) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_250ml_water_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LocalDrink, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("+250 ml", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(tr("1 Glass", "૧ ગ્લાસ", "1 गिलास"), fontSize = 10.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.logWater(500) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_500ml_water_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("+500 ml", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(tr("Bottle", "બોટલ", "बोतल"), fontSize = 10.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.logWater(150) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_150ml_water_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("+150 ml", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(tr("Small Glass", "નાનો ગ્લાસ", "छोटा गिलास"), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // Reminder Frequency & Notification Alert Box
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr("Reminder Interval", "રિમાઇન્ડર સમય", "रिमाइंडर अंतराल"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        waterNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.triggerWaterReminderNotification()
                                    }
                                },
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("send_test_alert_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    tr("Test Alert", "ટેસ્ટ એલર્ટ", "टेस्ट अलर्ट"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tr(
                                "How often should we remind you to drink water?",
                                "દર કેટલા કલાકે પાણી પીવાની યાદ અપાવવી?",
                                "कितने घंटे के अंतराल पर पानी पीने की याद दिलाएं?"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 2, 3, 4).forEach { hours ->
                                FilterChip(
                                    selected = reminderIntervalHours == hours,
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            waterNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        viewModel.setReminderInterval(hours)
                                    },
                                    label = { Text(tr("Every ${hours}h", "દર $hours કલાકે", "हर $hours घंटे")) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tr(
                                "Outside-app system notifications & alarms active every ${reminderIntervalHours}h.",
                                "એપની બહાર પણ દર $reminderIntervalHours કલાકે નોટિફિકેશન અને એલાર્મ સક્રિય છે.",
                                "ऐप के बाहर भी हर $reminderIntervalHours घंटे नोटिफिकेशन व अलार्म सक्रिय हैं।"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("water_outside_status_label")
                        )
                    }
                }
            }

            // Health Benefits Note (પાણી પીવાના ફાયદા)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = tr(
                                "💡 Hydration Health Benefits:",
                                "💡 પાણી પીવાના આરોગ્ય ફાયદા:",
                                "💡 पानी पीने के स्वास्थ्य लाभ:"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tr(
                                "• Prevents Headaches: Even 1–2% dehydration can trigger headaches.\n" +
                                    "• Boosts Energy: Reduces fatigue and keeps your mind alert.\n" +
                                    "• Improves Digestion: Helps reduce acidity and supports gut health.\n" +
                                    "• Kidney Protection: Helps prevent kidney stones and urinary discomfort.",
                                "• માથાનો દુખાવો રોકે છે: શરીરમાં ૧-૨% પાણી ઓછું થવાથી માથું દુખવા લાગે છે.\n" +
                                    "• થાક અને નબળાઈ દૂર કરે છે અને મગજને તાજગી આપે છે.\n" +
                                    "• પાચન ક્રિયા સુધારે છે અને એસિડિટી ઘટાડે છે.\n" +
                                    "• કિડની સ્ટોન અને પેશાબની બળતરાથી રક્ષણ આપે છે.",
                                "• सिरदर्द से बचाव: शरीर में 1-2% पानी की कमी से भी सिरदर्द शुरू हो सकता है।\n" +
                                    "• थकान व कमज़ोरी दूर कर दिमाग को ताज़गी देता है।\n" +
                                    "• पाचन क्रिया सुधारता है और एसिडिटी कम करता है।\n" +
                                    "• किडनी स्टोन और यूरिन की जलन से सुरक्षा देता है।"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Today's Logs Header & List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Today's Logs", "આજના રેકોર્ડ", "आज के रिकॉर्ड"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (waterLogs.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { viewModel.clearTodayWater() },
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(tr("Clear Today", "આજનું સાફ કરો", "आज का साफ़ करें"), fontSize = 11.sp)
                        }
                    }
                }
            }

            if (waterLogs.isEmpty()) {
                item {
                    Text(
                        text = tr(
                            "No water logged today yet. Tap the quick buttons above to add.",
                            "આજે હજી કોઈ પાણી નોંધાયેલ નથી. ઉપરના બટન દબાવીને પાણી ઉમેરો.",
                            "आज अभी तक कोई पानी दर्ज नहीं है। ऊपर दिए बटन से जोड़ें।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(waterLogs, key = { it.id }) { log ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = tr(
                                            "+${log.amountMl} ml water",
                                            "+${log.amountMl} ml પાણી",
                                            "+${log.amountMl} ml पानी"
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = timeFormat.format(Date(log.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteWaterLog(log) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = tr("Delete", "કાઢી નાખો", "हटाएं"),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

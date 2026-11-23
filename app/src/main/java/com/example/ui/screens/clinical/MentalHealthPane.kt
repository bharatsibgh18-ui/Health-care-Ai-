package com.example.ui.screens.clinical

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.MediAssistViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MentalHealthPane(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingMentalHealth.collectAsState()
    val activeCheckIn by viewModel.activeMentalHealthCheckIn.collectAsState()
    val crisisActive by viewModel.activeMentalHealthCrisis.collectAsState()
    val checkIns by viewModel.mentalHealthCheckIns.collectAsState()

    var journalText by remember { mutableStateOf("") }
    var stressSlider by remember { mutableIntStateOf(5) }
    var sleepHours by remember { mutableFloatStateOf(6.5f) }
    var anxietyDays by remember { mutableIntStateOf(3) }
    var facialPhotoUri by remember { mutableStateOf<Uri?>(null) }

    var isListening by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var breathingActive by remember { mutableStateOf(false) }
    var breathPhaseSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(breathingActive) {
        if (breathingActive) {
            var tick = 0
            while (breathingActive) {
                breathPhaseSeconds = tick % 19 // 4 inhale + 7 hold + 8 exhale = 19s
                delay(1000L)
                tick++
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            facialPhotoUri = uri
        }
    }

    val speechFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                journalText = if (journalText.isBlank()) spoken else "$journalText $spoken"
            }
        }
    }

    DisposableEffect(Unit) {
        val sr = if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try { SpeechRecognizer.createSpeechRecognizer(context) } catch (_: Exception) { null }
        } else null
        recognizer = sr
        onDispose {
            try { sr?.destroy() } catch (_: Exception) {}
        }
    }

    val startVoiceCheckIn: () -> Unit = {
        val langTag = com.example.ui.theme.AppLanguages.speechTagFor(selectedLanguage)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                trLang(
                    selectedLanguage,
                    "Share how you are feeling today...",
                    "આજે તમે કેવું અનુભવી રહ્યા છો તે બોલો...",
                    "आज आप कैसा महसूस कर रहे हैं, बोलें..."
                )
            )
        }
        val sr = recognizer
        if (sr != null) {
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening = true }
                override fun onBeginningOfSpeech() { isListening = true }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }
                override fun onError(error: Int) {
                    isListening = false
                    try { speechFallbackLauncher.launch(intent) } catch (_: Exception) {}
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!spoken.isNullOrBlank()) {
                        journalText = if (journalText.isBlank()) spoken else "$journalText $spoken"
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            try {
                sr.startListening(intent)
            } catch (_: Exception) {
                isListening = false
                try { speechFallbackLauncher.launch(intent) } catch (_: Exception) {}
            }
        } else {
            try { speechFallbackLauncher.launch(intent) } catch (_: Exception) {}
        }
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceCheckIn()
        else Toast.makeText(
            context,
            trLang(selectedLanguage, "Microphone permission needed", "માઇક્રોફોન પરવાનગી જરૂરી છે", "माइक्रोफ़ोन अनुमति आवश्यक है"),
            Toast.LENGTH_SHORT
        ).show()
    }

    val dialHelpline: (String) -> Unit = { number ->
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            "Mental Health Support & AI Therapy",
                            "માનસિક સ્વાસ્થ્ય સપોર્ટ & પ્રાથમિક કાઉન્સેલિંગ",
                            "मानसिक स्वास्थ्य सहायता और प्राथमिक काउंसलिंग"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Analyze voice tone, facial cues & mood metrics to detect stress/anxiety and deliver calm therapy",
                            "અવાજ, ચહેરાના હાવભાવ અને ઊંઘ પરથી ડિપ્રેશન/ચિંતા ઓળખી પ્રાથમિક કાઉન્સેલિંગ મેળવો",
                            "आवाज़, चेहरे के भाव और तनाव के आधार पर चिंता/अवसाद की पहचान और प्राथमिक परामर्श"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Crisis Escalation Banner
        if (crisisActive) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmergencyRed.copy(alpha = 0.14f)),
                modifier = Modifier.fillMaxWidth().testTag("mh_crisis_banner")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tr(
                            "🚨 You Are Not Alone — 24/7 Confidential Helpline",
                            "🚨 તમે એકલા નથી — ૨૪×૭ મફત માનસિક સ્વાસ્થ્ય હેલ્પલાઇન",
                            "🚨 आप अकेले नहीं हैं — 24×7 गोपनीय हेल्पलाइन"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmergencyRed
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { dialHelpline("14416") },
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Tele-MANAS 14416", "Tele-MANAS 14416", "Tele-MANAS 14416"))
                        }
                        OutlinedButton(
                            onClick = { dialHelpline("112") },
                            modifier = Modifier.weight(0.75f).height(48.dp)
                        ) {
                            Text(tr("Call 112", "કૉલ 112", "कॉल 112"))
                        }
                    }
                }
            }
        }

        // Multimodal Check-In Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = journalText,
                    onValueChange = { journalText = it },
                    label = { Text(tr("Share how you feel (Voice / Text)", "તમારી મનની વાત બોલો અથવા લખો", "अपनी भावनाएं बोलें या लिखें")) },
                    placeholder = {
                        Text(
                            tr(
                                "e.g. Feeling overwhelmed at work, trouble sleeping, heart racing from stress...",
                                "દા.ત. કામનો તણાવ લાગે છે, રાત્રે ઊંઘ આવતી નથી, મનમાં ગભરામણ રહે છે...",
                                "जैसे: काम का बहुत तनाव है, नींद नहीं आ रही, घबराहट महसूस हो रही है..."
                            )
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("mh_journal_input")
                )

                // Voice + Photo Expression Picker
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            if (isListening) {
                                try { recognizer?.stopListening() } catch (_: Exception) {}
                                isListening = false
                            } else {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (granted) startVoiceCheckIn() else audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        colors = if (isListening) ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("mh_voice_btn")
                    ) {
                        Icon(if (isListening) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isListening) tr("Listening...", "સાંભળે છે...", "सुन रहा है...") else tr("Voice Input", "અવાજથી બોલો", "आवाज़ से बोलें"))
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("mh_face_photo_btn")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Expression Photo", "ચહેરાનો ફોટો", "चेहरे की फ़ोटो"))
                    }
                }

                facialPhotoUri?.let { uri ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = uri,
                            contentDescription = tr("Selected expression photo", "પસંદ કરેલ ચહેરાનો ફોટો", "चयनित चेहरे की फ़ोटो"),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tr("Facial expression cue attached", "ચહેરાના હાવભાવ ફોટો જોડેલ છે", "चेहरे के भाव की फ़ोटो संलग्न"),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { facialPhotoUri = null }) {
                            Icon(Icons.Default.Close, contentDescription = tr("Remove photo", "ફોટો દૂર કરો", "फ़ोटो हटाएं"))
                        }
                    }
                }

                // Quantitative Sliders
                Text(
                    text = tr("Stress Level: $stressSlider / 10", "તણાવનું સ્તર: $stressSlider / ૧૦", "तनाव स्तर: $stressSlider / 10"),
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = stressSlider.toFloat(),
                    onValueChange = { stressSlider = it.roundToInt() },
                    valueRange = 1f..10f,
                    steps = 8
                )

                Text(
                    text = tr(
                        "Sleep Duration: ${String.format(Locale.US, "%.1f", sleepHours)} hrs/night",
                        "ઊંઘના કલાક: ${String.format(Locale.US, "%.1f", sleepHours)} કલાક",
                        "नींद की अवधि: ${String.format(Locale.US, "%.1f", sleepHours)} घंटे"
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = sleepHours,
                    onValueChange = { sleepHours = (it * 2).roundToInt() / 2f },
                    valueRange = 2f..11f
                )

                Text(
                    text = tr(
                        "Anxious / Low-Mood Days (Past 2 Weeks): $anxietyDays days",
                        "છેલ્લા ૨ અઠવાડિયામાં ચિંતા/ઉદાસીના દિવસો: $anxietyDays દિવસ",
                        "पिछले 2 हफ्तों में चिंता/उदासी के दिन: $anxietyDays दिन"
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = anxietyDays.toFloat(),
                    onValueChange = { anxietyDays = it.roundToInt() },
                    valueRange = 0f..14f
                )

                Button(
                    onClick = {
                        viewModel.analyzeAndSaveMentalHealth(
                            context = context,
                            transcript = journalText,
                            stressSlider = stressSlider,
                            sleepHours = sleepHours,
                            anxietyDays = anxietyDays,
                            facialPhotoUri = facialPhotoUri
                        )
                    },
                    enabled = !isAnalyzing,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("mh_analyze_btn")
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(tr("Analyze & Get Primary Therapy", "વિશ્લેષણ કરો & કાઉન્સેલિંગ મેળવો", "विश्लेषण करें और काउंसलिंग पाएं"))
                }
            }
        }

        // Guided 4-7-8 Calming Breathing Tool
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr("Guided 4-7-8 Anxiety Calming Breath", "તણાવ ઘટાડવા ૪-૭-૮ શ્વાસ કસરત", "तनाव कम करने का 4-7-8 श्वास अभ्यास"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { breathingActive = !breathingActive },
                        modifier = Modifier.testTag("mh_breath_toggle_btn")
                    ) {
                        Text(if (breathingActive) tr("Stop", "રોકો", "रोकें") else tr("Start", "શરૂ કરો", "शुरू करें"))
                    }
                }
                if (breathingActive) {
                    val phaseLabel = when {
                        breathPhaseSeconds < 4 -> tr("Inhale Slowly (${4 - breathPhaseSeconds}s)", "ધીમે ઊંડો શ્વાસ લો (${4 - breathPhaseSeconds} સે.)", "धीरे गहरी सांस लें (${4 - breathPhaseSeconds}s)")
                        breathPhaseSeconds < 11 -> tr("Hold Gently (${11 - breathPhaseSeconds}s)", "શ્વાસ રોકી રાખો (${11 - breathPhaseSeconds} સે.)", "सांस रोककर रखें (${11 - breathPhaseSeconds}s)")
                        else -> tr("Exhale Completely (${19 - breathPhaseSeconds}s)", "ધીમે બહાર છોડો (${19 - breathPhaseSeconds} સે.)", "धीरे से छोड़ें (${19 - breathPhaseSeconds}s)")
                    }
                    Text(phaseLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    LinearProgressIndicator(
                        progress = { (breathPhaseSeconds + 1) / 19f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // Active Counseling Card
        activeCheckIn?.let { res ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SafeMint.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth().testTag("mh_result_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${res.moodLabel} • ${tr("Stress Index", "તણાવ સ્કોર", "तनाव स्कोर")}: ${res.stressScore}/100",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SafeMint
                    )
                    LinearProgressIndicator(
                        progress = { res.stressScore / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Text(res.counselingGuidance, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Check-In History
        if (checkIns.isNotEmpty()) {
            Text(
                text = tr("Wellness Check-In Journal (${checkIns.size})", "માનસિક સ્વાસ્થ્ય ડાયરી (${checkIns.size})", "मानसिक स्वास्थ्य डायरी (${checkIns.size})"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            val fmt = remember { SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()) }
            checkIns.take(5).forEach { entry ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${entry.moodLabel} (${entry.stressScore}/100)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(fmt.format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(entry.spokenTranscript, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                        IconButton(onClick = { viewModel.deleteMentalHealthCheckIn(entry) }) {
                            Icon(Icons.Default.Delete, contentDescription = tr("Delete", "કાઢો", "हटाएं"), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

package com.example.ui.screens.clinical

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.MediAssistViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EhrAutomationPane(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isOrganizing by viewModel.isOrganizingEhr.collectAsState()
    val activeRecord by viewModel.activeEhrRecord.collectAsState()
    val savedRecords by viewModel.ehrRecords.collectAsState()

    var patientName by remember(userProfile) { mutableStateOf(userProfile?.fullName ?: "Patient") }
    var rawNotes by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val speechFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                rawNotes = if (rawNotes.isBlank()) spoken else "$rawNotes\n$spoken"
            }
        }
    }

    DisposableEffect(Unit) {
        val sr = if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                SpeechRecognizer.createSpeechRecognizer(context)
            } catch (_: Exception) {
                null
            }
        } else null
        recognizer = sr
        onDispose {
            try { sr?.destroy() } catch (_: Exception) {}
        }
    }

    val startDictation: () -> Unit = {
        val langTag = com.example.ui.theme.AppLanguages.speechTagFor(selectedLanguage)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                trLang(
                    selectedLanguage,
                    "Dictate patient history, vitals & meds...",
                    "દર્દીની વિગતો, BP અને દવાઓ બોલો...",
                    "मरीज का विवरण, BP और दवाइयां बोलें..."
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
                        rawNotes = if (rawNotes.isBlank()) spoken else "$rawNotes\n$spoken"
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
        if (granted) startDictation()
        else Toast.makeText(
            context,
            trLang(selectedLanguage, "Microphone permission required", "માઇક્રોફોન પરવાનગી જરૂરી છે", "माइक्रोफ़ोन अनुमति आवश्यक है"),
            Toast.LENGTH_SHORT
        ).show()
    }

    val shareEhrText: (String, String) -> Unit = { title, content ->
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "EHR Record: $title")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(shareIntent, title))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Banner
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
                        Icon(Icons.Default.FolderShared, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            "Electronic Health Records (EHR) Automation",
                            "ઇલેક્ટ્રોનિક હેલ્થ રેકોર્ડ્સ (EHR) ઓટોમેશન",
                            "इलेक्ट्रॉनिक हेल्थ रिकॉर्ड्स (EHR) ऑटोमेशन"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "NLP auto-organizes dictated notes, allergies, vitals & medications into structured EHR files",
                            "ડૉક્ટરનો સમય બચાવવા NLP દ્વારા દર્દીની વિગતો, તપાસ અને દવાઓનું ડિજિટલ EHR ફોર્મેટિંગ",
                            "डॉक्टर का समय बचाने हेतु NLP द्वारा मरीज के नोट्स, जांच व दवाइयों का डिजिटल EHR आयोजन"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Clinical Intake Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text(tr("Patient Full Name", "દર્દીનું પૂરું નામ", "मरीज का पूरा नाम")) },
                    modifier = Modifier.fillMaxWidth().testTag("ehr_patient_name_input")
                )

                OutlinedTextField(
                    value = rawNotes,
                    onValueChange = { rawNotes = it },
                    label = { Text(tr("Unstructured Patient Notes / Dictation", "દર્દીની વિગતો / નોટ્સ", "अव्यवस्थित क्लिनिकल नोट्स")) },
                    placeholder = {
                        Text(
                            tr(
                                "e.g. Pt Ramesh 48y, BP 142/90, HbA1c 7.4%, dry cough 4 days, Azithromycin 500mg once daily...",
                                "દા.ત. દર્દી રમેશ 48 વર્ષ, BP 142/90, HbA1c 7.4%, ૪ દિવસથી ઉધરસ, Azithromycin 500mg...",
                                "जैसे: रमेश 48 वर्ष, BP 142/90, HbA1c 7.4%, 4 दिन से खांसी, Azithromycin 500mg..."
                            )
                        )
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag("ehr_raw_notes_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            if (isListening) {
                                try { recognizer?.stopListening() } catch (_: Exception) {}
                                isListening = false
                            } else {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (granted) startDictation() else audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        colors = if (isListening) ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("ehr_dictate_btn")
                    ) {
                        Icon(if (isListening) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isListening) tr("Listening...", "સાંભળે છે...", "सुन रहा है...")
                            else tr("Voice Type", "અવાજથી લખો", "बोलकर लिखें")
                        )
                    }

                    Button(
                        onClick = { viewModel.organizeAndSaveEhr(patientName, rawNotes) },
                        enabled = rawNotes.isNotBlank() && !isOrganizing,
                        modifier = Modifier.weight(1.25f).height(48.dp).testTag("ehr_organize_btn")
                    ) {
                        if (isOrganizing) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(tr("Auto-Organize EHR", "EHR ગોઠવો", "EHR व्यवस्थित करें"))
                    }
                }
            }
        }

        // Latest Organized EHR Output
        activeRecord?.let { rec ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SafeMint.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth().testTag("ehr_result_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${rec.patientName} • ${rec.category}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SafeMint
                            )
                            Text(
                                text = tr("Tags: ${rec.diagnosisTags}", "ટેગ્સ: ${rec.diagnosisTags}", "टैग्स: ${rec.diagnosisTags}"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { shareEhrText(rec.patientName, rec.structuredSummary) }) {
                            Icon(Icons.Default.Share, contentDescription = tr("Share EHR", "EHR શેર કરો", "EHR शेयर करें"), tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    AssistChip(
                        onClick = {},
                        label = { Text(tr("Rx Detected: ${rec.medicationsDetected}", "દવા: ${rec.medicationsDetected}", "दवा: ${rec.medicationsDetected}")) }
                    )

                    Text(rec.structuredSummary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Saved EHR Archive
        if (savedRecords.isNotEmpty()) {
            Text(
                text = tr(
                    "Organized EHR Vault (${savedRecords.size})",
                    "સાચવેલા ડિજિટલ EHR રેકોર્ડ્સ (${savedRecords.size})",
                    "सहेजे गए EHR रिकॉर्ड (${savedRecords.size})"
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            val fmt = remember { SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()) }
            savedRecords.take(8).forEach { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${item.patientName} • ${item.category}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(fmt.format(Date(item.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { shareEhrText(item.patientName, item.structuredSummary) }) {
                                Icon(Icons.Default.Share, contentDescription = tr("Share", "શેર", "शेयर"), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteEhrRecord(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = tr("Delete", "કાઢો", "हटाएं"), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Text(item.structuredSummary, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

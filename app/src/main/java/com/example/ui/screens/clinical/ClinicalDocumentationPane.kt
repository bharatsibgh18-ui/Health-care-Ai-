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
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PictureAsPdf
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
import com.example.utils.ClinicalNotePdfHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClinicalDocumentationPane(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isGenerating by viewModel.isGeneratingClinicalDoc.collectAsState()
    val latestDoc by viewModel.latestParsedClinicalDoc.collectAsState()
    val savedNotes by viewModel.clinicalNotes.collectAsState()

    var patientName by remember(userProfile) { mutableStateOf(userProfile?.fullName ?: "Patient") }
    var transcript by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val speechFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                transcript = if (transcript.isBlank()) spoken else "$transcript\n$spoken"
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
            try {
                sr?.destroy()
            } catch (_: Exception) {}
        }
    }

    val startListeningAction: () -> Unit = {
        val langTag = com.example.ui.theme.AppLanguages.speechTagFor(selectedLanguage)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                trLang(
                    selectedLanguage,
                    "Record doctor-patient consultation...",
                    "ડૉક્ટર અને દર્દીની વાતચીત રેકોર્ડ કરો...",
                    "डॉक्टर और मरीज की बातचीत रिकॉर्ड करें..."
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
                    try {
                        speechFallbackLauncher.launch(intent)
                    } catch (_: Exception) {}
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        transcript = if (transcript.isBlank()) text else "$transcript\n$text"
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

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListeningAction()
        } else {
            Toast.makeText(
                context,
                trLang(
                    selectedLanguage,
                    "Microphone permission required for voice dictation",
                    "વાતચીત રેકોર્ડ કરવા માટે માઇક્રોફોન પરવાનગી જરૂરી છે",
                    "आवाज़ रिकॉर्ड करने के लिए माइक्रोफ़ोन अनुमति आवश्यक है"
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(
                            "Automated Clinical Documentation (NLP)",
                            "ઓટોમેટેડ ક્લિનિકલ નોટ્સ & Rx (NLP)",
                            "स्वचालित क्लिनिकल डॉक्यूमेंटेशन (NLP)"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Dictate or paste doctor-patient dialogue to auto-generate SOAP notes, reports & prescriptions",
                            "ડૉક્ટર-દર્દીની વાતચીત પરથી ઓટોમેટિક મેડિકલ નોટ્સ, રિપોર્ટ અને પ્રિસ્ક્રિપ્શન બનાવો",
                            "डॉक्टर-मरीज बातचीत से स्वतः SOAP नोट्स, रिपोर्ट और पर्चा तैयार करें"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Input Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text(tr("Patient Name", "દર્દીનું નામ", "मरीज का नाम")) },
                    modifier = Modifier.fillMaxWidth().testTag("scribe_patient_name_input")
                )

                OutlinedTextField(
                    value = transcript,
                    onValueChange = { transcript = it },
                    label = { Text(tr("Doctor-Patient Conversation Transcript", "ડૉક્ટર અને દર્દીની વાતચીત", "डॉक्टर-मरीज बातचीत ट्रांसक्रिप्ट")) },
                    placeholder = {
                        Text(
                            tr(
                                "Dictate via mic or type dialogue (symptoms, vitals like BP 130/85, medicines like Paracetamol 500mg after food)...",
                                "માઇકથી બોલો અથવા લખો (લક્ષણો, BP 130/85, દવાઓ જેમ કે Paracetamol 500mg)...",
                                "माइक से बोलें या टाइप करें (लक्षण, BP 130/85, दवाइयां जैसे Paracetamol 500mg)..."
                            )
                        )
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag("scribe_transcript_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            if (isListening) {
                                try { recognizer?.stopListening() } catch (_: Exception) {}
                                isListening = false
                            } else {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (granted) startListeningAction() else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        colors = if (isListening) ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("scribe_mic_btn")
                    ) {
                        Icon(if (isListening) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isListening) tr("Listening...", "સાંભળી રહ્યા છીએ...", "सुन रहा है...")
                            else tr("Dictate Dialogue", "અવાજથી રેકોર્ડ કરો", "बातचीत रिकॉर्ड करें")
                        )
                    }

                    Button(
                        onClick = { viewModel.generateClinicalDocumentation(patientName, transcript) },
                        enabled = transcript.isNotBlank() && !isGenerating,
                        modifier = Modifier.weight(1.2f).height(48.dp).testTag("scribe_generate_btn")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(tr("Generate Notes & Rx", "નોટ્સ & Rx બનાવો", "नोट्स और Rx बनाएं"))
                    }
                }
            }
        }

        // Generated Structured Clinical Documentation
        latestDoc?.let { doc ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SafeMint.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth().testTag("scribe_result_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = tr("Generated SOAP Note", "ઓટોમેટેડ SOAP નોટ", "स्वचालित SOAP नोट"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SafeMint
                    )
                    Text(doc.soapNote, style = MaterialTheme.typography.bodySmall)

                    Text(
                        text = tr("Medical Report Summary", "મેડિકલ રિપોર્ટ સારાંશ", "मेडिकल रिपोर्ट सारांश"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(doc.medicalReport, style = MaterialTheme.typography.bodySmall)

                    Text(
                        text = tr("Draft Prescription (Rx)", "ડ્રાફ્ટ પ્રિસ્ક્રિપ્શન (Rx)", "ड्राफ्ट प्रिस्क्रिप्शन (Rx)"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(doc.prescriptionSummary, style = MaterialTheme.typography.bodySmall)

                    if (doc.extractedMedicines.isNotEmpty()) {
                        Button(
                            onClick = {
                                viewModel.addExtractedMedicinesToReminders(doc.extractedMedicines)
                                Toast.makeText(
                                    context,
                                    trLang(
                                        selectedLanguage,
                                        "Added ${doc.extractedMedicines.size} Rx medicine(s) to Reminders!",
                                        "${doc.extractedMedicines.size} દવાઓ રિમાઇન્ડરમાં ઉમેરાઈ!",
                                        "${doc.extractedMedicines.size} दवाइयाँ रिमाइंडर में जोड़ी गईं!"
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("scribe_add_rx_reminders_btn")
                        ) {
                            Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Schedule Rx in Medicine Reminders", "દવાઓ રિમાઇન્ડરમાં ઉમેરો", "दवाइयाँ रिमाइंडर में जोड़ें"))
                        }
                    }
                }
            }
        }

        // Saved Clinical Notes Archive
        if (savedNotes.isNotEmpty()) {
            Text(
                text = tr("Saved Clinical Notes (${savedNotes.size})", "સાચવેલ ક્લિનિકલ નોટ્સ (${savedNotes.size})", "सहेजे गए क्लिनिकल नोट्स (${savedNotes.size})"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            val fmt = remember { SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()) }
            savedNotes.take(5).forEach { note ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.patientName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(fmt.format(Date(note.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { ClinicalNotePdfHelper.shareClinicalNotePdf(context, note) }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = tr("Share PDF", "PDF શેર કરો", "PDF शेयर करें"), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteClinicalNote(note) }) {
                                Icon(Icons.Default.Delete, contentDescription = tr("Delete", "કાઢી નાખો", "हटाएं"), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Text(
                            text = note.medicalReport,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.R
import com.example.ui.theme.ThemeMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.EmergencyHeaderBanner
import com.example.ui.components.SymptomContextSheet
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.OnEmergencyRed
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MediAssistViewModel
import com.example.ui.viewmodel.SymptomDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTriageScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val emergencyTriggered by viewModel.emergencyAlertTriggered.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val speakingMessageId by viewModel.speakingMessageId.collectAsState()
    val autoSpeakEnabled by viewModel.autoSpeakEnabled.collectAsState()
    val showSymptomSheet by viewModel.showSymptomContextSheet.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var attachedContext by remember { mutableStateOf<SymptomDetails?>(null) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    val apkScope = rememberCoroutineScope()

    // SpeechRecognizer setup
    var speechRecognizer by remember { mutableStateOf<android.speech.SpeechRecognizer?>(null) }

    // System Speech Recognizer Activity Launcher as a reliable fallback
    val speechPromptLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val text = spokenMatches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                val current = viewModel.inputText.value
                val combined = if (current.isBlank()) text else "$current $text"
                viewModel.updateInputText(combined)
                android.widget.Toast.makeText(
                    context,
                    trLang(
                        selectedLanguage,
                        "Voice input: $text",
                        "અવાજ મળ્યો: $text",
                        "आवाज़ प्राप्त हुई: $text"
                    ),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val createSpeechIntent: () -> android.content.Intent = {
        val langTag = when (selectedLanguage) {
            "Gujarati" -> "gu-IN"
            "Hindi" -> "hi-IN"
            else -> "en-IN"
        }
        android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("gu-IN", "hi-IN", "en-IN", "en-US"))
            putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(
                android.speech.RecognizerIntent.EXTRA_PROMPT,
                trLang(
                    selectedLanguage,
                    "Describe your symptoms...",
                    "તમારા લક્ષણો બોલો...",
                    "अपने लक्षण बोलें..."
                )
            )
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        val recognizer = if (android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                android.speech.SpeechRecognizer.createSpeechRecognizer(context)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
        speechRecognizer = recognizer
        onDispose {
            try {
                recognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    val startVoiceListening: () -> Unit = {
        val intent = createSpeechIntent()
        val isServiceAvailable = android.speech.SpeechRecognizer.isRecognitionAvailable(context)

        if (isServiceAvailable) {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(context)
                }

                speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) {
                        isListening = true
                    }
                    override fun onBeginningOfSpeech() {
                        isListening = true
                    }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                    }
                    override fun onError(error: Int) {
                        isListening = false
                        // If background recognizer fails (e.g. no Google Speech Service background binding), fallback to system voice dialog
                        if (error == android.speech.SpeechRecognizer.ERROR_CLIENT ||
                            error == android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                            error == android.speech.SpeechRecognizer.ERROR_SERVER ||
                            error == android.speech.SpeechRecognizer.ERROR_NETWORK ||
                            error == android.speech.SpeechRecognizer.ERROR_AUDIO
                        ) {
                            try {
                                speechPromptLauncher.launch(intent)
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    trLang(
                                        selectedLanguage,
                                        "Speech recognition error ($error). Please type symptoms.",
                                        "અવાજ ઓળખવામાં ભૂલ થઈ. કૃપા કરીને ટાઈપ કરો.",
                                        "आवाज़ पहचानने में त्रुटि ($error)। कृपया टाइप करें।"
                                    ),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else if (error == android.speech.SpeechRecognizer.ERROR_NO_MATCH ||
                            error == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        ) {
                            android.widget.Toast.makeText(
                                context,
                                trLang(
                                    selectedLanguage,
                                    "No speech detected, please try again",
                                    "કંઈ સંભળાયું નહીં, ફરીથી બોલો",
                                    "कोई आवाज़ नहीं सुनी गई, कृपया पुनः प्रयास करें"
                                ),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    override fun onResults(results: android.os.Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                        val recognizedVoiceText = matches?.firstOrNull()
                        if (!recognizedVoiceText.isNullOrBlank()) {
                            val current = viewModel.inputText.value
                            if (current.isBlank()) {
                                viewModel.updateInputText(recognizedVoiceText)
                            } else {
                                viewModel.updateInputText("$current $recognizedVoiceText")
                            }
                            android.widget.Toast.makeText(
                                context,
                                trLang(
                                    selectedLanguage,
                                    "Voice input: $recognizedVoiceText",
                                    "અવાજ મળ્યો: $recognizedVoiceText",
                                    "आवाज़ प्राप्त हुई: $recognizedVoiceText"
                                ),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    override fun onPartialResults(partialResults: android.os.Bundle?) {
                        val matches = partialResults?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            val current = viewModel.inputText.value
                            if (current.isBlank()) {
                                viewModel.updateInputText(partial)
                            }
                        }
                    }
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                })

                speechRecognizer?.startListening(intent)
                isListening = true
                android.widget.Toast.makeText(
                    context,
                    trLang(
                        selectedLanguage,
                        "🎤 Listening... speak now",
                        "🎤 સાંભળી રહ્યું છે... બોલો",
                        "🎤 सुन रहा हूँ... अब बोलें"
                    ),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                isListening = false
                try {
                    speechPromptLauncher.launch(intent)
                } catch (ex: Exception) {
                    android.widget.Toast.makeText(
                        context,
                        trLang(
                            selectedLanguage,
                            "Voice recognition unavailable on this device",
                            "આ ઉપકરણ પર અવાજ ઓળખ ઉપલબ્ધ નથી",
                            "इस डिवाइस पर आवाज़ पहचान उपलब्ध नहीं है"
                        ),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            // SpeechRecognizer service not directly available; use system RecognizerIntent directly
            try {
                speechPromptLauncher.launch(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    trLang(
                        selectedLanguage,
                        "Voice recognition service not found on device",
                        "આ ઉપકરણ પર વોઇસ સર્વિસ મળી નથી",
                        "डिवाइस पर वॉयस सेवा नहीं मिली"
                    ),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Permission launcher for RECORD_AUDIO
    val audioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceListening()
        } else {
            android.widget.Toast.makeText(
                context,
                trLang(
                    selectedLanguage,
                    "Microphone permission is required for voice input",
                    "માઇક્રોફોન પરમિશન જરૂરી છે",
                    "वॉयस इनपुट के लिए माइक्रोफ़ोन अनुमति आवश्यक है"
                ),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    // Photo picker launcher (zero-permission Android Photo Picker)
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    viewModel.setSelectedImage(uri.toString(), base64String, mimeType)
                    android.widget.Toast.makeText(
                        context,
                        trLang(
                            selectedLanguage,
                            "Photo attached! Ready for AI analysis",
                            "ફોટો સિલેક્ટ થયો! AI એનાલિસિસ માટે તૈયાર છે",
                            "फ़ोटो संलग्न! AI विश्लेषण के लिए तैयार है"
                        ),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {
                // Safe fallback
            }
        }
    }

    // PDF Lab Report Picker Launcher (zero-permission Storage Access Framework)
    val pdfReportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            android.widget.Toast.makeText(
                context,
                trLang(
                    selectedLanguage,
                    "Reading PDF lab report...",
                    "PDF રિપોર્ટ વંચાઈ રહ્યો છે...",
                    "PDF लैब रिपोर्ट पढ़ी जा रही है..."
                ),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            viewModel.analyzePdfReportInChat(context, uri)
        }
    }

    // Camera Capture Launcher for live photos of ailment/lesion/wound
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                val bytes = stream.toByteArray()
                val base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val tempFile = java.io.File(context.cacheDir, "symptom_capture_${System.currentTimeMillis()}.jpg")
                tempFile.writeBytes(bytes)
                val uriString = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                ).toString()
                viewModel.setSelectedImage(uriString, base64String, "image/jpeg")
                android.widget.Toast.makeText(
                    context,
                    trLang(
                        selectedLanguage,
                        "Photo captured! AI ready to analyze",
                        "ફોટો લેવાયો! AI હવે એનાલિસિસ કરશે",
                        "फ़ोटो लिया गया! AI विश्लेषण के लिए तैयार है"
                    ),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (_: Exception) {
                android.widget.Toast.makeText(
                    context,
                    trLang(
                        selectedLanguage,
                        "Error saving captured photo",
                        "ફોટો સેવ કરવામાં ભૂલ આવી",
                        "फ़ोटो सहेजने में त्रुटि"
                    ),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            android.widget.Toast.makeText(
                context,
                trLang(
                    selectedLanguage,
                    "Camera permission required to take photo",
                    "કેમેરા પરમિશન જરૂરી છે",
                    "फ़ोटो लेने के लिए कैमरा अनुमति आवश्यक है"
                ),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val emergencyNumber = userProfile?.emergencyContactPhone?.ifBlank { "112" } ?: "112"

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showSymptomSheet) {
        SymptomContextSheet(
            onDismiss = { viewModel.toggleSymptomContextSheet(false) },
            onSubmit = { details ->
                attachedContext = details
                viewModel.toggleSymptomContextSheet(false)
            }
        )
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr(
                            "Attach Photo or PDF Report",
                            "ફોટો અથવા PDF રિપોર્ટ ઉમેરો",
                            "फ़ोटो या PDF रिपोर्ट जोड़ें"
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = tr(
                            "Capture or select a clear photo of an ailment, or upload a PDF lab report directly for AI explanation.",
                            "બીમારીનો ફોટો પાડો અથવા લેબોરેટરીમાંથી મળેલો PDF રિપોર્ટ સીધો અપલોડ કરો જેથી AI સરળ ભાષામાં સમજાવી શકે.",
                            "लक्षण की स्पष्ट फ़ोटो लें या लैब की PDF रिपोर्ट सीधे अपलोड करें ताकि AI सरल भाषा में समझा सके।"
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Option 1: Live Camera
                    Surface(
                        onClick = {
                            showPhotoSourceDialog = false
                            val hasCameraPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.CAMERA
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasCameraPerm) {
                                cameraLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr(
                                        "📸 Take Live Photo with Camera",
                                        "📸 કેમેરાથી ફોટો પાડો",
                                        "📸 कैमरे से लाइव फ़ोटो लें"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = tr(
                                        "Capture directly with device camera",
                                        "અસરગ્રસ્ત ભાગનો સીધો ફોટો લો",
                                        "डिवाइस कैमरे से सीधे फ़ोटो लें"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Option 2: Choose from Gallery
                    Surface(
                        onClick = {
                            showPhotoSourceDialog = false
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr(
                                        "🖼️ Choose from Gallery",
                                        "🖼️ ગેલેરીમાંથી પસંદ કરો",
                                        "🖼️ गैलरी से चुनें"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        "Select existing image from device",
                                        "સાચવેલો ફોટો અપલોડ કરો",
                                        "डिवाइस से सहेजी गई फ़ोटो चुनें"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 2B: Direct PDF Lab Report Upload
                    Surface(
                        onClick = {
                            showPhotoSourceDialog = false
                            pdfReportLauncher.launch(arrayOf("application/pdf"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_upload_pdf_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr(
                                        "📑 Upload PDF Lab Report",
                                        "📑 PDF લેબ રિપોર્ટ અપલોડ કરો",
                                        "📑 PDF लैब रिपोर्ट अपलोड करें"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = tr(
                                        "Select multi-page lab PDF for AI chat analysis",
                                        "લેબોરેટરીની PDF ફાઈલ સીધી વાંચી સમજૂતી મેળવો",
                                        "AI चैट विश्लेषण के लिए लैब की PDF फ़ाइल चुनें"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Option 3: Full OCR Report Scanner
                    Surface(
                        onClick = {
                            showPhotoSourceDialog = false
                            viewModel.setScreen(AppScreen.REPORT_SCANNER)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_open_ocr_scanner_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr(
                                        "📄 Medical Report & Rx Scanner (OCR)",
                                        "📄 મેડિકલ રિપોર્ટ / Rx સ્કેનર (OCR)",
                                        "📄 मेडिकल रिपोर्ट और पर्चा स्कैनर (OCR)"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = tr(
                                        "Scan lab tests or prescriptions for AI explanation",
                                        "લખાણ વાંચી AI દ્વારા સરળ સમજૂતી મેળવો",
                                        "लैब टेस्ट या पर्चे स्कैन कर सरल AI व्याख्या पाएं"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Option 4: Step Counter & Fitness
                    Surface(
                        onClick = {
                            showPhotoSourceDialog = false
                            viewModel.setScreen(AppScreen.STEP_COUNTER)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_open_step_counter_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr(
                                        "🏃 Step Counter & Fitness Tracking",
                                        "🏃 સ્ટેપ કાઉન્ટર અને ફિટનેસ",
                                        "🏃 स्टेप काउंटर और फिटनेस ट्रैकिंग"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = tr(
                                        "Track daily steps, calories & distance",
                                        "ચાલવાના પગલાં અને કેલરી ગણો",
                                        "रोज के कदम, कैलोरी और दूरी ट्रैक करें"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    // Option 5: Smart Vitals Tracker (BP, Sugar, HR)
                    Surface(
                        onClick = {
                            showPhotoSourceDialog = false
                            viewModel.setScreen(AppScreen.VITALS_TRACKER)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_open_vitals_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonitorHeart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr(
                                        "💓 Vitals Tracker (BP, Sugar, HR)",
                                        "💓 વાઇટલ્સ ટ્રેકર (BP, Sugar, HR, SpO2)",
                                        "💓 वाइटल्स ट्रैकर (BP, शुगर, HR)"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = tr(
                                        "Track blood pressure, sugar, weight & heart rate trends",
                                        "રોજિંદા BP, બ્લડ સુગર, હાર્ટ રેટ અને વજન લોગ કરો",
                                        "ब्लड प्रेशर, शुगर, वजन और हार्ट रेट रिकॉर्ड करें"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Option 6: Family & Caregiver Features
                    Surface(
                        onClick = {
                            showPhotoSourceDialog = false
                            viewModel.setScreen(AppScreen.FAMILY_CARE)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_open_family_care_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr(
                                        "👨\u200D👩\u200D👧 Family Profiles & Caregiver Alert",
                                        "👨\u200D👩\u200D👧 કુટુંબ પ્રોફાઇલ & કેરગિવર એલર્ટ",
                                        "👨\u200D👩\u200D👧 परिवार प्रोफ़ाइल और देखभाल अलर्ट"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = tr(
                                        "Manage elders, children & instant SMS caregiver alerts",
                                        "વડીલો માટે SMS એલર્ટ અને સભ્યોના મેડિકલ રેકોર્ડ",
                                        "बुजुर्गों के लिए SMS अलर्ट और परिवार के मेडिकल रिकॉर्ड"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    // Option 7: Smart Health Ecosystem & AI (Diet, Lab, Tele-Consult)
                    Surface(
                        onClick = {
                            showPhotoSourceDialog = false
                            viewModel.setScreen(AppScreen.SMART_HUB)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_open_smart_hub_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr(
                                        "✨ Smart Health Ecosystem & Diet AI",
                                        "✨ સ્માર્ટ હેલ્થ ઇકોસિસ્ટમ & ડાયેટ AI",
                                        "✨ स्मार्ट हेल्थ इकोसिस्टम और डाइट AI"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = tr(
                                        "AI Diet scanner, doctor video consult & lab tests",
                                        "ડાયેટ સ્કેનર, ટેલી-કન્સલ્ટ અને લેબ બુકિંગ",
                                        "AI डाइट स्कैनर, डॉक्टर वीडियो परामर्श और लैब टेस्ट"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = "Health Care AI",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Compact Language Switcher
                    com.example.ui.components.LanguageSelector(
                        viewModel = viewModel,
                        modifier = Modifier.padding(end = 2.dp),
                        showRefreshButton = false
                    )

                    // Auto-Speak Toggle
                    IconButton(
                        onClick = { viewModel.toggleAutoSpeak() },
                        modifier = Modifier.testTag("auto_speak_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (autoSpeakEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (autoSpeakEnabled) {
                                tr("Auto-Speak Active", "ઓટો-સ્પીક ચાલુ", "ऑटो-स्पीक सक्रिय")
                            } else {
                                tr("Auto-Speak Off", "ઓટો-સ્પીક બંધ", "ऑटो-स्पीक बंद")
                            },
                            tint = if (autoSpeakEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Emergency SOS quick trigger button
                    IconButton(
                        onClick = { viewModel.triggerEmergencyDirect() },
                        modifier = Modifier
                            .testTag("sos_button")
                            .background(EmergencyRedContainer, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = tr("Emergency SOS", "ઇમરજન્સી SOS", "आपातकालीन SOS"),
                            tint = EmergencyRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Overflow Menu for all health modules & settings (prevents TopAppBar crushing)
                    Box {
                        IconButton(
                            onClick = { moreMenuExpanded = true },
                            modifier = Modifier.testTag("more_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = tr(
                                    "More Health Features & Settings",
                                    "વધુ આરોગ્ય સુવિધાઓ અને સેટિંગ્સ",
                                    "अधिक स्वास्थ्य सुविधाएं और सेटिंग्स"
                                ),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        tr(
                                            "Vitals Tracker (BP, Sugar, HR)",
                                            "વાઇટલ્સ ટ્રેકર (BP, Sugar, HR)",
                                            "वाइटल्स ट्रैकर (BP, शुगर, HR)"
                                        )
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    moreMenuExpanded = false
                                    viewModel.setScreen(AppScreen.VITALS_TRACKER)
                                },
                                modifier = Modifier.testTag("open_vitals_button")
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        tr(
                                            "Smart Health Hub & AI",
                                            "સ્માર્ટ હેલ્થ હબ અને AI",
                                            "स्मार्ट हेल्थ हब और AI"
                                        )
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    moreMenuExpanded = false
                                    viewModel.setScreen(AppScreen.SMART_HUB)
                                },
                                modifier = Modifier.testTag("open_smart_hub_button")
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.scan_report_fab)) },
                                leadingIcon = { Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    moreMenuExpanded = false
                                    viewModel.setScreen(AppScreen.REPORT_SCANNER)
                                },
                                modifier = Modifier.testTag("open_scanner_button")
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.step_counter_title)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    moreMenuExpanded = false
                                    viewModel.setScreen(AppScreen.STEP_COUNTER)
                                },
                                modifier = Modifier.testTag("open_step_counter_button")
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.nav_medicine_reminder)) },
                                leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    moreMenuExpanded = false
                                    viewModel.setScreen(AppScreen.MEDICINE_REMINDER)
                                },
                                modifier = Modifier.testTag("open_medicine_menu_button")
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        tr(
                                            "Family Care & Caregiver Alert",
                                            "કુટુંબ સંભાળ અને કેરગિવર એલર્ટ",
                                            "परिवार देखभाल और अलर्ट"
                                        )
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    moreMenuExpanded = false
                                    viewModel.setScreen(AppScreen.FAMILY_CARE)
                                },
                                modifier = Modifier.testTag("open_family_care_button")
                            )
                            HorizontalDivider()
                            val (themeIcon, themeName) = when (themeMode) {
                                ThemeMode.DARK -> Icons.Default.DarkMode to stringResource(R.string.theme_dark)
                                ThemeMode.LIGHT -> Icons.Default.LightMode to stringResource(R.string.theme_light)
                                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto to stringResource(R.string.theme_system)
                            }
                            DropdownMenuItem(
                                text = { Text(tr("Theme: $themeName", "થીમ: $themeName", "थीम: $themeName")) },
                                leadingIcon = { Icon(themeIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    moreMenuExpanded = false
                                    viewModel.toggleThemeMode()
                                },
                                modifier = Modifier.testTag("theme_toggle_button")
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.btn_refresh_language)) },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    moreMenuExpanded = false
                                    viewModel.refreshAppLanguage()
                                },
                                modifier = Modifier.testTag("language_refresh_button")
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        tr(
                                            "Share App (.apk)",
                                            "એપ શેર કરો (.apk)",
                                            "ऐप शेयर करें (.apk)"
                                        )
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    moreMenuExpanded = false
                                    apkScope.launch {
                                        com.example.utils.ApkShareHelper.shareInstalledApk(context, selectedLanguage)
                                    }
                                },
                                modifier = Modifier.testTag("menu_share_apk_button")
                            )
                            DropdownMenuItem(
                                text = { Text(tr("App Info", "એપ માહિતી", "ऐप जानकारी")) },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    moreMenuExpanded = false
                                    viewModel.setScreen(AppScreen.APP_INFO)
                                },
                                modifier = Modifier.testTag("app_info_top_button")
                            )
                        }
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
        ) {
            // Emergency Header Banner if triggered
            EmergencyHeaderBanner(
                visible = emergencyTriggered,
                onDismiss = { viewModel.dismissEmergencyAlert() },
                emergencyNumber = emergencyNumber
            )

            // Legal & Medical Disclaimer Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = tr("Disclaimer", "સૂચના", "अस्वीकरण"),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr(
                            "Guidance is educational and non-diagnostic. Call emergency if severe.",
                            "આ માહિતી ફક્ત માર્ગદર્શન માટે છે, સત્તાવાર નિદાન નથી. ગંભીર સ્થિતિમાં ઇમરજન્સીને કૉલ કરો.",
                            "यह सलाह केवल जानकारी के लिए है, चिकित्सा निदान नहीं। गंभीर स्थिति में आपातकाल को कॉल करें।"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Room Database Personalization Context Banner
            val symptomLogs by viewModel.symptomLogs.collectAsState()
            if (symptomLogs.isNotEmpty() || (userProfile != null && userProfile?.chronicConditions != "None")) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(
                                "Personalized with ${symptomLogs.size} saved symptom records & health profile",
                                "તમારા ${symptomLogs.size} સાચવેલા રેકોર્ડ અને પ્રોફાઇલના આધારે સચોટ માર્ગદર્શન",
                                "आपके ${symptomLogs.size} सहेजे गए रिकॉर्ड और स्वास्थ्य प्रोफ़ाइल के आधार पर व्यक्तिगत सलाह"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Chat Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    // Welcome & Quick Starters View
                    WelcomeTriageView(
                        selectedLanguage = selectedLanguage,
                        onSelectSymptom = { symptomPrompt ->
                            viewModel.sendMessage(symptomPrompt)
                        },
                        onOpenQuestionnaire = {
                            viewModel.toggleSymptomContextSheet(true)
                        },
                        onOpenScreen = { screen ->
                            viewModel.setScreen(screen)
                        },
                        onShareApp = {
                            apkScope.launch {
                                com.example.utils.ApkShareHelper.shareInstalledApk(context, selectedLanguage)
                            }
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 8.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChatMessageBubble(
                                message = msg,
                                isSpeaking = isSpeaking,
                                isSpeakingThisMessage = speakingMessageId == msg.id,
                                onSpeakClick = { message ->
                                    viewModel.toggleSpeech(message.text, message.id)
                                }
                            )
                        }

                        if (isLoading) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = tr(
                                            "Health Care AI is evaluating your symptoms...",
                                            "AI તમારા લક્ષણોની તપાસ કરી રહ્યું છે...",
                                            "AI आपके लक्षणों का मूल्यांकन कर रहा है..."
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Symptom Chips Carousel when chatting
            if (messages.isNotEmpty()) {
                QuickSymptomChipsRow(
                    onSelect = { symptom ->
                        viewModel.sendMessage(symptom)
                    }
                )
            }

            // Attached Clinical Context Badge
            attachedContext?.let { ctx ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InputChip(
                        selected = true,
                        onClick = { attachedContext = null },
                        label = {
                            Text(
                                tr(
                                    "Context: ${ctx.severity}/10 Severity | ${ctx.duration} | ${ctx.location}",
                                    "વિગત: તીવ્રતા ${ctx.severity}/10 | ${ctx.duration} | ${ctx.location}",
                                    "संदर्भ: तीव्रता ${ctx.severity}/10 | ${ctx.duration} | ${ctx.location}"
                                )
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr("Remove Context", "વિગત દૂર કરો", "संदर्भ हटाएं"),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            // Attached Medical Image Preview & Quick Visual Triage Action Card
            selectedImageUri?.let { uriStr ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        coil.compose.AsyncImage(
                            model = uriStr,
                            contentDescription = tr("Attached medical photo", "જોડેલો મેડિકલ ફોટો", "संलग्न मेडिकल फ़ोटो"),
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr(
                                        "🩺 Medical Photo Ready",
                                        "🩺 ફોટો એનાલિસિસ તૈયાર છે",
                                        "🩺 मेडिकल फ़ोटो विश्लेषण के लिए तैयार है"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = tr(
                                    "AI will inspect the ailment & suggest remedies",
                                    "AI ચામડી/બીમારીનું નિરીક્ષણ કરી દવા અને ઉપાય આપશે",
                                    "AI लक्षणों की जांच करके सुरक्षित उपाय बताएगा"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // One-tap Analyze button
                        Button(
                            onClick = {
                                val prompt = trLang(
                                    selectedLanguage,
                                    "Please clinically inspect this symptom photo, identify what has occurred at this afflicted site, and advise safe remedies and care.",
                                    "આ દર્શાવેલા ફોટાનું ક્લિનિકલ નિરીક્ષણ કરી જણાવો કે અહીં શું થયું છે અને તેના માટે શું ઘરેલુ ઉપાય કે દવા કરવી?",
                                    "कृपया इस फ़ोटो का चिकित्सकीय निरीक्षण करें, बताएं कि यहां क्या हुआ है और इसके लिए सुरक्षित घरेलू उपाय व देखभाल बताएं।"
                                )
                                viewModel.sendMessage(overrideText = prompt, symptomDetails = attachedContext)
                                attachedContext = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("quick_analyze_photo_button")
                        ) {
                            Text(
                                text = tr("Analyze", "એનાલાઇઝ", "जांचें"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearSelectedImage() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr("Remove Image", "ફોટો દૂર કરો", "फ़ोटो हटाएं"),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Active Voice Listening Banner
            if (isListening) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(
                                "🎤 Listening to symptoms... speak now",
                                "🎤 સાંભળી રહ્યું છે... તમારા લક્ષણો બોલો",
                                "🎤 सुन रहा हूँ... अपने लक्षण बोलें"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                try { speechRecognizer?.stopListening() } catch (_: Exception) {}
                                isListening = false
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr("Stop", "રોકો", "रोकें"),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Input Bar with Voice Input & Image Attachment
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Clinical Context Form Questionnaire Button
                    IconButton(
                        onClick = { viewModel.toggleSymptomContextSheet(true) },
                        modifier = Modifier
                            .testTag("open_clinical_questionnaire_button")
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                CircleShape
                            )
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FactCheck,
                            contentDescription = tr("Clinical Context Form", "લક્ષણ વિગત ફોર્મ", "लक्षण विवरण फ़ॉर्म"),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // 2. Image / PDF Attachment Icon
                    IconButton(
                        onClick = {
                            showPhotoSourceDialog = true
                        },
                        modifier = Modifier
                            .testTag("attach_image_button")
                            .background(
                                if (selectedImageUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = tr("Attach Photo or PDF", "ફોટો કે PDF ઉમેરો", "फ़ोटो या PDF जोड़ें"),
                            tint = if (selectedImageUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // 3. Outlined Text Input
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.updateInputText(it) },
                        placeholder = {
                            Text(
                                text = if (isListening) {
                                    tr("Listening...", "સાંભળી રહ્યું છે...", "सुन रहा हूँ...")
                                } else {
                                    stringResource(R.string.chat_input_placeholder)
                                },
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("symptom_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // 4. Voice Input (SpeechRecognizer) Microphone Icon next to Send button
                    IconButton(
                        onClick = {
                            if (isListening) {
                                speechRecognizer?.stopListening()
                                isListening = false
                            } else {
                                val hasAudioPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (hasAudioPermission) {
                                    startVoiceListening()
                                } else {
                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            )
                            .testTag("voice_input_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isListening) {
                                tr("Stop Voice Input", "વોઇસ બંધ કરો", "आवाज़ रोकें")
                            } else {
                                tr("Start Voice Input", "વોઇસ શરૂ કરો", "आवाज़ से लिखें")
                            },
                            tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // 5. Send Button
                    FloatingActionButton(
                        onClick = {
                            if (isListening) {
                                speechRecognizer?.stopListening()
                                isListening = false
                            }
                            viewModel.sendMessage(symptomDetails = attachedContext)
                            attachedContext = null
                        },
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("send_symptom_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = tr("Send", "મોકલો", "भेजें"),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeTriageView(
    selectedLanguage: String = "English",
    onSelectSymptom: (String) -> Unit,
    onOpenQuestionnaire: () -> Unit,
    onOpenScreen: (AppScreen) -> Unit = {},
    onShareApp: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.welcome_screen_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = trLang(
                selectedLanguage,
                "Describe your symptoms to receive structured triage evaluation, clarifying questions, self-care guidance, and specialist recommendations.",
                "તમારા લક્ષણો લખો અથવા બોલો — AI પ્રાથમિક તપાસ, ઘરેલુ ઉપાય અને યોગ્ય ડૉક્ટરનું માર્ગદર્શન આપશે.",
                "अपने लक्षण लिखें या बोलें — AI प्राथमिक जांच, घरेलू उपाय और सही डॉक्टर का मार्गदर्शन देगा।"
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )

        // Quick-access horizontal strip for secondary health tools
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                onClick = { onOpenScreen(AppScreen.STEP_COUNTER) },
                label = { Text(trLang(selectedLanguage, "Steps", "સ્ટેપ્સ", "कदम")) },
                icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("home_quick_steps")
            )
            SuggestionChip(
                onClick = { onOpenScreen(AppScreen.VITALS_TRACKER) },
                label = { Text(trLang(selectedLanguage, "Vitals", "વાઇટલ્સ", "वाइटल्स")) },
                icon = { Icon(Icons.Default.MonitorHeart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("home_quick_vitals")
            )
            SuggestionChip(
                onClick = { onOpenScreen(AppScreen.REPORT_SCANNER) },
                label = { Text(trLang(selectedLanguage, "Rx / PDF", "Rx / PDF", "Rx / PDF")) },
                icon = { Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("home_quick_scanner")
            )
            SuggestionChip(
                onClick = { onOpenScreen(AppScreen.MEDICINE_REMINDER) },
                label = { Text(trLang(selectedLanguage, "Medicines", "દવા યાદ", "दवाइयाँ")) },
                icon = { Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("home_quick_medicine")
            )
            SuggestionChip(
                onClick = { onOpenScreen(AppScreen.SMART_HUB) },
                label = { Text(trLang(selectedLanguage, "Smart Hub", "સ્માર્ટ હબ", "स्मार्ट हब")) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("home_quick_smart_hub")
            )
            SuggestionChip(
                onClick = { onOpenScreen(AppScreen.FAMILY_CARE) },
                label = { Text(trLang(selectedLanguage, "Family", "પરિવાર", "परिवार")) },
                icon = { Icon(Icons.Default.FamilyRestroom, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("home_quick_family")
            )
            SuggestionChip(
                onClick = onShareApp,
                label = { Text(trLang(selectedLanguage, "Share App", "એપ શેર કરો", "ऐप शेयर करें")) },
                icon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("home_quick_share_apk")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = trLang(
                selectedLanguage,
                "Common Symptom Starters:",
                "સામાન્ય લક્ષણો (ઝડપી શરૂઆત):",
                "सामान्य लक्षण (त्वरित शुरुआत):"
            ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        val commonStarters = listOf(
            trLang(
                selectedLanguage,
                "Fever with chills and mild body ache for 2 days",
                "૨ દિવસથી ઠંડી લાગવી, તાવ અને શરીરમાં કળતર છે",
                "2 दिनों से ठंड लगकर बुखार और बदन दर्द है"
            ) to false,
            trLang(
                selectedLanguage,
                "Sharp throat pain when swallowing and dry cough",
                "ગળવામાં તીવ્ર દુખાવો અને સૂકી ઉધરસ છે",
                "निगलते समय गले में तेज दर्द और सूखी खांसी है"
            ) to false,
            trLang(
                selectedLanguage,
                "Dull throbbing headache on right side for 6 hours",
                "૬ કલાકથી માથાના જમણા ભાગમાં દુખાવો છે",
                "6 घंटे से सिर के दाहिने हिस्से में दर्द हो रहा है"
            ) to false,
            trLang(
                selectedLanguage,
                "Cramping abdominal pain after meals",
                "જમ્યા પછી પેટમાં ચૂંક અને દુખાવો થાય છે",
                "भोजन के बाद पेट में मरोड़ और दर्द होता है"
            ) to false
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            commonStarters.forEach { (starter, isRedFlag) ->
                SuggestionChip(
                    onClick = { onSelectSymptom(starter) },
                    label = {
                        Text(
                            starter,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = if (isRedFlag) Icons.Default.Warning else Icons.Default.AddAlert,
                            contentDescription = null,
                            tint = if (isRedFlag) EmergencyRed else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isRedFlag) EmergencyRedContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun QuickSymptomChipsRow(onSelect: (String) -> Unit) {
    val quickPrompts = listOf(
        tr("Severe Chest Tightness ⚠️", "છાતીમાં ભારે દબાણ ⚠️", "सीने में तेज जकड़न ⚠️") to true,
        tr("Persistent Fever 38°C+", "સતત તાવ ૧૦૧°F+", "लगातार तेज बुखार") to false,
        tr("Stomach Cramps", "પેટમાં ચૂંક અને દુખાવો", "पेट में मरोड़ और दर्द") to false,
        tr("Sore Throat & Cough", "ગળામાં દુખાવો અને ઉધરસ", "गले में खराश और खांसी") to false,
        tr("Dizziness & Fatigue", "ચક્કર અને થાક લાગવો", "चक्कर और थकान") to false,
        tr("Knee Joint Swelling", "ઘૂંટણમાં સોજો અને દુખાવો", "घुटने में सूजन और दर्द") to false
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickPrompts.forEach { (prompt, isWarning) ->
            SuggestionChip(
                onClick = { onSelect(prompt.replace(" ⚠️", "")) },
                label = { Text(prompt, fontSize = 12.sp) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (isWarning) EmergencyRedContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

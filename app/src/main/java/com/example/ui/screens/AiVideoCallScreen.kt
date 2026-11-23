package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.theme.trLang
import com.example.ui.viewmodel.MediAssistViewModel
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.util.Locale

@Composable
fun AiVideoCallScreen(
    viewModel: MediAssistViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val turns by viewModel.videoCallTurns.collectAsState()
    val isAiResponding by viewModel.isVideoAiResponding.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val autoSpeakEnabled by viewModel.autoSpeakEnabled.collectAsState()
    val autoListenEnabled by viewModel.videoCallAutoListen.collectAsState()
    val emergencyTriggered by viewModel.emergencyAlertTriggered.collectAsState()

    var callSeconds by remember { mutableIntStateOf(0) }
    var isVideoEnabled by remember { mutableStateOf(true) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var isCameraPiPExpanded by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var isMicMuted by remember { mutableStateOf(false) }
    var autoListenPulse by remember { mutableIntStateOf(0) }
    var partialSpokenText by remember { mutableStateOf("") }
    var showTextComposer by remember { mutableStateOf(false) }
    var typedCaption by remember { mutableStateOf("") }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasAudioPermission = results[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        viewModel.startVideoCallGreeting()
        val missing = mutableListOf<String>()
        if (!hasCameraPermission) missing.add(Manifest.permission.CAMERA)
        if (!hasAudioPermission) missing.add(Manifest.permission.RECORD_AUDIO)
        if (missing.isNotEmpty()) {
            permissionsLauncher.launch(missing.toTypedArray())
        }
    }

    // Live Call Timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            callSeconds++
        }
    }

    BackHandler {
        viewModel.endAndSaveVideoCallToEhr()
    }

    // CameraX use cases
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Rebind CameraX cleanly whenever camera toggle, front/back switch, or permission changes
    DisposableEffect(hasCameraPermission, isVideoEnabled, useFrontCamera, lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        if (hasCameraPermission && isVideoEnabled) {
            providerFuture.addListener({
                try {
                    val cameraProvider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val selector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageCapture
                    )
                } catch (_: Exception) {
                }
            }, executor)
        } else {
            try {
                providerFuture.get().unbindAll()
            } catch (_: Exception) {
            }
        }
        onDispose {
            try {
                providerFuture.get().unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    // Helper: capture current live video frame as compressed Base64 JPEG (falls back to null smoothly if camera off)
    val captureVideoFrameAndSend: (String) -> Unit = { spokenPrompt ->
        if (hasCameraPermission && isVideoEnabled) {
            try {
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val encoded = try {
                                encodeImageProxyToBase64(image)
                            } catch (_: Exception) {
                                null
                            } finally {
                                image.close()
                            }
                            viewModel.sendVideoCallMessage(spokenPrompt, encoded)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            viewModel.sendVideoCallMessage(spokenPrompt, null)
                        }
                    }
                )
            } catch (_: Exception) {
                viewModel.sendVideoCallMessage(spokenPrompt, null)
            }
        } else {
            viewModel.sendVideoCallMessage(spokenPrompt, null)
        }
    }

    // SpeechRecognizer fallback Activity
    val speechFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                captureVideoFrameAndSend(spoken)
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
            } catch (_: Exception) {
            }
        }
    }

    val startVoiceTurn: (Boolean) -> Unit = { allowPopupFallback ->
        if (!hasAudioPermission) {
            if (allowPopupFallback) {
                permissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            }
        } else {
            if (isSpeaking) viewModel.stopSpeaking()
            val langTag = com.example.ui.theme.AppLanguages.speechTagFor(selectedLanguage)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(langTag, "en-IN", "hi-IN", "gu-IN"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1600L)
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    trLang(
                        selectedLanguage,
                        "Speak to your AI Doctor...",
                        "AI ડૉક્ટર સાથે વાત કરો...",
                        "AI डॉक्टर से बात करें..."
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
                        partialSpokenText = ""
                        val recoverableHandsFree = error == SpeechRecognizer.ERROR_NO_MATCH ||
                            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                            error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                            error == SpeechRecognizer.ERROR_CLIENT ||
                            error == SpeechRecognizer.ERROR_NETWORK ||
                            error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                            error == SpeechRecognizer.ERROR_AUDIO
                        if (recoverableHandsFree) {
                            if (autoListenEnabled && !isMicMuted && !isSpeaking && !isAiResponding) {
                                autoListenPulse++
                            }
                        } else if (allowPopupFallback && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            try { speechFallbackLauncher.launch(intent) } catch (_: Exception) {}
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        isListening = false
                        partialSpokenText = ""
                        val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!spoken.isNullOrBlank()) {
                            captureVideoFrameAndSend(spoken)
                        } else if (autoListenEnabled && !isMicMuted && !isSpeaking && !isAiResponding) {
                            autoListenPulse++
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            partialSpokenText = partial
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                try {
                    if (isListening) {
                        try { sr.cancel() } catch (_: Exception) {}
                    }
                    sr.startListening(intent)
                    isListening = true
                } catch (_: Exception) {
                    isListening = false
                    if (allowPopupFallback) {
                        try { speechFallbackLauncher.launch(intent) } catch (_: Exception) {}
                    } else if (autoListenEnabled && !isMicMuted) {
                        autoListenPulse++
                    }
                }
            } else if (allowPopupFallback) {
                try { speechFallbackLauncher.launch(intent) } catch (_: Exception) {}
            }
        }
    }

    // Continuous Hands-Free Conversation Loop: automatically starts from turn 1 and keeps listening without needing Mic presses
    LaunchedEffect(hasAudioPermission, autoListenEnabled, isMicMuted, isSpeaking, isAiResponding, autoListenPulse, recognizer) {
        if (!hasAudioPermission || !autoListenEnabled || isMicMuted || isSpeaking || isAiResponding || isListening) {
            return@LaunchedEffect
        }
        val waitMs = if (turns.size <= 1 && callSeconds < 2) 1100L else 650L
        delay(waitMs)
        if (hasAudioPermission && autoListenEnabled && !isMicMuted && !isSpeaking && !isAiResponding && !isListening) {
            startVoiceTurn(false)
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty()) {
            listState.animateScrollToItem(turns.size - 1)
        }
    }

    val formattedTime = remember(callSeconds) {
        val mins = callSeconds / 60
        val secs = callSeconds % 60
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    // Pulsing AI Doctor Orb Animation
    val infiniteTransition = rememberInfiniteTransition(label = "ai_doctor_orb")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeaking || isListening) 1.24f else 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 700 else 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07111E),
                        Color(0xFF0E1D33),
                        Color(0xFF060E19)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP CALL HEADER HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, SafeMint.copy(alpha = 0.45f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(SafeMint)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = tr("AI Doctor Video Call", "AI ડૉક્ટર વિડિયો કૉલ", "AI डॉक्टर वीडियो कॉल"),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "● LIVE $formattedTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = SafeMint
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LanguageSelector(viewModel = viewModel, showRefreshButton = false)
                    IconButton(
                        onClick = { viewModel.toggleAutoSpeak() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("video_call_speaker_btn")
                    ) {
                        Icon(
                            imageVector = if (autoSpeakEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = tr("Toggle Voice Reply", "અવાજ ચાલુ/બંધ", "आवाज़ चालू/बंद"),
                            tint = Color.White
                        )
                    }
                }
            }

            // Emergency Call Banner inside Video Call
            AnimatedVisibility(visible = emergencyTriggered) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EmergencyRed.copy(alpha = 0.92f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr("🚨 Critical Sign! Call 108 / 112 Now", "🚨 તાત્કાલિક ૧૦૮ / ૧૧૨ કૉલ કરો!", "🚨 गंभीर लक्षण! 108 / 112 कॉल करें!"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:108")))
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = EmergencyRed),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("108", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. CENTER VIDEO STAGE (AI Virtual Doctor Stage + Floating Patient CameraX PiP)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Main AI Doctor Visual Stage
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF10223A).copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Animated AI Doctor Avatar Ring
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(164.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(144.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isListening -> SafeMint.copy(alpha = 0.25f)
                                            isSpeaking -> PrimaryTeal.copy(alpha = 0.28f)
                                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        }
                                    )
                            )
                            Surface(
                                shape = CircleShape,
                                color = PrimaryTeal.copy(alpha = 0.92f),
                                shadowElevation = 10.dp,
                                modifier = Modifier.size(104.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isAiResponding) {
                                        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(42.dp))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.HealthAndSafety,
                                            contentDescription = "AI Doctor",
                                            tint = Color.White,
                                            modifier = Modifier.size(54.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = when {
                                isAiResponding -> tr("Inspecting voice & video frame...", "AI તમારો અવાજ અને વિડિયો તપાસી રહ્યું છે...", "AI आपकी आवाज़ और वीडियो देख रहा है...")
                                isMicMuted -> tr("🔇 Mic Muted — Tap Mic to Unmute", "🔇 માઇક બંધ છે — ચાલુ કરવા દબાવો", "🔇 माइक बंद है — चालू करने के लिए दबाएं")
                                isListening && partialSpokenText.isNotBlank() -> "🗣️ \"$partialSpokenText\""
                                isListening -> tr("🎤 Hands-Free Mic On — Just speak...", "🎤 માઇક ચાલુ જ છે — સીધું બોલો...", "🎤 माइक चालू है — सीधे बोलें...")
                                isSpeaking -> tr("🔊 AI Doctor is speaking clearly...", "🔊 AI ડૉક્ટર જવાબ આપી રહ્યા છે...", "🔊 AI डॉक्टर बोल रहे हैं...")
                                else -> tr("Hands-free voice ready", "માઇક દબાવ્યા વગર સીધું બોલો", "बिना दबाए सीधे बोलें")
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isMicMuted) EmergencyRed else if (isListening) SafeMint else Color(0xFFBFE9E4),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Subtitles / Consultation Dialogue Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.42f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 210.dp)
                                .testTag("video_call_subtitles_box")
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(turns, key = { it.id }) { turn ->
                                    val isPatient = turn.speaker == "PATIENT"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isPatient) Arrangement.End else Arrangement.Start
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isPatient) {
                                                PrimaryTeal.copy(alpha = 0.85f)
                                            } else if (turn.isEmergency) {
                                                EmergencyRed.copy(alpha = 0.85f)
                                            } else {
                                                Color.White.copy(alpha = 0.14f)
                                            },
                                            modifier = Modifier.fillMaxWidth(0.92f)
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                                Text(
                                                    text = if (isPatient) {
                                                        if (turn.hadCameraSnapshot) {
                                                            tr("You (Voice + Camera)", "તમે (અવાજ + કૅમેરા)", "आप (आवाज़ + कैमरा)")
                                                        } else {
                                                            tr("You", "તમે", "आप")
                                                        }
                                                    } else {
                                                        tr("AI Doctor", "AI ડૉક્ટર", "AI डॉक्टर")
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SafeMint,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = turn.text,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Patient Self-View Live CameraX PiP Window (Top-Right)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF0A1422),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .width(if (isCameraPiPExpanded) 170.dp else 112.dp)
                        .height(if (isCameraPiPExpanded) 224.dp else 150.dp)
                        .border(2.dp, SafeMint.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                        .clickable { isCameraPiPExpanded = !isCameraPiPExpanded }
                        .testTag("video_call_pip_camera")
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (hasCameraPermission && isVideoEnabled) {
                            AndroidView(
                                factory = { previewView },
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.55f),
                                modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp)
                            ) {
                                Text(
                                    text = if (useFrontCamera) tr("Self Cam", "ફ્રન્ટ કૅમેરા", "फ़्रंट कैमरा") else tr("Back Cam", "બેક કૅમેરા", "बैक कैमरा"),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.VideocamOff,
                                    contentDescription = tr("Camera Off", "કૅમેરા બંધ", "कैमरा बंद"),
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (!hasCameraPermission) {
                                        tr("Tap Allow Cam", "પરવાનગી આપો", "अनुमति दें")
                                    } else {
                                        tr("Video Off", "વિડિયો બંધ", "वीडियो बंद")
                                    },
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Optional Quick Text Input Bar (in case room is noisy or speech service missing)
            AnimatedVisibility(visible = showTextComposer) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    OutlinedTextField(
                        value = typedCaption,
                        onValueChange = { typedCaption = it },
                        placeholder = { Text(tr("Type question for AI Doctor...", "અહીં ટાઇપ કરો...", "यहाँ टाइप करें..."), color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SafeMint,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.weight(1f).testTag("video_call_text_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val msg = typedCaption.trim()
                            if (msg.isNotBlank()) {
                                typedCaption = ""
                                captureVideoFrameAndSend(msg)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal)
                            .testTag("video_call_send_text_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = tr("Send", "મોકલો", "भेजें"), tint = Color.White)
                    }
                }
            }

            // Hands-Free Loop + Show Symptom on Camera quick pills
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = autoListenEnabled,
                    onClick = { viewModel.setVideoCallAutoListen(!autoListenEnabled) },
                    label = {
                        Text(
                            tr("Hands-Free Auto-Listen", "ઓટો વાતચીત", "ऑटो-लिसन"),
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SafeMint.copy(alpha = 0.25f),
                        selectedLabelColor = SafeMint,
                        labelColor = Color.White.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier.testTag("video_call_autolisten_chip")
                )

                OutlinedButton(
                    onClick = {
                        if (!hasCameraPermission) {
                            permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        } else {
                            if (!isVideoEnabled) isVideoEnabled = true
                            captureVideoFrameAndSend(
                                trLang(
                                    selectedLanguage,
                                    "Look at what I am showing on the live camera and advise care steps.",
                                    "કૃપા કરીને હું વિડિયો કૅમેરામાં જે બતાવી રહ્યો છું તે જોઈને સલાહ અને ઉપાય આપો.",
                                    "कृपया मैं कैमरे में जो दिखा रहा हूँ उसे देखकर उचित सलाह और उपाय बताएं।"
                                )
                            )
                        }
                    },
                    border = BorderStroke(1.dp, SafeMint.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SafeMint),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("video_call_inspect_cam_btn")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        tr("Show Symptom", "લક્ષણ બતાવો", "लक्षण दिखाएं"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
            }

            // 3. BOTTOM VIDEO CALL CONTROL DOCK (Zoom / Meet style)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF132238),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle Video Cam
                    IconButton(
                        onClick = {
                            if (!hasCameraPermission) {
                                permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                            } else {
                                isVideoEnabled = !isVideoEnabled
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isVideoEnabled) Color.White.copy(alpha = 0.14f) else EmergencyRed.copy(alpha = 0.7f))
                            .testTag("video_call_toggle_cam_btn")
                    ) {
                        Icon(
                            imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = tr("Toggle Camera", "કૅમેરા ચાલુ/બંધ", "कैमरा चालू/बंद"),
                            tint = Color.White
                        )
                    }

                    // Flip Front/Back Camera
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .testTag("video_call_flip_cam_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = tr("Switch Camera", "કૅમેરા બદલો", "कैमरा बदलें"),
                            tint = Color.White
                        )
                    }

                    // Mute / Unmute Hands-Free Mic Toggle Button
                    IconButton(
                        onClick = {
                            if (!isMicMuted && (isListening || autoListenEnabled)) {
                                isMicMuted = true
                                try { recognizer?.cancel() } catch (_: Exception) {}
                                isListening = false
                            } else {
                                isMicMuted = false
                                viewModel.setVideoCallAutoListen(true)
                                startVoiceTurn(true)
                            }
                        },
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(if (isMicMuted) EmergencyRed.copy(alpha = 0.85f) else if (isListening) SafeMint else PrimaryTeal)
                            .testTag("video_call_mic_btn")
                    ) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = tr("Mute or Speak on Video Call", "માઇક ચાલુ કે મ્યૂટ કરો", "माइक चालू या म्यूट करें"),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Toggle Text Input Fallback
                    IconButton(
                        onClick = { showTextComposer = !showTextComposer },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (showTextComposer) PrimaryTeal.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.14f))
                            .testTag("video_call_keyboard_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = tr("Type Message", "લખીને પૂછો", "लिखकर पूछें"),
                            tint = Color.White
                        )
                    }

                    // End Call & Save Consultation to EHR
                    IconButton(
                        onClick = {
                            Toast.makeText(
                                context,
                                trLang(
                                    selectedLanguage,
                                    "Video call ended. Summary archived if symptoms discussed.",
                                    "વિડિયો કૉલ પૂર્ણ થયો.",
                                    "वीडियो कॉल समाप्त हुआ।"
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.endAndSaveVideoCallToEhr()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(EmergencyRed)
                            .testTag("video_call_end_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = tr("End Video Call", "કૉલ પૂરો કરો", "कॉल समाप्त करें"),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun encodeImageProxyToBase64(image: ImageProxy): String {
    val raw = image.toBitmap()
    val rotation = image.imageInfo.rotationDegrees.toFloat()
    val rotated = if (rotation != 0f) {
        val matrix = Matrix().apply { postRotate(rotation) }
        Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    } else {
        raw
    }
    val maxDim = 768
    val scaled = if (rotated.width > maxDim || rotated.height > maxDim) {
        val ratio = maxDim.toFloat() / maxOf(rotated.width, rotated.height).toFloat()
        Bitmap.createScaledBitmap(
            rotated,
            (rotated.width * ratio).toInt().coerceAtLeast(1),
            (rotated.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    } else {
        rotated
    }
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
    return android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
}

package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeMint
import com.example.ui.theme.tr
import com.example.ui.viewmodel.MediAssistViewModel

@Composable
fun AuthScreen(
    viewModel: MediAssistViewModel,
    initialTab: Int = 0, // 0: Password Login, 1: Sign Up, 2: Mobile OTP, 3: Developer Login
    onAuthSuccess: () -> Unit = {}
) {
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val authInfo by viewModel.authInfoMessage.collectAsStateWithLifecycle()
    val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()
    val otpSentToPhone by viewModel.otpSentToPhone.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedMode by remember { mutableIntStateOf(initialTab) }

    // Login & SignUp fields
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var signupDisplayName by remember { mutableStateOf("") }
    var signupUsername by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupConfirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Mobile OTP fields
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }

    // Developer login fields
    var devUsername by remember { mutableStateOf("") }
    var devPassword by remember { mutableStateOf("") }
    var showDevPassword by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.sendLoginOtp(phoneInput)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LanguageSelector(viewModel = viewModel)
            }

            // Hero Header
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = "Health Care AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = tr("Health Care AI", "હેલ્થ કેર AI", "हेल्थ केयर AI"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = tr(
                        "Sign in securely with Mobile OTP, Custom Account, or Developer Portal",
                        "મોબાઇલ OTP, તમારું એકાઉન્ટ અથવા ડેવલપર લોગિન દ્વારા સુરક્ષિત પ્રવેશ કરો",
                        "मोबाइल OTP, कस्टम अकाउंट या डेवलपर पोर्टल से सुरक्षित लॉगिन करें"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // 4-Mode Selector Grid (2x2 for clean readability without cramping)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMode == 0,
                        onClick = {
                            selectedMode = 0
                            viewModel.clearAuthFeedback()
                        },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(tr("User Login", "લોગિન", "यूज़र लॉगिन"), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("auth_tab_password_login")
                    )
                    FilterChip(
                        selected = selectedMode == 1,
                        onClick = {
                            selectedMode = 1
                            viewModel.clearAuthFeedback()
                        },
                        leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(tr("Sign Up", "નવું એકાઉન્ટ", "साइन अप"), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("auth_tab_signup")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMode == 2,
                        onClick = {
                            selectedMode = 2
                            viewModel.clearAuthFeedback()
                        },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(tr("Mobile OTP", "મોબાઇલ OTP", "मोबाइल OTP"), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("auth_tab_mobile_otp")
                    )
                    FilterChip(
                        selected = selectedMode == 3,
                        onClick = {
                            selectedMode = 3
                            viewModel.clearAuthFeedback()
                        },
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(tr("Developer Login", "ડેવલપર લોગિન", "डेवलपर लॉगिन"), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("auth_tab_developer")
                    )
                }
            }

            // Feedback banners
            AnimatedVisibility(visible = !authError.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmergencyRed.copy(alpha = 0.14f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("auth_error_banner")
                ) {
                    Text(
                        text = authError.orEmpty(),
                        color = EmergencyRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            AnimatedVisibility(visible = !authInfo.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SafeMint.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("auth_info_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = authInfo.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Form Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedMode) {
                        0 -> {
                            // Normal Custom Username + Password Login
                            Text(
                                text = tr("Login with Your Account", "તમારા એકાઉન્ટથી લોગિન કરો", "अपने अकाउंट से लॉगिन करें"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Enter the exact username and password you created during Sign Up.",
                                    "તમે Sign Up વખતે બનાવેલ યુઝરનેમ અને પાસવર્ડ જ દાખલ કરો.",
                                    "साइन अप के समय बनाया गया सटीक यूज़रनेम और पासवर्ड दर्ज करें।"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = loginUsername,
                                onValueChange = { loginUsername = it },
                                label = { Text(tr("Username", "યુઝરનેમ", "यूज़रनेम")) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_login_username_input")
                            )

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                label = { Text(tr("Password", "પાસવર્ડ", "पासवर्ड")) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = tr("Toggle password", "પાસવર્ડ જુઓ", "पासवर्ड देखें")
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_login_password_input")
                            )

                            Button(
                                onClick = {
                                    viewModel.loginWithUsernamePassword(
                                        username = loginUsername,
                                        password = loginPassword,
                                        onSuccess = onAuthSuccess
                                    )
                                },
                                enabled = !authLoading,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_login_submit_btn")
                            ) {
                                if (authLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text(tr("Login to Health Care AI", "એપમાં લોગિન કરો", "ऐप में लॉगिन करें"), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        1 -> {
                            // Normal Sign Up (Create Custom Username & Password)
                            Text(
                                text = tr("Create New Patient Account", "નવું દર્દી એકાઉન્ટ બનાવો", "नया मरीज़ अकाउंट बनाएं"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Create your own username & password. Future logins will require these exact credentials.",
                                    "તમારું પોતાનું યુઝરનેમ અને પાસવર્ડ બનાવો. આગળ લોગિન આ જ વિગતોથી થશે.",
                                    "अपना खुद का यूज़रनेम और पासवर्ड बनाएं। आगे इसी से लॉगिन होगा।"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = signupDisplayName,
                                onValueChange = { signupDisplayName = it },
                                label = { Text(tr("Full Name", "પૂરું નામ", "पूरा नाम")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_signup_name_input")
                            )

                            OutlinedTextField(
                                value = signupUsername,
                                onValueChange = { signupUsername = it },
                                label = { Text(tr("Create Username (min 3 chars)", "નવું યુઝરનેમ (ઓછામાં ઓછા 3 અક્ષર)", "नया यूज़रनेम बनाएं")) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_signup_username_input")
                            )

                            OutlinedTextField(
                                value = signupPassword,
                                onValueChange = { signupPassword = it },
                                label = { Text(tr("Create Password (min 6 chars)", "નવો પાસવર્ડ (ઓછામાં ઓછા 6 અક્ષર)", "नया पासवर्ड (कम से कम 6 अक्षर)")) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_signup_password_input")
                            )

                            OutlinedTextField(
                                value = signupConfirmPassword,
                                onValueChange = { signupConfirmPassword = it },
                                label = { Text(tr("Confirm Password", "પાસવર્ડ ફરી લખો", "पासवर्ड की पुष्टि करें")) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_signup_confirm_input")
                            )

                            Button(
                                onClick = {
                                    viewModel.registerWithUsernamePassword(
                                        username = signupUsername,
                                        displayName = signupDisplayName,
                                        password = signupPassword,
                                        confirmPassword = signupConfirmPassword,
                                        onSuccess = onAuthSuccess
                                    )
                                },
                                enabled = !authLoading,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_signup_submit_btn")
                            ) {
                                if (authLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text(tr("Create Account & Sign In", "એકાઉન્ટ બનાવો અને પ્રવેશ કરો", "अकाउंट बनाएं और प्रवेश करें"), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        2 -> {
                            // Mobile Number + 6-Digit OTP Login
                            Text(
                                text = tr("Mobile Number OTP Login", "મોબાઇલ નંબર અને OTP લોગિન", "मोबाइल नंबर OTP लॉगिन"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "We will send a 6-digit OTP to your mobile via SMS & notification bar alert.",
                                    "તમારા મોબાઇલ પર SMS અને નોટિફિકેશન દ્વારા 6 અંકનો OTP કોડ આવશે.",
                                    "आपके मोबाइल पर SMS और नोटिफिकेशन द्वारा 6 अंकों का OTP भेजा जाएगा।"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }
                                    phoneInput = if (digits.length > 10) digits.takeLast(10) else digits
                                },
                                label = { Text(tr("10-Digit Mobile Number (+91)", "10 અંકનો મોબાઇલ નંબર (+91)", "10 अंकों का मोबाइल नंबर (+91)")) },
                                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_otp_phone_input")
                            )

                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.sendLoginOtp(phoneInput)
                                    }
                                },
                                enabled = !authLoading,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_send_otp_btn")
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (otpSentToPhone == null) {
                                        tr("Send 6-Digit OTP", "OTP મોકલો", "OTP भेजें")
                                    } else {
                                        tr("Resend OTP", "ફરી OTP મોકલો", "OTP पुनः भेजें")
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            AnimatedVisibility(visible = otpSentToPhone != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = otpInput,
                                        onValueChange = { raw -> otpInput = raw.filter { it.isDigit() }.take(6) },
                                        label = { Text(tr("Enter 6-Digit OTP", "6 અંકનો OTP નાખો", "6 अंकों का OTP दर्ज करें")) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("auth_otp_code_input")
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.verifyLoginOtp(
                                                rawPhone = phoneInput,
                                                enteredCode = otpInput,
                                                onSuccess = onAuthSuccess
                                            )
                                        },
                                        enabled = !authLoading,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_verify_otp_btn")
                                    ) {
                                        if (authLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Text(tr("Verify OTP & Login", "OTP ચકાસો અને લોગિન કરો", "OTP सत्यापित करें और लॉगिन करें"), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // Strictly Authorized Developer Login (SHAILESH)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr("Authorized Developer Portal", "અધિકૃત ડેવલપર પોર્ટલ", "अधिकृत डेवलपर पोर्टल"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = tr(
                                    "Restricted access for authorized app developer only. Unlocks Developer Logs & diagnostic tools.",
                                    "માત્ર અધિકૃત ડેવલપર માટે સુરક્ષિત પ્રવેશ — ડેવલપર લોગ્સ અને ડાયગ્નોસ્ટિક્સ અનલોક કરે છે.",
                                    "केवल अधिकृत डेवलपर के लिए सुरक्षित प्रवेश — डेवलपर लॉग और डायग्नोस्टिक्स अनलॉक करता है।"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = devUsername,
                                onValueChange = { devUsername = it },
                                label = { Text(tr("Developer Username", "ડેવલપર યુઝરનેમ", "डेवलपर यूज़रनेम")) },
                                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_dev_username_input")
                            )

                            OutlinedTextField(
                                value = devPassword,
                                onValueChange = { devPassword = it },
                                label = { Text(tr("Developer Password", "ડેવલપર પાસવર્ડ", "डेवलपर पासवर्ड")) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { showDevPassword = !showDevPassword }) {
                                        Icon(
                                            imageVector = if (showDevPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = tr("Toggle password", "પાસવર્ડ જુઓ", "पासवर्ड देखें")
                                        )
                                    }
                                },
                                visualTransformation = if (showDevPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_dev_password_input")
                            )

                            Button(
                                onClick = {
                                    if (viewModel.loginAsDeveloper(devUsername, devPassword)) {
                                        onAuthSuccess()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_dev_submit_btn")
                            ) {
                                Text(tr("Sign In as Developer", "ડેવલપર તરીકે લોગિન કરો", "डेवलपर के रूप में लॉगिन करें"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

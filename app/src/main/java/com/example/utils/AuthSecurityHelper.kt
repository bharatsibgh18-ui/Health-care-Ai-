package com.example.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

enum class AuthRole {
    NONE,
    PATIENT,
    DEVELOPER
}

data class AuthSessionState(
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val role: AuthRole = AuthRole.NONE,
    val authMethod: String = ""
) {
    val isDeveloper: Boolean
        get() = isLoggedIn && role == AuthRole.DEVELOPER
}

object AuthSecurityHelper {

    const val DEVELOPER_USERNAME = "SHAILESH"
    const val DEVELOPER_PASSWORD = "Shailesh@1736"

    private const val PREF_AUTH = "mediassist_auth_session"
    private const val KEY_LOGGED_IN = "auth_logged_in"
    private const val KEY_USERNAME = "auth_username"
    private const val KEY_DISPLAY_NAME = "auth_display_name"
    private const val KEY_PHONE = "auth_phone"
    private const val KEY_ROLE = "auth_role"
    private const val KEY_METHOD = "auth_method"

    const val OTP_CHANNEL_ID = "otp_verification_channel"
    private const val OTP_NOTIFICATION_ID = 4901

    private val secureRandom = SecureRandom()

    /**
     * Strictly verifies developer login credentials.
     * ONLY exact username "SHAILESH" and password "Shailesh@1736" are accepted.
     */
    fun verifyDeveloperCredentials(username: String, password: String): Boolean {
        val cleanUser = username.trim()
        val userMatch = MessageDigest.isEqual(
            cleanUser.toByteArray(Charsets.UTF_8),
            DEVELOPER_USERNAME.toByteArray(Charsets.UTF_8)
        )
        val passMatch = MessageDigest.isEqual(
            password.toByteArray(Charsets.UTF_8),
            DEVELOPER_PASSWORD.toByteArray(Charsets.UTF_8)
        )
        return userMatch && passMatch
    }

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(bytes)
        } else {
            bytes.joinToString("") { "%02x".format(it) }
        }
    }

    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$password".toByteArray(Charsets.UTF_8)
        val hashed = digest.digest(input)
        return hashed.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        if (salt.isBlank() || expectedHash.isBlank()) return false
        val candidate = hashPassword(password, salt)
        return MessageDigest.isEqual(
            candidate.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun normalizePhoneNumber(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    fun generateOtpCode(): String {
        val code = secureRandom.nextInt(900000) + 100000
        return code.toString()
    }

    fun saveSession(context: Context, session: AuthSessionState) {
        val prefs = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, session.isLoggedIn)
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .putString(KEY_PHONE, session.phoneNumber)
            .putString(KEY_ROLE, session.role.name)
            .putString(KEY_METHOD, session.authMethod)
            .apply()
    }

    fun loadSession(context: Context): AuthSessionState {
        val prefs = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
        val loggedIn = prefs.getBoolean(KEY_LOGGED_IN, false)
        if (!loggedIn) return AuthSessionState()
        val roleStr = prefs.getString(KEY_ROLE, AuthRole.NONE.name) ?: AuthRole.NONE.name
        val role = runCatching { AuthRole.valueOf(roleStr) }.getOrDefault(AuthRole.NONE)
        return AuthSessionState(
            isLoggedIn = true,
            username = prefs.getString(KEY_USERNAME, "").orEmpty(),
            displayName = prefs.getString(KEY_DISPLAY_NAME, "").orEmpty(),
            phoneNumber = prefs.getString(KEY_PHONE, "").orEmpty(),
            role = role,
            authMethod = prefs.getString(KEY_METHOD, "").orEmpty()
        )
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * Dispatches a 6-digit OTP to the user's phone via SMS (when SEND_SMS is granted)
     * AND posts a WhatsApp-style high-priority system notification so the OTP is always
     * received reliably outside the app on any Android device or emulator.
     */
    fun dispatchMobileOtp(context: Context, rawPhone: String, otp: String): Boolean {
        val normalizedPhone = normalizePhoneNumber(rawPhone)
        var smsSent = false
        try {
            val hasSmsPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasSmsPerm && normalizedPhone.length == 10) {
                val smsManager = context.getSystemService(SmsManager::class.java)
                smsManager?.sendTextMessage(
                    normalizedPhone,
                    null,
                    "Health Care AI Login OTP: $otp. Valid for 5 minutes. Do not share this code.",
                    null,
                    null
                )
                smsSent = true
            }
        } catch (_: Exception) {
            smsSent = false
        }

        // Always also send outside system notification (heads-up message style)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    OTP_CHANNEL_ID,
                    "OTP Verification Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Instant mobile OTP verification alerts"
                    enableVibration(true)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        null
                    )
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.createNotificationChannel(channel)
            }

            val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (canNotify) {
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pi = PendingIntent.getActivity(
                    context,
                    OTP_NOTIFICATION_ID,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val notification = NotificationCompat.Builder(context, OTP_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_health)
                    .setContentTitle("Health Care AI • OTP: $otp")
                    .setContentText("Your login verification code for +91 $normalizedPhone is $otp")
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText("Health Care AI Verification Code: $otp\nMobile: +91 $normalizedPhone\nValid for 5 minutes. Enter this code to sign in.")
                    )
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .build()

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(OTP_NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {}

        DevLogger.i("AuthOTP", "Dispatched 6-digit OTP to +91 $normalizedPhone (smsSent=$smsSent)")
        return smsSent
    }
}

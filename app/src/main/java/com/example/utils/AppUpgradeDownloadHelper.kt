package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.ui.theme.trLang

/**
 * Opens the developer-published App Upgrade download URL (such as a GitHub Releases APK page
 * or direct HTTPS download link) safely in the user's browser without requiring sensitive permissions.
 */
object AppUpgradeDownloadHelper {

    fun normalizeDownloadUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val normalized = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> return null
        }
        val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: return null
        return if (!uri.host.isNullOrBlank()) normalized else null
    }

    fun buildDownloadIntent(rawUrl: String?): Intent? {
        val safeUrl = normalizeDownloadUrl(rawUrl) ?: return null
        return Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openUpgradeDownload(
        context: Context,
        rawUrl: String?,
        language: String = "English"
    ): Boolean {
        val intent = buildDownloadIntent(rawUrl)
        if (intent == null) {
            Toast.makeText(
                context,
                trLang(
                    language,
                    "Upgrade download link is not configured yet. Please contact the developer.",
                    "અપગ્રેડ ડાઉનલોડ લિંક હજુ સેટ કરેલ નથી. કૃપા કરીને ડેવલપરનો સંપર્ક કરો.",
                    "अपग्रेड डाउनलोड लिंक अभी सेट नहीं है। कृपया डेवलपर से संपर्क करें।"
                ),
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            try {
                val chooser = Intent.createChooser(
                    intent,
                    trLang(language, "Download Upgrade", "અપગ્રેડ ડાઉનલોડ કરો", "अपग्रेड डाउनलोड करें")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            } catch (_: Throwable) {
                Toast.makeText(
                    context,
                    trLang(
                        language,
                        "Unable to open download link in browser.",
                        "બ્રાઉઝરમાં ડાઉનલોડ લિંક ખોલી શકાઈ નહીં.",
                        "ब्राउज़र में डाउनलोड लिंक नहीं खोला जा सका।"
                    ),
                    Toast.LENGTH_LONG
                ).show()
                false
            }
        }
    }
}

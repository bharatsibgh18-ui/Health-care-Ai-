package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ApkShareHelper {

    fun resolveSourceApkFile(context: Context): File? {
        val appInfo = context.applicationInfo ?: return null
        val path = appInfo.publicSourceDir ?: appInfo.sourceDir ?: return null
        val file = File(path)
        return if (file.exists() && file.canRead()) file else null
    }

    suspend fun shareInstalledApk(context: Context, isGujarati: Boolean) {
        shareInstalledApk(context, if (isGujarati) "Gujarati" else "English")
    }

    suspend fun shareInstalledApk(context: Context, language: String) {
        val result = withContext(Dispatchers.IO) {
            try {
                val sourceFile = resolveSourceApkFile(context)
                    ?: return@withContext Result.failure<Uri>(IllegalStateException("Source APK not accessible"))
                val cachedApk = File(context.cacheDir, "HealthCareAI.apk")
                sourceFile.copyTo(cachedApk, overwrite = true)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cachedApk
                )
                Result.success(uri)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        result.onSuccess { uri ->
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Health Care AI (HealthCareAI.apk)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(
                    shareIntent,
                    com.example.ui.theme.trLang(
                        language,
                        "Share Health Care AI (.apk)",
                        "Health Care AI (.apk) શેર કરો",
                        "Health Care AI (.apk) शेयर करें"
                    )
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    com.example.ui.theme.trLang(
                        language,
                        "Unable to share APK: ${e.message}",
                        "APK શેર કરવામાં ભૂલ આવી",
                        "APK शेयर करने में त्रुटि हुई: ${e.message}"
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.onFailure {
            Toast.makeText(
                context,
                com.example.ui.theme.trLang(
                    language,
                    "APK export is only available on an installed Android build",
                    "આ ડિવાઇસ પર APK ફાઇલ ઉપલબ્ધ નથી",
                    "इस डिवाइस पर APK फ़ाइल उपलब्ध नहीं है"
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

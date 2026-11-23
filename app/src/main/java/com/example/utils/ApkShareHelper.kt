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

    fun getInstalledApkSizeLabel(context: Context): String? {
        val file = resolveSourceApkFile(context) ?: return null
        val mb = file.length().toDouble() / (1024.0 * 1024.0)
        return if (mb > 0.1) String.format(java.util.Locale.US, "%.1f MB", mb) else null
    }

    suspend fun shareInstalledApk(context: Context, isGujarati: Boolean) {
        shareInstalledApk(context, if (isGujarati) "Gujarati" else "English")
    }

    suspend fun shareInstalledApk(context: Context, language: String) {
        val result = withContext(Dispatchers.IO) {
            try {
                val sourceFile = resolveSourceApkFile(context)
                    ?: return@withContext Result.failure<Uri>(IllegalStateException("Source APK not accessible"))
                val versionName = com.example.BuildConfig.VERSION_NAME
                val apkFileName = "HealthCareAI_v${versionName}.apk"
                val oldUnversioned = File(context.cacheDir, "HealthCareAI.apk")
                if (oldUnversioned.exists()) oldUnversioned.delete()
                val cachedApk = File(context.cacheDir, apkFileName)
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
                val apkFileName = "HealthCareAI_v${com.example.BuildConfig.VERSION_NAME}.apk"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Health Care AI ($apkFileName)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(
                    shareIntent,
                    com.example.ui.theme.trLang(
                        language,
                        "Share $apkFileName",
                        "$apkFileName શેર કરો",
                        "$apkFileName शेयर करें"
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

    suspend fun saveInstalledApkToDownloads(context: Context, language: String) {
        val versionName = com.example.BuildConfig.VERSION_NAME
        val apkFileName = "HealthCareAI_v${versionName}.apk"
        val result = withContext(Dispatchers.IO) {
            try {
                val sourceFile = resolveSourceApkFile(context)
                    ?: return@withContext Result.failure<String>(IllegalStateException("Source APK not accessible"))
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Downloads.DISPLAY_NAME, apkFileName)
                        put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                        put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                        put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext Result.failure<String>(IllegalStateException("Unable to create download file"))
                    resolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { input -> input.copyTo(out) }
                    } ?: return@withContext Result.failure<String>(IllegalStateException("Unable to write download file"))
                    values.clear()
                    values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    Result.success("Downloads/$apkFileName")
                } else {
                    val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                    val dest = File(dir, apkFileName)
                    sourceFile.copyTo(dest, overwrite = true)
                    Result.success(dest.absolutePath)
                }
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

        result.onSuccess { savedPath ->
            Toast.makeText(
                context,
                com.example.ui.theme.trLang(
                    language,
                    "Saved $apkFileName to $savedPath",
                    "$apkFileName સફળતાપૂર્વક સેવ થઈ ગઈ ($savedPath)",
                    "$apkFileName सफलतापूर्वक सेव हो गई ($savedPath)"
                ),
                Toast.LENGTH_LONG
            ).show()
        }.onFailure {
            Toast.makeText(
                context,
                com.example.ui.theme.trLang(
                    language,
                    "Unable to save APK on this device",
                    "આ ડિવાઇસ પર APK સેવ થઈ શકી નહીં",
                    "इस डिवाइस पर APK सेव नहीं हो सकी"
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

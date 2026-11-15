package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ApkShareHelper {

    fun resolveSourceApkFile(context: Context): File? {
        val appInfo = context.applicationInfo ?: return null
        val path = appInfo.publicSourceDir ?: appInfo.sourceDir ?: return null
        val file = File(path)
        return if (file.exists() && file.canRead()) file else null
    }

    suspend fun saveApkToDownloads(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = resolveSourceApkFile(context)
                ?: return@withContext Result.failure(IllegalStateException("Source APK not accessible"))

            val fileName = "HealthCareAI.apk"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(IllegalStateException("Could not create Downloads entry"))
                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    } ?: throw IllegalStateException("Could not open Downloads output stream")

                    val doneValues = ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }
                    resolver.update(uri, doneValues, null, null)
                    Result.success("Downloads/$fileName")
                } catch (e: Exception) {
                    runCatching { resolver.delete(uri, null, null) }
                    Result.failure(e)
                }
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                if (!dir.exists()) dir.mkdirs()
                val outFile = File(dir, fileName)
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(outFile).use { out ->
                        input.copyTo(out)
                    }
                }
                Result.success(outFile.absolutePath)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copyApkToUri(context: Context, destinationUri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = resolveSourceApkFile(context)
                ?: return@withContext Result.failure(IllegalStateException("Source APK not accessible"))
            var totalBytes = 0L
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    totalBytes = input.copyTo(out)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open destination stream"))
            Result.success(totalBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

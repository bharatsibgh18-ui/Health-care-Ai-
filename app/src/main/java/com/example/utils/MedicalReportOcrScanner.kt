package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.roundToInt

object MedicalReportOcrScanner {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    fun isPdfUri(context: Context, uri: Uri): Boolean {
        val mime = try {
            context.contentResolver.getType(uri)
        } catch (_: Exception) {
            null
        }
        if (mime?.equals("application/pdf", ignoreCase = true) == true) return true
        val fileName = getFileName(context, uri) ?: uri.toString()
        return fileName.lowercase().endsWith(".pdf")
    }

    suspend fun extractTextFromBitmap(bitmap: Bitmap): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val rawText = visionText.text.trim()
                    if (rawText.isBlank()) {
                        continuation.resume(
                            Result.failure(
                                Exception("No legible medical text detected. Please ensure good lighting, avoid glare, and keep the document flat.")
                            )
                        )
                    } else {
                        continuation.resume(Result.success(rawText))
                    }
                }
                .addOnFailureListener { error ->
                    continuation.resume(Result.failure(error))
                }
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    suspend fun extractTextFromPdfUri(
        context: Context,
        pdfUri: Uri,
        maxPages: Int = 10
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: return@withContext Result.failure(Exception("Unable to open the selected PDF file."))

            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    val totalPages = renderer.pageCount
                    if (totalPages <= 0) {
                        return@withContext Result.failure(Exception("The selected PDF has no readable pages."))
                    }

                    val pagesToRead = minOf(totalPages, maxPages)
                    val extractedPages = mutableListOf<String>()

                    for (pageIndex in 0 until pagesToRead) {
                        renderer.openPage(pageIndex).use { page ->
                            val targetWidth = (page.width * 2).coerceIn(1080, 2200)
                            val scale = targetWidth.toFloat() / page.width.coerceAtLeast(1)
                            val targetHeight = (page.height * scale).roundToInt().coerceIn(1080, 3200)

                            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                            try {
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val pageResult = extractTextFromBitmap(bitmap)
                                val pageText = pageResult.getOrNull()?.trim()
                                if (!pageText.isNullOrBlank()) {
                                    if (pagesToRead > 1) {
                                        extractedPages.add("--- Page ${pageIndex + 1} ---\n$pageText")
                                    } else {
                                        extractedPages.add(pageText)
                                    }
                                }
                            } finally {
                                if (!bitmap.isRecycled) {
                                    bitmap.recycle()
                                }
                            }
                        }
                    }

                    val combinedText = extractedPages.joinToString("\n\n").trim()
                    if (combinedText.isBlank()) {
                        Result.failure(
                            Exception("No readable lab text was found inside this PDF. Please check if the PDF is password-protected or blank.")
                        )
                    } else {
                        Result.success(combinedText)
                    }
                }
            }
        } catch (se: SecurityException) {
            Result.failure(Exception("This PDF is password-protected or access was denied: ${se.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to read PDF lab report: ${e.message ?: "Invalid or corrupted PDF"}"))
        }
    }

    suspend fun extractTextFromUri(context: Context, uri: Uri): Result<String> {
        if (isPdfUri(context, uri)) {
            return extractTextFromPdfUri(context, uri)
        }
        return suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val rawText = visionText.text.trim()
                        if (rawText.isBlank()) {
                            continuation.resume(
                                Result.failure(
                                    Exception("No legible medical text detected. Please ensure good lighting, avoid glare, and keep the document flat.")
                                )
                            )
                        } else {
                            continuation.resume(Result.success(rawText))
                        }
                    }
                    .addOnFailureListener { error ->
                        continuation.resume(Result.failure(error))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }
}

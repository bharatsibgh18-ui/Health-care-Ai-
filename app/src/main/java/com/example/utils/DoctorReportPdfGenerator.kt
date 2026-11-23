package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.SymptomLogEntity
import com.example.data.local.TriageSessionEntity
import com.example.data.local.UserProfileEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DoctorReportPdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points (72 dpi)
    private const val MARGIN = 40f

    /**
     * Generates a clinical PDF document using Android's native PdfDocument API.
     */
    fun createDoctorReportPdf(
        context: Context,
        profile: UserProfileEntity?,
        symptomLogs: List<SymptomLogEntity>,
        triageSessions: List<TriageSessionEntity>
    ): PdfDocument {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Colors
        val tealPrimary = Color.rgb(0, 121, 107)      // #00796B
        val tealLight = Color.rgb(224, 242, 241)        // #E0F2F1
        val darkGray = Color.rgb(33, 33, 33)           // #212121
        val mediumGray = Color.rgb(117, 117, 117)      // #757575
        val lightGray = Color.rgb(245, 245, 245)       // #F5F5F5
        val redAlert = Color.rgb(211, 47, 47)          // #D32F2F

        // Paints
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        var y = MARGIN

        // 1. Header Banner
        paint.color = tealPrimary
        val headerRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 65f)
        canvas.drawRoundRect(headerRect, 8f, 8f, paint)

        // Title text inside banner
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("HEALTH CARE AI • CLINICAL PATIENT REPORT", MARGIN + 16f, y + 28f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy • hh:mm a", Locale.getDefault())
        val generatedDate = dateFormat.format(Date())
        canvas.drawText("Generated on: $generatedDate | Prepared for Physician Review", MARGIN + 16f, y + 48f, paint)

        y += 80f

        // 2. Patient Demographics & Background Section
        paint.color = darkGray
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("1. PATIENT DEMOGRAPHICS & CLINICAL BACKGROUND", MARGIN, y, paint)
        y += 6f

        // Demographic background card
        paint.color = lightGray
        val demoRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 90f)
        canvas.drawRoundRect(demoRect, 6f, 6f, paint)

        paint.color = darkGray
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val name = profile?.fullName ?: "Patient"
        val ageGenderBlood = "Age: ${profile?.age ?: 30} | Gender: ${profile?.gender ?: "Not Specified"} | Blood: ${profile?.bloodGroup ?: "Not Specified"}"
        val allergies = "Allergies: ${profile?.allergies?.ifBlank { "None Reported" } ?: "None Reported"}"
        val conditions = "Chronic Conditions: ${profile?.chronicConditions?.ifBlank { "None Reported" } ?: "None Reported"}"
        val meds = "Current Medications: ${profile?.currentMedications?.ifBlank { "None" } ?: "None"}"
        val emergencyContact = "Emergency Contact: ${profile?.emergencyContactName ?: "Family"} (${profile?.emergencyContactPhone ?: "112"})"
        val pmjayText = profile?.pmjayCardNumber?.takeIf { it.isNotBlank() }?.let { "PMJAY Card: $it" }
        val abhaText = profile?.abhaCardNumber?.takeIf { it.isNotBlank() }?.let { "ABHA Card: ${HealthCardValidator.formatAbhaForDisplay(it)}" }

        canvas.drawText("• Patient Name: $name", MARGIN + 12f, y + 20f, paint)
        canvas.drawText("• Demographics: $ageGenderBlood", MARGIN + 12f, y + 36f, paint)
        canvas.drawText("• $allergies", MARGIN + 12f, y + 52f, paint)
        canvas.drawText("• $conditions", MARGIN + 12f, y + 68f, paint)
        canvas.drawText("• $meds", MARGIN + 280f, y + 20f, paint)
        canvas.drawText("• $emergencyContact", MARGIN + 280f, y + 36f, paint)
        if (pmjayText != null) {
            canvas.drawText("• $pmjayText", MARGIN + 280f, y + 52f, paint)
        }
        if (abhaText != null) {
            canvas.drawText("• $abhaText", MARGIN + 280f, y + 68f, paint)
        }

        y += 105f

        // 3. Recorded Symptom Diary Section
        paint.color = darkGray
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("2. RECENT SYMPTOM LOGS & SEVERITY MONITOR (Room Database)", MARGIN, y, paint)
        y += 10f

        // Table Header
        paint.color = tealLight
        val tableHeaderRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 22f)
        canvas.drawRect(tableHeaderRect, paint)

        paint.color = tealPrimary
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DATE", MARGIN + 8f, y + 15f, paint)
        canvas.drawText("SYMPTOM TITLE", MARGIN + 75f, y + 15f, paint)
        canvas.drawText("SEVERITY", MARGIN + 240f, y + 15f, paint)
        canvas.drawText("LOCATION / DURATION", MARGIN + 310f, y + 15f, paint)
        canvas.drawText("STATUS", MARGIN + 445f, y + 15f, paint)
        y += 24f

        val logDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val displayLogs = symptomLogs.sortedByDescending { it.timestamp }.take(5)

        if (displayLogs.isEmpty()) {
            paint.color = mediumGray
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("No previous symptoms recorded in diary.", MARGIN + 10f, y + 16f, paint)
            y += 26f
        } else {
            displayLogs.forEachIndexed { idx, log ->
                paint.color = if (idx % 2 == 0) Color.WHITE else Color.rgb(250, 250, 250)
                canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f), paint)

                paint.color = darkGray
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                val dateStr = logDateFormat.format(Date(log.timestamp))
                canvas.drawText(dateStr, MARGIN + 8f, y + 14f, paint)
                canvas.drawText(log.symptomTitle.take(30), MARGIN + 75f, y + 14f, paint)

                // Severity pill text
                paint.color = when {
                    log.severity >= 8 -> redAlert
                    log.severity >= 4 -> tealPrimary
                    else -> Color.rgb(46, 125, 50)
                }
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${log.severity}/10", MARGIN + 240f, y + 14f, paint)

                paint.color = darkGray
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("${log.bodyLocation} • ${log.duration}".take(24), MARGIN + 310f, y + 14f, paint)
                canvas.drawText(log.status, MARGIN + 445f, y + 14f, paint)

                y += 20f
            }
        }

        y += 16f

        // 4. Clinical Triage Sessions Section
        paint.color = darkGray
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("3. CLINICAL TRIAGE CONSULTATIONS", MARGIN, y, paint)
        y += 12f

        val displaySessions = triageSessions.sortedByDescending { it.timestamp }.take(2)
        if (displaySessions.isEmpty()) {
            paint.color = mediumGray
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("No recorded AI triage consultations yet.", MARGIN + 10f, y + 14f, paint)
            y += 26f
        } else {
            val sessionDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            displaySessions.forEach { session ->
                paint.color = Color.rgb(248, 249, 250)
                val sessRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 68f)
                canvas.drawRoundRect(sessRect, 6f, 6f, paint)

                paint.color = darkGray
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val sessionDate = sessionDateFormat.format(Date(session.timestamp))
                canvas.drawText("• [$sessionDate] ${session.primarySymptom}", MARGIN + 10f, y + 16f, paint)

                paint.color = tealPrimary
                canvas.drawText("Recommended Specialist: ${session.recommendedSpecialist}", MARGIN + 10f, y + 32f, paint)

                paint.color = mediumGray
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val summarySnippet = session.summary.replace("\n", " ").take(130) + if (session.summary.length > 130) "..." else ""
                canvas.drawText("Summary: $summarySnippet", MARGIN + 10f, y + 48f, paint)

                if (session.emergencyFlagged) {
                    paint.color = redAlert
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("[EMERGENCY FLAG ACTIVATED]", MARGIN + 360f, y + 16f, paint)
                }

                y += 76f
            }
        }

        // 5. Medical Disclaimer Footer Card
        val footerY = PAGE_HEIGHT - MARGIN - 65f
        paint.color = Color.rgb(255, 235, 238) // Light red container
        val disclaimerRect = RectF(MARGIN, footerY, PAGE_WIDTH - MARGIN, footerY + 60f)
        canvas.drawRoundRect(disclaimerRect, 6f, 6f, paint)

        paint.color = redAlert
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("LEGAL & MEDICAL DISCLAIMER FOR PHYSICIANS:", MARGIN + 12f, footerY + 16f, paint)

        paint.color = Color.rgb(66, 66, 66)
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("1. Health Care AI is a personal triage assistant and NOT a licensed medical practitioner.", MARGIN + 12f, footerY + 28f, paint)
        canvas.drawText("2. The symptoms and severity entries above are patient-reported longitudinal self-assessments.", MARGIN + 12f, footerY + 39f, paint)
        canvas.drawText("3. This document is intended solely to aid clinical history taking during certified healthcare consultations.", MARGIN + 12f, footerY + 50f, paint)

        // Finish the page
        document.finishPage(page)
        return document
    }

    /**
     * Saves the generated PDF into the public Downloads directory.
     * Returns the file path or URI string.
     */
    fun savePdfToDownloads(context: Context, document: PdfDocument): String? {
        val fileName = "HealthCareAI_Doctor_Report_${System.currentTimeMillis()}.pdf"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null

                resolver.openOutputStream(uri)?.use { outputStream ->
                    document.writeTo(outputStream)
                }
                "Downloads/$fileName"
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    document.writeTo(outputStream)
                }
                file.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            document.close()
        }
    }

    /**
     * Writes the PDF to a cache file and opens Android's native share sheet.
     */
    fun shareDoctorReportPdf(context: Context, document: PdfDocument) {
        try {
            val cacheDir = File(context.cacheDir, "reports")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val file = File(cacheDir, "HealthCareAI_Doctor_Report.pdf")
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Health Care AI - Patient Clinical Report")
                putExtra(Intent.EXTRA_TEXT, "Attached is the clinical patient summary report prepared for doctor review.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Clinical PDF with Doctor via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document.close()
        }
    }
}

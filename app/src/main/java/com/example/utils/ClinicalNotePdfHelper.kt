package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.ClinicalNoteEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClinicalNotePdfHelper {

    fun shareClinicalNotePdf(context: Context, note: ClinicalNoteEntity) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val margin = 40f
        var y = margin

        val tealPrimary = Color.rgb(0, 104, 116)
        val darkGray = Color.rgb(33, 33, 33)
        val mutedGray = Color.rgb(100, 100, 100)

        paint.color = tealPrimary
        canvas.drawRoundRect(RectF(margin, y, 595f - margin, y + 60f), 8f, 8f, paint)

        paint.color = Color.WHITE
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("HEALTH CARE AI • CLINICAL CONSULTATION NOTE", margin + 14f, y + 26f, paint)

        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateStr = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(note.timestamp))
        canvas.drawText("Patient: ${note.patientName} | Generated: $dateStr", margin + 14f, y + 46f, paint)
        y += 78f

        fun drawWrappedSection(title: String, body: String) {
            if (y > 760f) return
            paint.color = tealPrimary
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(title, margin, y, paint)
            y += 16f

            paint.color = darkGray
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            val maxChars = 84
            body.lines().forEach { rawLine ->
                var remaining = rawLine.trim()
                if (remaining.isEmpty()) {
                    y += 6f
                } else {
                    while (remaining.isNotEmpty() && y < 785f) {
                        val takeCount = if (remaining.length <= maxChars) remaining.length else {
                            val lastSpace = remaining.take(maxChars).lastIndexOf(' ')
                            if (lastSpace > 20) lastSpace else maxChars
                        }
                        val chunk = remaining.substring(0, takeCount).trim()
                        canvas.drawText(chunk, margin + 8f, y, paint)
                        y += 13f
                        remaining = remaining.substring(takeCount).trim()
                    }
                }
            }
            y += 10f
        }

        drawWrappedSection("1. AUTOMATED SOAP NOTE", note.soapNote)
        drawWrappedSection("2. MEDICAL REPORT SUMMARY", note.medicalReport)
        drawWrappedSection("3. DRAFT PRESCRIPTION (Rx)", note.prescriptionSummary)

        paint.color = mutedGray
        paint.textSize = 8f
        canvas.drawText("Disclaimer: Auto-documented via Health Care AI NLP. Must be verified by a licensed physician.", margin, 815f, paint)

        document.finishPage(page)

        try {
            val cacheDir = File(context.cacheDir, "clinical_notes")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "Clinical_Note_${note.id}.pdf")
            FileOutputStream(file).use { out -> document.writeTo(out) }

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Clinical Consultation Note - ${note.patientName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Clinical Note PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
        } finally {
            document.close()
        }
    }
}

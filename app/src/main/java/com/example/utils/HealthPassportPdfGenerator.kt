package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.MedicineEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.VitalLogEntity
import com.example.ui.theme.trLang
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HealthPassportSnapshot(
    val patientName: String,
    val age: Int,
    val gender: String,
    val bloodGroup: String,
    val allergies: String,
    val chronicConditions: String,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val pmjayCardNumber: String,
    val abhaCardNumber: String,
    val healthScore: Int,
    val scoreTierLabel: String,
    val todaySteps: Int,
    val todayWaterMl: Int,
    val todayCalories: Int,
    val activeMedicines: List<MedicineEntity>,
    val latestVitals: List<VitalLogEntity>
)

/**
 * Generates a formatted, doctor-ready A4 Clinical Health Passport PDF using
 * Android's native PdfDocument API and shares it via FileProvider (zero storage permissions).
 */
object HealthPassportPdfGenerator {

    fun buildSnapshot(
        profile: UserProfileEntity?,
        fallbackName: String,
        scoreReport: DailyHealthScoreReport,
        todaySteps: Int,
        todayWaterMl: Int,
        todayCalories: Int,
        activeMedicines: List<MedicineEntity>,
        latestVitals: List<VitalLogEntity>
    ): HealthPassportSnapshot {
        val tier = when (scoreReport.tier) {
            HealthScoreTier.ULTRA_OPTIMAL -> "Ultra Optimal"
            HealthScoreTier.STRONG -> "Strong"
            HealthScoreTier.STEADY -> "Steady"
            HealthScoreTier.NEEDS_ATTENTION -> "Boost Needed"
        }
        return HealthPassportSnapshot(
            patientName = profile?.fullName?.takeIf { it.isNotBlank() } ?: fallbackName.ifBlank { "Patient" },
            age = profile?.age ?: 0,
            gender = profile?.gender?.takeIf { it.isNotBlank() } ?: "Not Specified",
            bloodGroup = profile?.bloodGroup?.takeIf { it.isNotBlank() } ?: "Unknown",
            allergies = profile?.allergies?.takeIf { it.isNotBlank() } ?: "None Known",
            chronicConditions = profile?.chronicConditions?.takeIf { it.isNotBlank() } ?: "None",
            emergencyContactName = profile?.emergencyContactName?.takeIf { it.isNotBlank() } ?: "Not Set",
            emergencyContactPhone = profile?.emergencyContactPhone?.takeIf { it.isNotBlank() } ?: "112",
            pmjayCardNumber = profile?.pmjayCardNumber?.takeIf { it.isNotBlank() } ?: "Not Linked",
            abhaCardNumber = profile?.abhaCardNumber?.takeIf { it.isNotBlank() } ?: "Not Linked",
            healthScore = scoreReport.overallScore,
            scoreTierLabel = tier,
            todaySteps = todaySteps,
            todayWaterMl = todayWaterMl,
            todayCalories = todayCalories,
            activeMedicines = activeMedicines.take(8),
            latestVitals = latestVitals.take(6)
        )
    }

    fun generatePdfFile(context: Context, snapshot: HealthPassportSnapshot): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "HealthCareAI_HealthPassport_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.pdf"
        val outFile = File(exportDir, fileName)
        var pdf: PdfDocument? = null
        try {
            pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

        val headerBg = Paint().apply { color = Color.parseColor("#005F6B") }
        val accentBg = Paint().apply { color = Color.parseColor("#E6F7F9") }
        val alertBg = Paint().apply { color = Color.parseColor("#FFF0F0") }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#00A8B5")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val whiteTitle = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val whiteSub = Paint().apply {
            color = Color.parseColor("#D5F5F8")
            textSize = 11f
            isAntiAlias = true
        }
        val sectionTitle = Paint().apply {
            color = Color.parseColor("#005F6B")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1A2426")
            textSize = 11f
            isAntiAlias = true
        }
        val boldBody = Paint().apply {
            color = Color.parseColor("#1A2426")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val alertText = Paint().apply {
            color = Color.parseColor("#B91C1C")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Top Clinical Header Banner
        canvas.drawRect(0f, 0f, 595f, 96f, headerBg)
        canvas.drawText("HEALTH CARE AI — CLINICAL HEALTH PASSPORT", 32f, 42f, whiteTitle)
        val stamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        canvas.drawText("Model School Sayla • Generated: $stamp • Score: ${snapshot.healthScore}/100 (${snapshot.scoreTierLabel})", 32f, 66f, whiteSub)
        canvas.drawText("Created by Shailesh and Ruturaj • Emergency & Physician Reference Summary", 32f, 82f, whiteSub)

        var y = 120f

        // Patient Demographics & IDs Card
        canvas.drawRoundRect(RectF(28f, y - 14f, 567f, y + 92f), 10f, 10f, accentBg)
        canvas.drawRoundRect(RectF(28f, y - 14f, 567f, y + 92f), 10f, 10f, borderPaint)
        canvas.drawText("1. PATIENT IDENTITY & GOVERNMENT HEALTH IDs", 40f, y + 4f, sectionTitle)
        y += 24f
        canvas.drawText("Name: ${snapshot.patientName}   |   Age/Gender: ${if (snapshot.age > 0) "${snapshot.age} yrs" else "N/A"} / ${snapshot.gender}", 40f, y, boldBody)
        y += 18f
        canvas.drawText("Blood Group: ${snapshot.bloodGroup}   |   Emergency Contact: ${snapshot.emergencyContactName} (${snapshot.emergencyContactPhone})", 40f, y, bodyPaint)
        y += 18f
        canvas.drawText("PM-JAY Card: ${snapshot.pmjayCardNumber}   |   ABHA ID: ${snapshot.abhaCardNumber}", 40f, y, bodyPaint)
        y += 42f

        // Medical Alerts Card
        canvas.drawRoundRect(RectF(28f, y - 14f, 567f, y + 54f), 10f, 10f, alertBg)
        canvas.drawText("2. CRITICAL ALLERGIES & CHRONIC CONDITIONS", 40f, y + 4f, alertText)
        y += 22f
        canvas.drawText("Known Allergies: ${snapshot.allergies}", 40f, y, boldBody)
        y += 18f
        canvas.drawText("Chronic Conditions: ${snapshot.chronicConditions}", 40f, y, bodyPaint)
        y += 38f

        // Today's Wellness Snapshot
        canvas.drawText("3. DAILY WELLNESS SNAPSHOT", 32f, y, sectionTitle)
        y += 18f
        canvas.drawText(
            "Ultra Health Score: ${snapshot.healthScore}/100  |  Steps: ${snapshot.todaySteps}  |  Water: ${snapshot.todayWaterMl} ml  |  Calories: ${snapshot.todayCalories} kcal",
            32f,
            y,
            bodyPaint
        )
        y += 28f

        // Active Medicines
        canvas.drawText("4. ACTIVE MEDICATIONS (${snapshot.activeMedicines.size})", 32f, y, sectionTitle)
        y += 18f
        if (snapshot.activeMedicines.isEmpty()) {
            canvas.drawText("• No active medications recorded.", 40f, y, bodyPaint)
            y += 16f
        } else {
            snapshot.activeMedicines.forEach { med ->
                val line = "• ${med.name} — ${med.dosage.ifBlank { "As prescribed" }} (${med.timeOfDay.ifBlank { "Daily" }})"
                canvas.drawText(line.take(86), 40f, y, bodyPaint)
                y += 16f
            }
        }
        y += 16f

        // Recent Vitals
        canvas.drawText("5. RECENT VITALS (${snapshot.latestVitals.size})", 32f, y, sectionTitle)
        y += 18f
        val vitalFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
        if (snapshot.latestVitals.isEmpty()) {
            canvas.drawText("• No vital readings recorded yet.", 40f, y, bodyPaint)
            y += 16f
        } else {
            snapshot.latestVitals.forEach { v ->
                val pVal = if (v.primaryValue % 1.0 == 0.0) v.primaryValue.toInt().toString() else String.format(Locale.US, "%.1f", v.primaryValue)
                val sVal = if (v.secondaryValue % 1.0 == 0.0) v.secondaryValue.toInt().toString() else String.format(Locale.US, "%.1f", v.secondaryValue)
                val valueStr = if (v.secondaryValue > 0.0) "$pVal/$sVal ${v.unit}" else "$pVal ${v.unit}"
                val line = "• ${v.vitalType}: $valueStr [${v.status}] — ${vitalFmt.format(Date(v.timestamp))}"
                canvas.drawText(line.take(86), 40f, y, bodyPaint)
                y += 16f
            }
        }

        // Footer disclaimer
        val footerPaint = Paint().apply {
            color = Color.parseColor("#546E7A")
            textSize = 9.5f
            isAntiAlias = true
        }
        canvas.drawText(
            "Disclaimer: Generated from patient-logged records in Health Care AI for clinician reference. Not a medical diagnosis.",
            32f,
            814f,
            footerPaint
        )

            pdf.finishPage(page)

            FileOutputStream(outFile).use { fos ->
                pdf.writeTo(fos)
            }
            return outFile
        } catch (_: Throwable) {
            writePurePdfFallback(outFile, snapshot)
            return outFile
        } finally {
            runCatching { pdf?.close() }
        }
    }

    private fun writePurePdfFallback(outFile: File, snapshot: HealthPassportSnapshot) {
        fun esc(s: String): String = s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
            .filter { it.code in 32..126 }

        val dateStamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        val lines = mutableListOf(
            "HEALTH CARE AI - CLINICAL HEALTH PASSPORT",
            "Generated: $dateStamp",
            "----------------------------------------------------------------",
            "Patient: ${snapshot.patientName} | Age: ${snapshot.age} | Gender: ${snapshot.gender}",
            "Blood Group: ${snapshot.bloodGroup} | Health Score: ${snapshot.healthScore}/100 (${snapshot.scoreTierLabel})",
            "PMJAY Card: ${snapshot.pmjayCardNumber} | ABHA ID: ${snapshot.abhaCardNumber}",
            "Allergies: ${snapshot.allergies}",
            "Chronic Conditions: ${snapshot.chronicConditions}",
            "Emergency Contact: ${snapshot.emergencyContactName} (${snapshot.emergencyContactPhone})",
            "Today Metrics: Steps=${snapshot.todaySteps}, Water=${snapshot.todayWaterMl}ml, Calories=${snapshot.todayCalories}kcal",
            "Active Medications (${snapshot.activeMedicines.size}):"
        )
        snapshot.activeMedicines.forEach { med ->
            lines += "  - ${med.name} ${med.dosage} (${med.timeOfDay})"
        }
        lines += "Recent Vitals (${snapshot.latestVitals.size}):"
        snapshot.latestVitals.forEach { v ->
            lines += "  - ${v.vitalType}: ${v.primaryValue} ${v.unit} [${v.status}]"
        }
        lines += "Disclaimer: Patient-logged clinical summary for clinician reference."

        val streamContent = buildString {
            append("BT\n/F1 11 Tf\n40 790 Td\n16 TL\n")
            lines.forEachIndexed { idx, line ->
                if (idx == 0) append("(${esc(line)}) Tj\n")
                else append("T* (${esc(line)}) Tj\n")
            }
            append("ET\n")
        }

        val objects = listOf(
            "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n",
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n",
            "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n",
            "5 0 obj\n<< /Length ${streamContent.toByteArray(Charsets.US_ASCII).size} >>\nstream\n${streamContent}endstream\nendobj\n"
        )

        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        val offsets = mutableListOf(0)
        for (obj in objects) {
            offsets += sb.length
            sb.append(obj)
        }
        val xrefPos = sb.length
        sb.append("xref\n0 ${objects.size + 1}\n")
        sb.append("0000000000 65535 f \n")
        for (i in 1..objects.size) {
            sb.append(String.format(Locale.US, "%010d 00000 n \n", offsets[i]))
        }
        sb.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xrefPos\n%%EOF\n")
        outFile.writeBytes(sb.toString().toByteArray(Charsets.US_ASCII))
    }

    fun exportAndSharePassportPdf(
        context: Context,
        snapshot: HealthPassportSnapshot,
        language: String = "English"
    ): Boolean {
        return try {
            val file = generatePdfFile(context, snapshot)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Health Care AI — Clinical Health Passport (${snapshot.patientName})")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Clinical Health Passport PDF for ${snapshot.patientName} (Blood Group: ${snapshot.bloodGroup}, Health Score: ${snapshot.healthScore}/100)."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(
                shareIntent,
                trLang(language, "Share Health Passport PDF", "હેલ્થ પાસપોર્ટ PDF શેર કરો", "हेल्थ पासपोर्ट PDF शेयर करें")
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
                    "Unable to generate or share Health Passport PDF.",
                    "હેલ્થ પાસપોર્ટ PDF બનાવી કે શેર કરી શકાઈ નહીં.",
                    "हेल्थ पासपोर्ट PDF बनाने या शेयर करने में समस्या आई।"
                ),
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }
}

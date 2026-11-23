package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.MedicationAdherenceEntity
import com.example.data.local.MedicineEntity
import com.example.data.local.SymptomLogEntity
import com.example.data.local.SymptomNoteEntity
import com.example.data.local.UserProfileEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val label: String
) {
    CSV("csv", "text/csv", "CSV"),
    JSON("json", "application/json", "JSON")
}

object HealthDataExportHelper {

    private fun isoFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private fun dateKeyFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun fileStampFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun formatIsoUtc(timestampMillis: Long): String = isoFormat().format(Date(timestampMillis))

    fun formatDateKey(timestampMillis: Long): String = dateKeyFormat().format(Date(timestampMillis))

    /**
     * Escapes a single CSV cell following RFC 4180 rules:
     * wrap in double quotes if it contains commas, quotes, CR, or LF, and double internal quotes.
     */
    fun escapeCsvField(raw: String): String {
        val normalized = raw.replace("\r\n", "\n").replace("\r", "\n")
        val needsQuotes = normalized.contains(',') ||
            normalized.contains('"') ||
            normalized.contains('\n')
        if (!needsQuotes) return normalized
        return "\"${normalized.replace("\"", "\"\"")}\""
    }

    private fun csvRow(vararg columns: String): String =
        columns.joinToString(",") { escapeCsvField(it) }

    fun buildFileName(format: ExportFormat, timestampMillis: Long = System.currentTimeMillis()): String {
        val stamp = fileStampFormat().format(Date(timestampMillis))
        return "HealthCareAI_Clinical_Data_$stamp.${format.extension}"
    }

    /**
     * Builds an RFC 4180 compliant CSV containing patient metadata, prescribed medications,
     * medication adherence logs, daily symptom logs, and timestamped follow-up notes.
     */
    fun buildClinicalExportCsv(
        profile: UserProfileEntity?,
        medicines: List<MedicineEntity>,
        adherenceLogs: List<MedicationAdherenceEntity>,
        symptomLogs: List<SymptomLogEntity>,
        symptomNotes: List<SymptomNoteEntity>,
        exportedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val sb = StringBuilder()
        sb.append(
            csvRow(
                "record_type",
                "record_id",
                "date",
                "timestamp_iso",
                "title_or_medicine",
                "dosage_or_location",
                "severity_or_status",
                "schedule_or_duration",
                "notes"
            )
        ).append('\n')

        val exportIso = formatIsoUtc(exportedAtMillis)
        val exportDate = formatDateKey(exportedAtMillis)

        if (profile != null) {
            val healthIdsSuffix = buildList {
                if (profile.pmjayCardNumber.isNotBlank()) add("PMJAY: ${profile.pmjayCardNumber}")
                if (profile.abhaCardNumber.isNotBlank()) add("ABHA: ${HealthCardValidator.formatAbhaForDisplay(profile.abhaCardNumber)}")
            }.joinToString(" | ")
            val profileNotes = buildString {
                append("Conditions: ${profile.chronicConditions} | Medications: ${profile.currentMedications}")
                if (healthIdsSuffix.isNotBlank()) {
                    append(" | ").append(healthIdsSuffix)
                }
            }
            sb.append(
                csvRow(
                    "PATIENT_PROFILE",
                    profile.id.toString(),
                    exportDate,
                    exportIso,
                    profile.fullName,
                    "Age ${profile.age} | ${profile.gender} | Blood: ${profile.bloodGroup}",
                    "ACTIVE",
                    "Allergies: ${profile.allergies}",
                    profileNotes
                )
            ).append('\n')
        }

        // Medication Adherence History (most recent first)
        adherenceLogs.sortedByDescending { it.actionTimestamp }.forEach { log ->
            sb.append(
                csvRow(
                    "MEDICATION_ADHERENCE",
                    log.id.toString(),
                    log.scheduledDate.ifBlank { formatDateKey(log.actionTimestamp) },
                    formatIsoUtc(log.actionTimestamp),
                    log.medicineName,
                    log.dosage,
                    log.status,
                    log.scheduledTime,
                    log.notes
                )
            ).append('\n')
        }

        // Active / Paused Medication Schedule
        medicines.sortedBy { it.name.lowercase(Locale.US) }.forEach { med ->
            val rxNote = buildString {
                append(med.instruction)
                if (med.prescribedBy.isNotBlank()) {
                    append(" | Prescribed by: ").append(med.prescribedBy)
                }
            }
            sb.append(
                csvRow(
                    "MEDICATION_SCHEDULE",
                    med.id.toString(),
                    formatDateKey(med.createdAt),
                    formatIsoUtc(med.createdAt),
                    med.name,
                    med.dosage,
                    if (med.isActive) "ACTIVE" else "PAUSED",
                    "${med.timeOfDay} (Every ${med.frequencyHours}h)",
                    rxNote
                )
            ).append('\n')
        }

        // Daily Symptom Logs (most recent first)
        val symptomTitleById = symptomLogs.associate { it.id to it.symptomTitle }
        symptomLogs.sortedByDescending { it.timestamp }.forEach { log ->
            val dateKey = log.dateKey.ifBlank { formatDateKey(log.timestamp) }
            sb.append(
                csvRow(
                    "SYMPTOM_LOG",
                    log.id.toString(),
                    dateKey,
                    formatIsoUtc(log.timestamp),
                    log.symptomTitle,
                    log.bodyLocation,
                    "${log.severity}/10 (${log.status})",
                    log.duration,
                    log.notesOrTriggers
                )
            ).append('\n')
        }

        // Timestamped Follow-Up Symptom Notes (most recent first)
        symptomNotes.sortedByDescending { it.timestamp }.forEach { note ->
            val parentTitle = symptomTitleById[note.symptomLogId] ?: "Symptom #${note.symptomLogId}"
            sb.append(
                csvRow(
                    "SYMPTOM_NOTE",
                    note.id.toString(),
                    formatDateKey(note.timestamp),
                    formatIsoUtc(note.timestamp),
                    parentTitle,
                    "SymptomLog:${note.symptomLogId}",
                    "${note.severitySnapshot}/10 (Follow-up)",
                    "",
                    note.noteText
                )
            ).append('\n')
        }

        return sb.toString()
    }

    /**
     * Builds a structured JSON clinical export document with nested follow-up notes and adherence summary.
     */
    fun buildClinicalExportJson(
        profile: UserProfileEntity?,
        medicines: List<MedicineEntity>,
        adherenceLogs: List<MedicationAdherenceEntity>,
        symptomLogs: List<SymptomLogEntity>,
        symptomNotes: List<SymptomNoteEntity>,
        exportedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val takenCount = adherenceLogs.count { it.status.equals("TAKEN", ignoreCase = true) }
        val skippedCount = adherenceLogs.count { it.status.equals("SKIPPED", ignoreCase = true) }
        val snoozedCount = adherenceLogs.count { it.status.equals("SNOOZED", ignoreCase = true) }
        val adherenceDenom = takenCount + skippedCount
        val adherenceRate = if (adherenceDenom > 0) ((takenCount * 100.0) / adherenceDenom).toInt() else 100

        val notesBySymptomId = symptomNotes.sortedByDescending { it.timestamp }.groupBy { it.symptomLogId }

        val metadataObj = JSONObject().apply {
            put("appName", "Health Care AI")
            put("schemaVersion", "1.0")
            put("exportedAtMillis", exportedAtMillis)
            put("exportedAtIso", formatIsoUtc(exportedAtMillis))
            put("disclaimer", "Patient-reported medication adherence and symptom diary exported for healthcare provider review.")
            put("counts", JSONObject().apply {
                put("medicines", medicines.size)
                put("adherenceRecords", adherenceLogs.size)
                put("symptomLogs", symptomLogs.size)
                put("followUpNotes", symptomNotes.size)
            })
        }

        val profileObj = JSONObject().apply {
            if (profile != null) {
                put("fullName", profile.fullName)
                put("age", profile.age)
                put("gender", profile.gender)
                put("bloodGroup", profile.bloodGroup)
                put("allergies", profile.allergies)
                put("chronicConditions", profile.chronicConditions)
                put("currentMedications", profile.currentMedications)
                put("pastSurgeries", profile.pastSurgeries)
                put("pmjayCardNumber", profile.pmjayCardNumber)
                put("abhaCardNumber", profile.abhaCardNumber)
                put("emergencyContactName", profile.emergencyContactName)
                put("emergencyContactPhone", profile.emergencyContactPhone)
            } else {
                put("configured", false)
            }
        }

        val adherenceSummaryObj = JSONObject().apply {
            put("totalRecords", adherenceLogs.size)
            put("takenCount", takenCount)
            put("skippedCount", skippedCount)
            put("snoozedCount", snoozedCount)
            put("adherenceRatePercent", adherenceRate)
        }

        val medicinesArray = JSONArray()
        medicines.sortedBy { it.name.lowercase(Locale.US) }.forEach { med ->
            medicinesArray.put(JSONObject().apply {
                put("id", med.id)
                put("name", med.name)
                put("dosage", med.dosage)
                put("timeOfDay", med.timeOfDay)
                put("instruction", med.instruction)
                put("frequencyHours", med.frequencyHours)
                put("prescribedBy", med.prescribedBy)
                put("isActive", med.isActive)
                put("createdAtIso", formatIsoUtc(med.createdAt))
            })
        }

        val adherenceArray = JSONArray()
        adherenceLogs.sortedByDescending { it.actionTimestamp }.forEach { log ->
            adherenceArray.put(JSONObject().apply {
                put("id", log.id)
                put("medicineId", log.medicineId)
                put("medicineName", log.medicineName)
                put("dosage", log.dosage)
                put("scheduledDate", log.scheduledDate.ifBlank { formatDateKey(log.actionTimestamp) })
                put("scheduledTime", log.scheduledTime)
                put("status", log.status)
                put("actionTimestampMillis", log.actionTimestamp)
                put("actionTimestampIso", formatIsoUtc(log.actionTimestamp))
                put("notes", log.notes)
            })
        }

        val symptomArray = JSONArray()
        symptomLogs.sortedByDescending { it.timestamp }.forEach { log ->
            val severityCategory = when {
                log.severity <= 3 -> "Mild"
                log.severity <= 6 -> "Moderate"
                log.severity <= 8 -> "Severe"
                else -> "Very Severe"
            }
            val nestedNotes = JSONArray()
            notesBySymptomId[log.id].orEmpty().forEach { note ->
                nestedNotes.put(JSONObject().apply {
                    put("id", note.id)
                    put("noteText", note.noteText)
                    put("severitySnapshot", note.severitySnapshot)
                    put("timestampMillis", note.timestamp)
                    put("timestampIso", formatIsoUtc(note.timestamp))
                })
            }
            symptomArray.put(JSONObject().apply {
                put("id", log.id)
                put("symptomTitle", log.symptomTitle)
                put("severity", log.severity)
                put("severityCategory", severityCategory)
                put("bodyLocation", log.bodyLocation)
                put("duration", log.duration)
                put("status", log.status)
                put("dateKey", log.dateKey.ifBlank { formatDateKey(log.timestamp) })
                put("timestampMillis", log.timestamp)
                put("timestampIso", formatIsoUtc(log.timestamp))
                put("notesOrTriggers", log.notesOrTriggers)
                put("followUpNotes", nestedNotes)
            })
        }

        val root = JSONObject().apply {
            put("exportMetadata", metadataObj)
            put("patientProfile", profileObj)
            put("medicationSummary", adherenceSummaryObj)
            put("medicationSchedule", medicinesArray)
            put("medicationAdherenceHistory", adherenceArray)
            put("symptomLogs", symptomArray)
        }
        return root.toString(2)
    }

    /**
     * Writes an export file inside cacheDir/exports so FileProvider can grant temporary read access
     * to receiving apps without requesting any broad external storage permissions.
     */
    fun createCacheExportFile(
        context: Context,
        format: ExportFormat,
        content: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): File {
        val exportsDir = File(context.cacheDir, "exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()
        val file = File(exportsDir, buildFileName(format, timestampMillis))
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    /**
     * Saves CSV or JSON clinical data into the user's Downloads folder via scoped MediaStore (API 29+).
     */
    fun saveExportFileToDownloads(
        context: Context,
        format: ExportFormat,
        content: String
    ): String? {
        val fileName = buildFileName(format)
        return try {
            val savedLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                } ?: return null
                "Downloads/$fileName"
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
                file.absolutePath
            }
            DevLogger.i("ClinicalExport", "Saved ${format.label} export (${content.length} chars) to $savedLocation")
            savedLocation
        } catch (e: Exception) {
            DevLogger.e("ClinicalExport", "Failed to save ${format.label} export: ${e.message}")
            null
        }
    }

    /**
     * Generates a FileProvider-backed URI for the CSV/JSON file and opens Android's share sheet
     * so the patient can send it to a healthcare provider (email, messaging, Drive, etc.).
     */
    fun shareExportFile(
        context: Context,
        format: ExportFormat,
        content: String,
        patientName: String? = null
    ): Boolean {
        return try {
            val file = createCacheExportFile(context, format, content)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val displayPatient = patientName?.takeIf { it.isNotBlank() } ?: "Patient"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Health Care AI - $displayPatient Medication Adherence & Symptom Logs (${format.label})"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Attached is the exported ${format.label} file containing medication adherence history and symptom logs for healthcare provider review."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(
                sendIntent,
                "Share ${format.label} Clinical Data with Healthcare Provider"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            DevLogger.i("ClinicalExport", "Shared ${format.label} clinical export (${file.name}, ${file.length()} bytes)")
            true
        } catch (e: Exception) {
            DevLogger.e("ClinicalExport", "Failed to share ${format.label} export: ${e.message}")
            false
        }
    }
}

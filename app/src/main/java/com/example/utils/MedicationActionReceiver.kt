package com.example.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.MedicationAdherenceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles interactive notification actions ("Mark Taken", "Snooze 15m", "Skip")
 * directly from medication reminder notifications and persists adherence history in Room.
 */
class MedicationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_TAKEN = "com.example.action.MEDICATION_MARK_TAKEN"
        const val ACTION_SNOOZE = "com.example.action.MEDICATION_SNOOZE"
        const val ACTION_SKIP = "com.example.action.MEDICATION_SKIP"

        const val EXTRA_MED_ID = "extra_med_id"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_MED_DOSAGE = "extra_med_dosage"
        const val EXTRA_MED_INSTRUCTION = "extra_med_instruction"
        const val EXTRA_MED_TIME = "extra_med_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
        if (medId <= 0L) return

        val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Prescribed Medication"
        val dosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: "1 Dose"
        val instruction = intent.getStringExtra(EXTRA_MED_INSTRUCTION) ?: "As prescribed"
        val scheduledTime = intent.getStringExtra(EXTRA_MED_TIME) ?: "08:00 AM"

        // Dismiss the active notification immediately for crisp feedback
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel((3000 + medId).toInt())

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(context).triageDao()
                val medicine = dao.getMedicineById(medId)
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                val newStatus = when (action) {
                    ACTION_MARK_TAKEN -> "TAKEN"
                    ACTION_SNOOZE -> "SNOOZED"
                    ACTION_SKIP -> "SKIPPED"
                    else -> return@launch
                }

                if (medicine != null) {
                    val existing = dao.getAdherenceLogForMedicineAndDate(medId, todayDate)
                    if (existing != null) {
                        dao.updateAdherenceLog(
                            existing.copy(
                                status = newStatus,
                                actionTimestamp = System.currentTimeMillis(),
                                notes = when (action) {
                                    ACTION_MARK_TAKEN -> "Confirmed from reminder notification"
                                    ACTION_SNOOZE -> "Snoozed 15 minutes from notification"
                                    ACTION_SKIP -> "Skipped from notification"
                                    else -> existing.notes
                                }
                            )
                        )
                    } else {
                        dao.insertAdherenceLog(
                            MedicationAdherenceEntity(
                                medicineId = medicine.id,
                                medicineName = medicine.name,
                                dosage = medicine.dosage,
                                scheduledTime = medicine.timeOfDay,
                                scheduledDate = todayDate,
                                status = newStatus,
                                actionTimestamp = System.currentTimeMillis(),
                                notes = when (action) {
                                    ACTION_MARK_TAKEN -> "Confirmed from reminder notification"
                                    ACTION_SNOOZE -> "Snoozed 15 minutes from notification"
                                    ACTION_SKIP -> "Skipped from notification"
                                    else -> ""
                                }
                            )
                        )
                    }
                }

                when (action) {
                    ACTION_SNOOZE -> {
                        MedicineReminderWorker.scheduleSnoozeReminder(
                            context = context,
                            medId = medId,
                            medName = medName,
                            dosage = dosage,
                            instruction = instruction,
                            timeOfDay = scheduledTime,
                            snoozeMinutes = 15
                        )
                    }
                    ACTION_MARK_TAKEN, ACTION_SKIP -> {
                        MedicineReminderWorker.cancelSnoozeReminder(context, medId)
                    }
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}

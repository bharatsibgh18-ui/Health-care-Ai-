package com.example.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.WaterLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles exact background AlarmManager triggers for Medicine & Water reminders
 * so outside-the-app WhatsApp-style heads-up notifications fire reliably even when closed.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MEDICINE_ALARM = "com.example.action.EXACT_MEDICINE_ALARM"
        const val ACTION_MEDICINE_SNOOZE_ALARM = "com.example.action.EXACT_MEDICINE_SNOOZE_ALARM"
        const val ACTION_WATER_ALARM = "com.example.action.EXACT_WATER_ALARM"
        const val ACTION_LOG_WATER_250ML = "com.example.action.LOG_WATER_250ML"

        const val EXTRA_MED_ID = "extra_med_id"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_MED_DOSAGE = "extra_med_dosage"
        const val EXTRA_MED_INSTRUCTION = "extra_med_instruction"
        const val EXTRA_MED_TIME = "extra_med_time"
        const val EXTRA_FREQ_HOURS = "extra_freq_hours"
        const val EXTRA_WATER_INTERVAL_HOURS = "extra_water_interval_hours"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTION_LOG_WATER_250ML -> {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(WaterReminderNotificationHelper.NOTIFICATION_ID)
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        AppDatabase.getDatabase(context).triageDao().insertWaterLog(
                            WaterLogEntity(amountMl = 250, dateString = today)
                        )
                        DevLogger.i("WaterReminder", "Logged +250 ml from outside notification action")
                    } catch (_: Exception) {
                    } finally {
                        pending.finish()
                    }
                }
            }

            ACTION_WATER_ALARM -> {
                val intervalHours = intent.getIntExtra(
                    EXTRA_WATER_INTERVAL_HOURS,
                    WaterReminderNotificationHelper.getSavedIntervalHours(context)
                ).coerceIn(1, 8)
                WaterReminderNotificationHelper.showWaterReminderNotification(context)
                val nextTrigger = System.currentTimeMillis() + intervalHours * 60L * 60L * 1000L
                WaterReminderNotificationHelper.scheduleWaterAlarmAt(context, nextTrigger, intervalHours)
            }

            ACTION_MEDICINE_ALARM, ACTION_MEDICINE_SNOOZE_ALARM -> {
                val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
                val isSnooze = action == ACTION_MEDICINE_SNOOZE_ALARM
                val fallbackName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Prescribed Medicine"
                val fallbackDosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: "1 Dose"
                val fallbackInst = intent.getStringExtra(EXTRA_MED_INSTRUCTION) ?: "As prescribed"
                val fallbackTime = intent.getStringExtra(EXTRA_MED_TIME) ?: "08:00 AM"
                val fallbackFreq = intent.getIntExtra(EXTRA_FREQ_HOURS, 24).coerceIn(1, 24)

                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        var medName = fallbackName
                        var dosage = fallbackDosage
                        var instruction = fallbackInst
                        var timeOfDay = fallbackTime
                        var freqHours = fallbackFreq
                        var shouldShow = true

                        if (medId > 0L) {
                            val dao = AppDatabase.getDatabase(context).triageDao()
                            val currentMed = dao.getMedicineById(medId)
                            if (currentMed != null) {
                                if (!currentMed.isActive) {
                                    shouldShow = false
                                } else {
                                    medName = currentMed.name
                                    dosage = currentMed.dosage
                                    instruction = currentMed.instruction
                                    timeOfDay = currentMed.timeOfDay
                                    freqHours = currentMed.frequencyHours.coerceIn(1, 24)
                                }
                            }
                            if (shouldShow && !isSnooze) {
                                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                val todayLog = dao.getAdherenceLogForMedicineAndDate(medId, todayDate)
                                if (todayLog?.status == "TAKEN") {
                                    shouldShow = false
                                }
                            }
                        }

                        if (shouldShow) {
                            MedicineReminderWorker.showReminderNotificationInternal(
                                context = context,
                                medId = medId,
                                medName = medName,
                                dosage = dosage,
                                instruction = instruction,
                                timeOfDay = timeOfDay,
                                isSnooze = isSnooze
                            )
                        }

                        // Reschedule next recurring exact alarm for regular medicine schedule
                        if (!isSnooze && medId > 0L) {
                            MedicineReminderWorker.scheduleMedicineExactAlarm(
                                context = context,
                                medId = medId,
                                medName = medName,
                                dosage = dosage,
                                instruction = instruction,
                                timeOfDay = timeOfDay,
                                frequencyHours = freqHours
                            )
                        }
                    } catch (_: Exception) {
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}

/**
 * Restores outside-the-app medicine & water alarms after device reboot.
 */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (WaterReminderNotificationHelper.isWaterReminderEnabled(context)) {
                    val hours = WaterReminderNotificationHelper.getSavedIntervalHours(context)
                    WaterReminderNotificationHelper.scheduleWaterReminder(context, hours)
                }
                val activeMeds = AppDatabase.getDatabase(context).triageDao().getActiveMedicinesOnce()
                activeMeds.forEach { med ->
                    MedicineReminderWorker.scheduleDailyMedicineReminder(
                        context = context,
                        medId = med.id,
                        medName = med.name,
                        dosage = med.dosage,
                        instruction = med.instruction,
                        timeOfDay = med.timeOfDay,
                        frequencyHours = med.frequencyHours
                    )
                }
                DevLogger.i("BootReceiver", "Rescheduled ${activeMeds.size} medicine & water alarms on boot")
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }
}

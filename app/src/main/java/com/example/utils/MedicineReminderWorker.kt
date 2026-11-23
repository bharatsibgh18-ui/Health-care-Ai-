package com.example.utils

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MedicineReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "medicine_reminder_channel"
        const val KEY_MED_ID = "key_med_id"
        const val KEY_MED_NAME = "key_med_name"
        const val KEY_MED_DOSAGE = "key_med_dosage"
        const val KEY_MED_INSTRUCTION = "key_med_instruction"
        const val KEY_MED_TIME = "key_med_time"
        const val KEY_IS_SNOOZE = "key_is_snooze"
        const val WORK_NAME_PREFIX = "med_reminder_work_"
        const val SNOOZE_WORK_PREFIX = "med_snooze_work_"
        const val WORK_TAG_MEDICATION = "medication_reminder_worker"

        /**
         * Parses time strings like "08:00 AM", "01:30 PM", or "21:15" into (hour24, minute).
         */
        fun parseTimeOfDay(timeOfDay: String): Pair<Int, Int> {
            val amPmMatch = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""", RegexOption.IGNORE_CASE).find(timeOfDay.trim())
            if (amPmMatch != null) {
                val rawHour = (amPmMatch.groupValues[1].toIntOrNull() ?: 8).coerceIn(1, 12)
                val minute = (amPmMatch.groupValues[2].toIntOrNull() ?: 0).coerceIn(0, 59)
                val isPm = amPmMatch.groupValues[3].equals("PM", ignoreCase = true)
                val hour24 = when {
                    isPm && rawHour < 12 -> rawHour + 12
                    !isPm && rawHour == 12 -> 0
                    else -> rawHour
                }
                return hour24 to minute
            }

            val twentyFourMatch = Regex("""(\d{1,2}):(\d{2})""").find(timeOfDay.trim())
            if (twentyFourMatch != null) {
                val hour24 = (twentyFourMatch.groupValues[1].toIntOrNull() ?: 8).coerceIn(0, 23)
                val minute = (twentyFourMatch.groupValues[2].toIntOrNull() ?: 0).coerceIn(0, 59)
                return hour24 to minute
            }
            return 8 to 0
        }

        /**
         * Calculates initial delay in milliseconds until the next scheduled occurrence of [timeOfDay].
         */
        fun computeInitialDelayMillis(
            timeOfDay: String,
            frequencyHours: Int = 24,
            nowMillis: Long = System.currentTimeMillis()
        ): Long {
            val (hour24, minute) = parseTimeOfDay(timeOfDay)
            val stepHours = frequencyHours.coerceIn(1, 24)
            val targetCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour24)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            while (targetCal.timeInMillis <= nowMillis) {
                targetCal.add(Calendar.HOUR_OF_DAY, stepHours)
            }
            return (targetCal.timeInMillis - nowMillis).coerceAtLeast(0L)
        }

        fun scheduleDailyMedicineReminder(
            context: Context,
            medId: Long,
            medName: String,
            dosage: String,
            instruction: String,
            timeOfDay: String = "08:00 AM",
            frequencyHours: Int = 24
        ) {
            val repeatHours = frequencyHours.coerceIn(1, 24).toLong()
            val initialDelayMs = computeInitialDelayMillis(timeOfDay, frequencyHours)
            val data = workDataOf(
                KEY_MED_ID to medId,
                KEY_MED_NAME to medName,
                KEY_MED_DOSAGE to dosage,
                KEY_MED_INSTRUCTION to instruction,
                KEY_MED_TIME to timeOfDay,
                KEY_IS_SNOOZE to false
            )

            val reminderRequest = PeriodicWorkRequestBuilder<MedicineReminderWorker>(
                repeatHours, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(WORK_TAG_MEDICATION)
                .addTag("med_$medId")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "$WORK_NAME_PREFIX$medId",
                ExistingPeriodicWorkPolicy.UPDATE,
                reminderRequest
            )
            scheduleMedicineExactAlarm(
                context = context,
                medId = medId,
                medName = medName,
                dosage = dosage,
                instruction = instruction,
                timeOfDay = timeOfDay,
                frequencyHours = frequencyHours,
                delayOverrideMs = initialDelayMs
            )
            val scheduledAtMillis = System.currentTimeMillis() + initialDelayMs.coerceAtLeast(10_000L)
            OneSignalNotificationService.schedulePushAsync(
                context = context,
                title = "💊 Medication Reminder: $medName",
                message = "Time to take $medName ($dosage • $instruction) at $timeOfDay.",
                scheduledAtMillis = scheduledAtMillis,
                extraData = mapOf(
                    "reminder_type" to "medicine",
                    "med_id" to medId.toString(),
                    "screen" to "MEDICINE_REMINDER"
                )
            )
            DevLogger.i(
                "WorkManager",
                "Scheduled medication #$medId '$medName' at $timeOfDay (every ${repeatHours}h, initialDelay=${initialDelayMs / 60000}m + exact AlarmManager + OneSignal ISO push)"
            )
        }

        fun scheduleMedicineExactAlarm(
            context: Context,
            medId: Long,
            medName: String,
            dosage: String,
            instruction: String,
            timeOfDay: String,
            frequencyHours: Int,
            delayOverrideMs: Long? = null
        ) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val delayMs = delayOverrideMs ?: computeInitialDelayMillis(timeOfDay, frequencyHours)
                val triggerAtMillis = System.currentTimeMillis() + delayMs.coerceAtLeast(5_000L)
                val alarmIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                    action = ReminderAlarmReceiver.ACTION_MEDICINE_ALARM
                    putExtra(ReminderAlarmReceiver.EXTRA_MED_ID, medId)
                    putExtra(ReminderAlarmReceiver.EXTRA_MED_NAME, medName)
                    putExtra(ReminderAlarmReceiver.EXTRA_MED_DOSAGE, dosage)
                    putExtra(ReminderAlarmReceiver.EXTRA_MED_INSTRUCTION, instruction)
                    putExtra(ReminderAlarmReceiver.EXTRA_MED_TIME, timeOfDay)
                    putExtra(ReminderAlarmReceiver.EXTRA_FREQ_HOURS, frequencyHours)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    (30000 + medId).toInt(),
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                }
            } catch (_: SecurityException) {
                try {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                    val delayMs = delayOverrideMs ?: computeInitialDelayMillis(timeOfDay, frequencyHours)
                    val triggerAtMillis = System.currentTimeMillis() + delayMs.coerceAtLeast(5_000L)
                    val alarmIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                        action = ReminderAlarmReceiver.ACTION_MEDICINE_ALARM
                        putExtra(ReminderAlarmReceiver.EXTRA_MED_ID, medId)
                        putExtra(ReminderAlarmReceiver.EXTRA_MED_NAME, medName)
                        putExtra(ReminderAlarmReceiver.EXTRA_MED_DOSAGE, dosage)
                        putExtra(ReminderAlarmReceiver.EXTRA_MED_INSTRUCTION, instruction)
                        putExtra(ReminderAlarmReceiver.EXTRA_MED_TIME, timeOfDay)
                        putExtra(ReminderAlarmReceiver.EXTRA_FREQ_HOURS, frequencyHours)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context,
                        (30000 + medId).toInt(),
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }

        fun scheduleSnoozeReminder(
            context: Context,
            medId: Long,
            medName: String,
            dosage: String,
            instruction: String,
            timeOfDay: String = "08:00 AM",
            snoozeMinutes: Int = 15
        ) {
            val safeMins = snoozeMinutes.toLong().coerceAtLeast(1L)
            val data = workDataOf(
                KEY_MED_ID to medId,
                KEY_MED_NAME to medName,
                KEY_MED_DOSAGE to dosage,
                KEY_MED_INSTRUCTION to instruction,
                KEY_MED_TIME to timeOfDay,
                KEY_IS_SNOOZE to true
            )

            val snoozeRequest = OneTimeWorkRequestBuilder<MedicineReminderWorker>()
                .setInitialDelay(safeMins, TimeUnit.MINUTES)
                .setInputData(data)
                .addTag(WORK_TAG_MEDICATION)
                .addTag("med_snooze_$medId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "$SNOOZE_WORK_PREFIX$medId",
                ExistingWorkPolicy.REPLACE,
                snoozeRequest
            )

            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val triggerAt = System.currentTimeMillis() + safeMins * 60_000L
                val alarmIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                    action = ReminderAlarmReceiver.ACTION_MEDICINE_SNOOZE_ALARM
                    putExtra(ReminderAlarmReceiver.EXTRA_MED_ID, medId)
                    putExtra(ReminderAlarmReceiver.EXTRA_MED_NAME, medName)
                    putExtra(ReminderAlarmReceiver.EXTRA_MED_DOSAGE, dosage)
                    putExtra(ReminderAlarmReceiver.EXTRA_MED_INSTRUCTION, instruction)
                    putExtra(ReminderAlarmReceiver.EXTRA_MED_TIME, timeOfDay)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    (40000 + medId).toInt(),
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } catch (_: Exception) {}

            val snoozeTriggerAt = System.currentTimeMillis() + safeMins * 60_000L
            OneSignalNotificationService.schedulePushAsync(
                context = context,
                title = "⏰ Snoozed Reminder: $medName",
                message = "Snoozed dose ready: $medName ($dosage • $instruction).",
                scheduledAtMillis = snoozeTriggerAt,
                extraData = mapOf(
                    "reminder_type" to "medicine_snooze",
                    "med_id" to medId.toString(),
                    "screen" to "MEDICINE_REMINDER"
                )
            )
        }

        fun cancelSnoozeReminder(context: Context, medId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("$SNOOZE_WORK_PREFIX$medId")
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val snoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                    action = ReminderAlarmReceiver.ACTION_MEDICINE_SNOOZE_ALARM
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    (40000 + medId).toInt(),
                    snoozeIntent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pi != null) alarmManager?.cancel(pi)
            } catch (_: Exception) {}
        }

        fun cancelMedicineReminder(context: Context, medId: Long) {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork("$WORK_NAME_PREFIX$medId")
            wm.cancelUniqueWork("$SNOOZE_WORK_PREFIX$medId")
            cancelSnoozeReminder(context, medId)
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val alarmIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                    action = ReminderAlarmReceiver.ACTION_MEDICINE_ALARM
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    (30000 + medId).toInt(),
                    alarmIntent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pi != null) alarmManager?.cancel(pi)
            } catch (_: Exception) {}
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel((3000 + medId).toInt())
            DevLogger.d("WorkManager", "Cancelled medication reminder #$medId")
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Medicine Reminders (દવા રિમાઇન્ડર)"
                val descriptionText = "Outside-the-app WhatsApp-style medicine alerts"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 350, 150, 350)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(soundUri, attrs)
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun showImmediateNotification(
            context: Context,
            medId: Long,
            medName: String,
            dosage: String,
            instruction: String,
            timeOfDay: String = "08:00 AM"
        ) {
            showReminderNotificationInternal(
                context = context,
                medId = medId,
                medName = medName,
                dosage = dosage,
                instruction = instruction,
                timeOfDay = timeOfDay,
                isSnooze = false
            )
        }

        internal fun showReminderNotificationInternal(
            context: Context,
            medId: Long,
            medName: String,
            dosage: String,
            instruction: String,
            timeOfDay: String,
            isSnooze: Boolean
        ) {
            createNotificationChannel(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionCheck = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_SCREEN, "MEDICINE_REMINDER")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                medId.toInt(),
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val safeMedId = if (medId > 0L) medId else 1L
            val takenIntent = Intent(context, MedicationActionReceiver::class.java).apply {
                action = MedicationActionReceiver.ACTION_MARK_TAKEN
                putExtra(MedicationActionReceiver.EXTRA_MED_ID, safeMedId)
                putExtra(MedicationActionReceiver.EXTRA_MED_NAME, medName)
                putExtra(MedicationActionReceiver.EXTRA_MED_DOSAGE, dosage)
                putExtra(MedicationActionReceiver.EXTRA_MED_INSTRUCTION, instruction)
                putExtra(MedicationActionReceiver.EXTRA_MED_TIME, timeOfDay)
            }
            val takenPendingIntent = PendingIntent.getBroadcast(
                context,
                (10000 + safeMedId).toInt(),
                takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, MedicationActionReceiver::class.java).apply {
                action = MedicationActionReceiver.ACTION_SNOOZE
                putExtra(MedicationActionReceiver.EXTRA_MED_ID, safeMedId)
                putExtra(MedicationActionReceiver.EXTRA_MED_NAME, medName)
                putExtra(MedicationActionReceiver.EXTRA_MED_DOSAGE, dosage)
                putExtra(MedicationActionReceiver.EXTRA_MED_INSTRUCTION, instruction)
                putExtra(MedicationActionReceiver.EXTRA_MED_TIME, timeOfDay)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                (20000 + safeMedId).toInt(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val titlePrefix = if (isSnooze) "⏰ Snoozed Reminder" else "💊 Medication Reminder"
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_health)
                .setContentTitle("$titlePrefix: $medName")
                .setContentText("$dosage • $instruction ($timeOfDay)")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("💊 Time to take: $medName\nDosage: $dosage\nSchedule: $timeOfDay ($instruction)\nTap 'Mark Taken' to log adherence or 'Snooze 15m'.")
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(longArrayOf(0, 350, 150, 350))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            if (medId > 0L) {
                builder.addAction(
                    android.R.drawable.checkbox_on_background,
                    "Mark Taken",
                    takenPendingIntent
                )
                builder.addAction(
                    android.R.drawable.ic_popup_reminder,
                    "Snooze 15m",
                    snoozePendingIntent
                )
            }

            try {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.notify((3000 + medId).toInt(), builder.build())
            } catch (_: Exception) {}
        }
    }

    override suspend fun doWork(): Result {
        val medId = inputData.getLong(KEY_MED_ID, 0L)
        var medName = inputData.getString(KEY_MED_NAME) ?: "Prescribed Medicine"
        var dosage = inputData.getString(KEY_MED_DOSAGE) ?: "1 Dose"
        var instruction = inputData.getString(KEY_MED_INSTRUCTION) ?: "As prescribed"
        var timeOfDay = inputData.getString(KEY_MED_TIME) ?: "08:00 AM"
        val isSnooze = inputData.getBoolean(KEY_IS_SNOOZE, false)

        // Verify current Room state: skip notification if paused or already logged TAKEN today
        if (medId > 0L) {
            try {
                val dao = AppDatabase.getDatabase(context).triageDao()
                val currentMed = dao.getMedicineById(medId)
                if (currentMed != null) {
                    if (!currentMed.isActive) {
                        return Result.success()
                    }
                    medName = currentMed.name
                    dosage = currentMed.dosage
                    instruction = currentMed.instruction
                    timeOfDay = currentMed.timeOfDay
                }
                if (!isSnooze) {
                    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val todayLog = dao.getAdherenceLogForMedicineAndDate(medId, todayDate)
                    if (todayLog?.status == "TAKEN") {
                        return Result.success()
                    }
                }
            } catch (_: Exception) {
                // Proceed using inputData if database query fails transiently
            }
        }

        DevLogger.i("WorkManager", "Executing reminder doWork for med #$medId ($medName), isSnooze=$isSnooze")
        showReminderNotificationInternal(
            context = context,
            medId = medId,
            medName = medName,
            dosage = dosage,
            instruction = instruction,
            timeOfDay = timeOfDay,
            isSnooze = isSnooze
        )
        return Result.success()
    }
}

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
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import java.util.concurrent.TimeUnit

object WaterReminderNotificationHelper {
    const val CHANNEL_ID = "water_reminder_channel_v2"
    const val NOTIFICATION_ID = 2001
    private const val PREF_WATER = "water_reminder_prefs"
    private const val KEY_INTERVAL_HOURS = "interval_hours"
    private const val KEY_ENABLED = "water_reminder_enabled"
    private const val WATER_WORK_NAME = "water_hydration_periodic_work"
    private const val WATER_ALARM_REQUEST_CODE = 8201

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Water & Hydration Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Outside-the-app WhatsApp-style hydration alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(soundUri, attrs)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun getSavedIntervalHours(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_WATER, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_INTERVAL_HOURS, 2).coerceIn(1, 8)
    }

    fun isWaterReminderEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_WATER, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, true)
    }

    fun scheduleWaterReminder(context: Context, intervalHours: Int) {
        val safeHours = intervalHours.coerceIn(1, 8)
        context.getSharedPreferences(PREF_WATER, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_INTERVAL_HOURS, safeHours)
            .putBoolean(KEY_ENABLED, true)
            .apply()

        val intervalMs = safeHours * 60L * 60L * 1000L
        val triggerAtMillis = System.currentTimeMillis() + intervalMs

        scheduleWaterAlarmAt(context, triggerAtMillis, safeHours)

        val workRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            safeHours.toLong(),
            TimeUnit.HOURS,
            15,
            TimeUnit.MINUTES
        )
            .setInitialDelay(safeHours.toLong(), TimeUnit.HOURS)
            .addTag("water_hydration_worker")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WATER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        OneSignalNotificationService.schedulePushAsync(
            context = context,
            title = "💧 Time to Drink Water! (પાણી પીવાનો સમય)",
            message = "Hydration reminder every ${safeHours}h — drink a 250ml glass of water now.",
            scheduledAtMillis = triggerAtMillis,
            extraData = mapOf(
                "reminder_type" to "water",
                "interval_hours" to safeHours.toString(),
                "screen" to "WATER_REMINDER"
            )
        )
        DevLogger.i("WaterReminder", "Scheduled outside hydration alarm + OneSignal ISO push every ${safeHours}h")
    }

    fun scheduleWaterAlarmAt(context: Context, triggerAtMillis: Long, intervalHours: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val alarmIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_WATER_ALARM
                putExtra(ReminderAlarmReceiver.EXTRA_WATER_INTERVAL_HOURS, intervalHours)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                WATER_ALARM_REQUEST_CODE,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (e: SecurityException) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val alarmIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                    action = ReminderAlarmReceiver.ACTION_WATER_ALARM
                    putExtra(ReminderAlarmReceiver.EXTRA_WATER_INTERVAL_HOURS, intervalHours)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    WATER_ALARM_REQUEST_CODE,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    fun showWaterReminderNotification(
        context: Context,
        title: String = "💧 Time to Drink Water! (પાણી પીવાનો સમય થયો છે)",
        message: String = "Staying hydrated boosts energy, aids kidney health, and prevents tension headaches. Drink a 250ml glass now!"
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

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_SCREEN, "WATER_REMINDER")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val logWaterIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_LOG_WATER_250ML
        }
        val logWaterPending = PendingIntent.getBroadcast(
            context,
            2002,
            logWaterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_health)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(
                android.R.drawable.ic_input_add,
                "Drink +250 ml",
                logWaterPending
            )

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIFICATION_ID, builder.build())
        } catch (_: Exception) {}
    }
}

class WaterReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        WaterReminderNotificationHelper.showWaterReminderNotification(context)
        return Result.success()
    }
}

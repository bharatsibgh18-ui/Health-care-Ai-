package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
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
        const val WORK_NAME_PREFIX = "med_reminder_work_"

        fun scheduleDailyMedicineReminder(
            context: Context,
            medId: Long,
            medName: String,
            dosage: String,
            instruction: String
        ) {
            val data = workDataOf(
                KEY_MED_ID to medId,
                KEY_MED_NAME to medName,
                KEY_MED_DOSAGE to dosage,
                KEY_MED_INSTRUCTION to instruction
            )

            // WorkManager periodic request runs every 24 hours
            val reminderRequest = PeriodicWorkRequestBuilder<MedicineReminderWorker>(
                24, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // Flex interval
            )
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "$WORK_NAME_PREFIX$medId",
                ExistingPeriodicWorkPolicy.UPDATE,
                reminderRequest
            )
        }

        fun cancelMedicineReminder(context: Context, medId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("$WORK_NAME_PREFIX$medId")
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Medicine Reminders (દવા રિમાઇન્ડર)"
                val descriptionText = "Notifications for taking prescribed medications on time"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
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
            instruction: String
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
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                medId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_health)
                .setContentTitle("💊 દવા લેવાનો સમય: $medName")
                .setContentText("માત્રા: $dosage ($instruction) • સમયસર દવા લો!")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("💊 દવા લેવાનો સમય થયો છે: $medName\nમાત્રા: $dosage\nસૂચના: $instruction\nનિયમિત દવા લેવાથી સ્વાસ્થ્ય જળવાય છે.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            try {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.notify((3000 + medId).toInt(), notification)
            } catch (_: Exception) {}
        }
    }

    override suspend fun doWork(): Result {
        val medId = inputData.getLong(KEY_MED_ID, 0L)
        val medName = inputData.getString(KEY_MED_NAME) ?: "Prescribed Medicine"
        val dosage = inputData.getString(KEY_MED_DOSAGE) ?: "1 Dose"
        val instruction = inputData.getString(KEY_MED_INSTRUCTION) ?: "As prescribed"

        showNotification(medId, medName, dosage, instruction)
        return Result.success()
    }

    private fun showNotification(medId: Long, medName: String, dosage: String, instruction: String) {
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
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            medId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_health)
            .setContentTitle("💊 Time for Medication: $medName")
            .setContentText("Dosage: $dosage ($instruction) • Take on schedule!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("💊 Time to take: $medName\nDosage: $dosage\nInstruction: $instruction\nStaying consistent with medications prevents clinical complications.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify((3000 + medId).toInt(), notification)
        } catch (_: Exception) {}
    }
}

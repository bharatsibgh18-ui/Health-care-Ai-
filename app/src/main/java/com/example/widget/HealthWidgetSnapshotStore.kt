package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HealthWidgetSnapshot(
    val healthScore: Int = 78,
    val scoreTierLabel: String = "Strong",
    val stepsToday: Int = 0,
    val stepTarget: Int = 8000,
    val waterMlToday: Int = 0,
    val waterTargetMl: Int = 2500,
    val nextMedicineLabel: String = "No dose due",
    val streakDays: Int = 0,
    val updatedAtLabel: String = ""
)

/**
 * Lightweight SharedPreferences cache bridging live ViewModel health streams
 * to the Android Home Screen Daily Health Summary Widget with zero battery drain.
 */
object HealthWidgetSnapshotStore {

    private const val PREFS_NAME = "ultra_health_widget_snapshot"
    private const val KEY_SCORE = "health_score"
    private const val KEY_TIER = "score_tier"
    private const val KEY_STEPS = "steps_today"
    private const val KEY_STEP_TARGET = "step_target"
    private const val KEY_WATER = "water_today"
    private const val KEY_WATER_TARGET = "water_target"
    private const val KEY_NEXT_MED = "next_med"
    private const val KEY_STREAK = "streak_days"
    private const val KEY_UPDATED = "updated_at"

    fun saveSnapshot(context: Context, snapshot: HealthWidgetSnapshot) {
        try {
            val timeLabel = snapshot.updatedAtLabel.ifBlank {
                SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            }
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SCORE, snapshot.healthScore.coerceIn(0, 100))
                .putString(KEY_TIER, snapshot.scoreTierLabel)
                .putInt(KEY_STEPS, snapshot.stepsToday.coerceAtLeast(0))
                .putInt(KEY_STEP_TARGET, snapshot.stepTarget.coerceAtLeast(1000))
                .putInt(KEY_WATER, snapshot.waterMlToday.coerceAtLeast(0))
                .putInt(KEY_WATER_TARGET, snapshot.waterTargetMl.coerceAtLeast(500))
                .putString(KEY_NEXT_MED, snapshot.nextMedicineLabel)
                .putInt(KEY_STREAK, snapshot.streakDays.coerceAtLeast(0))
                .putString(KEY_UPDATED, timeLabel)
                .apply()
        } catch (_: Throwable) {
        }
    }

    fun loadSnapshot(context: Context): HealthWidgetSnapshot {
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            HealthWidgetSnapshot(
                healthScore = prefs.getInt(KEY_SCORE, 78),
                scoreTierLabel = prefs.getString(KEY_TIER, "Strong") ?: "Strong",
                stepsToday = prefs.getInt(KEY_STEPS, 0),
                stepTarget = prefs.getInt(KEY_STEP_TARGET, 8000),
                waterMlToday = prefs.getInt(KEY_WATER, 0),
                waterTargetMl = prefs.getInt(KEY_WATER_TARGET, 2500),
                nextMedicineLabel = prefs.getString(KEY_NEXT_MED, "No dose due") ?: "No dose due",
                streakDays = prefs.getInt(KEY_STREAK, 0),
                updatedAtLabel = prefs.getString(KEY_UPDATED, "") ?: ""
            )
        } catch (_: Throwable) {
            HealthWidgetSnapshot()
        }
    }

    fun updateAndRefreshWidgets(context: Context, snapshot: HealthWidgetSnapshot) {
        saveSnapshot(context, snapshot)
        try {
            val appCtx = context.applicationContext
            val manager = AppWidgetManager.getInstance(appCtx) ?: return
            val component = ComponentName(appCtx, HealthSummaryWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids != null && ids.isNotEmpty()) {
                val intent = Intent(appCtx, HealthSummaryWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                appCtx.sendBroadcast(intent)
            }
        } catch (_: Throwable) {
            // Safe no-op in headless/unit test environments without launcher widget host
        }
    }
}

package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

/**
 * Native Android Home Screen Daily Health Summary Widget.
 * Displays live composite Health Score, Steps, Water intake, Next Medicine / Streak,
 * plus 1-tap deep-link buttons for Emergency SOS and AI Chat Triage.
 */
class HealthSummaryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val SCREEN_ULTRA_HUB = "ULTRA_HEALTH_HUB"
        const val SCREEN_EMERGENCY = "EMERGENCY_GUIDE"
        const val SCREEN_TRIAGE_CHAT = "TRIAGE_CHAT"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val snapshot = HealthWidgetSnapshotStore.loadSnapshot(context)
            val views = buildRemoteViews(context, snapshot)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun buildRemoteViews(context: Context, snapshot: HealthWidgetSnapshot): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_health_summary)

            views.setTextViewText(R.id.widget_score_value, "${snapshot.healthScore}")
            views.setTextViewText(
                R.id.widget_score_tier,
                "${snapshot.scoreTierLabel} • ${snapshot.streakDays}d Streak"
            )
            views.setTextViewText(
                R.id.widget_steps_text,
                "${snapshot.stepsToday} / ${snapshot.stepTarget}"
            )
            views.setTextViewText(
                R.id.widget_water_text,
                "${snapshot.waterMlToday} / ${snapshot.waterTargetMl} ml"
            )
            views.setTextViewText(
                R.id.widget_med_text,
                snapshot.nextMedicineLabel.ifBlank { "All doses clear" }
            )

            views.setOnClickPendingIntent(
                R.id.widget_root_container,
                createDeepLinkPendingIntent(context, SCREEN_ULTRA_HUB, 4101)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_sos,
                createDeepLinkPendingIntent(context, SCREEN_EMERGENCY, 4102)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_ai_chat,
                createDeepLinkPendingIntent(context, SCREEN_TRIAGE_CHAT, 4103)
            )

            return views
        }

        private fun createDeepLinkPendingIntent(
            context: Context,
            targetScreen: String,
            requestCode: Int
        ): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_SCREEN, targetScreen)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

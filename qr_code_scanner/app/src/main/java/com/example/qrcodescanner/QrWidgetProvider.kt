package com.example.qrcodescanner

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.qrcodescanner.data.AppLanguage
import com.example.qrcodescanner.data.L10n

class QrWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.cyber_widget)

        val prefs = context.getSharedPreferences("secure_vault_prefs", Context.MODE_PRIVATE)
        val langStr = prefs.getString("app_language", "AR") ?: "AR"
        val currentLanguage = when (langStr) {
            "FR" -> AppLanguage.FR
            "EN" -> AppLanguage.EN
            else -> AppLanguage.AR
        }

        views.setTextViewText(R.id.tv_widget_scan, L10n.get("tab_scan", currentLanguage))
        views.setTextViewText(R.id.tv_widget_generate, L10n.get("tab_generate", currentLanguage))
        views.setTextViewText(R.id.tv_widget_vault, L10n.get("tab_vault", currentLanguage))

        // RemoteViews on Android versions < 12 do NOT support setColorFilter.
        // It will cause an ActionException crash and the widget will say "Problem loading widget".

        // Set up intents for buttons to launch MainActivity with selected tab
        views.setOnClickPendingIntent(R.id.btn_widget_scan, getPendingSelfIntent(context, 0))
        views.setOnClickPendingIntent(R.id.btn_widget_generate, getPendingSelfIntent(context, 2))
        views.setOnClickPendingIntent(R.id.btn_widget_vault, getPendingSelfIntent(context, 3))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPendingSelfIntent(context: Context, selectedTab: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("selected_tab", selectedTab)
        }
        // Make request code unique per button so intents don't overwrite each other
        return PendingIntent.getActivity(
            context,
            selectedTab,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

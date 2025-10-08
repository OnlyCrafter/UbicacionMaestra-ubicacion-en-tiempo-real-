package com.esime.ubicacionmaestra.Firstapp.ui.utilities.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.esime.ubicacionmaestra.Firstapp.ui.panic.panicBttonActivity
import com.esime.ubicacionmaestra.R

class PanicButtonWidget : AppWidgetProvider() {
    companion object {
        private const val TAG = "panicButtonWidget"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            for (appWidgetId in appWidgetIds) {
                val intent = Intent(context, panicBttonActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0, // Consider using a unique request code
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT // Consider FLAG_CANCEL_CURRENT
                )

                val views = RemoteViews(context.packageName, R.layout.panic_button_widget).apply {
                    setOnClickPendingIntent(R.id.widget_button, pendingIntent)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            Log.e("PanicButtonWidget", "Error updating widget", e)
        }
    }


}


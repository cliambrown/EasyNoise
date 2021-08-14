package com.cliambrown.easynoise

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.cliambrown.easynoise.helpers.*
import android.app.PendingIntent

/**
 * Implementation of App Widget functionality.
 */
class EasyNoiseWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.i("info", "onUpdate")
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        when (action) {
            WIDGET_PLAY -> play(context)
            WIDGET_PAUSE -> pause(context)
        }
        super.onReceive(context, intent)
    }

    fun play(context: Context?) {
        Log.i("info", "widget play")
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.setAction(PLAY)
        context?.sendBroadcast(intent)
    }

    fun pause(context: Context?) {
        Log.i("info", "widget pause")
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.setAction(PAUSE)
        context?.sendBroadcast(intent)
    }
}

@SuppressLint("RemoteViewLayout")
internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.easy_noise_widget)

    val playIntent = Intent(context, EasyNoiseWidget::class.java)
    playIntent.setAction(WIDGET_PLAY)
    val pendingPlayIntent = PendingIntent.getBroadcast(context, 0, playIntent, 0)
    views.setOnClickPendingIntent(R.id.playButton, pendingPlayIntent)

    val pauseIntent = Intent(context, EasyNoiseWidget:: class.java)
    pauseIntent.setAction(WIDGET_PAUSE)
    val pendingPauseIntent = PendingIntent.getBroadcast(context, 0, pauseIntent, 0)
    views.setOnClickPendingIntent(R.id.pauseButton, pendingPauseIntent)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
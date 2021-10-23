package com.cliambrown.easynoise

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.cliambrown.easynoise.helpers.*

class NotificationUtils(base: Context?) : ContextWrapper(base) {

    private var mManager: NotificationManager? = null

    companion object {
        var NOTIFICATION_ID = 64 // Random number
    }

    private fun getManager(): NotificationManager? {
        if (mManager == null) {
            mManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        }
        return mManager
    }

    fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                enableLights(false)
                enableVibration(false)
            }
            // Register the channel with the system
            getManager()?.createNotificationChannel(channel)
        }
    }

    fun createNotification(isPlaying: Boolean): Notification {

        createNotificationChannel()

        val volUpIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = VOLUME_UP
        }
        val pendingVolUpIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, volUpIntent, PendingIntent.FLAG_IMMUTABLE)

        val volDownIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = VOLUME_DOWN
        }
        val pendingVolDownIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, volDownIntent, PendingIntent.FLAG_IMMUTABLE)

        val dismissIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = DISMISS
        }
        val pendingDismissIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, dismissIntent, PendingIntent.FLAG_IMMUTABLE)

        val notificationLayout = RemoteViews(packageName, R.layout.notification_layout)

        if (isPlaying) {
            notificationLayout.setViewVisibility(R.id.playButton, View.GONE)
            val pauseIntent = Intent(this, NotificationReceiver::class.java).apply {
                action = PAUSE
            }
            val pendingPauseIntent: PendingIntent =
                PendingIntent.getBroadcast(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            notificationLayout.setOnClickPendingIntent(R.id.pauseButton, pendingPauseIntent)
        } else {
            notificationLayout.setViewVisibility(R.id.pauseButton, View.GONE)
            val playIntent = Intent(this, NotificationReceiver::class.java).apply {
                action = PLAY
            }
            val pendingPlayIntent: PendingIntent =
                PendingIntent.getBroadcast(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE)
            notificationLayout.setOnClickPendingIntent(R.id.playButton, pendingPlayIntent)
        }

        notificationLayout.setOnClickPendingIntent(R.id.upButton, pendingVolUpIntent)
        notificationLayout.setOnClickPendingIntent(R.id.downButton, pendingVolDownIntent)
        notificationLayout.setOnClickPendingIntent(R.id.dismissButton, pendingDismissIntent)

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingMainIntent: PendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        val smallIcon = if (isPlaying) R.drawable.notification_icon else R.drawable.paused_notification_icon
        if (!isPlaying) {
            notificationLayout.setTextViewCompoundDrawables(R.id.appName, R.drawable.paused_notification_icon, 0, 0, 0)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentText(null)
            .setContentIntent(pendingMainIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCustomContentView(notificationLayout)
            .build()

        return notification
    }
}
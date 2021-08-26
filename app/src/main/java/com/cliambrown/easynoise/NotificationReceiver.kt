package com.cliambrown.easynoise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.cliambrown.easynoise.helpers.*

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val actions = arrayOf(PLAY, PAUSE, TOGGLE_PLAY, DISMISS, VOLUME_UP, VOLUME_DOWN)
        if (!actions.contains(action)) {
            return
        }
        val newIntent = Intent(context, PlayerService::class.java)
        newIntent.setAction(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(newIntent)
        } else {
            context.startService(newIntent)
        }
    }
}
package com.cliambrown.easynoise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.cliambrown.easynoise.helpers.*

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent == null) {
            return
        }
        val action = intent.action
        if (action !== PLAY && action !== PAUSE && action !== DISMISS) {
            return
        }
        val intent = Intent(context, PlayerService::class.java)
        intent.setAction(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
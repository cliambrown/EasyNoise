package com.cliambrown.easynoise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.cliambrown.easynoise.helpers.*

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("clb-info", "NotificationReceiver onReceive")
        val action = intent.action
        val actions = arrayOf(PLAY, PAUSE, TOGGLE_PLAY, DISMISS, VOLUME_UP, VOLUME_DOWN)
        if (!actions.contains(action) || action == null) {
            return
        }
        Util.startPlayerService(context, action)
    }
}
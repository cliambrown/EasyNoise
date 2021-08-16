package com.cliambrown.easynoise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.cliambrown.easynoise.helpers.*

class HeadsetStateBroadcastReceiver : BroadcastReceiver() {

    val HEADPHONE_ACTIONS = arrayOf(
        Intent.ACTION_HEADSET_PLUG,
        "android.bluetooth.headset.action.STATE_CHANGED",
        "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED"
    )

    override fun onReceive(context: Context, intent: Intent) {

        val prefs = context.getSharedPreferences(context.packageName, 0)
        val wasPlaying = prefs.getBoolean("wasPlaying", false)
        if (!wasPlaying) return

        var doUpdate = false
        var setPlaying = false

        val action = intent.action
        when (action) {
            HEADPHONE_ACTIONS[0] -> {
                // Wired headset monitoring
                doUpdate = true
                val state = intent.getIntExtra("state", 0)
                setPlaying = (state > 0)
            }
            HEADPHONE_ACTIONS[1] -> {
                // Bluetooth monitoring
                doUpdate = true
                val state = intent.getIntExtra("android.bluetooth.headset.extra.STATE", 0)
                setPlaying = (state == 2)
            }
            HEADPHONE_ACTIONS[2] -> {
                // Bluetooth, works for Ice Cream Sandwich
                doUpdate = true
                val state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0)
                setPlaying = (state == 2)
            }
        }

        var update = "Headset state changed: doUpdate = "
        if (doUpdate) update += "false"
        else update += "true"
        update += "; setPlaying = "
        if (setPlaying) update += "true"
        else update += "false"
        Log.i("info", update)

        if (!doUpdate) return

        val newIntent = Intent(context, PlayerService::class.java)
        if (setPlaying) {
            newIntent.setAction(HEADPHONE_PLAY)
        } else {
            newIntent.setAction(HEADPHONE_PAUSE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(newIntent)
        } else {
            context.startService(newIntent)
        }

    }
}
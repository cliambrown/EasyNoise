package com.cliambrown.easynoise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.cliambrown.easynoise.helpers.*

class OutsidePauseReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val prefs = context.getSharedPreferences(context.packageName, 0)
        val wasPlaying = prefs.getBoolean("wasPlaying", false)
        if (!wasPlaying) return

        var doUpdate = false
        var setPlaying = false

        val action = intent.action
        when (action) {
            PHONE_STATE -> {
                // Phone call start/stop
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                Log.i("info", "OutsidePauseReceiver onReceive; state="+state.toString())
                val states = arrayOf("IDLE", "RINGING", "OFFHOOK")
                doUpdate = (states.contains(state))
                setPlaying = (state == "IDLE")
            }
            HEADSET_PLUG -> {
                // Wired headset monitoring
                doUpdate = true
                val state = intent.getIntExtra("state", 0)
                Log.i("info", "OutsidePauseReceiver onReceive; state="+state.toString())
                setPlaying = (state > 0)
            }
            HEADSET_STATE_CHANGED -> {
                // Bluetooth monitoring
                doUpdate = true
                val state = intent.getIntExtra("android.bluetooth.headset.extra.STATE", 0)
                setPlaying = (state == 2)
            }
            CONNECTION_STATE_CHANGED -> {
                // Bluetooth, works for Ice Cream Sandwich
                doUpdate = true
                val state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0)
                setPlaying = (state == 2)
            }
        }

        Log.i("info", "OutsidePauseReceiver onReceive; action="+action.toString()+"; doUpdate="+(if (doUpdate) "true" else "false")+"; setPlaying="+(if (setPlaying) "true" else "false"))

        if (!doUpdate) return

        val newIntent = Intent(context, PlayerService::class.java)
        if (setPlaying) {
            newIntent.setAction(OUTSIDE_PLAY)
        } else {
            newIntent.setAction(OUTSIDE_PAUSE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(newIntent)
        } else {
            context.startService(newIntent)
        }
    }
}
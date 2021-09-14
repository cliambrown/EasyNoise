package com.cliambrown.easynoise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.telephony.TelephonyManager
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
                val states = arrayOf("IDLE", "RINGING", "OFFHOOK")
                doUpdate = (states.contains(state))
                setPlaying = (state == "IDLE")
            }
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                setPlaying = false
                doUpdate = true
            }
            HEADSET_PLUG -> {
                // Wired headset monitoring
                val state = intent.getIntExtra("state", 0)
                setPlaying = (state > 0)
                doUpdate = setPlaying
            }
            HEADSET_STATE_CHANGED -> {
                // Bluetooth monitoring
                val state = intent.getIntExtra("android.bluetooth.headset.extra.STATE", 0)
                setPlaying = (state == 2)
                doUpdate = setPlaying
            }
            CONNECTION_STATE_CHANGED -> {
                // Bluetooth, works for Ice Cream Sandwich
                val state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0)
                setPlaying = (state == 2)
                doUpdate = setPlaying
            }
        }

        if (!doUpdate) {
            return
        }

        val playerAction = if (setPlaying) OUTSIDE_PLAY else OUTSIDE_PAUSE
        Util.startPlayerService(context, playerAction)
    }
}
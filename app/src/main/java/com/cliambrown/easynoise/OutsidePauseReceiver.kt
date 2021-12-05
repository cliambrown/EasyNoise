package com.cliambrown.easynoise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telephony.TelephonyManager
import android.util.Log
import com.cliambrown.easynoise.helpers.*

class OutsidePauseReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("clb-info", "OutsidePauseReceiver onReceive")

        val action = intent.action
        var playerAction: String? = null

        when (action) {
            PHONE_STATE -> {
                // Phone call start/stop
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val states = arrayOf("IDLE", "RINGING", "OFFHOOK")
                if (!states.contains(state)) return
                if (state == "IDLE") {
                    playerAction = CALL_ENDED
                } else {
                    playerAction = CALL_STARTED
                }
            }
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                // Headphones unplugged / disconnected
                playerAction = AUDIO_BECOMING_NOISY
            }
            HEADSET_PLUG -> {
                // Wired headset monitoring
                val state = intent.getIntExtra("state", 0)
                if (state > 0) playerAction = HEADPHONES_CONNECTED
            }
            HEADSET_STATE_CHANGED -> {
                // Bluetooth monitoring
                val state = intent.getIntExtra("android.bluetooth.headset.extra.STATE", 0)
                if (state == 2) playerAction = HEADPHONES_CONNECTED
            }
            CONNECTION_STATE_CHANGED -> {
                // Bluetooth, works for Ice Cream Sandwich
                val state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0)
                if (state == 2) playerAction = HEADPHONES_CONNECTED
            }
        }

        if (playerAction != null) {
            Util.startPlayerService(context, playerAction)
        }
    }
}
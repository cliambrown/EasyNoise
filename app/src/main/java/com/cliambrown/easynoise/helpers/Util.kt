package com.cliambrown.easynoise.helpers

import android.content.Context
import android.content.Intent
import android.os.Build
import com.cliambrown.easynoise.PlayerService

class Util {

    companion object {
        fun startPlayerService(context: Context, action: String) : Boolean {
            val intent = Intent(context, PlayerService::class.java)
            intent.setAction(action)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            return true
        }
    }
}
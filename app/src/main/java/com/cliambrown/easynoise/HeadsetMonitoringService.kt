package com.cliambrown.easynoise

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.content.IntentFilter
import android.util.Log


class HeadsetMonitoringService : Service() {

    var headsetStateReceiver: HeadsetStateBroadcastReceiver? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.i("info", "HeadsetMonitoringService onCreate")
        headsetStateReceiver = HeadsetStateBroadcastReceiver()
        val filter = IntentFilter()
        for (action in headsetStateReceiver!!.HEADPHONE_ACTIONS) {
            filter.addAction(action)
        }
        registerReceiver(headsetStateReceiver, filter)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("info", "HeadsetMonitoringService onStart")
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(headsetStateReceiver)
        super.onDestroy()
    }
}
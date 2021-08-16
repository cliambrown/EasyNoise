package com.cliambrown.easynoise

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.cliambrown.easynoise.helpers.*

class PlayerService : Service() {

    private val binder = LocalBinder()
    var mActivity: Callbacks? = null

    var prefs: SharedPreferences? = null
    var mediaPlayer: MediaPlayer? = null
    var notificationUtils: NotificationUtils? = null

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): PlayerService = this@PlayerService
    }

    fun registerClient(activity: Activity) {
        mActivity = activity as Callbacks
    }

    //callbacks interface for communication with service clients!
    interface Callbacks {
        fun updateClient(action: String)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startService(Intent(this, HeadsetMonitoringService::class.java))

        if (intent == null) {
            return START_NOT_STICKY
        }
        val action = intent.action
        when (action) {
            PLAY -> play()
            PAUSE -> pause()
            DISMISS -> dismiss()
            HEADPHONE_PLAY -> play(false)
            HEADPHONE_PAUSE -> pause(false)
        }
        if (action !== DISMISS) {
            if (notificationUtils === null) {
                notificationUtils = NotificationUtils(this)
            }
            notificationUtils?.createNotificationChannel()
            val notification = notificationUtils?.createNotification(action === PLAY)
            startForeground(NotificationUtils.NOTIFICATION_ID, notification)
        }
        val newIntent = Intent(this, EasyNoiseWidget::class.java)
        if (action === PLAY) {
            newIntent.setAction(SET_PLAYING)
        } else {
            newIntent.setAction(SET_PAUSED)
        }
        sendBroadcast(newIntent)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        getMPlayer()?.stop()
        getMPlayer()?.release()
        mediaPlayer = null
    }

    fun getMPlayer(): MediaPlayer {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(applicationContext, R.raw.pass)
            mediaPlayer?.setLooping(true)
        }
        return mediaPlayer!!
    }

    fun getIsPlaying(): Boolean {
        return !!getMPlayer().isPlaying()
    }

    fun play(doUpdatePref: Boolean = true) {
        updateVolume()
        getMPlayer().start()
        mActivity?.updateClient(PLAY)
        if (doUpdatePref) {
            getPrefs().edit().putBoolean("wasPlaying", true).apply()
        }
    }

    fun pause(doUpdatePref: Boolean = true) {
        if (getIsPlaying()) {
            getMPlayer().pause()
            getMPlayer().seekTo(0)
        }
        mActivity?.updateClient(PAUSE)
        if (doUpdatePref) {
            getPrefs().edit().putBoolean("wasPlaying", false).apply()
        }
    }

    fun dismiss() {
        pause()
        stopForeground(true)
        stopSelf()
    }

    @JvmName("getPrefs1")
    fun getPrefs(): SharedPreferences {
        if (prefs === null) {
            prefs = getSharedPreferences(applicationContext.packageName, 0)
        }
        return prefs!!
    }

    fun updateVolume() {
        val volume = getPrefs().getInt("volume", 50).toDouble()
        val maxVolume = 100.0
        val toVolume = volume.div(maxVolume).toFloat()
        getMPlayer().setVolume(toVolume, toVolume)
    }
}
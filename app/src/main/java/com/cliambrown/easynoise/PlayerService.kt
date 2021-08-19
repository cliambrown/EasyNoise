package com.cliambrown.easynoise

import android.Manifest
import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import com.cliambrown.easynoise.helpers.*
import androidx.core.content.ContextCompat

class PlayerService : Service() {

    private val binder = LocalBinder()
    var mActivity: Callbacks? = null

    var prefs: SharedPreferences? = null
    var mediaPlayer: MediaPlayer? = null
    var notificationUtils: NotificationUtils? = null

    var outsidePauseReceiver: OutsidePauseReceiver? = null

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

    override fun onCreate() {
        outsidePauseReceiver = OutsidePauseReceiver()
        val filter = IntentFilter()
        filter.addAction(PHONE_STATE)
        filter.addAction(HEADSET_STATE_CHANGED)
        filter.addAction(CONNECTION_STATE_CHANGED)
        filter.addAction(HEADSET_PLUG)
        registerReceiver(outsidePauseReceiver, filter)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val readPhoneState =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        if (readPhoneState != PackageManager.PERMISSION_GRANTED) {
            val mainIntent = Intent(this, MainActivity::class.java)
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            this.startActivity(mainIntent)
            return START_NOT_STICKY
        }

        if (intent == null) {
            return START_NOT_STICKY
        }
        val action = intent.action
        when (action) {
            PLAY -> play()
            PAUSE -> pause()
            DISMISS -> dismiss()
            OUTSIDE_PLAY -> play(false)
            OUTSIDE_PAUSE -> pause(false)
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
        if (action === PLAY || action === OUTSIDE_PLAY) {
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
        unregisterReceiver(outsidePauseReceiver)
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
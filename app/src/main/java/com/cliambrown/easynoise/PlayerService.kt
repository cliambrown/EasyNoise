package com.cliambrown.easynoise

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import com.cliambrown.easynoise.helpers.*

class PlayerService : Service() {

    private val binder = LocalBinder()
    var mActivity: Callbacks? = null

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
        if (intent == null) {
            return START_NOT_STICKY
        }
        val action = intent.action
        when (action) {
            PLAY -> play()
            PAUSE -> pause()
            DISMISS -> dismiss()
        }
        if (action !== DISMISS) {
            if (notificationUtils === null) {
                notificationUtils = NotificationUtils(this)
            }
            notificationUtils?.createNotificationChannel()
            val notification = notificationUtils?.createNotification(action === PLAY)
            startForeground(NotificationUtils.NOTIFICATION_ID, notification)
            val intent = Intent(this, EasyNoiseWidget::class.java)
            if (action === PLAY) {
                intent.setAction(SET_PLAYING)
            } else {
                intent.setAction(SET_PAUSED)
            }
            sendBroadcast(intent)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        getMPlayer()?.stop()
        getMPlayer()?.release()
        mediaPlayer = null
    }

    fun getMPlayer(): MediaPlayer? {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(applicationContext, R.raw.pass)
            mediaPlayer?.setLooping(true)
        }
        return mediaPlayer
    }

    fun getIsPlaying(): Boolean {
        val r: Boolean? = getMPlayer()?.isPlaying()
        if (r is Boolean) return r
        return false
    }

    fun play() {
        getMPlayer()?.start()
        mActivity?.updateClient(PLAY)
    }

    fun pause() {
        if (getIsPlaying()) {
            getMPlayer()?.pause()
            getMPlayer()?.seekTo(0)
        }
        mActivity?.updateClient(PAUSE)
    }

    fun dismiss() {
        pause()
        stopForeground(true)
        stopSelf()
    }
}
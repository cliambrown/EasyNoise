package com.cliambrown.easynoise

import android.Manifest
import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.cliambrown.easynoise.helpers.*
import androidx.core.content.ContextCompat

class PlayerService : Service(), SoundPool.OnLoadCompleteListener {

    private val binder = LocalBinder()
    var mActivity: Callbacks? = null

    var prefs: SharedPreferences? = null
    var soundPool: SoundPool? = null
    var streamID: Int = 0
    var isLoading: Boolean = false
    var streamLoaded: Boolean = false
    var isPlaying: Boolean = false
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
        Log.i("info", "PlayerService onStartCommand; streamLoaded="+streamLoaded+"; isPlaying="+isPlaying+"; action="+action)
        when (action) {
            PLAY -> play()
            PAUSE -> pause()
            DISMISS -> dismiss()
            OUTSIDE_PLAY -> play(false)
            OUTSIDE_PAUSE -> pause(false)
        }
        if (action !== DISMISS) {
            if (notificationUtils == null) {
                notificationUtils = NotificationUtils(this)
            }
            notificationUtils?.createNotificationChannel()
            val notification = notificationUtils?.createNotification(action == PLAY || action == OUTSIDE_PLAY)
            startForeground(NotificationUtils.NOTIFICATION_ID, notification)
        }
        val newIntent = Intent(this, EasyNoiseWidget::class.java)
        if (action == PLAY || action == OUTSIDE_PLAY) {
            newIntent.setAction(SET_PLAYING)
        } else {
            newIntent.setAction(SET_PAUSED)
        }
        sendBroadcast(newIntent)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i("info", "PlayerService onDestroy; streamLoaded="+streamLoaded+"; isPlaying="+isPlaying+"; streamID="+streamID)
        unregisterReceiver(outsidePauseReceiver)
        soundPool?.stop(streamID)
        soundPool?.release()
        soundPool = null
        isPlaying = false
        streamLoaded = false
        super.onDestroy()
    }

    fun initSoundPool() {
        Log.i("info", "PlayerService initSoundPool; streamLoaded="+streamLoaded+"; isPlaying="+isPlaying+"; streamID="+streamID)
        if (isLoading) return
        isLoading = true
        if (soundPool == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            soundPool = SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build() as SoundPool
            soundPool!!.setOnLoadCompleteListener(this)
        }
        if (!streamLoaded) {
            streamID = soundPool!!.load(this, R.raw.grey_noise, 1)
        }
    }

    override fun onLoadComplete(pSoundPool: SoundPool, pSampleID: Int, status: Int) {
        Log.i("info", "PlayerService onLoadComplete; streamLoaded="+streamLoaded+"; isPlaying="+isPlaying+"; streamID="+streamID)
        streamLoaded = true
        isLoading = false
        playLoaded()
    }

    fun playLoaded() {
        Log.i("info", "PlayerService playLoaded; streamLoaded="+streamLoaded+"; isPlaying="+isPlaying+"; streamID="+streamID)
        val floatVol = updateVolume()
        soundPool?.play(streamID, floatVol, floatVol, 1, -1, 1.0F)
        isPlaying = true
    }

    fun getIsPlaying(): Boolean {
        return isPlaying
    }

    fun play(doUpdatePref: Boolean = true) {
        Log.i("info", "PlayerService play; streamLoaded="+streamLoaded+"; isPlaying="+isPlaying+"; streamID="+streamID)
        if (streamLoaded) {
            playLoaded()
        } else {
            initSoundPool()
        }
        mActivity?.updateClient(PLAY)
        if (doUpdatePref) {
            getPrefs().edit().putBoolean("wasPlaying", true).apply()
        }
    }

    fun pause(doUpdatePref: Boolean = true) {
        Log.i("info", "PlayerService pause; streamLoaded="+streamLoaded+"; isPlaying="+isPlaying+"; streamID="+streamID)
        soundPool?.pause(streamID)
        soundPool?.autoPause()
        isPlaying = false
        mActivity?.updateClient(PAUSE)
        if (doUpdatePref) {
            getPrefs().edit().putBoolean("wasPlaying", false).apply()
        }
    }

    fun dismiss() {
        Log.i("info", "PlayerService dismiss; streamLoaded="+streamLoaded+"; isPlaying="+isPlaying+"; streamID="+streamID)
        pause()
        stopForeground(true)
        stopSelf()
    }

    @JvmName("getPrefs1")
    fun getPrefs(): SharedPreferences {
        if (prefs == null) {
            prefs = getSharedPreferences(applicationContext.packageName, 0)
        }
        return prefs!!
    }

    fun updateVolume(): Float {
        val volume = getPrefs().getInt("volume", 50).toDouble()
        val maxVolume = 100.0
        val toVolume = volume.div(maxVolume).toFloat()
        if (streamLoaded) {
            soundPool!!.setVolume(streamID, toVolume, toVolume)
        }
        return toVolume
    }
}
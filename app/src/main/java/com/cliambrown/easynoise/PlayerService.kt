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
import android.widget.Toast
import kotlin.math.roundToInt

class PlayerService : Service(), SoundPool.OnLoadCompleteListener {

    private val binder = LocalBinder()
    var mActivity: Callbacks? = null

    var prefs: SharedPreferences? = null
    var soundPool: SoundPool? = null
    var soundID: Int = -1
    var streamID: Int? = -1
    var isLoading: Boolean = false
    var streamLoaded: Boolean = false
    var isPlaying: Boolean = false
    var lastAction: String? = null
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
            TOGGLE_PLAY -> togglePlay()
            DISMISS -> dismiss()
            OUTSIDE_PLAY -> play(false)
            OUTSIDE_PAUSE -> pause(false)
            VOLUME_UP -> updateVolume(+5)
            VOLUME_DOWN -> updateVolume(-5)
        }

        return START_NOT_STICKY
    }

    fun createNotification(isPlaying: Boolean) {
        if (notificationUtils == null) {
            notificationUtils = NotificationUtils(this)
        }
        val notification = notificationUtils?.createNotification(isPlaying)
        startForeground(NotificationUtils.NOTIFICATION_ID, notification)
    }

    fun updateWidget(toIsPlaying: Boolean) {
        val newIntent = Intent(this, EasyNoiseWidget::class.java)
        if (toIsPlaying) {
            newIntent.setAction(SET_PLAYING)
        } else {
            newIntent.setAction(SET_PAUSED)
        }
        sendBroadcast(newIntent)
    }

    override fun onDestroy() {
        unregisterReceiver(outsidePauseReceiver)
        if (streamID != null && streamID!! > 0) {
            soundPool?.stop(streamID!!)
        }
        soundPool?.release()
        soundPool = null
        isPlaying = false
        streamLoaded = false
        super.onDestroy()
    }

    fun initSoundPool() {
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
            soundID = soundPool!!.load(this, R.raw.grey_noise, 1)
        }
    }

    override fun onLoadComplete(pSoundPool: SoundPool, pSampleID: Int, status: Int) {
        streamLoaded = (soundID > 0)
        isLoading = false
        if (streamLoaded && lastAction.equals("play")) {
            playLoaded()
        } else {
            val toast = Toast.makeText(applicationContext,
                "Error loading sound",
                Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    fun playLoaded() {
        val floatVol = updateVolume()
        streamID = soundPool?.play(soundID, floatVol, floatVol, 1, -1, 1.0F)
        isPlaying = true
    }

    fun getIsPlaying(): Boolean {
        return isPlaying
    }

    fun togglePlay() {
        if (!isPlaying && !isLoading) {
            play()
        } else {
            pause()
        }
    }

    fun play(doUpdatePref: Boolean = true) {
        lastAction = "play"
        if (streamLoaded) {
            playLoaded()
        } else {
            initSoundPool()
        }
        onPlayChanged(true, doUpdatePref)
    }

    fun pause(doUpdatePref: Boolean = true) {
        lastAction = "pause"
        soundPool?.autoPause()
        isPlaying = false
        onPlayChanged(false, doUpdatePref)
    }

    fun onPlayChanged(toPlaying: Boolean, doUpdatePref: Boolean) {
        mActivity?.updateClient(if (toPlaying) PLAY else PAUSE)
        updateWidget(toPlaying)
        createNotification(toPlaying)
        if (doUpdatePref) {
            getPrefs().edit().putBoolean("wasPlaying", toPlaying).apply()
        }
    }

    fun dismiss() {
        mActivity?.updateClient(DISMISS)
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

    fun updateVolume(adjustBy: Int? = null): Float {
        var volume = getPrefs().getInt("volume", 50).toDouble()
        val maxVolume = 100.0
        if (adjustBy != null) {
            volume += adjustBy
            if (volume > maxVolume) volume = maxVolume
            else if (volume < 0.0) volume = 0.0
            getPrefs().edit().putInt("volume", volume.roundToInt()).apply()
            mActivity?.updateClient(VOLUME_CHANGED)
        }
        val toVolume = volume.div(maxVolume).toFloat()
        if (streamLoaded) {
            soundPool!!.setVolume(streamID!!, toVolume, toVolume)
        }
        return toVolume
    }
}
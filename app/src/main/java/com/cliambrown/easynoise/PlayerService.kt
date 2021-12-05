package com.cliambrown.easynoise

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.cliambrown.easynoise.helpers.*
import android.widget.Toast
import kotlin.math.roundToInt

class PlayerService : Service(), SoundPool.OnLoadCompleteListener {

    private val binder = LocalBinder()
    var mActivity: Callbacks? = null

    var prefs: SharedPreferences? = null
    var currentNoise: String? = null
    var soundPool: SoundPool? = null
    var soundID: Int = -1
    var streamID: Int? = -1
    var isLoading: Boolean = false
    var streamLoaded: Boolean = false
    private var isPlaying: Boolean = false
    var wasPlaying: Boolean = false
    var lastAction: String? = null
    var notificationUtils: NotificationUtils? = null

    var onPhoneCall = false
    var audioIsNoisy = false

    var outsidePauseReceiver: OutsidePauseReceiver? = null

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): PlayerService = this@PlayerService
    }

    fun registerClient(activity: Activity) {
        Log.i("clb-info", "PlayerService registerClient")
        mActivity = activity as Callbacks
    }

    // callbacks interface for communication with service clients!
    interface Callbacks {
        fun updateClient(action: String)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        Log.i("clb-info", "PlayerService onCreate")
        wasPlaying = getPrefs().getBoolean("wasPlaying", false)
        outsidePauseReceiver = OutsidePauseReceiver()
        val filter = IntentFilter()
        filter.addAction(PHONE_STATE)
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        filter.addAction(HEADSET_STATE_CHANGED)
        filter.addAction(CONNECTION_STATE_CHANGED)
        filter.addAction(HEADSET_PLUG)
        registerReceiver(outsidePauseReceiver, filter)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("clb-info", "PlayerService onStartCommand")

        if (intent == null) {
            return START_NOT_STICKY
        }
        Log.i("clb-info", "PlayerService test1")

        val action = intent.action
        Log.i("clb-info", "action = " + action)
        when (action) {
            PLAY -> play()
            PAUSE -> pause()
            TOGGLE_PLAY -> togglePlay()
            DISMISS -> dismiss()
            VOLUME_UP -> updateVolume(+5)
            VOLUME_DOWN -> updateVolume(-5)
            CALL_STARTED -> {
                onPhoneCall = true
                if (isPlaying) pause(false)
            }
            CALL_ENDED -> {
                onPhoneCall = false
                if (wasPlaying && !audioIsNoisy) play(false)
            }
            AUDIO_BECOMING_NOISY -> {
                audioIsNoisy = true
                if (isPlaying) pause(false)
            }
            HEADPHONES_CONNECTED -> {
                Log.i("clb-info", "PlayerService test2")
                audioIsNoisy = false
                Log.i("clb-info", "wasPlaying = " + wasPlaying.toString())
                Log.i("clb-info", "onPhoneCall = " + onPhoneCall.toString())
                if (wasPlaying && !onPhoneCall) play(false)
            }
        }
        Log.i("clb-info", "PlayerService test3")

        return START_NOT_STICKY
    }

    fun createNotification(isPlaying: Boolean) {
        Log.i("clb-info", "PlayerService createNotification")
        if (notificationUtils == null) {
            notificationUtils = NotificationUtils(this)
        }
        val notification = notificationUtils?.createNotification(isPlaying)
        startForeground(NotificationUtils.NOTIFICATION_ID, notification)
    }

    fun updateWidget(toIsPlaying: Boolean) {
        Log.i("clb-info", "PlayerService updateWidget")
        val newIntent = Intent(this, EasyNoiseWidget::class.java)
        if (toIsPlaying) {
            newIntent.setAction(SET_PLAYING)
        } else {
            newIntent.setAction(SET_PAUSED)
        }
        sendBroadcast(newIntent)
    }

    override fun onDestroy() {
        Log.i("clb-info", "PlayerService onDestroy")
        pause()
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
        Log.i("clb-info", "PlayerService initSoundPool")
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
            loadNoise()
        }
    }

    fun loadNoise() {
        Log.i("clb-info", "PlayerService loadNoise")
        val noise = getPrefs().getString("noise", "fuzz")
        currentNoise = noise
        var resource: Int = when (noise) {
            resources.getString(R.string.fuzz) -> R.raw.fuzz
            resources.getString(R.string.gray) -> R.raw.grey_noise
            resources.getString(R.string.gray_2) -> R.raw.grey_noise_2
            resources.getString(R.string.white) -> R.raw.white_noise
            resources.getString(R.string.pink) -> R.raw.pink_noise
            resources.getString(R.string.brown) -> R.raw.brown_noise
            resources.getString(R.string.blue) -> R.raw.blue_noise
            else -> -1
        }
        if (resource > 0) {
            soundID = soundPool!!.load(this, resource, 1)
        }
    }

    override fun onLoadComplete(pSoundPool: SoundPool, pSampleID: Int, status: Int) {
        Log.i("clb-info", "PlayerService onLoadComplete")
        streamLoaded = (soundID > 0)
        isLoading = false
        if (streamLoaded) {
            if (lastAction.equals("play")) playLoaded()
        } else {
            val toast = Toast.makeText(applicationContext,
                resources.getString(R.string.load_error),
                Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    fun playLoaded() {
        Log.i("clb-info", "PlayerService playLoaded")
        val floatVol = updateVolume()
        streamID = soundPool?.play(soundID, floatVol, floatVol, 1, -1, 1.0F)
        isPlaying = true
    }

    fun getIsPlaying(): Boolean {
        Log.i("clb-info", "PlayerService getIsPlaying")
        return isPlaying
    }

    fun togglePlay() {
        Log.i("clb-info", "PlayerService togglePlay")
        if (!isPlaying && !isLoading) {
            play()
        } else {
            pause()
        }
    }

    fun play(doUpdatePref: Boolean = true) {
        Log.i("clb-info", "PlayerService play")
        lastAction = "play"
        if (streamLoaded) {
            playLoaded()
        } else {
            initSoundPool()
        }
        onPlayChanged(true, doUpdatePref)
    }

    fun pause(doUpdatePref: Boolean = true) {
        Log.i("clb-info", "PlayerService pause")
        lastAction = "pause"
        soundPool?.autoPause()
        isPlaying = false
        onPlayChanged(false, doUpdatePref)
    }

    fun onPlayChanged(toPlaying: Boolean, doUpdatePref: Boolean) {
        Log.i("clb-info", "PlayerService onPlayChanged")
        mActivity?.updateClient(if (toPlaying) PLAY else PAUSE)
        updateWidget(toPlaying)
        createNotification(toPlaying)
        getPrefs().edit().putBoolean("isPlaying", toPlaying).apply()
        if (doUpdatePref) {
            wasPlaying = toPlaying
            getPrefs().edit().putBoolean("wasPlaying", toPlaying).apply()
        }
    }

    fun dismiss() {
        Log.i("clb-info", "PlayerService dismiss")
        mActivity?.updateClient(DISMISS)
        pause()
        stopForeground(true)
        stopSelf()
    }

    @JvmName("getPrefs1")
    fun getPrefs(): SharedPreferences {
        Log.i("clb-info", "PlayerService getPrefs")
        if (prefs == null) {
            prefs = getSharedPreferences(applicationContext.packageName, 0)
        }
        return prefs!!
    }

    fun updateVolume(adjustBy: Int? = null): Float {
        Log.i("clb-info", "PlayerService updateVolume")
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

    fun noiseChanged() {
        Log.i("clb-info", "PlayerService noiseChanged")
        val newNoise = getPrefs().getString("noise", "fuzz")
        if (newNoise.equals(currentNoise)) return
        val tempIsPlaying = isPlaying
        if (tempIsPlaying) pause(false)
        if (streamLoaded) {
            soundPool?.stop(streamID!!)
            soundPool?.unload(soundID)
            streamLoaded = false
        }
        if (tempIsPlaying) play(false)
    }
}
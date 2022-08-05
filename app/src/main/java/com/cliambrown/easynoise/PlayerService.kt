package com.cliambrown.easynoise

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.TileService
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

    companion object {
        fun start(context: Context, action: String): Boolean {
            Intent(context, PlayerService::class.java).setAction(action).run {
                if (Build.VERSION.SDK_INT < 26) context.startService(this)
                else context.startForegroundService(this)
            }
            return true
        }
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): PlayerService = this@PlayerService
    }

    fun registerClient(activity: Activity) {
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
        wasPlaying = getPrefs().getBoolean("wasPlaying", false)
        outsidePauseReceiver = OutsidePauseReceiver()
        val filter = IntentFilter()
        filter.addAction(PHONE_STATE)
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        filter.addAction(HEADSET_STATE_CHANGED)
        filter.addAction(CONNECTION_STATE_CHANGED)
        filter.addAction(HEADSET_PLUG)
        registerReceiver(outsidePauseReceiver, filter)
        createNotification(false)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent == null) {
            return START_NOT_STICKY
        }

        val action = intent.action
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
                audioIsNoisy = false
                if (wasPlaying && !onPhoneCall) play(false)
            }
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
        if (isLoading) return
        isLoading = true
        if (soundPool == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
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
        val noise = getPrefs().getString("noise", "fuzz")
        currentNoise = noise
        val resource: Int = when (noise) {
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
        getPrefs().edit().putBoolean("isPlaying", toPlaying).apply()
        if (doUpdatePref) {
            wasPlaying = toPlaying
            getPrefs().edit().putBoolean("wasPlaying", toPlaying).apply()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(
                this,
                ComponentName(this, QSTileService::class.java.getName())
            )
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

    fun noiseChanged() {
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
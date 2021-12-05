package com.cliambrown.easynoise

import android.Manifest
import android.animation.AnimatorListenerAdapter
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.cliambrown.easynoise.helpers.*
import android.os.IBinder
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.view.MenuItem
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.view.Menu
import android.view.animation.Animation
import androidx.constraintlayout.widget.ConstraintLayout
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AccelerateDecelerateInterpolator

class MainActivity : AppCompatActivity(), PlayerService.Callbacks, SeekBar.OnSeekBarChangeListener {

    lateinit var showPermissionNoticeButton: ImageButton
    lateinit var permissionNotice: ConstraintLayout
    lateinit var playButton: ImageButton
    lateinit var pauseButton: ImageButton
    lateinit var volumeBar: SeekBar
    lateinit var noiseSpinner: Spinner
    lateinit var noises: Array<String>
    lateinit var prefs: SharedPreferences

    private lateinit var playerService: PlayerService
    private var serviceIsBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to PlayerService, cast the IBinder and get PlayerService instance
            val binder = service as PlayerService.LocalBinder
            playerService = binder.getService()
            playerService.registerClient(this@MainActivity)
            serviceIsBound = true
            setButtonsVisibility(playerService.getIsPlaying())
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceIsBound = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.main_toolbar, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.my_toolbar))

        showPermissionNoticeButton = findViewById(R.id.showPermissionNoticeButton) as ImageButton
        permissionNotice = findViewById(R.id.permissionNotice) as ConstraintLayout

        playButton = findViewById(R.id.playButton) as ImageButton
        pauseButton = findViewById(R.id.pauseButton) as ImageButton

        volumeBar = findViewById(R.id.volumeBar) as SeekBar
        volumeBar.setOnSeekBarChangeListener(this)
        prefs = getSharedPreferences(applicationContext.packageName, 0)
        val volume = prefs.getInt("volume", 50)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            volumeBar.setProgress(volume, false)
        }

        val noise = prefs.getString("noise", "fuzz")

        noiseSpinner = findViewById(R.id.noiseSpinner)
        noises = arrayOf(
            resources.getString(R.string.fuzz),
            resources.getString(R.string.gray),
            resources.getString(R.string.gray_2),
            resources.getString(R.string.white),
            resources.getString(R.string.pink),
            resources.getString(R.string.brown),
            resources.getString(R.string.blue),
        )
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_list, noises
        )
        noiseSpinner.adapter = adapter

        val spinnerPosition: Int = adapter.getPosition(noise)
        noiseSpinner.setSelection(spinnerPosition)

        noiseSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val newNoise = noises[position]
                    prefs.edit().putString("noise", newNoise).apply()
                    if (serviceIsBound) {
                        playerService.noiseChanged()
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    //
                }
            }

        updateHasPhonePermission()
    }

    override fun onStart() {
        super.onStart()
        // Bind to LocalService
        Intent(this, PlayerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        serviceIsBound = false
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_info -> {
            // Click "info" button
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/cliambrown/EasyNoise"))
            startActivity(browserIntent)
            true
        }
        else -> {
            // Unrecognized action: invoke superclass
            super.onOptionsItemSelected(item)
        }
    }

    fun getCenterX(view: View): Float {
        return view.getX() + (view.getWidth() / 2)
    }

    fun getCenterY(view: View): Float {
        return view.getY() + (view.getHeight() / 2)
    }

    fun updateHasPhonePermission(animate: Boolean = false) {
        val readPhoneState =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        val granted = (readPhoneState == PackageManager.PERMISSION_GRANTED)
        if (granted) {
            showPermissionNoticeButton.setVisibility(View.GONE)
            permissionNotice.setVisibility(View.GONE)
        } else {
            val showNotice = prefs.getBoolean("showPhonePermissionNotice", true)
            val duration: Long = 400
            if (showNotice) {
                if (animate) {
                    val anim = AnimationSet(true)
                    anim.setFillAfter(true)
                    val scale = ScaleAnimation(0f,1f,0f,1f,Animation.RELATIVE_TO_SELF,1f,Animation.RELATIVE_TO_SELF,0f)
                    scale.setDuration(duration)
                    scale.setInterpolator(AccelerateDecelerateInterpolator())
                    anim.addAnimation(scale)
                    val deltaX = getCenterX(showPermissionNoticeButton) - permissionNotice.getX() - permissionNotice.getWidth()
                    val deltaY = getCenterY(showPermissionNoticeButton) - permissionNotice.getY()
                    val trans = TranslateAnimation(deltaX,0f,deltaY,0f)
                    trans.setDuration(duration)
                    trans.setInterpolator(AccelerateDecelerateInterpolator())
                    anim.addAnimation(trans)
                    anim.setAnimationListener(object : AnimationListener {
                        override fun onAnimationStart(p0: Animation?) {
                            showPermissionNoticeButton.setVisibility(View.INVISIBLE)
                        }
                        override fun onAnimationRepeat(p0: Animation?) {}
                        override fun onAnimationEnd(animation: Animation) {
                            permissionNotice.clearAnimation()
                            permissionNotice.setVisibility(View.VISIBLE)
                        }
                    })
                    permissionNotice.startAnimation(anim)
                } else {
                    showPermissionNoticeButton.setVisibility(View.INVISIBLE)
                    permissionNotice.setVisibility(View.VISIBLE)
                }
            } else {
                if (animate) {
                    val anim = AnimationSet(true)
                    anim.setFillAfter(true)
                    val scale = ScaleAnimation(1f, 0f, 1f, 0f)
                    scale.setDuration(duration)
                    scale.setInterpolator(AccelerateDecelerateInterpolator())
                    anim.addAnimation(scale)
                    val fromX = permissionNotice.getX()
                    val toX = getCenterX(showPermissionNoticeButton)
                    val deltaX = toX - fromX
                    val fromY = permissionNotice.getY()
                    val toY = getCenterY(showPermissionNoticeButton)
                    val deltaY = toY - fromY
                    val trans = TranslateAnimation(0f, deltaX, 0f, deltaY)
                    trans.setDuration(duration)
                    trans.setInterpolator(AccelerateDecelerateInterpolator())
                    anim.addAnimation(trans)
                    anim.setAnimationListener(object : AnimationListener {
                        override fun onAnimationStart(p0: Animation?) {}
                        override fun onAnimationRepeat(p0: Animation?) {}
                        override fun onAnimationEnd(animation: Animation) {
                            permissionNotice.clearAnimation()
                            permissionNotice.setVisibility(View.INVISIBLE)
                            showPermissionNoticeButton.setVisibility(View.VISIBLE)
                        }
                    })
                    permissionNotice.startAnimation(anim)
                } else {
                    showPermissionNoticeButton.setVisibility(View.VISIBLE)
                    permissionNotice.setVisibility(View.INVISIBLE)
                }
            }
        }
    }

    fun requestPhonePermission(@Suppress("UNUSED_PARAMETER")view: View) {
        val readPhoneState =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        if (readPhoneState != PackageManager.PERMISSION_GRANTED) {
            val permissions = arrayOf(Manifest.permission.READ_PHONE_STATE)
            ActivityCompat.requestPermissions(this, permissions, 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateHasPhonePermission()
    }

    fun hidePhonePermissionNotice(@Suppress("UNUSED_PARAMETER")view: View) {
        prefs.edit().putBoolean("showPhonePermissionNotice", false).apply()
        updateHasPhonePermission(true)
    }

    fun showPhonePermissionNotice(@Suppress("UNUSED_PARAMETER")view: View) {
        prefs.edit().putBoolean("showPhonePermissionNotice", true).apply()
        updateHasPhonePermission(true)
    }

    fun play(@Suppress("UNUSED_PARAMETER")view: View) {
        val intent = Intent(this@MainActivity, NotificationReceiver::class.java)
        intent.setAction(PLAY)
        sendBroadcast(intent)
    }

    fun pause(@Suppress("UNUSED_PARAMETER")view: View) {
        val intent = Intent(this@MainActivity, NotificationReceiver::class.java)
        intent.setAction(PAUSE)
        sendBroadcast(intent)
    }

    override fun updateClient(action: String) {
        when (action) {
            PLAY -> setButtonsVisibility(true)
            PAUSE -> setButtonsVisibility(false)
            DISMISS -> this.finishAndRemoveTask()
            VOLUME_CHANGED -> volumeChanged()
        }
    }

    fun setButtonsVisibility(isPlaying: Boolean) {
        if (isPlaying) {
            pauseButton.setVisibility(View.VISIBLE)
            playButton.setVisibility(View.GONE)
        } else {
            playButton.setVisibility(View.VISIBLE)
            pauseButton.setVisibility(View.GONE)
        }
    }

    fun volumeChanged() {
        val volume = prefs.getInt("volume", 50)
        volumeBar.setProgress(volume)
    }

    override fun onProgressChanged(
        seekBar: SeekBar?, progress: Int,
        fromUser: Boolean,
    ) {
        prefs.edit().putInt("volume", progress).apply()
        if (serviceIsBound) playerService.updateVolume()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        // Needed for some reason
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        // Needed for some reason
    }

}
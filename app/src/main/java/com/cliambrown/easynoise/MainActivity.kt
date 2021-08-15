package com.cliambrown.easynoise

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import com.cliambrown.easynoise.helpers.*
import android.os.IBinder

class MainActivity : AppCompatActivity(), PlayerService.Callbacks {

    lateinit var playButton: ImageButton
    lateinit var pauseButton: ImageButton

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        playButton = findViewById(R.id.playButton) as ImageButton
        pauseButton = findViewById(R.id.pauseButton) as ImageButton
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

    fun play(view: View) {
        val intent = Intent(this@MainActivity, NotificationReceiver::class.java)
        intent.setAction(PLAY)
        sendBroadcast(intent)
    }

    fun pause(view: View) {
        val intent = Intent(this@MainActivity, NotificationReceiver::class.java)
        intent.setAction(PAUSE)
        sendBroadcast(intent)
    }

    override fun updateClient(action: String) {
        when (action) {
            PLAY -> setButtonsVisibility(true)
            PAUSE -> setButtonsVisibility(false)
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

}
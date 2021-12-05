package com.cliambrown.easynoise

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.cliambrown.easynoise.helpers.*

@RequiresApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if(qsTile.state == Tile.STATE_INACTIVE) {
            PlayerService.start(applicationContext, PLAY)
        } else {
            PlayerService.start(applicationContext, PAUSE)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    fun updateTile() {
        val isPlaying = getSharedPreferences(applicationContext.packageName, 0)
            .getBoolean("isPlaying", false)
        val resource = if (isPlaying) R.drawable.notification_icon else R.drawable.paused_notification_icon
        qsTile.setIcon(Icon.createWithResource(this, resource))
        qsTile.state = if (isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}
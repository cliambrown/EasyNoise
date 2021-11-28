package com.cliambrown.easynoise

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import com.cliambrown.easynoise.helpers.*

@RequiresApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {

    override fun onTileAdded() {
        Log.i("clb-info", "onTileAdded()")
        super.onTileAdded()
        updateTile()
    }

    override fun onClick() {
        Log.i("clb-info", "onClick()")
        super.onClick()
        if(qsTile.state == Tile.STATE_INACTIVE) {
            Util.startPlayerService(applicationContext, PLAY)
        } else {
            Util.startPlayerService(applicationContext, PAUSE)
        }
    }

    override fun onStartListening() {
        Log.i("clb-info", "onStartListening()")
        super.onStartListening()
        updateTile()
    }

    fun updateTile() {
        Log.i("clb-info", "updateTile()")
        val isPlaying = getSharedPreferences(applicationContext.packageName, 0)
            .getBoolean("isPlaying", false)
        val resource = if (isPlaying) R.drawable.notification_icon else R.drawable.paused_notification_icon
        qsTile.setIcon(Icon.createWithResource(this, resource))
        qsTile.state = if (isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}
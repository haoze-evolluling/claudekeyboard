package com.haoze.claudekeyboard.bluetooth

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.haoze.claudekeyboard.R

/**
 * Quick Settings tile for one-tap Bluetooth connect/disconnect.
 *
 * Click behaviour:
 *   - Connected → disconnect
 *   - Not connected, has last device → connect
 *   - Not connected, no last device → open app
 */
class BluetoothTileService : TileService() {

    private var hidService: BluetoothHidService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothHidService.LocalBinder
            hidService = binder.getService()
            isBound = true
            updateTile()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hidService = null
            isBound = false
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        bindToService()
    }

    override fun onStopListening() {
        super.onStopListening()
        unbindFromService()
    }

    override fun onClick() {
        super.onClick()
        val svc = hidService

        if (svc == null || !isBound) {
            bindToService()
            return
        }

        if (svc.isConnected()) {
            svc.disconnect()
            updateTileState(false)
        } else {
            val lastAddr = svc.getLastConnectedDeviceAddress()
            if (lastAddr != null) {
                if (svc.connectToDevice(lastAddr)) {
                    updateTileState(true)
                }
            } else {
                val intent = Intent(Intent.ACTION_MAIN)
                    .setClassName(packageName, "com.haoze.claudekeyboard.MainActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivityAndCollapse(intent)
            }
        }
    }

    private fun bindToService() {
        if (isBound) return
        val intent = Intent(this, BluetoothHidService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindFromService() {
        if (isBound) {
            runCatching { unbindService(serviceConnection) }
            isBound = false
        }
        hidService = null
    }

    private fun updateTile() {
        val svc = hidService
        updateTileState(svc?.isConnected() == true)
    }

    private fun updateTileState(connected: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (connected) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(if (connected) R.string.tile_connected else R.string.tile_disconnected)
        tile.updateTile()
    }

    }
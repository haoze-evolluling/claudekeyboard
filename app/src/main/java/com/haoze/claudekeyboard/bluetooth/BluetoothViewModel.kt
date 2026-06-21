package com.haoze.claudekeyboard.bluetooth

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * ViewModel that manages the BluetoothHidService lifecycle and exposes
 * connection state, senders, and device info as observable LiveData.
 *
 * This eliminates the need for Fragments to reach through Activity → Service
 * chains, and ensures state survives configuration changes.
 */
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    // ---- Observable state ----

    private val _connectionState = MutableLiveData(false)
    val connectionState: LiveData<Boolean> = _connectionState

    private val _connectedDeviceName = MutableLiveData<String?>()
    val connectedDeviceName: LiveData<String?> = _connectedDeviceName

    private val _keyboardSender = MutableLiveData<KeyboardSender?>()
    val keyboardSender: LiveData<KeyboardSender?> = _keyboardSender

    private val _mouseSender = MutableLiveData<MouseSender?>()
    val mouseSender: LiveData<MouseSender?> = _mouseSender

    private val _tvRemoteSender = MutableLiveData<TvRemoteSender?>()
    val tvRemoteSender: LiveData<TvRemoteSender?> = _tvRemoteSender

    private val _registrationState = MutableLiveData(true)
    val registrationState: LiveData<Boolean> = _registrationState

    private val _sendError = MutableLiveData<String>()
    val sendError: LiveData<String> = _sendError

    // ---- Service binding ----

    private var hidService: BluetoothHidService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothHidService.LocalBinder
            hidService = binder.getService()
            isBound = true
            setupServiceCallbacks()
            // Sync connection state only; registration state will be driven
            // by the Service callback once registerHidDevice() completes.
            val svc = hidService ?: return
            updateConnectionState(svc.isConnected(), svc.getConnectedDeviceName())
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hidService = null
            isBound = false
            _connectionState.value = false
            _keyboardSender.value = null
            _mouseSender.value = null
            _tvRemoteSender.value = null
        }
    }

    /**
     * Start and bind to the BluetoothHidService.
     * Call this once from Activity.onCreate().
     */
    fun startAndBindService() {
        val context = getApplication<Application>()
        val intent = Intent(context, BluetoothHidService::class.java)
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupServiceCallbacks() {
        hidService?.setOnConnectionStateChangedListener { isConnected, deviceName ->
            updateConnectionState(isConnected, deviceName)
        }
        hidService?.setOnRegistrationStateChangedListener { isRegistered ->
            _registrationState.postValue(isRegistered)
        }
        hidService?.setOnSendErrorListener { message ->
            _sendError.postValue(message)
        }
    }

    private fun updateConnectionState(isConnected: Boolean, deviceName: String?) {
        if (isConnected) {
            _connectionState.postValue(true)
            _connectedDeviceName.postValue(deviceName)
            _keyboardSender.postValue(hidService?.getKeyboardSender())
            _mouseSender.postValue(hidService?.getMouseSender())
            _tvRemoteSender.postValue(hidService?.getTvRemoteSender())
        } else {
            _connectionState.postValue(false)
            _connectedDeviceName.postValue(null)
            _keyboardSender.postValue(null)
            _mouseSender.postValue(null)
            _tvRemoteSender.postValue(null)
        }
    }

    // ---- Public API ----

    fun isConnected(): Boolean = hidService?.isConnected() == true

    fun getConnectedDeviceName(): String? = hidService?.getConnectedDeviceName()

    fun getConnectedDeviceAddress(): String? = hidService?.getConnectedDeviceAddress()

    fun getLastConnectedDeviceAddress(): String? = hidService?.getLastConnectedDeviceAddress()

    fun hasLastConnectedDevice(): Boolean = hidService?.hasLastConnectedDevice() == true

    fun getKeyboardSenderDirect(): KeyboardSender? = hidService?.getKeyboardSender()
    fun getTvRemoteSenderDirect(): TvRemoteSender? = hidService?.getTvRemoteSender()

    fun connectToDevice(address: String): Boolean {
        return hidService?.connectToDevice(address) == true
    }

    fun disconnect() {
        hidService?.disconnect()
    }

    fun dismissConnectionNotification() {
        hidService?.dismissConnectionNotification()
    }

    fun hasBluetoothSupport(): Boolean {
        val context = getApplication<Application>()
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }

    // ---- Lifecycle ----

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}

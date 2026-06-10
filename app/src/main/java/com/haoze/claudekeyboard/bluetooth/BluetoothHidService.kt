package com.haoze.claudekeyboard.bluetooth

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.haoze.claudekeyboard.MainActivity
import com.haoze.claudekeyboard.R
import java.util.concurrent.Executors

/**
 * Foreground service that manages Bluetooth HID device registration and connection.
 * Uses Kontroller's DescriptorCollection for HID descriptors and KeyboardSender for report sending.
 */
class BluetoothHidService : Service() {

    companion object {
        private const val TAG = "BluetoothHidService"
        private const val NOTIFICATION_CHANNEL_ID = "bluetooth_hid_channel"
        private const val NOTIFICATION_ID = 1001
        private const val MAX_REGISTRATION_RETRIES = 3
    }

    // Binder given to clients
    private val binder = LocalBinder()

    // Bluetooth components
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null

    // Keyboard sender (created on connection)
    private var keyboardSender: KeyboardSender? = null

    // Callbacks
    private var onConnectionStateChanged: ((Boolean, String?) -> Unit)? = null
    private var onRegistrationStateChanged: ((Boolean) -> Unit)? = null

    // State
    private var isRegistered = false
    private var isRegistering = false
    private var isConnected = false
    private var userInitiatedDisconnect = false
    private var isShuttingDown = false
    private var registrationRetryCount = 0

    // Last connected device address (for auto-reconnect)
    private var lastConnectedDeviceAddress: String? = null
    private var lastReconnectAttempt: Long = 0

    // Main thread handler for scheduling reconnection
    private val mainHandler = Handler(Looper.getMainLooper())

    // Bluetooth state receiver - re-initialize HID when Bluetooth turns on
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        Log.d(TAG, "Bluetooth turned on, re-initializing HID")
                        isRegistered = false
                        isRegistering = false
                        isConnected = false
                        registrationRetryCount = 0
                        connectedDevice = null
                        keyboardSender = null
                        mainHandler.removeCallbacksAndMessages(null)
                        hidDevice?.let {
                            try { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it) } catch (_: Exception) {}
                        }
                        hidDevice = null
                        // Delay initialization to let Bluetooth stack stabilize
                        mainHandler.postDelayed({
                            initializeHidDevice()
                        }, 1000)
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        Log.d(TAG, "Bluetooth turned off")
                        isRegistered = false
                        isRegistering = false
                        isConnected = false
                        registrationRetryCount = 0
                        connectedDevice = null
                        keyboardSender = null
                        hidDevice?.let {
                            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
                        }
                        hidDevice = null
                        onConnectionStateChanged?.invoke(false, null)
                        updateNotification(getString(R.string.notification_waiting))
                    }
                }
            }
        }
    }

    /**
     * Binder class for client binding.
     */
    inner class LocalBinder : Binder() {
        fun getService(): BluetoothHidService = this@BluetoothHidService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported")
            stopSelf()
            return
        }

        // Register Bluetooth state receiver
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(getString(R.string.notification_waiting)))
        initializeHidDevice()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        isShuttingDown = true
        mainHandler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Receiver already unregistered")
        }
        disconnect()
        unregisterHidDevice()
    }

    /**
     * Set callback for connection state changes.
     */
    fun setOnConnectionStateChangedListener(listener: (Boolean, String?) -> Unit) {
        onConnectionStateChanged = listener
    }

    /**
     * Set callback for registration state changes.
     */
    fun setOnRegistrationStateChangedListener(listener: (Boolean) -> Unit) {
        onRegistrationStateChanged = listener
    }

    /**
     * Get the keyboard sender. Null if not connected.
     */
    fun getKeyboardSender(): KeyboardSender? = keyboardSender

    /**
     * Initialize the HID device profile proxy.
     */
    private fun initializeHidDevice() {
        Log.d(TAG, "Initializing HID device")
        bluetoothAdapter?.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = proxy as BluetoothHidDevice
                    Log.d(TAG, "HID device proxy obtained")
                    registerHidDevice()
                    setDiscoverable()
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = null
                    isRegistered = false
                    Log.d(TAG, "HID device proxy disconnected")
                }
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    /**
     * Register this device as an HID device using Kontroller's DescriptorCollection.
     */
    private fun registerHidDevice() {
        val hidDevice = hidDevice ?: return

        if (isRegistered) {
            Log.d(TAG, "HID device already registered, skipping")
            return
        }
        if (isRegistering) {
            Log.d(TAG, "HID device registration already in progress, skipping")
            return
        }

        // Clean up any stale registration (safe to call even if not registered)
        try {
            hidDevice.unregisterApp()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister stale app: ${e.message}")
        }

        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "Claude Code Keyboard",
            "Claude Code Bluetooth HID Keyboard",
            "Claude Code",
            BluetoothHidDevice.SUBCLASS1_KEYBOARD,
            DescriptorCollection.KEYBOARD
        )

        val executor = Executors.newSingleThreadExecutor()

        val callback = object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                super.onAppStatusChanged(pluggedDevice, registered)
                isRegistering = false
                isRegistered = registered
                Log.d(TAG, "HID app registration status changed: $registered")

                if (registered) {
                    registrationRetryCount = 0
                    updateNotification(getString(R.string.notification_waiting))
                    onRegistrationStateChanged?.invoke(true)
                } else {
                    scheduleRegistrationRetry()
                }
            }

            override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                super.onConnectionStateChanged(device, state)
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectedDevice = device
                        isConnected = true
                        userInitiatedDisconnect = false
                        lastConnectedDeviceAddress = device?.address
                        Log.d(TAG, "Connected to: ${device?.name}")

                        // Create KeyboardSender for the connected device
                        keyboardSender = KeyboardSender(hidDevice, device!!)

                        // Set connection policy to allow auto-reconnect (API 33+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            try {
                                val method = hidDevice.javaClass.getMethod(
                                    "setConnectionPolicy",
                                    BluetoothDevice::class.java,
                                    Int::class.javaPrimitiveType
                                )
                                // CONNECTION_POLICY_ALLOWED = 1
                                method.invoke(hidDevice, device, 1)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to set connection policy: ${e.message}")
                            }
                        }
                        updateNotification(getString(R.string.notification_connected, device?.name ?: ""))
                        onConnectionStateChanged?.invoke(true, device?.name)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectedDevice = null
                        isConnected = false
                        keyboardSender = null
                        Log.d(TAG, "Disconnected")
                        updateNotification(getString(R.string.notification_waiting))
                        onConnectionStateChanged?.invoke(false, null)
                        setDiscoverable()
                        // Auto-connect only if not user-initiated
                        if (!userInitiatedDisconnect) {
                            scheduleReconnect()
                        }
                        userInitiatedDisconnect = false
                    }
                }
            }

            override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
                super.onGetReport(device, type, id, bufferSize)
                // Host is requesting current key state — reply with all-zeros (no keys pressed)
                try {
                    val method = hidDevice.javaClass.getMethod(
                        "sendReply",
                        BluetoothDevice::class.java,
                        Byte::class.javaPrimitiveType,
                        Byte::class.javaPrimitiveType,
                        ByteArray::class.java
                    )
                    method.invoke(hidDevice, device, type, id, ByteArray(8))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send GET_REPORT reply: ${e.message}")
                }
            }
        }

        isRegistering = true
        val registerStarted = hidDevice.registerApp(sdpSettings, null, null, executor, callback)
        Log.d(TAG, "HID app registration started: $registerStarted")
        if (!registerStarted) {
            isRegistering = false
            scheduleRegistrationRetry()
        }
    }

    /**
     * Make the device discoverable via hidden API setScanMode.
     * Ported from Kontroller's Unhide.kt pattern.
     * SCAN_MODE_CONNECTABLE_DISCOVERABLE = 23, duration 300 seconds.
     */
    private fun setDiscoverable() {
        try {
            val method = bluetoothAdapter?.javaClass?.getMethod(
                "setScanMode",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            val result = method?.invoke(bluetoothAdapter, 23, 300)
            Log.d(TAG, "setScanMode result: $result")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set discoverable mode: ${e.message}")
        }
    }

    private fun scheduleRegistrationRetry() {
        val adapter = bluetoothAdapter ?: return
        if (isShuttingDown || !adapter.isEnabled || hidDevice == null || isRegistered || isRegistering) return
        if (registrationRetryCount >= MAX_REGISTRATION_RETRIES) {
            Log.w(TAG, "HID app registration failed after retries")
            onRegistrationStateChanged?.invoke(false)
            return
        }

        registrationRetryCount++
        Log.d(TAG, "Retrying HID app registration ($registrationRetryCount/$MAX_REGISTRATION_RETRIES)")
        mainHandler.postDelayed({
            registerHidDevice()
        }, 1000L * registrationRetryCount)
    }

    /**
     * Schedule a proactive reconnection attempt after unexpected disconnect.
     */
    private fun scheduleReconnect() {
        val address = lastConnectedDeviceAddress ?: return
        Log.d(TAG, "Scheduling reconnect to $address")

        // Give the Bluetooth stack a moment to settle before trying to connect.
        mainHandler.postDelayed({
            val hd = hidDevice ?: return@postDelayed
            if (isConnected || userInitiatedDisconnect) return@postDelayed

            tryConnectToLastDevice()
        }, 2500)
    }

    /**
     * Proactively connect to the last known device.
     */
    private fun tryConnectToLastDevice(): Boolean {
        val address = lastConnectedDeviceAddress ?: return false
        val hd = hidDevice ?: return false
        val adapter = bluetoothAdapter ?: return false
        if (isConnected) return true
        if (!isRegistered) {
            Log.w(TAG, "Cannot connect: HID not registered yet")
            return false
        }

        val now = System.currentTimeMillis()
        if (now - lastReconnectAttempt < 2000) return false
        lastReconnectAttempt = now

        val device = adapter.bondedDevices?.find { it.address == address } ?: run {
            Log.w(TAG, "Last device ($address) not found in bonded devices")
            return false
        }
        Log.d(TAG, "Attempting HID connect to ${device.name} ($address)")
        val result = try {
            hd.connect(device)
        } catch (e: Exception) {
            Log.w(TAG, "HID connect failed: ${e.message}")
            false
        }
        Log.d(TAG, "hd.connect returned $result")
        if (!result) {
            Log.d(TAG, "Phone-initiated connect failed — the host must discover and connect to this device")
        }
        return result
    }

    /**
     * Unregister the HID device.
     */
    private fun unregisterHidDevice() {
        hidDevice?.let { device ->
            if (isRegistered) {
                device.unregisterApp()
                isRegistered = false
                Log.d(TAG, "HID device unregistered")
            }
        }
    }

    /**
     * Check if the device is currently connected.
     */
    fun isConnected(): Boolean = isConnected

    /**
     * Check if the HID device is registered.
     */
    fun isRegistered(): Boolean = isRegistered

    /**
     * Check if there is a previous host to reconnect to.
     */
    fun hasLastConnectedDevice(): Boolean = lastConnectedDeviceAddress != null

    /**
     * Get the name of the connected device.
     */
    fun getConnectedDeviceName(): String? = connectedDevice?.name

    /**
     * Disconnect from the current device.
     */
    fun disconnect() {
        userInitiatedDisconnect = true
        connectedDevice?.let { device ->
            hidDevice?.disconnect(device)
        }
        setDiscoverable()
    }

    /**
     * Connect to the last paired host from the app instead of relying on system Bluetooth UI.
     */
    fun connectToLastDevice(): Boolean {
        userInitiatedDisconnect = false
        lastReconnectAttempt = 0
        setDiscoverable()
        return tryConnectToLastDevice()
    }

    /**
     * Create notification channel for foreground service.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create a notification for the foreground service.
     */
    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    /**
     * Update the foreground service notification.
     */
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

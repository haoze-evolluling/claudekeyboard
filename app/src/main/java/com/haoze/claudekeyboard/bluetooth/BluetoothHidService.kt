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
 */
class BluetoothHidService : Service() {

    companion object {
        private const val TAG = "BluetoothHidService"
        private const val NOTIFICATION_CHANNEL_ID = "bluetooth_hid_channel"
        private const val NOTIFICATION_ID = 1001
        private const val HID_REPORT_ID: Byte = 0x01
    }

    // Binder given to clients
    private val binder = LocalBinder()

    // Bluetooth components
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null

    // Callbacks
    private var onConnectionStateChanged: ((Boolean, String?) -> Unit)? = null
    private var onRegistrationStateChanged: ((Boolean) -> Unit)? = null

    // State
    private var isRegistered = false
    private var isConnected = false
    private var userInitiatedDisconnect = false

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
                        isConnected = false
                        connectedDevice = null
                        hidDevice = null
                        initializeHidDevice()
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        Log.d(TAG, "Bluetooth turned off")
                        isRegistered = false
                        isConnected = false
                        connectedDevice = null
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

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
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
     * Register this device as an HID device.
     */
    private fun registerHidDevice() {
        val hidDevice = hidDevice ?: return

        if (isRegistered) {
            Log.d(TAG, "HID device already registered, skipping")
            return
        }

        // Clean up any stale registration from a previous session (e.g. after crash)
        try {
            hidDevice.unregisterApp()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister stale app: ${e.message}")
        }

        // Standard keyboard HID Report Descriptor
        // Report: [modifier(1) + reserved(1) + keys(6)] = 8 bytes
        val descriptor = byteArrayOf(
            0x05.toByte(), 0x01.toByte(),  // Usage Page (Generic Desktop)
            0x09.toByte(), 0x06.toByte(),  // Usage (Keyboard)
            0xA1.toByte(), 0x01.toByte(),  // Collection (Application)
            0x85.toByte(), HID_REPORT_ID,  //   Report ID (1)
            // Modifier byte
            0x75.toByte(), 0x08.toByte(),  //   Report Size (8)
            0x95.toByte(), 0x01.toByte(),  //   Report Count (1)
            0x05.toByte(), 0x07.toByte(),  //   Usage Page (Keyboard/Keypad)
            0x19.toByte(), 0xE0.toByte(),  //   Usage Minimum (Left Control)
            0x29.toByte(), 0xE7.toByte(),  //   Usage Maximum (Right GUI)
            0x15.toByte(), 0x00.toByte(),  //   Logical Minimum (0)
            0x25.toByte(), 0x01.toByte(),  //   Logical Maximum (1)
            0x81.toByte(), 0x02.toByte(),  //   Input (Data, Variable, Absolute)
            // Reserved byte
            0x75.toByte(), 0x08.toByte(),  //   Report Size (8)
            0x95.toByte(), 0x01.toByte(),  //   Report Count (1)
            0x81.toByte(), 0x01.toByte(),  //   Input (Constant)
            // Key array (6 keys)
            0x75.toByte(), 0x08.toByte(),  //   Report Size (8)
            0x95.toByte(), 0x06.toByte(),  //   Report Count (6)
            0x15.toByte(), 0x00.toByte(),  //   Logical Minimum (0)
            0x25.toByte(), 0x65.toByte(),  //   Logical Maximum (101)
            0x05.toByte(), 0x07.toByte(),  //   Usage Page (Keyboard/Keypad)
            0x19.toByte(), 0x00.toByte(),  //   Usage Minimum (0)
            0x29.toByte(), 0x65.toByte(),  //   Usage Maximum (101)
            0x81.toByte(), 0x00.toByte(),  //   Input (Data, Array)
            0xC0.toByte()                  // End Collection
        )

        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "Claude Code Keyboard",
            "Claude Code Bluetooth HID Keyboard",
            "Claude Code",
            BluetoothHidDevice.SUBCLASS1_KEYBOARD,
            descriptor
        )

        val executor = Executors.newSingleThreadExecutor()

        hidDevice.registerApp(sdpSettings, null, null, executor, object : BluetoothHidDevice.Callback() {
            override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                super.onConnectionStateChanged(device, state)
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectedDevice = device
                        isConnected = true
                        userInitiatedDisconnect = false
                        lastConnectedDeviceAddress = device?.address
                        Log.d(TAG, "Connected to: ${device?.name}")
                        // Set connection policy to allow auto-reconnect (API 33+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            try {
                                val method = hidDevice?.javaClass?.getMethod(
                                    "setConnectionPolicy",
                                    BluetoothDevice::class.java,
                                    Int::class.javaPrimitiveType
                                )
                                // CONNECTION_POLICY_ALLOWED = 1
                                method?.invoke(hidDevice, device, 1)
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
                        isRegistered = false
                        Log.d(TAG, "Disconnected")
                        updateNotification(getString(R.string.notification_waiting))
                        onConnectionStateChanged?.invoke(false, null)
                        // Cancel any pending reconnect callbacks first
                        mainHandler.removeCallbacksAndMessages(null)
                        // Auto-reconnect only if not user-initiated
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
                if (id.toInt() == HID_REPORT_ID.toInt()) {
                    try {
                        val method = hidDevice?.javaClass?.getMethod(
                            "sendReply",
                            BluetoothDevice::class.java,
                            Byte::class.javaPrimitiveType,
                            Byte::class.javaPrimitiveType,
                            ByteArray::class.java
                        )
                        method?.invoke(hidDevice, device, type, id, ByteArray(8))
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to send GET_REPORT reply: ${e.message}")
                    }
                }
            }
        })

        // registerApp() is async — mark as registered now.
        // If it fails internally, the SDP record won't exist and no host
        // will connect, so the user sees "Waiting" (correct feedback).
        isRegistered = true
        Log.d(TAG, "HID device registered, waiting for host connection")
        updateNotification(getString(R.string.notification_waiting))
        onRegistrationStateChanged?.invoke(true)
    }

    /**
     * Schedule a reconnection attempt after disconnect.
     * Unregisters and re-registers the SDP record, then waits for the host
     * to discover and connect (or proactively connects on API 33+).
     */
    private fun scheduleReconnect() {
        val address = lastConnectedDeviceAddress ?: return
        Log.d(TAG, "Scheduling reconnect to $address")

        mainHandler.postDelayed({
            val hd = hidDevice ?: return@postDelayed
            if (isConnected || isRegistered || userInitiatedDisconnect) return@postDelayed

            Log.d(TAG, "Re-registering HID SDP for reconnect")
            registerHidDevice()

            // After re-registering, give SDP time to propagate, then try connect
            mainHandler.postDelayed({
                tryConnectToLastDevice()
            }, 1500)
        }, 1000)
    }

    /**
     * Proactively connect to the last known device.
     * Only works on API 33+ where BluetoothHidDevice.connect() is available.
     * For older API levels, the host must initiate the connection.
     */
    private fun tryConnectToLastDevice() {
        val address = lastConnectedDeviceAddress ?: return
        val hd = hidDevice ?: return
        val adapter = bluetoothAdapter ?: return
        if (isConnected) return
        if (!isRegistered) {
            Log.w(TAG, "Cannot connect: HID not registered yet")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastReconnectAttempt < 2000) return
        lastReconnectAttempt = now

        val device = adapter.bondedDevices?.find { it.address == address } ?: run {
            Log.w(TAG, "Last device ($address) not found in bonded devices")
            return
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
     * Send a HID report to the connected device.
     */
    fun sendReport(report: ByteArray): Boolean {
        if (!isConnected || connectedDevice == null) {
            Log.w(TAG, "Not connected, cannot send report")
            return false
        }

        return try {
            val result = hidDevice?.sendReport(connectedDevice, HID_REPORT_ID.toInt(), report) ?: false
            if (!result) {
                Log.w(TAG, "sendReport returned false, report was rejected")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send report: ${e.message}")
            false
        }
    }

    /**
     * Send a key press and release.
     */
    fun sendKeyPress(modifier: Byte, keyCode: Byte, delayMs: Long = 20) {
        val report = HidReportHelper.buildReport(modifier, keyCode)
        if (sendReport(report)) {
            Thread.sleep(delayMs)
            sendReport(HidReportHelper.buildReleaseReport())
        }
    }

    /**
     * Send a string as key presses.
     */
    fun sendText(text: String, delayMs: Long = 30) {
        for (char in text) {
            val hidCode = HidReportHelper.charToHidCode(char)
            if (hidCode != null) {
                sendKeyPress(hidCode.first, hidCode.second)
                Thread.sleep(delayMs)
            }
        }
    }

    /**
     * Send a macro command.
     */
    fun sendMacro(command: String) {
        sendText(command)
        sendKeyPress(0x00, HidReportHelper.KEY_ENTER)
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
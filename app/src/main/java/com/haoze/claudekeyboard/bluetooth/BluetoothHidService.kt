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
import android.os.IBinder
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

    // Last connected device (for auto-reconnect)
    private var lastConnectedDevice: BluetoothDevice? = null
    private var isReconnecting = false

    // Bluetooth state receiver - re-initialize HID when Bluetooth turns on
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        Log.d(TAG, "Bluetooth turned on, re-initializing HID")
                        isRegistered = false
                        hidDevice = null
                        initializeHidDevice()
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        Log.d(TAG, "Bluetooth turned off")
                        isRegistered = false
                        isConnected = false
                        connectedDevice = null
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
        val descriptor = byteArrayOf(
            0x05.toByte(), 0x01.toByte(),  // Usage Page (Generic Desktop)
            0x09.toByte(), 0x06.toByte(),  // Usage (Keyboard)
            0xA1.toByte(), 0x01.toByte(),  // Collection (Application)
            0x85.toByte(), HID_REPORT_ID,  // Report ID (1)
            0x75.toByte(), 0x08.toByte(),  // Report Size (8)
            0x95.toByte(), 0x01.toByte(),  // Report Count (1)
            0x05.toByte(), 0x07.toByte(),  // Usage Page (Keyboard/Keypad)
            0x19.toByte(), 0xE0.toByte(),  // Usage Minimum (Left Control)
            0x29.toByte(), 0xE7.toByte(),  // Usage Maximum (Right GUI)
            0x15.toByte(), 0x00.toByte(),  // Logical Minimum (0)
            0x25.toByte(), 0x01.toByte(),  // Logical Maximum (1)
            0x81.toByte(), 0x02.toByte(),  // Input (Data, Variable, Absolute)
            0x75.toByte(), 0x08.toByte(),  // Report Size (8)
            0x95.toByte(), 0x07.toByte(),  // Report Count (7)
            0x15.toByte(), 0x00.toByte(),  // Logical Minimum (0)
            0x25.toByte(), 0x65.toByte(),  // Logical Maximum (101)
            0x05.toByte(), 0x07.toByte(),  // Usage Page (Keyboard/Keypad)
            0x19.toByte(), 0x00.toByte(),  // Usage Minimum (0)
            0x29.toByte(), 0x65.toByte(),  // Usage Maximum (101)
            0x81.toByte(), 0x00.toByte(),  // Input (Data, Array)
            0xC0.toByte()   // End Collection
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
                        isReconnecting = false
                        lastConnectedDevice = device
                        Log.d(TAG, "Connected to: ${device?.name}")
                        updateNotification(getString(R.string.notification_connected, device?.name ?: ""))
                        onConnectionStateChanged?.invoke(true, device?.name)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectedDevice = null
                        isConnected = false
                        Log.d(TAG, "Disconnected, re-registering HID for reconnection")
                        updateNotification(getString(R.string.notification_waiting))
                        onConnectionStateChanged?.invoke(false, null)
                        if (!isReconnecting) {
                            isReconnecting = true
                            isRegistered = false
                            registerHidDevice()
                        }
                    }
                }
            }

        })

        isRegistered = true
        Log.d(TAG, "HID device registered")
        updateNotification(getString(R.string.notification_waiting))
        onRegistrationStateChanged?.invoke(true)
        tryConnectToLastDevice()
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
            hidDevice?.sendReport(connectedDevice, HID_REPORT_ID.toInt(), report)
            true
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
        connectedDevice?.let { device ->
            hidDevice?.disconnect(device)
        }
    }

    /**
     * Proactively connect to the last known device.
     * This is needed because Android's BluetoothHidDevice doesn't
     * properly accept incoming reconnections after a disconnect.
     */
    private fun tryConnectToLastDevice() {
        val device = lastConnectedDevice ?: return
        val hd = hidDevice ?: return
        if (isConnected) return
        Log.d(TAG, "Auto-connecting to last device: ${device.name}")
        try {
            val success = hd.connect(device)
            if (!success) {
                Log.w(TAG, "Auto-connect returned false")
                isReconnecting = false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Auto-connect failed: ${e.message}")
            isReconnecting = false
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
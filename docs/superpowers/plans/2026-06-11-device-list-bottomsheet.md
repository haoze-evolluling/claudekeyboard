# Device List BottomSheet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single-device connect button with a BottomSheet dialog showing all system paired Bluetooth devices for selection.

**Architecture:** A new `DeviceListBottomSheetFragment` gets bonded devices from `BluetoothAdapter`, displays them in a RecyclerView via `DeviceAdapter`, and notifies `MainActivity` to connect on selection. `BluetoothHidService` gains a `connectToDevice(address)` method.

**Tech Stack:** Kotlin, Android Material3 BottomSheetDialogFragment, BluetoothAdapter bondedDevices

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `app/src/main/res/values/strings.xml` | Modify | Add device list string resources |
| `app/src/main/res/layout/item_device.xml` | Create | RecyclerView list item for device |
| `app/src/main/java/com/haoze/claudekeyboard/ui/device/DeviceAdapter.kt` | Create | RecyclerView adapter for device list |
| `app/src/main/java/com/haoze/claudekeyboard/ui/device/DeviceListBottomSheetFragment.kt` | Create | BottomSheet dialog with device list |
| `app/src/main/java/com/haoze/claudekeyboard/bluetooth/BluetoothHidService.kt` | Modify | Add `connectToDevice()` method |
| `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt` | Modify | Wire BottomSheet to connect button |

---

### Task 1: Add string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add device list strings**

Add before the closing `</resources>` tag (after the `switch_send_enter` line):

```xml
    <string name="device_list_title">设备列表</string>
    <string name="device_last_connected">上次连接</string>
    <string name="device_no_paired">没有已配对设备，请先在系统蓝牙设置中配对</string>
    <string name="device_connected">已连接</string>
```

---

### Task 2: Create item_device.xml layout

**Files:**
- Create: `app/src/main/res/layout/item_device.xml`

- [ ] **Step 1: Create the layout file**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="?attr/selectableItemBackground"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingHorizontal="24dp"
    android:paddingVertical="16dp">

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/tv_device_name"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textAppearance="?attr/textAppearanceBodyLarge"
            android:textColor="?attr/colorOnSurface" />

        <TextView
            android:id="@+id/tv_device_address"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="2dp"
            android:textAppearance="?attr/textAppearanceBodySmall"
            android:textColor="?attr/colorOnSurfaceVariant" />

    </LinearLayout>

    <TextView
        android:id="@+id/tv_device_status"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:textAppearance="?attr/textAppearanceLabelMedium"
        android:textColor="?attr/colorPrimary"
        android:visibility="gone" />

</LinearLayout>
```

---

### Task 3: Create DeviceAdapter

**Files:**
- Create: `app/src/main/java/com/haoze/claudekeyboard/ui/device/DeviceAdapter.kt`

- [ ] **Step 1: Create the adapter**

```kotlin
package com.haoze.claudekeyboard.ui.device

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.haoze.claudekeyboard.R

/**
 * Adapter for displaying paired Bluetooth devices in a list.
 */
class DeviceAdapter(
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private var devices: List<BluetoothDevice> = emptyList()
    private var connectedAddress: String? = null
    private var lastConnectedAddress: String? = null

    /**
     * Update the device list and status.
     * @param devices List of bonded devices
     * @param connectedAddress Address of currently connected device (null if disconnected)
     * @param lastConnectedAddress Address of last connected device
     */
    fun updateDevices(devices: List<BluetoothDevice>, connectedAddress: String?, lastConnectedAddress: String?) {
        this.devices = devices
        this.connectedAddress = connectedAddress
        this.lastConnectedAddress = lastConnectedAddress
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_device_name)
        private val tvAddress: TextView = itemView.findViewById(R.id.tv_device_address)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_device_status)

        fun bind(device: BluetoothDevice) {
            tvName.text = device.name ?: itemView.context.getString(R.string.status_unknown_device)
            tvAddress.text = device.address

            when (device.address) {
                connectedAddress -> {
                    tvStatus.text = itemView.context.getString(R.string.device_connected)
                    tvStatus.visibility = View.VISIBLE
                    itemView.isEnabled = false
                    itemView.alpha = 0.6f
                }
                lastConnectedAddress -> {
                    tvStatus.text = itemView.context.getString(R.string.device_last_connected)
                    tvStatus.visibility = View.VISIBLE
                    itemView.isEnabled = true
                    itemView.alpha = 1.0f
                }
                else -> {
                    tvStatus.visibility = View.GONE
                    itemView.isEnabled = true
                    itemView.alpha = 1.0f
                }
            }

            itemView.setOnClickListener {
                if (device.address != connectedAddress) {
                    onDeviceClick(device)
                }
            }
        }
    }
}
```

---

### Task 4: Create DeviceListBottomSheetFragment

**Files:**
- Create: `app/src/main/java/com/haoze/claudekeyboard/ui/device/DeviceListBottomSheetFragment.kt`

- [ ] **Step 1: Create the BottomSheet fragment**

```kotlin
package com.haoze.claudekeyboard.ui.device

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.haoze.claudekeyboard.R

/**
 * BottomSheet dialog showing paired Bluetooth devices for connection.
 */
class DeviceListBottomSheetFragment : BottomSheetDialogFragment() {

    interface DeviceSelectionListener {
        fun onDeviceSelected(device: BluetoothDevice)
    }

    private var listener: DeviceSelectionListener? = null
    private var connectedAddress: String? = null
    private var lastConnectedAddress: String? = null

    companion object {
        private const val ARG_CONNECTED_ADDRESS = "connected_address"
        private const val ARG_LAST_CONNECTED_ADDRESS = "last_connected_address"

        fun newInstance(
            connectedAddress: String?,
            lastConnectedAddress: String?
        ): DeviceListBottomSheetFragment {
            return DeviceListBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONNECTED_ADDRESS, connectedAddress)
                    putString(ARG_LAST_CONNECTED_ADDRESS, lastConnectedAddress)
                }
            }
        }
    }

    fun setDeviceSelectionListener(listener: DeviceSelectionListener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectedAddress = arguments?.getString(ARG_CONNECTED_ADDRESS)
        lastConnectedAddress = arguments?.getString(ARG_LAST_CONNECTED_ADDRESS)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_device_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tv_device_list_title)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_device_empty)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_devices)

        tvTitle.text = getString(R.string.device_list_title)

        val adapter = DeviceAdapter { device ->
            listener?.onDeviceSelected(device)
            dismiss()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bondedDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()

        if (bondedDevices.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.updateDevices(bondedDevices, connectedAddress, lastConnectedAddress)
        }
    }
}
```

---

### Task 5: Create fragment_device_list.xml layout

**Files:**
- Create: `app/src/main/res/layout/fragment_device_list.xml`

- [ ] **Step 1: Create the layout file**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingBottom="16dp">

    <TextView
        android:id="@+id/tv_device_list_title"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="24dp"
        android:layout_marginTop="16dp"
        android:layout_marginBottom="8dp"
        android:text="@string/device_list_title"
        android:textAppearance="?attr/textAppearanceTitleMedium"
        android:textColor="?attr/colorOnSurface" />

    <TextView
        android:id="@+id/tv_device_empty"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:padding="32dp"
        android:text="@string/device_no_paired"
        android:textAppearance="?attr/textAppearanceBodyMedium"
        android:textColor="?attr/colorOnSurfaceVariant"
        android:visibility="gone" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_devices"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:maxHeight="400dp" />

</LinearLayout>
```

---

### Task 6: Add connectToDevice to BluetoothHidService

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/bluetooth/BluetoothHidService.kt`

- [ ] **Step 1: Add connectToDevice method**

Add after the `connectToLastDevice()` method (line 494):

```kotlin
    /**
     * Connect to a specific bonded device by address.
     * @param address Bluetooth MAC address of the target device
     * @return true if the connect call was initiated
     */
    fun connectToDevice(address: String): Boolean {
        val hd = hidDevice ?: return false
        val adapter = bluetoothAdapter ?: return false
        if (!isRegistered) {
            Log.w(TAG, "Cannot connect: HID not registered yet")
            return false
        }

        // Disconnect current device if connected
        if (isConnected) {
            userInitiatedDisconnect = true
            connectedDevice?.let { hd.disconnect(it) }
        }

        val device = adapter.bondedDevices?.find { it.address == address } ?: run {
            Log.w(TAG, "Device ($address) not found in bonded devices")
            return false
        }

        // Update last device for reconnect
        lastConnectedDeviceAddress = address
        lastConnectedDeviceName = device.name
        userInitiatedDisconnect = false
        lastReconnectAttempt = 0
        setDiscoverable()

        Log.d(TAG, "Attempting HID connect to ${device.name} ($address)")
        val result = try {
            hd.connect(device)
        } catch (e: Exception) {
            Log.w(TAG, "HID connect failed: ${e.message}")
            false
        }
        Log.d(TAG, "hd.connect returned $result")
        return result
    }
```

- [ ] **Step 2: Add getLastConnectedDeviceAddress method**

Add after the `getLastConnectedDeviceName()` method (line 473):

```kotlin
    /**
     * Get the address of the last connected device (persisted).
     */
    fun getLastConnectedDeviceAddress(): String? = lastConnectedDeviceAddress
```

---

### Task 7: Update MainActivity to use BottomSheet

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt`

- [ ] **Step 1: Add import for DeviceListBottomSheetFragment**

Add to the imports section:

```kotlin
import com.haoze.claudekeyboard.ui.device.DeviceListBottomSheetFragment
```

- [ ] **Step 2: Update btnDisconnect click handler**

Replace the existing `btnDisconnect.setOnClickListener` block (lines 120-127):

```kotlin
        btnDisconnect.setOnClickListener {
            val service = hidService ?: return@setOnClickListener
            if (service.isConnected()) {
                service.disconnect()
            } else {
                showDeviceListDialog()
            }
        }
```

- [ ] **Step 3: Add showDeviceListDialog method**

Add after the `showSettingsDialog()` method:

```kotlin
    private fun showDeviceListDialog() {
        val service = hidService ?: return
        val dialog = DeviceListBottomSheetFragment.newInstance(
            connectedAddress = if (service.isConnected()) service.getLastConnectedDeviceAddress() else null,
            lastConnectedAddress = service.getLastConnectedDeviceAddress()
        )
        dialog.setDeviceSelectionListener(object : DeviceListBottomSheetFragment.DeviceSelectionListener {
            override fun onDeviceSelected(device: android.bluetooth.BluetoothDevice) {
                Thread {
                    service.connectToDevice(device.address)
                }.start()
            }
        })
        dialog.show(supportFragmentManager, "device_list")
    }
```

---

### Task 8: Build and verify

- [ ] **Step 1: Build debug APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Commit all changes**

```bash
git add -A
git commit -m "feat: add device list BottomSheet for Bluetooth connection selection"
```

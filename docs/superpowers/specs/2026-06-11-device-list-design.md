# Device List BottomSheet Design

## Overview

Replace the single-device connect button with a device list BottomSheet that shows all system paired Bluetooth devices. Users can select a device to connect from the list.

## Behavior

- **Connected state**: Button shows "断开连接" (Disconnect), clicking disconnects directly (unchanged)
- **Disconnected state**: Button shows "连接" (Connect), clicking opens Device List BottomSheet
- **Device List BottomSheet**:
  - Gets devices from `BluetoothAdapter.getBondedDevices()`
  - Shows device name + MAC address for each paired device
  - Last connected device shows "上次连接" (Last connected) marker
  - Click device → connect to selected device
  - No paired devices → show empty state message

## New Files

### DeviceListBottomSheetFragment.kt
- Material3 BottomSheetDialogFragment
- Gets bonded devices from BluetoothAdapter
- DeviceAdapter for RecyclerView
- Callback interface to notify MainActivity for connect/disconnect

### item_device.xml
- RecyclerView list item layout
- Device name (primary text), MAC address (secondary text), status marker

### DeviceAdapter.kt
- RecyclerView.Adapter for device list
- Click handler for device selection

## Modified Files

### BluetoothHidService.kt
- Add `connectToDevice(address: String): Boolean` method
- Keep existing `connectToLastDevice()` for backward compatibility

### MainActivity.kt
- `btnDisconnect` click: when disconnected, open DeviceListBottomSheetFragment
- Handle fragment callbacks for device connection

### strings.xml
- Add: `device_list_title` = "设备列表"
- Add: `device_last_connected` = "上次连接"
- Add: `device_no_paired` = "没有已配对设备"
- Add: `device_connecting` = "正在连接…"

## UI Mockup

```
┌─────────────────────────────┐
│ 设备列表                     │
├─────────────────────────────┤
│ 📱 MacBook Pro              │
│    AA:BB:CC:DD:EE:FF        │
│    上次连接                   │
├─────────────────────────────┤
│ 📱 iPhone 15                │
│    11:22:33:44:55:66        │
├─────────────────────────────┤
│ 📱 Windows PC               │
│    77:88:99:AA:BB:CC        │
└─────────────────────────────┘
```

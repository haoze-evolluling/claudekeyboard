# Claude Code Bluetooth HID Keyboard - Design Spec

## Overview

Android (Kotlin/XML) app that simulates a Bluetooth HID keyboard for Claude Code CLI usage. The phone registers as a standard HID keyboard via Bluetooth, requiring no software on the computer side. Designed for landscape orientation with large touch targets for "blind operation" during Vibe Coding sessions.

**Package:** `com.haoze.claudekeyboard`
**Min SDK:** 28 (Android 9.0)
**Target/Compile SDK:** 36 (Android 16)
**Orientation:** Landscape only

## Architecture

**Approach:** Single Activity + Foreground Service (Option A)

```
┌─────────────────────────────────────────────────────────┐
│                   MainActivity                           │
│  ┌─────────────────────────────────────────────────────┐│
│  │              控制面板 UI (横屏)                       ││
│  │  ┌──────────┐  ┌──────────────────────────────────┐ ││
│  │  │ 核心按钮区 │  │          宏命令区                 │ ││
│  │  │  (大按钮)  │  │  /clear  /exit  /compact  ...    │ ││
│  │  │  Yes/No   │  │  [+ 添加自定义宏]                 │ ││
│  │  │  Ctrl+C   │  │                                  │ ││
│  │  └──────────┘  └──────────────────────────────────┘ ││
│  └─────────────────────────────────────────────────────┘│
│                     ↕ Binder IPC                         │
│  ┌─────────────────────────────────────────────────────┐│
│  │         BluetoothHidService (前台 Service)            ││
│  │  • 注册 HID Device (键盘 SDP)                         ││
│  │  • 管理蓝牙连接生命周期                                ││
│  │  • sendKeyReport() 发送 HID 报告                      ││
│  │  • 通知栏显示连接状态 + 断开按钮                        ││
│  └─────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘

         ↕ Bluetooth HID Protocol
┌─────────────────────┐
│   电脑 / 平板 / TV    │
│  (蓝牙设置中配对即可)  │
└─────────────────────┘
```

### Components

| Component | Responsibility |
|-----------|---------------|
| `MainActivity` | Landscape UI host — core buttons, macro buttons, QWERTY overlay, custom macro management |
| `BluetoothHidService` | Foreground Service — `BluetoothHidDevice` registration, connection management, HID report transmission |
| `MacroRepository` | Macro data layer — preset + user custom macros via SharedPreferences + JSON |
| `KeyboardOverlayFragment` | QWERTY keyboard overlay Fragment, toggled via floating button |

### HID Profile Configuration

- Device Subclass: `0x40` (Keyboard)
- Protocol: `0x01` (Keyboard)
- Standard keyboard HID Report Descriptor (8-byte report: modifier + 6KRO)

## UI Layout (Landscape)

```
┌─────────────────────────────────────────────────────────────────────┐
│  状态栏: 🔋 已连接: MacBook-Pro    [断开]           [⚙ 设置]       │
├─────────────────────────────────┬───────────────────────────────────┤
│                                 │                                   │
│         核 心 按 钮 区            │          宏 命 令 区               │
│                                 │                                   │
│   ┌─────────────┐ ┌───────────┐ │  ┌─────────┐ ┌─────────┐         │
│   │             │ │           │ │  │ /clear  │ │ /exit   │         │
│   │     Yes     │ │  Yes to   │ │  └─────────┘ └─────────┘         │
│   │  (y ↵)      │ │  All      │ │  ┌─────────┐ ┌─────────┐         │
│   │             │ │ (y! ↵)    │ │  │/compact │ │ /cost   │         │
│   └─────────────┘ └───────────┘ │  └─────────┘ └─────────┘         │
│   ┌─────────────┐ ┌───────────┐ │  ┌─────────┐ ┌─────────┐         │
│   │             │ │           │ │  │ /model  │ │ /help   │         │
│   │     No      │ │  Ctrl+C   │ │  └─────────┘ └─────────┘         │
│   │   (n ↵)     │ │  (中断)    │ │  ┌─────────────────────┐         │
│   │             │ │           │ │  │  ＋ 添加自定义宏      │         │
│   └─────────────┘ └───────────┘ │  └─────────────────────┘         │
│                                 │                                   │
├─────────────────────────────────┴───────────────────────────────────┤
│                                           ┌─────┐                   │
│                                           │ ABC │ ← 悬浮QWERTY切换   │
│                                           └─────┘                   │
└─────────────────────────────────────────────────────────────────────┘
```

### Layout Zones

| Zone | Description |
|------|-------------|
| **Top status bar** | Bluetooth connection status, paired device name, disconnect/settings buttons |
| **Core buttons (left 40%)** | 4 large buttons (~2×2 inches each) for blind operation. Yes=green, No=red, Ctrl+C=orange, Yes to All=green variant |
| **Macro buttons (right 60%)** | 2-3 column grid of macro buttons, slightly smaller than core buttons. 6 preset + "Add custom macro" entry |
| **Floating toggle (bottom-right)** | Circular "ABC" button, toggles QWERTY keyboard overlay |

### QWERTY Overlay

```
┌─────────────────────────────────────────────────────────────────────┐
│  [Esc] [1][2][3][4][5][6][7][8][9][0][-][=] [Backspace]           │
│  [Tab] [Q][W][E][R][T][Y][U][I][O][P][ \]                         │
│  [Ctrl][A][S][D][F][G][H][J][K][L][;][']        [Enter]           │
│  [Shift][Z][X][C][V][B][N][M][,][.][/][Shift]                     │
│           [Alt] [━━━━━━━━━ Space ━━━━━━━━━] [Alt]  [↑]            │
│                                              [←][↓][→]    [✕ 关闭] │
└─────────────────────────────────────────────────────────────────────┘
```

- Includes Ctrl, Alt, Shift modifiers (supports combos like Ctrl+C/V/Z)
- Arrow keys, Esc, Tab, Backspace, Enter
- Close button returns to large button mode

## Bluetooth HID Implementation

### HID Report Descriptor

Standard keyboard descriptor supporting:
- 8-bit modifier bitmap (Left/Right Ctrl, Shift, Alt, Win)
- Reserved byte
- 6KRO key slots
- LED output (Num/Caps/Scroll Lock)

### HID Report Format (8 bytes)

| Byte | Content |
|------|---------|
| 0 | Modifier bitmap (Ctrl_L/Shift_L/Alt_L/Win_L/Ctrl_R/Shift_R/Alt_R/Win_R) |
| 1 | Reserved (0x00) |
| 2-7 | Key codes (up to 6 simultaneous) |

### Send Flow

```
User taps button
    ↓
Build HID Report: [modifier, 0x00, key1, key2, ..., 0x00]
    ↓
BluetoothHidDevice.sendReport(device, reportId, report)
    ↓
Release: send all-zero report [0x00 × 8]
    ↓
Wait ~20ms → send next key (simulates human typing pace)
```

### Macro Execution Examples

| Macro | Send Sequence |
|-------|--------------|
| Yes (`y` + Enter) | Send `y` → release → send `Enter` → release |
| Yes to All (`y!` + Enter) | Send `Shift` + `y` → release → send `Shift` + `1` (!) → release → send `Enter` → release |
| No (`n` + Enter) | Send `n` → release → send `Enter` → release |
| Ctrl+C | Send `Ctrl_L` modifier + `c` key → release |
| /clear | Send `/` → `c` → `l` → `e` → `a` → `r` → `Enter`, each key ~30ms apart |

### Connection Lifecycle

```
Service onCreate()
    ↓
BluetoothAdapter.getProfileProxy() → BluetoothHidDevice
    ↓
registerApp(SDP Record, Callback) → Register as HID device
    ↓
onAppRegistered() → Notification: "Waiting for pairing"
    ↓
Computer Bluetooth scan → Pair → onConnectionStateChanged(CONNECTED)
    ↓
Notification: "Connected: [device name]"
    ↓
User disconnects / Bluetooth off / Remote disconnects → onConnectionStateChanged(DISCONNECTED)
    ↓
Auto-return to "Waiting for pairing" state
```

## Macro Storage

### Data Model

```kotlin
data class Macro(
    val id: String,          // UUID
    val label: String,       // Button display text, e.g. "/clear"
    val command: String,     // Actual content sent, e.g. "/clear"
    val isPreset: Boolean,   // Preset macro (not deletable)
    val sortOrder: Int       // Display order
)
```

### Preset Macros

| Label | Content | Description |
|-------|---------|-------------|
| `/clear` | `/clear\n` | Clear screen |
| `/exit` | `/exit\n` | Exit Claude Code |
| `/compact` | `/compact\n` | Compress context |
| `/cost` | `/cost\n` | View token usage |
| `/model` | `/model\n` | Switch model |
| `/help` | `/help\n` | Help info |

### Storage: SharedPreferences + JSON

Rationale: Small data volume (6 preset + user customs), no need for Room.

```
SharedPreferences("macro_prefs")
    └── "macros" → JSON Array of Macro objects
```

- First launch: write preset macro list
- User adds custom macro: append to list, `isPreset = false`
- User edits/deletes custom macro: only `isPreset = false` items
- Presets fixed at top, customs appended after

### Custom Macro UI

Click "+ Add custom macro" → Material Dialog:
- **Label** (`EditText`): button display text
- **Content** (`EditText`): actual text to send
- Save → persist → refresh button grid

Custom macro buttons support **long-press** → edit/delete menu.

## Permissions

| Permission | Purpose | Runtime Request |
|-----------|---------|----------------|
| `BLUETOOTH` | Basic BT ops | No (install-time on API 28) |
| `BLUETOOTH_ADMIN` | BT management | No (install-time on API 28) |
| `BLUETOOTH_CONNECT` | API 31+ BT connect | Yes (API 31+ only) |
| `ACCESS_FINE_LOCATION` | API 28-30 BT scan | Yes (API 28-30 only) |
| `FOREGROUND_SERVICE` | Foreground Service | No |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | API 34+ service type | No |

### Foreground Service Type (API 34+)

```xml
<service
    android:name=".BluetoothHidService"
    android:foregroundServiceType="connectedDevice"
    android:exported="false" />
```

## Error Handling

| Scenario | Handling |
|----------|----------|
| Device lacks BLE/HID support | Startup check → Dialog "This device does not support Bluetooth HID", disable all buttons |
| Bluetooth off | Status bar "Bluetooth is off", tap → system BT enable dialog |
| Permission denied | Guide user to system settings |
| HID registration failed | Notification error, button area "Registration failed, tap to retry" |
| Remote device disconnects | Status bar → "Disconnected", auto-enter reconnect waiting state |
| Send report failure | Toast "Send failed", does not block subsequent operations |

### Version Compatibility

```
API 28-30: Legacy BLUETOOTH/BLUETOOTH_ADMIN + LOCATION permissions
API 31+:   New BLUETOOTH_CONNECT permission model
API 34+:   Must declare foregroundServiceType="connectedDevice"
```

A `BluetoothPermissionHelper` utility class encapsulates version-specific permission logic.

## Project Structure

```
app/src/main/java/com/haoze/claudekeyboard/
├── MainActivity.kt                  // Main control panel Activity
├── bluetooth/
│   ├── BluetoothHidService.kt       // Foreground Service, HID registration & connection
│   ├── HidReportHelper.kt           // HID report builder (keycode → 8-byte report)
│   └── BluetoothPermissionHelper.kt // Multi-version permission handling
├── macro/
│   ├── Macro.kt                     // Data model
│   ├── MacroRepository.kt           // Storage (preset + custom macro management)
│   └── PresetMacros.kt              // Preset macro constants
├── ui/
│   ├── keyboard/
│   │   ├── KeyboardOverlayFragment.kt  // QWERTY overlay Fragment
│   │   └── KeyboardOverlayViewModel.kt // Overlay state management
│   ├── macro/
│   │   ├── MacroButtonAdapter.kt       // Macro button RecyclerView Adapter
│   │   └── MacroEditDialogFragment.kt  // Custom macro edit dialog
│   └── theme/
│       └── ThemeExtensions.kt          // Button color/style helpers
└── util/
    └── Constants.kt                   // Global constants (HID keycodes, delays, etc.)

app/src/main/res/
├── layout/
│   ├── activity_main.xml              // Main landscape layout
│   ├── fragment_keyboard_overlay.xml  // QWERTY keyboard layout
│   ├── item_macro_button.xml          // Macro button item layout
│   └── dialog_macro_edit.xml          // Macro edit dialog layout
├── drawable/
│   ├── btn_yes.xml                    // Yes button background (green rounded)
│   ├── btn_no.xml                     // No button background (red rounded)
│   ├── btn_ctrl_c.xml                 // Ctrl+C button background (orange)
│   ├── btn_macro.xml                  // Macro button background
│   └── btn_floating.xml              // Floating toggle button background
├── values/
│   ├── colors.xml                     // Color definitions
│   ├── strings.xml                    // String resources
│   └── dimens.xml                     // Dimension definitions
└── values-land/
    └── dimens.xml                     // Landscape-specific dimension tweaks
```

### Landscape-Only Configuration

```xml
<activity
    android:name=".MainActivity"
    android:screenOrientation="landscape"
    android:configChanges="orientation|screenSize">
```

### Dependencies

No additional third-party dependencies. All implementation uses AndroidX + Material + `android.bluetooth` system API only.

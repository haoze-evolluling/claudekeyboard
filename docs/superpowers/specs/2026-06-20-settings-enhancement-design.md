# Settings Page Enhancement Design

**Date:** 2026-06-20
**Status:** Approved

## Overview

Enhance SyncTouch settings page by adding 6 new settings and refactoring the UI from a flat `LinearLayout` to a `RecyclerView`-based grouped list for better maintainability and extensibility.

## Current State

- Settings page: `content_settings.xml` (315 lines, `LinearLayout`)
- Settings logic: `MainActivity.setupSettingsPage()` (lines 396-468)
- 4 sections: Connection, Data Management, Touchpad, Appearance
- 5 settings: auto-connect, auto-reconnect, sensitivity, reset macros, theme mode

## Target State

- 6 sections, 13 settings items
- `RecyclerView` with `SettingsAdapter` driven by `SettingsItem` sealed class
- All existing settings preserved with same behavior

## Section Breakdown

### 1. Connection (连接)
| Setting | Type | Key | Default | New? |
|---|---|---|---|---|
| Auto-connect on launch | Switch | `auto_connect_on_launch` | `true` | No |
| Auto-reconnect on disconnect | Switch | `auto_reconnect_on_disconnect` | `true` | No |
| Keep screen on | Switch | `keep_screen_on` | `true` | Yes |
| Connection notifications | Switch | `connection_notifications` | `true` | Yes |

### 2. Touchpad (触控板)
| Setting | Type | Key | Default | New? |
|---|---|---|---|---|
| Default sensitivity | Slider (1-10) | `touchpad_sensitivity` | `5` | No |
| Cursor speed | Slider (1-10) | `cursor_speed` | `5` | Yes |
| Natural scrolling | Switch | `scroll_direction_natural` | `false` | Yes |

### 3. Interaction (交互)
| Setting | Type | Key | Default | New? |
|---|---|---|---|---|
| Haptic feedback | Switch | `haptic_feedback` | `true` | Yes |

### 4. Data Management (数据管理)
| Setting | Type | Key | Default | New? |
|---|---|---|---|---|
| Reset macros | Button | N/A | N/A | No |

### 5. Appearance (外观)
| Setting | Type | Key | Default | New? |
|---|---|---|---|---|
| Theme mode | ToggleGroup | `theme_mode` | `"MODE_SYSTEM"` | No |

### 6. About (关于)
| Setting | Type | Key | Default | New? |
|---|---|---|---|---|
| Version info | Info display | N/A | N/A | Yes |
| App name | Info display | N/A | N/A | Yes |
| Open source license | Clickable | N/A | N/A | Yes |

## Data Model

```kotlin
sealed class SettingsItem {
    data class SectionHeader(val title: String) : SettingsItem()
    data class SwitchItem(
        val key: String,
        val title: String,
        val subtitle: String? = null,
        val defaultValue: Boolean
    ) : SettingsItem()
    data class SliderItem(
        val key: String,
        val title: String,
        val subtitle: String? = null,
        val defaultValue: Int,
        val min: Float,
        val max: Float,
        val stepSize: Float
    ) : SettingsItem()
    data class ButtonItem(
        val title: String,
        val subtitle: String? = null,
        val onClick: () -> Unit
    ) : SettingsItem()
    data class ToggleGroupItem(
        val key: String,
        val title: String,
        val options: List<String>,
        val defaultIndex: Int
    ) : SettingsItem()
    data class InfoItem(
        val label: String,
        val value: String,
        val onClick: (() -> Unit)? = null
    ) : SettingsItem()
}
```

## File Changes

### New Files
- `app/src/main/java/com/haoze/claudekeyboard/ui/settings/SettingsItem.kt` — Data model
- `app/src/main/java/com/haoze/claudekeyboard/ui/settings/SettingsAdapter.kt` — RecyclerView adapter with ViewHolder types for each item type

### Modified Files
- `content_settings.xml` — Replace `ScrollView`/`LinearLayout` with `RecyclerView`
- `MainActivity.kt` — Rewrite `setupSettingsPage()` to build item list and bind adapter
- `strings.xml` — Add new setting labels and descriptions
- `BluetoothHidService.kt` — Add keep-screen-on logic and connection notification logic
- `TouchpadFragment.kt` — Read `cursor_speed` and `scroll_direction_natural` from prefs
- `HapticHelper.kt` — Check `haptic_feedback` pref before vibrating

## Behavior Details

### Keep Screen On
When enabled and a BLE device is connected, `FLAG_KEEP_SCREEN_ON` is added to the window. Removed on disconnect. Read from `settings_prefs` on each connect/disconnect event.

### Connection Notifications
When enabled, `BluetoothHidService` shows a notification on device connect/disconnect. The foreground service notification already exists; this adds transient status notifications.

### Cursor Speed
Multiplier applied to mouse movement deltas in `TouchpadFragment`. Stored as integer 1-10, mapped to multiplier 0.2x-2.0x.

### Natural Scrolling
When enabled, scroll delta values are inverted (negated) in `TouchpadFragment`.

### Haptic Feedback
`HapticHelper.kt` extension functions check `settings_prefs` before performing haptic feedback. If disabled, all haptic calls are silently skipped.

## Testing

- Manual verification on Android device
- Verify each new setting persists across app restarts
- Verify RecyclerView scrolls correctly
- Verify existing settings still work as before
- Verify theme changes apply immediately
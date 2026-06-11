# Haptic Feedback Design

**Date:** 2026-06-11
**Status:** Draft

## Goal

Add crisp, short haptic feedback ("哒") to all tappable areas in the app — keyboard keys, action buttons, macro buttons, device list items, and other interactive elements. The feedback should feel like tapping a physical keyboard key.

## Approach

Use `View.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)` via a Kotlin inline extension function. No `VIBRATE` permission needed. Respects the user's system-level haptic settings.

- **Feedback type:** `HapticFeedbackConstants.KEYBOARD_TAP` — crisp, short click
- **Always on** — no settings toggle
- **Every press** — including long-press repeat on keyboard keys

## Implementation

### 1. New file: `util/HapticHelper.kt`

```kotlin
package com.haoze.claudekeyboard.util

import android.view.HapticFeedbackConstants
import android.view.View

inline fun View.performKeyClick() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}
```

Single inline extension function. Zero runtime overhead.

### 2. `ui/keyboard/KeyboardFragment.kt`

**Modifier keys** (`ACTION_DOWN` in the MODIFIER touch listener, ~line 228):
- Add `v.performKeyClick()` right after `button.setBackgroundResource(R.drawable.bg_key_pressed)`

**Normal/Special keys** (`ACTION_DOWN` in the NORMAL/SPECIAL touch listener, ~line 252):
- Add `v.performKeyClick()` right after `button.setBackgroundResource(R.drawable.bg_key_pressed)`
- This fires on every press, including long-press repeats (the repeat runnable re-enters `ACTION_DOWN` logic via the timer, but the haptic is only in `ACTION_DOWN` which only fires once per physical press — to get haptic on repeats, add `v.performKeyClick()` inside the `repeatRunnable.run()` block before `sender.sendKeyPress()`)

**Back-to-Claude button** (~line 195):
- Add `it.performKeyClick()` inside the `setOnClickListener` lambda

### 3. `MainActivity.kt`

**Core buttons** (`setupCoreButtons()`, ~line 261):
- Add `it.performKeyClick()` as the first line in each of the 6 `setOnClickListener` lambdas (btnYes, btnNo, btnCtrlC, btnYesToAll, btnBackspace, btnEnter)

**Settings button** (`initViews()`, ~line 176):
- Add `it.performKeyClick()` in `btnSettings.setOnClickListener`

**Device action text** (`initViews()`, ~line 168):
- Add `it.performKeyClick()` in `tvDeviceAction.setOnClickListener`

**Text input send button** (`setupTextInput()`, ~line 314):
- Add `view.performKeyClick()` in `inputLayout.setEndIconOnClickListener`

**Bottom navigation** (`setupBottomNavigation()`, ~line 184):
- Add haptic in `setOnItemSelectedListener` callback: `bottomNav.performKeyClick()`

### 4. `ui/macro/MacroButtonAdapter.kt`

**Macro click** (`bind()`, ~line 38):
- Add `it.performKeyClick()` in `button.setOnClickListener`

**Macro long-click** (`bind()`, ~line 42):
- Add `it.performKeyClick()` in `button.setOnLongClickListener`

### 5. `ui/device/DeviceAdapter.kt`

**Device item click** (`bind()`, ~line 103):
- Add `itemView.performKeyClick()` in `itemView.setOnClickListener`

## Files Changed

| File | Action |
|------|--------|
| `util/HapticHelper.kt` | **New** — extension function |
| `ui/keyboard/KeyboardFragment.kt` | Add haptic to 3 touch listener branches + back button |
| `MainActivity.kt` | Add haptic to 6 core buttons + 3 other clickables |
| `ui/macro/MacroButtonAdapter.kt` | Add haptic to click + long-click |
| `ui/device/DeviceAdapter.kt` | Add haptic to device item click |

## Out of Scope

- Dialog buttons (MaterialAlertDialogBuilder manages their lifecycle; adding haptic there would require custom button views or post-show listeners — not worth the complexity for rarely-used dialogs)
- Vibration permission — not needed for `KEYBOARD_TAP`
- Settings toggle — always on per user preference

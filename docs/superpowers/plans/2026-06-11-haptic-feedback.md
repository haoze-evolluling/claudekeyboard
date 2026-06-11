# Haptic Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add crisp "哒" haptic feedback to all tappable areas in the app using `HapticFeedbackConstants.KEYBOARD_TAP`.

**Architecture:** A single inline extension function `View.performKeyClick()` in a new `HapticHelper.kt` utility file. All existing click/touch handlers get one added line calling this function. No new permissions, no settings toggle.

**Tech Stack:** Kotlin, Android SDK (minSdk 28), `View.performHapticFeedback()` API

---

### Task 1: Create HapticHelper extension function

**Files:**
- Create: `app/src/main/java/com/haoze/claudekeyboard/util/HapticHelper.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.haoze.claudekeyboard.util

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Trigger crisp haptic feedback on a clickable view.
 * Uses KEYBOARD_TAP for a short, tactile "click" feel.
 * No VIBRATE permission required — respects system haptic settings.
 */
inline fun View.performKeyClick() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}
```

- [ ] **Step 2: Verify build compiles**

Run: `cd C:/Users/leeha/claudekeyboard && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
cd C:/Users/leeha/claudekeyboard
git add app/src/main/java/com/haoze/claudekeyboard/util/HapticHelper.kt
git commit -m "feat: add View.performKeyClick() haptic extension function"
```

---

### Task 2: Add haptic to KeyboardFragment keys

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/ui/keyboard/KeyboardFragment.kt`
  - Line 228-229: Modifier key `ACTION_DOWN`
  - Line 252-254: Normal/Special key `ACTION_DOWN`
  - Line 272-278: Long-press repeat `run()` block
  - Line 195: Back-to-Claude button `setOnClickListener`

- [ ] **Step 1: Add import at top of file (after existing imports)**

```kotlin
import com.haoze.claudekeyboard.util.performKeyClick
```

- [ ] **Step 2: Add haptic to modifier key ACTION_DOWN**

In the MODIFIER touch listener, `ACTION_DOWN` branch. Current code:
```kotlin
MotionEvent.ACTION_DOWN -> {
    button.setBackgroundResource(R.drawable.bg_key_pressed)
    v.isPressed = true
    true
}
```

Change to:
```kotlin
MotionEvent.ACTION_DOWN -> {
    v.performKeyClick()
    button.setBackgroundResource(R.drawable.bg_key_pressed)
    v.isPressed = true
    true
}
```

- [ ] **Step 3: Add haptic to normal/special key ACTION_DOWN**

In the NORMAL/SPECIAL touch listener, `ACTION_DOWN` branch. Current code:
```kotlin
MotionEvent.ACTION_DOWN -> {
    isKeyPressed = true
    button.setBackgroundResource(R.drawable.bg_key_pressed)
    v.isPressed = true
```

Change to:
```kotlin
MotionEvent.ACTION_DOWN -> {
    v.performKeyClick()
    isKeyPressed = true
    button.setBackgroundResource(R.drawable.bg_key_pressed)
    v.isPressed = true
```

- [ ] **Step 4: Add haptic to long-press repeat**

Inside the `repeatRunnable` `run()` block. Current code:
```kotlin
repeatRunnable = object : Runnable {
    override fun run() {
        if (!isKeyPressed) return
        Thread {
            sender.sendKeyPress(combinedModifier, keyData.hidKeyCode)
        }.start()
        handler.postDelayed(this, 50)
    }
}
```

Change to:
```kotlin
repeatRunnable = object : Runnable {
    override fun run() {
        if (!isKeyPressed) return
        v.performKeyClick()
        Thread {
            sender.sendKeyPress(combinedModifier, keyData.hidKeyCode)
        }.start()
        handler.postDelayed(this, 50)
    }
}
```

- [ ] **Step 5: Add haptic to back-to-Claude button**

Current code (~line 195):
```kotlin
setOnClickListener { (activity as? MainActivity)?.switchToClaudeTab() }
```

Change to:
```kotlin
setOnClickListener {
    it.performKeyClick()
    (activity as? MainActivity)?.switchToClaudeTab()
}
```

- [ ] **Step 6: Verify build compiles**

Run: `cd C:/Users/leeha/claudekeyboard && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
cd C:/Users/leeha/claudekeyboard
git add app/src/main/java/com/haoze/claudekeyboard/ui/keyboard/KeyboardFragment.kt
git commit -m "feat: add haptic feedback to keyboard keys and back button"
```

---

### Task 3: Add haptic to MainActivity buttons

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt`
  - Line 168-175: `tvDeviceAction.setOnClickListener`
  - Line 176: `btnSettings.setOnClickListener`
  - Line 184: `setOnItemSelectedListener`
  - Line 261-280: `setupCoreButtons()` — 6 button click listeners
  - Line 314: `inputLayout.setEndIconOnClickListener`

- [ ] **Step 1: Add import at top of file (after existing imports)**

```kotlin
import com.haoze.claudekeyboard.util.performKeyClick
```

- [ ] **Step 2: Add haptic to tvDeviceAction click**

Current code:
```kotlin
tvDeviceAction.setOnClickListener {
    val service = hidService
    if (service != null && service.isConnected()) {
        service.disconnect()
    } else {
        showDeviceListDialog()
    }
}
```

Change to:
```kotlin
tvDeviceAction.setOnClickListener {
    it.performKeyClick()
    val service = hidService
    if (service != null && service.isConnected()) {
        service.disconnect()
    } else {
        showDeviceListDialog()
    }
}
```

- [ ] **Step 3: Add haptic to settings button click**

Current code:
```kotlin
btnSettings.setOnClickListener { showSettingsDialog() }
```

Change to:
```kotlin
btnSettings.setOnClickListener {
    it.performKeyClick()
    showSettingsDialog()
}
```

- [ ] **Step 4: Add haptic to bottom navigation**

Current code:
```kotlin
bottomNav.setOnItemSelectedListener { item ->
    when (item.itemId) {
```

Change to:
```kotlin
bottomNav.setOnItemSelectedListener { item ->
    bottomNav.performKeyClick()
    when (item.itemId) {
```

- [ ] **Step 5: Add haptic to all 6 core buttons**

Current code:
```kotlin
private fun setupCoreButtons() {
    btnYes.setOnClickListener {
        hidService?.getKeyboardSender()?.let { s -> Thread { s.sendText("y") }.start() }
    }
    btnNo.setOnClickListener {
        hidService?.getKeyboardSender()?.let { s -> Thread { s.sendText("n") }.start() }
    }
    btnCtrlC.setOnClickListener {
        hidService?.getKeyboardSender()?.let { s -> Thread { s.sendKeyPress(KeyboardSender.MODIFIER_CTRL_LEFT, KeyboardSender.KEY_C) }.start() }
    }
    btnYesToAll.setOnClickListener {
        hidService?.getKeyboardSender()?.let { s -> Thread { s.sendText("a") }.start() }
    }
    btnBackspace.setOnClickListener {
        hidService?.getKeyboardSender()?.let { s -> Thread { s.sendKeyPress(0x00, KeyboardSender.KEY_BACKSPACE) }.start() }
    }
    btnEnter.setOnClickListener {
        hidService?.getKeyboardSender()?.let { s -> Thread { s.sendKeyPress(0x00, KeyboardSender.KEY_ENTER) }.start() }
    }
}
```

Change to:
```kotlin
private fun setupCoreButtons() {
    btnYes.setOnClickListener {
        it.performKeyClick()
        hidService?.getKeyboardSender()?.let { s -> Thread { s.sendText("y") }.start() }
    }
    btnNo.setOnClickListener {
        it.performKeyClick()
        hidService?.getKeyboardSender()?.let { s -> Thread { s.sendText("n") }.start() }
    }
    btnCtrlC.setOnClickListener {
        it.performKeyClick()
        hidService?.getKeyboardSender()?.let { s -> Thread { s.sendKeyPress(KeyboardSender.MODIFIER_CTRL_LEFT, KeyboardSender.KEY_C) }.start() }
    }
    btnYesToAll.setOnClickListener {
        it.performKeyClick()
        hidService?.getKeyboardSender()?.let { s -> Thread { s.sendText("a") }.start() }
    }
    btnBackspace.setOnClickListener {
        it.performKeyClick()
        hidService?.getKeyboardSender()?.let { s -> Thread { s.sendKeyPress(0x00, KeyboardSender.KEY_BACKSPACE) }.start() }
    }
    btnEnter.setOnClickListener {
        it.performKeyClick()
        hidService?.getKeyboardSender()?.let { s -> Thread { s.sendKeyPress(0x00, KeyboardSender.KEY_ENTER) }.start() }
    }
}
```

- [ ] **Step 6: Add haptic to text input send button**

Current code:
```kotlin
inputLayout.setEndIconOnClickListener { sendInputText() }
```

Change to:
```kotlin
inputLayout.setEndIconOnClickListener {
    it.performKeyClick()
    sendInputText()
}
```

- [ ] **Step 7: Verify build compiles**

Run: `cd C:/Users/leeha/claudekeyboard && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
cd C:/Users/leeha/claudekeyboard
git add app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt
git commit -m "feat: add haptic feedback to all MainActivity buttons"
```

---

### Task 4: Add haptic to MacroButtonAdapter

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/ui/macro/MacroButtonAdapter.kt`
  - Line 38-39: `button.setOnClickListener`
  - Line 42-44: `button.setOnLongClickListener`

- [ ] **Step 1: Add import at top of file (after existing imports)**

```kotlin
import com.haoze.claudekeyboard.util.performKeyClick
```

- [ ] **Step 2: Add haptic to macro click and long-click**

Current code:
```kotlin
fun bind(macro: Macro) {
    button.text = macro.label

    button.setOnClickListener {
        onMacroClick(macro)
    }

    button.setOnLongClickListener {
        onMacroLongClick(macro)
        true
    }
}
```

Change to:
```kotlin
fun bind(macro: Macro) {
    button.text = macro.label

    button.setOnClickListener {
        it.performKeyClick()
        onMacroClick(macro)
    }

    button.setOnLongClickListener {
        it.performKeyClick()
        onMacroLongClick(macro)
        true
    }
}
```

- [ ] **Step 3: Verify build compiles**

Run: `cd C:/Users/leeha/claudekeyboard && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
cd C:/Users/leeha/claudekeyboard
git add app/src/main/java/com/haoze/claudekeyboard/ui/macro/MacroButtonAdapter.kt
git commit -m "feat: add haptic feedback to macro button clicks"
```

---

### Task 5: Add haptic to DeviceAdapter

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/ui/device/DeviceAdapter.kt`
  - Line 103-106: `itemView.setOnClickListener`

- [ ] **Step 1: Add import at top of file (after existing imports)**

```kotlin
import com.haoze.claudekeyboard.util.performKeyClick
```

- [ ] **Step 2: Add haptic to device item click**

Current code:
```kotlin
itemView.setOnClickListener {
    if (device.address != connectedAddress && connectingAddress == null) {
        onDeviceClick(device)
    }
}
```

Change to:
```kotlin
itemView.setOnClickListener {
    if (device.address != connectedAddress && connectingAddress == null) {
        it.performKeyClick()
        onDeviceClick(device)
    }
}
```

- [ ] **Step 3: Verify build compiles**

Run: `cd C:/Users/leeha/claudekeyboard && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
cd C:/Users/leeha/claudekeyboard
git add app/src/main/java/com/haoze/claudekeyboard/ui/device/DeviceAdapter.kt
git commit -m "feat: add haptic feedback to device list item clicks"
```

---

### Task 6: Final verification and cleanup

- [ ] **Step 1: Full build verification**

Run: `cd C:/Users/leeha/claudekeyboard && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify all haptic call sites with grep**

Run: `grep -rn "performKeyClick" C:/Users/leeha/claudekeyboard/app/src/main/java/`
Expected: 15+ matches across 5 files (HapticHelper.kt definition + KeyboardFragment + MainActivity + MacroButtonAdapter + DeviceAdapter)

- [ ] **Step 3: Final commit with all files (if any uncommitted)**

```bash
cd C:/Users/leeha/claudekeyboard
git status
# If clean, no commit needed. If any files pending:
git add -A
git commit -m "feat: haptic feedback on all tappable areas"
```

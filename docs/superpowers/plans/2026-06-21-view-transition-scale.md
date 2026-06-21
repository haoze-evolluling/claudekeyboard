# View Transition Scale Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add scale+fade animation to view transitions in SyncTouch, skipping animation when orientation changes.

**Architecture:** Modify the existing `showOnly()` method in `MainActivity.kt` to accept an `animate` parameter. When animated, use `ViewPropertyAnimator` to scale+fade the outgoing and incoming views. `navigateToPage()` and `navigateToHome()` detect orientation changes and pass `animate = false` when the target orientation differs from the current one.

**Tech Stack:** Kotlin, Android ViewPropertyAnimator, no new dependencies

## Global Constraints

- No new files; single file modified: `MainActivity.kt`
- No XML animation resources
- Duration: 300ms, `AccelerateDecelerateInterpolator`
- Scale: 0.8 ↔ 1.0, Alpha: 0.0 ↔ 1.0
- Animation skipped when orientation changes (portrait ↔ landscape)

---

### Task 1: Add scale animation to `showOnly()` and wire orientation-aware skip

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt`

**Interfaces:**
- Produces: `private fun showOnly(target: View, animate: Boolean = true)` — new `animate` parameter
- Modifies: `fun navigateToPage(targetContent: View, landscape: Boolean)` — detects orientation change
- Modifies: `fun navigateToHome()` — detects orientation change

- [ ] **Step 1: Add new imports**

Add at line 4 (after `import android.content.pm.ActivityInfo`):

```kotlin
import android.content.res.Configuration
```

Add at line 41 (after `import android.animation.ObjectAnimator`):

```kotlin
import android.view.animation.AccelerateDecelerateInterpolator
```

- [ ] **Step 2: Replace `showOnly()` with animated version**

Replace lines 294-296:

```kotlin
    private fun showOnly(target: View) {
        allContentViews.forEach { it.visibility = if (it == target) View.VISIBLE else View.GONE }
    }
```

With:

```kotlin
    private fun showOnly(target: View, animate: Boolean = true) {
        val oldView = allContentViews.firstOrNull { it.visibility == View.VISIBLE }
        if (oldView == target || !animate) {
            allContentViews.forEach { it.visibility = if (it == target) View.VISIBLE else View.GONE }
            return
        }

        oldView?.animate()
            ?.scaleX(0.8f)?.scaleY(0.8f)?.alpha(0f)
            ?.setDuration(300)
            ?.setInterpolator(AccelerateDecelerateInterpolator())
            ?.withEndAction { oldView.visibility = View.GONE }
            ?.start()

        target.apply {
            visibility = View.VISIBLE
            scaleX = 0.8f
            scaleY = 0.8f
            alpha = 0f
            animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        allContentViews.forEach { if (it != target && it != oldView) it.visibility = View.GONE }
    }
```

- [ ] **Step 3: Update `navigateToPage()` to detect orientation change**

Replace lines 298-318:

```kotlin
    fun navigateToPage(targetContent: View, landscape: Boolean) {
        showOnly(targetContent)

        requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        if (targetContent == contentKeyboard) {
            keyboardFragment = supportFragmentManager.findFragmentById(R.id.keyboard_fragment_container) as? KeyboardFragment
            updateFragmentEnabledStates()
        } else if (targetContent == contentTouchpad) {
            touchpadFragment = supportFragmentManager.findFragmentById(R.id.touchpad_fragment_container) as? TouchpadFragment
            touchpadFragment?.reloadSettings()
            updateFragmentEnabledStates()
        } else if (targetContent == contentTvRemote) {
            tvRemoteFragment = supportFragmentManager.findFragmentById(R.id.tvremote_fragment_container) as? TvRemoteFragment
            updateFragmentEnabledStates()
        }
    }
```

With:

```kotlin
    fun navigateToPage(targetContent: View, landscape: Boolean) {
        val currentOrientation = resources.configuration.orientation
        val targetOrientation = if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        val sameOrientation = currentOrientation == targetOrientation

        showOnly(targetContent, animate = sameOrientation)

        requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        if (targetContent == contentKeyboard) {
            keyboardFragment = supportFragmentManager.findFragmentById(R.id.keyboard_fragment_container) as? KeyboardFragment
            updateFragmentEnabledStates()
        } else if (targetContent == contentTouchpad) {
            touchpadFragment = supportFragmentManager.findFragmentById(R.id.touchpad_fragment_container) as? TouchpadFragment
            touchpadFragment?.reloadSettings()
            updateFragmentEnabledStates()
        } else if (targetContent == contentTvRemote) {
            tvRemoteFragment = supportFragmentManager.findFragmentById(R.id.tvremote_fragment_container) as? TvRemoteFragment
            updateFragmentEnabledStates()
        }
    }
```

- [ ] **Step 4: Update `navigateToHome()` to detect orientation change**

Replace lines 320-325:

```kotlin
    fun navigateToHome() {
        showOnly(contentHome)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        updateDeviceSubtitle()
        updateKeepScreenOn(bluetoothViewModel.isConnected())
    }
```

With:

```kotlin
    fun navigateToHome() {
        val currentOrientation = resources.configuration.orientation
        val sameOrientation = currentOrientation == Configuration.ORIENTATION_PORTRAIT

        showOnly(contentHome, animate = sameOrientation)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        updateDeviceSubtitle()
        updateKeepScreenOn(bluetoothViewModel.isConnected())
    }
```

- [ ] **Step 5: Build and verify compilation**

Run: `.\gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt
git commit -m "feat: add scale animation to view transitions"
```
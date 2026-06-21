# View Transition Animation: Scale

**Date:** 2026-06-21
**Status:** Design

## Context

SyncTouch is a single-Activity Android app with 6 content views stacked in a `FrameLayout`. Switching between views is done via `showOnly()` in `MainActivity.kt`, which toggles `View.VISIBLE` / `View.GONE` instantly with no animation.

## Goal

Add a scale animation to view transitions, providing visual feedback when switching between pages.

## Requirements

- **Animation style:** Scale — incoming view scales up (0.8 → 1.0), outgoing view scales down (1.0 → 0.8), combined with alpha fade
- **Duration:** 300ms, `AccelerateDecelerateInterpolator`
- **Scope:** All view transitions **except** when the target orientation differs from the current orientation (portrait ↔ landscape switches skip animation)

## Design

### File changes

Single file: `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt`

### Core logic

```kotlin
private fun showOnly(target: View, animate: Boolean = true) {
    val oldView = allContentViews.firstOrNull { it.visibility == View.VISIBLE }
    if (oldView == target || !animate) {
        allContentViews.forEach { it.visibility = if (it == target) View.VISIBLE else View.GONE }
        return
    }

    // Outgoing: scale down + fade out
    oldView?.animate()
        ?.scaleX(0.8f)?.scaleY(0.8f)?.alpha(0f)
        ?.setDuration(300)
        ?.setInterpolator(AccelerateDecelerateInterpolator())
        ?.withEndAction { oldView.visibility = View.GONE }
        ?.start()

    // Incoming: scale up + fade in
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

    // Hide other views
    allContentViews.forEach { if (it != target && it != oldView) it.visibility = View.GONE }
}
```

### Call-site changes

In `navigateToPage()` and `navigateToHome()`, determine whether orientation changes. If the requested orientation differs from the current orientation, pass `animate = false`.

```kotlin
fun navigateToPage(targetContent: View, landscape: Boolean) {
    val currentOrientation = resources.configuration.orientation
    val targetOrientation = if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
    val sameOrientation = currentOrientation == targetOrientation

    showOnly(targetContent, animate = sameOrientation)
    requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    // ... fragment setup (unchanged)
}

fun navigateToHome() {
    val currentOrientation = resources.configuration.orientation
    val sameOrientation = currentOrientation == Configuration.ORIENTATION_PORTRAIT

    showOnly(contentHome, animate = sameOrientation)
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    // ... (unchanged)
}
```

### Animation parameters

| Property | From | To |
|---|---|---|
| scaleX/scaleY (outgoing) | 1.0 | 0.8 |
| scaleX/scaleY (incoming) | 0.8 | 1.0 |
| alpha (outgoing) | 1.0 | 0.0 |
| alpha (incoming) | 0.0 | 1.0 |
| duration | 300ms | — |
| interpolator | AccelerateDecelerate | — |

## Edge cases

- **Same view targeted:** `oldView == target` → no animation, skip
- **Orientation change:** `animate = false` → instant switch, avoid animation fighting with system rotation
- **Rapid double-tap:** `ViewPropertyAnimator` handles this naturally — animating a second time on the same view cancels the previous animation
- **Other views:** immediately set to `GONE` without animation, preventing flicker

## Non-goals

- No XML animation resources
- No new files
- No fragment transition customization
- No configurable animation type or duration
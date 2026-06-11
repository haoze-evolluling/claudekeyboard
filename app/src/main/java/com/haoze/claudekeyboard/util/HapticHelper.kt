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

package com.haoze.claudekeyboard.util

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.haoze.claudekeyboard.R

/**
 * Utility functions for dialog background styling and theme helpers.
 */

/**
 * Resolve a color from the current theme attribute.
 */
fun Context.resolveAttrColor(attr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}

/**
 * Replace dialog background with solid white, preserving M3 rounded corners.
 *
 * This fixes the issue where M3's MaterialShapeDrawable applies tonal elevation tint
 * that can't be overridden via theme attributes alone.
 */
fun Dialog.fixM3Background() {
    val window = window ?: return
    val cornerRadius = 12f * context.resources.displayMetrics.density // 12dp in pixels

    val drawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(ContextCompat.getColor(context, R.color.home_card))
        this.cornerRadius = cornerRadius
    }
    window.decorView.background = drawable
}

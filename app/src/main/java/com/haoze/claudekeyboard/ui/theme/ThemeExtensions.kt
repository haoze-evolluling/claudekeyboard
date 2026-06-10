package com.haoze.claudekeyboard.ui.theme

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import androidx.core.content.ContextCompat
import com.haoze.claudekeyboard.R

/**
 * Extension functions for theming buttons.
 */
object ThemeExtensions {

    /**
     * Apply Yes button style (green).
     */
    fun Button.applyYesStyle(context: Context) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(ContextCompat.getColor(context, R.color.btn_yes))
        }
        background = drawable
        setTextColor(ContextCompat.getColor(context, R.color.btn_text))
        textSize = 18f
        isAllCaps = false
    }

    /**
     * Apply No button style (red).
     */
    fun Button.applyNoStyle(context: Context) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(ContextCompat.getColor(context, R.color.btn_no))
        }
        background = drawable
        setTextColor(ContextCompat.getColor(context, R.color.btn_text))
        textSize = 18f
        isAllCaps = false
    }

    /**
     * Apply Ctrl+C button style (orange).
     */
    fun Button.applyCtrlCStyle(context: Context) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(ContextCompat.getColor(context, R.color.btn_ctrl_c))
        }
        background = drawable
        setTextColor(ContextCompat.getColor(context, R.color.btn_text))
        textSize = 18f
        isAllCaps = false
    }

    /**
     * Apply Yes to All button style (green variant).
     */
    fun Button.applyYesToAllStyle(context: Context) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(ContextCompat.getColor(context, R.color.btn_yes_to_all))
        }
        background = drawable
        setTextColor(ContextCompat.getColor(context, R.color.btn_text))
        textSize = 16f
        isAllCaps = false
    }

    /**
     * Apply macro button style.
     */
    fun Button.applyMacroStyle(context: Context) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f
            setColor(ContextCompat.getColor(context, R.color.btn_macro))
            setStroke(2, ContextCompat.getColor(context, R.color.btn_macro_border))
        }
        background = drawable
        setTextColor(ContextCompat.getColor(context, R.color.btn_macro_text))
        textSize = 14f
        isAllCaps = false
    }

    /**
     * Apply floating button style.
     */
    fun Button.applyFloatingStyle(context: Context) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(context, R.color.btn_floating))
        }
        background = drawable
        setTextColor(ContextCompat.getColor(context, R.color.btn_text))
        textSize = 16f
        isAllCaps = false
    }
}
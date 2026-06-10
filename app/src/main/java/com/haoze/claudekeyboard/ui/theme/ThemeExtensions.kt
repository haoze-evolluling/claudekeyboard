package com.haoze.claudekeyboard.ui.theme

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import androidx.core.content.ContextCompat
import com.haoze.claudekeyboard.R

/**
 * Extension functions for theming buttons.
 * Theme: White background + Sky blue buttons + Black borders
 * All corner radius and border widths use unified dimens values.
 */
object ThemeExtensions {

    /**
     * Get unified corner radius from dimens.
     */
    private fun getCornerRadius(context: Context): Float {
        return context.resources.getDimension(R.dimen.corner_radius)
    }

    /**
     * Get unified border width from dimens (in pixels).
     */
    private fun getBorderWidth(context: Context): Int {
        return context.resources.getDimensionPixelSize(R.dimen.border_width)
    }

    /**
     * Apply Yes button style (sky blue).
     */
    fun Button.applyYesStyle(context: Context) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = getCornerRadius(context)
            setColor(ContextCompat.getColor(context, R.color.btn_yes))
            setStroke(getBorderWidth(context), ContextCompat.getColor(context, R.color.btn_border))
        }
        background = drawable
        setTextColor(ContextCompat.getColor(context, R.color.btn_text))
        textSize = 18f
        isAllCaps = false
    }

    /**
     * Apply No button style (light sky blue).
     */
    fun Button.applyNoStyle(context: Context) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = getCornerRadius(context)
            setColor(ContextCompat.getColor(context, R.color.btn_no))
            setStroke(getBorderWidth(context), ContextCompat.getColor(context, R.color.btn_border))
        }
        background = drawable
        setTextColor(ContextCompat.getColor(context, R.color.btn_text))
        textSize = 18f
        isAllCaps = false
    }

    /**
     * Apply Ctrl+C button style (lighter sky blue).
     */
    fun Button.applyCtrlCStyle(context: Context) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = getCornerRadius(context)
            setColor(ContextCompat.getColor(context, R.color.btn_ctrl_c))
            setStroke(getBorderWidth(context), ContextCompat.getColor(context, R.color.btn_border))
        }
        background = drawable
        setTextColor(ContextCompat.getColor(context, R.color.btn_text))
        textSize = 18f
        isAllCaps = false
    }

    /**
     * Apply Yes to All button style (sky blue variant).
     */
    fun Button.applyYesToAllStyle(context: Context) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = getCornerRadius(context)
            setColor(ContextCompat.getColor(context, R.color.btn_yes_to_all))
            setStroke(getBorderWidth(context), ContextCompat.getColor(context, R.color.btn_border))
        }
        background = drawable
        setTextColor(ContextCompat.getColor(context, R.color.btn_text))
        textSize = 16f
        isAllCaps = false
    }

    /**
     * Apply macro button style (light sky blue with black border).
     */
    fun Button.applyMacroStyle(context: Context) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = getCornerRadius(context)
            setColor(ContextCompat.getColor(context, R.color.btn_macro))
            setStroke(getBorderWidth(context), ContextCompat.getColor(context, R.color.btn_macro_border))
        }
        background = drawable
        setTextColor(ContextCompat.getColor(context, R.color.btn_macro_text))
        textSize = 14f
        isAllCaps = false
    }

    /**
     * Apply floating button style (white with black border).
     */
    fun Button.applyFloatingStyle(context: Context) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = getCornerRadius(context)
            setColor(ContextCompat.getColor(context, R.color.white))
            setStroke(getBorderWidth(context), ContextCompat.getColor(context, R.color.btn_border))
        }
        background = drawable
        setTextColor(ContextCompat.getColor(context, R.color.btn_text))
        textSize = 16f
        isAllCaps = false
    }
}

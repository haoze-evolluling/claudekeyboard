package com.haoze.claudekeyboard.ui.keyboard

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.haoze.claudekeyboard.MainActivity
import com.haoze.claudekeyboard.R
import com.haoze.claudekeyboard.bluetooth.KeyboardSender

/**
 * Full QWERTY keyboard fragment for Bluetooth HID input.
 * Supports modifier toggling, shift auto-release, and caps lock.
 */
class KeyboardFragment : Fragment() {

    // Modifier state
    private var isShiftActive = false
    private var isCtrlActive = false
    private var isAltActive = false
    private var isWinActive = false
    private var isCapsLock = false

    // Modifier button references for visual state updates
    private val shiftButtons = mutableListOf<TextView>()
    private var ctrlButton: TextView? = null
    private var altLeftButton: TextView? = null
    private var altRightButton: TextView? = null
    private var winLeftButton: TextView? = null
    private var winRightButton: TextView? = null

    // All key buttons for shift label updates
    private val allKeyButtons = mutableMapOf<TextView, KeyData>()

    // Caps lock long press handler
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Key type enum.
     */
    enum class KeyType {
        NORMAL,     // Regular character key
        MODIFIER,   // Ctrl, Shift, Alt, Win
        SPECIAL     // Esc, Tab, Caps, Backspace, Enter, Space
    }

    /**
     * Data class representing a single key on the keyboard.
     */
    data class KeyData(
        val primaryLabel: String,       // Normal label
        val shiftLabel: String = "",    // Shift-state label (superscript)
        val hidKeyCode: Byte,           // HID key code
        val hidModifier: Byte = 0x00,   // Modifier needed for this key (e.g., shift for '!')
        val type: KeyType = KeyType.NORMAL,
        val modifierBit: Byte = 0x00,   // For MODIFIER type: which modifier bit this key toggles
        val weight: Float = 1f          // Layout weight for sizing
    )

    // ---- Key Row Definitions ----

    private val row0Keys = listOf(
        KeyData("Esc", "", KeyboardSender.KEY_ESC, type = KeyType.SPECIAL, weight = 1.2f),
        KeyData("1", "!", KeyboardSender.KEY_1, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("2", "@", KeyboardSender.KEY_2, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("3", "#", KeyboardSender.KEY_3, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("4", "$", KeyboardSender.KEY_4, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("5", "%", KeyboardSender.KEY_5, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("6", "^", KeyboardSender.KEY_6, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("7", "&", KeyboardSender.KEY_7, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("8", "*", KeyboardSender.KEY_8, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("9", "(", KeyboardSender.KEY_9, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("0", ")", KeyboardSender.KEY_0, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("-", "_", KeyboardSender.KEY_MINUS, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("=", "+", KeyboardSender.KEY_EQUAL, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("`", "~", KeyboardSender.KEY_GRAVE, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("Bksp", "", KeyboardSender.KEY_BACKSPACE, type = KeyType.SPECIAL, weight = 1.5f)
    )

    private val row1Keys = listOf(
        KeyData("Tab", "", KeyboardSender.KEY_TAB, type = KeyType.SPECIAL, weight = 1.3f),
        KeyData("Q", "", KeyboardSender.KEY_Q),
        KeyData("W", "", KeyboardSender.KEY_W),
        KeyData("E", "", KeyboardSender.KEY_E),
        KeyData("R", "", KeyboardSender.KEY_R),
        KeyData("T", "", KeyboardSender.KEY_T),
        KeyData("Y", "", KeyboardSender.KEY_Y),
        KeyData("U", "", KeyboardSender.KEY_U),
        KeyData("I", "", KeyboardSender.KEY_I),
        KeyData("O", "", KeyboardSender.KEY_O),
        KeyData("P", "", KeyboardSender.KEY_P),
        KeyData("[", "{", KeyboardSender.KEY_LEFT_BRACKET, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("]", "}", KeyboardSender.KEY_RIGHT_BRACKET, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("\\", "|", KeyboardSender.KEY_BACKSLASH, KeyboardSender.MODIFIER_SHIFT_LEFT)
    )

    private val row2Keys = listOf(
        KeyData("Caps", "", KeyboardSender.KEY_ESC, type = KeyType.SPECIAL, weight = 1.5f),
        KeyData("A", "", KeyboardSender.KEY_A),
        KeyData("S", "", KeyboardSender.KEY_S),
        KeyData("D", "", KeyboardSender.KEY_D),
        KeyData("F", "", KeyboardSender.KEY_F),
        KeyData("G", "", KeyboardSender.KEY_G),
        KeyData("H", "", KeyboardSender.KEY_H),
        KeyData("J", "", KeyboardSender.KEY_J),
        KeyData("K", "", KeyboardSender.KEY_K),
        KeyData("L", "", KeyboardSender.KEY_L),
        KeyData(";", ":", KeyboardSender.KEY_SEMICOLON, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("'", "\"", KeyboardSender.KEY_APOSTROPHE, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("Enter", "", KeyboardSender.KEY_ENTER, type = KeyType.SPECIAL, weight = 1.8f)
    )

    private val row3Keys = listOf(
        KeyData("Shift", "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_SHIFT_LEFT, weight = 1.8f),
        KeyData("Z", "", KeyboardSender.KEY_Z),
        KeyData("X", "", KeyboardSender.KEY_X),
        KeyData("C", "", KeyboardSender.KEY_C),
        KeyData("V", "", KeyboardSender.KEY_V),
        KeyData("B", "", KeyboardSender.KEY_B),
        KeyData("N", "", KeyboardSender.KEY_N),
        KeyData("M", "", KeyboardSender.KEY_M),
        KeyData(",", "<", KeyboardSender.KEY_COMMA, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData(".", ">", KeyboardSender.KEY_PERIOD, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("/", "?", KeyboardSender.KEY_SLASH, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("Shift", "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_SHIFT_LEFT, weight = 1.8f)
    )

    private val row4Keys = listOf(
        KeyData("Ctrl", "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_CTRL_LEFT, weight = 1.2f),
        KeyData("Win", "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_GUI_LEFT, weight = 1.0f),
        KeyData("Alt", "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_ALT_LEFT, weight = 1.0f),
        KeyData("Space", "", KeyboardSender.KEY_SPACE, type = KeyType.SPECIAL, weight = 5f),
        KeyData("Alt", "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_ALT_RIGHT, weight = 1.0f),
        KeyData("Win", "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_GUI_RIGHT, weight = 1.0f),
        KeyData("Ctrl", "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_CTRL_RIGHT, weight = 1.2f)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_keyboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildKeyboard(view)
    }

    /**
     * Build all keyboard rows programmatically.
     */
    private fun buildKeyboard(root: View) {
        val rows = listOf(
            R.id.keyboard_row_0 to row0Keys,
            R.id.keyboard_row_1 to row1Keys,
            R.id.keyboard_row_2 to row2Keys,
            R.id.keyboard_row_3 to row3Keys,
            R.id.keyboard_row_4 to row4Keys
        )

        for ((rowId, keys) in rows) {
            val rowLayout = root.findViewById<LinearLayout>(rowId)
            buildRow(rowLayout, keys)
        }

        // Back to Claude button in row 5
        val row5 = root.findViewById<LinearLayout>(R.id.keyboard_row_5)
        val margin = resources.getDimensionPixelSize(R.dimen.keyboard_key_margin)
        val keyHeight = resources.getDimensionPixelSize(R.dimen.keyboard_key_height)
        val textSize = resources.getDimensionPixelSize(R.dimen.keyboard_key_text_size).toFloat()

        val button = TextView(requireContext()).apply {
            text = getString(R.string.tab_claude)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setBackgroundResource(R.drawable.bg_outline_button)
            isClickable = true
            isFocusable = false
            setOnClickListener { (activity as? MainActivity)?.switchToClaudeTab() }
        }
        val buttonWidth = resources.getDimensionPixelSize(R.dimen.keyboard_back_button_width)
        val params = LinearLayout.LayoutParams(
            buttonWidth, keyHeight
        )
        params.setMargins(margin, 0, margin, 0)
        button.layoutParams = params
        row5.addView(button)
    }

    /**
     * Build a single keyboard row.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun buildRow(rowLayout: LinearLayout, keys: List<KeyData>) {
        val margin = resources.getDimensionPixelSize(R.dimen.keyboard_key_margin)
        val keyHeight = resources.getDimensionPixelSize(R.dimen.keyboard_key_height)
        val textSize = resources.getDimensionPixelSize(R.dimen.keyboard_key_text_size).toFloat()
        val shiftTextSize = resources.getDimensionPixelSize(R.dimen.keyboard_shift_text_size).toFloat()

        for (keyData in keys) {
            val button = createKeyButton(keyData, keyHeight, textSize, shiftTextSize)

            val params = LinearLayout.LayoutParams(0, keyHeight, keyData.weight)
            params.setMargins(margin, 0, margin, 0)
            button.layoutParams = params

            // Set click listener
            button.setOnClickListener { onKeyPressed(keyData, button) }

            // Caps Lock long press for toggle
            if (keyData.primaryLabel == "Caps") {
                button.setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            handler.postDelayed({
                                toggleCapsLock(button)
                            }, 500)
                            v.isPressed = true
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            handler.removeCallbacksAndMessages(null)
                            if (event.action == MotionEvent.ACTION_UP && event.eventTime - event.downTime < 500) {
                                // Short press: send Esc
                                onKeyPressed(keyData, button)
                            }
                            v.isPressed = false
                            true
                        }
                        else -> false
                    }
                }
            }

            rowLayout.addView(button)
        }
    }

    /**
     * Create a key button view.
     */
    private fun createKeyButton(
        keyData: KeyData,
        height: Int,
        textSize: Float,
        shiftTextSize: Float
    ): TextView {
        val button = TextView(requireContext()).apply {
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_key_normal)
            isClickable = true
            isFocusable = false
        }

        // Set text appearance based on key type
        when (keyData.type) {
            KeyType.MODIFIER -> {
                button.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                button.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnSurface))
            }
            KeyType.SPECIAL -> {
                button.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                button.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnSurface))
            }
            KeyType.NORMAL -> {
                // For normal keys with shift labels, use a spannable or just show primary
                button.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                button.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnSurface))
            }
        }

        // Set label
        updateKeyLabel(button, keyData, shiftTextSize)

        // Track the button
        if (keyData.type == KeyType.MODIFIER) {
            registerModifierButton(keyData, button)
        }
        allKeyButtons[button] = keyData

        return button
    }

    /**
     * Update a key button's label based on current shift state.
     */
    private fun updateKeyLabel(button: TextView, keyData: KeyData, shiftTextSize: Float) {
        if (keyData.type == KeyType.NORMAL && keyData.shiftLabel.isNotEmpty()) {
            // Show both primary and shift label
            val displayLabel = if (isShiftActive || isCapsLock) {
                keyData.shiftLabel
            } else {
                keyData.primaryLabel
            }
            button.text = displayLabel
        } else {
            button.text = keyData.primaryLabel
        }
    }

    /**
     * Register a modifier button for state tracking.
     */
    private fun registerModifierButton(keyData: KeyData, button: TextView) {
        when (keyData.modifierBit) {
            KeyboardSender.MODIFIER_SHIFT_LEFT, KeyboardSender.MODIFIER_SHIFT_RIGHT -> {
                shiftButtons.add(button)
            }
            KeyboardSender.MODIFIER_CTRL_LEFT, KeyboardSender.MODIFIER_CTRL_RIGHT -> {
                // Store both ctrl buttons
                if (ctrlButton == null) ctrlButton = button
                // We'll update both via the modifier state
            }
            KeyboardSender.MODIFIER_ALT_LEFT -> altLeftButton = button
            KeyboardSender.MODIFIER_ALT_RIGHT -> altRightButton = button
            KeyboardSender.MODIFIER_GUI_LEFT -> winLeftButton = button
            KeyboardSender.MODIFIER_GUI_RIGHT -> winRightButton = button
        }
    }

    /**
     * Handle a key press event.
     */
    private fun onKeyPressed(keyData: KeyData, button: TextView) {
        val sender = getKeyboardSender() ?: return

        when (keyData.type) {
            KeyType.MODIFIER -> {
                toggleModifier(keyData.modifierBit)
                return
            }
            KeyType.SPECIAL -> {
                // Special keys: send with current modifiers
                val modifier = buildModifierByte()
                Thread { sender.sendKeyPress(modifier, keyData.hidKeyCode) }.start()
            }
            KeyType.NORMAL -> {
                // Normal keys: determine if we need shift
                val effectiveModifier = when {
                    isShiftActive -> KeyboardSender.MODIFIER_SHIFT_LEFT
                    isCapsLock && keyData.primaryLabel[0].isLetter() -> KeyboardSender.MODIFIER_SHIFT_LEFT
                    else -> keyData.hidModifier
                }
                val combinedModifier = (effectiveModifier.toInt() or buildModifierByte().toInt()).toByte()
                Thread { sender.sendKeyPress(combinedModifier, keyData.hidKeyCode) }.start()
            }
        }

        // Auto-release shift after key press (one-shot behavior)
        if (isShiftActive) {
            isShiftActive = false
            updateModifierVisuals()
            updateAllKeyLabels()
        }

        // Auto-release other modifiers after key press (one-shot behavior)
        if (isCtrlActive) {
            isCtrlActive = false
            updateModifierVisuals()
        }
        if (isAltActive) {
            isAltActive = false
            updateModifierVisuals()
        }
        if (isWinActive) {
            isWinActive = false
            updateModifierVisuals()
        }
    }

    /**
     * Toggle a modifier state.
     */
    private fun toggleModifier(modifierBit: Byte) {
        when (modifierBit) {
            KeyboardSender.MODIFIER_SHIFT_LEFT, KeyboardSender.MODIFIER_SHIFT_RIGHT -> {
                isShiftActive = !isShiftActive
            }
            KeyboardSender.MODIFIER_CTRL_LEFT, KeyboardSender.MODIFIER_CTRL_RIGHT -> {
                isCtrlActive = !isCtrlActive
            }
            KeyboardSender.MODIFIER_ALT_LEFT, KeyboardSender.MODIFIER_ALT_RIGHT -> {
                isAltActive = !isAltActive
            }
            KeyboardSender.MODIFIER_GUI_LEFT, KeyboardSender.MODIFIER_GUI_RIGHT -> {
                isWinActive = !isWinActive
            }
        }
        updateModifierVisuals()
        updateAllKeyLabels()
    }

    /**
     * Toggle caps lock state (called on long press of Caps key).
     */
    private fun toggleCapsLock(button: TextView) {
        isCapsLock = !isCapsLock
        updateModifierVisuals()
        updateAllKeyLabels()
    }

    /**
     * Build the current modifier byte from active modifier states.
     */
    private fun buildModifierByte(): Byte {
        var modifier: Byte = 0
        if (isCtrlActive) modifier = (modifier.toInt() or KeyboardSender.MODIFIER_CTRL_LEFT.toInt()).toByte()
        if (isShiftActive) modifier = (modifier.toInt() or KeyboardSender.MODIFIER_SHIFT_LEFT.toInt()).toByte()
        if (isAltActive) modifier = (modifier.toInt() or KeyboardSender.MODIFIER_ALT_LEFT.toInt()).toByte()
        if (isWinActive) modifier = (modifier.toInt() or KeyboardSender.MODIFIER_GUI_LEFT.toInt()).toByte()
        return modifier
    }

    /**
     * Update modifier button visual states.
     */
    private fun updateModifierVisuals() {
        val activeBg = R.drawable.bg_key_modifier_active
        val normalBg = R.drawable.bg_key_normal

        // Shift buttons
        for (btn in shiftButtons) {
            btn.setBackgroundResource(if (isShiftActive) activeBg else normalBg)
            btn.setTextColor(if (isShiftActive) resolveAttrColor(com.google.android.material.R.attr.colorOnPrimary)
            else resolveAttrColor(com.google.android.material.R.attr.colorOnSurface))
        }

        // Caps lock button
        allKeyButtons.forEach { (btn, data) ->
            if (data.primaryLabel == "Caps") {
                btn.setBackgroundResource(if (isCapsLock) activeBg else normalBg)
                btn.setTextColor(if (isCapsLock) resolveAttrColor(com.google.android.material.R.attr.colorOnPrimary)
                else resolveAttrColor(com.google.android.material.R.attr.colorOnSurface))
            }
        }

        // Ctrl buttons
        ctrlButton?.let {
            it.setBackgroundResource(if (isCtrlActive) activeBg else normalBg)
            it.setTextColor(if (isCtrlActive) resolveAttrColor(com.google.android.material.R.attr.colorOnPrimary)
            else resolveAttrColor(com.google.android.material.R.attr.colorOnSurface))
        }

        // Alt buttons
        altLeftButton?.let {
            it.setBackgroundResource(if (isAltActive) activeBg else normalBg)
            it.setTextColor(if (isAltActive) resolveAttrColor(com.google.android.material.R.attr.colorOnPrimary)
            else resolveAttrColor(com.google.android.material.R.attr.colorOnSurface))
        }
        altRightButton?.let {
            it.setBackgroundResource(if (isAltActive) activeBg else normalBg)
            it.setTextColor(if (isAltActive) resolveAttrColor(com.google.android.material.R.attr.colorOnPrimary)
            else resolveAttrColor(com.google.android.material.R.attr.colorOnSurface))
        }

        // Win buttons
        winLeftButton?.let {
            it.setBackgroundResource(if (isWinActive) activeBg else normalBg)
            it.setTextColor(if (isWinActive) resolveAttrColor(com.google.android.material.R.attr.colorOnPrimary)
            else resolveAttrColor(com.google.android.material.R.attr.colorOnSurface))
        }
        winRightButton?.let {
            it.setBackgroundResource(if (isWinActive) activeBg else normalBg)
            it.setTextColor(if (isWinActive) resolveAttrColor(com.google.android.material.R.attr.colorOnPrimary)
            else resolveAttrColor(com.google.android.material.R.attr.colorOnSurface))
        }
    }

    /**
     * Update all key labels based on shift/caps state.
     */
    private fun updateAllKeyLabels() {
        val shiftTextSize = resources.getDimensionPixelSize(R.dimen.keyboard_shift_text_size).toFloat()
        for ((button, keyData) in allKeyButtons) {
            if (keyData.type == KeyType.NORMAL) {
                updateKeyLabel(button, keyData, shiftTextSize)
            }
        }
    }

    /**
     * Resolve a color from the current theme attribute.
     */
    private fun resolveAttrColor(attr: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    /**
     * Get the KeyboardSender from the hosting activity.
     */
    private fun getKeyboardSender(): KeyboardSender? {
        return (activity as? MainActivity)?.getKeyboardSender()
    }

    /**
     * Enable or disable the keyboard rows (e.g., when connection state changes).
     * The back-to-Claude button is always enabled.
     */
    fun setKeyboardEnabled(enabled: Boolean) {
        val rowIds = listOf(
            R.id.keyboard_row_0,
            R.id.keyboard_row_1,
            R.id.keyboard_row_2,
            R.id.keyboard_row_3,
            R.id.keyboard_row_4
            // Row 5 (Back to Claude) is always enabled
        )
        for (id in rowIds) {
            view?.findViewById<View>(id)?.let { row ->
                row.isEnabled = enabled
                row.alpha = if (enabled) 1.0f else 0.4f
                if (row is ViewGroup) setViewGroupEnabled(row, enabled)
            }
        }
    }

    /**
     * Recursively enable/disable all views in a ViewGroup.
     */
    private fun setViewGroupEnabled(group: ViewGroup, enabled: Boolean) {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            child.isEnabled = enabled
            child.alpha = if (enabled) 1.0f else 0.4f
            if (child is ViewGroup) {
                setViewGroupEnabled(child, enabled)
            }
        }
    }
}

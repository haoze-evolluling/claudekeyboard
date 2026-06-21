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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.haoze.claudekeyboard.MainActivity
import com.haoze.claudekeyboard.R
import com.haoze.claudekeyboard.bluetooth.BluetoothViewModel
import com.haoze.claudekeyboard.bluetooth.KeyboardSender
import com.haoze.claudekeyboard.util.performKeyClick
import com.haoze.claudekeyboard.util.resolveAttrColor

/**
 * Full QWERTY keyboard fragment for Bluetooth HID input.
 * Supports modifier toggling, shift auto-release, and caps lock.
 */
class KeyboardFragment : Fragment() {

    private val bluetoothViewModel: BluetoothViewModel by activityViewModels()

    // Modifier state
    private var isCtrlLeftActive = false   // Left Ctrl: toggle, hold for combos
    private var isAltLeftActive = false    // Left Alt: toggle, hold for combos
    private var isWinRightActive = false  // Right Win: toggle only, for combos like Win+R
    private var isSymbolLock = false  // Right Shift: toggle symbol mode for number/punctuation keys

    // Modifier button references for visual state updates
    private val leftShiftButtons = mutableListOf<TextView>()
    private val rightShiftButtons = mutableListOf<TextView>()
    private val ctrlLeftButtons = mutableListOf<TextView>()
    private val ctrlRightButtons = mutableListOf<TextView>()
    private var altLeftButton: TextView? = null
    private var altRightButton: TextView? = null
    private var winLeftButton: TextView? = null
    private var winRightButton: TextView? = null

    // All key buttons for shift label updates
    private val allKeyButtons = mutableMapOf<TextView, KeyData>()

    // Key repeat handler
    private val handler = Handler(Looper.getMainLooper())
    private var repeatRunnable: Runnable? = null
    private var isKeyPressed = false

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
        val weight: Float = 1f,         // Layout weight for sizing
        val shiftKeyCode: Byte = 0x00   // Alternative HID key code when symbol lock is active (e.g., arrow keys)
    )

    // ---- Key Row Definitions ----

    private val row0Keys by lazy { listOf(
        KeyData(getString(R.string.key_esc), "", KeyboardSender.KEY_ESC, type = KeyType.SPECIAL, weight = 1.2f),
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
        KeyData(getString(R.string.key_bksp), "", KeyboardSender.KEY_BACKSPACE, type = KeyType.SPECIAL, weight = 1.5f)
    ) }

    private val row1Keys by lazy { listOf(
        KeyData(getString(R.string.key_tab), "", KeyboardSender.KEY_TAB, type = KeyType.SPECIAL, weight = 1.3f),
        KeyData("Q", "", KeyboardSender.KEY_Q),
        KeyData("W", "↑", KeyboardSender.KEY_W, shiftKeyCode = KeyboardSender.KEY_UP),
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
    ) }

    private val row2Keys by lazy { listOf(
        KeyData(getString(R.string.key_caps), "", KeyboardSender.KEY_CAPS_LOCK, type = KeyType.SPECIAL, weight = 1.5f),
        KeyData("A", "←", KeyboardSender.KEY_A, shiftKeyCode = KeyboardSender.KEY_LEFT),
        KeyData("S", "↓", KeyboardSender.KEY_S, shiftKeyCode = KeyboardSender.KEY_DOWN),
        KeyData("D", "→", KeyboardSender.KEY_D, shiftKeyCode = KeyboardSender.KEY_RIGHT),
        KeyData("F", "", KeyboardSender.KEY_F),
        KeyData("G", "", KeyboardSender.KEY_G),
        KeyData("H", "", KeyboardSender.KEY_H),
        KeyData("J", "", KeyboardSender.KEY_J),
        KeyData("K", "", KeyboardSender.KEY_K),
        KeyData("L", "", KeyboardSender.KEY_L),
        KeyData(";", ":", KeyboardSender.KEY_SEMICOLON, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData("'", "\"", KeyboardSender.KEY_APOSTROPHE, KeyboardSender.MODIFIER_SHIFT_LEFT),
        KeyData(getString(R.string.key_enter), "", KeyboardSender.KEY_ENTER, type = KeyType.SPECIAL, weight = 1.8f)
    ) }

    private val row3Keys by lazy { listOf(
        KeyData(getString(R.string.key_shift), "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_SHIFT_LEFT, weight = 1.8f),
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
        KeyData(getString(R.string.key_shift), "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_SHIFT_RIGHT, weight = 1.8f)
    ) }

    private val row4Keys by lazy { listOf(
        KeyData(getString(R.string.key_ctrl), "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_CTRL_LEFT, weight = 1.2f),
        KeyData(getString(R.string.key_win), "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_GUI_LEFT, weight = 1.0f),
        KeyData(getString(R.string.key_alt), "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_ALT_LEFT, weight = 1.0f),
        KeyData(getString(R.string.key_space), "", KeyboardSender.KEY_SPACE, type = KeyType.SPECIAL, weight = 5f),
        KeyData(getString(R.string.key_alt), "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_ALT_RIGHT, weight = 1.0f),
        KeyData(getString(R.string.key_win), "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_GUI_RIGHT, weight = 1.0f),
        KeyData(getString(R.string.key_ctrl), "", 0, type = KeyType.MODIFIER, modifierBit = KeyboardSender.MODIFIER_CTRL_RIGHT, weight = 1.2f)
    ) }

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

        // Row 5: Navigation buttons (defined in XML)
        root.findViewById<TextView>(R.id.btn_nav_home).setOnClickListener {
            it.performKeyClick()
            (activity as? MainActivity)?.navigateToHome()
        }
        root.findViewById<TextView>(R.id.btn_nav_touchpad).setOnClickListener {
            it.performKeyClick()
            (activity as? MainActivity)?.switchToTouchpadTab()
        }
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

            when {
                // Modifier keys: touch to toggle with press feedback
                keyData.type == KeyType.MODIFIER -> {
                    button.setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                v.performKeyClick()
                                button.setBackgroundResource(R.drawable.bg_key_pressed)
                                v.isPressed = true
                                true
                            }
                            MotionEvent.ACTION_UP -> {
                                v.isPressed = false
                                onKeyPressed(keyData)
                                updateModifierVisuals()
                                true
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                v.isPressed = false
                                updateModifierVisuals()
                                true
                            }
                            else -> false
                        }
                    }
                }
                // Normal and Special keys: touch for press feedback + long-press repeat
                else -> {
                    button.setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                v.performKeyClick()
                                isKeyPressed = true
                                button.setBackgroundResource(R.drawable.bg_key_pressed)
                                v.isPressed = true

                                // Build effective key code and modifier once for this press and all repeats
                                val effectiveKeyCode: Byte
                                val combinedModifier: Byte
                                if (isSymbolLock && keyData.shiftKeyCode.toInt() != 0) {
                                    effectiveKeyCode = keyData.shiftKeyCode
                                    combinedModifier = buildModifierByte(includeShift = false)
                                } else if (isSymbolLock && keyData.shiftLabel.isNotEmpty()) {
                                    effectiveKeyCode = keyData.hidKeyCode
                                    combinedModifier = buildModifierByte()
                                } else {
                                    effectiveKeyCode = keyData.hidKeyCode
                                    combinedModifier = buildModifierByte()
                                }
                                val sender = getKeyboardSender()

                                if (sender != null) {
                                    // Send initial keypress
                                    Thread {
                                        sender.sendKeyPress(combinedModifier, effectiveKeyCode)
                                    }.start()

                                    // Start long-press repeat
                                    repeatRunnable = object : Runnable {
                                        override fun run() {
                                            if (!isKeyPressed) return
                                            v.performKeyClick()
                                            Thread {
                                                sender.sendKeyPress(combinedModifier, effectiveKeyCode)
                                            }.start()
                                            handler.postDelayed(this, 50)
                                        }
                                    }
                                    handler.postDelayed(repeatRunnable!!, 400)
                                }
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                isKeyPressed = false
                                repeatRunnable?.let { handler.removeCallbacks(it) }
                                repeatRunnable = null
                                button.setBackgroundResource(R.drawable.bg_key_normal)
                                v.isPressed = false
                                true
                            }
                            else -> false
                        }
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

        // Set text appearance
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        button.setTextColor(requireContext().resolveAttrColor(com.google.android.material.R.attr.colorOnSurface))

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
            val displayLabel = if (isSymbolLock) {
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
            KeyboardSender.MODIFIER_SHIFT_LEFT -> leftShiftButtons.add(button)
            KeyboardSender.MODIFIER_SHIFT_RIGHT -> rightShiftButtons.add(button)
            KeyboardSender.MODIFIER_CTRL_LEFT -> ctrlLeftButtons.add(button)
            KeyboardSender.MODIFIER_CTRL_RIGHT -> ctrlRightButtons.add(button)
            KeyboardSender.MODIFIER_ALT_LEFT -> altLeftButton = button
            KeyboardSender.MODIFIER_ALT_RIGHT -> altRightButton = button
            KeyboardSender.MODIFIER_GUI_LEFT -> winLeftButton = button
            KeyboardSender.MODIFIER_GUI_RIGHT -> winRightButton = button
        }
    }

    /**
     * Handle a modifier key press event.
     */
    private fun onKeyPressed(keyData: KeyData) {
        toggleModifier(keyData.modifierBit)
    }

    /**
     * Send a standalone modifier keypress (fire-and-forget).
     */
    private fun sendStandaloneModifier(modifierByte: Byte) {
        getKeyboardSender()?.let { Thread { it.sendKeyPress(modifierByte, 0x00) }.start() }
    }

    /**
     * Toggle a modifier state.
     */
    private fun toggleModifier(modifierBit: Byte) {
        when (modifierBit) {
            KeyboardSender.MODIFIER_SHIFT_LEFT -> sendStandaloneModifier(KeyboardSender.MODIFIER_SHIFT_LEFT)
            KeyboardSender.MODIFIER_SHIFT_RIGHT -> isSymbolLock = !isSymbolLock
            KeyboardSender.MODIFIER_CTRL_LEFT -> isCtrlLeftActive = !isCtrlLeftActive
            KeyboardSender.MODIFIER_CTRL_RIGHT -> sendStandaloneModifier(KeyboardSender.MODIFIER_CTRL_RIGHT)
            KeyboardSender.MODIFIER_ALT_LEFT -> isAltLeftActive = !isAltLeftActive
            KeyboardSender.MODIFIER_ALT_RIGHT -> sendStandaloneModifier(KeyboardSender.MODIFIER_ALT_RIGHT)
            KeyboardSender.MODIFIER_GUI_LEFT -> sendStandaloneModifier(KeyboardSender.MODIFIER_GUI_LEFT)
            KeyboardSender.MODIFIER_GUI_RIGHT -> isWinRightActive = !isWinRightActive
        }
        updateModifierVisuals()
        updateAllKeyLabels()
    }

    /**
     * Build the current modifier byte from active modifier states.
     * @param includeShift whether to include the symbol lock shift bit (default true)
     */
    private fun buildModifierByte(includeShift: Boolean = true): Byte {
        var modifier: Byte = 0
        if (isCtrlLeftActive) modifier = (modifier.toInt() or KeyboardSender.MODIFIER_CTRL_LEFT.toInt()).toByte()
        if (includeShift && isSymbolLock) modifier = (modifier.toInt() or KeyboardSender.MODIFIER_SHIFT_LEFT.toInt()).toByte()
        if (isAltLeftActive) modifier = (modifier.toInt() or KeyboardSender.MODIFIER_ALT_LEFT.toInt()).toByte()
        if (isWinRightActive) modifier = (modifier.toInt() or KeyboardSender.MODIFIER_GUI_RIGHT.toInt()).toByte()
        return modifier
    }

    /**
     * Update modifier button visual states.
     */
    private fun updateModifierVisuals() {
        val activeBg = R.drawable.bg_key_modifier_active
        val normalBg = R.drawable.bg_key_normal

        val activeTextColor = requireContext().resolveAttrColor(com.google.android.material.R.attr.colorOnPrimary)
        val normalTextColor = requireContext().resolveAttrColor(com.google.android.material.R.attr.colorOnSurface)

        // Left Shift buttons (fire-and-forget, no persistent state)
        for (btn in leftShiftButtons) {
            btn.setBackgroundResource(normalBg)
            btn.setTextColor(normalTextColor)
        }

        // Right Shift buttons (symbol lock toggle)
        for (btn in rightShiftButtons) {
            btn.setBackgroundResource(if (isSymbolLock) activeBg else normalBg)
            btn.setTextColor(if (isSymbolLock) activeTextColor else normalTextColor)
        }

        // Ctrl buttons (left = toggle, right = fire-and-forget)
        for (btn in ctrlLeftButtons) {
            btn.setBackgroundResource(if (isCtrlLeftActive) activeBg else normalBg)
            btn.setTextColor(if (isCtrlLeftActive) activeTextColor else normalTextColor)
        }
        for (btn in ctrlRightButtons) {
            btn.setBackgroundResource(normalBg)
            btn.setTextColor(normalTextColor)
        }

        // Alt buttons (left = toggle, right = fire-and-forget)
        altLeftButton?.let {
            it.setBackgroundResource(if (isAltLeftActive) activeBg else normalBg)
            it.setTextColor(if (isAltLeftActive) activeTextColor else normalTextColor)
        }
        altRightButton?.let {
            it.setBackgroundResource(normalBg)
            it.setTextColor(normalTextColor)
        }

        // Win buttons (independent)
        winLeftButton?.let {
            it.setBackgroundResource(normalBg)
            it.setTextColor(normalTextColor)
        }
        winRightButton?.let {
            it.setBackgroundResource(if (isWinRightActive) activeBg else normalBg)
            it.setTextColor(if (isWinRightActive) activeTextColor else normalTextColor)
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
     * Get the KeyboardSender from the ViewModel.
     */
    private fun getKeyboardSender(): KeyboardSender? {
        return bluetoothViewModel.keyboardSender.value
    }

    /**
     * Enable or disable the keyboard rows (e.g., when connection state changes).
     * The back-to-home button is always enabled.
     */
    fun setKeyboardEnabled(enabled: Boolean) {
        val rowIds = listOf(
            R.id.keyboard_row_0,
            R.id.keyboard_row_1,
            R.id.keyboard_row_2,
            R.id.keyboard_row_3,
            R.id.keyboard_row_4
            // Row 5 (Back to Home) is always enabled
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

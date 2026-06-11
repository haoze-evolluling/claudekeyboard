package com.haoze.claudekeyboard.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.util.Log
import android.view.KeyEvent

/**
 * Sends HID keyboard reports via Bluetooth.
 * Ported from Kontroller project, extended with char-to-HID mapping.
 */
@Suppress("MemberVisibilityCanBePrivate")
open class KeyboardSender(
    val hidDevice: BluetoothHidDevice,
    val host: BluetoothDevice
) {
    val keyboardReport = KeyboardReport()

    /**
     * Send the current keyboard report to the host.
     */
    protected open fun sendKeys() {
        if (!hidDevice.sendReport(host, KeyboardReport.ID, keyboardReport.bytes)) {
            Log.e(TAG, "Report wasn't sent")
        }
    }

    /**
     * Send an all-zeros release report.
     */
    fun sendNullKeys() {
        keyboardReport.bytes.fill(0)
        if (!hidDevice.sendReport(host, KeyboardReport.ID, keyboardReport.bytes)) {
            Log.e(TAG, "Null report wasn't sent")
        }
    }

    /**
     * Set modifier keys from an Android KeyEvent.
     */
    protected open fun setModifiers(event: KeyEvent) {
        if (event.isShiftPressed) keyboardReport.leftShift = true
        if (event.isAltPressed) keyboardReport.leftAlt = true
        if (event.isCtrlPressed) keyboardReport.leftControl = true
        if (event.isMetaPressed) keyboardReport.leftGui = true
    }

    /**
     * Send a key press then release (for KeyEvent-based input).
     */
    protected open fun customSender(modifierCheckedState: Int) {
        sendKeys()
        if (modifierCheckedState == 0) sendNullKeys()
        else {
            keyboardReport.key1 = 0.toByte()
            sendKeys()
        }
    }

    /**
     * Handle a key event: map to HID code, set modifiers, send.
     * @return true if the key was handled
     */
    fun keyEventHandler(keyEventCode: Int, event: KeyEvent, modifierCheckedState: Int): Boolean {
        val byteKey = KeyboardReport.KeyEventMap[keyEventCode] ?: return false

        setModifiers(event)
        // Special handling for @ # * which need shift
        if (event.keyCode == KeyEvent.KEYCODE_AT ||
            event.keyCode == KeyEvent.KEYCODE_POUND ||
            event.keyCode == KeyEvent.KEYCODE_STAR
        ) {
            keyboardReport.leftShift = true
        }
        keyboardReport.key1 = byteKey.toByte()
        customSender(modifierCheckedState)
        return true
    }

    /**
     * Send a keyboard event (called from Activity's onKeyDown/onKeyUp).
     * @return true if the key was handled
     */
    fun sendKeyboard(keyCode: Int, event: KeyEvent, modifierCheckedState: Int): Boolean {
        return keyEventHandler(event.keyCode, event, modifierCheckedState)
    }

    // ---- Convenience methods for direct key sending (no KeyEvent needed) ----

    /**
     * Send a direct key press with optional modifier, then release.
     * @param modifier Modifier byte (0x00 for none, 0x01 for left ctrl, etc.)
     * @param keyCode HID key code
     * @param delayMs Delay between press and release
     */
    fun sendKeyPress(modifier: Byte, keyCode: Byte, delayMs: Long = 30) {
        keyboardReport.reset()
        keyboardReport.bytes[1] = modifier
        keyboardReport.key1 = keyCode
        sendKeys()
        Thread.sleep(delayMs)
        sendNullKeys()
    }

    /**
     * Type a string character by character.
     * @param text The string to type
     * @param delayMs Delay between each character
     */
    fun sendText(text: String, delayMs: Long = 50) {
        for (char in text) {
            val hidCode = charToHidCode(char) ?: continue
            sendKeyPress(hidCode.first, hidCode.second)
            Thread.sleep(delayMs)
        }
    }

    /**
     * Type a command string and press Enter.
     * @param command The command to send
     */
    fun sendMacro(command: String) {
        sendText(command)
        sendKeyPress(0x00, KEY_ENTER)
    }

    /**
     * Send a key press with combined modifier byte, then release.
     * @param modifiers Combined modifier byte (e.g., MODIFIER_CTRL_LEFT or MODIFIER_SHIFT_LEFT)
     * @param keyCode HID key code
     * @param delayMs Delay between press and release
     */
    fun sendModifierCombo(modifiers: Byte, keyCode: Byte, delayMs: Long = 30) {
        sendKeyPress(modifiers, keyCode, delayMs)
    }

    companion object {
        const val TAG = "KeyboardSender"

        // Modifier bit masks
        const val MODIFIER_CTRL_LEFT: Byte = 0x01
        const val MODIFIER_SHIFT_LEFT: Byte = 0x02
        const val MODIFIER_ALT_LEFT: Byte = 0x04
        const val MODIFIER_GUI_LEFT: Byte = 0x08
        const val MODIFIER_CTRL_RIGHT: Byte = 0x10
        const val MODIFIER_SHIFT_RIGHT: Byte = 0x20
        const val MODIFIER_ALT_RIGHT: Byte = 0x40
        const val MODIFIER_GUI_RIGHT: Byte = 0x80.toByte()

        // Common HID key codes
        const val KEY_A: Byte = 0x04
        const val KEY_B: Byte = 0x05
        const val KEY_C: Byte = 0x06
        const val KEY_D: Byte = 0x07
        const val KEY_E: Byte = 0x08
        const val KEY_F: Byte = 0x09
        const val KEY_G: Byte = 0x0A
        const val KEY_H: Byte = 0x0B
        const val KEY_I: Byte = 0x0C
        const val KEY_J: Byte = 0x0D
        const val KEY_K: Byte = 0x0E
        const val KEY_L: Byte = 0x0F
        const val KEY_M: Byte = 0x10
        const val KEY_N: Byte = 0x11
        const val KEY_O: Byte = 0x12
        const val KEY_P: Byte = 0x13
        const val KEY_Q: Byte = 0x14
        const val KEY_R: Byte = 0x15
        const val KEY_S: Byte = 0x16
        const val KEY_T: Byte = 0x17
        const val KEY_U: Byte = 0x18
        const val KEY_V: Byte = 0x19
        const val KEY_W: Byte = 0x1A
        const val KEY_X: Byte = 0x1B
        const val KEY_Y: Byte = 0x1C
        const val KEY_Z: Byte = 0x1D

        const val KEY_1: Byte = 0x1E
        const val KEY_2: Byte = 0x1F
        const val KEY_3: Byte = 0x20
        const val KEY_4: Byte = 0x21
        const val KEY_5: Byte = 0x22
        const val KEY_6: Byte = 0x23
        const val KEY_7: Byte = 0x24
        const val KEY_8: Byte = 0x25
        const val KEY_9: Byte = 0x26
        const val KEY_0: Byte = 0x27

        const val KEY_ENTER: Byte = 0x28
        const val KEY_ESC: Byte = 0x29
        const val KEY_BACKSPACE: Byte = 0x2A
        const val KEY_TAB: Byte = 0x2B
        const val KEY_SPACE: Byte = 0x2C
        const val KEY_MINUS: Byte = 0x2D
        const val KEY_EQUAL: Byte = 0x2E
        const val KEY_LEFT_BRACKET: Byte = 0x2F
        const val KEY_RIGHT_BRACKET: Byte = 0x30
        const val KEY_BACKSLASH: Byte = 0x31
        const val KEY_SEMICOLON: Byte = 0x33
        const val KEY_APOSTROPHE: Byte = 0x34
        const val KEY_GRAVE: Byte = 0x35
        const val KEY_COMMA: Byte = 0x36
        const val KEY_PERIOD: Byte = 0x37
        const val KEY_SLASH: Byte = 0x38
        const val KEY_CAPS_LOCK: Byte = 0x39

        // Arrow keys
        const val KEY_RIGHT: Byte = 0x4F
        const val KEY_LEFT: Byte = 0x50
        const val KEY_DOWN: Byte = 0x51
        const val KEY_UP: Byte = 0x52

        /**
         * Convert a character to (modifier, hidKeyCode) pair.
         */
        fun charToHidCode(char: Char): Pair<Byte, Byte>? {
            return when (char) {
                'a' -> Pair(0x00, 0x04); 'b' -> Pair(0x00, 0x05); 'c' -> Pair(0x00, 0x06)
                'd' -> Pair(0x00, 0x07); 'e' -> Pair(0x00, 0x08); 'f' -> Pair(0x00, 0x09)
                'g' -> Pair(0x00, 0x0A); 'h' -> Pair(0x00, 0x0B); 'i' -> Pair(0x00, 0x0C)
                'j' -> Pair(0x00, 0x0D); 'k' -> Pair(0x00, 0x0E); 'l' -> Pair(0x00, 0x0F)
                'm' -> Pair(0x00, 0x10); 'n' -> Pair(0x00, 0x11); 'o' -> Pair(0x00, 0x12)
                'p' -> Pair(0x00, 0x13); 'q' -> Pair(0x00, 0x14); 'r' -> Pair(0x00, 0x15)
                's' -> Pair(0x00, 0x16); 't' -> Pair(0x00, 0x17); 'u' -> Pair(0x00, 0x18)
                'v' -> Pair(0x00, 0x19); 'w' -> Pair(0x00, 0x1A); 'x' -> Pair(0x00, 0x1B)
                'y' -> Pair(0x00, 0x1C); 'z' -> Pair(0x00, 0x1D)

                'A' -> Pair(MODIFIER_SHIFT_LEFT, 0x04); 'B' -> Pair(MODIFIER_SHIFT_LEFT, 0x05)
                'C' -> Pair(MODIFIER_SHIFT_LEFT, 0x06); 'D' -> Pair(MODIFIER_SHIFT_LEFT, 0x07)
                'E' -> Pair(MODIFIER_SHIFT_LEFT, 0x08); 'F' -> Pair(MODIFIER_SHIFT_LEFT, 0x09)
                'G' -> Pair(MODIFIER_SHIFT_LEFT, 0x0A); 'H' -> Pair(MODIFIER_SHIFT_LEFT, 0x0B)
                'I' -> Pair(MODIFIER_SHIFT_LEFT, 0x0C); 'J' -> Pair(MODIFIER_SHIFT_LEFT, 0x0D)
                'K' -> Pair(MODIFIER_SHIFT_LEFT, 0x0E); 'L' -> Pair(MODIFIER_SHIFT_LEFT, 0x0F)
                'M' -> Pair(MODIFIER_SHIFT_LEFT, 0x10); 'N' -> Pair(MODIFIER_SHIFT_LEFT, 0x11)
                'O' -> Pair(MODIFIER_SHIFT_LEFT, 0x12); 'P' -> Pair(MODIFIER_SHIFT_LEFT, 0x13)
                'Q' -> Pair(MODIFIER_SHIFT_LEFT, 0x14); 'R' -> Pair(MODIFIER_SHIFT_LEFT, 0x15)
                'S' -> Pair(MODIFIER_SHIFT_LEFT, 0x16); 'T' -> Pair(MODIFIER_SHIFT_LEFT, 0x17)
                'U' -> Pair(MODIFIER_SHIFT_LEFT, 0x18); 'V' -> Pair(MODIFIER_SHIFT_LEFT, 0x19)
                'W' -> Pair(MODIFIER_SHIFT_LEFT, 0x1A); 'X' -> Pair(MODIFIER_SHIFT_LEFT, 0x1B)
                'Y' -> Pair(MODIFIER_SHIFT_LEFT, 0x1C); 'Z' -> Pair(MODIFIER_SHIFT_LEFT, 0x1D)

                '1' -> Pair(0x00, 0x1E); '2' -> Pair(0x00, 0x1F); '3' -> Pair(0x00, 0x20)
                '4' -> Pair(0x00, 0x21); '5' -> Pair(0x00, 0x22); '6' -> Pair(0x00, 0x23)
                '7' -> Pair(0x00, 0x24); '8' -> Pair(0x00, 0x25); '9' -> Pair(0x00, 0x26)
                '0' -> Pair(0x00, 0x27)

                '!' -> Pair(MODIFIER_SHIFT_LEFT, 0x1E); '@' -> Pair(MODIFIER_SHIFT_LEFT, 0x1F)
                '#' -> Pair(MODIFIER_SHIFT_LEFT, 0x20); '$' -> Pair(MODIFIER_SHIFT_LEFT, 0x21)
                '%' -> Pair(MODIFIER_SHIFT_LEFT, 0x22); '^' -> Pair(MODIFIER_SHIFT_LEFT, 0x23)
                '&' -> Pair(MODIFIER_SHIFT_LEFT, 0x24); '*' -> Pair(MODIFIER_SHIFT_LEFT, 0x25)
                '(' -> Pair(MODIFIER_SHIFT_LEFT, 0x26); ')' -> Pair(MODIFIER_SHIFT_LEFT, 0x27)

                ' ' -> Pair(0x00, KEY_SPACE)
                '\n' -> Pair(0x00, KEY_ENTER)
                '\t' -> Pair(0x00, 0x2B)

                '-' -> Pair(0x00, KEY_MINUS); '_' -> Pair(MODIFIER_SHIFT_LEFT, KEY_MINUS)
                '=' -> Pair(0x00, KEY_EQUAL); '+' -> Pair(MODIFIER_SHIFT_LEFT, KEY_EQUAL)
                '[' -> Pair(0x00, KEY_LEFT_BRACKET); '{' -> Pair(MODIFIER_SHIFT_LEFT, KEY_LEFT_BRACKET)
                ']' -> Pair(0x00, KEY_RIGHT_BRACKET); '}' -> Pair(MODIFIER_SHIFT_LEFT, KEY_RIGHT_BRACKET)
                '\\' -> Pair(0x00, KEY_BACKSLASH); '|' -> Pair(MODIFIER_SHIFT_LEFT, KEY_BACKSLASH)
                ';' -> Pair(0x00, KEY_SEMICOLON); ':' -> Pair(MODIFIER_SHIFT_LEFT, KEY_SEMICOLON)
                '\'' -> Pair(0x00, KEY_APOSTROPHE); '"' -> Pair(MODIFIER_SHIFT_LEFT, KEY_APOSTROPHE)
                '`' -> Pair(0x00, KEY_GRAVE); '~' -> Pair(MODIFIER_SHIFT_LEFT, KEY_GRAVE)
                ',' -> Pair(0x00, KEY_COMMA); '<' -> Pair(MODIFIER_SHIFT_LEFT, KEY_COMMA)
                '.' -> Pair(0x00, KEY_PERIOD); '>' -> Pair(MODIFIER_SHIFT_LEFT, KEY_PERIOD)
                '/' -> Pair(0x00, KEY_SLASH); '?' -> Pair(MODIFIER_SHIFT_LEFT, KEY_SLASH)

                else -> null
            }
        }
    }
}

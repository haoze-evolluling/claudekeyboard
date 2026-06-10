package com.haoze.claudekeyboard.bluetooth

/**
 * Helper class for building HID reports for Bluetooth keyboard.
 * Standard keyboard HID report: 8 bytes (modifier + reserved + 6 key codes)
 */
object HidReportHelper {

    // Modifier bit masks
    const val MODIFIER_CTRL_LEFT: Byte = 0x01
    const val MODIFIER_SHIFT_LEFT: Byte = 0x02
    const val MODIFIER_ALT_LEFT: Byte = 0x04
    const val MODIFIER_WIN_LEFT: Byte = 0x08
    const val MODIFIER_CTRL_RIGHT: Byte = 0x10
    const val MODIFIER_SHIFT_RIGHT: Byte = 0x20
    const val MODIFIER_ALT_RIGHT: Byte = 0x40

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
    const val KEY_ESCAPE: Byte = 0x29
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

    const val KEY_ARROW_UP: Byte = 0x52
    const val KEY_ARROW_DOWN: Byte = 0x51
    const val KEY_ARROW_LEFT: Byte = 0x50
    const val KEY_ARROW_RIGHT: Byte = 0x4F

    /**
     * Build a HID report for a single key press.
     * @param modifier Modifier key mask (0 for none)
     * @param keyCode HID key code
     * @return 8-byte HID report
     */
    fun buildReport(modifier: Byte, keyCode: Byte): ByteArray {
        return byteArrayOf(
            modifier,  // Modifier byte
            0x00,      // Reserved
            keyCode,   // Key 1
            0x00,      // Key 2
            0x00,      // Key 3
            0x00,      // Key 4
            0x00,      // Key 5
            0x00       // Key 6
        )
    }

    /**
     * Build a release report (all zeros).
     * @return 8-byte zero report
     */
    fun buildReleaseReport(): ByteArray {
        return ByteArray(8) { 0x00 }
    }

    /**
     * Convert a character to HID key code and modifier.
     * @param char The character to convert
     * @return Pair of (modifier, keyCode) or null if unsupported
     */
    fun charToHidCode(char: Char): Pair<Byte, Byte>? {
        return when (char) {
            'a' -> Pair(0x00, KEY_A)
            'b' -> Pair(0x00, KEY_B)
            'c' -> Pair(0x00, KEY_C)
            'd' -> Pair(0x00, KEY_D)
            'e' -> Pair(0x00, KEY_E)
            'f' -> Pair(0x00, KEY_F)
            'g' -> Pair(0x00, KEY_G)
            'h' -> Pair(0x00, KEY_H)
            'i' -> Pair(0x00, KEY_I)
            'j' -> Pair(0x00, KEY_J)
            'k' -> Pair(0x00, KEY_K)
            'l' -> Pair(0x00, KEY_L)
            'm' -> Pair(0x00, KEY_M)
            'n' -> Pair(0x00, KEY_N)
            'o' -> Pair(0x00, KEY_O)
            'p' -> Pair(0x00, KEY_P)
            'q' -> Pair(0x00, KEY_Q)
            'r' -> Pair(0x00, KEY_R)
            's' -> Pair(0x00, KEY_S)
            't' -> Pair(0x00, KEY_T)
            'u' -> Pair(0x00, KEY_U)
            'v' -> Pair(0x00, KEY_V)
            'w' -> Pair(0x00, KEY_W)
            'x' -> Pair(0x00, KEY_X)
            'y' -> Pair(0x00, KEY_Y)
            'z' -> Pair(0x00, KEY_Z)

            'A' -> Pair(MODIFIER_SHIFT_LEFT, KEY_A)
            'B' -> Pair(MODIFIER_SHIFT_LEFT, KEY_B)
            'C' -> Pair(MODIFIER_SHIFT_LEFT, KEY_C)
            'D' -> Pair(MODIFIER_SHIFT_LEFT, KEY_D)
            'E' -> Pair(MODIFIER_SHIFT_LEFT, KEY_E)
            'F' -> Pair(MODIFIER_SHIFT_LEFT, KEY_F)
            'G' -> Pair(MODIFIER_SHIFT_LEFT, KEY_G)
            'H' -> Pair(MODIFIER_SHIFT_LEFT, KEY_H)
            'I' -> Pair(MODIFIER_SHIFT_LEFT, KEY_I)
            'J' -> Pair(MODIFIER_SHIFT_LEFT, KEY_J)
            'K' -> Pair(MODIFIER_SHIFT_LEFT, KEY_K)
            'L' -> Pair(MODIFIER_SHIFT_LEFT, KEY_L)
            'M' -> Pair(MODIFIER_SHIFT_LEFT, KEY_M)
            'N' -> Pair(MODIFIER_SHIFT_LEFT, KEY_N)
            'O' -> Pair(MODIFIER_SHIFT_LEFT, KEY_O)
            'P' -> Pair(MODIFIER_SHIFT_LEFT, KEY_P)
            'Q' -> Pair(MODIFIER_SHIFT_LEFT, KEY_Q)
            'R' -> Pair(MODIFIER_SHIFT_LEFT, KEY_R)
            'S' -> Pair(MODIFIER_SHIFT_LEFT, KEY_S)
            'T' -> Pair(MODIFIER_SHIFT_LEFT, KEY_T)
            'U' -> Pair(MODIFIER_SHIFT_LEFT, KEY_U)
            'V' -> Pair(MODIFIER_SHIFT_LEFT, KEY_V)
            'W' -> Pair(MODIFIER_SHIFT_LEFT, KEY_W)
            'X' -> Pair(MODIFIER_SHIFT_LEFT, KEY_X)
            'Y' -> Pair(MODIFIER_SHIFT_LEFT, KEY_Y)
            'Z' -> Pair(MODIFIER_SHIFT_LEFT, KEY_Z)

            '1' -> Pair(0x00, KEY_1)
            '2' -> Pair(0x00, KEY_2)
            '3' -> Pair(0x00, KEY_3)
            '4' -> Pair(0x00, KEY_4)
            '5' -> Pair(0x00, KEY_5)
            '6' -> Pair(0x00, KEY_6)
            '7' -> Pair(0x00, KEY_7)
            '8' -> Pair(0x00, KEY_8)
            '9' -> Pair(0x00, KEY_9)
            '0' -> Pair(0x00, KEY_0)

            '!' -> Pair(MODIFIER_SHIFT_LEFT, KEY_1)
            '@' -> Pair(MODIFIER_SHIFT_LEFT, KEY_2)
            '#' -> Pair(MODIFIER_SHIFT_LEFT, KEY_3)
            '$' -> Pair(MODIFIER_SHIFT_LEFT, KEY_4)
            '%' -> Pair(MODIFIER_SHIFT_LEFT, KEY_5)
            '^' -> Pair(MODIFIER_SHIFT_LEFT, KEY_6)
            '&' -> Pair(MODIFIER_SHIFT_LEFT, KEY_7)
            '*' -> Pair(MODIFIER_SHIFT_LEFT, KEY_8)
            '(' -> Pair(MODIFIER_SHIFT_LEFT, KEY_9)
            ')' -> Pair(MODIFIER_SHIFT_LEFT, KEY_0)

            ' ' -> Pair(0x00, KEY_SPACE)
            '\n' -> Pair(0x00, KEY_ENTER)
            '\t' -> Pair(0x00, KEY_TAB)

            '-' -> Pair(0x00, KEY_MINUS)
            '=' -> Pair(0x00, KEY_EQUAL)
            '[' -> Pair(0x00, KEY_LEFT_BRACKET)
            ']' -> Pair(0x00, KEY_RIGHT_BRACKET)
            '\\' -> Pair(0x00, KEY_BACKSLASH)
            ';' -> Pair(0x00, KEY_SEMICOLON)
            '\'' -> Pair(0x00, KEY_APOSTROPHE)
            '`' -> Pair(0x00, KEY_GRAVE)
            ',' -> Pair(0x00, KEY_COMMA)
            '.' -> Pair(0x00, KEY_PERIOD)
            '/' -> Pair(0x00, KEY_SLASH)

            '_' -> Pair(MODIFIER_SHIFT_LEFT, KEY_MINUS)
            '+' -> Pair(MODIFIER_SHIFT_LEFT, KEY_EQUAL)
            '{' -> Pair(MODIFIER_SHIFT_LEFT, KEY_LEFT_BRACKET)
            '}' -> Pair(MODIFIER_SHIFT_LEFT, KEY_RIGHT_BRACKET)
            '|' -> Pair(MODIFIER_SHIFT_LEFT, KEY_BACKSLASH)
            ':' -> Pair(MODIFIER_SHIFT_LEFT, KEY_SEMICOLON)
            '"' -> Pair(MODIFIER_SHIFT_LEFT, KEY_APOSTROPHE)
            '~' -> Pair(MODIFIER_SHIFT_LEFT, KEY_GRAVE)
            '<' -> Pair(MODIFIER_SHIFT_LEFT, KEY_COMMA)
            '>' -> Pair(MODIFIER_SHIFT_LEFT, KEY_PERIOD)
            '?' -> Pair(MODIFIER_SHIFT_LEFT, KEY_SLASH)

            else -> null
        }
    }
}
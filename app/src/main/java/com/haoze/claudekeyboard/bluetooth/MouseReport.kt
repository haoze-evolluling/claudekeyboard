package com.haoze.claudekeyboard.bluetooth

import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Inline class wrapping a 6-byte HID mouse report.
 * Report format: [reportId(1) + buttons(1) + deltaX(1) + deltaY(1) + wheel(1) + horizontalWheel(1)]
 * Report ID = 2 in combined descriptor.
 */
@JvmInline
value class MouseReport(
    val bytes: ByteArray = ByteArray(6) { 0 }
) {

    init {
        bytes[0] = ID.toByte()
    }

    // ---- Buttons byte (bytes[1]) ----

    var leftButton: Boolean
        get() = bytes[1] and 0x01 != 0.toByte()
        set(value) {
            bytes[1] = if (value)
                bytes[1] or 0x01
            else
                bytes[1] and 0xFE.toByte()
        }

    var rightButton: Boolean
        get() = bytes[1] and 0x02 != 0.toByte()
        set(value) {
            bytes[1] = if (value)
                bytes[1] or 0x02
            else
                bytes[1] and 0xFD.toByte()
        }

    var middleButton: Boolean
        get() = bytes[1] and 0x04 != 0.toByte()
        set(value) {
            bytes[1] = if (value)
                bytes[1] or 0x04
            else
                bytes[1] and 0xFB.toByte()
        }

    // ---- Movement bytes ----

    var deltaX: Byte
        get() = bytes[2]
        set(value) { bytes[2] = value }

    var deltaY: Byte
        get() = bytes[3]
        set(value) { bytes[3] = value }

    var wheel: Byte
        get() = bytes[4]
        set(value) { bytes[4] = value }

    var horizontalWheel: Byte
        get() = bytes[5]
        set(value) { bytes[5] = value }

    /**
     * Reset all fields except Report ID to zero.
     */
    fun reset() {
        bytes[1] = 0  // buttons
        bytes[2] = 0  // deltaX
        bytes[3] = 0  // deltaY
        bytes[4] = 0  // wheel
        bytes[5] = 0  // horizontalWheel
    }

    companion object {
        /** Report ID = 2 for mouse in combined descriptor */
        const val ID = 2

        // Button constants
        const val BUTTON_LEFT = 0
        const val BUTTON_RIGHT = 1
        const val BUTTON_MIDDLE = 2
    }
}

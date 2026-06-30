package com.haoze.claudekeyboard.bluetooth

/**
 * Inline class wrapping a 3-byte HID Consumer Control report.
 * Report format: [reportId(1) + usageLow(1) + usageHigh(1)]
 * Report ID = 3 in the combined descriptor.
 */
@JvmInline
value class ConsumerReport(
    val bytes: ByteArray = ByteArray(3) { 0 }
) {

    init {
        bytes[0] = ID.toByte()
    }

    /**
     * 16-bit consumer usage code (little-endian in the report).
     */
    var usage: Int
        get() = (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
        set(value) {
            val v = value.coerceIn(0x0000, 0xFFFF)
            bytes[1] = (v and 0xFF).toByte()
            bytes[2] = ((v shr 8) and 0xFF).toByte()
        }

    fun reset() {
        bytes.fill(0)
        bytes[0] = ID.toByte()
    }

    companion object {
        /** Report ID = 3 for consumer control in combined descriptor */
        const val ID = 3

        // Common Consumer usages (HID Usage Tables, Usage Page 0x0C)
        const val USAGE_POWER = 0x30
        const val USAGE_AC_HOME = 0x223
        const val USAGE_MUTE = 0xE2
        const val USAGE_VOLUME_UP = 0xE9
        const val USAGE_VOLUME_DOWN = 0xEA
        const val USAGE_AC_SEARCH = 0x221
        const val USAGE_PLAY_PAUSE = 0xCD
        const val USAGE_NEXT = 0xB5
        const val USAGE_PREVIOUS = 0xB6
        const val USAGE_STOP = 0xB7
    }
}

package com.haoze.claudekeyboard.bluetooth

@JvmInline
value class KeyboardReport(
    val bytes: ByteArray = ByteArray(9) { 0 }
) {

    init {
        bytes[0] = ID.toByte()
    }

    var key1: Byte
        get() = bytes[3]
        set(value) { bytes[3] = value }

    fun reset() {
        bytes.fill(0)
        bytes[0] = ID.toByte()
    }

    companion object {
        const val ID = 1
    }
}

package com.haoze.claudekeyboard.bluetooth

/**
 * HID Report Descriptors for Bluetooth HID device.
 * Ported from Kontroller project.
 */
object DescriptorCollection {

    /**
     * Standard keyboard HID descriptor.
     * Report format: [modifier(1) + reserved(1) + keys(6)] = 8 bytes
     * No Report ID (single TLC).
     */
    val KEYBOARD = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),  // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(),  // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(),  // Collection (Application)
        0x05.toByte(), 0x07.toByte(),  //     Usage Page (Key Codes)
        0x19.toByte(), 0xE0.toByte(),  //     Usage Minimum (224)
        0x29.toByte(), 0xE7.toByte(),  //     Usage Maximum (231)
        0x15.toByte(), 0x00.toByte(),  //     Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),  //     Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),  //     Report Size (1)
        0x95.toByte(), 0x08.toByte(),  //     Report Count (8)
        0x81.toByte(), 0x02.toByte(),  //     Input (Data, Variable, Absolute)

        0x95.toByte(), 0x01.toByte(),  //     Report Count (1)
        0x75.toByte(), 0x08.toByte(),  //     Report Size (8)
        0x81.toByte(), 0x01.toByte(),  //     Input (Constant) reserved byte

        0x95.toByte(), 0x05.toByte(),  //     Report Count (5)
        0x75.toByte(), 0x01.toByte(),  //     Report Size (1)
        0x05.toByte(), 0x08.toByte(),  //     Usage Page (Page# for LEDs)
        0x19.toByte(), 0x01.toByte(),  //     Usage Minimum (1)
        0x29.toByte(), 0x05.toByte(),  //     Usage Maximum (5)
        0x91.toByte(), 0x02.toByte(),  //     Output (Data, Variable, Absolute), Led report
        0x95.toByte(), 0x01.toByte(),  //     Report Count (1)
        0x75.toByte(), 0x03.toByte(),  //     Report Size (3)
        0x91.toByte(), 0x01.toByte(),  //     Output (Data, Variable, Absolute), Led report padding

        0x95.toByte(), 0x06.toByte(),  //     Report Count (6)
        0x75.toByte(), 0x08.toByte(),  //     Report Size (8)
        0x15.toByte(), 0x00.toByte(),  //     Logical Minimum (0)
        0x25.toByte(), 0x65.toByte(),  //     Logical Maximum (101)
        0x05.toByte(), 0x07.toByte(),  //     Usage Page (Key codes)
        0x19.toByte(), 0x00.toByte(),  //     Usage Minimum (0)
        0x29.toByte(), 0x65.toByte(),  //     Usage Maximum (101)
        0x81.toByte(), 0x00.toByte(),  //     Input (Data, Array) Key array (6 bytes)
        0xC0.toByte()                  // End Collection (Application)
    )
}

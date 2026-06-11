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

    /**
     * Combined keyboard + mouse HID descriptor.
     * Two TLCs with Report IDs:
     *   - Keyboard: Report ID 1, 8 bytes [modifier + reserved + keys(6)]
     *   - Mouse:    Report ID 2, 5 bytes [buttons + X + Y + wheel + AC Pan]
     */
    val COMBINED = byteArrayOf(
        // ---- Keyboard TLC (Report ID 1) ----
        0x05.toByte(), 0x01.toByte(),  // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(),  // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(),  // Collection (Application)
        0x85.toByte(), 0x01.toByte(),  //     Report ID (1)

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
        0xC0.toByte(),                 // End Collection (Application)

        // ---- Mouse TLC (Report ID 2) ----
        0x05.toByte(), 0x01.toByte(),  // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(),  // Usage (Mouse)
        0xA1.toByte(), 0x01.toByte(),  // Collection (Application)
        0x85.toByte(), 0x02.toByte(),  //     Report ID (2)
        0x09.toByte(), 0x01.toByte(),  //     Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(),  //     Collection (Physical)

        // Buttons
        0x05.toByte(), 0x09.toByte(),  //         Usage Page (Buttons)
        0x19.toByte(), 0x01.toByte(),  //         Usage Minimum (1)
        0x29.toByte(), 0x03.toByte(),  //         Usage Maximum (3)
        0x15.toByte(), 0x00.toByte(),  //         Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),  //         Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),  //         Report Size (1)
        0x95.toByte(), 0x03.toByte(),  //         Report Count (3)
        0x81.toByte(), 0x02.toByte(),  //         Input (Data, Variable, Absolute)

        // Padding (5 bits to fill the byte)
        0x75.toByte(), 0x05.toByte(),  //         Report Size (5)
        0x95.toByte(), 0x01.toByte(),  //         Report Count (1)
        0x81.toByte(), 0x01.toByte(),  //         Input (Constant)

        // X and Y movement
        0x05.toByte(), 0x01.toByte(),  //         Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(),  //         Usage (X)
        0x09.toByte(), 0x31.toByte(),  //         Usage (Y)
        0x15.toByte(), 0x81.toByte(),  //         Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),  //         Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),  //         Report Size (8)
        0x95.toByte(), 0x02.toByte(),  //         Report Count (2)
        0x81.toByte(), 0x06.toByte(),  //         Input (Data, Variable, Relative)

        // Wheel
        0x09.toByte(), 0x38.toByte(),  //         Usage (Wheel)
        0x15.toByte(), 0x81.toByte(),  //         Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),  //         Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),  //         Report Size (8)
        0x95.toByte(), 0x01.toByte(),  //         Report Count (1)
        0x81.toByte(), 0x06.toByte(),  //         Input (Data, Variable, Relative)

        // AC Pan (horizontal scroll)
        0x05.toByte(), 0x0C.toByte(),  //         Usage Page (Consumer)
        0x0A.toByte(), 0x38.toByte(), 0x02.toByte(),  //         Usage (AC Pan)
        0x15.toByte(), 0x81.toByte(),  //         Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),  //         Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),  //         Report Size (8)
        0x95.toByte(), 0x01.toByte(),  //         Report Count (1)
        0x81.toByte(), 0x06.toByte(),  //         Input (Data, Variable, Relative)

        0xC0.toByte(),                 //     End Collection (Physical)
        0xC0.toByte()                  // End Collection (Application)
    )
}

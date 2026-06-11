package com.haoze.claudekeyboard.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.util.Log

/**
 * Sends HID mouse reports via Bluetooth.
 * Shares the same hidDevice connection as KeyboardSender.
 */
class MouseSender(
    val hidDevice: BluetoothHidDevice,
    val host: BluetoothDevice
) {
    val mouseReport = MouseReport()

    /**
     * Send the current mouse report to the host.
     */
    fun sendMouseReport() {
        if (!hidDevice.sendReport(host, MouseReport.ID, mouseReport.bytes.copyOfRange(1, mouseReport.bytes.size))) {
            Log.e(TAG, "Mouse report wasn't sent")
        }
    }

    /**
     * Send a relative mouse movement (no buttons held).
     * @param dx X-axis movement (-127 to 127)
     * @param dy Y-axis movement (-127 to 127)
     */
    fun sendMouseMove(dx: Int, dy: Int) {
        mouseReport.reset()
        mouseReport.deltaX = dx.coerceIn(-127, 127).toByte()
        mouseReport.deltaY = dy.coerceIn(-127, 127).toByte()
        sendMouseReport()
        // Send zero movement to clear
        mouseReport.deltaX = 0
        mouseReport.deltaY = 0
        sendMouseReport()
    }

    /**
     * Send a relative mouse movement while preserving current button state.
     * Used during drag operations where buttons must stay held.
     * @param dx X-axis movement (-127 to 127)
     * @param dy Y-axis movement (-127 to 127)
     */
    fun sendMouseMovePreserveButtons(dx: Int, dy: Int) {
        mouseReport.deltaX = dx.coerceIn(-127, 127).toByte()
        mouseReport.deltaY = dy.coerceIn(-127, 127).toByte()
        sendMouseReport()
        // Clear movement but keep buttons
        mouseReport.deltaX = 0
        mouseReport.deltaY = 0
        sendMouseReport()
    }

    /**
     * Send a mouse button click (press + release).
     * @param button MouseReport.BUTTON_LEFT, BUTTON_RIGHT, or BUTTON_MIDDLE
     */
    fun sendMouseClick(button: Int) {
        mouseReport.reset()
        when (button) {
            MouseReport.BUTTON_LEFT -> mouseReport.leftButton = true
            MouseReport.BUTTON_RIGHT -> mouseReport.rightButton = true
            MouseReport.BUTTON_MIDDLE -> mouseReport.middleButton = true
        }
        sendMouseReport()
        // Release
        mouseReport.reset()
        sendMouseReport()
    }

    /**
     * Send a mouse scroll event.
     * @param amount Scroll amount (positive = up, negative = down)
     */
    fun sendMouseScroll(amount: Int) {
        mouseReport.reset()
        mouseReport.wheel = amount.coerceIn(-127, 127).toByte()
        sendMouseReport()
        // Clear
        mouseReport.reset()
        sendMouseReport()
    }

    /**
     * Send a horizontal mouse scroll event.
     * @param hDelta Horizontal scroll amount (-127 to 127)
     */
    fun sendMouseHScroll(hDelta: Int) {
        mouseReport.reset()
        mouseReport.horizontalWheel = hDelta.coerceIn(-127, 127).toByte()
        sendMouseReport()
        // Clear
        mouseReport.reset()
        sendMouseReport()
    }

    companion object {
        private const val TAG = "MouseSender"
    }
}

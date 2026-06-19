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

    /** Called when a HID report fails to send. Parameter is a human-readable message. */
    var onSendError: ((String) -> Unit)? = null

    /**
     * Send a mouse report using a local report instance (thread-safe).
     */
    private fun sendReportLocal(report: MouseReport) {
        if (!hidDevice.sendReport(host, MouseReport.ID, report.bytes.copyOfRange(1, report.bytes.size))) {
            Log.e(TAG, "Mouse report wasn't sent")
            onSendError?.invoke("Mouse report send failed")
        }
    }

    /**
     * Send a relative mouse movement (no buttons held).
     * Uses local report instances to avoid shared-state race conditions.
     * @param dx X-axis movement (-127 to 127)
     * @param dy Y-axis movement (-127 to 127)
     */
    fun sendMouseMove(dx: Int, dy: Int) {
        val report = MouseReport()
        report.deltaX = dx.coerceIn(-127, 127).toByte()
        report.deltaY = dy.coerceIn(-127, 127).toByte()
        sendReportLocal(report)
        // Send zero movement to clear
        val clear = MouseReport()
        sendReportLocal(clear)
    }

    /**
     * Send a relative mouse movement with explicit button state.
     * Used during drag operations where buttons must stay held.
     * @param dx X-axis movement (-127 to 127)
     * @param dy Y-axis movement (-127 to 127)
     * @param leftButton Whether left button is held
     * @param rightButton Whether right button is held
     * @param middleButton Whether middle button is held
     */
    fun sendMouseMoveWithButtons(dx: Int, dy: Int, leftButton: Boolean = false, rightButton: Boolean = false, middleButton: Boolean = false) {
        val report = MouseReport()
        report.leftButton = leftButton
        report.rightButton = rightButton
        report.middleButton = middleButton
        report.deltaX = dx.coerceIn(-127, 127).toByte()
        report.deltaY = dy.coerceIn(-127, 127).toByte()
        sendReportLocal(report)
        // Clear movement but keep buttons
        val hold = MouseReport()
        hold.leftButton = leftButton
        hold.rightButton = rightButton
        hold.middleButton = middleButton
        sendReportLocal(hold)
    }

    /**
     * Send a mouse button click (press + release).
     * @param button MouseReport.BUTTON_LEFT, BUTTON_RIGHT, or BUTTON_MIDDLE
     */
    fun sendMouseClick(button: Int) {
        val press = MouseReport()
        when (button) {
            MouseReport.BUTTON_LEFT -> press.leftButton = true
            MouseReport.BUTTON_RIGHT -> press.rightButton = true
            MouseReport.BUTTON_MIDDLE -> press.middleButton = true
        }
        sendReportLocal(press)
        // Release
        val release = MouseReport()
        sendReportLocal(release)
    }

    /**
     * Send a mouse scroll event.
     * @param amount Scroll amount (positive = up, negative = down)
     */
    fun sendMouseScroll(amount: Int) {
        val report = MouseReport()
        report.wheel = amount.coerceIn(-127, 127).toByte()
        sendReportLocal(report)
        // Clear
        val clear = MouseReport()
        sendReportLocal(clear)
    }

    /**
     * Send a horizontal mouse scroll event.
     * @param hDelta Horizontal scroll amount (-127 to 127)
     */
    fun sendMouseHScroll(hDelta: Int) {
        val report = MouseReport()
        report.horizontalWheel = hDelta.coerceIn(-127, 127).toByte()
        sendReportLocal(report)
        // Clear
        val clear = MouseReport()
        sendReportLocal(clear)
    }

    companion object {
        private const val TAG = "MouseSender"
    }
}

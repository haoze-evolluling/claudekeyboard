package com.haoze.claudekeyboard.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.util.Log

/**
 * Sends Bluetooth HID reports for TV remote control.
 *
 * Mapping strategy targets Android TV / set-top boxes:
 *   - D-pad and OK use keyboard reports (arrows + Enter) for maximum compatibility.
 *   - System/media functions use Consumer Control reports (volume, mute, power,
 *     home, back, voice assistant) as expected by Android's input system.
 *
 * All HID transmissions run on a background thread and use local report instances
 * to avoid shared-state races.
 */
class TvRemoteSender(
    private val hidDevice: BluetoothHidDevice,
    private val host: BluetoothDevice,
    private val keyboardSender: KeyboardSender
) {

    /** Called when a HID report fails to send. Parameter is a human-readable message. */
    var onSendError: ((String) -> Unit)? = null

    /** D-pad up */
    fun sendUp() = sendKeyboardKey(KeyboardSender.KEY_UP)

    /** D-pad down */
    fun sendDown() = sendKeyboardKey(KeyboardSender.KEY_DOWN)

    /** D-pad left */
    fun sendLeft() = sendKeyboardKey(KeyboardSender.KEY_LEFT)

    /** D-pad right */
    fun sendRight() = sendKeyboardKey(KeyboardSender.KEY_RIGHT)

    /** OK / Select / Enter */
    fun sendConfirm() = sendKeyboardKey(KeyboardSender.KEY_ENTER)

    /** Back (Android TV typically maps keyboard ESC to KEYCODE_BACK) */
    fun sendBack() = sendKeyboardKey(KeyboardSender.KEY_ESC)

    /** Home */
    fun sendHome() = sendConsumer(ConsumerReport.USAGE_AC_HOME)

    /** Voice assistant / search (AC Search is what Google TV remotes send) */
    fun sendAssistant() = sendConsumer(ConsumerReport.USAGE_AC_SEARCH)

    /** Mute toggle */
    fun sendMute() = sendConsumer(ConsumerReport.USAGE_MUTE)

    /** Volume up */
    fun sendVolumeUp() = sendConsumer(ConsumerReport.USAGE_VOLUME_UP)

    /** Volume down */
    fun sendVolumeDown() = sendConsumer(ConsumerReport.USAGE_VOLUME_DOWN)

    /** Power */
    fun sendPower() = sendConsumer(ConsumerReport.USAGE_POWER)

    private fun sendKeyboardKey(keyCode: Byte) {
        Thread {
            keyboardSender.sendKeyPress(0x00, keyCode, PRESS_DURATION_MS)
        }.start()
    }

    private fun sendConsumer(usage: Int) {
        Thread {
            val press = ConsumerReport()
            press.usage = usage
            sendConsumerReport(press)

            Thread.sleep(PRESS_DURATION_MS)

            val release = ConsumerReport()
            sendConsumerReport(release)
        }.start()
    }

    private fun sendConsumerReport(report: ConsumerReport) {
        if (!hidDevice.sendReport(host, ConsumerReport.ID, report.bytes.copyOfRange(1, report.bytes.size))) {
            Log.e(TAG, "Consumer report wasn't sent")
            onSendError?.invoke("Consumer report send failed")
        }
    }

    companion object {
        private const val TAG = "TvRemoteSender"
        private const val PRESS_DURATION_MS = 50L
    }
}

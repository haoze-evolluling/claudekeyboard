package com.haoze.claudekeyboard.ui.tvremote

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.haoze.claudekeyboard.MainActivity
import com.haoze.claudekeyboard.R
import com.haoze.claudekeyboard.bluetooth.BluetoothViewModel
import com.haoze.claudekeyboard.bluetooth.TvRemoteSender
import com.haoze.claudekeyboard.util.performKeyClick

/**
 * TV remote UI fragment.
 *
 * Sends Bluetooth HID reports through [TvRemoteSender]:
 *   - D-pad and OK use keyboard reports (arrows + Enter).
 *   - Back, Home, Assistant, volume, mute and power use Consumer Control reports.
 */
class TvRemoteFragment : Fragment() {

    private val bluetoothViewModel: BluetoothViewModel by activityViewModels()

    private val handler = Handler(Looper.getMainLooper())
    private var ledIndicator: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tvremote, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
    }

    private fun flashLed() {
        ledIndicator?.let { led ->
            led.setBackgroundResource(R.drawable.bg_led_dot_active)
            handler.postDelayed({
                led.setBackgroundResource(R.drawable.bg_led_dot)
            }, 150)
        }
    }

    private fun send(action: TvRemoteSender.() -> Unit) {
        bluetoothViewModel.getTvRemoteSenderDirect()?.action()
    }

    private fun setupViews(root: View) {
        ledIndicator = root.findViewById(R.id.led_indicator)

        val circularDpad = root.findViewById<CircularDpadView>(R.id.circular_dpad)
        circularDpad.onDirectionListener = object : CircularDpadView.OnDirectionListener {
            override fun onDirection(direction: CircularDpadView.Direction) {
                flashLed()
                when (direction) {
                    CircularDpadView.Direction.UP -> send { sendUp() }
                    CircularDpadView.Direction.DOWN -> send { sendDown() }
                    CircularDpadView.Direction.LEFT -> send { sendLeft() }
                    CircularDpadView.Direction.RIGHT -> send { sendRight() }
                }
            }
        }
        circularDpad.onConfirmListener = {
            flashLed()
            send { sendConfirm() }
        }

        val backToHome = root.findViewById<ImageButton>(R.id.btn_back_to_home)
        backToHome.setOnClickListener {
            it.performKeyClick()
            flashLed()
            (activity as? MainActivity)?.navigateToHome()
        }

        root.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            it.performKeyClick(); flashLed(); send { sendBack() }
        }
        root.findViewById<View>(R.id.btn_assistant)?.setOnClickListener {
            it.performKeyClick(); flashLed(); send { sendAssistant() }
        }
        root.findViewById<View>(R.id.btn_home)?.setOnClickListener {
            it.performKeyClick(); flashLed(); send { sendHome() }
        }
        root.findViewById<View>(R.id.btn_mute)?.setOnClickListener {
            it.performKeyClick(); flashLed(); send { sendMute() }
        }
        root.findViewById<View>(R.id.btn_volume_down)?.setOnClickListener {
            it.performKeyClick(); flashLed(); send { sendVolumeDown() }
        }
        root.findViewById<View>(R.id.btn_volume_up)?.setOnClickListener {
            it.performKeyClick(); flashLed(); send { sendVolumeUp() }
        }
        root.findViewById<ImageButton>(R.id.btn_power)?.setOnClickListener {
            it.performKeyClick(); flashLed(); send { sendPower() }
        }

        root.findViewById<View>(R.id.btn_play_pause)?.setOnClickListener {
            it.performKeyClick(); flashLed(); send { sendPlayPause() }
        }
        root.findViewById<View>(R.id.btn_next)?.setOnClickListener {
            it.performKeyClick(); flashLed(); send { sendNext() }
        }
        root.findViewById<View>(R.id.btn_previous)?.setOnClickListener {
            it.performKeyClick(); flashLed(); send { sendPrevious() }
        }
        root.findViewById<View>(R.id.btn_stop)?.setOnClickListener {
            it.performKeyClick(); flashLed(); send { sendStop() }
        }
    }

    fun setTvRemoteEnabled(enabled: Boolean) {
        view?.let { root ->
            root.findViewById<CircularDpadView>(R.id.circular_dpad)?.let { dpad ->
                dpad.dpadEnabled = enabled
                dpad.alpha = if (enabled) 1.0f else 0.4f
            }
            val imageButtons = listOf(
                R.id.btn_back, R.id.btn_assistant,
                R.id.btn_home, R.id.btn_mute, R.id.btn_power,
                R.id.btn_volume_stack,
                R.id.btn_play_pause, R.id.btn_next, R.id.btn_previous, R.id.btn_stop
            )
            imageButtons.forEach { id ->
                root.findViewById<View>(id)?.let { btn ->
                    btn.isEnabled = enabled
                    btn.alpha = if (enabled) 1.0f else 0.4f
                }
            }
        }
    }
}

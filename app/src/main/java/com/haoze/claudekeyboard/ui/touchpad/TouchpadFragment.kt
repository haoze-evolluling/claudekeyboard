package com.haoze.claudekeyboard.ui.touchpad

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.slider.Slider
import com.haoze.claudekeyboard.MainActivity
import com.haoze.claudekeyboard.R
import com.haoze.claudekeyboard.bluetooth.MouseReport
import com.haoze.claudekeyboard.bluetooth.MouseSender
import com.haoze.claudekeyboard.util.performKeyClick
import kotlin.math.sqrt

/**
 * Touchpad fragment for Bluetooth HID mouse input.
 * Supports single-finger cursor movement, tap-to-click, and two-finger scroll.
 */
class TouchpadFragment : Fragment() {

    // Touch tracking state
    private var lastX = 0f
    private var lastY = 0f
    private var startX = 0f
    private var startY = 0f
    private var startTime = 0L
    private var isScrollMode = false
    private var lastPointerCount = 0

    // Sensitivity (1-10, maps to multiplier)
    private var sensitivity = 5

    // Throttle: minimum interval between mouse reports (ms)
    private var lastSendTime = 0L
    private val throttleInterval = 8L  // ~120Hz

    // Tap detection thresholds
    private val tapMaxDistance = 20f  // pixels
    private val tapMaxDuration = 200L  // ms

    // Two-finger scroll tracking
    private var lastTwoFingerY = 0f

    // SharedPreferences key
    private val PREFS_NAME = "touchpad_prefs"
    private val KEY_SENSITIVITY = "sensitivity"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_touchpad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        loadSensitivity()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupViews(root: View) {
        val backToClaude = root.findViewById<TextView>(R.id.tv_back_to_claude)
        val slider = root.findViewById<Slider>(R.id.slider_sensitivity)
        val touchpadArea = root.findViewById<View>(R.id.touchpad_area)
        val btnLeftClick = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_left_click)
        val btnRightClick = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_right_click)

        // Back to Claude button
        backToClaude.setOnClickListener {
            it.performKeyClick()
            (activity as? MainActivity)?.switchToClaudeTab()
        }

        // Sensitivity slider
        slider.addOnChangeListener { _, value, _ ->
            sensitivity = value.toInt()
            saveSensitivity()
        }

        // Touchpad area touch handling
        touchpadArea.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }

        // Left click button
        btnLeftClick.setOnClickListener {
            it.performKeyClick()
            getMouseSender()?.let { sender ->
                Thread { sender.sendMouseClick(MouseReport.BUTTON_LEFT) }.start()
            }
        }

        // Right click button
        btnRightClick.setOnClickListener {
            it.performKeyClick()
            getMouseSender()?.let { sender ->
                Thread { sender.sendMouseClick(MouseReport.BUTTON_RIGHT) }.start()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleTouchEvent(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                startX = event.x
                startY = event.y
                startTime = System.currentTimeMillis()
                isScrollMode = false
                lastPointerCount = 1
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (pointerCount == 2) {
                    // Enter scroll mode
                    isScrollMode = true
                    lastTwoFingerY = (event.getY(0) + event.getY(1)) / 2f
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val now = System.currentTimeMillis()
                if (now - lastSendTime < throttleInterval) return true

                if (isScrollMode && pointerCount >= 2) {
                    // Two-finger scroll
                    val currentY = (event.getY(0) + event.getY(1)) / 2f
                    val dy = lastTwoFingerY - currentY
                    val scrollAmount = (dy * sensitivity / 50).toInt().coerceIn(-5, 5)

                    if (scrollAmount != 0) {
                        getMouseSender()?.let { sender ->
                            Thread { sender.sendMouseScroll(scrollAmount) }.start()
                        }
                        lastTwoFingerY = currentY
                        lastSendTime = now
                    }
                } else if (!isScrollMode && pointerCount == 1) {
                    // Single finger cursor movement
                    val dx = event.x - lastX
                    val dy = event.y - lastY

                    // Apply sensitivity multiplier
                    val multiplier = sensitivity / 5f
                    val moveX = (dx * multiplier).toInt().coerceIn(-127, 127)
                    val moveY = (dy * multiplier).toInt().coerceIn(-127, 127)

                    if (moveX != 0 || moveY != 0) {
                        getMouseSender()?.let { sender ->
                            Thread { sender.sendMouseMove(moveX, moveY) }.start()
                        }
                        lastX = event.x
                        lastY = event.y
                        lastSendTime = now
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (pointerCount == 2) {
                    // Exiting scroll mode
                    isScrollMode = false
                }
            }

            MotionEvent.ACTION_UP -> {
                if (!isScrollMode && pointerCount == 1) {
                    // Check for tap (left click)
                    val dx = event.x - startX
                    val dy = event.y - startY
                    val distance = sqrt(dx * dx + dy * dy)
                    val duration = System.currentTimeMillis() - startTime

                    if (distance < tapMaxDistance && duration < tapMaxDuration) {
                        getMouseSender()?.let { sender ->
                            Thread { sender.sendMouseClick(MouseReport.BUTTON_LEFT) }.start()
                        }
                    }
                }
                isScrollMode = false
            }
        }

        lastPointerCount = pointerCount
        return true
    }

    private fun loadSensitivity() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sensitivity = prefs.getInt(KEY_SENSITIVITY, 5)
        view?.findViewById<Slider>(R.id.slider_sensitivity)?.value = sensitivity.toFloat()
    }

    private fun saveSensitivity() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SENSITIVITY, sensitivity).apply()
    }

    private fun getMouseSender(): MouseSender? {
        return (activity as? MainActivity)?.getMouseSender()
    }

    /**
     * Enable or disable the touchpad (e.g., when connection state changes).
     */
    fun setTouchpadEnabled(enabled: Boolean) {
        view?.let { root ->
            val touchpadArea = root.findViewById<View>(R.id.touchpad_area)
            val btnLeftClick = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_left_click)
            val btnRightClick = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_right_click)

            touchpadArea.isEnabled = enabled
            touchpadArea.alpha = if (enabled) 1.0f else 0.4f
            btnLeftClick.isEnabled = enabled
            btnRightClick.isEnabled = enabled
        }
    }
}

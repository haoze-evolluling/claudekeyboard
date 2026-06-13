package com.haoze.claudekeyboard.ui.touchpad

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
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
 * Supports single-finger cursor movement, tap-to-click, two-finger scroll,
 * and long-press drag (hold left button while moving).
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
    private var scrollEndTime = 0L
    private val scrollCooldown = 100L  // ms to ignore single-finger events after scroll

    // Drag (long-press hold) state
    private var isDragging = false
    private var longPressRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private val longPressDelay = 300L  // ms to trigger drag mode
    private val dragMaxDistance = 30f  // pixels - max movement before canceling long-press

    // Sensitivity (1-10, maps to multiplier)
    private var sensitivity = 5

    // Throttle: minimum interval between mouse reports (ms)
    private var lastSendTime = 0L
    private val throttleInterval = 8L  // ~120Hz

    // Tap detection thresholds
    private val tapMaxDistance = 20f  // pixels
    private val tapMaxDuration = 200L  // ms

    // Two-finger scroll tracking
    private var lastTwoFingerX = 0f
    private var lastTwoFingerY = 0f

    // Two-finger gesture state
    private var isTwoFingerGesture = false
    private var twoFingerStartTime = 0L
    private var twoFingerStartX = floatArrayOf(0f, 0f)
    private var twoFingerStartY = floatArrayOf(0f, 0f)

    // Threshold constants
    private val TWO_FINGER_TAP_TIMEOUT = 300L  // milliseconds
    private val TWO_FINGER_TAP_MAX_DISTANCE = 50f  // pixels
    private val SCROLL_THRESHOLD = 10f  // scroll threshold

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
        val toKeyboard = root.findViewById<TextView>(R.id.tv_to_keyboard)
        val slider = root.findViewById<Slider>(R.id.slider_sensitivity)
        val touchpadArea = root.findViewById<View>(R.id.touchpad_area)
        val btnLeftClick = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_left_click)
        val btnRightClick = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_right_click)

        // Back to Claude button
        backToClaude.setOnClickListener {
            it.performKeyClick()
            (activity as? MainActivity)?.navigateToHome()
        }

        // Go to Keyboard button
        toKeyboard.setOnClickListener {
            it.performKeyClick()
            (activity as? MainActivity)?.switchToKeyboardTab()
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
                // Ignore touch events during scroll cooldown
                if (System.currentTimeMillis() - scrollEndTime < scrollCooldown) return true

                lastX = event.x
                lastY = event.y
                startX = event.x
                startY = event.y
                startTime = System.currentTimeMillis()
                isScrollMode = false
                isDragging = false
                lastPointerCount = 1

                // Start long-press timer for drag mode
                longPressRunnable?.let { handler.removeCallbacks(it) }
                longPressRunnable = Runnable {
                    // Long press triggered → enter drag mode (hold left button)
                    if (!isScrollMode) {
                        isDragging = true
                        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        getMouseSender()?.let { sender ->
                            Thread {
                                sender.mouseReport.reset()
                                sender.mouseReport.leftButton = true
                                sender.sendMouseReport()
                            }.start()
                        }
                    }
                }
                handler.postDelayed(longPressRunnable!!, longPressDelay)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (pointerCount == 2) {
                    // Cancel long-press / drag, enter scroll mode
                    cancelLongPress()
                    if (isDragging) {
                        releaseDrag()
                    }
                    isScrollMode = true
                    view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    lastTwoFingerX = (event.getX(0) + event.getX(1)) / 2f
                    lastTwoFingerY = (event.getY(0) + event.getY(1)) / 2f

                    // Track two-finger gesture start
                    isTwoFingerGesture = true
                    twoFingerStartTime = SystemClock.uptimeMillis()
                    twoFingerStartX[0] = event.getX(0)
                    twoFingerStartY[0] = event.getY(0)
                    twoFingerStartX[1] = event.getX(1)
                    twoFingerStartY[1] = event.getY(1)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val now = System.currentTimeMillis()

                // If finger moved too far before long-press triggers, cancel it
                if (!isDragging && !isScrollMode && pointerCount == 1) {
                    val dx = event.x - startX
                    val dy = event.y - startY
                    val distance = sqrt(dx * dx + dy * dy)
                    if (distance > dragMaxDistance) {
                        cancelLongPress()
                    }
                }

                if (now - lastSendTime < throttleInterval) return true

                if (isScrollMode && pointerCount >= 2) {
                    // Two-finger scroll
                    val currentX = (event.getX(0) + event.getX(1)) / 2f
                    val currentY = (event.getY(0) + event.getY(1)) / 2f
                    val dx = currentX - lastTwoFingerX
                    val dy = currentY - lastTwoFingerY

                    // Vertical scroll (natural: swipe up = dy negative = scroll down)
                    val vScrollAmount = (dy * sensitivity / 50).toInt().coerceIn(-5, 5)

                    // Horizontal scroll (inverted: swipe left = scroll right, swipe right = scroll left)
                    val hScrollAmount = (-dx * sensitivity / 50).toInt().coerceIn(-5, 5)

                    if (vScrollAmount != 0 || hScrollAmount != 0) {
                        getMouseSender()?.let { sender ->
                            Thread {
                                if (vScrollAmount != 0) sender.sendMouseScroll(vScrollAmount)
                                if (hScrollAmount != 0) sender.sendMouseHScroll(hScrollAmount)
                            }.start()
                        }
                        lastTwoFingerX = currentX
                        lastTwoFingerY = currentY
                        lastSendTime = now
                    }
                } else if (!isScrollMode && pointerCount == 1
                    && System.currentTimeMillis() - scrollEndTime >= scrollCooldown) {
                    // Single finger cursor movement (or drag movement)
                    val dx = event.x - lastX
                    val dy = event.y - lastY

                    // Apply sensitivity multiplier
                    val multiplier = sensitivity / 5f
                    val moveX = (dx * multiplier).toInt().coerceIn(-127, 127)
                    val moveY = (dy * multiplier).toInt().coerceIn(-127, 127)

                    if (moveX != 0 || moveY != 0) {
                        getMouseSender()?.let { sender ->
                            Thread {
                                if (isDragging) {
                                    sender.sendMouseMovePreserveButtons(moveX, moveY)
                                } else {
                                    sender.sendMouseMove(moveX, moveY)
                                }
                            }.start()
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
                    scrollEndTime = System.currentTimeMillis()

                    // Detect two-finger tap (right click)
                    var isTap = false
                    if (isTwoFingerGesture) {
                        val duration = SystemClock.uptimeMillis() - twoFingerStartTime
                        val dx = event.getX(0) - twoFingerStartX[0]
                        val dy = event.getY(0) - twoFingerStartY[0]
                        val distance = sqrt(dx * dx + dy * dy)

                        if (duration < TWO_FINGER_TAP_TIMEOUT && distance < TWO_FINGER_TAP_MAX_DISTANCE) {
                            isTap = true
                            // Two-finger tap detected - send right click
                            getMouseSender()?.let { sender ->
                                Thread { sender.sendMouseClick(MouseReport.BUTTON_RIGHT) }.start()
                            }
                            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    }
                    // Haptic feedback for scroll lift (skip if already triggered by tap)
                    if (!isTap) {
                        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    isTwoFingerGesture = false
                }
            }

            MotionEvent.ACTION_UP -> {
                cancelLongPress()

                if (isDragging) {
                    // Release drag (release left button)
                    releaseDrag()
                } else if (!isScrollMode && pointerCount == 1) {
                    // Check for tap (left click)
                    val dx = event.x - startX
                    val dy = event.y - startY
                    val distance = sqrt(dx * dx + dy * dy)
                    val duration = System.currentTimeMillis() - startTime

                    if (distance < tapMaxDistance && duration < tapMaxDuration) {
                        getMouseSender()?.let { sender ->
                            Thread { sender.sendMouseClick(MouseReport.BUTTON_LEFT) }.start()
                        }
                        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                }
                isScrollMode = false
                isTwoFingerGesture = false
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelLongPress()
                if (isDragging) releaseDrag()
                isScrollMode = false
            }
        }

        lastPointerCount = pointerCount
        return true
    }

    private fun cancelLongPress() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        longPressRunnable = null
    }

    private fun releaseDrag() {
        isDragging = false
        getMouseSender()?.let { sender ->
            Thread {
                sender.mouseReport.reset()
                sender.sendMouseReport()
            }.start()
        }
    }

    private fun calculateTwoFingerDistance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt(dx * dx + dy * dy)
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

package com.haoze.claudekeyboard.ui.tvremote

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.R as MaterialR
import com.haoze.claudekeyboard.R
import com.haoze.claudekeyboard.util.performKeyClick
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class CircularDpadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Direction { UP, DOWN, LEFT, RIGHT }

    interface OnDirectionListener {
        fun onDirection(direction: Direction)
    }

    var onDirectionListener: OnDirectionListener? = null
    var onConfirmListener: (() -> Unit)? = null
    var repeatDelay: Long = 200L
    var dpadEnabled: Boolean = true

    private var outerRadius: Float = 0f
    private var innerRadius: Float = 0f
    private var ringColor: Int = 0
    private var ringBorderColor: Int = 0
    private var centerColor: Int = 0
    private var centerBorderColor: Int = 0
    private var dividerColor: Int = 0
    private var iconColor: Int = 0
    private var textColor: Int = 0

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val ringRect = RectF()
    private val centerRect = RectF()

    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var currentDirection: Direction? = null
    private var pressedDirection: Direction? = null
    private var isCenterPressed: Boolean = false

    private var repeatRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private val iconUp: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_nav_up)
    private val iconDown: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_nav_down)
    private val iconLeft: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_nav_left)
    private val iconRight: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_nav_right)
    private val centerText: String = context.resources.getString(R.string.tvremote_ok)

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.CircularDpadView, defStyleAttr, 0)
            outerRadius = a.getDimension(R.styleable.CircularDpadView_outerRadius, 90f)
            innerRadius = a.getDimension(R.styleable.CircularDpadView_innerRadius, 32f)
            repeatDelay = a.getInteger(R.styleable.CircularDpadView_repeatDelay, 200).toLong()
            ringColor = a.getColor(R.styleable.CircularDpadView_ringColor, resolveColor(MaterialR.attr.colorSurfaceVariant, Color.parseColor("#E8E8E8")))
            ringBorderColor = a.getColor(R.styleable.CircularDpadView_ringBorderColor, resolveColor(MaterialR.attr.colorOutlineVariant, Color.parseColor("#D0D0D0")))
            centerColor = a.getColor(R.styleable.CircularDpadView_centerColor, resolveColor(MaterialR.attr.colorSurface, Color.parseColor("#E8E8E8")))
            centerBorderColor = a.getColor(R.styleable.CircularDpadView_centerBorderColor, resolveColor(MaterialR.attr.colorOutline, Color.parseColor("#D0D0D0")))
            dividerColor = a.getColor(R.styleable.CircularDpadView_dividerColor, resolveColor(MaterialR.attr.colorOutlineVariant, Color.parseColor("#D0D0D0")))
            iconColor = a.getColor(R.styleable.CircularDpadView_iconColor, resolveColor(MaterialR.attr.colorOnSurface, Color.parseColor("#6A6E7C")))
            textColor = a.getColor(R.styleable.CircularDpadView_textColor, textColor)
            a.recycle()
        } ?: run {
            ringColor = resolveColor(MaterialR.attr.colorSurfaceVariant, Color.parseColor("#E8E8E8"))
            ringBorderColor = resolveColor(MaterialR.attr.colorOutlineVariant, Color.parseColor("#D0D0D0"))
            centerColor = resolveColor(MaterialR.attr.colorSurface, Color.parseColor("#E8E8E8"))
            centerBorderColor = resolveColor(MaterialR.attr.colorOutline, Color.parseColor("#D0D0D0"))
            dividerColor = resolveColor(MaterialR.attr.colorOutlineVariant, Color.parseColor("#D0D0D0"))
            iconColor = resolveColor(MaterialR.attr.colorOnSurface, Color.parseColor("#6A6E7C"))
        }

        if (textColor == 0) {
            val tv = TypedValue()
            context.theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)
            textColor = tv.data
        }

        setupPaints()
        setupIcons()
    }

    private fun resolveColor(attr: Int, fallback: Int): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) ContextCompat.getColor(context, tv.resourceId) else tv.data
        } else {
            fallback
        }
    }

    private fun setupPaints() {
        ringPaint.apply {
            color = ringColor
            style = Paint.Style.FILL
        }
        ringBorderPaint.apply {
            color = ringBorderColor
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        centerPaint.apply {
            color = centerColor
            style = Paint.Style.FILL
        }
        centerBorderPaint.apply {
            color = centerBorderColor
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        dividerPaint.apply {
            color = dividerColor
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        highlightPaint.apply {
            color = (ringBorderColor and 0x00FFFFFF) or 0x33000000
            style = Paint.Style.FILL
        }
        textPaint.apply {
            color = textColor
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 22f, resources.displayMetrics
            )
            textAlign = Paint.Align.CENTER
        }
    }

    private fun setupIcons() {
        val iconSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics
        ).toInt()
        val halfSize = iconSizePx / 2
        listOf(iconUp, iconDown, iconLeft, iconRight).forEach { icon ->
            icon?.setTint(iconColor)
            icon?.setBounds(-halfSize, -halfSize, halfSize, halfSize)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (outerRadius * 2).toInt()
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f

        val strokeInset = ringBorderPaint.strokeWidth / 2f
        outerRadius = min(w, h) / 2f - strokeInset
        ringRect.set(strokeInset, strokeInset, w - strokeInset, h - strokeInset)
        centerRect.set(
            centerX - innerRadius,
            centerY - innerRadius,
            centerX + innerRadius,
            centerY + innerRadius
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawOval(ringRect, ringPaint)
        canvas.drawOval(ringRect, ringBorderPaint)

        // 高亮按下的方向扇区
        pressedDirection?.let { dir ->
            val startAngle = when (dir) {
                Direction.RIGHT -> 315f
                Direction.DOWN -> 45f
                Direction.LEFT -> 135f
                Direction.UP -> 225f
            }
            drawSegmentHighlight(canvas, startAngle, 90f)
        }

        canvas.save()
        canvas.rotate(45f, centerX, centerY)
        canvas.drawLine(centerX, centerY - outerRadius, centerX, centerY + outerRadius, dividerPaint)
        canvas.restore()

        canvas.save()
        canvas.rotate(135f, centerX, centerY)
        canvas.drawLine(centerX, centerY - outerRadius, centerX, centerY + outerRadius, dividerPaint)
        canvas.restore()

        canvas.drawOval(centerRect, centerPaint)
        if (isCenterPressed) {
            canvas.drawOval(centerRect, highlightPaint)
        }
        canvas.drawOval(centerRect, centerBorderPaint)

        val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(centerText, centerX, textY, textPaint)

        drawIcon(canvas, iconUp, 90f)
        drawIcon(canvas, iconDown, 270f)
        drawIcon(canvas, iconLeft, 180f)
        drawIcon(canvas, iconRight, 0f)
    }

    private fun drawIcon(canvas: Canvas, icon: Drawable?, angleDeg: Float) {
        icon ?: return
        val midRadius = (outerRadius + innerRadius) / 2
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val iconX = centerX + midRadius * cos(angleRad).toFloat()
        val iconY = centerY - midRadius * sin(angleRad).toFloat()

        canvas.save()
        canvas.translate(iconX, iconY)
        icon.draw(canvas)
        canvas.restore()
    }

    private fun drawSegmentHighlight(canvas: Canvas, startAngle: Float, sweepAngle: Float) {
        val path = Path()
        path.arcTo(ringRect, startAngle, sweepAngle)
        path.arcTo(centerRect, startAngle + sweepAngle, -sweepAngle)
        path.close()
        canvas.drawPath(path, highlightPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!dpadEnabled) return false

        val x = event.x - centerX
        val y = event.y - centerY
        val distance = sqrt(x * x + y * y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                performKeyClick()

                if (distance < innerRadius) {
                    isCenterPressed = true
                    invalidate()
                    onConfirmListener?.invoke()
                    return true
                }

                if (distance <= outerRadius) {
                    currentDirection = calculateDirection(x, y)
                    pressedDirection = currentDirection
                    invalidate()
                    currentDirection?.let { dir ->
                        onDirectionListener?.onDirection(dir)
                        startRepeat(dir)
                    }
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // 只处理移出区域的情况，不切换方向
                if (distance > outerRadius) {
                    stopRepeat()
                    currentDirection = null
                    pressedDirection = null
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                currentDirection = null
                pressedDirection = null
                isCenterPressed = false
                stopRepeat()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun calculateDirection(x: Float, y: Float): Direction {
        var angle = Math.toDegrees(atan2(y, x).toDouble())
        if (angle < 0) angle += 360.0

        return when {
            angle > 315 || angle <= 45 -> Direction.RIGHT
            angle > 45 && angle <= 135 -> Direction.DOWN
            angle > 135 && angle <= 225 -> Direction.LEFT
            else -> Direction.UP
        }
    }

    private fun startRepeat(direction: Direction) {
        stopRepeat()
        repeatRunnable = object : Runnable {
            override fun run() {
                onDirectionListener?.onDirection(direction)
                handler.postDelayed(this, repeatDelay)
            }
        }
        handler.postDelayed(repeatRunnable!!, 500L)
    }

    private fun stopRepeat() {
        repeatRunnable?.let { handler.removeCallbacks(it) }
        repeatRunnable = null
    }
}

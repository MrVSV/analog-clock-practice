package com.vsv.analogclock

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.toRectF
import com.vsv.analogclock.extentions.dpToPx
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.properties.Delegates

class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_SIZE = 300
        private const val DEFAULT_BORDER_WIDTH = 10f
        private const val DEFAULT_BORDER_COLOR = Color.BLACK
        private const val DEFAULT_HOURS_HAND_COLOR = Color.BLACK
        private const val DEFAULT_MINUTES_HAND_COLOR = Color.BLACK
        private const val DEFAULT_SECONDS_HAND_COLOR = Color.RED
        private const val DEFAULT_HOURS_NUMBERS_COLOR = Color.BLACK
    }

    @ColorInt
    private var borderColor = Color.BLACK

    @ColorInt
    private var hoursNumbersColor = Color.BLACK

    @ColorInt
    private var hoursHandColor = Color.BLACK

    @ColorInt
    private var minutesHandColor = Color.BLACK

    @ColorInt
    private var secondsHandColor = Color.RED

    private val borderWidth = DEFAULT_BORDER_WIDTH

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hoursMarksPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val minutesMarksPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hoursTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hoursHandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val minutesHandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val secondsHandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val viewRect = Rect()
    private var viewCenterX by Delegates.notNull<Float>()
    private var viewCenterY by Delegates.notNull<Float>()
    private var size by Delegates.notNull<Int>()
    private var borderRadius by Delegates.notNull<Float>()
    private var centerRadius by Delegates.notNull<Float>()
    private val hours = (1..12).toList()
    private val minutes = (0..59).toList()
    private var timeList: MutableList<Double> =
        mutableListOf(0.toDouble(), 3.toDouble(), 4.toDouble())
    private var handTruncation by Delegates.notNull<Int>()
    private var hourHandTruncation by Delegates.notNull<Int>()
    private var hoursTextSize by Delegates.notNull<Float>()
    private val hoursTextRect = Rect()
    private var moverX by Delegates.notNull<Float>()
    private val list = mutableListOf<Int>()
    private var angle: Double = 0.0
    private var minuteText = 0

    private val myListener =
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
        }

    private val detector: GestureDetector = GestureDetector(context, myListener)

    init {
        attrs?.let { setAttrs(it) }
        setup()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        if (!detector.onTouchEvent(event)) {
            if (y in (viewRect.bottom - 40).toFloat()..(viewRect.bottom).toFloat()) {
                if (x in (list.first().toFloat()..list.last().toFloat())) {
                    moverX = x
                    list.forEachIndexed { index, mark ->
                        if (mark in (moverX.toInt() - 10)..(moverX.toInt() + 10)) {
                            timeList[0] = index.toDouble()
                        }
                    }
                    invalidate()
                }
            } else if (y in (viewCenterY - borderRadius)..(viewCenterY + borderRadius) &&
                x in (viewCenterX - borderRadius)..(viewCenterX + borderRadius)
            ) {

                angle =
                    if((x-viewCenterX) > 0) atan(((y-viewCenterY)/(x-viewCenterX)).toDouble())
                else atan(((y-viewCenterY)/(x-viewCenterX)).toDouble())+Math.PI
                minuteText = (angle / Math.PI * 30).roundToInt()+15
                if(minuteText == 60) minuteText = 0
                invalidate()
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = context.dpToPx(DEFAULT_SIZE).toInt() + paddingStart + paddingEnd
        val desiredHeight = context.dpToPx(DEFAULT_SIZE).toInt() + paddingTop + paddingBottom
        val newMeasuredWidth = resolveSize(desiredWidth, widthMeasureSpec)
        val newMeasuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(newMeasuredWidth, newMeasuredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0) return
        with(viewRect) {
            left = 0
            top = 0
            right = w
            bottom = h
        }
        prepareSize(w, h)

    }

    override fun onDraw(canvas: Canvas) {
        drawBorderAndCenter(canvas)
        drawHours(canvas)
        drawMinutes(canvas)
//        drawHandLine(canvas, (timeList[0] + timeList[1] / 60) * 5, isHour = true, isSecond = false)
        drawHandLine(canvas, timeList[1] + timeList[2] / 60, isHour = false, isSecond = false)
//        drawHandLine(canvas, timeList[2], isHour = false, isSecond = true)
//        drawSlider(canvas)
        drawMinutesText(canvas)
    }

    private fun drawMinutesText(canvas: Canvas) {
        with(hoursTextPaint){
            textSize = hoursTextSize
        }
        canvas.drawText("minutes: $minuteText", viewCenterX, (viewRect.bottom-40).toFloat(), hoursTextPaint)

    }

    private fun setAttrs(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AnalogClockView)
        borderColor =
            typedArray.getColor(R.styleable.AnalogClockView_borderColor, DEFAULT_BORDER_COLOR)
        hoursNumbersColor = typedArray.getColor(
            R.styleable.AnalogClockView_hoursNumbersColor,
            DEFAULT_HOURS_NUMBERS_COLOR
        )
        hoursHandColor = typedArray.getColor(
            R.styleable.AnalogClockView_hoursHandColor,
            DEFAULT_HOURS_HAND_COLOR
        )
        minutesHandColor = typedArray.getColor(
            R.styleable.AnalogClockView_minutesHandColor,
            DEFAULT_MINUTES_HAND_COLOR
        )
        secondsHandColor = typedArray.getColor(
            R.styleable.AnalogClockView_secondsHandColor,
            DEFAULT_SECONDS_HAND_COLOR
        )
        typedArray.recycle()
    }

    private fun setup() {
        with(borderPaint) {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
        }
        with(centerPaint) {
            color = borderColor
            style = Paint.Style.FILL
        }
        with(hoursMarksPaint) {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
        }
        with(minutesMarksPaint) {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = borderWidth / 2
        }
        with(hoursTextPaint) {
            color = hoursNumbersColor
        }
        with(hoursHandPaint) {
            color = hoursHandColor
            strokeWidth = borderWidth
        }
        with(minutesHandPaint) {
            color = minutesHandColor
            strokeWidth = borderWidth / 2
        }
        with(secondsHandPaint) {
            color = secondsHandColor
            strokeWidth = borderWidth / 3
        }
    }

    private fun prepareSize(w: Int, h: Int) {
        size = min(w - paddingStart - paddingEnd, h - paddingTop - paddingBottom)
        borderRadius = size / 2f - borderWidth / 2 - 100
        viewCenterX = viewRect.toRectF().centerX() - paddingEnd / 2 + paddingStart / 2
        viewCenterY = viewRect.toRectF().centerY() - paddingBottom / 2 + paddingTop / 2
        centerRadius = borderRadius / 20
        hoursTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            borderRadius / 20,
            resources.displayMetrics
        )
        handTruncation = (size / 10)
        hourHandTruncation = (size / 17)

        val mark = (viewRect.width() - 75) / 12
        for (i in (1..12)) {
            list.add(i * mark)
        }
        moverX = list.first().toFloat()
    }

    private fun drawSlider(canvas: Canvas) {
        canvas.drawLine(
            (list.first()).toFloat(),
            (viewRect.bottom - 20).toFloat(),
            (list.last()).toFloat(),
            (viewRect.bottom - 20).toFloat(),
            minutesHandPaint
        )
        list.forEach { mark ->
            canvas.drawLine(
                mark.toFloat(),
                (viewRect.bottom - 10).toFloat(),
                mark.toFloat(),
                (viewRect.bottom - 30).toFloat(),
                minutesMarksPaint
            )
        }
        canvas.drawCircle(moverX, (viewRect.bottom - 20).toFloat(), 20f, centerPaint)
    }

    private fun drawHandLine(canvas: Canvas, moment: Double, isHour: Boolean, isSecond: Boolean) {
//        angle = Math.PI * (moment - 15) / 30
        val handRadius: Float =
            if (isHour) borderRadius - handTruncation - hourHandTruncation
            else borderRadius - handTruncation
        val handPaint =
            if (isHour) hoursHandPaint
            else if (isSecond) secondsHandPaint
            else minutesHandPaint
        canvas.drawLine(
            viewCenterX,
            viewCenterY,
            ((viewCenterX + cos(angle) * handRadius).toFloat()),
            ((viewCenterY + sin(angle) * handRadius).toFloat()),
            handPaint
        )
    }

    private fun drawMinutes(canvas: Canvas) {
        minutes.forEach { minute ->
            val angle: Double = Math.PI * minute.toDouble() / 30
            val startX =
                (viewCenterX + cos(angle) * (borderRadius - borderWidth / 2) * 0.95).toFloat()
            val startY =
                (viewCenterY + sin(angle) * (borderRadius - borderWidth / 2) * 0.95).toFloat()
            val endX = (viewCenterX + cos(angle) * (borderRadius - borderWidth / 2)).toFloat()
            val endY = (viewCenterY + sin(angle) * (borderRadius - borderWidth / 2)).toFloat()
            canvas.drawLine(startX, startY, endX, endY, minutesMarksPaint)
        }
    }

    private fun drawHours(canvas: Canvas) {
        hours.forEach { hour ->
            val tmp = hour.toString()
            with(hoursTextPaint) {
                textSize = hoursTextSize
                getTextBounds(tmp, 0, tmp.length, hoursTextRect)
            }
            val angle = Math.PI * (hour - 3).toDouble() / 6
            val startX =
                (viewCenterX + cos(angle) * (borderRadius - borderWidth / 2) * 0.9).toFloat()
            val startY =
                (viewCenterY + sin(angle) * (borderRadius - borderWidth / 2) * 0.9).toFloat()
            val endX = (viewCenterX + cos(angle) * (borderRadius - borderWidth / 2)).toFloat()
            val endY = (viewCenterY + sin(angle) * (borderRadius - borderWidth / 2)).toFloat()
            val textX =
                (viewCenterX + cos(angle) * (borderRadius - borderWidth / 2) * 0.8 - hoursTextRect.width() / 2).toFloat()
            val textY =
                (viewCenterY + sin(angle) * (borderRadius - borderWidth / 2) * 0.8 + hoursTextRect.height() / 2).toFloat()
            canvas.drawLine(startX, startY, endX, endY, hoursMarksPaint)
            canvas.drawText(tmp, textX, textY, hoursTextPaint)
        }
    }

    private fun drawBorderAndCenter(canvas: Canvas) {
        canvas.drawCircle(viewCenterX, viewCenterY, borderRadius, borderPaint)
        canvas.drawCircle(viewCenterX, viewCenterY, centerRadius, centerPaint)
    }

//    fun setTime() {
//        val calendar = Calendar.getInstance()
//        var hour = calendar.get(Calendar.HOUR_OF_DAY).toDouble()
//        hour = if (hour > 12) hour - 12 else hour
//        val minute = calendar.get(Calendar.MINUTE).toDouble()
//        val second = calendar.get(Calendar.SECOND).toDouble()
//        timeList = listOf(hour, minute, second)
//        invalidate()
//    }
}
package com.vsv.analogclock

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val mPaint: Paint = Paint()
    private var viewWidth =0
    private var viewHeight =0
    private var mHeight: Float = 0f
    private var mWidth: Float = 0f
    private var mRadius: Float = 0f
    private val mHours: List<Int> = (1..12).toList()
    private val mMinutes: List<Int> = (1..60).toList()
    private var mHandTruncation: Int = 0
    private var mHourHandTruncation: Int = 0
    private val mRect: Rect = Rect()
    private var timeList: List<Double> = listOf(0.toDouble(), 0.toDouble(), 0.toDouble())
    private var size: Int = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)


        mWidth = width.toFloat()
        mHeight = height.toFloat()
        size = min(mWidth, mHeight).toInt()
        mRadius = size / 2f
        mHandTruncation = (size / 20).toInt()
        mHourHandTruncation = (size / 17).toInt()

        mPaint.apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = 5f
        }
        canvas.drawCircle(mWidth / 2f, mHeight / 2f, mRadius, mPaint)

        mPaint.apply {
            style = Paint.Style.FILL
        }
        canvas.drawCircle(mWidth / 2f, mHeight / 2f, 15f, mPaint)

        val fontSize =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)

        mPaint.textSize = fontSize

        mHours.forEach { hour ->
            val tmp = hour.toString()
            mPaint.getTextBounds(tmp, 0, tmp.length, mRect)
            val angle = Math.PI * (hour - 3).toDouble() / 6
            val startX = (mWidth / 2f + cos(angle) * mRadius * 0.9).toFloat()
            val startY = (mHeight / 2f + sin(angle) * mRadius * 0.9).toFloat()
            val endX = (mWidth / 2f + cos(angle) * mRadius).toFloat()
            val endY = (mHeight / 2f + sin(angle) * mRadius).toFloat()
            val textX = (mWidth / 2f + cos(angle) * mRadius * 0.8 - mRect.width() / 2).toFloat()
            val textY = (mHeight / 2f + sin(angle) * mRadius * 0.8 + mRect.height() / 2).toFloat()
            canvas.drawLine(startX, startY, endX, endY, mPaint)
            canvas.drawText(tmp, textX, textY, mPaint)
        }

        mMinutes.forEach { minute ->
            val angle: Double = Math.PI * minute.toDouble() / 30
            val startX = (mWidth / 2f + cos(angle) * mRadius * 0.95).toFloat()
            val startY = (mHeight / 2f + sin(angle) * mRadius * 0.95).toFloat()
            val endX = (mWidth / 2f + cos(angle) * mRadius).toFloat()
            val endY = (mHeight / 2f + sin(angle) * mRadius).toFloat()
            canvas.drawLine(startX, startY, endX, endY, mPaint)
        }

        drawHandLine(canvas, (timeList[0] + timeList[1] / 60) * 5, isHour = true, isSecond = false)
        drawHandLine(canvas, timeList[1] + timeList[2] / 60, isHour = false, isSecond = false)
        drawHandLine(canvas, timeList[2], isHour = false, isSecond = true)
        postInvalidateDelayed(500)
        invalidate()
    }

    private fun drawHandLine(
        canvas: Canvas,
        moment: Double,
        isHour: Boolean,
        isSecond: Boolean,
    ) {
        val angle: Double = Math.PI * (moment - 15) / 30
        val handRadius: Float = if (isHour) {
            mRadius - mHandTruncation - mHourHandTruncation
        } else mRadius - mHandTruncation
        if (isSecond) mPaint.color = Color.RED
        canvas.drawLine(
            mWidth / 2f,
            mHeight / 2f,
            ((mWidth / 2f + cos(angle) * handRadius).toFloat()),
            ((mHeight / 2f + sin(angle) * handRadius).toFloat()),
            mPaint
        )
    }

    fun setTime() {
        val calendar = Calendar.getInstance()
        var hour = calendar.get(Calendar.HOUR_OF_DAY).toDouble()
        hour = if (hour > 12) hour - 12 else hour
        val minute = calendar.get(Calendar.MINUTE).toDouble()
        val second = calendar.get(Calendar.SECOND).toDouble()
        timeList = listOf(hour, minute, second)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val desiredWidth = 800
        val desiredHeight = 800
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingEnd
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom

        Log.d(TAG, "w: ${MeasureSpec.toString(widthMeasureSpec)}")
        Log.d(TAG, "h: ${MeasureSpec.toString(heightMeasureSpec)}")
        Log.d(TAG, "onMeasure: w: $widthSize, h: $heightSize")

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }
        viewWidth = width+paddingStart+paddingEnd
        viewHeight = height+paddingTop+paddingBottom
        Log.d(TAG, "onMeasure: width: $width, height: $height")
        setMeasuredDimension(viewWidth, viewHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged: $paddingStart, $paddingEnd, $paddingTop, $paddingBottom")
        mWidth = viewWidth.toFloat() - paddingStart - paddingEnd
        mHeight = viewHeight.toFloat() - paddingTop - paddingBottom
        Log.d(TAG, "onSizeChanged: $mWidth, $mHeight")
    }

}
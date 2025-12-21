package com.example.imagestitcher.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.example.imagestitcher.model.Orientation

class CropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    var cropStart: Float = 0f
        set(value) {
            field = value.coerceIn(0f, cropEnd - 0.01f)
            invalidate()
        }

    var cropEnd: Float = 1f
        set(value) {
            field = value.coerceIn(cropStart + 0.01f, 1f)
            invalidate()
        }

    var orientation: Orientation = Orientation.VERTICAL
        set(value) {
            field = value
            invalidate()
        }

    var showCropSliders: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var onCropChanged: ((Float, Float) -> Unit)? = null

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#80000000")
        style = Paint.Style.FILL
    }

    private val sliderPaint = Paint().apply {
        color = Color.parseColor("#FF4081")
        style = Paint.Style.FILL
    }

    private val sliderStrokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val sliderSize = 60f
    private val sliderWidth = 40f

    private var draggingSlider: Slider? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private enum class Slider {
        START, END
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!showCropSliders || drawable == null) return

        when (orientation) {
            Orientation.VERTICAL -> drawVerticalSliders(canvas)
            Orientation.HORIZONTAL -> drawHorizontalSliders(canvas)
        }
    }

    private fun drawVerticalSliders(canvas: Canvas) {
        val height = height.toFloat()
        val width = width.toFloat()

        val startY = height * cropStart
        val endY = height * cropEnd

        // 绘制遮罩
        if (cropStart > 0) {
            canvas.drawRect(0f, 0f, width, startY, overlayPaint)
        }
        if (cropEnd < 1f) {
            canvas.drawRect(0f, endY, width, height, overlayPaint)
        }

        // 绘制上滑块
        val startSliderRect = RectF(
            width / 2 - sliderWidth / 2,
            startY - sliderSize / 2,
            width / 2 + sliderWidth / 2,
            startY + sliderSize / 2
        )
        canvas.drawRoundRect(startSliderRect, 8f, 8f, sliderPaint)
        canvas.drawRoundRect(startSliderRect, 8f, 8f, sliderStrokePaint)

        // 绘制下滑块
        val endSliderRect = RectF(
            width / 2 - sliderWidth / 2,
            endY - sliderSize / 2,
            width / 2 + sliderWidth / 2,
            endY + sliderSize / 2
        )
        canvas.drawRoundRect(endSliderRect, 8f, 8f, sliderPaint)
        canvas.drawRoundRect(endSliderRect, 8f, 8f, sliderStrokePaint)
    }

    private fun drawHorizontalSliders(canvas: Canvas) {
        val height = height.toFloat()
        val width = width.toFloat()

        val startX = width * cropStart
        val endX = width * cropEnd

        // 绘制遮罩
        if (cropStart > 0) {
            canvas.drawRect(0f, 0f, startX, height, overlayPaint)
        }
        if (cropEnd < 1f) {
            canvas.drawRect(endX, 0f, width, height, overlayPaint)
        }

        // 绘制左滑块
        val startSliderRect = RectF(
            startX - sliderSize / 2,
            height / 2 - sliderWidth / 2,
            startX + sliderSize / 2,
            height / 2 + sliderWidth / 2
        )
        canvas.drawRoundRect(startSliderRect, 8f, 8f, sliderPaint)
        canvas.drawRoundRect(startSliderRect, 8f, 8f, sliderStrokePaint)

        // 绘制右滑块
        val endSliderRect = RectF(
            endX - sliderSize / 2,
            height / 2 - sliderWidth / 2,
            endX + sliderSize / 2,
            height / 2 + sliderWidth / 2
        )
        canvas.drawRoundRect(endSliderRect, 8f, 8f, sliderPaint)
        canvas.drawRoundRect(endSliderRect, 8f, 8f, sliderStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!showCropSliders) return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                draggingSlider = findTouchedSlider(event.x, event.y)
                if (draggingSlider != null) {
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggingSlider != null) {
                    handleSliderDrag(event.x, event.y)
                    lastTouchX = event.x
                    lastTouchY = event.y
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (draggingSlider != null) {
                    parent.requestDisallowInterceptTouchEvent(false)
                    draggingSlider = null
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findTouchedSlider(x: Float, y: Float): Slider? {
        // 只允许在滑块中心区域触摸，不允许在边界触摸
        // 使用 sliderWidth 作为触摸半径，而不是 sliderSize
        val touchRadius = sliderWidth

        when (orientation) {
            Orientation.VERTICAL -> {
                val startY = height * cropStart
                val endY = height * cropEnd
                val centerX = width / 2

                // 检查上滑块：只有在滑块中心区域内才响应
                if (Math.abs(x - centerX) <= touchRadius &&
                    Math.abs(y - startY) <= touchRadius) {
                    return Slider.START
                }
                // 检查下滑块：只有在滑块中心区域内才响应
                if (Math.abs(x - centerX) <= touchRadius &&
                    Math.abs(y - endY) <= touchRadius) {
                    return Slider.END
                }
            }
            Orientation.HORIZONTAL -> {
                val startX = width * cropStart
                val endX = width * cropEnd
                val centerY = height / 2

                // 检查左滑块：只有在滑块中心区域内才响应
                if (Math.abs(x - startX) <= touchRadius &&
                    Math.abs(y - centerY) <= touchRadius) {
                    return Slider.START
                }
                // 检查右滑块：只有在滑块中心区域内才响应
                if (Math.abs(x - endX) <= touchRadius &&
                    Math.abs(y - centerY) <= touchRadius) {
                    return Slider.END
                }
            }
        }
        return null
    }

    private fun handleSliderDrag(x: Float, y: Float) {
        when (orientation) {
            Orientation.VERTICAL -> {
                val newPosition = (y / height).coerceIn(0f, 1f)
                when (draggingSlider) {
                    Slider.START -> {
                        cropStart = newPosition
                        onCropChanged?.invoke(cropStart, cropEnd)
                    }
                    Slider.END -> {
                        cropEnd = newPosition
                        onCropChanged?.invoke(cropStart, cropEnd)
                    }
                    null -> {}
                }
            }
            Orientation.HORIZONTAL -> {
                val newPosition = (x / width).coerceIn(0f, 1f)
                when (draggingSlider) {
                    Slider.START -> {
                        cropStart = newPosition
                        onCropChanged?.invoke(cropStart, cropEnd)
                    }
                    Slider.END -> {
                        cropEnd = newPosition
                        onCropChanged?.invoke(cropStart, cropEnd)
                    }
                    null -> {}
                }
            }
        }
    }
}

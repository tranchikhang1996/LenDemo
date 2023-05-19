package com.example.lendemo.view

import android.content.Context
import android.content.res.Resources
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.Canvas
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.core.graphics.values
import androidx.core.view.GestureDetectorCompat
import com.example.ocrsurface.OcrSurface
import com.google.mlkit.vision.common.InputImage
import kotlin.math.roundToInt

class TextImageView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    style: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(ctx, attrs, style) {

    private var minScale = 0.1f
    private var maxScale = 8f

    private val ocrSurface = OcrSurface(ctx = ctx).apply {
        onSurfaceChangedListener = { invalidate() }
    }

    var onTextSelectedListener: ((String) -> Unit)? = null
        set(value) {
            field = value
            ocrSurface.onTextSelectedListener = value
        }

    private var matrix = Matrix()
    private var inputImage: InputImage? = null
    private val cursorSize = (24 * Resources.getSystem().displayMetrics.density).roundToInt()

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val iWidth = inputImage?.width ?: return true
            val iHeight = inputImage?.height ?: return true
            val values = matrix.values()
            val currentScaleX = values[0]
            val currentScaleY = values[4]
            val newScaleX = (values[0] * detector.scaleFactor).let { minScale.coerceAtLeast(it.coerceAtMost(maxScale)) }
            val newScaleY = (values[4] * detector.scaleFactor).let { minScale.coerceAtLeast(it.coerceAtMost(maxScale)) }
            val offsetX = (newScaleX - currentScaleX) * iWidth / 2
            val offsetY = (newScaleY - currentScaleY) * iHeight / 2
            var newTransX = values[2] - offsetX
            var newTransY = values[5] - offsetY
            if (newTransX.compareTo(0) > 0) {
                newTransX = if ((iWidth * newScaleX).compareTo(width) >= 0) 0f else ((width - (iWidth * newScaleX)) / 2)
            } else if ((newScaleX * iWidth + newTransX).compareTo(width) < 0) {
                newTransX = width - newScaleX * iWidth
                if (newTransX.compareTo(0) > 0) {
                    newTransX = ((width - (iWidth * newScaleX)) / 2)
                }
            }
            if (newTransY.compareTo(0) > 0) {
                newTransY = if ((iHeight * newScaleY).compareTo(height) >= 0) 0f else ((height - (iHeight * newScaleY)) / 2)
            } else if ((newScaleY * iHeight + newTransY).compareTo(height) < 0) {
                newTransY = height - newScaleY * iHeight
                if (newTransY.compareTo(0) > 0) {
                    newTransY = ((height - (iHeight * newScaleY)) / 2)
                }
            }
            values[0] = newScaleX
            values[4] = newScaleY
            values[2] = newTransX
            values[5] = newTransY
            matrix.setValues(values)
            updateTransform()
            return true
        }
    }

    private val scaleDetector = ScaleGestureDetector(ctx, scaleListener)

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapUp(event: MotionEvent): Boolean {
            val values = matrix.values()
            val eventX = (event.x - values[2]) / values[0]
            val eventY = (event.y - values[5]) / values[4]
            ocrSurface.onTouchEvent(MotionEvent.ACTION_DOWN, eventX, eventY)
            ocrSurface.onTouchEvent(MotionEvent.ACTION_UP, eventX, eventY)
            return true
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val iWidth = inputImage?.width ?: return true
            val iHeight = inputImage?.height ?: return true
            val values = matrix.values()
            val currentLeft = values[2]
            val currentRight = iWidth * values[0] + values[2]
            val currentTop = values[5]
            val currentBottom = iHeight * values[4] + values[5]
            if((currentLeft - distanceX).compareTo(0) > 0 && (currentRight - distanceX).compareTo(width) < 0) {
                values[2] = (width - iWidth * values[0]) / 2
            } else if((currentLeft - distanceX).compareTo(0) > 0) {
                values[2] = 0f
            } else if((currentRight - distanceX).compareTo(width) < 0) {
                values[2] = -(iWidth * values[0] - width)
            } else {
                values[2] -= distanceX
            }

            if((currentTop - distanceY).compareTo(0) > 0 && (currentBottom - distanceY).compareTo(height) < 0) {
                values[5] = (height - iHeight * values[4]) / 2
            } else if((currentTop - distanceY).compareTo(0) > 0) {
                values[5] = 0f
            } else if((currentBottom - distanceY).compareTo(height) < 0) {
                values[5] = -(iHeight * values[4] - height)
            } else {
                values[5] -= distanceY
            }
            matrix.setValues(values)
            updateTransform()
            return true
        }
    }

    private val scrollDetector = GestureDetectorCompat(ctx, gestureListener)

    private fun updateTransform() {
        val values = matrix.values()
        val viewPort = RectF(
            (-values[2]) / values[0],
            (-values[5]) / values[4],
            (width - values[2]) / values[0],
            (height - values[5]) / values[4]
        )
        imageMatrix = matrix
        ocrSurface.setViewPort(viewPort, cursorSize / values[0])
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        inputImage?.let { scaleFitCenter(it.width.toFloat(), it.height.toFloat()) }
    }

    fun setImage(uri: Uri) {
        this.inputImage = InputImage.fromFilePath(context, uri)
        scaleFitCenter(inputImage!!.width.toFloat(), inputImage!!.height.toFloat())
        inputImage?.bitmapInternal?.let {
            setImageBitmap(it)
            ocrSurface.setImage(it)
        }
    }

    private fun scaleFitCenter(iWidth: Float, iHeight: Float) {
        val vWidth = width.toFloat()
        val vHeight = height.toFloat()
        val widthRatio = vWidth / iWidth
        val heightRatio = vHeight / iHeight
        val scale = if (vHeight.compareTo(widthRatio * iHeight) >= 0) widthRatio else heightRatio
        val translateX = (vWidth - (iWidth * scale)) / 2
        val translateY = (vHeight - (iHeight * scale)) / 2
        matrix.reset()
        matrix.setTranslate(translateX, translateY)
        matrix.preScale(scale, scale)
        minScale = scale
        maxScale = minScale * 5f
        updateTransform()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        ocrSurface.draw(canvas, matrix)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val values = matrix.values()
        val eventX = (event.x - values[2]) / values[0]
        val eventY = (event.y - values[5]) / values[4]
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if(ocrSurface.isOnCursorTouch(eventX, eventY)) {
                    return ocrSurface.onTouchEvent(event.actionMasked, eventX, eventY)
                }
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if(ocrSurface.isCursorPressing()) {
                    return ocrSurface.onTouchEvent(event.actionMasked, eventX, eventY)
                }
            }
        }
        scaleDetector.onTouchEvent(event)
        scrollDetector.onTouchEvent(event)
        return true
    }
}
package com.example.ocrsurface

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.PointF
import android.graphics.Path
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import com.example.ocrsurface.data.BoundingBox
import com.example.ocrsurface.data.Element
import com.example.ocrsurface.data.Line
import com.example.ocrsurface.data.OcrResult
import com.example.ocrsurface.engine.MLKitEngine
import com.example.ocrsurface.engine.OcrEngine
import com.example.ocrsurface.utils.centerOf
import com.example.ocrsurface.utils.rotate
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.abs

typealias Cursor = BoundingBox

class OcrSurface constructor(
    private val ctx: Context,
    private val engine: OcrEngine = MLKitEngine()
) {
    var onSurfaceChangedListener: (() -> Unit)? = null

    var onTextSelectedListener: ((String) -> Unit)? = null

    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(ctx, R.color.cursor_color)
    }

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(ctx, R.color.dim)
    }
    private val textSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(ctx, R.color.text_selected_background)
    }

    private var lines: List<Line> = emptyList()
    private var dimOverlay: Bitmap? = null
    private var viewPort: RectF? = null
    private var cursorSize: Float = 24f
    private val values = FloatArray(9)
    private val cursorPath = Path()

    private var startSelectedElement: Element? = null
    private var endSelectedElement: Element? = null
    private var startSelectedLine: Line? = null
    private var endSelectedLine: Line? = null
    private var startCursor: Cursor? = null
    private var endCursor: Cursor? = null
    private var isEndCursorPressing = false
    private var isStartCursorPressing = false
    private var lastMoveUpdateTime = 100L
    private val selectedLines = mutableListOf<BoundingBox>()
    private var selectedText: String? = null
    private var isFirstSelection = true

    private var cursorPressPadding: PointF? = null

    fun setImage(bitmap: Bitmap) {
        engine.process(bitmap, ::onOcrResult)
    }

    fun setViewPort(rectF: RectF, cursorSize: Float) {
        this.viewPort = rectF
        this.cursorSize = cursorSize
        startSelectedElement?.rect?.let {
            startCursor = createStartCursor(it.pointD, -it.angle)
        }
        endSelectedElement?.rect?.let {
            endCursor = createEndCursor(it.pointC, -it.angle)
        }
        updateDimBackground()
    }

    fun draw(canvas: Canvas, matrix: Matrix) {
        dimOverlay?.let {
            canvas.drawBitmap(it, matrix, null)
            drawCursor(canvas, matrix)
        }
    }

    fun isCursorPressing() = isEndCursorPressing || isStartCursorPressing

    fun isOnCursorTouch(x: Float, y: Float) =
        (startCursor?.contain(x, y) == true) || (endCursor?.contain(x, y) == true)

    fun onTouchEvent(action: Int, x: Float, y: Float): Boolean {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                onActionDown(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                onActionMove(x, y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onActionUp(x, y)
            }
        }
        updateDimBackground()
        return true
    }

    private fun drawCursor(canvas: Canvas, matrix: Matrix) {
        matrix.getValues(values)
        val scaleX = values[0]
        val scaleY = values[4]
        val transX = values[2]
        val transY = values[5]
        cursorPath.reset()
        startCursor?.let {
            val bx = it.pointB.x * scaleX + transX
            val by = it.pointB.y * scaleY + transY
            val ax = it.pointA.x * scaleX + transX
            val ay = it.pointA.y * scaleY + transY
            val p1x = (ax + bx) / 2
            val p1y = (ay + by) / 2
            val p2x = ((p1x - bx) * cos(-PI / 2) - (p1y - by) * sin(-PI / 2) + bx).toFloat()
            val p2y = ((p1x - bx) * sin(-PI / 2) + (p1y - by) * cos(-PI / 2) + by).toFloat()
            val cenX = p1x + p2x - bx
            val cenY = p1y + p2y - by
            val radius = sqrt((bx - p1x).pow(2) + (by - p1y).pow(2))
            cursorPath.moveTo(bx, by)
            cursorPath.lineTo(p1x, p1y)
            cursorPath.lineTo(p2x, p2y)
            cursorPath.lineTo(bx, by)
            cursorPath.moveTo(cenX, cenY)
            cursorPath.addCircle(cenX, cenY, radius, Path.Direction.CCW)
        }
        endCursor?.let {
            val ax = it.pointA.x * scaleX + transX
            val ay = it.pointA.y * scaleY + transY
            val bx = it.pointB.x * scaleX + transX
            val by = it.pointB.y * scaleY + transY
            val p1x = (bx + ax) / 2
            val p1y = (by + ay) / 2
            val p2x = ((p1x - ax) * cos(PI / 2) - (p1y - ay) * sin(PI / 2) + ax).toFloat()
            val p2y = ((p1x - ax) * sin(PI / 2) + (p1y - ay) * cos(PI / 2) + ay).toFloat()
            val cenX = p1x + p2x - ax
            val cenY = p1y + p2y - ay
            val radius = sqrt((ax - p1x).pow(2) + (ay - p1y).pow(2))
            cursorPath.moveTo(ax, ay)
            cursorPath.lineTo(p1x, p1y)
            cursorPath.lineTo(p2x, p2y)
            cursorPath.lineTo(ax, ay)
            cursorPath.moveTo(cenX, cenY)
            cursorPath.addCircle(cenX, cenY, radius, Path.Direction.CW)
        }
        canvas.drawPath(cursorPath, cursorPaint)
    }

    private fun onOcrResult(result: OcrResult) {
        this.lines = result.textLines
        reset()
        if (dimOverlay?.width != result.width || dimOverlay?.height != result.height) {
            dimOverlay?.recycle()
            dimOverlay = Bitmap.createBitmap(result.width, result.height, Bitmap.Config.ARGB_8888)
        }
        updateDimBackground()
    }

    private fun updateDimBackground() {
        dimOverlay?.applyCanvas {
            drawRect(0f, 0f, width.toFloat(), height.toFloat(), clearPaint)
            drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
            drawBoxes(this, lines.map { it.rect }, clearPaint)
            drawBoxes(this, selectedLines, textSelectedPaint)
        }
        onSurfaceChangedListener?.invoke()
    }

    private fun drawBoxes(canvas: Canvas, boxes: List<BoundingBox>, paint: Paint) {
        val path = Path()
        boxes.forEach { box ->
            path.moveTo(box.pointA.x, box.pointA.y)
            path.lineTo(box.pointB.x, box.pointB.y)
            path.lineTo(box.pointC.x, box.pointC.y)
            path.lineTo(box.pointD.x, box.pointD.y)
            path.lineTo(box.pointA.x, box.pointA.y)
        }
        canvas.drawPath(path, paint)
    }

    private fun onActionUp(x: Float, y: Float) {
        if (isEndCursorPressing) {
            onEndCursorMove(
                PointF(x + (cursorPressPadding?.x ?: 0f), y + (cursorPressPadding?.y ?: 0f))
            )
        } else if (isStartCursorPressing) {
            onStartCursorMove(
                PointF(x + (cursorPressPadding?.x ?: 0f), y + (cursorPressPadding?.y ?: 0f))
            )
        }
        startSelectedElement?.rect?.let {
            startCursor = createStartCursor(it.pointD, -it.angle)
        }
        endSelectedElement?.rect?.let {
            endCursor = createEndCursor(it.pointC, -it.angle)
        }
        isStartCursorPressing = false
        isEndCursorPressing = false
        cursorPressPadding = null
        updateTextAndSelectedLines()
    }

    private fun reset() {
        startCursor = null
        endCursor = null
        startSelectedElement = null
        endSelectedElement = null
        startSelectedLine = null
        endSelectedLine = null
        selectedLines.clear()
        selectedText = null
        isEndCursorPressing = false
        isStartCursorPressing = false
        cursorPressPadding = null
    }

    private fun onActionDown(x: Float, y: Float) {
        if (endCursor?.contain(x, y) == true) {
            cursorPressPadding = PointF(
                endCursor?.pointA?.x?.let { it - x } ?: 0f,
                endCursor?.pointA?.y?.let { it - y } ?: 0f
            )
            isEndCursorPressing = true
        } else if (startCursor?.contain(x, y) == true) {
            cursorPressPadding = PointF(
                startCursor?.pointB?.x?.let { it - x } ?: 0f,
                startCursor?.pointB?.y?.let { it - y } ?: 0f
            )
            isStartCursorPressing = true
        } else {
            reset()
            lines@ for (line in lines) {
                if (!line.rect.contain(x, y)) continue
                for (element in line.elements) {
                    if (!element.rect.contain(x, y)) continue
                    startSelectedLine = if (isFirstSelection) lines.first() else line
                    startSelectedElement =
                        if (isFirstSelection) startSelectedLine?.elements?.firstOrNull() else element
                    endSelectedLine = if (isFirstSelection) lines.last() else line
                    endSelectedElement =
                        if (isFirstSelection) endSelectedLine?.elements?.lastOrNull() else element
                    isFirstSelection = false
                    break@lines
                }
            }
        }
    }

    private fun onActionMove(x: Float, y: Float) {
        if (System.currentTimeMillis() - lastMoveUpdateTime < 100L) {
            return
        }
        if (isStartCursorPressing) {
            onStartCursorMove(
                PointF(x + (cursorPressPadding?.x ?: 0f), y + (cursorPressPadding?.y ?: 0f))
            )
            updateTextAndSelectedLines()
            lastMoveUpdateTime = System.currentTimeMillis()
        } else if (isEndCursorPressing) {
            onEndCursorMove(
                PointF(x + (cursorPressPadding?.x ?: 0f), y + (cursorPressPadding?.y ?: 0f))
            )
            updateTextAndSelectedLines()
            lastMoveUpdateTime = System.currentTimeMillis()
        }
    }

    private fun updateTextAndSelectedLines() {
        val sLine = startSelectedLine ?: return
        val eLine = endSelectedLine ?: return
        val sElement = startSelectedElement ?: return
        val eElement = endSelectedElement ?: return
        val fromLine = lines.indexOf(sLine)
        val toLine = lines.indexOf(eLine)
        val textBuilder = StringBuilder("")
        selectedLines.clear()
        if (sLine.id == eLine.id) {
            val fromElement = sLine.elements.indexOf(sElement)
            val toElement = eLine.elements.indexOf(eElement)
            for (i in fromElement..toElement) {
                textBuilder.append(sLine.elements[i].text)
                textBuilder.append(" ")
            }
            textBuilder.trim()
            selectedLines.add(
                BoundingBox(
                    sElement.rect.pointA,
                    eElement.rect.pointB,
                    eElement.rect.pointC,
                    sElement.rect.pointD
                )
            )
        } else {
            for (i in fromLine..toLine) {
                val line = lines[i]
                when (line.id) {
                    sLine.id -> {
                        val fromElement = line.elements.indexOf(sElement)
                        for (j in fromElement until line.elements.size) {
                            textBuilder.append(line.elements[j].text)
                            textBuilder.append(" ")
                        }
                        textBuilder.trim()
                        textBuilder.appendLine()
                        selectedLines.add(
                            BoundingBox(
                                sElement.rect.pointA,
                                line.rect.pointB,
                                line.rect.pointC,
                                sElement.rect.pointD
                            )
                        )
                    }
                    eLine.id -> {
                        val toElement = line.elements.indexOf(eElement)
                        for (j in 0..toElement) {
                            textBuilder.append(line.elements[j].text)
                            textBuilder.append(" ")
                        }
                        textBuilder.trim()
                        selectedLines.add(
                            BoundingBox(
                                line.rect.pointA,
                                eElement.rect.pointB,
                                eElement.rect.pointC,
                                line.rect.pointD
                            )
                        )
                    }
                    else -> {
                        textBuilder.append(line.text)
                        textBuilder.trim()
                        textBuilder.appendLine()
                        selectedLines.add(line.rect)
                    }
                }
            }
        }
        textBuilder.trim().toString().apply {
            selectedText = this
            onTextSelectedListener?.invoke(this)
        }
    }

    private fun onEndCursorMove(p: PointF) {
        endCursor = null
        val startLineIndex = lines.indexOf(startSelectedLine)
        for (i in startLineIndex until lines.size) {
            val line = lines[i]
            if (line.rect.contain(p.x, p.y)) {
                if (line.id == startSelectedLine?.id) {
                    if (startSelectedElement?.rect?.isPointInLeftSide(p) == true) {
                        return toStartCursorPressMode(p)
                    } else {
                        endSelectedElement = line.elements.findLast {
                            it.rect.isPointInRightSide(p) || it.rect.contain(p.x, p.y)
                        }
                        endSelectedLine = line
                        return
                    }
                } else {
                    endSelectedElement = line.elements.findLast {
                        it.rect.isPointInRightSide(p) || it.rect.contain(p.x, p.y)
                    }
                    endSelectedLine = line
                    return
                }
            }
        }

        for (i in startLineIndex until lines.size) {
            val line = lines[i]
            if (line.rect.isPointInAbove(p)) {
                if (line.id == startSelectedLine?.id) {
                    return toStartCursorPressMode(p)
                } else {
                    return
                }
            } else {
                endSelectedElement = line.elements.last()
                endSelectedLine = line
            }
        }
    }

    private fun toStartCursorPressMode(p: PointF) {
        endSelectedElement = startSelectedElement
        endSelectedLine = startSelectedLine
        startSelectedElement = null
        startSelectedLine = null
        isEndCursorPressing = false
        isStartCursorPressing = true
        endSelectedElement?.rect?.let {
            endCursor = createEndCursor(it.pointC, -it.angle)
        }
        onStartCursorMove(p)
    }

    private fun onStartCursorMove(p: PointF) {
        startCursor = null
        val endLineIndex = lines.indexOf(endSelectedLine)
        for (i in endLineIndex downTo 0) {
            val line = lines[i]
            if (line.rect.contain(p.x, p.y)) {
                if (line.id == endSelectedLine?.id) {
                    if (endSelectedElement?.rect?.isPointInRightSide(p) == true) {
                        return toEndCursorPressMode(p)
                    } else {
                        startSelectedElement = line.elements.find {
                            it.rect.isPointInLeftSide(p) || it.rect.contain(p.x, p.y)
                        }
                        startSelectedLine = line
                        return
                    }
                } else {
                    startSelectedElement = line.elements.find {
                        it.rect.isPointInLeftSide(p) || it.rect.contain(p.x, p.y)
                    }
                    startSelectedLine = line
                    return
                }
            }
        }

        for (i in endLineIndex downTo 0) {
            val line = lines[i]
            if (line.rect.isPointInBelow(p)) {
                if (line.id == endSelectedLine?.id) {
                    return toEndCursorPressMode(p)
                } else {
                    return
                }
            } else {
                startSelectedElement = line.elements.first()
                startSelectedLine = line
            }
        }
    }

    private fun toEndCursorPressMode(p: PointF) {
        startSelectedElement = endSelectedElement
        startSelectedLine = endSelectedLine
        endSelectedElement = null
        endSelectedLine = null
        isStartCursorPressing = false
        isEndCursorPressing = true
        startSelectedElement?.rect?.let {
            startCursor = createStartCursor(it.pointD, -it.angle)
        }
        onEndCursorMove(p)
    }

    private fun createStartCursor(anchor: PointF, angle: Float): Cursor {
        val region = Cursor(
            PointF(anchor.x - cursorSize, anchor.y).rotate(anchor, angle.toDouble()),
            anchor,
            PointF(anchor.x, anchor.y + cursorSize).rotate(anchor, angle.toDouble()),
            PointF(anchor.x - cursorSize, anchor.y + cursorSize).rotate(anchor, angle.toDouble())
        )

        val viewPort = this.viewPort ?: return region

        if ((anchor.x - viewPort.left).compareTo(0) < 0) {
            return region
        }
        val radius = cursorSize / 2f
        val center = centerOf(region.pointB, region.pointD)
        if (abs(center.x - viewPort.left).compareTo(radius) >= 0) {
            return region
        }
        val distance = abs(viewPort.left + radius - anchor.x)
        val newCenterY = anchor.y + sqrt(2 * radius * radius - distance * distance)
        val newCenterX = viewPort.left + radius
        val newCenter = PointF(newCenterX, newCenterY)
        return Cursor(
            anchor.rotate(newCenter, -Math.PI / 2),
            anchor,
            anchor.rotate(newCenter, Math.PI / 2),
            anchor.rotate(newCenter, Math.PI)
        )
    }

    private fun createEndCursor(anchor: PointF, angle: Float): Cursor {
        val region = Cursor(
            anchor,
            PointF(anchor.x + cursorSize, anchor.y).rotate(anchor, angle.toDouble()),
            PointF(anchor.x + cursorSize, anchor.y + cursorSize).rotate(anchor, angle.toDouble()),
            PointF(anchor.x, anchor.y + cursorSize).rotate(anchor, angle.toDouble())
        )
        val viewPort = this.viewPort ?: return region

        if ((anchor.x - viewPort.right).compareTo(0) > 0) {
            return region
        }
        val radius = cursorSize / 2f
        val center = centerOf(region.pointA, region.pointC)
        if (abs(center.x - viewPort.right).compareTo(radius) >= 0) {
            return region
        }
        val distance = abs(viewPort.right - radius - anchor.x)
        val newCenterY = anchor.y + sqrt(2 * radius * radius - distance * distance)
        val newCenterX = viewPort.right - radius
        val newCenter = PointF(newCenterX, newCenterY)
        return Cursor(
            anchor,
            anchor.rotate(newCenter, Math.PI / 2),
            anchor.rotate(newCenter, Math.PI),
            anchor.rotate(newCenter, -Math.PI / 2)
        )
    }
}
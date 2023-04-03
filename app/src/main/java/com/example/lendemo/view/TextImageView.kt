package com.example.lendemo.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import com.example.lendemo.*
import com.example.lendemo.extension.dp
import com.example.lendemo.extension.rotate
import com.example.lendemo.view.CopyCursor.Companion.createEndCursor
import com.example.lendemo.view.CopyCursor.Companion.createStartCursor
import com.example.lendemo.view.CopyCursor.Companion.cursorWidth

class TextImageView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    style: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(ctx, attrs, style) {

    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textSelectedPaint = Paint()
    private var dimBackground: Bitmap? = null
    private var result: ScannedResult? = null
    private var scaledLines: List<Line> = emptyList()
    private var startElement: Element? = null
    private var endElement: Element? = null
    private var startLine: Line? = null
    private var endLine: Line? = null
    private var startCursor: CopyCursor? = null
    private var endCursor: CopyCursor? = null
    private var isEndPressing = false
    private var isStartPressing = false
    private var lastMoveUpdateTime = 0L
    private val selectedLines = mutableListOf<BoundingBox>()
    private var selectedText: String? = null

    private var cursorPressPadding: PointF? = null

    var onTextSelectedListener: ((String?) -> Unit)? = null


    init {
        cursorPaint.color = ContextCompat.getColor(context, R.color.cursor_color)
        textSelectedPaint.color = ContextCompat.getColor(context, R.color.text_selected_background)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        result?.run {
            val xRatio = (w.toFloat() - 2f * cursorWidth) / image.width.toFloat()
            val yRatio = (h.toFloat() - 2f * cursorWidth) / image.height.toFloat()
            scaledLines = blocks.map { block ->
                block.scale(
                    xRatio,
                    yRatio,
                    PointF(cursorWidth.toFloat(), cursorWidth.toFloat())
                )
            }
            createDimBackground(w, h)
        }
    }

    fun setTextBlocks(result: ScannedResult) {
        this.result = result
        val xRatio = (width.toFloat() - 2f * cursorWidth) / result.image.width.toFloat()
        val yRatio = (height.toFloat() - 2f * cursorWidth) / result.image.height.toFloat()
        this.scaledLines = result.blocks.map {
            it.scale(
                xRatio,
                yRatio,
                PointF(cursorWidth.toFloat(), cursorWidth.toFloat())
            )
        }
        createDimBackground(width, height)
        invalidate()
    }

    private fun createDimBackground(w: Int, h: Int) {
        dimBackground = if (w > 0 && h > 0) {
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).applyCanvas {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ContextCompat.getColor(context, R.color.dim)
                }
                drawRect(
                    cursorWidth.toFloat(),
                    cursorWidth.toFloat(),
                    w.toFloat() - cursorWidth,
                    h.toFloat() - cursorWidth,
                    paint
                )
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                scaledLines.forEach { drawBox(this, it.rect, paint) }
            }
        } else null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        dimBackground?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        selectedLines.forEach { drawBox(canvas, it, textSelectedPaint) }
        drawStartCursor(canvas)
        drawEndCursor(canvas)
    }

    private fun drawBox(canvas: Canvas, box: BoundingBox?, paint: Paint) {
        box ?: return
        val path = Path()
        path.moveTo(box.pointA.x, box.pointA.y)
        path.lineTo(box.pointB.x, box.pointB.y)
        path.lineTo(box.pointC.x, box.pointC.y)
        path.lineTo(box.pointD.x, box.pointD.y)
        path.lineTo(box.pointA.x, box.pointA.y)
        canvas.drawPath(path, paint)
    }

    private fun drawEndCursor(canvas: Canvas) {
        endCursor?.let {
            val path = Path()
            val cx = (it.region.pointA.x + it.region.pointC.x) / 2
            val cy = (it.region.pointA.y + it.region.pointC.y) / 2
            val p1x = (it.region.pointA.x + it.region.pointB.x) / 2
            val p1y = (it.region.pointA.y + it.region.pointB.y) / 2
            val p2x = (it.region.pointA.x + it.region.pointD.x) / 2
            val p2y = (it.region.pointA.y + it.region.pointD.y) / 2
            path.moveTo(it.region.pointA.x, it.region.pointA.y)
            path.lineTo(p1x, p1y)
            path.lineTo(p2x, p2y)
            path.lineTo(it.region.pointA.x, it.region.pointA.y)
            path.moveTo(cx, cy)
            path.addCircle(cx, cy, cursorWidth / 2f, Path.Direction.CW)
            canvas.drawPath(path, cursorPaint)
        }
    }

    private fun drawStartCursor(canvas: Canvas) {
        startCursor?.let {
            val path = Path()
            val cx = (it.region.pointA.x + it.region.pointC.x) / 2
            val cy = (it.region.pointA.y + it.region.pointC.y) / 2
            val p1x = (it.region.pointA.x + it.region.pointB.x) / 2
            val p1y = (it.region.pointA.y + it.region.pointB.y) / 2
            val p2x = (it.region.pointB.x + it.region.pointC.x) / 2
            val p2y = (it.region.pointB.y + it.region.pointC.y) / 2
            path.moveTo(it.region.pointB.x, it.region.pointB.y)
            path.lineTo(p1x, p1y)
            path.lineTo(p2x, p2y)
            path.lineTo(it.region.pointB.x, it.region.pointB.y)
            path.moveTo(cx, cy)
            path.addCircle(cx, cy, cursorWidth / 2f, Path.Direction.CCW)
            canvas.drawPath(path, cursorPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onActionDown(event)
            }
            MotionEvent.ACTION_MOVE -> {
                onActionMove(event)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onActionUp(event)
            }
        }
        invalidate()
        return true
    }

    private fun onActionUp(event: MotionEvent) {
        if (isEndPressing) {
            onEndCursorMove(
                PointF(
                    event.x + (cursorPressPadding?.x ?: 0f),
                    event.y + (cursorPressPadding?.y ?: 0f)
                )
            )
        } else if (isStartPressing) {
            onStartCursorMove(
                PointF(
                    event.x + (cursorPressPadding?.x ?: 0f),
                    event.y + (cursorPressPadding?.y ?: 0f)
                )
            )
        }
        startCursor = startElement?.rect?.let { createStartCursor(it.pointD, -it.angle) }
        endCursor = endElement?.rect?.let { createEndCursor(it.pointC, -it.angle) }
        isStartPressing = false
        isEndPressing = false
        cursorPressPadding = null
        updateTextAndSelectedLines()
    }

    private fun onActionDown(event: MotionEvent) {
        if (endCursor?.region?.contain(event.x, event.y) == true) {
            cursorPressPadding = PointF(
                endCursor?.region?.pointA?.x?.let { it - event.x } ?: 0f,
                endCursor?.region?.pointA?.y?.let { it - event.y } ?: 0f
            )
            isEndPressing = true
        } else if (startCursor?.region?.contain(event.x, event.y) == true) {
            cursorPressPadding = PointF(
                startCursor?.region?.pointB?.x?.let { it - event.x } ?: 0f,
                startCursor?.region?.pointB?.y?.let { it - event.y } ?: 0f
            )
            isStartPressing = true
        } else {
            startCursor = null
            endCursor = null
            startElement = null
            endElement = null
            startLine = null
            endLine = null
            selectedLines.clear()
            selectedText = null
            isEndPressing = false
            isStartPressing = false
            cursorPressPadding = null
            lines@ for (line in scaledLines) {
                if (!line.rect.contain(event.x, event.y)) continue
                for (element in line.elements) {
                    if (!element.rect.contain(event.x, event.y)) continue
                    startElement = element
                    endElement = element
                    startLine = line
                    endLine = line
                    startCursor = createStartCursor(element.rect.pointD, -element.rect.angle)
                    endCursor = createEndCursor(element.rect.pointC, -element.rect.angle)
                    break@lines
                }
            }
        }
    }

    private fun onActionMove(event: MotionEvent) {
        if (System.currentTimeMillis() - lastMoveUpdateTime < 100L) {
            return
        }
        if (isStartPressing) {
            onStartCursorMove(
                PointF(
                    event.x + (cursorPressPadding?.x ?: 0f),
                    event.y + (cursorPressPadding?.y ?: 0f)
                )
            )
            updateTextAndSelectedLines()
            lastMoveUpdateTime = System.currentTimeMillis()
        } else if (isEndPressing) {
            onEndCursorMove(
                PointF(
                    event.x + (cursorPressPadding?.x ?: 0f),
                    event.y + (cursorPressPadding?.y ?: 0f)
                )
            )
            updateTextAndSelectedLines()
            lastMoveUpdateTime = System.currentTimeMillis()
        }
    }

    private fun updateTextAndSelectedLines() {
        val sLine = startLine ?: return
        val eLine = endLine ?: return
        val sElement = startElement ?: return
        val eElement = endElement ?: return
        val fromLine = scaledLines.indexOf(sLine)
        val toLine = scaledLines.indexOf(eLine)
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
                val line = scaledLines[i]
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
        selectedText = textBuilder.trim().toString()
        onTextSelectedListener?.invoke(selectedText)
    }

    private fun onEndCursorMove(p: PointF) {
        endCursor = null
        val startLineIndex = scaledLines.indexOf(startLine)
        for (i in startLineIndex until scaledLines.size) {
            val line = scaledLines[i]
            if (line.rect.contain(p.x, p.y)) {
                if (line.id == startLine?.id) {
                    if (startElement?.rect?.isPointInLeftSide(p) == true) {
                        return toStartPressMode(p)
                    } else {
                        endElement = line.elements.findLast {
                            it.rect.isPointInRightSide(p) || it.rect.contain(
                                p.x,
                                p.y
                            )
                        }
                        endLine = line
                        return
                    }
                } else {
                    endElement = line.elements.findLast {
                        it.rect.isPointInRightSide(p) || it.rect.contain(
                            p.x,
                            p.y
                        )
                    }
                    endLine = line
                    return
                }
            }
        }

        for (i in startLineIndex until scaledLines.size) {
            val line = scaledLines[i]
            if (line.rect.isPointInAbove(p)) {
                if (line.id == startLine?.id) {
                    return toStartPressMode(p)
                } else {
                    return
                }
            } else {
                endElement = line.elements.last()
                endLine = line
            }
        }
    }

    private fun toStartPressMode(p: PointF) {
        endElement = startElement
        endLine = startLine
        startElement = null
        startLine = null
        isEndPressing = false
        isStartPressing = true
        endElement?.let {
            endCursor = createEndCursor(it.rect.pointC, -it.rect.angle)
        }
        onStartCursorMove(p)
    }

    private fun onStartCursorMove(p: PointF) {
        startCursor = null
        val endLineIndex = scaledLines.indexOf(endLine)
        for (i in endLineIndex downTo 0) {
            val line = scaledLines[i]
            if (line.rect.contain(p.x, p.y)) {
                if (line.id == endLine?.id) {
                    if (endElement?.rect?.isPointInRightSide(p) == true) {
                        return toEndPressMode(p)
                    } else {
                        startElement = line.elements.find {
                            it.rect.isPointInLeftSide(p) || it.rect.contain(
                                p.x,
                                p.y
                            )
                        }
                        startLine = line
                        return
                    }
                } else {
                    startElement = line.elements.find {
                        it.rect.isPointInLeftSide(p) || it.rect.contain(
                            p.x,
                            p.y
                        )
                    }
                    startLine = line
                    return
                }
            }
        }

        for (i in endLineIndex downTo 0) {
            val line = scaledLines[i]
            if (line.rect.isPointInBelow(p)) {
                if (line.id == endLine?.id) {
                    return toEndPressMode(p)
                } else {
                    return
                }
            } else {
                startElement = line.elements.first()
                startLine = line
            }
        }
    }

    private fun toEndPressMode(p: PointF) {
        startElement = endElement
        startLine = endLine
        endElement = null
        endLine = null
        isStartPressing = false
        isEndPressing = true
        startElement?.let {
            startCursor = createStartCursor(it.rect.pointD, -it.rect.angle)
        }
        onEndCursorMove(p)
    }
}

data class CopyCursor(val region: BoundingBox, val angle: Float) {
    companion object {
        val cursorWidth = 24.dp

        fun createEndCursor(p: PointF, angle: Float) =
            CopyCursor(
                BoundingBox(
                    p,
                    PointF(p.x + cursorWidth, p.y).rotate(p, angle.toDouble()),
                    PointF(p.x + cursorWidth, p.y + cursorWidth).rotate(p, angle.toDouble()),
                    PointF(p.x, p.y + cursorWidth).rotate(p, angle.toDouble())
                ), angle
            )

        fun createStartCursor(p: PointF, angle: Float) =
            CopyCursor(
                BoundingBox(
                    PointF(p.x - cursorWidth, p.y).rotate(p, angle.toDouble()),
                    p,
                    PointF(p.x, p.y + cursorWidth).rotate(p, angle.toDouble()),
                    PointF(p.x - cursorWidth, p.y + cursorWidth).rotate(p, angle.toDouble())
                ), angle
            )
    }
}
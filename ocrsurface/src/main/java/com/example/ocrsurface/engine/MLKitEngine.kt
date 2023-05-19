package com.example.ocrsurface.engine

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import androidx.core.graphics.minus
import androidx.core.graphics.toPointF
import com.example.ocrsurface.data.BoundingBox
import com.example.ocrsurface.data.Line
import com.example.ocrsurface.data.OcrResult
import com.example.ocrsurface.utils.crossProduct
import com.example.ocrsurface.utils.rotate
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.*

class MLKitEngine : OcrEngine {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun process(bitmap: Bitmap): OcrResult {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        return suspendCoroutine { continuation ->
            recognizer.process(inputImage).continueWith {
                continuation.resume(
                    OcrResult(
                        inputImage.width,
                        inputImage.height,
                        getLines(it.result.textBlocks)
                    )
                )
            }
        }
    }

    override fun process(bitmap: Bitmap, onResult: ((OcrResult) -> Unit)?) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(inputImage).addOnSuccessListener {
            onResult?.invoke(
                OcrResult(
                    inputImage.width,
                    inputImage.height,
                    getLines(it.textBlocks)
                )
            )
        }
    }


    private fun getLines(blocks: List<Text.TextBlock>): List<Line> {
        val lines = mutableListOf<Line>()
        var count = 0
        var elementCount = 0
        blocks.forEach { block ->
            lines.addAll(block.lines.filter { it.cornerPoints != null }.map { line ->
                val lineBox = line.cornerPoints!!.toBoundingBox()
                Line(
                    count++,
                    line.text,
                    lineBox,
                    line.elements.filter { it.cornerPoints != null }.map { element ->
                        com.example.ocrsurface.data.Element(
                            elementCount++,
                            element.text,
                            element.cornerPoints!!.toElementBoundingBox(lineBox)
                        )
                    }.sortedBy {
                        it.rect.pointD.rotate(
                            lineBox.pointD,
                            -lineBox.angle.toDouble()
                        ).x
                    })
            })
            return@forEach
        }
        return lines.sortedBy { it.rect.lineBox.top }
    }

    private fun Array<Point>.toElementBoundingBox(lineBox: BoundingBox): BoundingBox {
        val bottomRight = get(2).toPointF()
        val bottomLeft = get(3).toPointF()
        val leftLine = lineBox.verticalCenLine.clone(bottomLeft)
        val rightLine = lineBox.verticalCenLine.clone(bottomRight)
        val pointA = lineBox.lineAB.findCrossPoint(leftLine)
        val pointB = lineBox.lineAB.findCrossPoint(rightLine)
        val pointC = lineBox.lineCD.findCrossPoint(rightLine)
        val pointD = lineBox.lineCD.findCrossPoint(leftLine)
        return BoundingBox(pointA!!, pointB!!, pointC!!, pointD!!)
    }

    private fun Array<Point>.toBoundingBox(): BoundingBox {
        val topLeft = get(0).toPointF()
        val topRight = get(1).toPointF()
        val bottomRight = get(2).toPointF()
        val bottomLeft = get(3).toPointF()
        val u = bottomRight.minus(bottomLeft)
        val a = u.y.toDouble()
        val b = u.x * -1.0
        val c = (a * bottomLeft.x + b * bottomLeft.y) * -1f
        val thickness = max(
            abs(a * topLeft.x + b * topLeft.y + c) / sqrt(a * a + b * b),
            abs(a * topRight.x + b * topRight.y + c) / sqrt(a * a + b * b)
        )
        return createBoundingBox(bottomLeft, bottomRight, thickness.toFloat())
    }

    private fun createBoundingBox(
        bottomLeft: PointF,
        bottomRight: PointF,
        height: Float
    ): BoundingBox {
        val u = bottomRight.minus(bottomLeft)
        val ox = PointF(1f, 0f)
        val cosAlphaUAndOx = u.crossProduct(ox) / (u.length())
        val alpha = acos(cosAlphaUAndOx.toDouble())
        val topLeft = when {
            u.y.compareTo(0) < 0 -> PointF(bottomLeft.x + height, bottomLeft.y).rotate(bottomLeft, -alpha - u.x.sign * PI / 2)
            u.y.compareTo(0) > 0 ->PointF(bottomLeft.x + height, bottomLeft.y).rotate(bottomLeft, alpha - u.x.sign * PI / 2)
            else -> PointF(bottomLeft.x + height, bottomLeft.y).rotate(bottomLeft, -PI / 2)
        }
        val topRight = PointF(topLeft.x + bottomRight.x - bottomLeft.x, topLeft.y + bottomRight.y - bottomLeft.y)
        return BoundingBox(topLeft, topRight, bottomRight, bottomLeft)
    }
}
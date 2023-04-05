package com.example.lendemo

import android.graphics.Point
import androidx.core.graphics.minus
import androidx.core.graphics.toPointF
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lendemo.extension.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class ScannedViewModel : ViewModel() {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val result = MutableLiveData<TextImage>()

    fun extractText(image: InputImage) {
        result.postValue(TextImage(image))
        viewModelScope.launch(Dispatchers.Default) {
            val textImage = suspendCoroutine<TextImage> { continuation ->
                recognizer.process(image).continueWith {
                    continuation.resume(TextImage(image, getLines(it.result.textBlocks)))
                }
            }
            result.postValue(textImage)
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
                        Element(
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
}

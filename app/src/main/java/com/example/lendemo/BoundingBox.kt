package com.example.lendemo

import android.graphics.PointF
import android.graphics.RectF
import androidx.core.graphics.minus
import com.example.lendemo.extension.angle
import com.example.lendemo.extension.centerOf
import com.example.lendemo.extension.isTheSameSide
import com.example.lendemo.extension.scale
import kotlin.math.sqrt

data class BoundingBox(
    val pointA: PointF = PointF(),
    val pointB: PointF = PointF(),
    val pointC: PointF = PointF(),
    val pointD: PointF = PointF()
) {
    val lineAB = LineEquation.from(pointA, pointB)
    val lineBC = LineEquation.from(pointB, pointC)
    val lineCD = LineEquation.from(pointC, pointD)
    val lineAD = LineEquation.from(pointA, pointD)
    val width = pointC.minus(pointD).let { sqrt(it.x * it.x + it.y * it.y) }
    val height = pointD.minus(pointA).let { sqrt(it.x * it.x + it.y * it.y) }
    val horizonCenLine = LineEquation.from(pointD, pointC, centerOf(pointD, pointB))
    val verticalCenLine = LineEquation.from(pointD, pointA, centerOf(pointD, pointB))
    val angle = pointC.minus(pointD).angle()
    val lineBox: RectF

    init {
        val minX = minOf(pointA.x, pointB.x, pointC.x, pointD.x)
        val maxX = maxOf(pointA.x, pointB.x, pointC.x, pointD.x)
        val minY = minOf(pointA.y, pointB.y, pointC.y, pointD.y)
        val maxY = maxOf(pointA.y, pointB.y, pointC.y, pointD.y)
        lineBox = RectF(minX, minY, maxX, maxY)
    }

    fun scale(xRatio: Float, yRatio: Float, p: PointF): BoundingBox {
        return BoundingBox(
            pointA.scale(xRatio, yRatio, p),
            pointB.scale(xRatio, yRatio, p),
            pointC.scale(xRatio, yRatio, p),
            pointD.scale(xRatio, yRatio, p)
        )
    }
    fun isPointInLeftSide(p: PointF): Boolean = !isTheSameSide(p, pointC, pointA, pointD)

    fun isPointInRightSide(p: PointF): Boolean = !isTheSameSide(p, pointA, pointB, pointC)

    fun isPointInAbove(p: PointF): Boolean {
        if (p.y.compareTo(lineBox.bottom) > 0) return false
        return !isTheSameSide(p, pointD, pointA, pointB)
    }

    fun isPointInBelow(p: PointF): Boolean {
        if(p.y.compareTo(lineBox.top) < 0) return false
        return !isTheSameSide(p, pointA, pointD, pointC)
    }

    fun contain(x: Float, y: Float) =
        (horizonCenLine.distanceFrom(x, y)
            .compareTo(height / 2) <= 0) && (verticalCenLine.distanceFrom(x, y)
            .compareTo(width / 2) <= 0)
}
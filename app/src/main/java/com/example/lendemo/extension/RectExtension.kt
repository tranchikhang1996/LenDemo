package com.example.lendemo.extension

import android.content.res.Resources
import android.graphics.PointF
import androidx.core.graphics.minus
import com.example.lendemo.BoundingBox
import kotlin.math.*

infix fun PointF.crossProduct(p: PointF) = x * p.x + y * p.y

fun PointF.scale(xRatio: Float, yRatio: Float, p: PointF) = PointF(x * xRatio + p.x, y * yRatio + p.y)

fun centerOf(p1: PointF, p2: PointF) = PointF((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)

fun PointF.rotate(p: PointF, angle: Double): PointF {
    val newX = (x - p.x) * cos(angle) - (y - p.y) * sin(angle) + p.x
    val newY = (x - p.x) * sin(angle) + (y - p.y) * cos(angle) + p.y
    return PointF(newX.toFloat(), newY.toFloat())
}

fun PointF.angle(): Float {
    return acos(x / length())
}

fun createBoundingBox(bottomLeft: PointF, bottomRight: PointF, height: Float): BoundingBox {
    val u = bottomRight.minus(bottomLeft)
    val y = PointF(1f, 0f)
    val cosAlphaUAndY = u.crossProduct(y) / (u.length())
    val angel = acos(cosAlphaUAndY.toDouble())
    val topLeft = PointF(bottomLeft.x, bottomLeft.y - height).rotate(bottomLeft, angel * -1f)
    val topRight = PointF(bottomRight.x, bottomRight.y - height).rotate(bottomRight, angel * -1f)
    return BoundingBox(topLeft, topRight, bottomRight, bottomLeft)
}

fun isTheSameSide(p1: PointF, p2: PointF, pointA: PointF, pointB: PointF): Boolean {
    val d1 = sign((p1.x - pointA.x) * (pointB.y - pointA.y) - (p1.y - pointA.y)*(pointB.x - pointA.x))
    val d2 = sign((p2.x - pointA.x) * (pointB.y - pointA.y) - (p2.y - pointA.y)*(pointB.x - pointA.x))
    return (d1*d2).compareTo(0) >= 0
}

val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()
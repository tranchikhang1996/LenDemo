package com.example.ocrsurface.utils

import android.graphics.PointF
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

infix fun PointF.crossProduct(p: PointF) = x * p.x + y * p.y

fun PointF.rotate(p: PointF, angle: Double): PointF {
    val newX = (x - p.x) * cos(angle) - (y - p.y) * sin(angle) + p.x
    val newY = (x - p.x) * sin(angle) + (y - p.y) * cos(angle) + p.y
    return PointF(newX.toFloat(), newY.toFloat())
}

fun PointF.angle(): Float {
    return acos(x / length())
}
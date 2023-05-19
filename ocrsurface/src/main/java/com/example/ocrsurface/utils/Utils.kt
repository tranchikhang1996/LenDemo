package com.example.ocrsurface.utils

import android.graphics.PointF
import kotlin.math.sign

fun isTheSameSide(p1: PointF, p2: PointF, pointA: PointF, pointB: PointF): Boolean {
    val d1 =
        sign((p1.x - pointA.x) * (pointB.y - pointA.y) - (p1.y - pointA.y) * (pointB.x - pointA.x))
    val d2 =
        sign((p2.x - pointA.x) * (pointB.y - pointA.y) - (p2.y - pointA.y) * (pointB.x - pointA.x))
    return (d1 * d2).compareTo(0) >= 0
}

fun centerOf(p1: PointF, p2: PointF) = PointF((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)

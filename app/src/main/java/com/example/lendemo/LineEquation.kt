package com.example.lendemo

import android.graphics.PointF
import androidx.core.graphics.minus
import kotlin.math.abs
import kotlin.math.sqrt

data class LineEquation(val a: Float, val b: Float, val c: Float) {
    fun clone(p: PointF): LineEquation {
        return LineEquation(a, b, (a * p.x + b * p.y) * -1)
    }

    fun findProjectionPoint(p: PointF): PointF {
        return from(a, b, p).findCrossPoint(this) ?: p
    }

    fun distanceFrom(x: Float, y: Float): Float {
        return abs(a * x + b * y + c) / sqrt(a * a + b * b)
    }

    fun findCrossPoint(other: LineEquation): PointF? {
        return kotlin.runCatching {
            if (a.compareTo(0) == 0) {
                val y = -c / b
                val x = (-other.c - other.b * y) / other.a
                PointF(x, y)
            } else {
                val y = (other.a * c / a - other.c) / (other.b - other.a * b / a)
                val x = (-b * y - c) / a
                PointF(x, y)
            }
        }.getOrNull()
    }

    companion object {
        fun from(p1: PointF, p2: PointF, p0: PointF = p1): LineEquation {
            val directionVector = p2.minus(p1)
            val a = directionVector.y
            val b = directionVector.x * -1
            return LineEquation(a, b, (a * p0.x + b * p0.y) * -1)
        }

        fun from(a: Float, b: Float, p: PointF): LineEquation {
            return LineEquation(b, -a, (b * p.x - a * p.y) * -1)
        }
    }
}
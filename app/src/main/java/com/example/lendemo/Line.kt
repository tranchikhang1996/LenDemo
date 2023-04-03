package com.example.lendemo

import android.graphics.PointF

data class Line(
    val id: Int,
    val text: String,
    val rect: BoundingBox,
    val elements: List<Element>
) {
    fun scale(xRatio: Float, yRatio: Float, p: PointF): Line {
        return Line(
            id,
            text,
            rect.scale(xRatio, yRatio, p),
            elements.map { it.scale(xRatio, yRatio, p) })
    }
}
package com.example.lendemo

import android.graphics.PointF

data class Element(val id: Int, val text: String, val rect: BoundingBox) {
    fun scale(xRatio: Float, yRatio: Float, p: PointF): Element {
        return Element(id, text, rect.scale(xRatio, yRatio, p))
    }
}
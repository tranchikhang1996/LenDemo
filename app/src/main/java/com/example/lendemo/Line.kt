package com.example.lendemo

data class Line(
    val id: Int,
    val text: String,
    val rect: BoundingBox,
    val elements: List<Element>
) {
    fun scale(xRatio: Float, yRatio: Float): Line {
        return Line(
            id,
            text,
            rect.scale(xRatio, yRatio),
            elements.map { it.scale(xRatio, yRatio) })
    }
}
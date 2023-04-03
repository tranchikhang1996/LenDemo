package com.example.lendemo

data class Element(val id: Int, val text: String, val rect: BoundingBox) {
    fun scale(xRatio: Float, yRatio: Float): Element {
        return Element(id, text, rect.scale(xRatio, yRatio))
    }
}
package com.example.ocrsurface.data

data class Line(
    val id: Int,
    val text: String,
    val rect: BoundingBox,
    val elements: List<Element>
)
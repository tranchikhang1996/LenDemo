package com.example.ocrsurface.data

data class OcrResult(val width: Int, val height: Int, val textLines: List<Line> = emptyList())

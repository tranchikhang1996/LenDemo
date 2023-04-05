package com.example.lendemo

import com.google.mlkit.vision.common.InputImage

data class TextImage(val image: InputImage, val textLines: List<Line> = emptyList())

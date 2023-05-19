package com.example.ocrsurface.engine

import android.graphics.Bitmap
import com.example.ocrsurface.data.OcrResult

interface OcrEngine {
    suspend fun process(bitmap: Bitmap): OcrResult
    fun process(bitmap: Bitmap, onResult: ((OcrResult) -> Unit)?)
}


package com.example.lendemo

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import com.example.lendemo.extension.isTheSameSide
import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val p = PointF(554f, 81.54785f)
        val A = PointF(10.062112f, 167.44186f)
        val C = PointF(910.062f, 210.97676f)
        val D = PointF(10.062112f, 210.97676f)
        assertEquals(true, isTheSameSide(p, A, D, C))
    }
}
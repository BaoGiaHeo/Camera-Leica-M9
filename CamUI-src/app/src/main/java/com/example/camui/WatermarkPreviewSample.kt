package com.example.camui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader

/**
 * Deterministic, clean preview source shared by Watermark Panel and Watermark Studio.
 * This bitmap is deliberately not loaded from MediaStore, so a previously stamped
 * capture can never be stamped a second time in the preview.
 */
internal object WatermarkPreviewSample {
    fun create(): Bitmap {
        val width = 960
        val height = 640
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                Color.rgb(46, 92, 126),
                Color.rgb(141, 164, 161),
                Color.rgb(226, 199, 157)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        paint.color = Color.rgb(241, 208, 151)
        canvas.drawCircle(765f, 125f, 46f, paint)

        paint.color = Color.rgb(45, 69, 64)
        canvas.drawOval(505f, 95f, 1_020f, 610f, paint)
        paint.color = Color.rgb(65, 91, 75)
        canvas.drawOval(280f, 250f, 820f, 770f, paint)

        paint.color = Color.rgb(192, 184, 144)
        canvas.drawRect(0f, 458f, width.toFloat(), height.toFloat(), paint)
        paint.color = Color.rgb(103, 132, 127)
        canvas.drawRect(0f, 490f, width.toFloat(), 548f, paint)
        paint.color = Color.rgb(225, 210, 174)
        canvas.drawRect(78f, 412f, 432f, 438f, paint)

        paint.color = Color.rgb(41, 63, 57)
        canvas.drawRect(655f, 335f, 681f, 490f, paint)
        canvas.drawOval(590f, 255f, 755f, 405f, paint)
        return bitmap
    }
}

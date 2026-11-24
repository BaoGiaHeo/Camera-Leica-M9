package com.example.camui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface

/** Measures the active typeface itself; never reads or scales against the removed VCR font. */
internal data class PixelOsdFontMetrics(
    val unitsPerEm: Int,
    val capHeightUnits: Int,
    val source: String
) {
    fun textSizeForCapHeight(capHeightPx: Float): Float {
        val ratio = capHeightUnits.toFloat() / unitsPerEm.coerceAtLeast(1)
        return (capHeightPx / ratio.coerceAtLeast(0.01f)).coerceAtLeast(1f)
    }

    companion object {
        private const val MEASUREMENT_SIZE = 100

        fun load(context: Context, typeface: Typeface): PixelOsdFontMetrics {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = MEASUREMENT_SIZE.toFloat()
            }
            val bounds = android.graphics.Rect()
            paint.getTextBounds("H", 0, 1, bounds)
            val capHeight = bounds.height().coerceIn(1, MEASUREMENT_SIZE - 1)
            return PixelOsdFontMetrics(
                unitsPerEm = MEASUREMENT_SIZE,
                capHeightUnits = capHeight,
                source = "Paint.getTextBounds(H)"
            )
        }
    }
}

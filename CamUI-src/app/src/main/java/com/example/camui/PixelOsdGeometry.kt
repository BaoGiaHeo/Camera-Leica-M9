package com.example.camui

import kotlin.math.roundToInt

/**
 * Pixel-grid geometry shared by the camera LCD OSD renderer and its tests.
 */
data class PixelOsdGeometry(
    val pixelScale: Int,
    val logicWidth: Int,
    val logicHeight: Int
) {
    fun x(normalized: Float): Int =
        (normalized.coerceIn(0f, 1f) * logicWidth).roundToLogicPixel().coerceIn(0, logicWidth - 1)

    fun y(normalized: Float): Int =
        (normalized.coerceIn(0f, 1f) * logicHeight).roundToLogicPixel().coerceIn(0, logicHeight - 1)

    companion object {
        fun fromDisplayPixels(widthPx: Int, heightPx: Int, pixelMode: String = "fine"): PixelOsdGeometry {
            require(widthPx > 0) { "LCD width must be greater than zero." }
            require(heightPx > 0) { "LCD height must be greater than zero." }
            val scale = when (pixelMode.lowercase()) {
                "fine" -> 2
                "medium" -> 3
                "chunky" -> 4
                else -> 2
            }
            return PixelOsdGeometry(
                pixelScale = scale,
                logicWidth = (widthPx / scale).coerceAtLeast(1),
                logicHeight = (heightPx / scale).coerceAtLeast(1)
            )
        }
    }
}

private fun Float.roundToLogicPixel(): Int = kotlin.math.round(this).toInt()

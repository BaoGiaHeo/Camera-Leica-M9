package com.example.camui

import androidx.compose.ui.graphics.Color

/**
 * Central design palette for the Fuji camera chrome.
 * FujiSkin.kt remains the Compose theme implementation; this file owns shared design tokens.
 */
internal object FujiCameraDesign {
    val background = Color(0xFF0E0F10)
    val surface = Color(0xFF1C1E20)
    val elevated = Color(0xFF2A2C2E)
    val stroke = Color(0xFF5A5D60)
    val text = Color(0xFFF2F2F2)
    val muted = Color(0xFF9A9DA1)
    val blue = Color(0xFF3B8EE6)
    val orange = Color(0xFFF0A030)
    val red = Color(0xFFD8283C)
    val green = Color(0xFF5ED08A)

    fun scale(widthDp: Float, heightDp: Float): Float =
        minOf(
            widthDp / Tuning.CAMERA_DESIGN_WIDTH_DP,
            heightDp / Tuning.CAMERA_DESIGN_HEIGHT_DP
        ).coerceIn(Tuning.CAMERA_SCALE_MIN, Tuning.CAMERA_SCALE_MAX)
}

package com.example.camui

import android.content.Context
import kotlin.math.roundToInt

/**
 * One immutable snapshot consumed by both the live preview shader and the JPEG renderer.
 * The preference keys remain stable so a camera restart does not reset the user's look.
 */
internal data class ColorPipelineParams(
    val lutId: String,
    val intensity: Float,
    /** -1 faded / +1 rich. Controls saturation and contrast together. */
    val toneX: Float,
    /** -1 dark / +1 bright. The processors apply highlight roll-off to this exposure. */
    val toneY: Float,
    val highlights: Float,
    val shadows: Float,
    val colorChrome: Float,
    val sharpness: Float,
    val noiseReduction: Float
) {
    companion object {
        fun from(context: Context, lutId: String): ColorPipelineParams {
            val prefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
            return ColorPipelineParams(
                lutId = lutId,
                intensity = prefs.getFloat("film_filter_intensity", 1f).coerceIn(0f, 1f),
                toneX = prefs.getFloat("film_custom_temperature", 0f).coerceIn(-1f, 1f),
                toneY = prefs.getFloat("film_custom_tone", 0f).coerceIn(-1f, 1f),
                highlights = prefs.getFloat("film_custom_highlight", 0f).coerceIn(-1f, 1f),
                shadows = prefs.getFloat("film_custom_shadow", 0f).coerceIn(-1f, 1f),
                colorChrome = prefs.getFloat("film_custom_color_chrome", 0f).coerceIn(0f, 1f),
                sharpness = prefs.getFloat("film_custom_sharpness", 0f).coerceIn(0f, 1f),
                noiseReduction = prefs.getFloat("film_custom_noise_reduction", 0f).coerceIn(0f, 1f)
            )
        }
    }

    /** Radius is proportional to the short edge so preview and full-resolution JPEG scale together. */
    fun spatialRadius(width: Int, height: Int): Int =
        (minOf(width, height).coerceAtLeast(1) * 0.002f).roundToInt().coerceAtLeast(1)
}

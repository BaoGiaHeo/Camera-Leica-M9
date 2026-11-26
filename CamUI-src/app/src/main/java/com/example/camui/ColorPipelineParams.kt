package com.example.camui

import android.content.Context

/**
 * Single source of truth for preview and saved-photo colour rendering.
 * Values are normalized once and then passed unchanged to the AGSL and CPU paths.
 */
internal data class ColorPipelineParams(
    val lutId: String,
    val intensity: Float,
    val toneX: Float,
    val toneY: Float,
    val highlights: Float,
    val shadows: Float,
    val colorChrome: Float,
    val sharpness: Float,
    val denoise: Float
) {
    companion object {
        fun from(context: Context, lutId: String): ColorPipelineParams {
            val preferences = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
            return ColorPipelineParams(
                lutId = lutId,
                intensity = preferences.getFloat("film_custom_intensity", 1f).coerceIn(0f, 1f),
                toneX = preferences.getFloat(
                    "film_custom_tone_x",
                    preferences.getFloat("film_custom_temperature", 0f)
                ).coerceIn(-1f, 1f),
                toneY = preferences.getFloat(
                    "film_custom_tone_y",
                    preferences.getFloat("film_custom_tone", 0f)
                ).coerceIn(-1f, 1f),
                highlights = preferences.getFloat("film_custom_highlight", 0f).coerceIn(-1f, 1f),
                shadows = preferences.getFloat("film_custom_shadow", 0f).coerceIn(-1f, 1f),
                colorChrome = preferences.getFloat("film_custom_color_chrome", 0f).coerceIn(0f, 1f),
                sharpness = preferences.getFloat("film_custom_sharpness", 0f).coerceIn(0f, 1f),
                denoise = preferences.getFloat("film_custom_denoise", 0f).coerceIn(0f, 1f)
            )
        }
    }
}

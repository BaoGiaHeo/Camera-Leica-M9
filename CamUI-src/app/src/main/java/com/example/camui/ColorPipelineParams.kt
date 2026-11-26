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

/**
 * In-memory snapshot shared by the preview shader and the JPEG renderer.
 * UI drags update this object synchronously; persistent preferences are written once
 * after the drag settles, avoiding a disk write for every pointer sample.
 */
internal object ColorPipelineParamsStore {
    @Volatile
    private var currentSnapshot: ColorPipelineParams? = null

    fun update(params: ColorPipelineParams) {
        currentSnapshot = params
    }

    fun current(context: Context, lutId: String): ColorPipelineParams =
        currentSnapshot?.takeIf { it.lutId == lutId } ?: ColorPipelineParams.from(context, lutId)

    fun persist(context: Context, params: ColorPipelineParams) {
        context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
            .edit()
            .putFloat("film_custom_intensity", params.intensity)
            .putFloat("film_custom_tone_x", params.toneX)
            .putFloat("film_custom_tone_y", params.toneY)
            .putFloat("film_custom_temperature", params.toneX)
            .putFloat("film_custom_tone", params.toneY)
            .putFloat("film_custom_highlight", params.highlights)
            .putFloat("film_custom_shadow", params.shadows)
            .putFloat("film_custom_color_chrome", params.colorChrome)
            .putFloat("film_custom_sharpness", params.sharpness)
            .putFloat("film_custom_denoise", params.denoise)
            .apply()
    }
}

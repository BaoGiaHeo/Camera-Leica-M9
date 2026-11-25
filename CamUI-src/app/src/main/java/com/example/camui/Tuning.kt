package com.example.camui

import android.content.Context
import android.graphics.Typeface
import android.util.Log

/**
 * Central camera/UI tuning values. Camera-dependent limits must still come from
 * CameraCharacteristics; these values are policy thresholds, not hardware IDs.
 */
internal object Tuning {
    const val LOW_LIGHT_ISO_ENTER = 1600
    const val LOW_LIGHT_ISO_EXIT = 1100
    const val LOW_LIGHT_LUMA_ENTER = 0.20f
    const val LOW_LIGHT_LUMA_EXIT = 0.24f
    const val LOW_LIGHT_HYSTERESIS_MS = 1_200L
    const val LOW_LIGHT_SCENE_CHANGE_LUMA = 0.12f
    const val LOW_LIGHT_AF_GYRO_THRESHOLD_RAD_S = 0.12f
    const val LOW_LIGHT_SCENE_CHANGE_GYRO_DEGREES = 2.5f
    const val AF_RETRY_MIN_INTERVAL_MS = 900L
    const val AF_TARGET_MOVE_THRESHOLD = 0.055f
    const val SETTINGS_ENTER_MS = 200
    const val SETTINGS_EXIT_MS = 160
    const val SETTINGS_TAB_MS = 160
    const val FILTER_CROSSFADE_MS = 150
    const val BUTTON_PRESS_MS = 80
    const val BUTTON_PRESS_SCALE = 0.96f
    const val ASPECT_RATIO_ANIMATION_MS = 220
    const val JANK_DIAGNOSTICS_ENABLED = true
    const val CAPTURE_DIAGNOSTICS_LIMIT = 30
    const val LUNA_DREAM_30_STACK_MAX_PIXELS = 48_000_000L
    const val LUNA_DREAM_30_WORKER_COUNT = 4
    const val LUNA_DREAM_30_MIN_OUTPUT_PIXELS = 50_000_000L
}

/**
 * Arial assets are optional project inputs. When they are absent, Android's
 * sans-serif family is used rather than failing resource or asset loading.
 */
internal object AppFonts {
    private const val TAG = "FujiAppFonts"
    @Volatile private var regularCache: Typeface? = null
    @Volatile private var boldCache: Typeface? = null

    fun regular(context: Context): Typeface {
        regularCache?.let { return it }
        return synchronized(this) {
            regularCache ?: runCatching {
                Typeface.createFromAsset(context.applicationContext.assets, "fonts/arial.ttf")
            }.onFailure {
                Log.w(TAG, "assets/fonts/arial.ttf is not present; using Android sans-serif.", it)
            }.getOrElse { Typeface.create("sans-serif", Typeface.NORMAL) }
                .also { regularCache = it }
        }
    }

    fun bold(context: Context): Typeface {
        boldCache?.let { return it }
        return synchronized(this) {
            boldCache ?: runCatching {
                Typeface.createFromAsset(context.applicationContext.assets, "fonts/arial_bold.ttf")
            }.onFailure {
                Log.w(TAG, "assets/fonts/arial_bold.ttf is not present; using Android sans-serif bold.", it)
            }.getOrElse { Typeface.create("sans-serif", Typeface.BOLD) }
                .also { boldCache = it }
        }
    }
}

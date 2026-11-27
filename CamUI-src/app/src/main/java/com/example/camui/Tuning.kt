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
    const val CAMERA_SLIDER_COMMAND_INTERVAL_MS = 67L
    const val CAMERA_SLIDER_COMMAND_MAX_HZ = 15
    const val ASPECT_RATIO_ANIMATION_MS = 220
    const val CAMERA_DESIGN_WIDTH_DP = 924f
    const val CAMERA_DESIGN_HEIGHT_DP = 427f
    const val CAMERA_SCALE_MIN = 0.85f
    const val CAMERA_SCALE_MAX = 1.25f
    const val CAMERA_TOP_BAR_HEIGHT_DP = 55f
    const val CAMERA_LEFT_COLUMN_WIDTH_DP = 129f
    const val CAMERA_RIGHT_COLUMN_WIDTH_DP = 314f
    const val CAMERA_ZOOM_KEY_WIDTH_DP = 66f
    const val CAMERA_ZOOM_KEY_HEIGHT_DP = 64f
    const val CAMERA_SIDE_KEY_WIDTH_DP = 70f
    const val CAMERA_SIDE_KEY_HEIGHT_DP = 82f
    const val CAMERA_TIMER_FLASH_WIDTH_DP = 124f
    const val CAMERA_TIMER_FLASH_HEIGHT_DP = 54f
    const val CAMERA_EV_KEY_SIZE_DP = 44f
    const val CAMERA_AF_SETTINGS_WIDTH_DP = 108f
    const val CAMERA_AF_SETTINGS_HEIGHT_DP = 54f
    const val CAMERA_SHUTTER_SIZE_DP = 100f
    const val CAMERA_PREVIEW_BORDER_DP = 5f
    const val CAMERA_PREVIEW_CHIN_EXTRA_DP = 7f
    const val CAMERA_PREVIEW_OUTER_RADIUS_DP = 16f
    const val CAMERA_PREVIEW_INNER_RADIUS_DP = 12f
    const val CAMERA_HUD_WIDTH_FRACTION = 0.58f
    const val CAMERA_HUD_HEIGHT_FRACTION = 0.14f
    const val CAMERA_BUTTON_RADIUS_DP = 12f
    const val CAMERA_BUTTON_SHADOW_DP = 6f
    const val CAMERA_BUTTON_ICON_DP = 24f
    const val CAMERA_BUTTON_LABEL_SP = 12f
    const val CAMERA_BUTTON_SECONDARY_SP = 10f
    const val JANK_DIAGNOSTICS_ENABLED = true
    const val CAPTURE_DIAGNOSTICS_LIMIT = 30
    const val MAX_CONTINUOUS_BURST_FRAMES = 300
    const val BURST_CAPTURE_QUEUE_CAPACITY = 6
    const val CONTINUOUS_BURST_MIN_FPS_TARGET = 3.0
    const val FILM_ROLL_PANEL_WIDTH_FRACTION = 0.26f
    const val FILM_ROLL_PANEL_MIN_WIDTH_DP = 240f
    const val FILM_ROLL_PANEL_MAX_WIDTH_DP = 300f
    const val FILM_ROLL_FILTER_ROW_HEIGHT_DP = 56
    const val FILM_ROLL_TONE_PAD_HEIGHT_DP = 60
    const val FILM_ROLL_RESET_TOUCH_TARGET_DP = 48
    const val FILM_ROLL_RESET_ICON_SIZE_DP = 24
    const val LUNA_DREAM_30_STACK_MAX_PIXELS = 48_000_000L
    const val LUNA_DREAM_30_WORKER_COUNT = 4
    const val LUNA_DREAM_30_MAX_OUTPUT_PIXELS = 180_000_000L
    const val LUNA_DREAM_30_MIN_OUTPUT_PIXELS = LUNA_DREAM_30_MAX_OUTPUT_PIXELS
    const val LUNA_DREAM_30_STAGE_WATCHDOG_FACTOR = 2L
    const val LUNA_DREAM_30_EXIF_BUDGET_MS = 1_000L
    const val LUNA_DREAM_30_MERGE_WATCHDOG_MS = 60_000L
    const val LUNA_DREAM_30_DECODE_BUDGET_MS = 4_000L
    const val LUNA_DREAM_30_ALIGN_BUDGET_MS = 3_000L
    const val LUNA_DREAM_30_FUSION_BUDGET_MS = 5_000L
    const val LUNA_DREAM_30_DETAIL_BUDGET_MS = 10_000L
    const val LUNA_DREAM_30_ENCODE_BUDGET_MS = 10_000L
    const val LUNA_DREAM_30_PUBLISH_BUDGET_MS = 5_000L
    const val LUNA_DREAM_30_VERIFY_BUDGET_MS = 2_000L
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

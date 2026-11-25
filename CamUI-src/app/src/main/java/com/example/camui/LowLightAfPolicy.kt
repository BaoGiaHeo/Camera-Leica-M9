package com.example.camui

/**
 * Hysteresis for low-light detection. A scene must remain beyond the enter/exit
 * threshold for the full hold interval before AF policy changes.
 */
internal data class LowLightAfTransition(
    val active: Boolean,
    val candidateStartedAtMs: Long,
    val changed: Boolean
)

internal fun updateLowLightAfTransition(
    normalizedLuma: Float,
    active: Boolean,
    candidateStartedAtMs: Long,
    nowMs: Long,
    enterThreshold: Float = Tuning.LOW_LIGHT_LUMA_ENTER,
    exitThreshold: Float = Tuning.LOW_LIGHT_LUMA_EXIT,
    holdDurationMs: Long = Tuning.LOW_LIGHT_HYSTERESIS_MS
): LowLightAfTransition {
    if (!normalizedLuma.isFinite()) {
        return LowLightAfTransition(active, 0L, false)
    }
    val luma = normalizedLuma.coerceIn(0f, 1f)
    val candidate = if (active) luma >= exitThreshold else luma <= enterThreshold
    if (!candidate) return LowLightAfTransition(active, 0L, false)
    val since = candidateStartedAtMs.takeIf { it > 0L } ?: nowMs.coerceAtLeast(1L)
    return if (nowMs - since >= holdDurationMs.coerceAtLeast(0L)) {
        LowLightAfTransition(!active, 0L, true)
    } else {
        LowLightAfTransition(active, since, false)
    }
}

internal fun lowLightAfRetryIntervalMs(lowLight: Boolean): Long =
    if (lowLight) Tuning.AF_RETRY_MIN_INTERVAL_MS else 200L

internal fun lowLightAfMovementThreshold(lowLight: Boolean): Float =
    if (lowLight) Tuning.AF_TARGET_MOVE_THRESHOLD else 0.025f

internal fun lowLightLuminanceSceneChanged(
    previousAfLuma: Float,
    currentLuma: Float,
    lowLight: Boolean
): Boolean = lowLight && currentLuma.isFinite() &&
    (!previousAfLuma.isFinite() ||
        kotlin.math.abs(currentLuma - previousAfLuma) >= Tuning.LOW_LIGHT_SCENE_CHANGE_LUMA)

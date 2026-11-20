package com.example.camui

import java.util.Locale
import kotlin.math.ln

/**
 * Normalizes the selected Pro control to its actual position on the displayed
 * value scale. The return value is a display-only fraction in [0, 1].
 */
internal fun cameraControlProgress(
    selectedControl: String,
    evIndex: Float,
    evMinimumIndex: Int,
    evMaximumIndex: Int,
    shutterTimeNanos: Long,
    isoValue: Int,
    manualFocusPosition: Float,
    whiteBalanceKelvin: Int,
    manualFocusEnabled: Boolean = false,
    manualWhiteBalanceEnabled: Boolean = false
): Float {
    return when (selectedControl.uppercase(Locale.US)) {
        "EV" -> {
            val span = (evMaximumIndex - evMinimumIndex).toFloat()
            if (span <= 0f) 0.5f else ((evIndex - evMinimumIndex) / span).coerceIn(0f, 1f)
        }
        "TV", "SHUTTER" -> {
            if (shutterTimeNanos <= 0L) {
                0.5f
            } else {
                (ln(1_000_000_000.0 / shutterTimeNanos.coerceIn(125_000L, 1_000_000_000L)) /
                    ln(8_000.0)).toFloat().coerceIn(0f, 1f)
            }
        }
        "ISO" -> {
            if (isoValue <= 0) 0.5f else {
                val safeIso = isoValue.coerceIn(50, 51_200).toFloat()
                (ln(safeIso / 50f) / ln(51_200f / 50f)).coerceIn(0f, 1f)
            }
        }
        "AF", "MF" -> if (manualFocusEnabled) {
            manualFocusPosition.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
        } else 0.5f
        "WB", "TONE" -> if (manualWhiteBalanceEnabled) {
            ((whiteBalanceKelvin - 2_000f) / 8_000f).coerceIn(0f, 1f)
        } else 0.5f
        else -> 0.5f
    }
}

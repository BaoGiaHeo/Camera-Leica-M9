package com.example.camui

/**
 * Keeps user-entered location text separate from its display preference.
 * The saved value is never erased merely because the display toggle is off.
 */
internal fun watermarkLocationForDisplay(enabled: Boolean, savedLocation: String?): String {
    if (!enabled) return ""
    return savedLocation?.trim().orEmpty()
}

/**
 * Returns the text baseline that places the visible capital-height box around
 * [centerY], independent of font descent/leading metrics.
 */
internal fun watermarkBaselineFromCapHeight(centerY: Float, capHeight: Float): Float {
    return centerY + capHeight.coerceAtLeast(0f) * 0.5f
}

package com.example.camui

/**
 * Shared watermark geometry rules. All dimensions use the short side of the
 * final upright output image as their reference unit.
 */
internal fun watermarkFooterFractionForRows(rows: Int): Float = when (rows) {
    1 -> 0.075f
    2 -> 0.095f
    else -> 0.11f
}

internal fun watermarkLogoHeightFraction(size: String?): Float = when (size?.uppercase()) {
    "S" -> 0.020f
    "L" -> 0.034f
    else -> 0.026f
}

/**
 * Returns true when a light footer is required for legible dark text.
 * Dark images receive a light plate; bright images receive a dark plate.
 */
internal fun watermarkAutoUsesLightPlate(luminance: Float): Boolean =
    luminance.isFinite() && luminance <= 0.5f

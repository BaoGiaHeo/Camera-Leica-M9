package com.example.camui

/**
 * Converts a horizontal pointer coordinate into a clamped slider value.
 * Invalid widths or coordinates fall back to the start of the range.
 */
internal fun filterSliderValueFromPosition(
    positionX: Float,
    trackWidth: Float,
    valueRange: ClosedFloatingPointRange<Float>
): Float {
    val minimum = valueRange.start
    val maximum = valueRange.endInclusive
    if (!minimum.isFinite() || !maximum.isFinite() || maximum <= minimum) {
        return minimum.takeIf { it.isFinite() } ?: 0f
    }
    if (!trackWidth.isFinite() || trackWidth <= 0f) return minimum
    val fraction = (positionX / trackWidth)
        .takeIf { it.isFinite() }
        ?.coerceIn(0f, 1f)
        ?: 0f
    return (minimum + (maximum - minimum) * fraction).coerceIn(minimum, maximum)
}

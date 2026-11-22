package com.example.camui

import kotlin.math.abs

/** Camera2 zoom restore is considered acknowledged within one percent of the request. */
internal fun isZoomWithinRestoreTolerance(
    observedRatio: Float,
    requestedRatio: Float,
    toleranceFraction: Float = 0.01f
): Boolean {
    if (!observedRatio.isFinite() || !requestedRatio.isFinite() || requestedRatio <= 0f) return false
    val tolerance = requestedRatio * toleranceFraction.coerceIn(0f, 0.1f)
    return abs(observedRatio - requestedRatio) <= tolerance
}

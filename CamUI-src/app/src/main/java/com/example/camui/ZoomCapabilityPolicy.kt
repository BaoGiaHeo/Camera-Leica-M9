package com.example.camui

import kotlin.math.abs
import kotlin.math.ln

internal data class OpticalZoomAssessment(
    val mainEquivalent35Mm: Float?,
    val longestEquivalent35Mm: Float?,
    val opticalRatio: Float?,
    val hasOpticalTelephoto: Boolean
)

internal fun assessOpticalZoom(
    equivalentFocalLengths35Mm: List<Float>,
    mainEquivalent35Mm: Float? = null,
    opticalThreshold: Float = 1.8f
): OpticalZoomAssessment {
    val safeLenses = equivalentFocalLengths35Mm
        .filter { it.isFinite() && it > 0f }
        .distinct()
    val main = mainEquivalent35Mm
        ?.takeIf { it.isFinite() && it > 0f }
        ?: safeLenses.minByOrNull { abs(it - 26f) }
    val longest = safeLenses.maxOrNull()
    val ratio = if (main != null && longest != null && main > 0f) longest / main else null
    val threshold = opticalThreshold.takeIf { it.isFinite() && it > 1f } ?: 1.8f
    return OpticalZoomAssessment(
        mainEquivalent35Mm = main,
        longestEquivalent35Mm = longest,
        opticalRatio = ratio,
        hasOpticalTelephoto = ratio != null && ratio >= threshold
    )
}

/**
 * CameraX accepts only ratios inside the active camera's advertised range.
 * This function returns the camera-controlled range; any zoom above it needs
 * a separate digital crop path and must not be sent to CameraControl.setZoomRatio.
 */
internal fun selectableCameraZoomMax(
    cameraReportedMax: Float,
    assessment: OpticalZoomAssessment
): Float {
    val reported = cameraReportedMax.takeIf { it.isFinite() && it >= 1f } ?: 1f
    val productLimit = if (assessment.hasOpticalTelephoto) 50f else 2f
    return minOf(reported, productLimit).coerceAtLeast(1f)
}

internal fun logarithmicZoomPosition(
    zoomRatio: Float,
    minimumRatio: Float,
    maximumRatio: Float
): Float {
    val min = minimumRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
    val max = maximumRatio.takeIf { it.isFinite() && it > min } ?: min
    if (max <= min) return 0f
    val zoom = zoomRatio.takeIf { it.isFinite() && it > 0f } ?: min
    return (ln(zoom.coerceIn(min, max) / min) / ln(max / min)).toFloat().coerceIn(0f, 1f)
}

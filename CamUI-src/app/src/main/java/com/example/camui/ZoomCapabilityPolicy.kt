package com.example.camui

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.hypot

internal data class EquivalentFocalRange35Mm(
    val shortest: Float?,
    val longest: Float?
)

/** Converts every valid advertised focal length into a 35mm-equivalent range. */
internal fun calculateEquivalentFocalRange35Mm(
    focalLengthsMm: List<Float>,
    sensorWidthMm: Float?,
    sensorHeightMm: Float?
): EquivalentFocalRange35Mm {
    val width = sensorWidthMm?.takeIf { it.isFinite() && it > 0f }
    val height = sensorHeightMm?.takeIf { it.isFinite() && it > 0f }
    if (width == null || height == null) return EquivalentFocalRange35Mm(null, null)
    val diagonal = hypot(width, height)
    if (!diagonal.isFinite() || diagonal <= 0f) return EquivalentFocalRange35Mm(null, null)
    val equivalent = focalLengthsMm
        .filter { it.isFinite() && it > 0f }
        .map { it * 43.27f / diagonal }
        .filter { it.isFinite() && it > 0f }
    return EquivalentFocalRange35Mm(
        shortest = equivalent.minOrNull(),
        longest = equivalent.maxOrNull()
    )
}

data class OpticalZoomAssessment(
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

internal fun advanceLogarithmicZoomRatio(
    currentZoom: Float,
    direction: Int,
    minimumRatio: Float,
    maximumRatio: Float,
    deltaSeconds: Float,
    traversalSeconds: Float
): Float {
    val minimum = minimumRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
    val maximum = maximumRatio.takeIf { it.isFinite() && it >= minimum } ?: minimum
    val current = currentZoom.takeIf { it.isFinite() && it > 0f }?.coerceIn(minimum, maximum) ?: minimum
    if (direction == 0 || deltaSeconds <= 0f || !deltaSeconds.isFinite() || maximum <= minimum) return current
    val availableLogTravel = if (direction > 0) {
        ln(maximum.coerceAtLeast(1f) / 1f)
    } else {
        ln(1f / minimum.coerceAtMost(1f))
    }
    if (availableLogTravel <= 0f) return current
    val duration = traversalSeconds.takeIf { it.isFinite() && it > 0f } ?: 1f
    val factor = kotlin.math.exp((availableLogTravel / duration) * deltaSeconds.coerceAtMost(0.05f))
    return (if (direction > 0) current * factor else current / factor).coerceIn(minimum, maximum)
}

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

internal data class ZoomExecutionPlan(
    val effectiveZoomRatio: Float,
    val cameraControlZoomRatio: Float,
    val digitalCropRatio: Float,
    val cameraControlMaximum: Float,
    val totalSelectableMaximum: Float
)

internal fun maxTotalSelectableZoom(
    cameraReportedMaximum: Float,
    assessment: OpticalZoomAssessment
): Float {
    val reported = cameraReportedMaximum.takeIf { it.isFinite() && it >= 1f } ?: 1f
    val opticalMaximum = assessment.opticalRatio
        ?.takeIf { assessment.hasOpticalTelephoto && it.isFinite() && it >= 1f }
    return if (opticalMaximum != null) {
        minOf(opticalMaximum, reported)
    } else {
        // Without an independently verified rear telephoto, preserve the product's
        // 2x ceiling even when the HAL advertises a larger digital ratio.
        minOf(2f, reported.coerceAtLeast(2f))
    }
}

/**
 * Splits the requested total ratio into CameraX-controlled zoom and an additional
 * preview/photo crop. The digital crop is deliberately never sent to CameraX.
 */
internal fun resolveZoomExecutionPlan(
    requestedZoomRatio: Float,
    minimumCameraRatio: Float,
    cameraReportedMaximum: Float,
    assessment: OpticalZoomAssessment
): ZoomExecutionPlan {
    val minimum = minimumCameraRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
    val cameraMaximum = selectableCameraZoomMax(cameraReportedMaximum, assessment)
        .coerceAtLeast(minimum)
    val totalMaximum = maxTotalSelectableZoom(cameraReportedMaximum, assessment)
        .coerceAtLeast(minimum)
    val requested = requestedZoomRatio
        .takeIf { it.isFinite() && it > 0f }
        ?.coerceIn(minimum, totalMaximum)
        ?: minimum
    val cameraRatio = requested.coerceAtMost(cameraMaximum).coerceAtLeast(minimum)
    val digitalCrop = (requested / cameraRatio).coerceAtLeast(1f)
    return ZoomExecutionPlan(
        effectiveZoomRatio = requested,
        cameraControlZoomRatio = cameraRatio,
        digitalCropRatio = digitalCrop,
        cameraControlMaximum = cameraMaximum,
        totalSelectableMaximum = totalMaximum
    )
}

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
    val rawRatio = if (main != null && longest != null && main > 0f) longest / main else null
    val threshold = opticalThreshold.takeIf { it.isFinite() && it > 1f } ?: 1.8f
    val hasTelephoto = rawRatio != null && rawRatio >= threshold
    // The requested product boundary is floored to one decimal, never rounded up.
    val opticalRatio = rawRatio
        ?.takeIf { it.isFinite() && it > 0f }
        ?.let { kotlin.math.floor(it * 10f) / 10f }
    return OpticalZoomAssessment(
        mainEquivalent35Mm = main,
        longestEquivalent35Mm = longest,
        opticalRatio = opticalRatio,
        hasOpticalTelephoto = hasTelephoto
    )
}

/**
 * CameraX accepts only ratios inside the active camera's advertised range.
 * On a verified telephoto device the optical boundary is also the hard product
 * limit; on a device without telephoto, total selection remains capped at 2x.
 */
internal fun selectableCameraZoomMax(
    cameraReportedMax: Float,
    assessment: OpticalZoomAssessment
): Float {
    val reported = cameraReportedMax.takeIf { it.isFinite() && it >= 1f } ?: 1f
    val opticalMaximum = assessment.opticalRatio
        ?.takeIf { assessment.hasOpticalTelephoto && it.isFinite() && it >= 1f }
    val productLimit = opticalMaximum ?: 2f
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
        // Preserve the requested 1x -> maximum traversal time (for example, 1.2s to 2x).
        ln(maximum.coerceAtLeast(1f) / 1f)
    } else {
        // Zooming out from a telephoto position must traverse the full logarithmic
        // range; using only 1x -> minimum made 100x -> 0.6x take far too long.
        ln(maximum.coerceAtLeast(minimum) / minimum)
    }
    if (availableLogTravel <= 0f) return current
    val duration = traversalSeconds.takeIf { it.isFinite() && it > 0f } ?: 1f
    val factor = kotlin.math.exp((availableLogTravel / duration) * deltaSeconds.coerceAtMost(0.05f))
    return (if (direction > 0) current * factor else current / factor).coerceIn(minimum, maximum)
}


/**
 * Returns the final physical-lens focal-length boundary expressed as a ratio
 * against the main rear camera. Camera API upper bounds are applied separately
 * by maxTotalSelectableZoom().
 */
internal fun opticalZoomBoundaryRatio(
    assessment: OpticalZoomAssessment,
    minimumZoomRatio: Float = 1f
): Float {
    val minimum = minimumZoomRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
    val physicalBoundary = if (assessment.hasOpticalTelephoto) {
        assessment.opticalRatio
            ?.takeIf { it.isFinite() && it >= 1f }
            ?: minimum
    } else {
        // Without a separate telephoto lens, the optical segment ends at the
        // active camera's minimum ratio; the rest of the selectable range is crop.
        minimum
    }
    return maxOf(minimum, physicalBoundary)
}

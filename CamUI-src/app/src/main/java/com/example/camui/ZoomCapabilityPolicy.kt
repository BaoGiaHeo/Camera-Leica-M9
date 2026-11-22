package com.example.camui

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.hypot
import kotlin.math.floor

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
): Float = selectableCameraZoomMax(cameraReportedMaximum, assessment)

/**
 * Clamps the requested zoom to the disclosed optical maximum and the active
 * CameraX/Camera2 maximum. The legacy crop ratio remains exactly 1 so no digital
 * zoom extension is applied by preview or capture processing.
 */
internal fun resolveZoomExecutionPlan(
    requestedZoomRatio: Float,
    minimumCameraRatio: Float,
    cameraReportedMaximum: Float,
    assessment: OpticalZoomAssessment
): ZoomExecutionPlan {
    val minimum = minimumCameraRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
    val totalMaximum = maxTotalSelectableZoom(cameraReportedMaximum, assessment)
        .coerceAtLeast(minimum)
    val requested = requestedZoomRatio
        .takeIf { it.isFinite() && it > 0f }
        ?.coerceIn(minimum, totalMaximum)
        ?: minimum
    // Never request a camera ratio outside its public HAL range and never add a
    // digital crop segment beyond the disclosed optical zoom maximum.
    val cameraRatio = requested.coerceAtMost(totalMaximum).coerceAtLeast(minimum)
    val cameraMaximum = totalMaximum
    val digitalCrop = 1f
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
    opticalThreshold: Float = 1.05f
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
 * This function returns the lower of that public range and the physical optical
 * ratio. No digital zoom segment is available above this maximum.
 */
internal fun selectableCameraZoomMax(
    cameraReportedMax: Float,
    assessment: OpticalZoomAssessment
): Float {
    val reported = cameraReportedMax.takeIf { it.isFinite() && it >= 1f } ?: 1f
    val opticalMaximum = if (assessment.hasOpticalTelephoto) {
        val ratio = assessment.opticalRatio
            ?.takeIf { it.isFinite() && it >= 1f }
            ?: 1f
        floor(ratio * 10f) / 10f
    } else {
        2f
    }
    return minOf(reported, opticalMaximum).coerceAtLeast(1f)
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
 * against the main rear camera. It is distinct from the HAL/camera-control
 * maximum, after which the renderer must use its additional GPU crop path.
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

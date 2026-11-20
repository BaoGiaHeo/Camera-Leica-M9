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

/**
 * Resolves the displayed zoom from Camera2 result metadata first. The crop
 * ratio is the fallback on older Android versions; CameraX's observable zoom
 * preserves sub-1x ultra-wide values when the HAL does not publish zoom ratio.
 */
internal fun resolveCaptureZoomRatio(
    controlZoomRatio: Float?,
    activeArrayWidth: Int?,
    cropRegionWidth: Int?,
    fallbackZoomRatio: Float?
): Float? {
    val reportedRatio = controlZoomRatio?.takeIf {
        it.isFinite() && it in 0.1f..100f
    }
    if (reportedRatio != null) return reportedRatio

    val cropRatio = if (
        activeArrayWidth != null && activeArrayWidth > 0 &&
        cropRegionWidth != null && cropRegionWidth > 0
    ) {
        (activeArrayWidth.toFloat() / cropRegionWidth.toFloat())
            .takeIf { it.isFinite() && it >= 1f && it <= 100f }
    } else {
        null
    }
    if (cropRatio != null && cropRatio > 1.005f) return cropRatio

    val cameraXRatio = fallbackZoomRatio?.takeIf {
        it.isFinite() && it in 0.1f..100f
    }
    return cameraXRatio ?: cropRatio
}


/**
 * Temporary preview-only scale compensation for a commanded camera zoom.
 * CameraX remains responsible for the actual sensor crop. The inverse scale
 * is clamped to ±15% and animates back to 1.0 within the OSD transition window.
 */
internal fun resolvePreviewZoomCompensation(
    startZoomRatio: Float,
    targetZoomRatio: Float
): Float {
    if (!startZoomRatio.isFinite() || !targetZoomRatio.isFinite() ||
        startZoomRatio <= 0f || targetZoomRatio <= 0f
    ) return 1f
    return (startZoomRatio / targetZoomRatio).coerceIn(0.85f, 1.15f)
}


/**
 * Text shown in the Pro OSD for the currently requested capture pipeline.
 * DR100 is the base still-photo profile; HDR is displayed only when the
 * HDRbyHDMP software pipeline can actually be selected for this capture.
 * This intentionally does not claim that a generic DR200/DR400 control exists.
 */
internal fun proDynamicRangeLabel(
    hdrEnabled: Boolean,
    isPhotoMode: Boolean,
    nightShotEnabled: Boolean,
    rawCaptureActive: Boolean,
    manualExposure: Boolean,
    manualIsoEnabled: Boolean,
    manualShutterEnabled: Boolean,
    nativeImageProcessingEnabled: Boolean,
    livePhotoEnabled: Boolean
): String {
    val hdrCaptureRequested = !nativeImageProcessingEnabled &&
        !livePhotoEnabled &&
        hdrEnabled &&
        isPhotoMode &&
        !nightShotEnabled &&
        !rawCaptureActive &&
        !(manualExposure && (manualIsoEnabled || manualShutterEnabled))
    return if (hdrCaptureRequested) "HDR" else "DR100"
}

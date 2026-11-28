package com.example.camui

internal data class LunaDreamOutputTarget(
    val targetPixels: Long
)

/** One source of truth for the model picker and the final JPEG output pipeline. */
internal fun lunaDreamOutputTarget(modeId: String): LunaDreamOutputTarget? = when (modeId) {
    "luna15" -> LunaDreamOutputTarget(50_000_000L)
    "luna20" -> LunaDreamOutputTarget(108_000_000L)
    "luna25" -> LunaDreamOutputTarget(180_000_000L)
    "luna30" -> LunaDreamOutputTarget(Tuning.LUNA_DREAM_30_MAX_OUTPUT_PIXELS)
    else -> null
}

/**
 * Calculate the output dimensions using the same pure geometry function as the JPEG writer.
 * The virtual source only supplies a center-crop rectangle; no bitmap is allocated here.
 */
internal fun lunaDreamOutputDimensions(
    modeId: String,
    targetHeightOverWidth: Float
): Pair<Int, Int>? {
    val target = lunaDreamOutputTarget(modeId) ?: return null
    val safeRatio = targetHeightOverWidth.takeIf { it.isFinite() && it > 0f } ?: (3f / 4f)
    val alignment = if (target.targetPixels >= Tuning.LUNA_DREAM_30_MIN_OUTPUT_PIXELS) 16 else 1
    val geometry = PhotoOutputGeometry.calculate(
        sourceWidth = 65_000,
        sourceHeight = 65_000,
        targetPixels = target.targetPixels,
        targetHeightOverWidth = safeRatio,
        dimensionAlignment = alignment
    )
    return geometry.outputWidth to geometry.outputHeight
}

/**
 * Eligibility for multi-frame LunaDream processing. The actual CameraX output format is
 * authoritative; a remembered RAW preference must not reject a JPEG-only Pro capture.
 */
internal object LunaDreamCapturePolicy {
    fun supportsStackedJpegCapture(
        cameraMode: String,
        imageCaptureUsesRaw: Boolean,
        ultraHdr: Boolean,
        burstMode: Boolean,
        hdrBracketMode: Boolean,
        focusBracketMode: Boolean,
        nativeProcessingEnabled: Boolean,
        livePhotoEnabled: Boolean
    ): Boolean =
        (cameraMode == "Photo" || cameraMode == "Pro") &&
            !imageCaptureUsesRaw &&
            !ultraHdr &&
            !burstMode &&
            !hdrBracketMode &&
            !focusBracketMode &&
            !nativeProcessingEnabled &&
            !livePhotoEnabled

    /** RawDream's RAW+JPEG capability check applies to the app's only capture mode: Pro. */
    fun shouldRejectRawDreamFormatMismatch(
        cameraMode: String,
        rawDreamSelected: Boolean,
        imageCaptureUsesRawJpeg: Boolean
    ): Boolean =
        cameraMode == "Pro" && rawDreamSelected && !imageCaptureUsesRawJpeg
}

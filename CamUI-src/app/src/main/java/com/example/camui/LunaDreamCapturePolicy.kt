package com.example.camui

internal data class LunaDreamOutputTarget(
    val resolutionLabel: String,
    val targetPixels: Long
)

/** One source of truth for the model picker and the final JPEG output pipeline. */
internal fun lunaDreamOutputTarget(modeId: String): LunaDreamOutputTarget? = when (modeId) {
    "luna15" -> LunaDreamOutputTarget("FHD+", 50_000_000L)
    "luna20" -> LunaDreamOutputTarget("UHD", 108_000_000L)
    "luna25" -> LunaDreamOutputTarget("UHD+", 150_000_000L)
    "luna30" -> LunaDreamOutputTarget("EUHD", Tuning.LUNA_DREAM_30_MAX_OUTPUT_PIXELS)
    else -> null
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

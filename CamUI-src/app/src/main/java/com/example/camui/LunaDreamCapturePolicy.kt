package com.example.camui

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

    /** RawDream's RAW+JPEG capability check is a Photo-only gate, never a Pro gate. */
    fun shouldRejectRawDreamFormatMismatch(
        cameraMode: String,
        rawDreamSelected: Boolean,
        imageCaptureUsesRawJpeg: Boolean
    ): Boolean =
        cameraMode == "Photo" && rawDreamSelected && !imageCaptureUsesRawJpeg
}

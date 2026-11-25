package com.example.camui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LunaDreamCapturePolicyTest {
    private fun allowed(
        cameraMode: String,
        imageCaptureUsesRaw: Boolean = false,
        ultraHdr: Boolean = false,
        burstMode: Boolean = false,
        hdrBracketMode: Boolean = false,
        focusBracketMode: Boolean = false,
        nativeProcessingEnabled: Boolean = false,
        livePhotoEnabled: Boolean = false
    ): Boolean = LunaDreamCapturePolicy.supportsStackedJpegCapture(
        cameraMode = cameraMode,
        imageCaptureUsesRaw = imageCaptureUsesRaw,
        ultraHdr = ultraHdr,
        burstMode = burstMode,
        hdrBracketMode = hdrBracketMode,
        focusBracketMode = focusBracketMode,
        nativeProcessingEnabled = nativeProcessingEnabled,
        livePhotoEnabled = livePhotoEnabled
    )

    @Test
    fun allowsPhotoJpegCapture() {
        assertTrue(allowed("Photo"))
    }

    @Test
    fun allowsProJpegEvenWhenSavedRawPreferenceWasPreviouslySelected() {
        // The policy receives the actual CameraX output format, not the stale RAW preference.
        assertTrue(allowed("Pro", imageCaptureUsesRaw = false))
    }

    @Test
    fun rawDreamFormatMismatchGuardDoesNotRejectProForPersistedRawPreference() {
        assertFalse(
            LunaDreamCapturePolicy.shouldRejectRawDreamFormatMismatch(
                cameraMode = "Pro",
                rawDreamSelected = true,
                imageCaptureUsesRawJpeg = false
            )
        )
    }

    @Test
    fun rawDreamFormatMismatchGuardStillProtectsPhotoMode() {
        assertTrue(
            LunaDreamCapturePolicy.shouldRejectRawDreamFormatMismatch(
                cameraMode = "Photo",
                rawDreamSelected = true,
                imageCaptureUsesRawJpeg = false
            )
        )
        assertFalse(
            LunaDreamCapturePolicy.shouldRejectRawDreamFormatMismatch(
                cameraMode = "Photo",
                rawDreamSelected = true,
                imageCaptureUsesRawJpeg = true
            )
        )
    }

    @Test
    fun rejectsActualRawOutput() {
        assertFalse(allowed("Pro", imageCaptureUsesRaw = true))
    }

    @Test
    fun rejectsUnsupportedCaptureCombinations() {
        assertFalse(allowed("Pro", ultraHdr = true))
        assertFalse(allowed("Pro", burstMode = true))
        assertFalse(allowed("Pro", hdrBracketMode = true))
        assertFalse(allowed("Pro", focusBracketMode = true))
        assertFalse(allowed("Pro", nativeProcessingEnabled = true))
        assertFalse(allowed("Pro", livePhotoEnabled = true))
        assertFalse(allowed("Video"))
    }
}

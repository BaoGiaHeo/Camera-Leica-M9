package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraControlProgressTest {
    private fun progress(
        control: String,
        ev: Float = 0f,
        min: Int = -9,
        max: Int = 9,
        shutter: Long = 0L,
        iso: Int = 100,
        focus: Float = 0.5f,
        wb: Int = 5_000
    ) = cameraControlProgress(control, ev, min, max, shutter, iso, focus, wb)

    @Test
    fun evUsesTheActualExposureCompensationRange() {
        assertEquals(0f, progress("EV", ev = -9f), 0.0001f)
        assertEquals(0.5f, progress("EV", ev = 0f), 0.0001f)
        assertEquals(1f, progress("EV", ev = 9f), 0.0001f)
    }

    @Test
    fun shutterAndIsoUseLogarithmicValuePositions() {
        assertEquals(0f, progress("TV", shutter = 1_000_000_000L), 0.0001f)
        assertEquals(1f, progress("TV", shutter = 125_000L), 0.0001f)
        assertEquals(0f, progress("ISO", iso = 50), 0.0001f)
        assertEquals(1f, progress("ISO", iso = 51_200), 0.0001f)
        assertEquals(0.5f, progress("ISO", iso = 0), 0.0001f)
    }

    @Test
    fun focusAndWhiteBalanceUseTheirNormalizedPositions() {
        assertEquals(0.5f, progress("AF", focus = 0f), 0.0001f)
        assertEquals(0f, cameraControlProgress("AF", 0f, -9, 9, 0L, 100, 0f, 5_000, manualFocusEnabled = true), 0.0001f)
        assertEquals(1f, cameraControlProgress("MF", 0f, -9, 9, 0L, 100, 1f, 5_000, manualFocusEnabled = true), 0.0001f)
        assertEquals(0.5f, progress("WB", wb = 2_000), 0.0001f)
        assertEquals(0f, cameraControlProgress("WB", 0f, -9, 9, 0L, 100, 0.5f, 2_000, manualWhiteBalanceEnabled = true), 0.0001f)
        assertEquals(1f, cameraControlProgress("TONE", 0f, -9, 9, 0L, 100, 0.5f, 10_000, manualWhiteBalanceEnabled = true), 0.0001f)
    }

    @Test
    fun captureResultZoomPrefersCamera2RatioThenCropAndCameraXFallback() {
        assertEquals(
            0.6f,
            resolveCaptureZoomRatio(0.6f, 6000, 6000, 1f)!!,
            0.0001f
        )
        assertEquals(
            2f,
            resolveCaptureZoomRatio(null, 6000, 3000, 1f)!!,
            0.0001f
        )
        assertEquals(
            0.6f,
            resolveCaptureZoomRatio(null, 6000, 6000, 0.6f)!!,
            0.0001f
        )
        assertEquals(
            1f,
            resolveCaptureZoomRatio(null, 6000, 0, 1f)!!,
            0.0001f
        )
        assertTrue(resolveCaptureZoomRatio(Float.NaN, 0, 0, Float.NaN) == null)
    }

    @Test
    fun proDynamicRangeLabelMatchesTheCapturePipelineSelection() {
        fun label(
            hdr: Boolean = true,
            photo: Boolean = true,
            night: Boolean = false,
            raw: Boolean = false,
            manual: Boolean = false,
            iso: Boolean = false,
            shutter: Boolean = false,
            oem: Boolean = false,
            live: Boolean = false
        ) = proDynamicRangeLabel(hdr, photo, night, raw, manual, iso, shutter, oem, live)

        assertEquals("HDR", label())
        assertEquals("DR100", label(hdr = false))
        assertEquals("DR100", label(photo = false))
        assertEquals("DR100", label(night = true))
        assertEquals("DR100", label(raw = true))
        assertEquals("DR100", label(manual = true, iso = true))
        assertEquals("DR100", label(manual = true, shutter = true))
        assertEquals("HDR", label(manual = true))
        assertEquals("DR100", label(oem = true))
        assertEquals("DR100", label(live = true))
    }

    @Test
    fun invalidOrUnavailableControlsUseTheMidpoint() {
        assertEquals(0.5f, progress("TV", shutter = 0L), 0.0001f)
        assertEquals(0.5f, progress("EV", ev = 0f, min = 2, max = 2), 0.0001f)
        assertEquals(0.5f, progress("RESET"), 0.0001f)
    }
}

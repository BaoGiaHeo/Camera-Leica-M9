package com.example.camui

import org.junit.Assert.assertEquals
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
    fun invalidOrUnavailableControlsUseTheMidpoint() {
        assertEquals(0.5f, progress("TV", shutter = 0L), 0.0001f)
        assertEquals(0.5f, progress("EV", ev = 0f, min = 2, max = 2), 0.0001f)
        assertEquals(0.5f, progress("RESET"), 0.0001f)
    }
}

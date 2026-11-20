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
    }

    @Test
    fun focusAndWhiteBalanceUseTheirNormalizedPositions() {
        assertEquals(0f, progress("AF", focus = 0f), 0.0001f)
        assertEquals(1f, progress("MF", focus = 1f), 0.0001f)
        assertEquals(0f, progress("WB", wb = 2_000), 0.0001f)
        assertEquals(1f, progress("TONE", wb = 10_000), 0.0001f)
    }

    @Test
    fun invalidOrUnavailableControlsUseTheMidpoint() {
        assertEquals(0.5f, progress("TV", shutter = 0L), 0.0001f)
        assertEquals(0.5f, progress("EV", ev = 0f, min = 2, max = 2), 0.0001f)
        assertEquals(0.5f, progress("RESET"), 0.0001f)
    }
}

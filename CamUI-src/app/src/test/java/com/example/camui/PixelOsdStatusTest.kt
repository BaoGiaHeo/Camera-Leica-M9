package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class PixelOsdStatusTest {
    @Test
    fun evReadoutUsesTheInterpolatedIndexAndStepSize() {
        assertEquals("EV +0.0", formatPixelEvReadout(0f, 1f / 3f))
        assertEquals("EV +1.0", formatPixelEvReadout(3f, 1f / 3f))
        assertEquals("EV -1.0", formatPixelEvReadout(-3f, 1f / 3f))
        assertEquals("EV +0.5", formatPixelEvReadout(1f, 0.5f))
        assertEquals("EV +0.0", formatPixelEvReadout(Float.NaN, 0f))
    }

    @Test
    fun batteryUsesFourDiscreteCells() {
        assertEquals(0, pixelBatteryCellCount(-1))
        assertEquals(0, pixelBatteryCellCount(0))
        assertEquals(1, pixelBatteryCellCount(1))
        assertEquals(1, pixelBatteryCellCount(25))
        assertEquals(2, pixelBatteryCellCount(26))
        assertEquals(2, pixelBatteryCellCount(50))
        assertEquals(3, pixelBatteryCellCount(51))
        assertEquals(3, pixelBatteryCellCount(75))
        assertEquals(4, pixelBatteryCellCount(76))
        assertEquals(4, pixelBatteryCellCount(100))
        assertEquals(0, pixelBatteryCellCount(101))
    }
}

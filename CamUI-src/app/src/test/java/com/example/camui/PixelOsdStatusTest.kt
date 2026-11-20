package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelOsdStatusTest {
    @Test
    fun lowBatteryThresholdIncludesExactlyTwentyPercent() {
        assertEquals(true, isPixelBatteryLow(0))
        assertEquals(true, isPixelBatteryLow(19))
        assertEquals(true, isPixelBatteryLow(20))
        assertEquals(false, isPixelBatteryLow(21))
        assertEquals(false, isPixelBatteryLow(-1))
    }

    @Test
    fun evAccentTransitionsFromNeutralWhiteToAmber() {
        assertEquals(0xFFF2F2F2.toInt(), pixelEvAccentColor(0f, 1f / 3f))
        assertEquals(0xFFFFB300.toInt(), pixelEvAccentColor(3f, 1f / 3f))
        assertEquals(
            0xFF3DA5FF.toInt(),
            pixelEvAccentColor(0f, 1f / 3f, selected = true)
        )
        assertEquals(
            0xFFFFB300.toInt(),
            pixelEvAccentColor(0f, 1f / 3f, manualExposure = true)
        )
        val middle = pixelEvAccentColor(1.5f, 1f / 3f)
        assertTrue(middle != 0xFFF2F2F2.toInt() && middle != 0xFFFFB300.toInt())
    }

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

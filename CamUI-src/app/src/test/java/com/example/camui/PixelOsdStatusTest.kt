package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class PixelOsdStatusTest {
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

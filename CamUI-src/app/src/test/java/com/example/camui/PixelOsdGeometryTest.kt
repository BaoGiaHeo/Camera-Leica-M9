package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelOsdGeometryTest {
    @Test
    fun scaleUsesAbout240LogicalRowsAndMinimumThreeDisplayPixelsPerLogicPixel() {
        val geometry = PixelOsdGeometry.fromDisplayPixels(1920, 1080)
        assertEquals(5, geometry.pixelScale)
        assertEquals(384, geometry.logicWidth)
        assertEquals(216, geometry.logicHeight)

        val lowHeight = PixelOsdGeometry.fromDisplayPixels(1280, 480)
        assertEquals(3, lowHeight.pixelScale)
        assertEquals(160, lowHeight.logicHeight)
    }

    @Test
    fun normalizedCoordinatesAreRoundedToIntegerLogicPixels() {
        val geometry = PixelOsdGeometry.fromDisplayPixels(1920, 1080)
        assertEquals(0, geometry.x(0f))
        assertEquals(192, geometry.x(0.5f))
        assertEquals(383, geometry.x(1f))
        assertEquals(0, geometry.y(0f))
        assertEquals(108, geometry.y(0.5f))
        assertEquals(215, geometry.y(1f))
    }

    @Test
    fun normalizedCoordinatesClampOutsideTheDisplayArea() {
        val geometry = PixelOsdGeometry.fromDisplayPixels(1600, 900)
        assertEquals(0, geometry.x(-0.2f))
        assertEquals(geometry.logicWidth - 1, geometry.x(1.2f))
        assertEquals(0, geometry.y(-0.2f))
        assertEquals(geometry.logicHeight - 1, geometry.y(1.2f))
        assertTrue(geometry.pixelScale >= 3)
    }
}

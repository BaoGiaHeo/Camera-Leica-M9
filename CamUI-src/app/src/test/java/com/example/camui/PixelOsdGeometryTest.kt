package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelOsdGeometryTest {
    @Test
    fun evReadoutShowsOneSignedValueRoundedToOneDecimalPlace() {
        assertEquals("+0.0 EV", formatEvReadout(0f))
        assertEquals("+0.3 EV", formatEvReadout(1f / 3f))
        assertEquals("-1.0 EV", formatEvReadout(-1f))
        assertEquals("+0.0 EV", formatEvReadout(-0.01f))
        assertEquals("+0.0 EV", formatEvReadout(Float.NaN))
    }

    @Test
    fun fineIsTheDefaultAndUsesTwoDisplayPixelsPerLogicPixel() {
        val geometry = PixelOsdGeometry.fromDisplayPixels(1920, 1080)
        assertEquals(2, geometry.pixelScale)
        assertEquals(960, geometry.logicWidth)
        assertEquals(540, geometry.logicHeight)
    }

    @Test
    fun mediumAndChunkyUseTheirExplicitGridScale() {
        val medium = PixelOsdGeometry.fromDisplayPixels(1920, 1080, "medium")
        assertEquals(3, medium.pixelScale)
        assertEquals(640, medium.logicWidth)
        assertEquals(360, medium.logicHeight)

        val chunky = PixelOsdGeometry.fromDisplayPixels(1920, 1080, "chunky")
        assertEquals(4, chunky.pixelScale)
        assertEquals(480, chunky.logicWidth)
        assertEquals(270, chunky.logicHeight)
    }

    @Test
    fun manualTouchOverrideKeepsAfReticleVisibleOverFaceBox() {
        assertTrue(shouldDrawCenterAfReticle(
            centerAfPointAvailable = true,
            faceBoxVisible = true,
            manualOverrideActive = true
        ))
        assertFalse(shouldDrawCenterAfReticle(
            centerAfPointAvailable = true,
            faceBoxVisible = true,
            manualOverrideActive = false
        ))
        assertTrue(shouldDrawCenterAfReticle(
            centerAfPointAvailable = true,
            faceBoxVisible = false,
            manualOverrideActive = false
        ))
        assertFalse(shouldDrawCenterAfReticle(
            centerAfPointAvailable = false,
            faceBoxVisible = false,
            manualOverrideActive = true
        ))
    }

    @Test
    fun normalizedCoordinatesAreRoundedToIntegerLogicPixels() {
        val geometry = PixelOsdGeometry.fromDisplayPixels(1920, 1080, "fine")
        assertEquals(0, geometry.x(0f))
        assertEquals(480, geometry.x(0.5f))
        assertEquals(959, geometry.x(1f))
        assertEquals(0, geometry.y(0f))
        assertEquals(270, geometry.y(0.5f))
        assertEquals(539, geometry.y(1f))
    }

    @Test
    fun normalizedCoordinatesClampOutsideTheDisplayArea() {
        val geometry = PixelOsdGeometry.fromDisplayPixels(1600, 900, "fine")
        assertEquals(0, geometry.x(-0.2f))
        assertEquals(geometry.logicWidth - 1, geometry.x(1.2f))
        assertEquals(0, geometry.y(-0.2f))
        assertEquals(geometry.logicHeight - 1, geometry.y(1.2f))
        assertTrue(geometry.pixelScale == 2)
    }

    @Test
    fun unknownPixelModeSafelyFallsBackToFine() {
        assertEquals(
            2,
            PixelOsdGeometry.fromDisplayPixels(1920, 1080, "unknown").pixelScale
        )
    }
}

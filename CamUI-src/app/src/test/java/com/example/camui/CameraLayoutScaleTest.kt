package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraLayoutScaleTest {
    @Test
    fun cameraScaleUsesReferenceCanvasAndClampsSmallLandscapeScreens() {
        assertEquals(0.85f, FujiCameraDesign.scale(640f, 360f), 0.0001f)
        assertEquals(412f / 427f, FujiCameraDesign.scale(915f, 412f), 0.0001f)
        assertEquals(1.0f, FujiCameraDesign.scale(924f, 427f), 0.0001f)
    }

    @Test
    fun hudClockUsesZeroPaddedHourMinuteSecondFields() {
        assertEquals("16h33m23s", formatCameraClock(16, 33, 23))
        assertEquals("00h00m00s", formatCameraClock(0, 0, 0))
    }

    @Test
    fun hudClockClampsInvalidValuesInsteadOfRenderingMalformedText() {
        assertEquals("23h59m59s", formatCameraClock(99, 99, 99))
        assertEquals("00h00m00s", formatCameraClock(-1, -1, -1))
    }
}

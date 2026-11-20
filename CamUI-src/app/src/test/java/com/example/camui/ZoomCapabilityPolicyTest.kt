package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomCapabilityPolicyTest {
    @Test
    fun cameraWithoutTelephotoIsCappedAtTwoTimesEvenWhenHalReportsMore() {
        val assessment = assessOpticalZoom(listOf(13f, 26f), mainEquivalent35Mm = 26f)
        assertFalse(assessment.hasOpticalTelephoto)
        assertEquals(2f, selectableCameraZoomMax(12f, assessment), 0.0001f)
    }

    @Test
    fun threeTimesTelephotoPassesTheOnePointEightThreshold() {
        val assessment = assessOpticalZoom(listOf(26f, 78f), mainEquivalent35Mm = 26f)
        assertTrue(assessment.hasOpticalTelephoto)
        assertEquals(3f, assessment.opticalRatio!!, 0.0001f)
        assertEquals(12f, selectableCameraZoomMax(12f, assessment), 0.0001f)
    }

    @Test
    fun tenTimesPeriscopeUsesCameraReportedMaximumUpToFiftyTimes() {
        val assessment = assessOpticalZoom(listOf(26f, 260f), mainEquivalent35Mm = 26f)
        assertTrue(assessment.hasOpticalTelephoto)
        assertEquals(10f, assessment.opticalRatio!!, 0.0001f)
        assertEquals(50f, selectableCameraZoomMax(80f, assessment), 0.0001f)
        assertEquals(24f, selectableCameraZoomMax(24f, assessment), 0.0001f)
    }

    @Test
    fun rejectsInvalidLensValuesAndInvalidCameraMaximum() {
        val assessment = assessOpticalZoom(listOf(Float.NaN, -1f, 26f))
        assertFalse(assessment.hasOpticalTelephoto)
        assertNotNull(assessment.mainEquivalent35Mm)
        assertEquals(1f, selectableCameraZoomMax(Float.NaN, assessment), 0.0001f)
    }

    @Test
    fun zoomBarUsesLogarithmicPosition() {
        assertEquals(0f, logarithmicZoomPosition(0.6f, 0.6f, 100f), 0.0001f)
        assertEquals(1f, logarithmicZoomPosition(100f, 0.6f, 100f), 0.0001f)
        val midpoint = logarithmicZoomPosition( sqrtProduct(0.6f, 100f), 0.6f, 100f)
        assertEquals(0.5f, midpoint, 0.0001f)
    }

    private fun sqrtProduct(a: Float, b: Float) = kotlin.math.sqrt(a * b)
    @Test
    fun nonTeleCameraMovesFromOneToTwoTimesInAboutOnePointTwoSeconds() {
        val zoom = advanceLogarithmicZoomRatio(1f, 1, 0.6f, 2f, 0.05f, 1.2f)
        var current = 1f
        repeat(24) {
            current = advanceLogarithmicZoomRatio(current, 1, 0.6f, 2f, 0.05f, 1.2f)
        }
        assertTrue(zoom > 1f)
        assertEquals(2f, current, 0.0001f)
    }

    @Test
    fun zoomHoldIsMonotonicAndStopsAtItsRequestedLimits() {
        var zoomIn = 1f
        repeat(200) {
            val next = advanceLogarithmicZoomRatio(zoomIn, 1, 0.6f, 50f, 0.05f, 6.5f)
            assertTrue(next >= zoomIn)
            zoomIn = next
        }
        assertEquals(50f, zoomIn, 0.0001f)

        var zoomOut = 1f
        repeat(200) {
            val next = advanceLogarithmicZoomRatio(zoomOut, -1, 0.6f, 50f, 0.05f, 6.5f)
            assertTrue(next <= zoomOut)
            zoomOut = next
        }
        assertEquals(0.6f, zoomOut, 0.0001f)
    }

}

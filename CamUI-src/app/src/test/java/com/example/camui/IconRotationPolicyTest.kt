package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconRotationPolicyTest {
    @Test
    fun mapsAllFourDeviceQuadrantsToRelativeDisplayRotation() {
        assertEquals(0, relativeIconRotationDegrees(90, 1))
        assertEquals(180, relativeIconRotationDegrees(270, 1))
        assertEquals(90, relativeIconRotationDegrees(180, 1))
        assertEquals(270, relativeIconRotationDegrees(0, 1))
    }

    @Test
    fun quadrantHysteresisIgnoresSmallJitterNearBoundary() {
        assertEquals(0, snapIconOrientationQuadrant(44, 0))
        assertEquals(0, snapIconOrientationQuadrant(50, 0))
        assertEquals(90, snapIconOrientationQuadrant(56, 0))
    }

    @Test
    fun invalidSensorReadingsPreservePreviousQuadrant() {
        assertNull(snapIconOrientationQuadrant(OrientationUnknown.VALUE, null))
        assertEquals(180, snapIconOrientationQuadrant(OrientationUnknown.VALUE, 180))
    }

    private object OrientationUnknown {
        const val VALUE = -1
    }
}

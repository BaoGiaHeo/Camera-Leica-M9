package com.example.camui

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceRotationPolicyTest {
    @Test
    fun mapsAllFourPhysicalOrientations() {
        assertEquals(Surface.ROTATION_0, DeviceRotationPolicy.rotationCandidate(0f))
        assertEquals(Surface.ROTATION_0, DeviceRotationPolicy.rotationCandidate(20f))
        assertEquals(Surface.ROTATION_270, DeviceRotationPolicy.rotationCandidate(90f))
        assertEquals(Surface.ROTATION_180, DeviceRotationPolicy.rotationCandidate(180f))
        assertEquals(Surface.ROTATION_90, DeviceRotationPolicy.rotationCandidate(270f))
        assertEquals(Surface.ROTATION_0, DeviceRotationPolicy.rotationCandidate(359f))
    }

    @Test
    fun handlesAnglesOutsideOneTurn() {
        assertEquals(Surface.ROTATION_270, DeviceRotationPolicy.rotationCandidate(450f))
        assertEquals(Surface.ROTATION_90, DeviceRotationPolicy.rotationCandidate(-90f))
    }
}

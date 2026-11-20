package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraExposureRequestPolicyTest {
    @Test
    fun bothManualControlsUseTheSelectedIsoAndShutter() {
        val state = resolveCameraExposureRequestState(true, true, 1600, 500_000_000L, 640, 33_333_333L)
        assertFalse(state.automatic)
        assertEquals(1600, state.sensorSensitivityIso)
        assertEquals(500_000_000L, state.sensorExposureTimeNs)
        assertEquals(500_000_000L, state.sensorFrameDurationNs)
    }

    @Test
    fun autoIsoWithManualShutterUsesTheLatestLiveIso() {
        val state = resolveCameraExposureRequestState(false, true, 2000, 250_000_000L, 800, 16_666_667L)
        assertFalse(state.automatic)
        assertEquals(800, state.sensorSensitivityIso)
        assertEquals(250_000_000L, state.sensorExposureTimeNs)
    }

    @Test
    fun autoShutterWithManualIsoUsesTheLatestLiveShutter() {
        val state = resolveCameraExposureRequestState(true, false, 1200, 1_000_000_000L, 400, 40_000_000L)
        assertFalse(state.automatic)
        assertEquals(1200, state.sensorSensitivityIso)
        assertEquals(40_000_000L, state.sensorExposureTimeNs)
        assertEquals(40_000_000L, state.sensorFrameDurationNs)
    }

    @Test
    fun twoAutoControlsReturnToAeWithoutManualSensorValues() {
        val state = resolveCameraExposureRequestState(false, false, 1200, 1_000_000_000L, 400, 40_000_000L)
        assertTrue(state.automatic)
        assertNull(state.sensorSensitivityIso)
        assertNull(state.sensorExposureTimeNs)
        assertNull(state.sensorFrameDurationNs)
    }

    @Test
    fun invalidLiveValuesUseSafeExposureDefaults() {
        val state = resolveCameraExposureRequestState(false, true, 0, 10_000_000L, 0, 0L)
        assertEquals(400, state.sensorSensitivityIso)
        assertEquals(10_000_000L, state.sensorExposureTimeNs)
    }
}

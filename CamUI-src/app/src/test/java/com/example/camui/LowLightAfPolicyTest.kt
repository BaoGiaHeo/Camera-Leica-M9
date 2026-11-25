package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LowLightAfPolicyTest {
    @Test
    fun lowLightRequiresStableDarkFramesBeforeEntering() {
        val first = updateLowLightAfTransition(
            normalizedLuma = 0.15f,
            active = false,
            candidateStartedAtMs = 0L,
            nowMs = 1_000L
        )
        assertFalse(first.active)
        assertEquals(1_000L, first.candidateStartedAtMs)

        val beforeHold = updateLowLightAfTransition(
            normalizedLuma = 0.14f,
            active = first.active,
            candidateStartedAtMs = first.candidateStartedAtMs,
            nowMs = 2_150L
        )
        assertFalse(beforeHold.active)

        val afterHold = updateLowLightAfTransition(
            normalizedLuma = 0.14f,
            active = beforeHold.active,
            candidateStartedAtMs = beforeHold.candidateStartedAtMs,
            nowMs = 2_250L
        )
        assertTrue(afterHold.active)
        assertTrue(afterHold.changed)
        assertEquals(0L, afterHold.candidateStartedAtMs)
    }

    @Test
    fun exitUsesHigherThresholdAndSameStabilityWindow() {
        val candidate = updateLowLightAfTransition(
            normalizedLuma = 0.25f,
            active = true,
            candidateStartedAtMs = 0L,
            nowMs = 5_000L
        )
        assertTrue(candidate.active)
        val stable = updateLowLightAfTransition(
            normalizedLuma = 0.26f,
            active = true,
            candidateStartedAtMs = candidate.candidateStartedAtMs,
            nowMs = 6_300L
        )
        assertFalse(stable.active)
        assertTrue(stable.changed)
    }

    @Test
    fun deadBandResetsAnUnconfirmedTransition() {
        val candidate = updateLowLightAfTransition(
            normalizedLuma = 0.16f,
            active = false,
            candidateStartedAtMs = 0L,
            nowMs = 1_000L
        )
        val stable = updateLowLightAfTransition(
            normalizedLuma = 0.22f,
            active = false,
            candidateStartedAtMs = candidate.candidateStartedAtMs,
            nowMs = 1_800L
        )
        assertFalse(stable.active)
        assertEquals(0L, stable.candidateStartedAtMs)
    }

    @Test
    fun lowLightAfWaitsForSceneChangeAndUsesLongerRetryInterval() {
        assertEquals(200L, lowLightAfRetryIntervalMs(false))
        assertEquals(Tuning.AF_RETRY_MIN_INTERVAL_MS, lowLightAfRetryIntervalMs(true))
        assertEquals(0.025f, lowLightAfMovementThreshold(false))
        assertEquals(Tuning.AF_TARGET_MOVE_THRESHOLD, lowLightAfMovementThreshold(true))
        assertTrue(lowLightLuminanceSceneChanged(Float.NaN, 0.18f, true))
        assertTrue(lowLightLuminanceSceneChanged(0.10f, 0.24f, true))
        assertFalse(lowLightLuminanceSceneChanged(0.10f, 0.12f, true))
        assertFalse(lowLightLuminanceSceneChanged(Float.NaN, 0.18f, false))
    }

    @Test
    fun gyroMotionUsesAngularVelocityAndRejectsInvalidSamples() {
        assertFalse(lowLightAfGyroMotionDetected(0.02f, 0.01f, 0.02f))
        assertTrue(lowLightAfGyroMotionDetected(0.13f, 0f, 0f))
        assertTrue(lowLightAfGyroMotionDetected(0f, 0.10f, 0.08f))
        assertFalse(lowLightAfGyroMotionDetected(Float.NaN, 0f, 0f))
        assertFalse(lowLightAfGyroMotionDetected(0.4f, 0f, 0f, thresholdRadiansPerSecond = Float.NaN))
    }
}

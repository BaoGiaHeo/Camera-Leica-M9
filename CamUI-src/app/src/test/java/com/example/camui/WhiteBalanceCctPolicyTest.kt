package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhiteBalanceCctPolicyTest {
    private val d65SrgbToXyz = floatArrayOf(
        0.4124564f, 0.3575761f, 0.1804375f,
        0.2126729f, 0.7151522f, 0.0721750f,
        0.0193339f, 0.1191920f, 0.9503041f
    )

    @Test
    fun neutralGainsWithD65CalibrationEstimateDaylightKelvin() {
        val result = WhiteBalanceCctPolicy.estimateKelvin(
            floatArrayOf(1f, 1f, 1f, 1f),
            d65SrgbToXyz
        )
        assertTrue(result != null)
        assertTrue(result!! in 6200..6800)
        assertEquals(0, result % 50)
    }

    @Test
    fun rejectsMissingInvalidOrNonFiniteCameraMetadata() {
        assertNull(WhiteBalanceCctPolicy.estimateKelvin(floatArrayOf(1f, 1f), d65SrgbToXyz))
        assertNull(WhiteBalanceCctPolicy.estimateKelvin(floatArrayOf(1f, 0f, 1f, 1f), d65SrgbToXyz))
        assertNull(
            WhiteBalanceCctPolicy.estimateKelvin(
                floatArrayOf(1f, 1f, 1f, 1f),
                FloatArray(9) { Float.NaN }
            )
        )
    }

    @Test
    fun roundsKelvinToFiftyKelvinSteps() {
        assertEquals(6500, WhiteBalanceCctPolicy.roundTo50Kelvin(6524))
        assertEquals(6550, WhiteBalanceCctPolicy.roundTo50Kelvin(6525))
    }

    @Test
    fun smootherLimitsUpdatesToTwoHertzAndUsesEma() {
        val smoother = WhiteBalanceKelvinSmoother(updateIntervalMs = 500L, newSampleWeight = 0.5f)
        assertEquals(6500, smoother.update(6500, 1_000L))
        assertEquals(6500, smoother.update(9000, 1_250L))
        val next = smoother.update(9000, 1_500L)
        assertTrue(next != null)
        assertTrue(next!! in 7700..7800)
    }

    @Test
    fun smootherReturnsAutoWhenAnEligibleSampleCannotBeEstimated() {
        val smoother = WhiteBalanceKelvinSmoother()
        assertTrue(smoother.update(6500, 1_000L) != null)
        assertNull(smoother.update(null, 1_500L))
        assertFalse(smoother.update(null, 1_600L) != null)
    }
}

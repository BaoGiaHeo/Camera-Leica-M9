package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviewFpsEstimatorTest {
    @Test
    fun waitsForThirtySensorTimestampsThenMeasuresActualCadence() {
        val estimator = PreviewFpsEstimator()
        var result: Float? = null
        repeat(29) { index ->
            result = estimator.add(index * 16_683_333L + 1L, index * 17L)
        }
        assertNull(result)

        result = estimator.add(29 * 16_683_333L + 1L, 500L)
        assertEquals(59.94f, result!!, 0.03f)
    }

    @Test
    fun publishesNoMoreOftenThanTwicePerSecond() {
        val estimator = PreviewFpsEstimator()
        var first: Float? = null
        repeat(30) { index ->
            first = estimator.add(index * 20_000_000L + 1L, index * 20L)
        }
        assertEquals(50f, first!!, 0.01f)

        assertNull(estimator.add(30 * 20_000_000L + 1L, 600L))
        assertEquals(50f, estimator.add(31 * 20_000_000L + 1L, 1_080L)!!, 0.01f)
    }

    @Test
    fun resetsOnNonMonotonicSensorTimestampAndRejectsInvalidValues() {
        val estimator = PreviewFpsEstimator()
        assertNull(estimator.add(0L, 0L))
        repeat(10) { index -> estimator.add(1_000_000L + index * 10_000_000L, index * 10L) }
        assertNull(estimator.add(5_000_000L, 200L))
        repeat(28) { index ->
            assertNull(estimator.add(15_000_000L + index * 10_000_000L, 210L + index * 10L))
        }
        assertEquals(100f, estimator.add(295_000_000L, 500L)!!, 0.1f)
    }
}

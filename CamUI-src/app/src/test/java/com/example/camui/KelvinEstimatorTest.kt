package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KelvinEstimatorTest {
    @Test
    fun mcCamyEstimateRecognizesDaylightWhitePoint() {
        val kelvin = KelvinEstimator.cctFromXyz(floatArrayOf(0.95047f, 1f, 1.08883f))
        assertEquals(6_500f, kelvin!!, 100f)
    }

    @Test
    fun mcCamyEstimateRecognizesTungstenAWhitePoint() {
        val kelvin = KelvinEstimator.cctFromXyz(floatArrayOf(1.0985f, 1f, 0.35585f))
        assertEquals(2_856f, kelvin!!, 100f)
    }

    @Test
    fun invalidXyzDoesNotProduceAFalseKelvinValue() {
        assertNull(KelvinEstimator.cctFromXyz(floatArrayOf(0f, 0f, 0f)))
        assertNull(KelvinEstimator.cctFromXyz(floatArrayOf(Float.NaN, 1f, 1f)))
    }

    @Test
    fun asShotNeutralUsesInverseRggbGainsAndAverageGreen() {
        val neutral = KelvinEstimator.asShotNeutral(
            android.hardware.camera2.params.RggbChannelVector(2f, 1f, 1.5f, 4f)
        )!!
        assertEquals(0.5f, neutral[0], 0.0001f)
        assertEquals(0.8f, neutral[1], 0.0001f)
        assertEquals(0.25f, neutral[2], 0.0001f)
    }

    @Test
    fun zeroOrNonFiniteGainsAreRejected() {
        assertNull(
            KelvinEstimator.asShotNeutral(
                android.hardware.camera2.params.RggbChannelVector(0f, 1f, 1f, 2f)
            )
        )
    }
}

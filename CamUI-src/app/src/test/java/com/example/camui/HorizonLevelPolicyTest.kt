package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

class HorizonLevelPolicyTest {
    private fun vectorAt(degrees: Float): Pair<Float, Float> {
        val radians = degrees * PI.toFloat() / 180f
        return sin(radians) to cos(radians)
    }

    @Test
    fun zeroErrorIsHorizontal() {
        val result = HorizonLevelPolicy.nearestAxisErrorDegrees(0f, 1f)
        assertEquals(0f, result!!, 0.0001f)
    }

    @Test
    fun ordinaryAndFormerEightyFiveDegreeErrorAreWrappedToNearestAxis() {
        val small = vectorAt(0.4f)
        val formerVerticalError = vectorAt(85.3f)
        assertEquals(0.4f, HorizonLevelPolicy.nearestAxisErrorDegrees(small.first, small.second)!!, 0.001f)
        // 85.3° is only 4.7° away from the next 90° axis.
        assertEquals(-4.7f, HorizonLevelPolicy.nearestAxisErrorDegrees(
            formerVerticalError.first, formerVerticalError.second
        )!!, 0.001f)
    }

    @Test
    fun portraitAndLandscapeAxesShareTheSameErrorDefinition() {
        val portraitAligned = HorizonLevelPolicy.nearestAxisErrorDegrees(1f, 0f)
        val landscapeAligned = HorizonLevelPolicy.nearestAxisErrorDegrees(0f, 1f)
        assertEquals(0f, portraitAligned!!, 0.001f)
        assertEquals(0f, landscapeAligned!!, 0.001f)
    }

    @Test
    fun nearlyFlatDeviceHidesTheLevelIndicator() {
        assertTrue(HorizonLevelPolicy.isNearFlat(9.6f, 9.81f))
        assertFalse(HorizonLevelPolicy.isNearFlat(2.0f, 9.81f))
        assertNull(HorizonLevelPolicy.nearestAxisErrorDegrees(0.01f, 0.01f))
    }

    @Test
    fun oneEuroFilterSettlesAndTracksNewAngle() {
        val filter = OneEuroFilter()
        val start = 1_000_000_000L
        val first = filter.filter(0f, start)
        val noisy = filter.filter(8f, start + 16_000_000L)
        val settled = filter.filter(8f, start + 16_000_000L * 40)
        assertEquals(0f, first, 0.0001f)
        assertTrue(noisy > 0f && noisy < 8f)
        assertTrue(settled > noisy)
        assertTrue(settled <= 8f)
    }
}

package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class HorizonLevelPolicyTest {
    private fun gravityAt(thetaDegrees: Float): FloatArray {
        val theta = Math.toRadians(thetaDegrees.toDouble())
        return floatArrayOf(
            (sin(theta) * 9.80665).toFloat(),
            (cos(theta) * 9.80665).toFloat(),
            0f
        )
    }

    private fun errorAt(thetaDegrees: Float, rotation: Int = 0): Float {
        val gravity = gravityAt(thetaDegrees)
        return HorizonLevelPolicy.errorDegrees(
            gravity[0], gravity[1], gravity[2], rotation
        ) ?: error("Expected a visible level for ${thetaDegrees} degrees")
    }

    @Test
    fun reportsErrorFromNearestNinetyDegreeAxis() {
        assertEquals(0f, HorizonLevelPolicy.errorDegrees(0f, 9.80665f, 0f, 0)!!, 0.001f)
        assertEquals(0.4f, errorAt(0.4f), 0.01f)
        assertEquals(4.7f, errorAt(4.7f), 0.01f)
        assertEquals(20f, errorAt(20f), 0.01f)
        assertEquals(44.9f, errorAt(44.9f), 0.02f)
        assertEquals(-44f, errorAt(46f), 0.02f)
    }

    @Test
    fun remapsAllDisplayOrientationsWithoutChangingErrorMagnitude() {
        val original = errorAt(12f)
        val rotated90 = errorAt(12f, 1)
        val rotated180 = errorAt(12f, 2)
        val rotated270 = errorAt(12f, 3)
        assertEquals(original, rotated90, 0.02f)
        assertEquals(original, rotated180, 0.02f)
        assertEquals(original, rotated270, 0.02f)
    }

    @Test
    fun hidesTheLevelWhenTheDeviceIsNearlyFlat() {
        assertNull(HorizonLevelPolicy.errorDegrees(0.1f, 0.1f, 9.8f, 0))
    }

    @Test
    fun balancedThresholdHasHysteresis() {
        assertTrue(HorizonLevelPolicy.balanced(0.4f, wasBalanced = false))
        assertFalse(HorizonLevelPolicy.balanced(0.7f, wasBalanced = false))
        assertTrue(HorizonLevelPolicy.balanced(0.8f, wasBalanced = true))
        assertFalse(HorizonLevelPolicy.balanced(1.0f, wasBalanced = true))
    }

    @Test
    fun oneEuroFilterSmoothsSmallJitterAndTracksLargerMovement() {
        val filter = OneEuroLevelFilter()
        val first = filter.filter(4f, 1_000_000_000L)
        val second = filter.filter(4.4f, 1_016_666_667L)
        val third = filter.filter(17f, 1_033_333_334L)
        assertEquals(4f, first, 0.001f)
        assertTrue(second in 4f..4.4f)
        assertTrue(third > second)
        filter.reset()
        assertEquals(3f, filter.filter(3f, 2_000_000_000L), 0.001f)
    }
}

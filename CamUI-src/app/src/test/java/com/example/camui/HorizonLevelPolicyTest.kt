package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class HorizonLevelPolicyTest {
    @Test
    fun portraitAndLandscapeGravityBothResolveToTheNearestAxis() {
        val portrait = HorizonLevelEstimator().update(
            gravityDeviceX = 0f,
            gravityDeviceY = 9.81f,
            gravityDeviceZ = 0f,
            displayRotation = 0,
            timestampNanos = 1_000_000_000L
        )
        val landscape = HorizonLevelEstimator().update(
            gravityDeviceX = 9.81f,
            gravityDeviceY = 0f,
            gravityDeviceZ = 0f,
            displayRotation = 1,
            timestampNanos = 1_000_000_000L
        )

        assertTrue(portrait.visible)
        assertTrue(landscape.visible)
        assertEquals(0f, portrait.errorDegrees, 0.01f)
        assertEquals(0f, landscape.errorDegrees, 0.01f)
        assertTrue(portrait.aligned)
        assertTrue(landscape.aligned)
    }

    @Test
    fun mapsTheReportedMinus853DegreeCaseToA47DegreeDeviation() {
        val theta = -85.3f
        assertEquals(4.7f, nearestAxisErrorDegrees(theta), 0.001f)
    }

    @Test
    fun mapsPositiveAndNegativeTiltToTheOppositeIndicatorAngle() {
        val estimator = HorizonLevelEstimator()
        val angle = Math.toRadians(4.7)
        val result = estimator.update(
            gravityDeviceX = cos(angle).toFloat(),
            gravityDeviceY = sin(angle).toFloat(),
            gravityDeviceZ = 0f,
            displayRotation = 1,
            timestampNanos = 1_000_000_000L
        )

        assertEquals(4.7f, result.errorDegrees, 0.02f)
        assertEquals(-4.7f, result.indicatorAngleDegrees, 0.02f)
        assertFalse(result.aligned)
    }

    @Test
    fun twentyDegreesAndTheFortyFiveDegreeBoundaryRemainBounded() {
        assertEquals(20f, nearestAxisErrorDegrees(20f), 0.001f)
        assertEquals(44.9f, nearestAxisErrorDegrees(44.9f), 0.001f)
        assertEquals(-44.9f, nearestAxisErrorDegrees(-44.9f), 0.001f)
        assertEquals(-45f, nearestAxisErrorDegrees(45f), 0.001f)
        assertEquals(-45f, nearestAxisErrorDegrees(-45f), 0.001f)
    }

    @Test
    fun flatPhoneHidesTheLevelIndicatorWhenGravityIsMostlyNormalToTheScreen() {
        val result = HorizonLevelEstimator().update(
            gravityDeviceX = 0f,
            gravityDeviceY = 0f,
            gravityDeviceZ = 9.81f,
            displayRotation = 1,
            timestampNanos = 1_000_000_000L
        )
        assertFalse(result.visible)
        assertTrue(result.gravityZRatio > 0.9f)
    }

    @Test
    fun alignedColorUsesHalfDegreeEntryAndPointNineDegreeExitHysteresis() {
        var aligned = updateHorizonAligned(false, 0.4f)
        assertTrue(aligned)
        aligned = updateHorizonAligned(aligned, 0.7f)
        assertTrue(aligned)
        aligned = updateHorizonAligned(aligned, 1.0f)
        assertFalse(aligned)
        aligned = updateHorizonAligned(aligned, 0.6f)
        assertFalse(aligned)
        aligned = updateHorizonAligned(aligned, 0.49f)
        assertTrue(aligned)
    }
}

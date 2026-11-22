package com.example.camui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraAfTouchPolicyTest {
    @Test
    fun ninePointGridNormalizesTouchCoordinatesAcrossLandscapePreviewSizes() {
        val sizes = listOf(2400 to 1080, 2640 to 1200, 1920 to 1080)
        val normalized = listOf(
            0.1f to 0.1f, 0.5f to 0.1f, 0.9f to 0.1f,
            0.1f to 0.5f, 0.5f to 0.5f, 0.9f to 0.5f,
            0.1f to 0.9f, 0.5f to 0.9f, 0.9f to 0.9f
        )
        sizes.forEach { (width, height) ->
            normalized.forEach { (x, y) ->
                val point = normalizedAfTapPoint(
                    Offset(width * x, height * y),
                    width,
                    height
                )
                assertEquals(x, point.x, 0.0001f)
                assertEquals(y, point.y, 0.0001f)
            }
        }
    }

    @Test
    fun afReticleHitTargetUsesPhysicalPixelsAndTwelvePercentSide() {
        val center = Offset(0.5f, 0.5f)
        assertTrue(isTapInsideAfReticle(Offset(0.52f, 0.53f), center, 2400, 1080))
        assertFalse(isTapInsideAfReticle(Offset(0.57f, 0.5f), center, 2400, 1080))
        assertFalse(isTapInsideAfReticle(Offset(0.5f, 0.57f), center, 2400, 1080))
        assertTrue(isTapInsideAfReticle(Offset(0.53f, 0.53f), center, 1080, 1920))
    }

    @Test
    fun invalidPreviewDimensionsNeverHitReticle() {
        assertFalse(isTapInsideAfReticle(Offset(0.5f, 0.5f), Offset(0.5f, 0.5f), 0, 1080))
    }

    @Test
    fun cameraRegionCapabilitiesSelectCamera2FallbackWhenAfRegionsAreUnavailable() {
        assertEquals(
            CameraAfRegionStrategy.AF_AND_AE_REGIONS,
            CameraAfRegionCapabilities(1, 1).strategy()
        )
        assertEquals(
            CameraAfRegionStrategy.AF_REGION_ONLY,
            CameraAfRegionCapabilities(3, 0).strategy()
        )
        assertEquals(
            CameraAfRegionStrategy.AE_REGION_ONLY_WITH_SENSOR_AF_TRIGGER,
            CameraAfRegionCapabilities(0, 1).strategy()
        )
        assertEquals(
            CameraAfRegionStrategy.SENSOR_AF_TRIGGER_ONLY,
            CameraAfRegionCapabilities(0, 0).strategy()
        )
    }

    @Test
    fun manualTouchSuppressesFaceFollowForThreeSecondsOnly() {
        val until = afManualFaceOverrideUntil(10_000L)
        assertEquals(13_000L, until)
        assertTrue(isAfManualFaceOverrideActive(until, 10_001L))
        assertTrue(isAfManualFaceOverrideActive(until, 12_999L))
        assertFalse(isAfManualFaceOverrideActive(until, 13_000L))
    }

    @Test
    fun longPressLockPolicyUsesFiveHundredMilliseconds() {
        assertEquals(500L, AF_LONG_PRESS_LOCK_DURATION_MS)
    }

}

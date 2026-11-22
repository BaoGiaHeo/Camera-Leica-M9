package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LunaDreamModelPolicyTest {
    @Test
    fun exposesFourOrderedTiersAndNominalFourByThreeDimensions() {
        val tiers = listOf(
            LunaDreamTier.LUNA_1_5,
            LunaDreamTier.LUNA_2_0,
            LunaDreamTier.LUNA_2_5,
            LunaDreamTier.LUNA_3_0
        )
        val pixels = tiers.map { requireNotNull(LunaDreamModelPolicy.nominal4x3PixelCount(it)) }
        assertTrue(pixels.zipWithNext().all { (a, b) -> b > a })
        tiers.forEach { assertTrue("$it must be within 1% of its nominal target", LunaDreamModelPolicy.isNominal4x3WithinOnePercent(it)) }
        assertEquals(49_939_200L, pixels[0])
        assertEquals(108_000_000L, pixels[1])
        assertEquals(150_039_552L, pixels[2])
        assertEquals(199_756_800L, pixels[3])
    }

    @Test
    fun frameBudgetsScaleWithIsoAndStayWithinEachTierRange() {
        val oneFive = requireNotNull(LunaDreamModelPolicy.spec(LunaDreamTier.LUNA_1_5))
        val two = requireNotNull(LunaDreamModelPolicy.spec(LunaDreamTier.LUNA_2_0))
        val twoFive = requireNotNull(LunaDreamModelPolicy.spec(LunaDreamTier.LUNA_2_5))
        val three = requireNotNull(LunaDreamModelPolicy.spec(LunaDreamTier.LUNA_3_0))
        assertEquals(1, oneFive.frameTarget(100))
        assertEquals(2, oneFive.frameTarget(1600))
        assertEquals(4, two.frameTarget(100))
        assertEquals(5, two.frameTarget(400))
        assertEquals(6, twoFive.frameTarget(50))
        assertEquals(8, twoFive.frameTarget(100))
        assertEquals(10, twoFive.frameTarget(400))
        assertEquals(12, three.frameTarget(50))
        assertEquals(14, three.frameTarget(100))
        assertEquals(16, three.frameTarget(400))
    }

    @Test
    fun sensorStrategyUsesMaximumModeOnlyWhenAdvertisedAndAtLeast190Mp() {
        val strategyA = LunaDreamSensorCapability(
            normalPixelArrayWidth = 8_160,
            normalPixelArrayHeight = 6_120,
            maximumResolutionWidth = 16_320,
            maximumResolutionHeight = 12_240,
            maximumResolutionStreamAvailable = true,
            ultraHighResolutionCapability = true
        )
        assertEquals(LunaDreamSourceStrategy.A_NATIVE_MAX_RESOLUTION_DETAIL, strategyA.strategy())

        val strategyB = strategyA.copy(maximumResolutionStreamAvailable = false)
        assertEquals(LunaDreamSourceStrategy.B_COMPUTATIONAL_MULTI_FRAME_SUPER_RESOLUTION, strategyB.strategy())

        val strategyC = LunaDreamSensorCapability(
            normalPixelArrayWidth = 6_400,
            normalPixelArrayHeight = 4_800,
            maximumResolutionWidth = 6_400,
            maximumResolutionHeight = 4_800,
            maximumResolutionStreamAvailable = false,
            ultraHighResolutionCapability = false
        )
        assertEquals(LunaDreamSourceStrategy.C_LIMITED_SENSOR_UPSCALE, strategyC.strategy())
    }

    @Test
    fun offAndUnknownPreferencesNeverSelectAProcessingTier() {
        assertEquals(LunaDreamTier.OFF, LunaDreamTier.fromPreference("off"))
        assertEquals(LunaDreamTier.OFF, LunaDreamTier.fromPreference("not-a-mode"))
        assertEquals(LunaDreamTier.LUNA_2_5, LunaDreamTier.fromPreference("luna25"))
        assertNotNull(LunaDreamModelPolicy.spec(LunaDreamTier.LUNA_3_0))
        assertFalse(LunaDreamModelPolicy.isNominal4x3WithinOnePercent(LunaDreamTier.OFF))
    }
}

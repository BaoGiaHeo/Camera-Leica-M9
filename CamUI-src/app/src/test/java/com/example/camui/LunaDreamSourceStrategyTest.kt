package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class LunaDreamSourceStrategyTest {
    @Test
    fun strategyARequiresAdvertisedUltraHighResolutionAndReal190MpStream() {
        val strategy = LunaDreamSensorCapability(
            normalPixelArrayWidth = 8160,
            normalPixelArrayHeight = 6120,
            maximumResolutionWidth = 16_320,
            maximumResolutionHeight = 12_240,
            maximumResolutionStreamAvailable = true,
            ultraHighResolutionCapability = true
        )
        assertEquals(LunaDreamSourceStrategy.A_NATIVE_HIGH_RESOLUTION, strategy.strategy())
    }

    @Test
    fun strategyBUsesComputationalSuperResolutionForLargeBinnedSensors() {
        val strategy = LunaDreamSensorCapability(
            normalPixelArrayWidth = 8160,
            normalPixelArrayHeight = 6120,
            maximumResolutionWidth = 8160,
            maximumResolutionHeight = 6120,
            maximumResolutionStreamAvailable = false,
            ultraHighResolutionCapability = false
        )
        assertEquals(LunaDreamSourceStrategy.B_COMPUTATIONAL_MULTI_FRAME, strategy.strategy())
    }

    @Test
    fun strategyCDoesNotMislabelSmallSensorsAs200Mp() {
        val strategy = LunaDreamSensorCapability(
            normalPixelArrayWidth = 4000,
            normalPixelArrayHeight = 3000,
            maximumResolutionWidth = 4000,
            maximumResolutionHeight = 3000,
            maximumResolutionStreamAvailable = false,
            ultraHighResolutionCapability = false
        )
        assertEquals(LunaDreamSourceStrategy.C_LIMITED_UPSCALE, strategy.strategy())
    }

    @Test
    fun a200MpPixelArrayWithoutAccessibleUltraHighResolutionStreamIsNotStrategyA() {
        val strategy = LunaDreamSensorCapability(
            normalPixelArrayWidth = 8160,
            normalPixelArrayHeight = 6120,
            maximumResolutionWidth = 16_320,
            maximumResolutionHeight = 12_240,
            maximumResolutionStreamAvailable = false,
            ultraHighResolutionCapability = true
        )
        assertEquals(LunaDreamSourceStrategy.B_COMPUTATIONAL_MULTI_FRAME, strategy.strategy())
    }
}

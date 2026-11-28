package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class LunaDreamOutputPolicyTest {
    @Test
    fun allLunaDreamModelsShareOneResolutionAndMegapixelSpecification() {
        assertEquals(LunaDreamOutputTarget("FHD+", 50_000_000L), lunaDreamOutputTarget("luna15"))
        assertEquals(LunaDreamOutputTarget("UHD", 108_000_000L), lunaDreamOutputTarget("luna20"))
        assertEquals(LunaDreamOutputTarget("UHD+", 150_000_000L), lunaDreamOutputTarget("luna25"))
        assertEquals(LunaDreamOutputTarget("EUHD", 200_000_000L), lunaDreamOutputTarget("luna30"))
        assertNull(lunaDreamOutputTarget("off"))
    }

    @Test
    fun lunaDream30Requests200MegapixelsWith180MegapixelSafetyFloor() {
        assertEquals(200_000_000L, Tuning.LUNA_DREAM_30_MAX_OUTPUT_PIXELS)
        assertEquals(180_000_000L, Tuning.LUNA_DREAM_30_MIN_OUTPUT_PIXELS)
        assertTrue(Tuning.LUNA_DREAM_30_MIN_OUTPUT_PIXELS < Tuning.LUNA_DREAM_30_MAX_OUTPUT_PIXELS)
    }
}

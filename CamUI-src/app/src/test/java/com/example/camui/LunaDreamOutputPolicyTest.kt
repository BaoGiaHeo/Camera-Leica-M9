package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LunaDreamOutputPolicyTest {
    @Test
    fun lunaDream30Requests200MegapixelsWith180MegapixelSafetyFloor() {
        assertEquals(200_000_000L, Tuning.LUNA_DREAM_30_MAX_OUTPUT_PIXELS)
        assertEquals(180_000_000L, Tuning.LUNA_DREAM_30_MIN_OUTPUT_PIXELS)
        assertTrue(Tuning.LUNA_DREAM_30_MIN_OUTPUT_PIXELS < Tuning.LUNA_DREAM_30_MAX_OUTPUT_PIXELS)
    }
}

package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class LunaDreamOutputPolicyTest {
    @Test
    fun allLunaDreamModelsShareOneResolutionAndMegapixelSpecification() {
        assertEquals(LunaDreamOutputTarget(50_000_000L), lunaDreamOutputTarget("luna15"))
        assertEquals(LunaDreamOutputTarget(108_000_000L), lunaDreamOutputTarget("luna20"))
        assertEquals(LunaDreamOutputTarget(180_000_000L), lunaDreamOutputTarget("luna25"))
        assertEquals(LunaDreamOutputTarget(200_000_000L), lunaDreamOutputTarget("luna30"))
        assertNull(lunaDreamOutputTarget("off"))
    }

    @Test
    fun outputDimensionsMatchTheSelectedFrameRatioForEveryModel() {
        val modes = listOf(
            "luna15" to 50_000_000L,
            "luna20" to 108_000_000L,
            "luna25" to 180_000_000L,
            "luna30" to 200_000_000L
        )
        modes.forEach { (mode, targetPixels) ->
            val alignment = if (targetPixels >= 180_000_000L) 16 else 1
            val portrait = lunaDreamOutputDimensions(mode, 16f / 9f)!!
            val landscape = lunaDreamOutputDimensions(mode, 9f / 16f)!!
            val expectedPortrait = PhotoOutputGeometry.calculate(
                65_000, 65_000, targetPixels, 16f / 9f, alignment
            )
            val expectedLandscape = PhotoOutputGeometry.calculate(
                65_000, 65_000, targetPixels, 9f / 16f, alignment
            )
            assertEquals(expectedPortrait.outputWidth to expectedPortrait.outputHeight, portrait)
            assertEquals(expectedLandscape.outputWidth to expectedLandscape.outputHeight, landscape)
            assertTrue(portrait.second > portrait.first)
            assertTrue(landscape.first > landscape.second)
            assertTrue(portrait.first.toLong() * portrait.second <= targetPixels)
            assertTrue(landscape.first.toLong() * landscape.second <= targetPixels)
        }
        assertNull(lunaDreamOutputDimensions("off", 16f / 9f))
    }

    @Test
    fun lunaDream30Requests200MegapixelsWith180MegapixelSafetyFloor() {
        assertEquals(200_000_000L, Tuning.LUNA_DREAM_30_MAX_OUTPUT_PIXELS)
        assertEquals(180_000_000L, Tuning.LUNA_DREAM_30_MIN_OUTPUT_PIXELS)
        assertTrue(Tuning.LUNA_DREAM_30_MIN_OUTPUT_PIXELS < Tuning.LUNA_DREAM_30_MAX_OUTPUT_PIXELS)
    }
}

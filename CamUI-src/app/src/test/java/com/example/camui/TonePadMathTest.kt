package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TonePadMathTest {
    @Test
    fun zeroIntensityPreservesSourceRgb() {
        val result = TonePadMath.apply(0.27f, 0.54f, 0.82f, 1f, -1f, 0f)
        assertEquals(0.27f, result.red, 0.0001f)
        assertEquals(0.54f, result.green, 0.0001f)
        assertEquals(0.82f, result.blue, 0.0001f)
    }

    @Test
    fun zeroPositionPreservesSourceRgb() {
        val result = TonePadMath.apply(0.27f, 0.54f, 0.82f, 0f, 0f, 1f)
        assertEquals(0.27f, result.red, 0.0001f)
        assertEquals(0.54f, result.green, 0.0001f)
        assertEquals(0.82f, result.blue, 0.0001f)
    }

    @Test
    fun brightDirectionIncreasesOklabLightness() {
        val source = TonePadMath.toOklab(0.32f, 0.42f, 0.58f)
        val resultRgb = TonePadMath.apply(0.32f, 0.42f, 0.58f, 0f, 1f, 1f)
        val result = TonePadMath.toOklab(resultRgb.red, resultRgb.green, resultRgb.blue)
        assertTrue(result.lightness > source.lightness)
    }

    @Test
    fun richDirectionIncreasesOklabChroma() {
        val source = TonePadMath.toOklab(0.60f, 0.32f, 0.24f)
        val resultRgb = TonePadMath.apply(0.60f, 0.32f, 0.24f, 1f, 0f, 1f)
        val result = TonePadMath.toOklab(resultRgb.red, resultRgb.green, resultRgb.blue)
        val sourceChroma = kotlin.math.sqrt(source.a * source.a + source.b * source.b)
        val resultChroma = kotlin.math.sqrt(result.a * result.a + result.b * result.b)
        assertTrue(resultChroma > sourceChroma)
    }

    @Test
    fun identityConversionHasNearZeroDeltaE() {
        val source = TonePadMath.toOklab(0.13f, 0.62f, 0.85f)
        val converted = TonePadMath.fromOklab(source)
        val roundTrip = TonePadMath.toOklab(converted.red, converted.green, converted.blue)
        assertTrue(TonePadMath.deltaE76(source, roundTrip) < 0.0001f)
    }

    @Test
    fun tonePadSettingsAreNamespacedByFilter() {
        assertEquals("tone_pad.provia", TonePadMath.preferencePrefix("Provia"))
        assertEquals("tone_pad.classic_chrome", TonePadMath.preferencePrefix("Classic Chrome"))
    }
}

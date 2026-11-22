package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class FujiScaleTest {
    @Test
    fun dragPositionClampsToTheRulerEndpoints() {
        assertEquals(0f, advanceFujiScalePosition(0.02f, -0.1f), 0.0001f)
        assertEquals(1f, advanceFujiScalePosition(0.98f, 0.1f), 0.0001f)
    }

    @Test
    fun dragPositionUsesTheCurrentFiniteValue() {
        assertEquals(0.65f, advanceFujiScalePosition(0.5f, 0.15f), 0.0001f)
        assertEquals(0.35f, advanceFujiScalePosition(0.5f, -0.15f), 0.0001f)
    }

    @Test
    fun invalidPositionAndDeltaAreHandledSafely() {
        assertEquals(0.5f, advanceFujiScalePosition(Float.NaN, Float.NaN), 0.0001f)
        assertEquals(0.9f, advanceFujiScalePosition(0.9f, Float.POSITIVE_INFINITY), 0.0001f)
    }
}

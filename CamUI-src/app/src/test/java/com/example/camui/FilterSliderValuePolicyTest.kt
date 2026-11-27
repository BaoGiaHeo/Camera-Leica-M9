package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class FilterSliderValuePolicyTest {
    @Test
    fun mapsTapAndDragAcrossSignedRange() {
        val range = -1f..1f
        assertEquals(-1f, filterSliderValueFromPosition(0f, 100f, range), 0.0001f)
        assertEquals(0f, filterSliderValueFromPosition(50f, 100f, range), 0.0001f)
        assertEquals(1f, filterSliderValueFromPosition(100f, 100f, range), 0.0001f)
    }

    @Test
    fun mapsIntensityRangeAndClampsBeyondTrack() {
        val range = 0f..1f
        assertEquals(0f, filterSliderValueFromPosition(-20f, 100f, range), 0.0001f)
        assertEquals(0.42f, filterSliderValueFromPosition(42f, 100f, range), 0.0001f)
        assertEquals(1f, filterSliderValueFromPosition(140f, 100f, range), 0.0001f)
    }

    @Test
    fun invalidCoordinatesFallBackToRangeStart() {
        val range = -1f..1f
        assertEquals(-1f, filterSliderValueFromPosition(Float.NaN, 100f, range), 0.0001f)
        assertEquals(-1f, filterSliderValueFromPosition(50f, 0f, range), 0.0001f)
    }
}

package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GeneratedCameraLutTest {
    @Test
    fun everyBuiltinPresetProducesAValid17Cube() {
        assertEquals(46, builtInCameraLutDefinitions.size)
        builtInCameraLutDefinitions.forEach { definition ->
            val lut = generateBuiltInCameraLookLut(definition)
            assertEquals(17, lut.size)
            assertEquals(17 * 17 * 17 * 3, lut.values.size)
            assertTrue(lut.values.all { it.isFinite() && it in 0f..1f })
        }
    }

    @Test
    fun visuallyDifferentPresetsProduceDifferentLutData() {
        val first = generateBuiltInCameraLookLut(builtInCameraLutDefinitions.first())
        val second = generateBuiltInCameraLookLut(builtInCameraLutDefinitions.last())
        val hasColourDifference = first.values.indices.any { index ->
            abs(first.values[index] - second.values[index]) > 0.01f
        }
        assertTrue("Expected different film looks to generate different 3D LUT values", hasColourDifference)
    }
}

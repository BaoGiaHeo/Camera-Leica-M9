package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.FloatBuffer

class HdmpAiColorLayoutTest {
    @Test
    fun writesRgbPlanesInNchwOrder() {
        val pixels = intArrayOf(0xFFFF0000.toInt(), 0xFF0080FF.toInt())
        val buffer = FloatBuffer.allocate(pixels.size * 3)
        HdmpAiColorLayout.writeRgb(pixels, buffer)
        assertEquals(1f, buffer.get(0), 0.0001f)
        assertEquals(0f, buffer.get(1), 0.0001f)
        assertEquals(0f, buffer.get(2), 0.0001f)
        assertEquals(128f / 255f, buffer.get(3), 0.0001f)
        assertEquals(0f, buffer.get(4), 0.0001f)
        assertEquals(1f, buffer.get(5), 0.0001f)
    }

    @Test
    fun preservesRedGreenBlueValues() {
        val buffer = FloatBuffer.allocate(3)
        HdmpAiColorLayout.writeRgb(intArrayOf(0xFF0C2238.toInt()), buffer)
        assertEquals(12f / 255f, buffer.get(0), 0.0001f)
        assertEquals(34f / 255f, buffer.get(1), 0.0001f)
        assertEquals(56f / 255f, buffer.get(2), 0.0001f)
    }
}

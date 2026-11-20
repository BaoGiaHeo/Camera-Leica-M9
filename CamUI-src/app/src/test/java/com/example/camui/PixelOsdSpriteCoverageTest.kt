package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelOsdSpriteCoverageTest {
    @Test
    fun allRequiredCameraOsdSpritesArePresent() {
        val entries = PixelOsdSprites.entries().toMap()
        assertTrue(
            "Missing OSD sprites: ${PixelOsdSprites.requiredSpriteNames - entries.keys}",
            entries.keys.containsAll(PixelOsdSprites.requiredSpriteNames)
        )
    }

    @Test
    fun eachSpriteIsAConsistentMonochromeOrPalettePixelGrid() {
        val allowed = setOf('.', 'W', 'G', 'R', 'A', 'B')
        PixelOsdSprites.entries().forEach { (name, rows) ->
            assertTrue("$name has no rows", rows.isNotEmpty())
            val width = rows.first().length
            assertTrue("$name has a zero-width row", width > 0)
            rows.forEach { row ->
                assertEquals("$name has inconsistent row width", width, row.length)
                assertTrue("$name has an unsupported pixel code", row.all(allowed::contains))
            }
        }
    }
}

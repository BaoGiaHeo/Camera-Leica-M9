package com.example.camui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelOsdInvalidationTest {
    @Test
    fun gridVisibilityChangeInvalidatesEntireLogicalBuffer() {
        val oldFrame = PixelOsdFrame(showGrid = false)
        val newFrame = PixelOsdFrame(showGrid = true)

        assertEquals(
            listOf(PixelOsdDirtyRect(0, 0, 320, 180)),
            pixelOsdDirtyRegions(320, 180, oldFrame, newFrame)
        )
    }

    @Test
    fun movingFocusPointInvalidatesOnlyOldAndNewPointRegions() {
        val oldFrame = PixelOsdFrame(focusPoints = listOf(Offset(0.2f, 0.3f)))
        val newFrame = PixelOsdFrame(focusPoints = listOf(Offset(0.8f, 0.7f)))

        val dirty = pixelOsdDirtyRegions(320, 180, oldFrame, newFrame)

        assertEquals(2, dirty.size)
        dirty.forEach { rect ->
            assertTrue(rect.right - rect.left <= 5)
            assertTrue(rect.bottom - rect.top <= 5)
        }
    }

    @Test
    fun hiddenZebraCoordinatesDoNotInvalidateTheBuffer() {
        val oldFrame = PixelOsdFrame(showZebra = false, zebraPoints = listOf(Offset(0.2f, 0.2f)))
        val newFrame = PixelOsdFrame(showZebra = false, zebraPoints = listOf(Offset(0.8f, 0.8f)))

        assertTrue(pixelOsdDirtyRegions(320, 180, oldFrame, newFrame).isEmpty())
    }

    @Test
    fun faceFocusAndLockChangesInvalidateOnlyTheirSpriteAreas() {
        val oldFrame = PixelOsdFrame(faceCenter = Offset(0.5f, 0.5f), faceFocused = false)
        val newFrame = PixelOsdFrame(
            faceCenter = Offset(0.5f, 0.5f),
            faceFocused = true,
            focusLocked = true
        )

        val dirty = pixelOsdDirtyRegions(320, 180, oldFrame, newFrame)

        assertEquals(2, dirty.size)
        assertTrue(dirty.all { it.right - it.left < 60 && it.bottom - it.top < 60 })
    }

    @Test
    fun resizingFaceBoxInvalidatesItsFullOldAndNewBounds() {
        val oldFrame = PixelOsdFrame(
            faceCenter = Offset(0.3f, 0.5f),
            faceSize = Offset(0.2f, 0.3f)
        )
        val newFrame = oldFrame.copy(
            faceCenter = Offset(0.7f, 0.5f),
            faceSize = Offset(0.5f, 0.6f)
        )

        val dirty = pixelOsdDirtyRegions(320, 180, oldFrame, newFrame)

        assertEquals(2, dirty.size)
        assertTrue(dirty.any { it.left < 100 })
        assertTrue(dirty.any { it.right > 250 })
        assertTrue(dirty.maxOf { it.right - it.left } >= 160)
    }

    @Test
    fun proHudVisibilityChangeInvalidatesTheProRows() {
        val oldFrame = PixelOsdFrame(isPro = true)
        val newFrame = oldFrame.copy(showProWhiteBalance = false)

        val dirty = pixelOsdDirtyRegions(320, 180, oldFrame, newFrame)

        assertTrue(dirty.any { it.top <= 8 && it.bottom >= 30 })
        assertTrue(dirty.any { it.top <= 164 && it.bottom == 180 })
    }

    @Test
    fun levelRotationInvalidatesCenterRegion() {
        val oldFrame = PixelOsdFrame(showLevel = true, horizonRollDegrees = 0f)
        val newFrame = PixelOsdFrame(showLevel = true, horizonRollDegrees = 5f)

        val dirty = pixelOsdDirtyRegions(320, 180, oldFrame, newFrame)

        assertEquals(1, dirty.size)
        assertTrue(dirty.single().left > 0)
        assertTrue(dirty.single().right < 320)
    }
}

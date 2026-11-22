package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraLcdDisplayModeTest {
    @Test
    fun displayButtonCyclesInRequestedOrder() {
        assertEquals(CameraLcdDisplayMode.CLEAN, CameraLcdDisplayMode.STANDARD.next())
        assertEquals(CameraLcdDisplayMode.LEVEL_GRID, CameraLcdDisplayMode.CLEAN.next())
        assertEquals(CameraLcdDisplayMode.STANDARD, CameraLcdDisplayMode.LEVEL_GRID.next())
        assertEquals(CameraLcdDisplayMode.STANDARD, CameraLcdDisplayMode.FULL_INFO.next())
    }

    @Test
    fun proHudRemainsVisibleInStandardAndFullInfoButNotCleanOrLevelGrid() {
        assertTrue(CameraLcdDisplayMode.STANDARD.showsProHud(true))
        assertTrue(CameraLcdDisplayMode.FULL_INFO.showsProHud(true))
        assertFalse(CameraLcdDisplayMode.CLEAN.showsProHud(true))
        assertFalse(CameraLcdDisplayMode.LEVEL_GRID.showsProHud(true))
        assertFalse(CameraLcdDisplayMode.STANDARD.showsProHud(false))
    }

    @Test
    fun visibleDispModesCycleBackToStandard() {
        repeat(3) {
            var mode = CameraLcdDisplayMode.STANDARD
            repeat(3) { mode = mode.next() }
            assertEquals(CameraLcdDisplayMode.STANDARD, mode)
        }
        assertEquals(CameraLcdDisplayMode.STANDARD, CameraLcdDisplayMode.FULL_INFO.next())
    }
}

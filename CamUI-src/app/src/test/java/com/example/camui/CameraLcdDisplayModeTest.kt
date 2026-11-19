package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraLcdDisplayModeTest {
    @Test
    fun displayButtonCyclesInRequestedOrder() {
        assertEquals(CameraLcdDisplayMode.FULL_INFO, CameraLcdDisplayMode.STANDARD.next())
        assertEquals(CameraLcdDisplayMode.CLEAN, CameraLcdDisplayMode.FULL_INFO.next())
        assertEquals(CameraLcdDisplayMode.LEVEL_GRID, CameraLcdDisplayMode.CLEAN.next())
        assertEquals(CameraLcdDisplayMode.STANDARD, CameraLcdDisplayMode.LEVEL_GRID.next())
    }

    @Test
    fun everyDisplayModeCyclesBackToStandard() {
        repeat(CameraLcdDisplayMode.entries.size) {
            var mode = CameraLcdDisplayMode.STANDARD
            repeat(CameraLcdDisplayMode.entries.size) { mode = mode.next() }
            assertEquals(CameraLcdDisplayMode.STANDARD, mode)
        }
    }
}

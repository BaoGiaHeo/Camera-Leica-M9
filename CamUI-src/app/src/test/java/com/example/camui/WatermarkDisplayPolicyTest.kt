package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class WatermarkDisplayPolicyTest {
    @Test
    fun locationToggleHidesDisplayWithoutDeletingSavedValue() {
        val savedLocation = "  Ho Chi Minh City  "

        assertEquals("Ho Chi Minh City", watermarkLocationForDisplay(true, savedLocation))
        assertEquals("", watermarkLocationForDisplay(false, savedLocation))
        assertEquals("Ho Chi Minh City", watermarkLocationForDisplay(true, savedLocation))
    }

    @Test
    fun blankLocationIsNeverRendered() {
        assertEquals("", watermarkLocationForDisplay(true, "  "))
        assertEquals("", watermarkLocationForDisplay(false, "Da Lat"))
        assertEquals("", watermarkLocationForDisplay(true, null))
    }

    @Test
    fun capHeightOpticalCenterDoesNotDependOnFontDescent() {
        val centerY = 120f
        val capHeight = 30f
        val baseline = watermarkBaselineFromCapHeight(centerY, capHeight)
        val visibleTop = baseline - capHeight

        assertEquals(centerY, (visibleTop + baseline) * 0.5f, 0.001f)
        assertEquals(135f, baseline, 0.001f)
    }

    @Test
    fun negativeCapHeightCannotMoveBaselineAboveOpticalCenter() {
        assertEquals(50f, watermarkBaselineFromCapHeight(50f, -10f), 0.001f)
    }
}

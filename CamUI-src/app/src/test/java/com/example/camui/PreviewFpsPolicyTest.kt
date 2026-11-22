package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewFpsPolicyTest {
    @Test
    fun fixedModesPreferExactAdvertisedRanges() {
        val ranges = listOf(
            PreviewFpsRange(15, 30),
            PreviewFpsRange(30, 30),
            PreviewFpsRange(24, 60),
            PreviewFpsRange(60, 60)
        )
        assertEquals(PreviewFpsRange(60, 60), selectPreviewFpsRange("60", ranges).range)
        assertEquals(PreviewFpsRange(30, 30), selectPreviewFpsRange("30", ranges).range)
        assertTrue(selectPreviewFpsRange("24", ranges).range != null)
        assertTrue(selectPreviewFpsRange("24", ranges).fellBack)
    }

    @Test
    fun fixedModeFallsBackToHighestAdvertisedRateNotAboveTarget() {
        val selection = selectPreviewFpsRange(
            "60",
            listOf(PreviewFpsRange(15, 30), PreviewFpsRange(24, 48))
        )
        assertEquals(PreviewFpsRange(24, 48), selection.range)
        assertEquals(48, selection.targetFps)
        assertTrue(selection.fellBack)
    }

    @Test
    fun autoPrefersARealTwentyFourToSixtyRange() {
        val selection = selectPreviewFpsRange(
            "AUTO",
            listOf(PreviewFpsRange(15, 30), PreviewFpsRange(24, 60), PreviewFpsRange(30, 60))
        )
        assertEquals(PreviewFpsRange(24, 60), selection.range)
        assertEquals(60, selection.targetFps)
        assertFalse(selection.fellBack)
    }

    @Test
    fun emptyCapabilityListDoesNotInventARequest() {
        val selection = selectPreviewFpsRange("60", emptyList())
        assertEquals(null, selection.range)
        assertEquals(null, selection.targetFps)
        assertTrue(selection.fellBack)
    }
}

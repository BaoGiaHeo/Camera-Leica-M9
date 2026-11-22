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
        assertEquals(PreviewFpsRange(15, 30), selection.range)
        assertEquals(30, selection.targetFps)
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
    fun onlyAdvertisedAeRatesAndStreamDurationsAreOffered() {
        val ranges = listOf(PreviewFpsRange(15, 30), PreviewFpsRange(24, 60))
        assertEquals(
            listOf("30", "24", "AUTO"),
            supportedPreviewFpsModes(ranges, listOf(33_333_333L))
        )
        assertEquals(
            listOf("30", "24"),
            supportedPreviewFpsModes(ranges, listOf(33_333_333L, 41_666_666L))
        )
        assertEquals("30", highestSupportedPreviewFpsMode(listOf("30", "24")))
    }

    @Test
    fun emptyCapabilityListDoesNotInventARequest() {
        val selection = selectPreviewFpsRange("60", emptyList())
        assertEquals(null, selection.range)
        assertEquals(null, selection.targetFps)
        assertTrue(selection.fellBack)
    }
}

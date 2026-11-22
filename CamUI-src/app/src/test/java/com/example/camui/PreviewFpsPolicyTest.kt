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
        assertEquals(PreviewFpsRange(15, 30), selectPreviewFpsRange("24", ranges).range)
        assertEquals(24, selectPreviewFpsRange("24", ranges).targetFps)
        assertFalse(selectPreviewFpsRange("24", ranges).fellBack)
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
    fun firstInstallDefaultUsesHighestActuallySupportedRate() {
        assertEquals("60", defaultPreviewFpsMode(listOf(PreviewFpsRange(15, 60))))
        assertEquals("30", defaultPreviewFpsMode(listOf(PreviewFpsRange(15, 30))))
        assertEquals("24", defaultPreviewFpsMode(listOf(PreviewFpsRange(24, 24))))
        assertEquals("AUTO", defaultPreviewFpsMode(emptyList()))
    }

    @Test
    fun settingsOnlyExposeFpsValuesContainedInAdvertisedRanges() {
        assertEquals(
            linkedSetOf("60", "30", "24", "AUTO"),
            supportedPreviewFpsModes(
                listOf(PreviewFpsRange(15, 30), PreviewFpsRange(24, 60))
            )
        )
        assertEquals(
            linkedSetOf("30", "24", "AUTO"),
            supportedPreviewFpsModes(listOf(PreviewFpsRange(15, 30)))
        )
        assertEquals(emptySet<String>(), supportedPreviewFpsModes(emptyList()))
    }

    @Test
    fun emptyCapabilityListDoesNotInventARequest() {
        val selection = selectPreviewFpsRange("60", emptyList())
        assertEquals(null, selection.range)
        assertEquals(null, selection.targetFps)
        assertTrue(selection.fellBack)
    }
}

package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewFpsPolicyTest {
    @Test
    fun acceptsOnlySpecifiedPreviewFpsSettings() {
        assertEquals("60", PreviewFpsPolicy.normalizeSetting(null))
        assertEquals("60", PreviewFpsPolicy.normalizeSetting("120"))
        assertEquals("60", PreviewFpsPolicy.normalizeSetting("system"))
        assertEquals("60", PreviewFpsPolicy.normalizeSetting("60"))
        assertEquals("30", PreviewFpsPolicy.normalizeSetting("30"))
        assertEquals("24", PreviewFpsPolicy.normalizeSetting("24"))
        assertEquals("auto", PreviewFpsPolicy.normalizeSetting("auto"))
    }

    @Test
    fun fixedSixtyUsesTheExactSupportedRange() {
        val selected = PreviewFpsPolicy.select(
            "60",
            listOf(PreviewFpsRange(24, 60), PreviewFpsRange(60, 60), PreviewFpsRange(30, 30))
        )
        assertEquals(PreviewFpsRange(60, 60), selected.range)
        assertEquals(60, selected.requestedFps)
        assertEquals(60, selected.effectiveMaximumFps)
        assertFalse(selected.fellBack)
    }

    @Test
    fun fixedTargetFallsBackToHighestSupportedRangeBelowTheTarget() {
        val selected = PreviewFpsPolicy.select(
            "60",
            listOf(PreviewFpsRange(15, 30), PreviewFpsRange(24, 30), PreviewFpsRange(30, 30))
        )
        assertEquals(PreviewFpsRange(30, 30), selected.range)
        assertEquals(30, selected.effectiveMaximumFps)
        assertTrue(selected.fellBack)
    }

    @Test
    fun variableRangeThatMerelyContainsThirtyIsNotTreatedAsFixedThirty() {
        val selected = PreviewFpsPolicy.select(
            "30",
            listOf(PreviewFpsRange(15, 60))
        )
        assertEquals(PreviewFpsRange(15, 60), selected.range)
        assertEquals(60, selected.effectiveMaximumFps)
        assertTrue(selected.fellBack)
    }

    @Test
    fun autoPrefersRangeCoveringTwentyFourToSixty() {
        val selected = PreviewFpsPolicy.select(
            "auto",
            listOf(PreviewFpsRange(30, 60), PreviewFpsRange(24, 60), PreviewFpsRange(15, 30))
        )
        assertEquals(PreviewFpsRange(24, 60), selected.range)
        assertNull(selected.requestedFps)
        assertFalse(selected.fellBack)
    }

    @Test
    fun autoUsesNearestRangeWhenTwentyFourToSixtyIsUnavailable() {
        val selected = PreviewFpsPolicy.select(
            "auto",
            listOf(PreviewFpsRange(15, 30), PreviewFpsRange(30, 30))
        )
        assertEquals(PreviewFpsRange(15, 30), selected.range)
        assertTrue(selected.fellBack)
    }

    @Test
    fun emptyCameraCapabilityListReportsUnsupportedFallback() {
        val selected = PreviewFpsPolicy.select("24", emptyList())
        assertNull(selected.range)
        assertEquals(24, selected.requestedFps)
        assertTrue(selected.fellBack)
    }
}

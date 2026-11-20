package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomHapticMilestonesTest {
    @Test
    fun reportsEachMilestoneCrossedWhileZoomingIn() {
        assertEquals(listOf(1f, 2f, 5f), zoomHapticMilestonesCrossed(0.6f, 5.4f))
    }

    @Test
    fun reportsEachMilestoneCrossedWhileZoomingOut() {
        assertEquals(listOf(1f, 2f, 5f), zoomHapticMilestonesCrossed(5.4f, 0.6f))
    }

    @Test
    fun doesNotRepeatMilestoneWhileStayingOnSameSide() {
        assertTrue(zoomHapticMilestonesCrossed(2.1f, 2.8f).isEmpty())
        assertTrue(zoomHapticMilestonesCrossed(1f, 1f).isEmpty())
    }

    @Test
    fun ignoresInvalidZoomValues() {
        assertTrue(zoomHapticMilestonesCrossed(Float.NaN, 2f).isEmpty())
        assertTrue(zoomHapticMilestonesCrossed(1f, Float.POSITIVE_INFINITY).isEmpty())
    }
}

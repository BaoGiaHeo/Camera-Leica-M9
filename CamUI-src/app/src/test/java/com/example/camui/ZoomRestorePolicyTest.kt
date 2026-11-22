package com.example.camui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomRestorePolicyTest {
    @Test
    fun acceptsCameraResultWithinOnePercent() {
        assertTrue(isZoomWithinRestoreTolerance(1.009f, 1f))
        assertTrue(isZoomWithinRestoreTolerance(1.991f, 2f))
    }

    @Test
    fun rejectsCameraResultOutsideOnePercent() {
        assertFalse(isZoomWithinRestoreTolerance(1.011f, 1f))
        assertFalse(isZoomWithinRestoreTolerance(1.98f, 2f))
    }

    @Test
    fun rejectsInvalidZoomReadings() {
        assertFalse(isZoomWithinRestoreTolerance(Float.NaN, 1f))
        assertFalse(isZoomWithinRestoreTolerance(1f, Float.POSITIVE_INFINITY))
        assertFalse(isZoomWithinRestoreTolerance(1f, 0f))
    }
}

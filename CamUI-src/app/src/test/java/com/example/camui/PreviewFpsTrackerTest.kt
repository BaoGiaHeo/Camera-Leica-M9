package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviewFpsTrackerTest {
    @Test
    fun waitsForThirtySensorFramesBeforeReportingAStableRate() {
        val tracker = PreviewFpsTracker(windowSize = 30)
        repeat(29) { index ->
            assertNull(tracker.addSensorTimestamp(index * 16_666_667L + 1L))
        }
        assertEquals(60f, tracker.addSensorTimestamp(29 * 16_666_667L + 1L)!!, 0.02f)
    }

    @Test
    fun usesTheNMinusOneTimestampIntervalForTwentyNinePointNinetySevenFps() {
        val tracker = PreviewFpsTracker(windowSize = 30)
        val interval = (1_000_000_000.0 / 29.97).toLong()
        var measured: Float? = null
        repeat(30) { index ->
            measured = tracker.addSensorTimestamp(index * interval + 1L) ?: measured
        }
        assertEquals(29.97f, measured!!, 0.03f)
    }

    @Test
    fun ignoresNullDuplicateAndOutOfOrderSensorTimestamps() {
        val tracker = PreviewFpsTracker(windowSize = 3)
        assertNull(tracker.addSensorTimestamp(null))
        assertNull(tracker.addSensorTimestamp(100L))
        assertNull(tracker.addSensorTimestamp(100L))
        assertNull(tracker.addSensorTimestamp(99L))
        assertNull(tracker.addSensorTimestamp(200L))
        assertEquals(2, tracker.sampleCount())
        assertEquals(2_000f, tracker.addSensorTimestamp(1_000_100L)!!, 0.001f)
    }

    @Test
    fun rollingWindowDropsOldestFrames() {
        val tracker = PreviewFpsTracker(windowSize = 3)
        tracker.addSensorTimestamp(1L)
        tracker.addSensorTimestamp(10_000_001L)
        assertEquals(100f, tracker.addSensorTimestamp(20_000_001L)!!, 0.001f)
        assertEquals(100f, tracker.addSensorTimestamp(30_000_001L)!!, 0.001f)
        assertEquals(3, tracker.sampleCount())
    }
}

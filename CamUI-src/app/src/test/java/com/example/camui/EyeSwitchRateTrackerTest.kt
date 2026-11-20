package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class EyeSwitchRateTrackerTest {
    @Test
    fun countsEyeChangesWithinTheRollingMinute() {
        val tracker = EyeSwitchRateTracker()
        assertEquals(1, tracker.recordSwitch(1_000L))
        assertEquals(2, tracker.recordSwitch(2_000L))
        assertEquals(2, tracker.switchesInWindow(60_000L))
        assertEquals(1, tracker.switchesInWindow(61_500L))
        assertEquals(0, tracker.switchesInWindow(62_001L))
    }

    @Test
    fun expiresSwitchesOlderThanTheWindow() {
        val tracker = EyeSwitchRateTracker(windowMs = 5_000L)
        tracker.recordSwitch(1_000L)
        tracker.recordSwitch(3_000L)
        assertEquals(1, tracker.switchesInWindow(6_001L))
        assertEquals(0, tracker.switchesInWindow(8_001L))
    }

    @Test
    fun resetClearsTheSwitchHistory() {
        val tracker = EyeSwitchRateTracker()
        tracker.recordSwitch(1_000L)
        tracker.reset()
        assertEquals(0, tracker.switchesInWindow(2_000L))
    }
}

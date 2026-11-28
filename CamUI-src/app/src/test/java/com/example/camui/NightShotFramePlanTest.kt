package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class NightShotFramePlanTest {
    @Test
    fun everyNightShotDurationUsesEightFastFrames() {
        assertEquals(8, NightShotFramePlan.targetFrames(3))
        assertEquals(8, NightShotFramePlan.targetFrames(5))
        assertEquals(8, NightShotFramePlan.targetFrames(10))
        assertEquals(8, NightShotFramePlan.targetFrames(7))
    }
}

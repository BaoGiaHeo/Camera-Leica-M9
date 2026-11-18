package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class NightShotFramePlanTest {
    @Test
    fun configuredDurationsUseRequestedFrameCaps() {
        assertEquals(15, NightShotFramePlan.targetFrames(3))
        assertEquals(30, NightShotFramePlan.targetFrames(5))
        assertEquals(50, NightShotFramePlan.targetFrames(10))
    }

    @Test
    fun unknownDurationUsesFiveSecondDefault() {
        assertEquals(30, NightShotFramePlan.targetFrames(7))
    }
}

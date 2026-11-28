package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class NightShotFramePlanTest {
    @Test fun threeSecondNightShotRequestsTwentyFourFrames() {
        assertEquals(24, NightShotFramePlan.targetFrames(3))
    }
    @Test fun fiveSecondNightShotRequestsFortyFrames() {
        assertEquals(40, NightShotFramePlan.targetFrames(5))
    }
    @Test fun tenSecondNightShotRequestsEightyFrames() {
        assertEquals(80, NightShotFramePlan.targetFrames(10))
    }
    @Test fun durationsAreClampedToSupportedRange() {
        assertEquals(24, NightShotFramePlan.targetFrames(0))
        assertEquals(80, NightShotFramePlan.targetFrames(30))
    }
}

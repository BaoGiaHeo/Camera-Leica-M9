package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraBurstFpsPolicyTest {
    @Test
    fun exactEightFpsRangeIsPreferred() {
        val selected = CameraBurstFpsPolicy.selectEightFpsRange(
            listOf(
                CameraBurstFpsRange(5, 10),
                CameraBurstFpsRange(8, 8),
                CameraBurstFpsRange(8, 15)
            )
        )
        assertEquals(CameraBurstFpsRange(8, 8), selected)
    }

    @Test
    fun narrowestAdvertisedRangeContainingEightIsSelected() {
        val selected = CameraBurstFpsPolicy.selectEightFpsRange(
            listOf(
                CameraBurstFpsRange(5, 30),
                CameraBurstFpsRange(7, 10),
                CameraBurstFpsRange(8, 15)
            )
        )
        assertEquals(CameraBurstFpsRange(7, 10), selected)
    }

    @Test
    fun unsupportedEightFpsDoesNotInventARange() {
        assertNull(
            CameraBurstFpsPolicy.selectEightFpsRange(
                listOf(CameraBurstFpsRange(15, 30), CameraBurstFpsRange(30, 30))
            )
        )
    }

    @Test
    fun eightFpsCadenceIs125Milliseconds() {
        assertEquals(125L, CameraBurstFpsPolicy.TARGET_FRAME_INTERVAL_MS)
    }
}

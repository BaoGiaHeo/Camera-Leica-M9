package com.example.camui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FujiKeyHoldPolicyTest {
    @Test
    fun pressBelowThresholdIsTreatedAsTap() {
        assertFalse(isFujiKeyHold(0L))
        assertFalse(isFujiKeyHold(FUJI_KEY_HOLD_THRESHOLD_MS - 1L))
    }

    @Test
    fun pressAtThresholdStartsContinuousHoldBehavior() {
        assertTrue(isFujiKeyHold(FUJI_KEY_HOLD_THRESHOLD_MS))
        assertTrue(isFujiKeyHold(FUJI_KEY_HOLD_THRESHOLD_MS + 1L))
    }
}

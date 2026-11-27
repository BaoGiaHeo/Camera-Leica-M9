package com.example.camui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryWarningPolicyTest {
    @Test
    fun exactThresholdsMatchSpec() {
        val normal = updateBatteryWarningState(BatteryWarningState(), 21, false)
        assertFalse(normal.lowRed)
        assertFalse(normal.blinking)
        assertFalse(normal.crossed)
        assertFalse(normal.showLowNotice)

        val low = updateBatteryWarningState(normal, 20, false)
        assertTrue(low.lowRed)
        assertFalse(low.blinking)
        assertFalse(low.crossed)

        val blink = updateBatteryWarningState(low, 5, false)
        assertTrue(blink.lowRed)
        assertTrue(blink.blinking)
        assertFalse(blink.crossed)
        assertFalse(blink.showLowNotice)

        val critical = updateBatteryWarningState(blink, 1, false)
        assertTrue(critical.lowRed)
        assertTrue(critical.blinking)
        assertTrue(critical.crossed)
        assertTrue(critical.showLowNotice)
    }

    @Test
    fun sixPercentIsRedButNotBlinking() {
        val state = updateBatteryWarningState(BatteryWarningState(), 6, false)
        assertTrue(state.lowRed)
        assertFalse(state.blinking)
        assertFalse(state.crossed)
        assertFalse(state.showLowNotice)
    }

    @Test
    fun chargingClearsAllWarnings() {
        val warning = updateBatteryWarningState(BatteryWarningState(), 1, false)
        val charging = updateBatteryWarningState(warning, 1, true)
        assertFalse(charging.lowRed)
        assertFalse(charging.blinking)
        assertFalse(charging.crossed)
        assertFalse(charging.showLowNotice)
    }
}

package com.example.camui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryWarningPolicyTest {
    @Test
    fun thresholdsMatchSpecifiedBatteryStates() {
        val normal = updateBatteryWarningState(BatteryWarningState(), 25, false)
        assertFalse(normal.lowRed)
        assertFalse(normal.blinking)
        assertFalse(normal.crossed)
        assertFalse(normal.showLowNotice)

        val red = updateBatteryWarningState(normal, 19, false)
        assertTrue(red.lowRed)
        assertFalse(red.blinking)

        val blinking = updateBatteryWarningState(red, 9, false)
        assertTrue(blinking.lowRed)
        assertTrue(blinking.blinking)
        assertFalse(blinking.crossed)

        val crossed = updateBatteryWarningState(blinking, 5, false)
        assertTrue(crossed.crossed)
        assertFalse(crossed.showLowNotice)

        val critical = updateBatteryWarningState(crossed, 1, false)
        assertTrue(critical.showLowNotice)
    }

    @Test
    fun hysteresisPreventsThresholdFlicker() {
        var state = updateBatteryWarningState(BatteryWarningState(), 19, false)
        state = updateBatteryWarningState(state, 20, false)
        assertTrue(state.lowRed)
        state = updateBatteryWarningState(state, 21, false)
        assertFalse(state.lowRed)

        state = updateBatteryWarningState(BatteryWarningState(), 9, false)
        state = updateBatteryWarningState(state, 10, false)
        assertTrue(state.blinking)
        state = updateBatteryWarningState(state, 11, false)
        assertFalse(state.blinking)

        state = updateBatteryWarningState(BatteryWarningState(), 5, false)
        state = updateBatteryWarningState(state, 6, false)
        assertTrue(state.crossed)
        state = updateBatteryWarningState(state, 7, false)
        assertFalse(state.crossed)
    }

    @Test
    fun criticalBannerClearsOnlyAboveTwoPercent() {
        var state = updateBatteryWarningState(BatteryWarningState(), 1, false)
        state = updateBatteryWarningState(state, 2, false)
        assertTrue(state.showLowNotice)
        state = updateBatteryWarningState(state, 3, false)
        assertFalse(state.showLowNotice)
    }

    @Test
    fun chargingImmediatelyClearsAllWarnings() {
        val warning = updateBatteryWarningState(BatteryWarningState(), 1, false)
        val charging = updateBatteryWarningState(warning, 1, true)
        assertFalse(charging.lowRed)
        assertFalse(charging.blinking)
        assertFalse(charging.crossed)
        assertFalse(charging.showLowNotice)
    }
}

package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryAlertStateTest {
    @Test
    fun lowBatteryThresholdUsesOnePercentHysteresis() {
        val entered = resolveBatteryAlertState(19, false)
        assertEquals(true, entered.low)
        assertEquals(false, resolveBatteryAlertState(20, false, BatteryAlertState()).low)
        assertEquals(true, resolveBatteryAlertState(20, false, entered).low)
        assertEquals(false, resolveBatteryAlertState(21, false, entered).low)
    }

    @Test
    fun blinkingThresholdUsesOnePercentHysteresis() {
        val entered = resolveBatteryAlertState(9, false)
        assertEquals(true, entered.blinking)
        assertEquals(true, resolveBatteryAlertState(10, false, entered).blinking)
        assertEquals(false, resolveBatteryAlertState(11, false, entered).blinking)
    }

    @Test
    fun criticalAndWarningThresholdsUseOnePercentHysteresis() {
        val critical = resolveBatteryAlertState(5, false)
        assertEquals(true, critical.critical)
        assertEquals(true, resolveBatteryAlertState(6, false, critical).critical)
        assertEquals(false, resolveBatteryAlertState(7, false, critical).critical)

        val warning = resolveBatteryAlertState(1, false)
        assertEquals(true, warning.warning)
        assertEquals(true, resolveBatteryAlertState(2, false, warning).warning)
        assertEquals(false, resolveBatteryAlertState(3, false, warning).warning)
    }

    @Test
    fun chargingClearsAllBatteryAlerts() {
        val critical = resolveBatteryAlertState(1, false)
        assertEquals(BatteryAlertState(), resolveBatteryAlertState(1, true, critical))
    }

    @Test
    fun higherThresholdsAreNotActivatedPrematurely() {
        assertEquals(BatteryAlertState(), resolveBatteryAlertState(20, false))
        assertEquals(BatteryAlertState(), resolveBatteryAlertState(10, false))
        assertEquals(false, resolveBatteryAlertState(6, false).critical)
        assertEquals(false, resolveBatteryAlertState(2, false).warning)
    }
}

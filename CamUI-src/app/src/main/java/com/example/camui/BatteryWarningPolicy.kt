package com.example.camui

internal data class BatteryWarningState(
    val lowRed: Boolean = false,
    val blinking: Boolean = false,
    val crossed: Boolean = false,
    val showLowNotice: Boolean = false
)

internal fun updateBatteryWarningState(
    previous: BatteryWarningState,
    batteryPercent: Int,
    charging: Boolean
): BatteryWarningState {
    if (charging) return BatteryWarningState()
    if (batteryPercent !in 0..100) return previous
    return BatteryWarningState(
        lowRed = batteryPercent <= 20,
        blinking = batteryPercent <= 5,
        crossed = batteryPercent <= 1,
        showLowNotice = batteryPercent <= 5
    )
}

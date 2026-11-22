package com.example.camui

/**
 * Battery warning thresholds with one-percent hysteresis.
 * The critical banner deliberately clears only above two percent.
 */
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
        lowRed = if (previous.lowRed) batteryPercent < 21 else batteryPercent < 20,
        blinking = if (previous.blinking) batteryPercent <= 10 else batteryPercent < 10,
        crossed = if (previous.crossed) batteryPercent <= 6 else batteryPercent <= 5,
        showLowNotice = if (previous.showLowNotice) batteryPercent <= 2 else batteryPercent <= 1
    )
}

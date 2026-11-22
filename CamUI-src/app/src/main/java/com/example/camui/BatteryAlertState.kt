package com.example.camui

internal data class BatteryAlertState(
    val low: Boolean = false,
    val blinking: Boolean = false,
    val critical: Boolean = false,
    val warning: Boolean = false
)

internal fun resolveBatteryAlertState(
    percent: Int,
    charging: Boolean,
    previous: BatteryAlertState = BatteryAlertState()
): BatteryAlertState {
    if (charging || percent !in 0..100) return BatteryAlertState()
    val low = if (previous.low) percent < 21 else percent < 20
    val blinking = low && if (previous.blinking) percent < 11 else percent < 10
    val critical = low && if (previous.critical) percent < 7 else percent <= 5
    val warning = critical && if (previous.warning) percent <= 2 else percent <= 1
    return BatteryAlertState(
        low = low,
        blinking = blinking,
        critical = critical,
        warning = warning
    )
}

package com.example.camui

import java.util.ArrayDeque

/**
 * Counts automatic primary-eye changes inside a rolling time window.
 * The selector's initial eye assignment is not a change and must not be recorded.
 */
internal class EyeSwitchRateTracker(
    private val windowMs: Long = 60_000L
) {
    private val switchTimestamps = ArrayDeque<Long>()

    init {
        require(windowMs > 0L) { "Eye-switch window must be positive." }
    }

    @Synchronized
    fun recordSwitch(nowMs: Long): Int {
        trim(nowMs)
        switchTimestamps.addLast(nowMs)
        trim(nowMs)
        return switchTimestamps.size
    }

    @Synchronized
    fun switchesInWindow(nowMs: Long): Int {
        trim(nowMs)
        return switchTimestamps.size
    }

    @Synchronized
    fun reset() {
        switchTimestamps.clear()
    }

    private fun trim(nowMs: Long) {
        val earliest = nowMs - windowMs
        while (switchTimestamps.isNotEmpty() && switchTimestamps.first() < earliest) {
            switchTimestamps.removeFirst()
        }
    }
}

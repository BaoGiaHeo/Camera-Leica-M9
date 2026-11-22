package com.example.camui

import java.util.ArrayDeque

/**
 * Measures actual camera preview cadence from SENSOR_TIMESTAMP values.
 * The reported rate uses the exact N-1 interval formula over a 30-frame window.
 */
internal class PreviewFpsTracker(
    private val windowSize: Int = 30
) {
    private val timestamps = ArrayDeque<Long>()

    init {
        require(windowSize >= 2)
    }

    @Synchronized
    fun addSensorTimestamp(timestampNanos: Long?): Float? {
        val timestamp = timestampNanos?.takeIf { it > 0L } ?: return null
        val previous = timestamps.peekLast()
        if (previous != null && timestamp <= previous) {
            return null
        }
        timestamps.addLast(timestamp)
        while (timestamps.size > windowSize) {
            timestamps.removeFirst()
        }
        if (timestamps.size < windowSize) return null
        val first = timestamps.peekFirst() ?: return null
        val last = timestamps.peekLast() ?: return null
        val elapsedNanos = last - first
        if (elapsedNanos <= 0L) return null
        val fps = ((timestamps.size - 1).toDouble() * 1_000_000_000.0 / elapsedNanos).toFloat()
        return fps.takeIf { it.isFinite() && it > 0f }
    }

    @Synchronized
    fun reset() {
        timestamps.clear()
    }

    @Synchronized
    fun sampleCount(): Int = timestamps.size
}

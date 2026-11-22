package com.example.camui

import java.util.ArrayDeque

/**
 * Measures camera preview cadence from Camera2 SENSOR_TIMESTAMP values.
 * It never substitutes display refresh rate or a requested FPS target.
 */
internal class PreviewFpsEstimator(
    private val windowFrames: Int = 30,
    private val publishIntervalMs: Long = 500L
) {
    private val timestampsNs = ArrayDeque<Long>()
    private var lastPublishedAtMs = Long.MIN_VALUE

    init {
        require(windowFrames >= 2) { "At least two timestamps are required." }
        require(publishIntervalMs >= 0L) { "Publish interval cannot be negative." }
    }

    @Synchronized
    fun add(sensorTimestampNs: Long, elapsedRealtimeMs: Long): Float? {
        if (sensorTimestampNs <= 0L || elapsedRealtimeMs < 0L) return null
        val previous = timestampsNs.peekLast()
        if (previous != null && sensorTimestampNs <= previous) {
            timestampsNs.clear()
        }
        timestampsNs.addLast(sensorTimestampNs)
        while (timestampsNs.size > windowFrames) timestampsNs.removeFirst()
        if (timestampsNs.size < windowFrames) return null

        val first = timestampsNs.peekFirst() ?: return null
        val last = timestampsNs.peekLast() ?: return null
        val durationNs = last - first
        if (durationNs <= 0L) return null
        if (lastPublishedAtMs != Long.MIN_VALUE &&
            elapsedRealtimeMs - lastPublishedAtMs < publishIntervalMs
        ) return null

        lastPublishedAtMs = elapsedRealtimeMs
        return ((windowFrames - 1) * 1_000_000_000.0 / durationNs).toFloat()
            .takeIf { it.isFinite() && it > 0f }
    }

    @Synchronized
    fun reset() {
        timestampsNs.clear()
        lastPublishedAtMs = Long.MIN_VALUE
    }
}

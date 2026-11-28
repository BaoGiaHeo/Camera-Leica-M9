package com.example.camui

/**
 * Night Shot target count follows the selected duration at the requested 8 FPS cadence.
 * Actual camera throughput and exposure time can reduce measured FPS; capture diagnostics
 * remain based on sensor timestamps instead of assuming the target was met.
 */
internal object NightShotFramePlan {
    const val TARGET_FPS = 8
    fun targetFrames(durationSeconds: Int): Int =
        durationSeconds.coerceIn(3, 10) * TARGET_FPS
}

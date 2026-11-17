package com.example.camui

/**
 * Maximum frame counts for NightShotMode's selected capture duration.
 * The existing duration deadline remains active, so slower cameras may capture fewer frames.
 */
internal object NightShotFramePlan {
    fun targetFrames(durationSeconds: Int): Int = when (durationSeconds) {
        3 -> 15
        5 -> 30
        10 -> 50
        else -> 30
    }
}

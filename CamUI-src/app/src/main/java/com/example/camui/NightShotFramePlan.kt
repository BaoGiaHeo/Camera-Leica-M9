package com.example.camui

/**
 * Required frame targets for the selected NightShot capture duration.
 * Capture completes the target count before alignment and rendering may begin.
 */
internal object NightShotFramePlan {
    fun targetFrames(durationSeconds: Int): Int = when (durationSeconds) {
        3 -> 15
        5 -> 30
        10 -> 50
        else -> 30
    }
}

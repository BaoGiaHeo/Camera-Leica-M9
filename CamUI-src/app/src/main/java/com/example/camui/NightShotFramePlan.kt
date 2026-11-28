package com.example.camui

/**
 * Night Shot uses the same fast eight-frame acquisition cadence as LunaDream 2.0/3.0.
 * The old duration preference is retained for settings migration, not for capture count.
 */
internal object NightShotFramePlan {
    @Suppress("UNUSED_PARAMETER")
    fun targetFrames(durationSeconds: Int): Int = 8
}

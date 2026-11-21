package com.example.camui

/**
 * Limits the ML Kit fallback analyzer to no more than the requested frames per second.
 * The default 24fps keeps face/eye tracking responsive without processing every camera
 * frame on devices whose analyzer stream runs at 30/60fps.
 */
internal fun shouldRunFaceDetectionFrame(
    nowMs: Long,
    lastStartedAtMs: Long,
    maxFramesPerSecond: Int = 24
): Boolean {
    val safeFps = maxFramesPerSecond.coerceIn(1, 60)
    val minimumIntervalMs = (1_000L + safeFps - 1L) / safeFps
    if (lastStartedAtMs <= 0L) return true
    return nowMs >= lastStartedAtMs && nowMs - lastStartedAtMs >= minimumIntervalMs
}

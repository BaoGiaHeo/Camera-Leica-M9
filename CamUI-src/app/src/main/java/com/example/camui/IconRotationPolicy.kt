package com.example.camui

import kotlin.math.abs

/**
 * Orientation-only presentation rotation. It never changes CameraX target
 * rotation, capture geometry or EXIF orientation.
 */
internal fun snapIconOrientationQuadrant(
    rawDegrees: Int,
    previousQuadrant: Int? = null,
    hysteresisDegrees: Int = 10
): Int? {
    if (rawDegrees !in 0..359) return previousQuadrant
    val normalized = ((rawDegrees % 360) + 360) % 360
    val candidate = (((normalized + 45) / 90) * 90) % 360
    val previous = previousQuadrant?.let { ((it % 360) + 360) % 360 } ?: return candidate
    if (candidate == previous) return previous
    val candidateDistance = abs(((normalized - candidate + 540) % 360) - 180)
    val switchThreshold = (45 - hysteresisDegrees.coerceIn(0, 44)).coerceAtLeast(1)
    return if (candidateDistance <= switchThreshold) candidate else previous
}

internal fun relativeIconRotationDegrees(
    deviceQuadrantDegrees: Int,
    displayRotation: Int
): Int {
    val device = ((deviceQuadrantDegrees % 360) + 360) % 360
    val display = (((displayRotation.coerceIn(0, 3) * 90) % 360) + 360) % 360
    return (device - display + 360) % 360
}

internal fun shortestIconRotationTarget(currentDegrees: Float, targetDegrees: Int): Float {
    val current = currentDegrees.takeIf { it.isFinite() } ?: 0f
    val target = ((targetDegrees % 360) + 360) % 360
    val turns = kotlin.math.round((current - target) / 360f)
    return target + turns * 360f
}

internal object FujiIconRotationState {
    var degrees by androidx.compose.runtime.mutableIntStateOf(0)
    var enabled by androidx.compose.runtime.mutableStateOf(true)
}

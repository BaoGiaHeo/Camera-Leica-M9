package com.example.camui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlin.math.roundToInt

/**
 * Quantizes physical handset orientation with a 12-degree hysteresis margin
 * around each 45-degree quadrant boundary.
 */
internal class FujiIconOrientationTracker(initialQuadrant: Int = 0) {
    private var quadrant = ((initialQuadrant % 4) + 4) % 4

    @Synchronized
    fun update(rawOrientationDegrees: Int): Int? {
        if (rawOrientationDegrees !in 0..359) return null
        val candidate = (((rawOrientationDegrees + 45) / 90) % 4)
        val centerDegrees = candidate * 90
        val distance = kotlin.math.abs(((rawOrientationDegrees - centerDegrees + 540) % 360) - 180)
        if (candidate != quadrant && distance <= 33) {
            quadrant = candidate
        }
        return quadrant * 90
    }
}

internal fun iconRotationRelativeToWindow(
    deviceQuadrantDegrees: Int,
    windowRotation: Int
): Int {
    val windowDegrees = when (windowRotation) {
        android.view.Surface.ROTATION_0 -> 0
        android.view.Surface.ROTATION_90 -> 90
        android.view.Surface.ROTATION_180 -> 180
        android.view.Surface.ROTATION_270 -> 270
        else -> 0
    }
    return ((deviceQuadrantDegrees - windowDegrees + 360) % 360)
}

@Composable
internal fun rememberFujiIconRotation(
    targetDegrees: Int,
    enabled: Boolean,
    reduceMotion: Boolean
): Float {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(targetDegrees, enabled, reduceMotion) {
        if (!enabled || reduceMotion) {
            rotation.snapTo(0f)
        } else {
            val normalized = ((targetDegrees % 360) + 360) % 360
            val nearestEquivalent = normalized +
                (((rotation.value - normalized) / 360f).roundToInt() * 360f)
            rotation.animateTo(
                nearestEquivalent,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
    return rotation.value
}

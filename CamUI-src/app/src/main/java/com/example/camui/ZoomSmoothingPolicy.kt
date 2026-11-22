package com.example.camui

import kotlin.math.abs
import kotlin.math.exp

/** Monotonic exponential response to a changing zoom target; no easing restart. */
internal fun advanceZoomTowardTarget(
    current: Float,
    target: Float,
    deltaSeconds: Float,
    responseHz: Float = 16f
): Float {
    val safeCurrent = current.takeIf { it.isFinite() } ?: target
    val safeTarget = target.takeIf { it.isFinite() } ?: safeCurrent
    val dt = deltaSeconds.takeIf { it.isFinite() }?.coerceIn(0f, 0.05f) ?: 0f
    val response = responseHz.takeIf { it.isFinite() && it > 0f } ?: 16f
    if (dt <= 0f) return safeCurrent
    val alpha = (1f - exp(-response * dt)).coerceIn(0f, 1f)
    return safeCurrent + (safeTarget - safeCurrent) * alpha
}

internal fun isZoomTargetReached(current: Float, target: Float, epsilon: Float = 0.0015f): Boolean =
    current.isFinite() && target.isFinite() && abs(current - target) <= epsilon

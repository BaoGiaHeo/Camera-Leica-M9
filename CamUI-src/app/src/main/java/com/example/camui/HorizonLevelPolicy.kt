package com.example.camui

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.PI
import kotlin.math.sqrt

/** Converts screen-plane gravity to the nearest horizontal/vertical axis error. */
internal object HorizonLevelPolicy {
    /**
     * The returned error is wrapped to [-45, 45), so a phone held in portrait
     * or landscape is judged against its nearest upright axis.
     */
    fun nearestAxisErrorDegrees(screenGravityX: Float, screenGravityY: Float): Float? {
        if (!screenGravityX.isFinite() || !screenGravityY.isFinite()) return null
        if (sqrt(screenGravityX * screenGravityX + screenGravityY * screenGravityY) < 0.15f) {
            return null
        }
        val theta = (atan2(screenGravityX.toDouble(), screenGravityY.toDouble()) * 180.0 / PI).toFloat()
        var wrapped = (theta + 45f) % 90f
        if (wrapped < 0f) wrapped += 90f
        return wrapped - 45f
    }

    /** Hide the bubble when the device is near horizontal and gravity is mostly on Z. */
    fun isNearFlat(gravityZ: Float, magnitude: Float): Boolean =
        gravityZ.isFinite() && magnitude.isFinite() && magnitude > 0.1f &&
            abs(gravityZ) > magnitude * 0.9f
}

/**
 * One-Euro low-pass filter for sensor angles. A lower cutoff suppresses hand tremor;
 * beta raises the cutoff during faster rotation so the indicator still follows movement.
 */
internal class OneEuroFilter(
    private val minimumCutoffHz: Float = 1.0f,
    private val beta: Float = 0.04f,
    private val derivativeCutoffHz: Float = 1.0f
) {
    private var previousTimestampSeconds: Double? = null
    private var previousRawValue: Float? = null
    private var filteredDerivative = 0f
    private var filteredValue = 0f

    fun reset() {
        previousTimestampSeconds = null
        previousRawValue = null
        filteredDerivative = 0f
    }

    fun filter(value: Float, timestampNanos: Long): Float {
        if (!value.isFinite()) return filteredValue
        val timestamp = timestampNanos / 1_000_000_000.0
        val previousTime = previousTimestampSeconds
        val previousRaw = previousRawValue
        if (previousTime == null || previousRaw == null) {
            previousTimestampSeconds = timestamp
            previousRawValue = value
            filteredValue = value
            return value
        }
        val dt = (timestamp - previousTime).toFloat().coerceIn(1f / 240f, 0.2f)
        val derivative = (value - previousRaw) / dt
        val derivativeAlpha = alpha(derivativeCutoffHz, dt)
        filteredDerivative += derivativeAlpha * (derivative - filteredDerivative)
        val cutoff = minimumCutoffHz + beta * abs(filteredDerivative)
        val valueAlpha = alpha(cutoff, dt)
        filteredValue += valueAlpha * (value - filteredValue)
        previousTimestampSeconds = timestamp
        previousRawValue = value
        return filteredValue
    }

    private fun alpha(cutoffHz: Float, dt: Float): Float {
        val tau = 1f / (2f * PI.toFloat() * cutoffHz.coerceAtLeast(0.01f))
        return 1f / (1f + tau / dt.coerceAtLeast(0.0001f))
    }
}

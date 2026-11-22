package com.example.camui

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

internal data class HorizonLevelReading(
    val visible: Boolean,
    val errorDegrees: Float,
    val indicatorAngleDegrees: Float,
    val aligned: Boolean,
    val gravityZRatio: Float
) {
    companion object {
        val HIDDEN = HorizonLevelReading(
            visible = false,
            errorDegrees = 0f,
            indicatorAngleDegrees = 0f,
            aligned = false,
            gravityZRatio = 1f
        )
    }
}

/**
 * Converts a device-space gravity vector to the current display axes, measures the
 * deviation from the nearest horizontal/vertical phone axis, and smooths it with a
 * One-Euro filter. Rotation values are Android Surface.ROTATION_* constants (0..3).
 */
internal class HorizonLevelEstimator(
    private val minCutoffHz: Float = 1.0f,
    private val beta: Float = 0.035f,
    private val derivativeCutoffHz: Float = 1.0f,
    private val enterLevelDegrees: Float = 0.5f,
    private val exitLevelDegrees: Float = 0.9f,
    private val flatGravityThreshold: Float = 0.9f
) {
    private var previousTimestampNanos = 0L
    private var previousRawError: Float? = null
    private var filteredError: Float? = null
    private var filteredDerivative = 0f
    private var aligned = false

    fun update(
        gravityDeviceX: Float,
        gravityDeviceY: Float,
        gravityDeviceZ: Float,
        displayRotation: Int,
        timestampNanos: Long
    ): HorizonLevelReading {
        if (!gravityDeviceX.isFinite() || !gravityDeviceY.isFinite() ||
            !gravityDeviceZ.isFinite()
        ) return HorizonLevelReading.HIDDEN

        val magnitude = hypot(hypot(gravityDeviceX, gravityDeviceY), gravityDeviceZ)
        if (!magnitude.isFinite() || magnitude < 0.01f) return HorizonLevelReading.HIDDEN

        val gx = gravityDeviceX / magnitude
        val gy = gravityDeviceY / magnitude
        val gz = gravityDeviceZ / magnitude
        val gravityZRatio = abs(gz)
        if (gravityZRatio > flatGravityThreshold) {
            return HorizonLevelReading(
                visible = false,
                errorDegrees = filteredError ?: 0f,
                indicatorAngleDegrees = -(filteredError ?: 0f),
                aligned = aligned,
                gravityZRatio = gravityZRatio
            )
        }

        val (screenX, screenY) = deviceGravityToScreen(gx, gy, displayRotation)
        val thetaDegrees = Math.toDegrees(atan2(screenX, screenY).toDouble()).toFloat()
        val rawError = nearestAxisErrorDegrees(thetaDegrees)
        val error = filterOneEuro(rawError, timestampNanos)

        aligned = updateHorizonAligned(
            currentlyAligned = aligned,
            absoluteErrorDegrees = abs(error),
            enterThresholdDegrees = enterLevelDegrees,
            exitThresholdDegrees = exitLevelDegrees
        )

        return HorizonLevelReading(
            visible = true,
            errorDegrees = error,
            // Rotate the LCD indicator in the opposite direction to the measured error.
            indicatorAngleDegrees = -error,
            aligned = aligned,
            gravityZRatio = gravityZRatio
        )
    }

    private fun filterOneEuro(value: Float, timestampNanos: Long): Float {
        val previous = filteredError
        if (previous == null) {
            previousRawError = value
            filteredError = value
            previousTimestampNanos = timestampNanos
            return value
        }

        val dt = if (previousTimestampNanos > 0L && timestampNanos > previousTimestampNanos) {
            ((timestampNanos - previousTimestampNanos) / 1_000_000_000f).coerceIn(1f / 240f, 0.25f)
        } else {
            1f / 60f
        }
        val rawPrevious = previousRawError ?: previous
        val delta = nearestAxisDeltaDegrees(value, rawPrevious)
        val derivative = delta / dt
        val derivativeAlpha = smoothingAlpha(derivativeCutoffHz, dt)
        filteredDerivative += derivativeAlpha * (derivative - filteredDerivative)

        val dynamicCutoff = minCutoffHz + beta * abs(filteredDerivative)
        val valueAlpha = smoothingAlpha(dynamicCutoff, dt)
        val filteredDelta = nearestAxisDeltaDegrees(value, previous)
        val next = wrapNearestAxisError(previous + valueAlpha * filteredDelta)

        previousRawError = value
        filteredError = next
        previousTimestampNanos = timestampNanos
        return next
    }

    private fun smoothingAlpha(cutoffHz: Float, dtSeconds: Float): Float {
        val safeCutoff = cutoffHz.coerceAtLeast(0.001f)
        val tau = 1f / (2f * PI.toFloat() * safeCutoff)
        return 1f / (1f + tau / dtSeconds.coerceAtLeast(0.0001f))
    }
}

internal fun nearestAxisErrorDegrees(thetaDegrees: Float): Float {
    if (!thetaDegrees.isFinite()) return 0f
    return wrapNearestAxisError(thetaDegrees)
}

internal fun updateHorizonAligned(
    currentlyAligned: Boolean,
    absoluteErrorDegrees: Float,
    enterThresholdDegrees: Float = 0.5f,
    exitThresholdDegrees: Float = 0.9f
): Boolean {
    val error = absoluteErrorDegrees.takeIf { it.isFinite() } ?: Float.POSITIVE_INFINITY
    return if (currentlyAligned) {
        error <= exitThresholdDegrees
    } else {
        error < enterThresholdDegrees
    }
}

private fun deviceGravityToScreen(
    gravityX: Float,
    gravityY: Float,
    displayRotation: Int
): Pair<Float, Float> = when (displayRotation) {
    1 -> gravityY to gravityX
    2 -> -gravityX to gravityY
    3 -> -gravityY to -gravityX
    else -> gravityX to -gravityY
}

private fun nearestAxisDeltaDegrees(current: Float, previous: Float): Float {
    var delta = current - previous
    while (delta >= 45f) delta -= 90f
    while (delta < -45f) delta += 90f
    return delta
}

private fun wrapNearestAxisError(degrees: Float): Float {
    val wrapped = ((degrees + 45f) % 90f + 90f) % 90f - 45f
    return if (wrapped == -45f && degrees > 0f) 45f else wrapped
}

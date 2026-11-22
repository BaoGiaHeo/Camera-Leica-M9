package com.example.camui

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Converts the measured gravity vector into the camera LCD's screen axes and
 * reports the error from the closest horizontal/vertical reference axis.
 * The value is always in [-45, 45); null means the handset is nearly flat.
 */
internal object HorizonLevelPolicy {
    private const val GRAVITY_EARTH = 9.80665f

    fun errorDegrees(
        gravityX: Float,
        gravityY: Float,
        gravityZ: Float,
        displayRotation: Int
    ): Float? {
        if (!gravityX.isFinite() || !gravityY.isFinite() || !gravityZ.isFinite()) return null
        val magnitude = sqrt(
            gravityX * gravityX + gravityY * gravityY + gravityZ * gravityZ
        )
        if (!magnitude.isFinite() || magnitude < GRAVITY_EARTH * 0.35f) return null
        val gx = gravityX / magnitude
        val gy = gravityY / magnitude
        val gz = gravityZ / magnitude
        if (abs(gz) > 0.9f) return null

        val (screenX, screenY) = when (displayRotation) {
            1 -> gy to -gx
            2 -> -gx to -gy
            3 -> -gy to gx
            else -> gx to gy
        }
        if (abs(screenX) + abs(screenY) < 0.05f) return null
        val theta = Math.toDegrees(atan2(screenX, screenY).toDouble()).toFloat()
        val wrapped = (((theta + 45f) % 90f) + 90f) % 90f
        return (wrapped - 45f).coerceIn(-45f, 45f)
    }

    fun balanced(errorDegrees: Float, wasBalanced: Boolean): Boolean =
        if (wasBalanced) abs(errorDegrees) <= 0.9f else abs(errorDegrees) < 0.5f
}

/** First-order One-Euro filter with a low-pass filtered derivative. */
internal class OneEuroLevelFilter(
    private val minCutoffHz: Float = 1f,
    private val beta: Float = 0.025f,
    private val derivativeCutoffHz: Float = 1f
) {
    private var lastTimestampNs = 0L
    private var lastRaw = 0f
    private var filtered = 0f
    private var filteredDerivative = 0f
    private var initialized = false

    fun reset() {
        lastTimestampNs = 0L
        lastRaw = 0f
        filtered = 0f
        filteredDerivative = 0f
        initialized = false
    }

    fun filter(value: Float, timestampNs: Long): Float {
        if (!value.isFinite()) return if (initialized) filtered else 0f
        if (!initialized || timestampNs <= lastTimestampNs) {
            initialized = true
            lastTimestampNs = timestampNs
            lastRaw = value
            filtered = value
            filteredDerivative = 0f
            return value
        }
        val dt = ((timestampNs - lastTimestampNs) / 1_000_000_000f).coerceIn(0.001f, 0.25f)
        val derivative = (value - lastRaw) / dt
        val derivativeAlpha = alpha(derivativeCutoffHz, dt)
        filteredDerivative += derivativeAlpha * (derivative - filteredDerivative)
        val cutoff = minCutoffHz + beta * abs(filteredDerivative)
        val valueAlpha = alpha(cutoff, dt)
        filtered += valueAlpha * (value - filtered)
        lastRaw = value
        lastTimestampNs = timestampNs
        return filtered
    }

    private fun alpha(cutoffHz: Float, dt: Float): Float {
        val tau = 1f / (2f * Math.PI.toFloat() * cutoffHz.coerceAtLeast(0.001f))
        return 1f / (1f + tau / dt)
    }
}

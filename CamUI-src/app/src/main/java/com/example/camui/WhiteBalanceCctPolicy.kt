package com.example.camui

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Estimate correlated colour temperature from Camera2 neutral gains and a
 * sensor-RGB-to-XYZ calibration matrix. This is a metadata-derived estimate,
 * not a promise that every OEM exposes usable calibration metadata.
 */
internal object WhiteBalanceCctPolicy {
    fun estimateKelvin(
        rggbGains: FloatArray,
        sensorToXyz: FloatArray
    ): Int? {
        if (rggbGains.size < 4 || sensorToXyz.size < 9) return null
        if (rggbGains.any { !it.isFinite() || it <= 0f }) return null
        if (sensorToXyz.any { !it.isFinite() }) return null

        val redNeutral = 1.0 / rggbGains[0].toDouble()
        val greenGain = (rggbGains[1] + rggbGains[2]) * 0.5
        if (!greenGain.isFinite() || greenGain <= 0f) return null
        val greenNeutral = 1.0 / greenGain.toDouble()
        val blueNeutral = 1.0 / rggbGains[3].toDouble()

        val x = sensorToXyz[0] * redNeutral +
            sensorToXyz[1] * greenNeutral +
            sensorToXyz[2] * blueNeutral
        val y = sensorToXyz[3] * redNeutral +
            sensorToXyz[4] * greenNeutral +
            sensorToXyz[5] * blueNeutral
        val z = sensorToXyz[6] * redNeutral +
            sensorToXyz[7] * greenNeutral +
            sensorToXyz[8] * blueNeutral
        val sum = x + y + z
        if (!sum.isFinite() || sum <= 1e-9) return null

        val chromaticityX = x / sum
        val chromaticityY = y / sum
        val denominator = 0.1858 - chromaticityY
        if (!chromaticityX.isFinite() || !chromaticityY.isFinite() ||
            abs(denominator) < 1e-6
        ) return null

        val n = (chromaticityX - 0.3320) / denominator
        val kelvin = -449.0 * n * n * n +
            3525.0 * n * n -
            6823.3 * n +
            5520.33
        if (!kelvin.isFinite() || kelvin !in 1800.0..25000.0) return null
        return roundTo50Kelvin(kelvin.roundToInt())
    }

    fun roundTo50Kelvin(kelvin: Int): Int =
        (((kelvin.coerceIn(1000, 40000) + 25) / 50) * 50)
}

/** Rate-limits changes to 2 Hz and applies an exponential moving average. */
internal class WhiteBalanceKelvinSmoother(
    private val updateIntervalMs: Long = 500L,
    private val newSampleWeight: Float = 0.28f
) {
    private var lastUpdateMs = Long.MIN_VALUE
    private var filteredKelvin = 0f

    fun update(sampleKelvin: Int?, nowMs: Long): Int? {
        if (nowMs < 0L) return currentOrNull()
        if (lastUpdateMs != Long.MIN_VALUE && nowMs - lastUpdateMs < updateIntervalMs) {
            return currentOrNull()
        }
        lastUpdateMs = nowMs
        val sample = sampleKelvin?.takeIf { it in 1800..25000 } ?: run {
            filteredKelvin = 0f
            return null
        }
        filteredKelvin = if (filteredKelvin <= 0f) {
            sample.toFloat()
        } else {
            filteredKelvin + newSampleWeight.coerceIn(0.01f, 1f) *
                (sample - filteredKelvin)
        }
        return WhiteBalanceCctPolicy.roundTo50Kelvin(filteredKelvin.roundToInt())
    }

    private fun currentOrNull(): Int? =
        filteredKelvin.takeIf { it > 0f }?.roundToInt()?.let(WhiteBalanceCctPolicy::roundTo50Kelvin)
}

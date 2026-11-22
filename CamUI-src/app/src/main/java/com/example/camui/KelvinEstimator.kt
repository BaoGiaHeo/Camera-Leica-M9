package com.example.camui

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.RggbChannelVector
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Estimates correlated colour temperature from the HAL's AWB gains.
 *
 * The estimator uses DNG forward matrices where the camera exposes them. Since
 * some OEM HALs do not publish those matrices, it has a documented sRGB matrix
 * approximation fallback. This is a WB readout estimate, not a calibrated
 * spectrometer measurement.
 */
internal object KelvinEstimator {
    private const val MIN_KELVIN = 1_800f
    private const val MAX_KELVIN = 12_000f

    fun estimate(
        result: TotalCaptureResult,
        characteristics: CameraCharacteristics
    ): Int? {
        val gains = result.get(CaptureResult.COLOR_CORRECTION_GAINS) ?: return null
        val neutral = asShotNeutral(gains) ?: return null
        val forward1 = toMatrix(
            characteristics.get(CameraCharacteristics.SENSOR_FORWARD_MATRIX1)
        )
        val forward2 = toMatrix(
            characteristics.get(CameraCharacteristics.SENSOR_FORWARD_MATRIX2)
        )
        val calibration1 = toMatrix(
            characteristics.get(CameraCharacteristics.SENSOR_CALIBRATION_TRANSFORM1)
        )
        val calibration2 = toMatrix(
            characteristics.get(CameraCharacteristics.SENSOR_CALIBRATION_TRANSFORM2)
        )
        // CalibrationTransform maps reference sensor RGB -> this physical sensor.
        // Invert it before applying the ForwardMatrix (sensor RGB -> XYZ D50).
        val matrix1 = forward1?.let { forward ->
            calibration1?.let { inverse(it)?.let { inverseCalibration -> multiply(forward, inverseCalibration) } }
                ?: forward
        }
        val matrix2 = forward2?.let { forward ->
            calibration2?.let { inverse(it)?.let { inverseCalibration -> multiply(forward, inverseCalibration) } }
                ?: forward
        }
        val reference1 = illuminantKelvin(
            characteristics.get(CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1)?.toInt()
        )
        val reference2 = illuminantKelvin(
            characteristics.get(CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT2)?.toInt()
        )

        val xyz = when {
            matrix1 != null && matrix2 != null -> {
                val seed = cctFromXyz(multiply(SRGB_TO_XYZ, neutral))
                    ?: reference1 ?: reference2 ?: 5_500f
                val blended = interpolateDualIlluminant(
                    matrix1, matrix2, reference1, reference2, seed
                )
                multiply(blended, neutral)
            }
            matrix1 != null -> multiply(matrix1, neutral)
            matrix2 != null -> multiply(matrix2, neutral)
            else -> {
                val colorCorrection = toMatrix(result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM))
                    ?: return null
                val correctedRgb = multiply(colorCorrection, neutral)
                multiply(SRGB_TO_XYZ, correctedRgb)
            }
        }
        val kelvin = cctFromXyz(xyz) ?: return null
        if (!kelvin.isFinite() || kelvin !in MIN_KELVIN..MAX_KELVIN) return null
        return ((kelvin / 50f).roundToInt() * 50).coerceIn(1_800, 12_000)
    }

    internal fun asShotNeutral(gains: RggbChannelVector): FloatArray? =
        asShotNeutral(gains.red, gains.greenEven, gains.greenOdd, gains.blue)

    internal fun asShotNeutral(
        redGain: Float,
        greenEvenGain: Float,
        greenOddGain: Float,
        blueGain: Float
    ): FloatArray? {
        val red = redGain.takeIf { it.isFinite() && it > 0f } ?: return null
        val green = ((greenEvenGain + greenOddGain) * 0.5f)
            .takeIf { it.isFinite() && it > 0f } ?: return null
        val blue = blueGain.takeIf { it.isFinite() && it > 0f } ?: return null
        return floatArrayOf(1f / red, 1f / green, 1f / blue)
    }

    internal fun cctFromXyz(xyz: FloatArray): Float? {
        if (xyz.size < 3 || xyz.any { !it.isFinite() || it <= 0f }) return null
        val sum = xyz[0] + xyz[1] + xyz[2]
        if (!sum.isFinite() || sum <= 0f) return null
        val x = xyz[0] / sum
        val y = xyz[1] / sum
        val denominator = y - 0.1858f
        if (abs(denominator) < 1e-5f) return null
        val n = (x - 0.3320f) / denominator
        val cct = -449f * n.pow(3) + 3_525f * n.pow(2) - 6_823.3f * n + 5_520.33f
        return cct.takeIf { it.isFinite() }
    }

    private fun toMatrix(transform: ColorSpaceTransform?): Array<FloatArray>? {
        transform ?: return null
        return runCatching {
            Array(3) { row ->
                FloatArray(3) { column ->
                    transform.getElement(column, row).toFloat()
                }
            }.takeIf { matrix ->
                matrix.all { row -> row.all { it.isFinite() } }
            }
        }.getOrNull()
    }

    private fun interpolateDualIlluminant(
        first: Array<FloatArray>,
        second: Array<FloatArray>,
        firstKelvin: Float?,
        secondKelvin: Float?,
        sceneKelvin: Float
    ): Array<FloatArray> {
        if (firstKelvin == null || secondKelvin == null ||
            firstKelvin <= 0f || secondKelvin <= 0f || firstKelvin == secondKelvin
        ) return first
        val sceneReciprocal = 1f / sceneKelvin.coerceIn(MIN_KELVIN, MAX_KELVIN)
        val firstReciprocal = 1f / firstKelvin
        val secondReciprocal = 1f / secondKelvin
        val amount = ((sceneReciprocal - firstReciprocal) /
            (secondReciprocal - firstReciprocal)).coerceIn(0f, 1f)
        return Array(3) { row ->
            FloatArray(3) { column ->
                first[row][column] * (1f - amount) + second[row][column] * amount
            }
        }
    }

    private fun illuminantKelvin(value: Int?): Float? = when (value) {
        1, 9 -> 5_500f      // Daylight / fine weather
        3, 17 -> 2_856f     // Tungsten / standard illuminant A
        20 -> 5_503f        // D55
        21 -> 6_504f        // D65
        22 -> 7_504f        // D75
        23 -> 5_003f        // D50
        24 -> 3_200f        // ISO studio tungsten
        else -> null
    }

    private fun multiply(matrix: Array<FloatArray>, vector: FloatArray): FloatArray =
        FloatArray(3) { row ->
            matrix[row][0] * vector[0] +
                matrix[row][1] * vector[1] +
                matrix[row][2] * vector[2]
        }

    private fun multiply(
        left: Array<FloatArray>,
        right: Array<FloatArray>
    ): Array<FloatArray> = Array(3) { row ->
        FloatArray(3) { column ->
            (0..2).sumOf { index ->
                (left[row][index] * right[index][column]).toDouble()
            }.toFloat()
        }
    }

    private fun inverse(matrix: Array<FloatArray>): Array<FloatArray>? {
        val a = matrix[0][0]
        val b = matrix[0][1]
        val c = matrix[0][2]
        val d = matrix[1][0]
        val e = matrix[1][1]
        val f = matrix[1][2]
        val g = matrix[2][0]
        val h = matrix[2][1]
        val i = matrix[2][2]
        val determinant = a * (e * i - f * h) -
            b * (d * i - f * g) + c * (d * h - e * g)
        if (!determinant.isFinite() || abs(determinant) < 1e-6f) return null
        val scale = 1f / determinant
        return arrayOf(
            floatArrayOf(e * i - f * h, c * h - b * i, b * f - c * e),
            floatArrayOf(f * g - d * i, a * i - c * g, c * d - a * f),
            floatArrayOf(d * h - e * g, b * g - a * h, a * e - b * d)
        ).map { row -> FloatArray(3) { column -> row[column] * scale } }.toTypedArray()
    }

    private val SRGB_TO_XYZ = arrayOf(
        floatArrayOf(0.4124564f, 0.3575761f, 0.1804375f),
        floatArrayOf(0.2126729f, 0.7151522f, 0.0721750f),
        floatArrayOf(0.0193339f, 0.1191920f, 0.9503041f)
    )
}

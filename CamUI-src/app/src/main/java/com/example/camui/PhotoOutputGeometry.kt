package com.example.camui

import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Full-frame output geometry. The source is uniformly fitted into the requested
 * aspect-ratio canvas; unused areas become black bars. The source is never cropped
 * or stretched, in either portrait or landscape orientation.
 */
internal data class PhotoOutputGeometryResult(
    val imageLeft: Int,
    val imageTop: Int,
    val imageWidth: Int,
    val imageHeight: Int,
    val outputWidth: Int,
    val outputHeight: Int,
    val requestedAspectRatio: Double
) {
    val outputPixels: Long get() = outputWidth.toLong() * outputHeight.toLong()
    val imagePixels: Long get() = imageWidth.toLong() * imageHeight.toLong()
}

internal object PhotoOutputGeometry {
    fun calculate(
        sourceWidth: Int,
        sourceHeight: Int,
        targetPixels: Long,
        targetAspectRatio: Float? = null
    ): PhotoOutputGeometryResult {
        require(sourceWidth >= 2 && sourceHeight >= 2) { "Source dimensions must be at least 2x2" }
        require(targetPixels >= 4L) { "Target pixel count must be at least four pixels" }

        val requestedAspect = targetAspectRatio
            ?.takeIf { it.isFinite() && it > 0f }
            ?.toDouble()
            ?: (sourceWidth.toDouble() / sourceHeight.toDouble())

        var outputHeight = sqrt(targetPixels.toDouble() / requestedAspect).roundToInt().coerceAtLeast(2)
        var outputWidth = (outputHeight * requestedAspect).roundToInt().coerceAtLeast(2)

        // Keep the requested pixel budget after integer rounding.
        while (outputWidth.toLong() * outputHeight.toLong() > targetPixels) {
            val actualAspect = outputWidth.toDouble() / outputHeight.toDouble()
            if (actualAspect > requestedAspect && outputWidth > 2) {
                outputWidth--
            } else if (outputHeight > 2) {
                outputHeight--
            } else if (outputWidth > 2) {
                outputWidth--
            } else {
                error("Cannot fit output dimensions into target pixel budget")
            }
        }

        // Fit the complete source inside the output canvas using one uniform scale.
        val scale = min(
            outputWidth.toDouble() / sourceWidth.toDouble(),
            outputHeight.toDouble() / sourceHeight.toDouble()
        )
        val imageWidth = (sourceWidth * scale).roundToInt().coerceIn(2, outputWidth)
        val imageHeight = (sourceHeight * scale).roundToInt().coerceIn(2, outputHeight)
        val imageLeft = (outputWidth - imageWidth) / 2
        val imageTop = (outputHeight - imageHeight) / 2

        return PhotoOutputGeometryResult(
            imageLeft = imageLeft,
            imageTop = imageTop,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            outputWidth = outputWidth,
            outputHeight = outputHeight,
            requestedAspectRatio = requestedAspect
        )
    }
}

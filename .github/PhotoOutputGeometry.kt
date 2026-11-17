package com.example.camui

import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Pure output-geometry calculation shared by photo export and unit tests.
 * The requested aspect ratio is applied as a centered crop before scaling.
 */
internal data class PhotoOutputGeometryResult(
    val cropLeft: Int,
    val cropTop: Int,
    val cropWidth: Int,
    val cropHeight: Int,
    val outputWidth: Int,
    val outputHeight: Int,
    val requestedAspectRatio: Double
) {
    val croppedPixels: Long get() = cropWidth.toLong() * cropHeight.toLong()
    val outputPixels: Long get() = outputWidth.toLong() * outputHeight.toLong()
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
        val rawAspect = sourceWidth.toDouble() / sourceHeight.toDouble()

        var cropLeft = 0
        var cropTop = 0
        var cropWidth = sourceWidth
        var cropHeight = sourceHeight
        if (rawAspect > requestedAspect) {
            cropWidth = (sourceHeight * requestedAspect).roundToInt().coerceIn(2, sourceWidth)
            cropLeft = (sourceWidth - cropWidth) / 2
        } else if (rawAspect < requestedAspect) {
            cropHeight = (sourceWidth / requestedAspect).roundToInt().coerceIn(2, sourceHeight)
            cropTop = (sourceHeight - cropHeight) / 2
        }

        val croppedPixels = cropWidth.toLong() * cropHeight.toLong()
        require(croppedPixels > 0L) { "Invalid cropped source dimensions" }
        val scale = sqrt(targetPixels.toDouble() / croppedPixels.toDouble())
        var outputHeight = (cropHeight * scale).roundToInt().coerceAtLeast(2)
        var outputWidth = (outputHeight * requestedAspect).roundToInt().coerceAtLeast(2)

        // Integer dimensions must never exceed the requested or memory-safe pixel budget.
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

        return PhotoOutputGeometryResult(
            cropLeft = cropLeft,
            cropTop = cropTop,
            cropWidth = cropWidth,
            cropHeight = cropHeight,
            outputWidth = outputWidth,
            outputHeight = outputHeight,
            requestedAspectRatio = requestedAspect
        )
    }
}

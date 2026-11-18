package com.example.camui

import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Output geometry for a direct center crop.
 *
 * Ratios supplied by the camera UI are height / width. The source rectangle is
 * cropped around its center to the matching output aspect ratio; no backdrop,
 * padding, or second image layer is generated.
 */
internal data class PhotoOutputGeometryResult(
    val sourceLeft: Int,
    val sourceTop: Int,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val outputWidth: Int,
    val outputHeight: Int,
    val requestedWidthOverHeight: Double
) {
    val outputPixels: Long get() = outputWidth.toLong() * outputHeight.toLong()
    val sourceCropPixels: Long get() = sourceWidth.toLong() * sourceHeight.toLong()
}

internal object PhotoOutputGeometry {
    fun calculate(
        sourceWidth: Int,
        sourceHeight: Int,
        targetPixels: Long,
        targetHeightOverWidth: Float? = null
    ): PhotoOutputGeometryResult {
        require(sourceWidth >= 2 && sourceHeight >= 2) { "Source dimensions must be at least 2x2" }
        require(targetPixels >= 4L) { "Target pixel count must be at least four pixels" }

        val requestedHeightOverWidth = targetHeightOverWidth
            ?.takeIf { it.isFinite() && it > 0f }
            ?.toDouble()
            ?: (sourceHeight.toDouble() / sourceWidth.toDouble())
        val requestedWidthOverHeight = 1.0 / requestedHeightOverWidth

        var outputHeight = sqrt(targetPixels.toDouble() / requestedWidthOverHeight)
            .roundToInt()
            .coerceAtLeast(2)
        var outputWidth = (outputHeight * requestedWidthOverHeight)
            .roundToInt()
            .coerceAtLeast(2)

        // Keep the requested pixel budget after integer rounding without changing the
        // selected ratio by more than the unavoidable one-pixel quantization.
        while (outputWidth.toLong() * outputHeight.toLong() > targetPixels) {
            val actualAspect = outputWidth.toDouble() / outputHeight.toDouble()
            if (actualAspect > requestedWidthOverHeight && outputWidth > 2) {
                outputWidth--
            } else if (outputHeight > 2) {
                outputHeight--
            } else if (outputWidth > 2) {
                outputWidth--
            } else {
                error("Cannot fit output dimensions into target pixel budget")
            }
        }

        // Center-crop the original upright Bitmap to the requested frame ratio.
        val sourceAspect = sourceWidth.toDouble() / sourceHeight.toDouble()
        val sourceLeft: Int
        val sourceTop: Int
        val cropWidth: Int
        val cropHeight: Int
        if (sourceAspect > requestedWidthOverHeight) {
            cropHeight = sourceHeight
            cropWidth = (sourceHeight * requestedWidthOverHeight)
                .roundToInt()
                .coerceIn(2, sourceWidth)
            sourceLeft = (sourceWidth - cropWidth) / 2
            sourceTop = 0
        } else {
            cropWidth = sourceWidth
            cropHeight = (sourceWidth / requestedWidthOverHeight)
                .roundToInt()
                .coerceIn(2, sourceHeight)
            sourceLeft = 0
            sourceTop = (sourceHeight - cropHeight) / 2
        }

        return PhotoOutputGeometryResult(
            sourceLeft = sourceLeft,
            sourceTop = sourceTop,
            sourceWidth = cropWidth,
            sourceHeight = cropHeight,
            outputWidth = outputWidth,
            outputHeight = outputHeight,
            requestedWidthOverHeight = requestedWidthOverHeight
        )
    }
}

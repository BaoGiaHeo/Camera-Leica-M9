package com.example.camui

import androidx.camera.core.ImageProxy
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Conservative preview-only lens obstruction heuristic.
 *
 * A flat scene, motion blur, weak focus or low light can resemble a smudged lens.
 * The caller requires several consecutive positive samples and the UI wording remains advisory.
 */
internal object LensDirtDetector {
    fun mayBeObscured(image: ImageProxy): Boolean {
        val plane = image.planes.firstOrNull() ?: return false
        val width = image.width
        val height = image.height
        if (width < 96 || height < 96) return false

        val buffer = plane.buffer.duplicate()
        val base = buffer.position()
        val limit = buffer.limit()
        val stepX = (width / 44).coerceAtLeast(1)
        val stepY = (height / 32).coerceAtLeast(1)
        val values = IntArray(44 * 32)
        var count = 0
        var sum = 0.0
        var sumSquares = 0.0
        var gradientSum = 0.0
        var gradientCount = 0
        var strongEdgeCount = 0

        var y = height / 8
        while (y < height * 7 / 8) {
            var x = width / 8
            while (x < width * 7 / 8) {
                val index = base + y * plane.rowStride + x * plane.pixelStride
                if (index < base || index >= limit) return false
                val value = buffer.get(index).toInt() and 0xFF
                values[count++] = value
                sum += value
                sumSquares += value.toDouble() * value.toDouble()
                x += stepX
            }
            y += stepY
        }
        if (count < 120) return false

        // Re-sample neighbouring luminance values to estimate global edge activity.
        y = height / 8
        while (y < height * 7 / 8) {
            var x = width / 8
            while (x < width * 7 / 8) {
                val index = base + y * plane.rowStride + x * plane.pixelStride
                val rightIndex = index + stepX * plane.pixelStride
                val downIndex = index + stepY * plane.rowStride
                if (index in base until limit && rightIndex in base until limit && downIndex in base until limit) {
                    val center = buffer.get(index).toInt() and 0xFF
                    val right = buffer.get(rightIndex).toInt() and 0xFF
                    val down = buffer.get(downIndex).toInt() and 0xFF
                    val gx = abs(center - right)
                    val gy = abs(center - down)
                    gradientSum += gx + gy
                    gradientCount += 2
                    if (gx >= 18 || gy >= 18) strongEdgeCount++
                }
                x += stepX
            }
            y += stepY
        }
        if (gradientCount == 0) return false

        val mean = sum / count
        val variance = (sumSquares / count - mean * mean).coerceAtLeast(0.0)
        val standardDeviation = sqrt(variance)
        val averageGradient = gradientSum / gradientCount
        val strongEdgeFraction = strongEdgeCount.toDouble() / count

        // Reject dark, very flat or genuinely textureless scenes to reduce false alarms.
        return mean in 38.0..222.0 &&
            standardDeviation >= 22.0 &&
            averageGradient < 7.2 &&
            strongEdgeFraction < 0.12
    }
}

package com.example.camui

import android.graphics.Bitmap
import java.util.Locale

internal data class PhotoQualityReport(
    val sharpnessLabel: String,
    val highlightClippingLabel: String,
    val shadowClippingLabel: String,
    val noiseLabel: String,
    val recommendation: String
)

/**
 * Advisory-only image checks on a small upright preview.
 * The analyzer never mutates, rejects, or deletes the source photo.
 * Sharpness is a Laplacian heuristic; its low score cannot distinguish camera shake,
 * subject motion, missed focus, or intentionally shallow depth of field.
 * Flat-patch noise estimates can also include real fine texture and JPEG artefacts.
 */
internal object PhotoQualityAnalyzer {
    fun analyze(bitmap: Bitmap): PhotoQualityReport {
        if (bitmap.width < 5 || bitmap.height < 5) return insufficientData()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return analyzePixels(bitmap.width, bitmap.height, pixels)
    }

    internal fun analyzePixels(width: Int, height: Int, pixels: IntArray): PhotoQualityReport {
        if (width < 5 || height < 5 || width.toLong() * height.toLong() > pixels.size.toLong()) {
            return insufficientData()
        }

        fun luma(pixel: Int): Float {
            val red = (pixel ushr 16) and 0xFF
            val green = (pixel ushr 8) and 0xFF
            val blue = pixel and 0xFF
            return red * 0.2126f + green * 0.7152f + blue * 0.0722f
        }

        var sampleCount = 0
        var highlightClipped = 0
        var shadowClipped = 0
        var laplacianSum = 0.0
        var laplacianSquaredSum = 0.0
        var flatPatchResidual = 0.0
        var flatPatchCount = 0
        val step = maxOf(2, maxOf(width, height) / 256)

        var y = 1
        while (y < height - 1) {
            var x = 1
            while (x < width - 1) {
                val index = y * width + x
                val center = luma(pixels[index])
                val left = luma(pixels[index - 1])
                val right = luma(pixels[index + 1])
                val up = luma(pixels[index - width])
                val down = luma(pixels[index + width])

                if (center >= 250f) highlightClipped++
                if (center <= 5f) shadowClipped++

                val laplacian = 4f * center - left - right - up - down
                laplacianSum += laplacian
                laplacianSquaredSum += laplacian * laplacian
                sampleCount++

                val localGradient = kotlin.math.abs(center - left) +
                    kotlin.math.abs(center - right) +
                    kotlin.math.abs(center - up) +
                    kotlin.math.abs(center - down)
                if (localGradient < 22f) {
                    flatPatchResidual += kotlin.math.abs(laplacian)
                    flatPatchCount++
                }
                x += step
            }
            y += step
        }

        if (sampleCount == 0) return insufficientData()

        val mean = laplacianSum / sampleCount
        val sharpnessVariance = (laplacianSquaredSum / sampleCount - mean * mean).coerceAtLeast(0.0)
        val sharpness = when {
            sharpnessVariance >= 180.0 -> "Cao · " + String.format(Locale.US, "%.0f", sharpnessVariance)
            sharpnessVariance >= 55.0 -> "Trung bình · " + String.format(Locale.US, "%.0f", sharpnessVariance)
            else -> "Thấp · " + String.format(Locale.US, "%.0f", sharpnessVariance)
        }

        val highlightPercent = highlightClipped * 100.0 / sampleCount
        val shadowPercent = shadowClipped * 100.0 / sampleCount
        val highlights = String.format(Locale.US, "%.2f%% pixel gần trắng tuyệt đối", highlightPercent)
        val shadows = String.format(Locale.US, "%.2f%% pixel gần đen tuyệt đối", shadowPercent)
        val noiseEstimate = if (flatPatchCount > 0) flatPatchResidual / flatPatchCount else 0.0
        val noise = when {
            flatPatchCount == 0 -> "Không đủ vùng phẳng để ước tính"
            noiseEstimate >= 18.0 -> "Cao · " + String.format(Locale.US, "%.1f", noiseEstimate) + " (lẫn texture)"
            noiseEstimate >= 8.0 -> "Vừa · " + String.format(Locale.US, "%.1f", noiseEstimate) + " (lẫn texture)"
            else -> "Thấp · " + String.format(Locale.US, "%.1f", noiseEstimate) + " (lẫn texture)"
        }

        val suggestions = buildList {
            if (sharpnessVariance < 55.0) {
                add("Biên ảnh mềm; có thể do rung máy, chuyển động chủ thể hoặc lệch nét. Hãy giữ máy chắc và chạm lấy nét")
            }
            if (highlightPercent > 3.0) {
                add("Có vùng sáng mất chi tiết; cân nhắc giảm EV hoặc bảo vệ highlight")
            }
            if (shadowPercent > 5.0) {
                add("Vùng tối có thể đã bệt đen; cân nhắc tăng sáng hoặc giảm tương phản")
            }
            if (noiseEstimate >= 18.0) {
                add("Nhiễu/texture cao; thử giảm ISO hoặc tăng ánh sáng")
            }
        }
        val recommendation = suggestions.takeIf { it.isNotEmpty() }?.joinToString(" · ")
            ?: "Chưa thấy dấu hiệu mờ hoặc clipping nghiêm trọng theo phép đo nhanh"
        return PhotoQualityReport(sharpness, highlights, shadows, noise, recommendation)
    }

    private fun insufficientData() = PhotoQualityReport(
        "Không đủ dữ liệu",
        "Không đủ dữ liệu",
        "Không đủ dữ liệu",
        "Không đủ dữ liệu",
        "Kiểm tra ảnh thủ công"
    )
}

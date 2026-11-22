package com.example.camui

import android.graphics.Bitmap
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

internal data class WatermarkOklab(val l: Double, val a: Double, val b: Double) {
    fun distanceTo(other: WatermarkOklab): Double {
        val dl = l - other.l
        val da = a - other.a
        val db = b - other.b
        return sqrt(dl * dl + da * da + db * db)
    }
}

internal fun watermarkRgbToOklab(color: Int): WatermarkOklab {
    fun linear(channel: Int): Double {
        val value = channel.coerceIn(0, 255) / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
    val r = linear(android.graphics.Color.red(color))
    val g = linear(android.graphics.Color.green(color))
    val b = linear(android.graphics.Color.blue(color))
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    return WatermarkOklab(
        0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    )
}

private fun oklabToRgb(lab: WatermarkOklab): Int {
    val l = (lab.l + 0.3963377774 * lab.a + 0.2158037573 * lab.b).let { it * it * it }
    val m = (lab.l - 0.1055613458 * lab.a - 0.0638541728 * lab.b).let { it * it * it }
    val s = (lab.l - 0.0894841775 * lab.a - 1.2914855480 * lab.b).let { it * it * it }
    val r = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
    val g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
    val b = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    fun encoded(value: Double): Int {
        val v = value.coerceIn(0.0, 1.0)
        val srgb = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1.0 / 2.4) - 0.055
        return (srgb * 255.0).toInt().coerceIn(0, 255)
    }
    return android.graphics.Color.rgb(encoded(r), encoded(g), encoded(b))
}

/**
 * Extracts five representative swatches from a 128px thumbnail using deterministic
 * k-means in OKLab. Near-black/near-white clusters are filtered first; remaining
 * swatches must be at least delta-E 8 apart when possible.
 */
internal fun extractWatermarkPalette(source: Bitmap, requestedCount: Int = 5): List<Int> {
    require(!source.isRecycled && source.width > 0 && source.height > 0)
    val count = requestedCount.coerceIn(1, 5)
    val factor = min(1f, 128f / max(source.width, source.height).toFloat())
    val sample = if (factor < 0.999f) {
        Bitmap.createScaledBitmap(
            source,
            (source.width * factor).toInt().coerceAtLeast(1),
            (source.height * factor).toInt().coerceAtLeast(1),
            true
        )
    } else source
    try {
        val pixels = IntArray(sample.width * sample.height)
        sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
        val labs = pixels.map(::watermarkRgbToOklab)
        if (labs.isEmpty()) return emptyList()
        val k = min(count, labs.size)
        val ordered = labs.sortedBy { it.l }
        var centers = (0 until k).map { index ->
            ordered[((index + 0.5) * ordered.size / k).toInt().coerceIn(0, ordered.lastIndex)]
        }
        repeat(10) {
            val sums = Array(k) { DoubleArray(3) }
            val weights = IntArray(k)
            labs.forEach { point ->
                var selected = 0
                var nearest = Double.MAX_VALUE
                centers.forEachIndexed { index, center ->
                    val distance = point.distanceTo(center)
                    if (distance < nearest) {
                        nearest = distance
                        selected = index
                    }
                }
                sums[selected][0] += point.l
                sums[selected][1] += point.a
                sums[selected][2] += point.b
                weights[selected]++
            }
            centers = centers.mapIndexed { index, previous ->
                if (weights[index] == 0) previous else WatermarkOklab(
                    sums[index][0] / weights[index],
                    sums[index][1] / weights[index],
                    sums[index][2] / weights[index]
                )
            }
        }

        val sorted = centers.distinct().sortedBy { it.l }
        val colorful = sorted.filter { it.l in 0.08..0.94 && sqrt(it.a * it.a + it.b * it.b) > 0.018 }
        val chosen = mutableListOf<WatermarkOklab>()
        (colorful + sorted).forEach { candidate ->
            if (chosen.size < count && chosen.all { it.distanceTo(candidate) * 100.0 > 8.0 }) {
                chosen += candidate
            }
        }
        if (chosen.size < count) {
            sorted.forEach { candidate ->
                if (chosen.size < count && candidate !in chosen) chosen += candidate
            }
        }
        return chosen.sortedBy { it.l }.map(::oklabToRgb)
    } finally {
        if (sample !== source && !sample.isRecycled) sample.recycle()
    }
}

package com.example.camui

import kotlin.math.pow
import kotlin.math.sqrt

data class TonePadRgb(val red: Float, val green: Float, val blue: Float)

data class TonePadLab(val lightness: Float, val a: Float, val b: Float)

/**
 * Two-axis color treatment in OKLab:
 * - X: pale (-1) to rich (+1), scaling a/b chroma
 * - Y: dark (-1) to bright (+1), shifting perceptual lightness
 * - Intensity: 0..1, scales both adjustments
 */
object TonePadMath {
    fun apply(
        red: Float,
        green: Float,
        blue: Float,
        paleRich: Float,
        darkBright: Float,
        intensity: Float
    ): TonePadRgb {
        val amount = intensity.coerceIn(0f, 1f)
        if (amount <= 0.0001f || (kotlin.math.abs(paleRich) <= 0.0001f &&
                    kotlin.math.abs(darkBright) <= 0.0001f)
        ) {
            return TonePadRgb(red.coerceIn(0f, 1f), green.coerceIn(0f, 1f), blue.coerceIn(0f, 1f))
        }
        val lab = toOklab(red, green, blue)
        val adjusted = TonePadLab(
            lightness = (lab.lightness + darkBright.coerceIn(-1f, 1f) * 0.14f * amount)
                .coerceIn(0f, 1f),
            a = lab.a * (1f + paleRich.coerceIn(-1f, 1f) * 0.55f * amount),
            b = lab.b * (1f + paleRich.coerceIn(-1f, 1f) * 0.55f * amount)
        )
        return fromOklab(adjusted)
    }

    fun toOklab(red: Float, green: Float, blue: Float): TonePadLab {
        val r = srgbToLinear(red.coerceIn(0f, 1f))
        val g = srgbToLinear(green.coerceIn(0f, 1f))
        val b = srgbToLinear(blue.coerceIn(0f, 1f))

        val l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b
        val m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b
        val s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b

        val lRoot = Math.cbrt(l.toDouble()).toFloat()
        val mRoot = Math.cbrt(m.toDouble()).toFloat()
        val sRoot = Math.cbrt(s.toDouble()).toFloat()

        return TonePadLab(
            lightness = 0.2104542553f * lRoot + 0.7936177850f * mRoot - 0.0040720468f * sRoot,
            a = 1.9779984951f * lRoot - 2.4285922050f * mRoot + 0.4505937099f * sRoot,
            b = 0.0259040371f * lRoot + 0.7827717662f * mRoot - 0.8086757660f * sRoot
        )
    }

    fun fromOklab(lab: TonePadLab): TonePadRgb {
        val lRoot = lab.lightness + 0.3963377774f * lab.a + 0.2158037573f * lab.b
        val mRoot = lab.lightness - 0.1055613458f * lab.a - 0.0638541728f * lab.b
        val sRoot = lab.lightness - 0.0894841775f * lab.a - 1.2914855480f * lab.b

        val l = lRoot * lRoot * lRoot
        val m = mRoot * mRoot * mRoot
        val s = sRoot * sRoot * sRoot

        val r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
        val g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
        val b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s

        return TonePadRgb(
            linearToSrgb(r).coerceIn(0f, 1f),
            linearToSrgb(g).coerceIn(0f, 1f),
            linearToSrgb(b).coerceIn(0f, 1f)
        )
    }

    fun deltaE76(first: TonePadLab, second: TonePadLab): Float {
        val dL = first.lightness - second.lightness
        val da = first.a - second.a
        val db = first.b - second.b
        return sqrt(dL * dL + da * da + db * db)
    }

    fun preferencePrefix(filterName: String): String {
        val normalized = filterName
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "none" }
        return "tone_pad.$normalized"
    }

    private fun srgbToLinear(value: Float): Float =
        if (value <= 0.04045f) value / 12.92f
        else ((value + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()

    private fun linearToSrgb(value: Float): Float =
        if (value <= 0.0031308f) value * 12.92f
        else (1.055f * value.coerceAtLeast(0f).toDouble().pow(1.0 / 2.4) - 0.055).toFloat()
}

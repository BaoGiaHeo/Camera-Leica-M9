package com.example.camui

import kotlin.math.ceil
import kotlin.math.sqrt

internal enum class LunaDreamTier(val preferenceId: String) {
    OFF("off"),
    LUNA_1_5("luna15"),
    LUNA_2_0("luna20"),
    LUNA_2_5("luna25"),
    LUNA_3_0("luna30");

    companion object {
        fun fromPreference(value: String?): LunaDreamTier =
            values().firstOrNull { it.preferenceId == value } ?: OFF
    }
}

internal data class LunaDreamModelSpec(
    val tier: LunaDreamTier,
    val targetPixels: Long,
    val nominalWidth4x3: Int,
    val nominalHeight4x3: Int,
    val minimumFrames: Int,
    val baseFrames: Int,
    val maximumFrames: Int,
    val alignmentPyramidLevels: Int,
    val targetDurationSeconds: Int
) {
    fun frameTarget(iso: Int, isoReference: Int = 100): Int {
        if (maximumFrames <= 1) return 1
        val normalizedIso = (iso.coerceAtLeast(1).toDouble() / isoReference.coerceAtLeast(1))
        val scaled = ceil(baseFrames * sqrt(normalizedIso)).toInt()
        return scaled.coerceIn(minimumFrames, maximumFrames)
    }
}


/**
 * Nominal output dimensions are a pixel-count contract only. A larger JPEG does
 * not demonstrate real super-resolution; the processing engine must also pass
 * the multi-frame detail and texture-retention benchmarks.
 */
internal object LunaDreamModelPolicy {
    private val specs = mapOf(
        LunaDreamTier.LUNA_1_5 to LunaDreamModelSpec(
            LunaDreamTier.LUNA_1_5, 50_000_000L, 8_160, 6_120,
            minimumFrames = 1, baseFrames = 1, maximumFrames = 2,
            alignmentPyramidLevels = 0, targetDurationSeconds = 4
        ),
        LunaDreamTier.LUNA_2_0 to LunaDreamModelSpec(
            LunaDreamTier.LUNA_2_0, 108_000_000L, 12_000, 9_000,
            minimumFrames = 3, baseFrames = 4, maximumFrames = 5,
            alignmentPyramidLevels = 3, targetDurationSeconds = 12
        ),
        LunaDreamTier.LUNA_2_5 to LunaDreamModelSpec(
            LunaDreamTier.LUNA_2_5, 150_000_000L, 14_144, 10_608,
            minimumFrames = 6, baseFrames = 8, maximumFrames = 10,
            alignmentPyramidLevels = 4, targetDurationSeconds = 30
        ),
        LunaDreamTier.LUNA_3_0 to LunaDreamModelSpec(
            LunaDreamTier.LUNA_3_0, 200_000_000L, 16_320, 12_240,
            minimumFrames = 12, baseFrames = 14, maximumFrames = 16,
            alignmentPyramidLevels = 5, targetDurationSeconds = 70
        )
    )

    fun spec(tier: LunaDreamTier): LunaDreamModelSpec? = specs[tier]

    fun targetPixels(tier: LunaDreamTier): Long? = spec(tier)?.targetPixels

    fun nominal4x3PixelCount(tier: LunaDreamTier): Long? =
        spec(tier)?.let { it.nominalWidth4x3.toLong() * it.nominalHeight4x3.toLong() }

    fun isNominal4x3WithinOnePercent(tier: LunaDreamTier): Boolean {
        val value = spec(tier) ?: return false
        val actual = value.nominalWidth4x3.toLong() * value.nominalHeight4x3.toLong()
        return actual >= value.targetPixels * 99L / 100L
    }
}

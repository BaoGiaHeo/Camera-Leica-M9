package com.example.camui

/**
 * Describes where the pixels in a LunaDream output are sourced from.
 * A: accessible maximum-resolution stream >=190MP; B: ~40MP+ sensor reconstructed by multi-frame SR;
 * C: smaller sensors use at most 2x linear enlargement and report actual MP.
 */
internal enum class LunaDreamSourceStrategy {
    A_NATIVE_HIGH_RESOLUTION,
    B_COMPUTATIONAL_MULTI_FRAME,
    C_LIMITED_UPSCALE
}

internal data class LunaDreamSensorCapability(
    val normalPixelArrayWidth: Int,
    val normalPixelArrayHeight: Int,
    val maximumResolutionWidth: Int,
    val maximumResolutionHeight: Int,
    val maximumResolutionStreamAvailable: Boolean,
    val ultraHighResolutionCapability: Boolean
) {
    val normalPixelCount: Long
        get() = normalPixelArrayWidth.toLong().coerceAtLeast(0L) * normalPixelArrayHeight.toLong().coerceAtLeast(0L)

    val maximumPixelCount: Long
        get() = maximumResolutionWidth.toLong().coerceAtLeast(0L) * maximumResolutionHeight.toLong().coerceAtLeast(0L)

    fun strategy(): LunaDreamSourceStrategy {
        val accessibleHighResolutionStream = maximumResolutionStreamAvailable &&
            ultraHighResolutionCapability && maximumPixelCount >= 190_000_000L
        return when {
            accessibleHighResolutionStream -> LunaDreamSourceStrategy.A_NATIVE_HIGH_RESOLUTION
            normalPixelCount >= 40_000_000L -> LunaDreamSourceStrategy.B_COMPUTATIONAL_MULTI_FRAME
            else -> LunaDreamSourceStrategy.C_LIMITED_UPSCALE
        }
    }

    fun diagnosticLine(): String =
        "normal=${normalPixelArrayWidth}x${normalPixelArrayHeight}(${normalPixelCount / 1_000_000L}MP) " +
        "maximum=${maximumResolutionWidth}x${maximumResolutionHeight}(${maximumPixelCount / 1_000_000L}MP) " +
        "maxStream=$maximumResolutionStreamAvailable ultraHighRes=$ultraHighResolutionCapability " +
        "strategy=${strategy()}"
}

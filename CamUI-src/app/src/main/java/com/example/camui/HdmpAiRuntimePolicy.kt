package com.example.camui

import kotlin.math.abs

/**
 * Runtime policy for HDMP AI.
 *
 * NNAPI is tried before XNNPACK so Android can delegate supported work to a GPU/NPU.
 * XNNPACK and the ORT CPU provider remain fallbacks; the selected NAFNet model is unchanged.
 *
 * The detail guard preserves more of the original signal at strong edges and thin strands,
 * while allowing full model blending in smoother areas for stronger denoising.
 */
internal object HdmpAiRuntimePolicy {
    fun providerOrder(availableProviders: Set<String>): List<String> = buildList {
        if ("NNAPI" in availableProviders) add("NNAPI_FP16")
        if ("XNNPACK" in availableProviders) add("XNNPACK")
        add("CPU")
    }

    fun detailPreservationScale(center: Int, left: Int, right: Int, up: Int, down: Int): Float {
        val centerLuma = luma(center)
        val maximumGradient = maxOf(
            abs(centerLuma - luma(left)),
            abs(centerLuma - luma(right)),
            abs(centerLuma - luma(up)),
            abs(centerLuma - luma(down))
        )
        val detailScore = (maximumGradient / 64f).coerceIn(0f, 1f)
        return (1f - detailScore * 0.40f).coerceIn(0.60f, 1f)
    }

    private fun luma(color: Int): Int =
        ((((color ushr 16) and 0xFF) * 3) +
            (((color ushr 8) and 0xFF) * 6) +
            (color and 0xFF)) / 10
}

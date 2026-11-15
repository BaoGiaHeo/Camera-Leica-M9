package com.example.camui

import kotlin.math.abs

/** Camera2 values required for selecting a close-focus physical rear lens. */
internal data class MacroLensSpec(
    val id: String,
    val facing: String,
    val equivalent35Mm: Float?,
    val minimumFocusDiopter: Float?
)

/**
 * Pure macro-candidate selector. Camera2Info normalizes LENS_FACING_BACK as "BACK";
 * keeping selection here makes the vendor metadata rules straightforward to test.
 */
internal object MacroLensSelection {
    fun find(
        logicalMainFocusDiopter: Float?,
        lenses: List<MacroLensSpec>,
        minZoom: Float,
        maxZoom: Float
    ): MacroLensCandidate? {
        val lowerZoom = minZoom.takeIf { it.isFinite() && it > 0f } ?: 1f
        val upperZoom = maxZoom.takeIf { it.isFinite() && it >= lowerZoom } ?: lowerZoom
        val rearLenses = lenses.filter { lens ->
            lens.facing.equals("BACK", ignoreCase = true) &&
                lens.equivalent35Mm?.let { it.isFinite() && it > 0f } == true &&
                lens.minimumFocusDiopter?.let { it.isFinite() && it > 0f } == true
        }
        if (rearLenses.size < 2) return null

        // Use the exposed rear lens closest to a 26mm-equivalent main module as baseline.
        val mainLens = rearLenses.minByOrNull { abs((it.equivalent35Mm ?: 0f) - 26f) }
            ?: return null
        val mainEquivalent = mainLens.equivalent35Mm ?: return null
        val mainCloseFocus = maxOf(
            mainLens.minimumFocusDiopter ?: 0f,
            logicalMainFocusDiopter?.takeIf { it.isFinite() && it > 0f } ?: 0f
        )

        return rearLenses.asSequence()
            .filter { it.id != mainLens.id }
            .mapNotNull { lens ->
                val equivalent = lens.equivalent35Mm ?: return@mapNotNull null
                val closeFocus = lens.minimumFocusDiopter ?: return@mapNotNull null
                val zoomRatio = equivalent / mainEquivalent
                val materiallyCloser = closeFocus >= maxOf(4f, mainCloseFocus * 1.20f)
                val selectable = zoomRatio.isFinite() &&
                    zoomRatio >= lowerZoom && zoomRatio <= upperZoom &&
                    abs(zoomRatio - 1f) >= 0.06f
                if (materiallyCloser && selectable) {
                    MacroLensCandidate(lens.id, zoomRatio, closeFocus)
                } else {
                    null
                }
            }
            .maxByOrNull { it.closestFocusDiopter }
    }
}

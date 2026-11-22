package com.example.camui

internal data class PreviewFpsRange(
    val lower: Int,
    val upper: Int
) {
    init {
        require(lower > 0 && upper >= lower)
    }
}

internal data class PreviewFpsSelection(
    val range: PreviewFpsRange?,
    val targetFps: Int?,
    val fellBack: Boolean,
    val reason: String
)

internal fun selectPreviewFpsRange(
    mode: String,
    supportedRanges: List<PreviewFpsRange>
): PreviewFpsSelection {
    val ranges = supportedRanges.distinct().sortedWith(
        compareBy<PreviewFpsRange> { it.lower }.thenBy { it.upper }
    )
    if (ranges.isEmpty()) {
        return PreviewFpsSelection(null, null, true, "Camera2 reported no AE FPS ranges")
    }

    if (mode.equals("AUTO", ignoreCase = true)) {
        val autoRange = ranges.firstOrNull { it.lower <= 24 && it.upper >= 60 }
            ?: ranges.minByOrNull {
                kotlin.math.abs(it.lower - 24) + kotlin.math.abs(it.upper - 60)
            }
        val spansTarget = autoRange != null && autoRange.lower <= 24 && autoRange.upper >= 60
        return PreviewFpsSelection(
            range = autoRange,
            targetFps = autoRange?.upper,
            fellBack = !spansTarget,
            reason = if (spansTarget) "AE range covers 24..60 FPS" else "Nearest advertised AE range used"
        )
    }

    val requested = mode.toIntOrNull()?.takeIf { it in setOf(24, 30, 60) } ?: 60
    val exactFixed = ranges.firstOrNull { it.lower == requested && it.upper == requested }
    if (exactFixed != null) {
        return PreviewFpsSelection(exactFixed, requested, false, "Exact fixed FPS range")
    }

    // Variable AE ranges can support a requested target even without a fixed range.
    val containingRanges = ranges.filter { it.lower <= requested && it.upper >= requested }
    val containing = containingRanges.minWithOrNull(
        compareBy<PreviewFpsRange> { it.upper - it.lower }
            .thenBy { kotlin.math.abs(it.upper - requested) }
            .thenBy { it.lower }
    )
    if (containing != null) {
        return PreviewFpsSelection(
            range = containing,
            targetFps = requested,
            fellBack = false,
            reason = "Requested FPS is contained in an advertised AE range"
        )
    }

    val cappedRanges = ranges.filter { it.upper <= requested }
    val selected = cappedRanges.maxWithOrNull(
        compareBy<PreviewFpsRange> { it.upper }.thenBy { it.lower }
    ) ?: ranges.minByOrNull {
        kotlin.math.abs(it.upper - requested) + if (it.lower > requested) 10_000 else 0
    }
    val target = selected?.upper
    return PreviewFpsSelection(
        range = selected,
        targetFps = target,
        fellBack = selected == null || target != requested || selected.lower != requested,
        reason = if (selected == null) "No usable AE FPS range"
            else "Requested " + requested + " FPS unavailable; selected advertised range " +
                selected.lower + ".." + selected.upper
    )
}

internal fun supportedPreviewFpsModes(
    supportedRanges: List<PreviewFpsRange>
): Set<String> {
    val ranges = supportedRanges.distinct()
    if (ranges.isEmpty()) return emptySet()
    val result = linkedSetOf<String>()
    for (requested in listOf(60, 30, 24)) {
        if (ranges.any { it.lower <= requested && it.upper >= requested }) {
            result += requested.toString()
        }
    }
    result += "AUTO"
    return result
}

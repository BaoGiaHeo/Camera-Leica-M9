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

/**
 * Only expose fixed FPS modes explicitly advertised by the active Camera2
 * camera and whose preview stream can sustain the requested frame interval.
 * AUTO remains available when the HAL advertises at least one AE range.
 */
internal fun supportedPreviewFpsOptions(
    supportedRanges: List<PreviewFpsRange>,
    previewMinFrameDurationNs: Long?
): Set<String> {
    if (supportedRanges.isEmpty()) return emptySet()
    val exactModes = listOf(60, 30, 24).filter { fps ->
        val exactRange = supportedRanges.any { it.lower == fps && it.upper == fps }
        val duration = previewMinFrameDurationNs ?: 0L
        val frameBudget = 1_000_000_000L / fps
        exactRange && (duration <= 0L || duration <= frameBudget)
    }.map(Int::toString)
    return (exactModes + "AUTO").toSet()
}

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

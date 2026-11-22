package com.example.camui

internal data class PreviewFpsRange(val lower: Int, val upper: Int) {
    init {
        require(lower > 0 && upper >= lower)
    }
}

internal data class PreviewFpsSelection(
    val range: PreviewFpsRange?,
    val requestedFps: Int?,
    val effectiveMaximumFps: Int?,
    val fellBack: Boolean,
    val reason: String
)

internal object PreviewFpsPolicy {
    const val DEFAULT_SETTING = "60"
    private val fixedTargets = setOf(60, 30, 24)

    fun normalizeSetting(value: String?): String =
        value?.takeIf { it == "60" || it == "30" || it == "24" || it == "auto" }
            ?: DEFAULT_SETTING

    fun select(setting: String?, available: List<PreviewFpsRange>): PreviewFpsSelection {
        if (available.isEmpty()) {
            return PreviewFpsSelection(null, target(normalizeSetting(setting)), null, true, "no_camera_fps_ranges")
        }
        val normalized = normalizeSetting(setting)
        val sorted = available.distinct().sortedWith(compareBy<PreviewFpsRange> { it.upper }.thenBy { it.lower })
        if (normalized == "auto") {
            val covering = sorted.filter { it.lower <= 24 && it.upper >= 60 }
                .minByOrNull { (24 - it.lower) + (it.upper - 60) }
            if (covering != null) {
                return PreviewFpsSelection(covering, null, covering.upper, false, "auto_24_60")
            }
            val nearest = sorted.minByOrNull { range ->
                distance(range.lower, 24) + distance(range.upper, 60)
            }!!
            return PreviewFpsSelection(nearest, null, nearest.upper, true, "auto_nearest_available")
        }

        val requested = normalized.toInt()
        val exact = sorted.firstOrNull { it.lower == requested && it.upper == requested }
        if (exact != null) {
            return PreviewFpsSelection(exact, requested, requested, false, "exact_fixed")
        }
        val containing = sorted
            .filter { it.lower <= requested && it.upper >= requested }
            .minByOrNull { it.upper - it.lower }
        if (containing != null) {
            return PreviewFpsSelection(containing, requested, minOf(requested, containing.upper), false, "range_contains_target")
        }
        val below = sorted.filter { it.upper <= requested }.maxByOrNull { it.upper }
        if (below != null) {
            return PreviewFpsSelection(below, requested, below.upper, true, "highest_supported_below_target")
        }
        val lowest = sorted.minByOrNull { it.lower }!!
        return PreviewFpsSelection(lowest, requested, lowest.upper, true, "minimum_available_above_target")
    }

    private fun target(setting: String): Int? =
        if (setting == "auto") null else setting.toIntOrNull()?.takeIf { it in fixedTargets }

    private fun distance(value: Int, target: Int): Int = kotlin.math.abs(value - target)
}

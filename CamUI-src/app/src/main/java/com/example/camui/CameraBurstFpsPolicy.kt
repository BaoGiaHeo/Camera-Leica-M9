package com.example.camui

/**
 * Camera2 may only accept FPS ranges advertised by the active camera HAL.
 * The 8 FPS request is best-effort; measured sensor timestamps remain authoritative.
 */
internal data class CameraBurstFpsRange(val lower: Int, val upper: Int) {
    init {
        require(lower > 0 && upper >= lower)
    }
}

internal object CameraBurstFpsPolicy {
    const val TARGET_FPS = 8
    const val TARGET_FRAME_INTERVAL_MS = 125L

    fun selectEightFpsRange(ranges: List<CameraBurstFpsRange>): CameraBurstFpsRange? {
        val valid = ranges.filter { it.lower > 0 && it.upper >= it.lower }
        return valid.firstOrNull { it.lower == TARGET_FPS && it.upper == TARGET_FPS }
            ?: valid.filter { it.lower <= TARGET_FPS && it.upper >= TARGET_FPS }
                .minWithOrNull(
                    compareBy<CameraBurstFpsRange> { it.upper - it.lower }
                        .thenBy { it.upper }
                        .thenBy { it.lower }
                )
    }
}

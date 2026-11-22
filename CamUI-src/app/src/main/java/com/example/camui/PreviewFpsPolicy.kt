package com.example.camui

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

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

internal fun supportedPreviewFpsModes(
    supportedRanges: List<PreviewFpsRange>,
    streamFrameDurationsNs: List<Long>
): List<String> {
    val ranges = supportedRanges.distinct()
    if (ranges.isEmpty()) return emptyList()
    val knownDurations = streamFrameDurationsNs.filter { it > 0L }

    fun streamCanReach(fps: Int): Boolean {
        if (knownDurations.isEmpty()) return true
        val requiredDuration = 1_000_000_000L / fps
        return knownDurations.any { it <= requiredDuration }
    }

    fun aeCanReach(fps: Int): Boolean = ranges.any { it.lower <= fps && it.upper >= fps }

    return buildList {
        if (aeCanReach(60) && streamCanReach(60)) add("60")
        if (aeCanReach(30) && streamCanReach(30)) add("30")
        if (aeCanReach(24) && streamCanReach(24)) add("24")
        if (ranges.any { it.lower <= 24 && it.upper >= 60 } && streamCanReach(60)) add("AUTO")
    }
}

internal fun highestSupportedPreviewFpsMode(modes: List<String>): String =
    listOf("60", "30", "24", "AUTO").firstOrNull { it in modes } ?: "AUTO"

/** Reads actual rear-camera AE ranges and SurfaceTexture stream durations. */
internal fun discoverSupportedPreviewFpsModes(context: Context): List<String> {
    val manager = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        ?: return emptyList()
    val ranges = mutableListOf<PreviewFpsRange>()
    val durations = mutableListOf<Long>()
    val ids = runCatching { manager.cameraIdList.toList() }.getOrDefault(emptyList())
    ids.forEach { id ->
        runCatching {
            val characteristics = manager.getCameraCharacteristics(id)
            if (characteristics.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK) {
                return@runCatching
            }
            characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                ?.forEach { ranges += PreviewFpsRange(it.lower, it.upper) }
            val streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (streamMap != null) {
                streamMap.getOutputSizes(SurfaceTexture::class.java).orEmpty().forEach { size ->
                    val duration = runCatching {
                        streamMap.getOutputMinFrameDuration(SurfaceTexture::class.java, size)
                    }.getOrDefault(0L)
                    durations += duration
                }
            }
        }
    }
    return supportedPreviewFpsModes(ranges, durations)
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
            ?: return PreviewFpsSelection(
                ranges.maxByOrNull { it.upper }, null, true,
                "Camera2 did not expose a variable range covering 24..60 FPS"
            )
        return PreviewFpsSelection(
            range = autoRange,
            targetFps = null,
            fellBack = false,
            reason = "AE range allows automatic adaptation from 24 to 60 FPS"
        )
    }

    val requested = mode.toIntOrNull()?.takeIf { it in setOf(24, 30, 60) } ?: 60
    val exactFixed = ranges.firstOrNull { it.lower == requested && it.upper == requested }
    if (exactFixed != null) {
        return PreviewFpsSelection(exactFixed, requested, false, "Exact fixed FPS range")
    }

    val containing = ranges.filter { it.lower <= requested && it.upper >= requested }
        .minByOrNull { it.upper - it.lower }
    if (containing != null) {
        return PreviewFpsSelection(
            containing, requested, true,
            "Requested FPS is available inside an advertised variable AE range"
        )
    }

    // Only expose the app's supported fixed targets (60/30/24); do not
    // invent intermediate values such as 48 FPS from a broad AE range.
    val fallbackTarget = listOf(30, 24).firstOrNull { candidate ->
        candidate < requested && ranges.any { it.lower <= candidate && it.upper >= candidate }
    }
    val selected = fallbackTarget?.let { target ->
        ranges.filter { it.lower <= target && it.upper >= target }
            .minByOrNull { it.upper - it.lower }
    }
    return PreviewFpsSelection(
        range = selected,
        targetFps = fallbackTarget,
        fellBack = true,
        reason = if (selected == null || fallbackTarget == null) {
            "No advertised AE range supports a lower fixed target"
        } else {
            "Requested " + requested + " FPS unavailable; selected supported target " +
                fallbackTarget + " FPS using AE range " + selected.lower + ".." + selected.upper
        }
    )
}

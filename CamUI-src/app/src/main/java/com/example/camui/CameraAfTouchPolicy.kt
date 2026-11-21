package com.example.camui

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.min

internal enum class CameraAfRegionStrategy {
    AF_AND_AE_REGIONS,
    AF_REGION_ONLY,
    AE_REGION_ONLY_WITH_SENSOR_AF_TRIGGER,
    SENSOR_AF_TRIGGER_ONLY
}

internal data class CameraAfRegionCapabilities(
    val maxAfRegions: Int,
    val maxAeRegions: Int
) {
    val supportsAfRegions: Boolean get() = maxAfRegions > 0
    val supportsAeRegions: Boolean get() = maxAeRegions > 0

    fun strategy(): CameraAfRegionStrategy = when {
        supportsAfRegions && supportsAeRegions -> CameraAfRegionStrategy.AF_AND_AE_REGIONS
        supportsAfRegions -> CameraAfRegionStrategy.AF_REGION_ONLY
        supportsAeRegions -> CameraAfRegionStrategy.AE_REGION_ONLY_WITH_SENSOR_AF_TRIGGER
        else -> CameraAfRegionStrategy.SENSOR_AF_TRIGGER_ONLY
    }
}

internal fun normalizedAfTapPoint(
    touch: Offset,
    widthPx: Int,
    heightPx: Int
): Offset {
    val width = widthPx.coerceAtLeast(1)
    val height = heightPx.coerceAtLeast(1)
    return Offset(
        (touch.x / width).coerceIn(0f, 1f),
        (touch.y / height).coerceIn(0f, 1f)
    )
}

/**
 * The AF reticle is a square with a side equal to 12% of the LCD height.
 * Hit-testing uses actual preview pixels so portrait/landscape aspect ratios
 * do not distort the tappable area.
 */
internal fun isTapInsideAfReticle(
    tapPoint: Offset,
    currentPoint: Offset,
    previewWidthPx: Int,
    previewHeightPx: Int,
    reticleSideFraction: Float = 0.12f
): Boolean {
    if (previewWidthPx <= 0 || previewHeightPx <= 0) return false
    if (!tapPoint.x.isFinite() || !tapPoint.y.isFinite() ||
        !currentPoint.x.isFinite() || !currentPoint.y.isFinite()
    ) return false
    val side = min(previewWidthPx, previewHeightPx) *
        reticleSideFraction.takeIf { it.isFinite() && it > 0f }!!.coerceAtMost(1f)
    val halfSide = side * 0.5f
    val dxPx = abs(tapPoint.x - currentPoint.x) * previewWidthPx
    val dyPx = abs(tapPoint.y - currentPoint.y) * previewHeightPx
    return dxPx <= halfSide && dyPx <= halfSide
}

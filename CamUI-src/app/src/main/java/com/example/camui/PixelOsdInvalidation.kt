package com.example.camui

import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt

/**
 * Low-resolution OSD dirty rectangles. Coordinates are in logical LCD pixels,
 * not physical screen pixels, so redraw work is restricted to the actual sprite area.
 */
internal data class PixelOsdDirtyRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

internal fun pixelOsdDirtyRegions(
    width: Int,
    height: Int,
    previous: PixelOsdFrame,
    current: PixelOsdFrame
): List<PixelOsdDirtyRect> {
    require(width > 0 && height > 0)
    if (previous.showGrid != current.showGrid ||
        previous.showPixelTexture != current.showPixelTexture
    ) {
        return listOf(PixelOsdDirtyRect(0, 0, width, height))
    }

    val dirty = mutableListOf<PixelOsdDirtyRect>()

    fun add(left: Int, top: Int, right: Int, bottom: Int) {
        val clipped = PixelOsdDirtyRect(
            left.coerceIn(0, width),
            top.coerceIn(0, height),
            right.coerceIn(0, width),
            bottom.coerceIn(0, height)
        )
        if (clipped.right > clipped.left && clipped.bottom > clipped.top) {
            dirty += clipped
        }
    }

    fun addPoint(point: Offset, radius: Int = 2) {
        val x = (point.x.coerceIn(0f, 1f) * width).roundToInt()
        val y = (point.y.coerceIn(0f, 1f) * height).roundToInt()
        add(x - radius, y - radius, x + radius + 1, y + radius + 1)
    }

    fun addPoints(points: List<Offset>) {
        points.forEach { point -> addPoint(point) }
    }

    fun addFaceBounds(center: Offset?, faceSize: Offset?) {
        if (center == null) return
        val halfWidth = faceSize?.x
            ?.let { (it.coerceAtLeast(0f) * width * 0.5f).roundToInt() }
            ?.coerceIn(12, (width / 3).coerceAtLeast(12)) ?: 14
        val halfHeight = faceSize?.y
            ?.let { (it.coerceAtLeast(0f) * height * 0.5f).roundToInt() }
            ?.coerceIn(14, (height / 3).coerceAtLeast(14)) ?: 17
        val x = (center.x.coerceIn(0f, 1f) * width).roundToInt()
        val y = (center.y.coerceIn(0f, 1f) * height).roundToInt()
        // Include eye marker and corner strokes, plus one pixel of clearing margin.
        add(x - halfWidth - 1, y - halfHeight - 1, x + halfWidth + 2, y + halfHeight + 2)
    }

    if (previous.focusPoints != current.focusPoints) {
        addPoints(previous.focusPoints)
        addPoints(current.focusPoints)
    }

    if (previous.showZebra != current.showZebra ||
        (previous.showZebra && current.showZebra && previous.zebraPoints != current.zebraPoints)
    ) {
        if (previous.showZebra) addPoints(previous.zebraPoints)
        if (current.showZebra) addPoints(current.zebraPoints)
    }

    if (previous.faceCenter != current.faceCenter ||
        (current.faceCenter != null && previous.faceFocused != current.faceFocused)
    ) {
        addFaceBounds(previous.faceCenter, previous.faceSize)
        addFaceBounds(current.faceCenter, current.faceSize)
    }

    if (previous.focusLocked != current.focusLocked) {
        add(0, 0, 50, 20)
        add(width - 100, 7, width - 50, 32)
    }

    if (previous.showLevel != current.showLevel ||
        ((previous.showLevel || current.showLevel) &&
            previous.horizonRollDegrees != current.horizonRollDegrees)
    ) {
        val centerX = width / 2
        val centerY = height / 2
        val radius = (width * 0.12f).roundToInt().coerceAtLeast(8) + 8
        add(centerX - radius, centerY - radius, centerX + radius + 1, centerY + radius + 1)
    }

    if (previous.centerAfPoint != current.centerAfPoint ||
        previous.centerAfStatus != current.centerAfStatus ||
        previous.centerAfScale != current.centerAfScale ||
        previous.manualFocusMode != current.manualFocusMode
    ) {
        listOfNotNull(previous.centerAfPoint, current.centerAfPoint).forEach { point ->
            val radius = 19
            val x = (point.x.coerceIn(0f, 1f) * width).roundToInt()
            val y = (point.y.coerceIn(0f, 1f) * height).roundToInt()
            add(x - radius, y - radius, x + radius + 1, y + radius + 1)
        }
    }

    if (previous.faceSize != current.faceSize) {
        addFaceBounds(previous.faceCenter, previous.faceSize)
        addFaceBounds(current.faceCenter, current.faceSize)
    }
    if (previous.eyeCenter != current.eyeCenter) {
        previous.eyeCenter?.let { addPoint(it, radius = 4) }
        current.eyeCenter?.let { addPoint(it, radius = 4) }
    }

    if (previous.showStatus != current.showStatus) {
        add(0, 0, width, 34)
    } else {
        if (previous.focusLocked != current.focusLocked || previous.meteringText != current.meteringText) {
            add(0, 0, 50, 20)
            add(width - 100, 7, width - 50, 32)
        }
        if (previous.batteryPercent != current.batteryPercent ||
            previous.batteryCharging != current.batteryCharging ||
            previous.batteryLowBlinkOn != current.batteryLowBlinkOn
        ) {
            add(width - 58, 0, width, 24)
        }
    }

    if (previous.showEvScale != current.showEvScale ||
        previous.evIndex != current.evIndex ||
        previous.evDisplayIndex != current.evDisplayIndex ||
        previous.evMinimumIndex != current.evMinimumIndex ||
        previous.evMaximumIndex != current.evMaximumIndex ||
        previous.evStepStops != current.evStepStops
    ) {
        add(0, 18, (width / 3).coerceAtLeast(38), height - 16)
        add(width / 2 - 48, 0, width / 2 + 48, 30)
    }

    if (previous.showHistogram != current.showHistogram || previous.histogram != current.histogram) {
        add(0, 12, (width / 4).coerceIn(48, 80) + 12, 44)
    }

    if (previous.isPro != current.isPro ||
        previous.afModeText != current.afModeText ||
        previous.shutterText != current.shutterText ||
        previous.apertureText != current.apertureText ||
        previous.isoText != current.isoText ||
        previous.dynamicRangeText != current.dynamicRangeText ||
        previous.whiteBalanceText != current.whiteBalanceText ||
        previous.filmSimulationText != current.filmSimulationText ||
        previous.manualExposure != current.manualExposure ||
        previous.manualWhiteBalance != current.manualWhiteBalance ||
        previous.manualIso != current.manualIso ||
        previous.manualShutter != current.manualShutter ||
        previous.selectedControl != current.selectedControl ||
        previous.selectedControlProgress != current.selectedControlProgress ||
        previous.showProEv != current.showProEv ||
        previous.showProShutter != current.showProShutter ||
        previous.showProIso != current.showProIso ||
        previous.showProFocus != current.showProFocus ||
        previous.showProWhiteBalance != current.showProWhiteBalance
    ) {
        add(0, height - 16, width, height)
        add(0, 8, width, 36)
    }

    if (previous.showZoomBar != current.showZoomBar ||
        previous.zoomRatio != current.zoomRatio ||
        previous.zoomMinimumRatio != current.zoomMinimumRatio ||
        previous.zoomMaximumRatio != current.zoomMaximumRatio ||
        previous.zoomOpticalBoundaryRatio != current.zoomOpticalBoundaryRatio ||
        previous.zoomDigitalBoundaryRatio != current.zoomDigitalBoundaryRatio ||
        previous.zoomBarPosition != current.zoomBarPosition ||
        previous.zoomBarAlpha != current.zoomBarAlpha
    ) {
        add(0, 0, width, 24)
    }
    return dirty.distinct()
}

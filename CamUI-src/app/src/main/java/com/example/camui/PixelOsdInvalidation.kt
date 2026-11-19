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
    if (previous.showGrid != current.showGrid) {
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
        listOfNotNull(previous.faceCenter, current.faceCenter).forEach { center ->
            val x = (center.x.coerceIn(0f, 1f) * width).roundToInt()
            val y = (center.y.coerceIn(0f, 1f) * height).roundToInt()
            add(x - 19, y - 22, x + 20, y + 23)
        }
    }

    if (previous.focusLocked != current.focusLocked) {
        add(0, 0, 42, 20)
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

    return dirty.distinct()
}

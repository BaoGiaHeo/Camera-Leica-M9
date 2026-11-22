package com.example.camui

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

internal enum class ProOsdTapTarget {
    AREA, WB, FILM, DYNAMIC_RANGE
}

/**
 * Hit testing for the four compact Pro OSD shortcuts.
 *
 * Visual glyphs remain pixel-sized; transparent touch regions are expanded to
 * the requested minimum size. If expanded regions overlap, the nearest visual
 * control centre wins.
 */
internal object ProOsdTapTargets {
    fun resolve(
        xPx: Float,
        yPx: Float,
        widthPx: Int,
        heightPx: Int,
        minimumTargetPx: Float = 48f
    ): ProOsdTapTarget? {
        if (widthPx <= 0 || heightPx <= 0 ||
            !xPx.isFinite() || !yPx.isFinite() ||
            !minimumTargetPx.isFinite() || minimumTargetPx <= 0f
        ) return null

        val unit = (heightPx / 48f).roundToInt().coerceIn(15, 28)
        val t1 = (unit * 1.2f).roundToInt().coerceAtLeast(1)
        val t2 = (unit * 0.95f).roundToInt().coerceAtLeast(1)
        val t3 = (unit * 0.8f).roundToInt().coerceAtLeast(1)
        val margin = (unit * 1.2f).roundToInt()
        val itemGap = (unit * 0.8f).roundToInt()
        val columnWidth = unit * 4.6f
        val rightX = max(widthPx - margin - columnWidth, margin.toFloat())
        val centerX = rightX + columnWidth / 2f
        var y = max(heightPx * 0.18f, (margin + t1 + itemGap).toFloat())

        val areaCenterY = y + unit * 1.2f
        y += unit * 2.5f
        y += t3
        y += unit * 1.35f
        val wbCenterY = y + t1 / 2f
        y += t1 + itemGap
        val filmCenterY = y + t2 / 2f
        y += t2 + itemGap
        val drLabelCenterY = y + t3 / 2f
        y += t3 + 1f
        val drValueCenterY = y + t2 / 2f
        val drCenterY = (drLabelCenterY + drValueCenterY) / 2f

        val candidates = listOf(
            ProOsdTapTarget.AREA to (centerX to areaCenterY),
            ProOsdTapTarget.WB to (centerX to wbCenterY),
            ProOsdTapTarget.FILM to (centerX to filmCenterY),
            ProOsdTapTarget.DYNAMIC_RANGE to (centerX to drCenterY)
        )
        val half = minimumTargetPx / 2f
        return candidates
            .mapNotNull { (target, center) ->
                val dx = abs(xPx - center.first)
                val dy = abs(yPx - center.second)
                if (dx <= half && dy <= half) {
                    target to (dx * dx + dy * dy)
                } else null
            }
            .minByOrNull { it.second }
            ?.first
    }
}

package com.example.camui

import kotlin.math.ceil
import kotlin.math.roundToInt

internal enum class PixelOsdElementKind { TEXT, ICON, RULE }

internal data class PixelOsdLayoutBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun intersects(other: PixelOsdLayoutBounds): Boolean =
        left < other.right && right > other.left && top < other.bottom && bottom > other.top

    fun isInside(width: Int, height: Int, margin: Float): Boolean =
        left >= margin - 0.5f &&
            top >= margin - 0.5f &&
            right <= width - margin + 0.5f &&
            bottom <= height - margin + 0.5f
}

internal data class PixelOsdLayoutElement(
    val id: String,
    val section: String,
    val kind: PixelOsdElementKind,
    val bounds: PixelOsdLayoutBounds,
    val text: String? = null,
    val sprite: String? = null,
    val capHeightPx: Int = 0
)

internal data class PixelOsdLayoutPlan(
    val widthPx: Int,
    val heightPx: Int,
    val pixelScale: Int,
    val unitPx: Int,
    val t1CapHeightPx: Int,
    val t2CapHeightPx: Int,
    val t3CapHeightPx: Int,
    val safeMarginPx: Int,
    val itemGapPx: Int,
    val groupGapPx: Int,
    val elements: List<PixelOsdLayoutElement>,
    val histogramBounds: PixelOsdLayoutBounds?,
    val evLineX: Float,
    val evTop: Float,
    val evBottom: Float,
    val zoomTrackBounds: PixelOsdLayoutBounds?
) {
    fun overlaps(): List<Pair<PixelOsdLayoutElement, PixelOsdLayoutElement>> {
        val visible = elements.filter { it.kind != PixelOsdElementKind.RULE }
        val pairs = ArrayList<Pair<PixelOsdLayoutElement, PixelOsdLayoutElement>>()
        for (i in visible.indices) {
            for (j in i + 1 until visible.size) {
                if (visible[i].bounds.intersects(visible[j].bounds)) {
                    pairs += visible[i] to visible[j]
                }
            }
        }
        return pairs
    }

    fun outOfSafeBounds(): List<PixelOsdLayoutElement> =
        elements.filter { !it.bounds.isInside(widthPx, heightPx, safeMarginPx.toFloat()) }

    fun iconTextHeightRatios(): List<Pair<String, Float>> {
        val icons = elements.filter { it.kind == PixelOsdElementKind.ICON && it.id != "battery" }
        return icons.mapNotNull { icon ->
            val text = elements.firstOrNull {
                it.kind == PixelOsdElementKind.TEXT &&
                    when (icon.id) {
                        "meter" -> it.id == "pro-mode"
                        "focus" -> it.id == "pro-af"
                        "shutter" -> it.id == "pro-shutter-value"
                        else -> false
                    }
            } ?: return@mapNotNull null
            icon.id to (icon.bounds.height / text.bounds.height.coerceAtLeast(1f))
        }
    }

    fun diagnosticLine(): String =
        "LCD=" + widthPx + "x" + heightPx + " PIXEL=" + pixelScale +
            " u=" + unitPx + " T1=" + t1CapHeightPx + " T2=" + t2CapHeightPx +
            " T3=" + t3CapHeightPx + " elements=" + elements.size +
            " overlaps=" + overlaps().size + " outOfBounds=" + outOfSafeBounds().size
}

/**
 * Physical-pixel layout for the Fuji LCD OSD. All adjacent text/icon positions are
 * measured before painting; the renderer consumes these same placements.
 */
internal object PixelOsdLayoutEngine {
    fun create(
        frame: PixelOsdFrame,
        widthPx: Int,
        heightPx: Int,
        pixelScale: Int,
        measureTextWidthPx: (String, Int) -> Float
    ): PixelOsdLayoutPlan {
        require(widthPx > 0 && heightPx > 0)
        val scale = pixelScale.coerceIn(2, 4)
        val unit = (heightPx / 48f).roundToInt().coerceIn(15, 28)
        val t1 = (unit * 1.2f).roundToInt().coerceAtLeast(1)
        val t2 = (unit * 0.95f).roundToInt().coerceAtLeast(1)
        val t3 = (unit * 0.8f).roundToInt().coerceAtLeast(1)
        val margin = (unit * 1.2f).roundToInt()
        val itemGap = (unit * 0.8f).roundToInt()
        val labelValueGap = (unit * 0.3f).roundToInt()
        val groupGap = (unit * 1.6f).roundToInt()
        val elements = ArrayList<PixelOsdLayoutElement>()

        fun snapGrid(value: Float): Float =
            (value / scale).roundToInt().coerceAtLeast(1) * scale.toFloat()

        fun addText(
            id: String,
            section: String,
            value: String,
            left: Float,
            top: Float,
            capHeight: Int
        ): Float {
            if (value.isBlank()) return 0f
            val safeWidth = measureTextWidthPx(value, capHeight).takeIf { it.isFinite() && it > 0f } ?: 0f
            val width = ceil(safeWidth).coerceAtLeast(1f)
            elements += PixelOsdLayoutElement(
                id = id,
                section = section,
                kind = PixelOsdElementKind.TEXT,
                bounds = PixelOsdLayoutBounds(left, top, left + width, top + capHeight),
                text = value,
                capHeightPx = capHeight
            )
            return width
        }

        fun addIcon(
            id: String,
            section: String,
            sprite: String,
            left: Float,
            top: Float,
            requestedHeight: Int,
            requestedWidth: Int? = null
        ): Float {
            val rows = PixelOsdSprites.get(sprite) ?: return 0f
            val height = snapGrid(requestedHeight.toFloat())
            val aspect = rows.first().length.toFloat() / rows.size.coerceAtLeast(1)
            val rawWidth = requestedWidth?.toFloat() ?: (height * aspect)
            val width = snapGrid(rawWidth)
            elements += PixelOsdLayoutElement(
                id = id,
                section = section,
                kind = PixelOsdElementKind.ICON,
                bounds = PixelOsdLayoutBounds(left, top, left + width, top + height),
                sprite = sprite
            )
            return width
        }

        fun addRule(id: String, section: String, x: Float, top: Float, height: Float) {
            elements += PixelOsdLayoutElement(
                id = id,
                section = section,
                kind = PixelOsdElementKind.RULE,
                bounds = PixelOsdLayoutBounds(x, top, x + maxOf(2f, unit * 0.12f), top + height)
            )
        }

        if (frame.showStatus) {
            val afText = if (frame.focusLocked) "AF-L" else frame.afModeText.ifBlank { "AF" }.take(6)
            val afWidth = measureTextWidthPx(afText, t1).coerceAtLeast(1f)
            val batteryLabel = frame.batteryPercent.takeIf { it in 0..100 }?.let { it.toString() + "%" }.orEmpty()
            val batteryTextWidth = if (batteryLabel.isNotBlank()) measureTextWidthPx(batteryLabel, t1) else 0f
            val batteryWidth = snapGrid(unit * 1.9f)
            val batteryHeight = snapGrid(unit.toFloat())
            val total = afWidth +
                if (batteryLabel.isNotBlank()) itemGap + batteryWidth + labelValueGap + batteryTextWidth else 0f
            var x = widthPx - margin - total
            val top = margin.toFloat()
            val afMeasuredWidth = addText("status-af", "status", afText, x, top, t1)
            x += afMeasuredWidth
            if (batteryLabel.isNotBlank()) {
                x += itemGap
                addIcon(
                    id = "battery",
                    section = "status",
                    sprite = "battery",
                    left = x,
                    top = top + (t1 - batteryHeight) / 2f,
                    requestedHeight = batteryHeight.roundToInt(),
                    requestedWidth = batteryWidth.roundToInt()
                )
                x += batteryWidth + labelValueGap
                addText("battery-percent", "status", batteryLabel, x, top, t1)
            }
        }

        if (frame.isPro && frame.showStatus) {
            val rightItems = buildList {
                if (frame.dynamicRangeText.isNotBlank()) add("pro-dr" to frame.dynamicRangeText.take(8))
                if (frame.showProWhiteBalance && frame.whiteBalanceText.isNotBlank()) {
                    add("pro-wb" to frame.whiteBalanceText.take(8))
                }
            }
            val widths = rightItems.map { measureTextWidthPx(it.second, t2).coerceAtLeast(1f) }
            val total = widths.sum() + groupGap * (rightItems.size - 1).coerceAtLeast(0)
            var x = widthPx - margin - total
            val top = margin + t1 + itemGap
            rightItems.forEachIndexed { index, item ->
                val w = addText(item.first, "pro-top", item.second, x, top.toFloat(), t2)
                x += w + if (index < rightItems.lastIndex) groupGap else 0
            }
        }

        val histogram = if (frame.showHistogram && frame.histogram.isNotEmpty()) {
            PixelOsdLayoutBounds(
                margin.toFloat(),
                margin.toFloat(),
                margin + widthPx * 0.14f,
                margin + heightPx * 0.075f
            )
        } else null

        val evTop = heightPx * 0.19f
        val evBottom = heightPx * 0.81f
        val evStep = frame.evStepStops.takeIf { it.isFinite() && it > 0f } ?: (1f / 3f)
        val minStops = (frame.evMinimumIndex * evStep).coerceAtLeast(-3f)
        val maxStops = (frame.evMaximumIndex * evStep).coerceAtMost(3f)
        val span = (maxStops - minStops).coerceAtLeast(1f / 3f)
        val labels = ArrayList<Pair<String, Float>>()
        var tick = kotlin.math.ceil(minStops * 3f).toInt()
        val finalTick = kotlin.math.floor(maxStops * 3f).toInt()
        while (tick <= finalTick) {
            if (tick % 3 == 0) {
                val label = if (tick == 0) "0" else if (tick > 0) "+" + (tick / 3) else (tick / 3).toString()
                val stops = tick / 3f
                val normalized = ((stops - minStops) / span).coerceIn(0f, 1f)
                val y = evBottom - normalized * (evBottom - evTop)
                labels += label to y
            }
            tick++
        }
        val maxEvLabelWidth = labels.maxOfOrNull { measureTextWidthPx(it.first, t2) } ?: 0f
        val evLabelX = margin.toFloat()
        labels.forEachIndexed { index, pair ->
            val y = pair.second - t2 / 2f
            addText("ev-label-" + index, "ev", pair.first, evLabelX, y, t2)
        }
        val evLineX = margin + maxEvLabelWidth + unit * 0.75f

        val zoomTrack = if (frame.showZoomBar) {
            val trackWidth = widthPx * 0.18f
            val trackHeight = unit * 0.9f
            val wText = "W"
            val tText = "T"
            val wWidth = measureTextWidthPx(wText, t3).coerceAtLeast(1f)
            val tWidth = measureTextWidthPx(tText, t3).coerceAtLeast(1f)
            val totalWidth = wWidth + itemGap + trackWidth + itemGap + tWidth
            val startX = (widthPx - totalWidth) / 2f
            val textTop = margin.toFloat()
            addText("zoom-w", "zoom", wText, startX, textTop, t3)
            val trackLeft = startX + wWidth + itemGap
            val trackTop = textTop + (t3 - trackHeight) / 2f
            val bounds = PixelOsdLayoutBounds(
                trackLeft,
                trackTop,
                trackLeft + trackWidth,
                trackTop + trackHeight
            )
            addText("zoom-t", "zoom", tText, trackLeft + trackWidth + itemGap, textTop, t3)
            bounds
        } else null

        if (frame.isPro) {
            data class Part(
                val id: String,
                val text: String? = null,
                val sprite: String? = null,
                val tier: Int = t1,
                val beforeGap: Int = 0
            )
            data class Group(val id: String, val parts: List<Part>)
            val groups = ArrayList<Group>()

            if (frame.showProEv) {
                groups += Group(
                    "mode",
                    listOf(
                        Part("pro-mode", text = "M"),
                        Part(
                            "meter",
                            sprite = when (frame.meteringText.uppercase()) {
                                "M" -> "meter-center"
                                "S" -> "meter-spot"
                                else -> "meter-matrix"
                            },
                            beforeGap = itemGap
                        )
                    )
                )
            }
            if (frame.showProFocus) {
                groups += Group(
                    "focus",
                    listOf(
                        Part("pro-af", text = frame.afModeText.take(6)),
                        Part("focus", sprite = "focus-dot", beforeGap = itemGap)
                    )
                )
            }
            if (frame.showProShutter) {
                groups += Group(
                    "shutter",
                    listOf(
                        Part("shutter", sprite = "shutter"),
                        Part("pro-shutter-label", text = "SS", tier = t3, beforeGap = itemGap),
                        Part("pro-shutter-value", text = frame.shutterText.ifBlank { "AUTO" }, beforeGap = labelValueGap)
                    )
                )
                groups += Group(
                    "aperture",
                    listOf(
                        Part("pro-aperture-label", text = "F", tier = t3),
                        Part("pro-aperture-value", text = frame.apertureText.removePrefix("F").ifBlank { "--" }, beforeGap = labelValueGap)
                    )
                )
            }
            if (frame.showProIso) {
                groups += Group(
                    "iso",
                    listOf(
                        Part("pro-iso-label", text = "ISO", tier = t3),
                        Part("pro-iso-value", text = frame.isoText.removePrefix("ISO").ifBlank { "AUTO" }, beforeGap = labelValueGap)
                    )
                )
            }

            fun partWidth(part: Part, shrink: Float): Float {
                return when {
                    part.text != null -> measureTextWidthPx(
                        part.text,
                        (part.tier * shrink).roundToInt().coerceAtLeast(1)
                    ).coerceAtLeast(1f)
                    part.sprite != null -> {
                        val rows = PixelOsdSprites.get(part.sprite) ?: return 0f
                        val h = snapGrid(t1 * shrink)
                        snapGrid(h * rows.first().length / rows.size.coerceAtLeast(1))
                    }
                    else -> 0f
                }
            }

            fun totalWidth(shrink: Float): Float =
                groups.sumOf { group ->
                    group.parts.sumOf { (partWidth(it, shrink) + it.beforeGap).toDouble() }
                } + groupGap * (groups.size - 1).coerceAtLeast(0)

            val available = (widthPx - margin * 2).coerceAtLeast(1).toFloat()
            val fitScale = if (totalWidth(1f) <= available) 1f
                else (available / totalWidth(1f)).coerceIn(0.85f, 1f)
            var x = margin.toFloat()
            val top = (heightPx - margin - t1 * fitScale).coerceAtLeast(margin.toFloat())
            groups.forEachIndexed { groupIndex, group ->
                group.parts.forEach { part ->
                    x += part.beforeGap
                    if (part.text != null) {
                        val cap = (part.tier * fitScale).roundToInt().coerceAtLeast(1)
                        x += addText(part.id, "pro-bottom", part.text, x, top, cap)
                    } else if (part.sprite != null) {
                        val rows = PixelOsdSprites.get(part.sprite)
                        val desiredHeight = (t1 * fitScale).roundToInt().coerceAtLeast(1)
                        val iconHeight = snapGrid(desiredHeight.toFloat())
                        val iconWidth = if (rows == null) iconHeight else {
                            snapGrid(iconHeight * rows.first().length / rows.size.coerceAtLeast(1))
                        }
                        x += addIcon(
                            id = part.id,
                            section = "pro-bottom",
                            sprite = part.sprite,
                            left = x,
                            top = top + (t1 * fitScale - iconHeight) / 2f,
                            requestedHeight = desiredHeight,
                            requestedWidth = iconWidth.roundToInt()
                        )
                    }
                }
                if (groupIndex < groups.lastIndex) {
                    x += groupGap
                    if (groupIndex == 0) {
                        addRule("pro-separator", "pro-bottom", x - groupGap / 2f, top, t1 * fitScale)
                    }
                }
            }
        }

        return PixelOsdLayoutPlan(
            widthPx = widthPx,
            heightPx = heightPx,
            pixelScale = scale,
            unitPx = unit,
            t1CapHeightPx = t1,
            t2CapHeightPx = t2,
            t3CapHeightPx = t3,
            safeMarginPx = margin,
            itemGapPx = itemGap,
            groupGapPx = groupGap,
            elements = elements,
            histogramBounds = histogram,
            evLineX = evLineX,
            evTop = evTop,
            evBottom = evBottom,
            zoomTrackBounds = zoomTrack
        )
    }
}

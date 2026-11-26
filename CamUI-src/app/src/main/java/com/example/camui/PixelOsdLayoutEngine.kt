package com.example.camui

import kotlin.math.ceil
import kotlin.math.round
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
                        "af-lock" -> it.id == "status-af"
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
        val unit = round(heightPx / 48f).toInt().coerceIn(15, 28)
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
            val resolutionText = frame.resolutionText.ifBlank { "FHD" }.take(4)
            addText("status-resolution", "status", resolutionText, margin + 3f, margin + 2f, t3)
            val fpsText = frame.previewFps
                ?.takeIf { it.isFinite() && it > 0f }
                ?.let { String.format(java.util.Locale.US, "%.2f", it) }
                ?: "--.--"
            addText(
                "status-fps", "status", fpsText,
                margin.toFloat(), (margin + t3 + labelValueGap).toFloat(), t1
            )
            val wbLabel = frame.whiteBalanceText.ifBlank { "AUTO" }.take(8)
            val batteryLabel = if (!frame.isPro) {
                frame.batteryPercent.takeIf { it in 0..100 }?.let { "$it%" }.orEmpty()
            } else ""
            val batteryWidth = if (batteryLabel.isNotBlank()) snapGrid(unit * 1.9f) else 0f
            val batteryTextWidth = if (batteryLabel.isNotBlank()) {
                ceil(measureTextWidthPx(batteryLabel, t1)).coerceAtLeast(1f)
            } else 0f
            val wbWidth = ceil(measureTextWidthPx(wbLabel, t2)).coerceAtLeast(1f)
            val fWidth = ceil(measureTextWidthPx("F", t1)).coerceAtLeast(1f)
            val sizeLabel = frame.outputSizeLabel.ifBlank { "L" }.take(1)
            val sizeWidth = ceil(measureTextWidthPx(sizeLabel, t1)).coerceAtLeast(1f)
            val total = fWidth + sizeWidth + wbWidth + groupGap * 2 +
                if (batteryLabel.isNotBlank()) itemGap + batteryWidth + labelValueGap + batteryTextWidth else 0f
            var x = widthPx - margin - total
            val top = margin.toFloat()
            val fActual = addText("status-focus", "status", "F", x, top, t1)
            x += fActual + groupGap
            val sizeActual = addText("status-size", "status", sizeLabel, x, top, t1)
            x += sizeActual + groupGap
            val wbActual = addText("status-wb", "status", wbLabel, x, top + (t1 - t2) / 2f, t2)
            x += wbActual
            if (batteryLabel.isNotBlank()) {
                x += itemGap
                addIcon(
                    id = "battery", section = "status", sprite = "battery",
                    left = x, top = top + (t1 - unit) / 2f,
                    requestedHeight = unit, requestedWidth = batteryWidth.roundToInt()
                )
                x += batteryWidth + labelValueGap
                addText("battery-percent", "status", batteryLabel, x, top, t1)
            }
        }

        if (frame.isPro && frame.showStatus) {
            // Keep the right LCD column aligned to the same pixel grid as its
            // icon renderer; otherwise the snapped AF-area icon can cross the safe edge.
            val columnWidth = snapGrid(unit * 4.6f)
            val rightX = (widthPx - margin - columnWidth).coerceAtLeast(margin.toFloat())
            var y = maxOf(heightPx * 0.18f, (margin + t1 + itemGap).toFloat())
            val areaHeight = (unit * 2.4f).roundToInt()
            addIcon(
                id = "pro-area-icon", section = "pro-right", sprite = "af-area",
                left = rightX, top = y, requestedHeight = areaHeight,
                requestedWidth = columnWidth.roundToInt()
            )
            y += unit * 2.5f
            val areaLabelWidth = measureTextWidthPx("AREA", t3)
            addText(
                "pro-area", "pro-right", "AREA",
                rightX + (columnWidth - areaLabelWidth) / 2f, y, t3
            )
            y += unit * 1.35f
            val film = frame.filmSimulationText.ifBlank { "ST" }.take(2)
            addText("pro-film", "pro-right", film, rightX + unit * 0.7f, y, t2)
            y += t2 + itemGap
            addText("pro-dr-label", "pro-right", "DR", rightX, y, t3)
            y += t3 + 1f
            val drValue = if (frame.dynamicRangeText.contains("400", ignoreCase = true)) "400" else "100"
            addText("pro-dr-value", "pro-right", drValue, rightX, y, t2)
            y += t2 + itemGap
            if (frame.focusLocked) {
                addIcon(
                    "pro-lock-icon", "pro-right", "lock",
                    rightX, y, (unit * 1.7f).roundToInt()
                )
                addText("pro-lock", "pro-right", "AF-L", rightX + unit * 1.7f, y, t3)
            }
        }
        val histogram = if (frame.showHistogram && frame.histogram.isNotEmpty()) {
            val histogramWidth = widthPx * 0.14f
            val histogramHeight = heightPx * 0.075f
            val bottomClearance = if (frame.isPro) {
                margin + t1 * 1.4f + unit * 1.8f
            } else margin.toFloat()
            val bottom = heightPx.toFloat() - bottomClearance
            PixelOsdLayoutBounds(
                (widthPx - margin - histogramWidth).coerceAtLeast(margin.toFloat()),
                (bottom - histogramHeight).coerceAtLeast(margin.toFloat()),
                widthPx - margin.toFloat(),
                bottom
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

        if (frame.showLevel && kotlin.math.abs(frame.horizonRollDegrees) >= 0.5f) {
            val angle = String.format(java.util.Locale.US, "%.1f°", frame.horizonRollDegrees)
            val width = measureTextWidthPx(angle, t3)
            addText(
                "level-angle", "level", angle,
                (widthPx - width) / 2f,
                (heightPx * 0.5f + unit * 1.7f).coerceAtMost((heightPx - margin - t3).toFloat()),
                t3
            )
        }

        val zoomTrack = if (frame.showZoomBar) {
            val wText = "W"
            val tText = "T"
            val wWidth = measureTextWidthPx(wText, t3).coerceAtLeast(1f)
            val tWidth = measureTextWidthPx(tText, t3).coerceAtLeast(1f)
            if (frame.zoomBarPosition == "middle_left") {
                // The vertical scale is centered at 50% LCD height and sits 2u
                // to the right of the EV scale; W is the minimum (bottom) end.
                val trackWidth = unit * 0.9f
                val trackHeight = heightPx * 0.30f
                val centerY = heightPx * 0.5f
                val trackTop = centerY - trackHeight * 0.5f
                val trackLeft = (evLineX + unit * 2f).coerceAtMost(widthPx - margin - trackWidth)
                    .coerceAtLeast(margin.toFloat())
                val trackBottom = trackTop + trackHeight
                addText(
                    "zoom-t", "zoom", tText,
                    trackLeft + (trackWidth - tWidth) * 0.5f,
                    trackTop - itemGap - t3,
                    t3
                )
                val bounds = PixelOsdLayoutBounds(
                    trackLeft, trackTop, trackLeft + trackWidth, trackBottom
                )
                addText(
                    "zoom-w", "zoom", wText,
                    trackLeft + (trackWidth - wWidth) * 0.5f,
                    trackBottom + itemGap,
                    t3
                )
                bounds
            } else {
                val trackWidth = widthPx * 0.18f
                val trackHeight = unit * 0.9f
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
            }
        } else null

        if (frame.isPro && frame.showProHud) {
            data class Part(
                val id: String,
                val text: String? = null,
                val sprite: String? = null,
                val tier: Int = t1,
                val beforeGap: Int = 0
            )
            data class Group(val id: String, val parts: List<Part>)
            val groups = ArrayList<Group>()

            groups += Group(
                "mode",
                listOf(
                    Part("pro-mode", text = frame.meteringText.ifBlank { "C" }),
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
            if (frame.showProEv) {
                val evStops = frame.evDisplayIndex * frame.evStepStops
                val evValue = String.format(java.util.Locale.US, "%.1f", evStops)
                groups += Group("ev", listOf(
                    Part("pro-ev-label", text = "EV", tier = t3),
                    Part("pro-ev-value", text = evValue, beforeGap = labelValueGap)
                ))
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
            if (frame.showProWhiteBalance) {
                groups += Group("white-balance", listOf(
                    Part("pro-wb-label", text = "WB", tier = t3),
                    Part("pro-wb-value", text = frame.whiteBalanceKelvinText.ifBlank { "AUTO" }, beforeGap = labelValueGap)
                ))
            }

            val proHudOrder = listOf("shutter", "aperture", "focus", "iso", "white-balance", "ev", "mode")
            groups.sortBy { group ->
                proHudOrder.indexOf(group.id).let { if (it < 0) Int.MAX_VALUE else it }
            }

            fun partWidth(part: Part, shrink: Float): Float {
                return when {
                    part.text != null -> ceil(measureTextWidthPx(
                        part.text,
                        (part.tier * shrink).roundToInt().coerceAtLeast(1)
                    )).coerceAtLeast(1f)
                    part.sprite != null -> {
                        val rows = PixelOsdSprites.get(part.sprite) ?: return 0f
                        val h = snapGrid(t1 * shrink)
                        snapGrid(h * rows.first().length / rows.size.coerceAtLeast(1))
                    }
                    else -> 0f
                }
            }

            fun totalWidth(shrink: Float): Float =
                (groups.sumOf { group ->
                    group.parts.sumOf { (partWidth(it, shrink) + it.beforeGap).toDouble() }
                } + groupGap * (groups.size - 1).coerceAtLeast(0)).toFloat()

            val batteryLabel = frame.batteryPercent.takeIf { it in 0..100 }?.let { "$it%" }.orEmpty()
            val proBatteryWidth = if (batteryLabel.isNotBlank()) {
                unit * 1.9f + labelValueGap + measureTextWidthPx(batteryLabel, t3).coerceAtLeast(1f) + itemGap
            } else 0f
            val available = (widthPx - margin * 2 - proBatteryWidth).coerceAtLeast(1f)
            val fitScale = if (totalWidth(1f) <= available) 1f
                else (available / totalWidth(1f)).coerceIn(0.72f, 1f)
            var x = margin.toFloat()
            val panelLift = if (frame.adjustmentPanelVisible) unit * 7f else 0f
            val top = (heightPx - margin - t1 * fitScale - panelLift).coerceAtLeast(margin.toFloat())
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
            if (batteryLabel.isNotBlank()) {
                val batteryWidth = snapGrid(unit * 1.9f)
                val batteryHeight = snapGrid(unit.toFloat())
                val batteryX = widthPx - margin - proBatteryWidth + itemGap
                addIcon(
                    id = "pro-battery", section = "pro-bottom", sprite = "battery",
                    left = batteryX, top = top + (t1 * fitScale - batteryHeight) / 2f,
                    requestedHeight = batteryHeight.roundToInt(),
                    requestedWidth = batteryWidth.roundToInt()
                )
                addText(
                    "pro-battery-percent", "pro-bottom", batteryLabel,
                    batteryX + batteryWidth + labelValueGap, top, t3
                )
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

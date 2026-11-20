package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.os.SystemClock
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

private object PixelOsdPalette {
    const val WHITE = 0xFFF2F2F2.toInt()
    const val GREEN = 0xFF5CFF7A.toInt()
    const val RED = 0xFFFF4A3D.toInt()
    const val AMBER = 0xFFFFB300.toInt()
    const val BLUE = 0xFF3DA5FF.toInt()
    const val SHADOW = 0xCC000000.toInt()
}

internal fun pixelBatteryCellCount(percent: Int): Int =
    if (percent !in 0..100) 0 else ((percent + 24) / 25).coerceIn(0, 4)

internal fun formatPixelEvReadout(evIndex: Float, evStepStops: Float): String {
    val index = evIndex.takeIf { it.isFinite() } ?: 0f
    val step = evStepStops.takeIf { it.isFinite() && it > 0f } ?: (1f / 3f)
    return String.format(Locale.US, "EV %+.1f", index * step)
}

internal object PixelOsdSprites {
    private val sprites = mapOf(
        "memory-card" to arrayOf("WWWWWW", "W....W", "W.WW.W", "W.WW.W", "W....W", "WWWWWW"),
        "battery" to arrayOf("..WWWWWW.", "WW......W", "W........", "WW......W", "..WWWWWW."),
        "battery-fill" to arrayOf("..WWWWWW.", "WWGG....W", "WGG......", "WWGG....W", "..WWWWWW."),
        "flash" to arrayOf("...WW...", "..WWW...", ".WWWWWWW", "...WW...", "..WW....."),
        "face" to arrayOf(".WWWWW.", "W.....W", "W.W.W.W", "W.....W", "W.W.W.W", "W..W..W", ".WWWWW."),
        "eye" to arrayOf(".WWWWW.", "W.....W", "W..W..W", ".WWWWW."),
        "lock" to arrayOf(".WWWWW.", "W.....W", "W.WWW.W", "W.W.W.W", "W.WWW.W", "WWWWWWW"),
        "focus-dot" to arrayOf("..G..", ".GGG.", "GGGGG", ".GGG.", "..G.."),
        "histogram" to arrayOf("......W", "...W..W", ".W.W.WW", "WWWWWWW"),
        "level" to arrayOf("W.......W", ".W.....W.", "..W...W..", "...W.W...", "....G...."),
        "wide" to arrayOf("W.....W", ".W...W.", "..W.W..", "...W..."),
        "tele" to arrayOf("...W...", "..W.W..", ".W...W.", "W.....W"),
        "location" to arrayOf("...W...", "..WWW..", ".WWWWW.", "..WWW..", "...W...", "...W..."),
        "warning" to arrayOf("...A...", "..A.A..", ".A...A.", "A..A..A", "AAAAAAA", "...A..."),
        "bokeh" to arrayOf("..W.W..", ".W...W.", "W.....W", ".W...W.", "..W.W.."),
        "shake" to arrayOf("W..W..W", ".W.W.W.", "..WWW..", "...W...", "..WWW..", ".W.W.W.", "W..W..W"),
        "battery-charge" to arrayOf("..WWWWWW.", "WWBBBB..W", "W..BBB...", "WW..BB..W", "..WWWWWW."),
        "battery-low" to arrayOf("..WWWWWW.", "WWRR...RW", "WRR.....", "WWRR...RW", "..WWWWWW."),
        "flash-off" to arrayOf("W..WW...", ".W.WW...", "..WWWWWWW", "...WW...", "..W......", "W........"),
        "drive" to arrayOf("..WWW..", ".W...W.", "W.....W", "W..W..W", "W.....W", ".W...W.", "..WWW.."),
        "timer" to arrayOf("..WWW..", ".W...W.", "W..W..W", "W.....W", "W....W.", ".W..W..", "..WWW.."),
        "burst" to arrayOf("W.W.W.W", ".W.W.W.", "W.W.W.W", ".W.W.W.", "W.W.W.W"),
        "meter-matrix" to arrayOf("WW.WW.WW", "WW.WW.WW", "........", "WW.WW.WW", "WW.WW.WW"),
        "meter-center" to arrayOf("........", "..WWWW..", "..W..W..", "..WWWW..", "........"),
        "meter-spot" to arrayOf(".........", "....W....", "....W....", ".WWWWW...", "....W....", "....W....", "........."),
        "af-area" to arrayOf("WW.....WW", "W.......W", ".........", ".........", "WW.....WW"),
        "shutter" to arrayOf("..WWW..", ".W...W.", "W..W..W", "W.W.W.W", "W..W..W", ".W...W.", "..WWW.."),
        "ae-lock" to arrayOf(".WWWWW.", "W.....W", "W.WWW.W", "W.W.W.W", "W.WWW.W", "WWWWWWW"),
        "pin" to arrayOf("..WWW..", ".W...W.", "W.....W", "W.....W", ".W...W.", "..WWW..", "...W..."),
        "temperature" to arrayOf("...W...", "...W...", "...W...", "..WWW..", "..WWW..", "...W...", "..WWW.."),
        "steady" to arrayOf("W.....W", ".W...W.", "..W.W..", "...W...", "..W.W..", ".W...W.", "W.....W")
    )

    fun get(name: String): Array<String>? = sprites[name]

    fun entries(): List<Pair<String, Array<String>>> = sprites.entries
        .sortedBy { it.key }
        .map { it.key to it.value }
}

/**
 * A frame contains normalized coordinates from the live camera preview.
 * The renderer rounds every destination to integer logic pixels before drawing.
 */
data class PixelOsdFrame(
    val showGrid: Boolean = false,
    val faceCenter: Offset? = null,
    val faceSize: Offset? = null,
    val eyeCenter: Offset? = null,
    val faceFocused: Boolean = false,
    val focusLocked: Boolean = false,
    val focusPoints: List<Offset> = emptyList(),
    val showLevel: Boolean = false,
    val horizonRollDegrees: Float = 0f,
    val showZebra: Boolean = false,
    val zebraPoints: List<Offset> = emptyList(),
    val centerAfPoint: Offset? = Offset(0.5f, 0.5f),
    val centerAfStatus: Int = 0,
    val centerAfScale: Int = 1,
    val showStatus: Boolean = true,
    val batteryPercent: Int = -1,
    val batteryCharging: Boolean = false,
    val batteryLowBlinkOn: Boolean = true,
    val showEvScale: Boolean = true,
    val evIndex: Int = 0,
    val evDisplayIndex: Float = evIndex.toFloat(),
    val evMinimumIndex: Int = -9,
    val evMaximumIndex: Int = 9,
    val evStepStops: Float = 1f / 3f,
    val evValueText: String = "0.0",
    val showEvOverlay: Boolean = false,
    val showHistogram: Boolean = false,
    val histogram: List<Int> = emptyList(),
    val isPro: Boolean = false,
    val afModeText: String = "AF",
    val meteringText: String = "C",
    val shutterText: String = "AUTO",
    val apertureText: String = "F--",
    val isoText: String = "AUTO",
    val dynamicRangeText: String = "DR100",
    val whiteBalanceText: String = "AUTO",
    val filmSimulationText: String = "",
    val manualExposure: Boolean = false,
    val manualWhiteBalance: Boolean = false,
    val manualIso: Boolean = false,
    val manualShutter: Boolean = false,
    val selectedControl: String = "",
    val showProEv: Boolean = true,
    val showProShutter: Boolean = true,
    val showProIso: Boolean = true,
    val showProFocus: Boolean = true,
    val showProWhiteBalance: Boolean = true,
    val showZoom: Boolean = false,
    val zoomText: String = "",
    val zoomAlpha: Float = 1f,
    val evOverlayAlpha: Float = 1f
)

/**
 * Reusable low-resolution LCD buffer. Its bitmap is allocated only when the LCD
 * dimensions change; unchanged frames reuse the previous pixels.
 */
class PixelOsdRenderer(context: Context) {
    private val appContext = context.applicationContext
    private var bitmap: Bitmap? = null
    private var nativeCanvas: NativeCanvas? = null
    private var logicWidth = 0
    private var logicHeight = 0
    private var pixelScale = 3
    private var lastFrame: PixelOsdFrame? = null
    private var loadedTypeface: Typeface? = null
    private var fallbackTypeface: Typeface? = null
    private val systemFallbackTypeface = FujiTypography.systemMonoTypeface()
    private var lastRenderUptimeMillis = 0L

    private val fillPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val bitmapPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }
    private val textPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        textSize = 8f
        typeface = Typeface.create("monospace", Typeface.NORMAL)
    }
    private val glyphCheckPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
    }
    private val fallbackGlyphCheckPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
    }

    fun render(widthPx: Int, heightPx: Int, frame: PixelOsdFrame, maxRefreshHz: Int = 120) {
        if (widthPx <= 0 || heightPx <= 0) return
        val geometry = PixelOsdGeometry.fromDisplayPixels(widthPx, heightPx)
        val newWidth = geometry.logicWidth
        val newHeight = geometry.logicHeight
        val needsAllocation = bitmap == null || newWidth != logicWidth || newHeight != logicHeight

        if (needsAllocation) {
            bitmap?.recycle()
            logicWidth = newWidth
            logicHeight = newHeight
            pixelScale = geometry.pixelScale
            bitmap = Bitmap.createBitmap(logicWidth, logicHeight, Bitmap.Config.ARGB_8888)
            nativeCanvas = NativeCanvas(bitmap!!)
            lastFrame = null
        }
        val now = SystemClock.uptimeMillis()
        if (!needsAllocation) {
            val refreshLimit = maxRefreshHz.coerceIn(1, 120)
            val minimumIntervalMs = 1000L / refreshLimit
            if (now - lastRenderUptimeMillis < minimumIntervalMs) return
        }
        if (lastFrame == frame) return

        val canvas = nativeCanvas ?: return
        val previous = lastFrame
        if (previous == null) {
            canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            drawFrame(canvas, frame)
        } else {
            val dirtyRegions = pixelOsdDirtyRegions(logicWidth, logicHeight, previous, frame)
            dirtyRegions.forEach { region ->
                val saveCount = canvas.save()
                canvas.clipRect(
                    region.left,
                    region.top,
                    region.right,
                    region.bottom
                )
                canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                drawFrame(canvas, frame)
                canvas.restoreToCount(saveCount)
            }
        }
        lastFrame = frame
        lastRenderUptimeMillis = SystemClock.uptimeMillis()
    }

    fun drawScaled(target: NativeCanvas, widthPx: Int, heightPx: Int) {
        val current = bitmap ?: return
        if (widthPx <= 0 || heightPx <= 0) return
        target.drawBitmap(
            current,
            null,
            Rect(0, 0, widthPx, heightPx),
            bitmapPaint
        )
    }

    fun logicSize(): IntSize = IntSize(logicWidth, logicHeight)

    fun scaleFactor(): Int = pixelScale

    private fun drawFrame(canvas: NativeCanvas, frame: PixelOsdFrame) {
        val width = logicWidth
        val height = logicHeight
        drawPixelTexture(canvas)
        if (frame.showGrid) {
            for (index in 1..2) {
                val x = (width * index / 3f).roundToInt()
                val y = (height * index / 3f).roundToInt()
                drawHLine(canvas, 0, width - 1, y, PixelOsdPalette.WHITE, 1)
                drawVLine(canvas, x, 0, height - 1, PixelOsdPalette.WHITE, 1)
            }
        }

        frame.zebraPoints.takeIf { frame.showZebra }?.forEach { point ->
            val x = (point.x * width).roundToInt()
            val y = (point.y * height).roundToInt()
            drawPixel(canvas, x, y, PixelOsdPalette.AMBER)
        }

        frame.focusPoints.forEach { point ->
            val x = (point.x * width).roundToInt()
            val y = (point.y * height).roundToInt()
            drawPixel(canvas, x, y, PixelOsdPalette.GREEN)
        }

        frame.faceCenter?.let { center ->
            val cx = (center.x * width).roundToInt()
            val cy = (center.y * height).roundToInt()
            val normalizedSize = frame.faceSize
            val halfWidth = normalizedSize?.x?.let { (it * width * 0.5f).roundToInt() }?.coerceIn(12, width / 3)
                ?: 14
            val halfHeight = normalizedSize?.y?.let { (it * height * 0.5f).roundToInt() }?.coerceIn(14, height / 3)
                ?: 17
            val color = if (frame.faceFocused) PixelOsdPalette.GREEN else PixelOsdPalette.WHITE
            drawCornerFrame(canvas, cx - halfWidth, cy - halfHeight, cx + halfWidth, cy + halfHeight, color, 5)
            frame.eyeCenter?.let { eye ->
                drawSprite(
                    canvas,
                    "eye",
                    (eye.x * width).roundToInt() - 3,
                    (eye.y * height).roundToInt() - 2
                )
            } ?: drawSprite(canvas, "eye", cx - 3, cy - 5)
        }

        if (frame.centerAfPoint != null && frame.faceCenter == null) {
            val cx = (frame.centerAfPoint.x * width).roundToInt()
            val cy = (frame.centerAfPoint.y * height).roundToInt()
            val half = when (frame.centerAfScale) {
                0 -> 7
                2 -> 15
                else -> 10
            }
            val color = when (frame.centerAfStatus) {
                1 -> PixelOsdPalette.GREEN
                -1 -> PixelOsdPalette.RED
                else -> PixelOsdPalette.WHITE
            }
            drawCornerFrame(canvas, cx - half, cy - half, cx + half, cy + half, color, 4)
            if (frame.centerAfStatus == 1) drawSprite(canvas, "focus-dot", cx - 2, cy - 2)
        }

        if (frame.focusLocked) {
            drawSprite(canvas, "lock", 7, 7)
        }

        if (frame.showStatus) {
            drawStatusHud(canvas, frame)
        }
        if (frame.showEvScale) {
            drawEvScale(canvas, frame)
        }
        if (frame.showHistogram) {
            drawHistogram(canvas, frame.histogram)
        }
        if (frame.isPro) {
            drawProHud(canvas, frame)
        }
        if (frame.showZoom && frame.zoomText.isNotBlank()) {
            drawPixelText(
                canvas,
                frame.zoomText,
                (width / 2f - frame.zoomText.length * 2.5f).roundToInt(),
                18,
                PixelOsdPalette.BLUE,
                frame.zoomAlpha
            )
        }
        if (frame.showEvOverlay) {
            drawPixelText(
                canvas,
                formatPixelEvReadout(frame.evDisplayIndex, frame.evStepStops),
                (width / 2f - 24).roundToInt().coerceAtLeast(24),
                23,
                PixelOsdPalette.AMBER,
                frame.evOverlayAlpha
            )
        }

        if (frame.showLevel) {
            val radians = Math.toRadians(frame.horizonRollDegrees.toDouble())
            val halfSpan = (width * 0.12f).roundToInt().coerceAtLeast(8)
            val cx = width / 2
            val cy = height / 2
            val dx = cos(radians).toFloat() * halfSpan
            val dy = sin(radians).toFloat() * halfSpan
            drawLine(
                canvas,
                (cx - dx).roundToInt(),
                (cy - dy).roundToInt(),
                (cx + dx).roundToInt(),
                (cy + dy).roundToInt(),
                if (kotlin.math.abs(frame.horizonRollDegrees) < 1.2f) PixelOsdPalette.GREEN else PixelOsdPalette.AMBER
            )
            drawSprite(canvas, "level", cx - 4, cy - 3)
        }
    }

    private fun drawPixelTexture(canvas: NativeCanvas) {
        // Neutral monochrome sub-pixel grid: 2% pixel alpha, no colour tint.
        fillPaint.color = PixelOsdPalette.WHITE
        fillPaint.alpha = 5
        var x = 0
        while (x < logicWidth) {
            canvas.drawRect(x.toFloat(), 0f, x + 1f, logicHeight.toFloat(), fillPaint)
            x += 4
        }
        var y = 0
        while (y < logicHeight) {
            canvas.drawRect(0f, y.toFloat(), logicWidth.toFloat(), y + 1f, fillPaint)
            y += 4
        }
        fillPaint.alpha = 255
        // A narrow neutral reflection along the upper bezel, kept below 3% opacity.
        fillPaint.alpha = 4
        canvas.drawRect(0f, 0f, logicWidth.toFloat(), 2f, fillPaint)
        fillPaint.alpha = 255
    }

    private fun drawStatusHud(canvas: NativeCanvas, frame: PixelOsdFrame) {
        val width = logicWidth
        if (frame.isPro && frame.meteringText.isNotBlank()) {
            drawSprite(
                canvas,
                when (frame.meteringText.uppercase()) {
                    "M" -> "meter-center"
                    "S" -> "meter-spot"
                    else -> "meter-matrix"
                },
                7,
                7
            )
        }
        val afLabel = if (frame.focusLocked) "AF-L" else "AF"
        drawPixelText(
            canvas,
            afLabel,
            width - if (frame.focusLocked) 95 else 65,
            15,
            if (frame.focusLocked) PixelOsdPalette.GREEN else PixelOsdPalette.WHITE
        )
        if (frame.focusLocked) {
            drawPixelText(canvas, "AE-L", width - 95, 24, PixelOsdPalette.GREEN)
        }

        if (frame.batteryPercent in 0..100) {
            val left = width - 49
            val top = 7
            val color = if (frame.batteryPercent < 20 && !frame.batteryCharging && !frame.batteryLowBlinkOn) {
                PixelOsdPalette.RED
            } else {
                PixelOsdPalette.WHITE
            }
            drawHLine(canvas, left, left + 10, top, color, 255)
            drawHLine(canvas, left, left + 10, top + 6, color, 255)
            drawVLine(canvas, left, top, top + 6, color, 255)
            drawVLine(canvas, left + 10, top + 1, top + 5, color, 255)
            drawVLine(canvas, left + 12, top + 2, top + 4, color, 255)
            val cells = pixelBatteryCellCount(frame.batteryPercent)
            if (color != PixelOsdPalette.RED) {
                for (cell in 0 until cells) {
                    val cellX = left + 1 + cell * 2
                    for (y in 1..5) drawPixel(canvas, cellX, top + y, color)
                }
            } else if (frame.batteryLowBlinkOn) {
                for (cell in 0 until 4) {
                    val cellX = left + 1 + cell * 2
                    for (y in 1..5) drawPixel(canvas, cellX, top + y, color)
                }
            }
            if (frame.batteryCharging) drawSprite(canvas, "flash", left + 3, top + 1)
            drawPixelText(canvas, "${frame.batteryPercent}%", width - 34, 15, color)
        }
    }

    private fun drawEvScale(canvas: NativeCanvas, frame: PixelOsdFrame) {
        val width = logicWidth
        val height = logicHeight
        val top = (height * 0.31f).roundToInt().coerceAtLeast(22)
        val bottom = (height * 0.76f).roundToInt().coerceAtMost(height - 22)
        val centerY = (top + bottom) / 2
        val halfRange = ((bottom - top) / 2f).coerceAtLeast(1f)
        val step = frame.evStepStops.takeIf { it.isFinite() && it > 0f } ?: (1f / 3f)
        val minStops = (frame.evMinimumIndex * step).coerceAtLeast(-3f)
        val maxStops = (frame.evMaximumIndex * step).coerceAtMost(3f)
        val span = (maxStops - minStops).coerceAtLeast(0.333f)
        drawVLine(canvas, 12, top, bottom, PixelOsdPalette.WHITE, 180)
        var tick = kotlin.math.ceil(minStops * 3f).toInt()
        val lastTick = kotlin.math.floor(maxStops * 3f).toInt()
        while (tick <= lastTick) {
            val stops = tick / 3f
            val y = (bottom - ((stops - minStops) / span) * (bottom - top)).roundToInt()
            val major = tick % 3 == 0
            val halfWidth = if (major) 4 else 2
            drawHLine(canvas, 12 - halfWidth, 12 + halfWidth, y, PixelOsdPalette.WHITE, if (major) 230 else 125)
            if (major) {
                val label = if (tick == 0) "0" else if (tick > 0) "+${tick / 3}" else "${tick / 3}"
                drawPixelText(canvas, label, 17, y + 3, PixelOsdPalette.WHITE)
            }
            tick++
        }
        val displayIndex = frame.evDisplayIndex.takeIf { it.isFinite() } ?: frame.evIndex.toFloat()
        val currentStops = (displayIndex * step).coerceIn(minStops, maxStops)
        val pointerY = (bottom - ((currentStops - minStops) / span) * (bottom - top)).roundToInt()
        val pointerColor = when {
            frame.manualExposure -> PixelOsdPalette.AMBER
            frame.selectedControl.equals("EV", ignoreCase = true) -> PixelOsdPalette.BLUE
            else -> PixelOsdPalette.WHITE
        }
        drawPixel(canvas, 7, pointerY, pointerColor)
        drawPixel(canvas, 8, pointerY - 1, pointerColor)
        drawPixel(canvas, 8, pointerY + 1, pointerColor)
        drawPixel(canvas, 9, pointerY, pointerColor)
        if (frame.showEvOverlay) drawPixelText(canvas, frame.evValueText, 3, centerY - 4, PixelOsdPalette.AMBER)
    }

    private fun drawHistogram(canvas: NativeCanvas, bins: List<Int>) {
        if (bins.isEmpty()) return
        val width = logicWidth
        val top = 15
        val chartWidth = (width / 4).coerceIn(48, 70)
        val chartHeight = 22
        val left = 6
        val maxValue = bins.maxOrNull()?.coerceAtLeast(1) ?: 1
        val count = minOf(64, bins.size)
        drawHLine(canvas, left - 1, left + chartWidth, top - 1, PixelOsdPalette.WHITE, 185)
        drawHLine(canvas, left - 1, left + chartWidth, top + chartHeight, PixelOsdPalette.WHITE, 185)
        drawVLine(canvas, left - 1, top - 1, top + chartHeight, PixelOsdPalette.WHITE, 185)
        drawVLine(canvas, left + chartWidth, top - 1, top + chartHeight, PixelOsdPalette.WHITE, 185)
        for (quarter in 1..3) {
            val gridY = top + quarter * chartHeight / 4
            drawHLine(canvas, left, left + chartWidth - 1, gridY, PixelOsdPalette.WHITE, 48)
        }
        for (index in 0 until count) {
            val x = left + index * chartWidth / count
            val nextX = left + (index + 1) * chartWidth / count
            val barWidth = (nextX - x).coerceAtLeast(1)
            val barHeight = (bins[index].coerceAtLeast(0).toFloat() / maxValue * chartHeight).roundToInt()
            if (barHeight > 0) {
                for (column in 0 until barWidth) {
                    drawVLine(canvas, x + column, top + chartHeight - barHeight, top + chartHeight - 1,
                        PixelOsdPalette.WHITE, 125)
                }
            }
        }
    }

    private fun drawProHud(canvas: NativeCanvas, frame: PixelOsdFrame) {
        val height = logicHeight
        val bottom = height - 7
        val selected = frame.selectedControl.uppercase()
        var x = 7

        fun put(value: String, key: String, color: Int = PixelOsdPalette.WHITE) {
            if (value.isBlank() || x > logicWidth - 25) return
            val textWidth = value.length * 5
            if (selected == key) {
                drawPixelSelectionArc(canvas, x - 2, bottom - 10, x + textWidth + 1, bottom + 2, PixelOsdPalette.BLUE)
            }
            drawPixelText(canvas, value, x, bottom, color)
            x += textWidth + 5
        }

        val exposureColor = when {
            frame.manualExposure -> PixelOsdPalette.AMBER
            selected == "EV" -> PixelOsdPalette.BLUE
            else -> PixelOsdPalette.WHITE
        }
        val afColor = if (selected == "AF") PixelOsdPalette.BLUE else PixelOsdPalette.WHITE
        val shutterColor = when {
            frame.manualShutter -> PixelOsdPalette.AMBER
            selected == "TV" || selected == "SHUTTER" -> PixelOsdPalette.BLUE
            else -> PixelOsdPalette.WHITE
        }
        val apertureColor = if (selected == "F" || selected == "APERTURE") {
            PixelOsdPalette.BLUE
        } else {
            PixelOsdPalette.WHITE
        }
        val isoColor = when {
            frame.manualIso -> PixelOsdPalette.AMBER
            selected == "ISO" -> PixelOsdPalette.BLUE
            else -> PixelOsdPalette.WHITE
        }

        if (frame.showProEv) put("M", "EV", exposureColor)
        if (frame.showProFocus) {
            if (x <= logicWidth - 36) {
                drawSprite(canvas, "af-area", x, bottom - 5)
                x += 13
            }
            put(frame.afModeText.take(4), "AF", afColor)
        }
        if (frame.showProShutter) {
            if (x <= logicWidth - 34) {
                drawSprite(canvas, "shutter", x, bottom - 7)
                x += 11
            }
            put(frame.shutterText.take(7), "TV", shutterColor)
            put(frame.apertureText.take(5), "", apertureColor)
        }
        if (frame.showProIso) put(frame.isoText.take(6), "ISO", isoColor)

        val dynamicRangeColor = if (selected == "DR") PixelOsdPalette.BLUE else PixelOsdPalette.WHITE
        val whiteBalanceColor = when {
            frame.manualWhiteBalance -> PixelOsdPalette.AMBER
            selected == "WB" -> PixelOsdPalette.BLUE
            else -> PixelOsdPalette.WHITE
        }
        val filmColor = if (selected == "FILM" || selected == "FS") {
            PixelOsdPalette.BLUE
        } else {
            PixelOsdPalette.WHITE
        }
        val rightSegments = buildList {
            add(Triple(frame.dynamicRangeText.take(5), dynamicRangeColor, "DR"))
            if (frame.showProWhiteBalance) {
                add(Triple(frame.whiteBalanceText.take(6), whiteBalanceColor, "WB"))
            }
            add(Triple(frame.filmSimulationText.take(3), filmColor, "FILM"))
        }.filter { it.first.isNotBlank() }
        val rightTextWidth = rightSegments.sumOf { it.first.length * 5 + 3 }
        var rightX = (logicWidth - rightTextWidth - 7).coerceAtLeast(x)
        rightSegments.forEach { (value, color, key) ->
            val textWidth = value.length * 5
            if (selected == key || (key == "FILM" && selected == "FS")) {
                drawPixelSelectionArc(canvas, rightX - 2, 18, rightX + textWidth + 1, 30, PixelOsdPalette.BLUE)
            }
            drawPixelText(canvas, value, rightX, 28, color)
            rightX += textWidth + 3
        }
    }

    /** Rounded pixel brackets mark the currently selected Pro setting value. */
    private fun drawPixelSelectionArc(
        canvas: NativeCanvas,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        color: Int
    ) {
        if (right <= left || bottom <= top) return
        drawHLine(canvas, left + 2, right - 2, top, color, 235)
        drawHLine(canvas, left + 2, right - 2, bottom, color, 235)
        drawVLine(canvas, left, top + 3, bottom - 3, color, 235)
        drawVLine(canvas, right, top + 3, bottom - 3, color, 235)
        drawPixel(canvas, left + 1, top + 1, color)
        drawPixel(canvas, right - 1, top + 1, color)
        drawPixel(canvas, left + 1, bottom - 1, color)
        drawPixel(canvas, right - 1, bottom - 1, color)
    }

    private fun drawCornerFrame(
        canvas: NativeCanvas,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        color: Int,
        length: Int
    ) {
        drawHLine(canvas, left, left + length, top, color)
        drawVLine(canvas, left, top, top + length, color)
        drawHLine(canvas, right - length, right, top, color)
        drawVLine(canvas, right, top, top + length, color)
        drawHLine(canvas, left, left + length, bottom, color)
        drawVLine(canvas, left, bottom - length, bottom, color)
        drawHLine(canvas, right - length, right, bottom, color)
        drawVLine(canvas, right, bottom - length, bottom, color)
    }

    private fun drawSprite(canvas: NativeCanvas, name: String, left: Int, top: Int) {
        val rows = PixelOsdSprites.get(name) ?: return
        rows.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, pixel ->
                val color = when (pixel) {
                    'W' -> PixelOsdPalette.WHITE
                    'G' -> PixelOsdPalette.GREEN
                    'R' -> PixelOsdPalette.RED
                    'A' -> PixelOsdPalette.AMBER
                    'B' -> PixelOsdPalette.BLUE
                    else -> null
                }
                if (color != null) drawPixel(canvas, left + columnIndex, top + rowIndex, color)
            }
        }
    }

    private fun drawPixel(canvas: NativeCanvas, x: Int, y: Int, color: Int) {
        if (x !in 0 until logicWidth || y !in 0 until logicHeight) return
        fillPaint.color = color
        canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, fillPaint)
    }

    private fun drawHLine(canvas: NativeCanvas, x1: Int, x2: Int, y: Int, color: Int, alpha: Int = 110) {
        if (y !in 0 until logicHeight) return
        fillPaint.color = color
        fillPaint.alpha = alpha
        canvas.drawRect(
            x1.coerceAtLeast(0).toFloat(),
            y.toFloat(),
            (x2 + 1).coerceAtMost(logicWidth).toFloat(),
            (y + 1).toFloat(),
            fillPaint
        )
        fillPaint.alpha = 255
    }

    private fun drawVLine(canvas: NativeCanvas, x: Int, y1: Int, y2: Int, color: Int, alpha: Int = 110) {
        if (x !in 0 until logicWidth) return
        fillPaint.color = color
        fillPaint.alpha = alpha
        canvas.drawRect(
            x.toFloat(),
            y1.coerceAtLeast(0).toFloat(),
            (x + 1).toFloat(),
            (y2 + 1).coerceAtMost(logicHeight).toFloat(),
            fillPaint
        )
        fillPaint.alpha = 255
    }

    private fun drawLine(canvas: NativeCanvas, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        strokePaint.color = color
        canvas.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), strokePaint)
    }

    private fun drawPixelText(
        canvas: NativeCanvas,
        text: String,
        x: Int,
        baseline: Int,
        color: Int,
        alpha: Float = 1f
    ) {
        if (loadedTypeface == null) {
            loadedTypeface = runCatching { FujiTypography.androidTypeface(appContext) }
                .getOrElse { Typeface.create("monospace", Typeface.NORMAL) }
        }
        if (fallbackTypeface == null) {
            fallbackTypeface = runCatching { FujiTypography.shareTechTypeface(appContext) }
                .getOrElse { Typeface.create("monospace", Typeface.NORMAL) }
        }
        drawPixelTextPass(canvas, text, x, baseline, PixelOsdPalette.SHADOW, Paint.Style.STROKE, alpha)
        drawPixelTextPass(canvas, text, x, baseline, color, Paint.Style.FILL, alpha)
    }

    private fun drawPixelTextPass(
        canvas: NativeCanvas,
        text: String,
        x: Int,
        baseline: Int,
        color: Int,
        style: Paint.Style,
        alpha: Float
    ) {
        val primary = loadedTypeface ?: Typeface.DEFAULT
        val fallback = fallbackTypeface ?: Typeface.create("monospace", Typeface.NORMAL)
        glyphCheckPaint.typeface = primary
        fallbackGlyphCheckPaint.typeface = fallback
        var cursorX = x.toFloat()
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val count = Character.charCount(codePoint)
            val glyph = text.substring(index, index + count)
            textPaint.typeface = when {
                glyphCheckPaint.hasGlyph(glyph) -> primary
                fallbackGlyphCheckPaint.hasGlyph(glyph) -> fallback
                else -> systemFallbackTypeface
            }
            textPaint.textSize = 8f
            textPaint.style = style
            textPaint.strokeWidth = if (style == Paint.Style.STROKE) 2f else 0f
            textPaint.color = color
            textPaint.alpha = ((color ushr 24) * alpha.coerceIn(0f, 1f)).roundToInt()
            canvas.drawText(text, index, index + count, cursorX, baseline.toFloat(), textPaint)
            cursorX += textPaint.measureText(text, index, index + count)
            textPaint.alpha = 255
            index += count
        }
    }
}

@Composable
fun PixelOsdLayer(
    modifier: Modifier = Modifier,
    frame: PixelOsdFrame,
    maxRefreshHz: Int = 120,
    performanceMode: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val renderer = remember(context.applicationContext) {
        PixelOsdRenderer(context.applicationContext)
    }
    val faceTarget = frame.faceCenter ?: Offset(0.5f, 0.5f)
    val animatedFaceCenter by animateOffsetAsState(
        targetValue = faceTarget,
        animationSpec = tween(durationMillis = if (performanceMode) 80 else 120),
        label = "face box display interpolation"
    )
    val faceSizeTarget = frame.faceSize ?: Offset.Zero
    val animatedFaceSize by animateOffsetAsState(
        targetValue = faceSizeTarget,
        animationSpec = tween(durationMillis = if (performanceMode) 80 else 120),
        label = "face box size interpolation"
    )
    val eyeTarget = frame.eyeCenter ?: Offset.Zero
    val animatedEyeCenter by animateOffsetAsState(
        targetValue = eyeTarget,
        animationSpec = tween(durationMillis = if (performanceMode) 80 else 120),
        label = "eye box display interpolation"
    )
    val afTarget = frame.centerAfPoint ?: Offset(0.5f, 0.5f)
    val animatedAfPoint by animateOffsetAsState(
        targetValue = afTarget,
        animationSpec = tween(durationMillis = if (performanceMode) 80 else 120),
        label = "center AF display interpolation"
    )
    val animatedFrame = frame.copy(
        faceCenter = frame.faceCenter?.let { animatedFaceCenter },
        faceSize = frame.faceSize?.let { animatedFaceSize },
        eyeCenter = frame.eyeCenter?.let { animatedEyeCenter },
        centerAfPoint = frame.centerAfPoint?.let { animatedAfPoint }
    )
    Canvas(modifier) {
        val widthPx = size.width.roundToInt()
        val heightPx = size.height.roundToInt()
        renderer.render(widthPx, heightPx, animatedFrame, maxRefreshHz)
        drawContext.canvas.nativeCanvas.let { native ->
            renderer.drawScaled(native, widthPx, heightPx)
        }
    }
}

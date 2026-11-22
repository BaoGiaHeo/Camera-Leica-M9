package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
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

internal const val PIXEL_OSD_GRID_ALPHA = 4
internal fun formatEvReadout(evStops: Float): String {
    val safeStops = evStops.takeIf { it.isFinite() } ?: 0f
    val roundedStops = (safeStops * 10f).roundToInt() / 10f
    val normalizedStops = if (roundedStops == 0f) 0f else roundedStops
    return String.format(Locale.US, "%+.1f EV", normalizedStops)
}

internal fun formatPixelEvReadout(evDisplayIndex: Float, evStepStops: Float): String {
    val safeIndex = evDisplayIndex.takeIf { it.isFinite() } ?: 0f
    val safeStep = evStepStops.takeIf { it.isFinite() && it > 0f } ?: (1f / 3f)
    return formatEvReadout(safeIndex * safeStep)
}

internal const val PIXEL_OSD_REFLECTION_ALPHA = 3

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

internal fun isPixelBatteryLow(percent: Int): Boolean = percent in 0 until 20

internal fun isPixelBatteryBlinking(percent: Int): Boolean = percent in 0 until 10

internal fun isPixelBatteryCritical(percent: Int): Boolean = percent in 0..5

internal fun isPixelBatteryLowWarning(percent: Int): Boolean = percent in 0..1

internal fun pixelEvAccentColor(
    evIndex: Float,
    evStepStops: Float,
    selected: Boolean = false,
    manualExposure: Boolean = false
): Int {
    if (selected) return 0xFF3DA5FF.toInt()
    if (manualExposure) return 0xFFFFB300.toInt()
    val index = evIndex.takeIf { it.isFinite() } ?: 0f
    val step = evStepStops.takeIf { it.isFinite() && it > 0f } ?: (1f / 3f)
    val amount = kotlin.math.abs(index * step).coerceIn(0f, 1f)
    fun mix(start: Int, end: Int): Int =
        (start + ((end - start) * amount).roundToInt()).coerceIn(0, 255)
    val red = mix(0xF2, 0xFF)
    val green = mix(0xF2, 0xB3)
    val blue = mix(0xF2, 0x00)
    return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}

internal object PixelOsdSprites {
    private val sprites = mapOf(
        "memory-card" to arrayOf("WWWWWW", "W....W", "W.WW.W", "W.WW.W", "W....W", "WWWWWW"),
        "ev-scale" to arrayOf("..W..", "..W..", "WWWWW", "..W..", "..W.."),
        "dynamic-range" to arrayOf("WWWWW", "W...W", "WWWWW", "W....", "WWWWW"),
        "white-balance" to arrayOf("...W...", "..WWW..", "WWWWWWW", "..WWW..", "...W..."),
        "live" to arrayOf("WWWWW", "W...W", "W.W.W", "W...W", "WWWWW"),
        "heat-warning" to arrayOf("..A..", "..A..", "..A..", "..A..", ".....", "..A.."),
        "battery" to arrayOf("..WWWWWW.", "WW......W", "W........", "WW......W", "..WWWWWW."),
        "battery-fill" to arrayOf("..WWWWWW.", "WWGG....W", "WGG......", "WWGG....W", "..WWWWWW."),
        "flash" to arrayOf("...WW....", "..WWW....", ".WWWWWWW.", "...WW....", "..WW....."),
        "face" to arrayOf(".WWWWW.", "W.....W", "W.W.W.W", "W.....W", "W.W.W.W", "W..W..W", ".WWWWW."),
        "eye" to arrayOf(".WWWWW.", "W.....W", "W..W..W", ".WWWWW."),
        "lock" to arrayOf(".WWWWW.", "W.....W", "W.WWW.W", "W.W.W.W", "W.WWW.W", "WWWWWWW"),
        "histogram" to arrayOf("......W", "...W..W", ".W.W.WW", "WWWWWWW"),
        "level" to arrayOf("W.......W", ".W.....W.", "..W...W..", "...W.W...", "....G...."),
        "wide" to arrayOf("W.....W", ".W...W.", "..W.W..", "...W..."),
        "tele" to arrayOf("...W...", "..W.W..", ".W...W.", "W.....W"),
        "location" to arrayOf("...W...", "..WWW..", ".WWWWW.", "..WWW..", "...W...", "...W..."),
        "warning" to arrayOf("...A...", "..A.A..", ".A...A.", "A..A..A", "AAAAAAA", "...A..."),
        "bokeh" to arrayOf("..W.W..", ".W...W.", "W.....W", ".W...W.", "..W.W.."),
        "shake" to arrayOf("W..W..W", ".W.W.W.", "..WWW..", "...W...", "..WWW..", ".W.W.W.", "W..W..W"),
        "battery-charge" to arrayOf("..WWWWWW.", "WWBBBB..W", "W..BBB...", "WW..BB..W", "..WWWWWW."),
        "battery-low" to arrayOf("..WWWWWW.", "WWRR...RW", "WRR......", "WWRR...RW", "..WWWWWW."),
        "flash-off" to arrayOf("W..WW....", ".W.WW....", "..WWWWWWW", "...WW....", "..W......", "W........"),
        "drive" to arrayOf("..WWW..", ".W...W.", "W.....W", "W..W..W", "W.....W", ".W...W.", "..WWW.."),
        "timer" to arrayOf("..WWW..", ".W...W.", "W..W..W", "W.....W", "W....W.", ".W..W..", "..WWW.."),
        "burst" to arrayOf("W.W.W.W", ".W.W.W.", "W.W.W.W", ".W.W.W.", "W.W.W.W"),
        "meter-matrix" to arrayOf("WW.WW.WW", "WW.WW.WW", "........", "WW.WW.WW", "WW.WW.WW"),
        "meter-center" to arrayOf("........", "..WWWW..", "..W..W..", "..WWWW..", "........"),
        "meter-spot" to arrayOf(".........", "....W....", "....W....", ".WWWWW...", "....W....", "....W....", "........."),
        "af-area" to arrayOf("WW.....WW", "W.......W", ".........", ".........", "WW.....WW"),
        "area-finger" to arrayOf(
            "....W...",
            "....W...",
            "....W...",
            "....WW..",
            "WWWWWWW.",
            ".W...W..",
            "..W.W..."
        ),
        "shutter" to arrayOf("..WWW..", ".W...W.", "W..W..W", "W.W.W.W", "W..W..W", ".W...W.", "..WWW.."),
        "ae-lock" to arrayOf(".WWWWW.", "W.....W", "W.WWW.W", "W.W.W.W", "W.WWW.W", "WWWWWWW"),
        "pin" to arrayOf("..WWW..", ".W...W.", "W.....W", "W.....W", ".W...W.", "..WWW..", "...W..."),
        "temperature" to arrayOf("...W...", "...W...", "...W...", "..WWW..", "..WWW..", "...W...", "..WWW.."),
        "steady" to arrayOf("W.....W", ".W...W.", "..W.W..", "...W...", "..W.W..", ".W...W.", "W.....W")
    )

    internal val requiredSpriteNames = setOf(
        "memory-card", "battery", "battery-charge", "battery-low", "flash", "flash-off",
                "face", "eye", "lock", "ae-lock", "af-area", "area-finger", "meter-matrix",
        "meter-center", "meter-spot", "shutter", "ev-scale", "dynamic-range",
        "white-balance", "level", "live", "timer", "burst", "heat-warning"
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
    val showPixelTexture: Boolean = true,
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
    val showManualAfReticle: Boolean = false,
    val centerAfScale: Int = 1,
    val manualFocusMode: Boolean = false,
    val showStatus: Boolean = true,
    val imageSizeText: String = "L",
    val focusStatusText: String = "F",
    val focusStatusCode: Int = 0,
    val videoFormatText: String = "FHD",
    val fpsText: String = "--.--",
    val fpsTarget: Int = 60,
    val fpsFallback: Boolean = false,
    val batteryPercent: Int = -1,
    val batteryCharging: Boolean = false,
    val batteryAlertState: BatteryAlertState = BatteryAlertState(),
    val batteryLowBlinkOn: Boolean = true,
    val showEvScale: Boolean = true,
    val evIndex: Int = 0,
    val evDisplayIndex: Float = evIndex.toFloat(),
    val evMinimumIndex: Int = -9,
    val evMaximumIndex: Int = 9,
    val evStepStops: Float = 1f / 3f,
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
    val selectedControlProgress: Float = 0.5f,
    val showProEv: Boolean = true,
    val showProShutter: Boolean = true,
    val showProIso: Boolean = true,
    val showProFocus: Boolean = true,
    val showProWhiteBalance: Boolean = true,
    val showZoomBar: Boolean = false,
    val zoomRatio: Float = 1f,
    val zoomMinimumRatio: Float = 1f,
    val zoomMaximumRatio: Float = 2f,
    val zoomOpticalBoundaryRatio: Float = 1f,
    val zoomDigitalBoundaryRatio: Float = 2f,
    val zoomBarPosition: String = "top_center",
    val zoomBarAlpha: Float = 1f,
    val pixelScaleMode: String = "fine"
)

/**
 * Reusable low-resolution LCD buffer. Its bitmap is allocated only when the LCD
 * dimensions change; unchanged frames reuse the previous pixels.
 */
class PixelOsdRenderer(context: Context) {
    private val appContext = context.applicationContext
    private var bitmap: Bitmap? = null
    private var nativeCanvas: NativeCanvas? = null
    private var textBitmap: Bitmap? = null
    private var textNativeCanvas: NativeCanvas? = null
    private var textWidthPx = 0
    private var textHeightPx = 0
    private var logicWidth = 0
    private var logicHeight = 0
    private var pixelScale = 2
    private val displayDensity = appContext.resources.displayMetrics.density
    private var lastFrame: PixelOsdFrame? = null
    private var loadedTypeface: Typeface? = null
    private var fallbackTypeface: Typeface? = null
    private var fontMetrics: PixelOsdFontMetrics? = null
    private var currentLayout: PixelOsdLayoutPlan? = null
    private var lastLayoutDiagnostic: String? = null
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
        val geometry = PixelOsdGeometry.fromDisplayPixels(widthPx, heightPx, frame.pixelScaleMode)
        val newWidth = geometry.logicWidth
        val newHeight = geometry.logicHeight
        val needsAllocation = bitmap == null || textBitmap == null ||
            newWidth != logicWidth || newHeight != logicHeight ||
            widthPx != textWidthPx || heightPx != textHeightPx

        if (needsAllocation) {
            bitmap?.recycle()
            textBitmap?.recycle()
            logicWidth = newWidth
            logicHeight = newHeight
            pixelScale = geometry.pixelScale
            textWidthPx = widthPx
            textHeightPx = heightPx
            bitmap = Bitmap.createBitmap(logicWidth, logicHeight, Bitmap.Config.ARGB_8888)
            nativeCanvas = NativeCanvas(bitmap!!)
            textBitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            textNativeCanvas = NativeCanvas(textBitmap!!)
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
        val highResolutionTextCanvas = textNativeCanvas ?: return
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        highResolutionTextCanvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        // Icons and reticles remain crisp nearest-neighbour sprites on the selected pixel grid.
        // Glyphs are routed to a full LCD-resolution overlay by drawPixelText().
        val layout = PixelOsdLayoutEngine.create(
            frame = frame,
            widthPx = widthPx,
            heightPx = heightPx,
            pixelScale = pixelScale
        ) { text, capHeight -> measurePixelTextWidthPx(text, capHeight) }
        currentLayout = layout
        val diagnostic = layout.diagnosticLine()
        if (diagnostic != lastLayoutDiagnostic) {
            android.util.Log.d("FUJI_OSD_LAYOUT", diagnostic)
            lastLayoutDiagnostic = diagnostic
        }
        drawFrame(canvas, frame, layout)
        lastFrame = frame
        lastRenderUptimeMillis = SystemClock.uptimeMillis()
    }

    fun drawScaled(target: NativeCanvas, widthPx: Int, heightPx: Int) {
        val current = bitmap ?: return
        if (widthPx <= 0 || heightPx <= 0) return
        val destination = Rect(0, 0, widthPx, heightPx)
        target.drawBitmap(current, null, destination, bitmapPaint)
        // The type layer already matches the LCD's physical pixel dimensions; never upscale it.
        textBitmap?.let { target.drawBitmap(it, null, destination, null) }
    }

    fun logicSize(): IntSize = IntSize(logicWidth, logicHeight)

    fun scaleFactor(): Int = pixelScale

    /** Snapshot of the full-resolution text plane for native-pixel instrumentation tests. */
    internal fun textLayerSnapshotForTesting(): Bitmap? =
        textBitmap?.takeUnless { it.isRecycled }?.copy(Bitmap.Config.ARGB_8888, false)

    internal fun layoutSnapshotForTesting(): PixelOsdLayoutPlan? = currentLayout
    internal fun fontMetricsSnapshotForTesting(): PixelOsdFontMetrics? = fontMetrics

    private fun drawFrame(canvas: NativeCanvas, frame: PixelOsdFrame, layout: PixelOsdLayoutPlan) {
        val width = logicWidth
        val height = logicHeight
        if (frame.showPixelTexture) drawPixelTexture(canvas)
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
            // Only display the eye marker when Camera2 or ML Kit supplied a real landmark.
            // A separate fine eye reticle is drawn at the selected primary eye only.
            frame.eyeCenter?.let { eye ->
                val eyeX = (eye.x * width).roundToInt()
                val eyeY = (eye.y * height).roundToInt()
                val eyeColor = if (frame.faceFocused) PixelOsdPalette.GREEN else PixelOsdPalette.WHITE
                val eyeHalf = (layout.unitPx * 1.75f / pixelScale).roundToInt().coerceAtLeast(2)
                drawCornerFrame(
                    canvas,
                    eyeX - eyeHalf,
                    eyeY - eyeHalf,
                    eyeX + eyeHalf,
                    eyeY + eyeHalf,
                    eyeColor,
                    (eyeHalf * 0.24f).roundToInt().coerceAtLeast(2)
                )
            }
        }

        val manualAfPoint = frame.centerAfPoint
        if (manualAfPoint != null && shouldDrawCenterAfReticle(
                centerAfPointAvailable = true,
                faceBoxVisible = frame.faceCenter != null,
                manualOverrideActive = frame.showManualAfReticle
            )
        ) {
            val cx = (manualAfPoint.x * width).roundToInt()
            val cy = (manualAfPoint.y * height).roundToInt()
            val half = when (frame.centerAfScale) {
                0 -> (height * 0.04f).roundToInt().coerceAtLeast(2)
                2 -> (height * 0.09f).roundToInt().coerceAtLeast(2)
                else -> (height * 0.06f).roundToInt().coerceAtLeast(2)
            }
            val color = when (frame.centerAfStatus) {
                1 -> PixelOsdPalette.GREEN
                -1 -> PixelOsdPalette.RED
                else -> PixelOsdPalette.WHITE
            }
            if (frame.manualFocusMode) {
                val magnifiedHalf = when (frame.centerAfScale) {
                    0 -> 16
                    2 -> 25
                    else -> 20
                }
                drawCornerFrame(
                    canvas,
                    cx - magnifiedHalf,
                    cy - magnifiedHalf,
                    cx + magnifiedHalf,
                    cy + magnifiedHalf,
                    PixelOsdPalette.BLUE,
                    7
                )
                drawHLine(canvas, cx - 4, cx + 4, cy, PixelOsdPalette.WHITE, 150)
                drawVLine(canvas, cx, cy - 4, cy + 4, PixelOsdPalette.WHITE, 150)
            } else {
                drawCornerFrame(canvas, cx - half, cy - half, cx + half, cy + half, color, 4)
            }
        }

        if (frame.showStatus) drawStatusHud(canvas, frame, layout)
        if (frame.showEvScale) drawEvScale(canvas, frame, layout)
        layout.histogramBounds?.let { drawHistogram(canvas, frame.histogram, it) }
        if (frame.isPro) drawProHud(canvas, frame, layout)
        if (frame.showZoomBar) drawZoomBar(canvas, frame, layout)
        if (frame.showLevel) {
            val radians = Math.toRadians(frame.horizonRollDegrees.toDouble())
            val halfSpan = (width * 0.5f - layout.safeMarginPx).roundToInt().coerceAtLeast(8)
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
        if (frame.batteryAlertState.warning &&
            !frame.batteryCharging && frame.batteryLowBlinkOn
        ) {
            val bannerHeight = (layout.t1CapHeightPx * 2.5f / pixelScale).roundToInt().coerceAtLeast(8)
            val top = (logicHeight - bannerHeight) / 2
            fillPaint.color = android.graphics.Color.BLACK
            fillPaint.alpha = 204
            canvas.drawRect(0f, top.toFloat(), logicWidth.toFloat(), (top + bannerHeight).toFloat(), fillPaint)
            fillPaint.alpha = 255
            val warning = "BATTERY LOW"
            val capHeight = (layout.t1CapHeightPx * 2).coerceAtLeast(1)
            val width = measurePixelTextWidthPx(warning, capHeight)
            drawPixelTextPhysical(
                text = warning,
                leftPx = (textWidthPx - width) * 0.5f,
                topPx = (textHeightPx - capHeight) * 0.5f,
                capHeightPx = capHeight,
                color = PixelOsdPalette.RED,
                alpha = 1f
            )
        }
    }

    private fun drawPixelTexture(canvas: NativeCanvas) {
        // Neutral monochrome sub-pixel grid: 4/255 alpha, no colour tint.
        fillPaint.color = PixelOsdPalette.WHITE
        fillPaint.alpha = PIXEL_OSD_GRID_ALPHA
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
        // A narrow neutral reflection; combined grid/reflection alpha remains below 3%.
        fillPaint.alpha = PIXEL_OSD_REFLECTION_ALPHA
        canvas.drawRect(0f, 0f, logicWidth.toFloat(), 2f, fillPaint)
        fillPaint.alpha = 255
    }

    private fun drawStatusHud(canvas: NativeCanvas, frame: PixelOsdFrame, layout: PixelOsdLayoutPlan) {
        val lowBattery = frame.batteryAlertState.low && !frame.batteryCharging
        val batteryAlertVisible = lowBattery &&
            (!frame.batteryAlertState.blinking || frame.batteryLowBlinkOn)
        layout.elements.filter { it.section == "status" }.forEach { element ->
            when (element.kind) {
                PixelOsdElementKind.TEXT -> {
                    val color = when {
                        element.id == "fps-value" -> {
                            val measured = frame.fpsText.toFloatOrNull()
                            when {
                                measured == null -> PixelOsdPalette.WHITE
                                measured < frame.fpsTarget.coerceAtLeast(1) * 0.5f -> PixelOsdPalette.RED
                                frame.fpsFallback || measured < frame.fpsTarget.coerceAtLeast(1) * 0.9f -> PixelOsdPalette.AMBER
                                else -> PixelOsdPalette.WHITE
                            }
                        }
                        element.id == "battery-percent" && batteryAlertVisible ->
                            PixelOsdPalette.RED
                        element.id == "status-af" && frame.focusLocked -> PixelOsdPalette.GREEN
                        else -> PixelOsdPalette.WHITE
                    }
                    if (element.id == "video-format") {
                        val padding = (layout.unitPx * 0.18f / pixelScale).roundToInt().coerceAtLeast(1)
                        drawDashedRect(
                            canvas,
                            (element.bounds.left / pixelScale).roundToInt() - padding,
                            (element.bounds.top / pixelScale).roundToInt() - padding,
                            (element.bounds.right / pixelScale).roundToInt() + padding,
                            (element.bounds.bottom / pixelScale).roundToInt() + padding,
                            PixelOsdPalette.WHITE
                        )
                    }
                    drawPixelOsdText(element, color)
                }
                PixelOsdElementKind.ICON -> {
                    if (element.id == "battery") {
                        drawBatteryIcon(
                            canvas = canvas,
                            bounds = element.bounds,
                            percent = frame.batteryPercent,
                            color = if (batteryAlertVisible) PixelOsdPalette.RED else PixelOsdPalette.WHITE,
                            unitPx = layout.unitPx,
                            charging = frame.batteryCharging,
                            critical = frame.batteryAlertState.critical && batteryAlertVisible
                        )
                    } else {
                        drawPixelOsdIcon(
                            canvas,
                            element,
                            monochromeColor = if (element.id == "af-lock") PixelOsdPalette.GREEN else PixelOsdPalette.WHITE
                        )
                    }
                }
                PixelOsdElementKind.RULE -> Unit
            }
        }
    }

    private fun drawEvScale(canvas: NativeCanvas, frame: PixelOsdFrame, layout: PixelOsdLayoutPlan) {
        val scale = pixelScale.coerceAtLeast(1)
        val lineX = (layout.evLineX / scale).roundToInt()
        val top = (layout.evTop / scale).roundToInt().coerceIn(0, logicHeight - 1)
        val bottom = (layout.evBottom / scale).roundToInt().coerceIn(top + 1, logicHeight - 1)
        val step = frame.evStepStops.takeIf { it.isFinite() && it > 0f } ?: (1f / 3f)
        val minStops = (frame.evMinimumIndex * step).coerceAtLeast(-5f)
        val maxStops = (frame.evMaximumIndex * step).coerceAtMost(5f)
        val span = (maxStops - minStops).coerceAtLeast(1f / 3f)
        val tickThickness = (layout.unitPx * 0.12f / scale).roundToInt().coerceAtLeast(1)
        for (offset in 0 until tickThickness) {
            drawVLine(canvas, lineX + offset, top, bottom, PixelOsdPalette.WHITE, 180)
        }

        var tick = kotlin.math.ceil(minStops * 3f).toInt()
        val lastTick = kotlin.math.floor(maxStops * 3f).toInt()
        while (tick <= lastTick) {
            val stops = tick / 3f
            val normalized = ((stops - minStops) / span).coerceIn(0f, 1f)
            val y = (bottom - normalized * (bottom - top)).roundToInt()
            val major = tick % 3 == 0
            val halfWidth = ((layout.unitPx * if (major) 0.35f else 0.18f) / scale)
                .roundToInt().coerceAtLeast(1)
            for (offset in 0 until tickThickness) {
                drawHLine(
                    canvas,
                    lineX - halfWidth,
                    lineX + halfWidth,
                    y + offset,
                    PixelOsdPalette.WHITE,
                    if (major) 230 else 125
                )
            }
            tick++
        }

        val displayIndex = frame.evDisplayIndex.takeIf { it.isFinite() } ?: frame.evIndex.toFloat()
        val currentStops = (displayIndex * step).coerceIn(minStops, maxStops)
        val normalized = ((currentStops - minStops) / span).coerceIn(0f, 1f)
        val pointerY = (bottom - normalized * (bottom - top)).roundToInt()
        val pointerColor = pixelEvAccentColor(
            displayIndex,
            step,
            selected = frame.selectedControl.equals("EV", ignoreCase = true),
            manualExposure = frame.manualExposure
        )
        val pointerSize = (layout.unitPx * 0.22f / scale).roundToInt().coerceAtLeast(2)
        for (row in -pointerSize..pointerSize) {
            val half = (pointerSize - kotlin.math.abs(row)).coerceAtLeast(0)
            drawHLine(canvas, lineX + 2, lineX + 2 + half, pointerY + row, pointerColor, 255)
        }

        layout.elements.filter { it.section == "ev" && it.kind == PixelOsdElementKind.TEXT }
            .forEach { drawPixelOsdText(it, PixelOsdPalette.WHITE) }
    }

    private fun drawHistogram(
        canvas: NativeCanvas,
        bins: List<Int>,
        bounds: PixelOsdLayoutBounds
    ) {
        if (bins.isEmpty()) return
        val scale = pixelScale.coerceAtLeast(1)
        val left = (bounds.left / scale).roundToInt()
        val top = (bounds.top / scale).roundToInt()
        val chartWidth = (bounds.width / scale).roundToInt().coerceAtLeast(12)
        val chartHeight = (bounds.height / scale).roundToInt().coerceAtLeast(8)
        val maxValue = bins.maxOrNull()?.coerceAtLeast(1) ?: 1
        val count = minOf(64, bins.size)
        drawHLine(canvas, left, left + chartWidth, top, PixelOsdPalette.WHITE, 185)
        drawHLine(canvas, left, left + chartWidth, top + chartHeight, PixelOsdPalette.WHITE, 185)
        drawVLine(canvas, left, top, top + chartHeight, PixelOsdPalette.WHITE, 185)
        drawVLine(canvas, left + chartWidth, top, top + chartHeight, PixelOsdPalette.WHITE, 185)
        for (quarter in 1..3) {
            val gridY = top + quarter * chartHeight / 4
            drawHLine(canvas, left + 1, left + chartWidth - 1, gridY, PixelOsdPalette.WHITE, 48)
        }
        for (index in 0 until count) {
            val x = left + index * chartWidth / count
            val nextX = left + (index + 1) * chartWidth / count
            val barWidth = (nextX - x).coerceAtLeast(1)
            val barHeight = (
                bins[index].coerceAtLeast(0).toFloat() / maxValue * (chartHeight - 2)
            ).roundToInt()
            if (barHeight > 0) {
                for (column in 0 until barWidth) {
                    drawVLine(
                        canvas,
                        x + column,
                        top + chartHeight - barHeight,
                        top + chartHeight - 1,
                        PixelOsdPalette.WHITE,
                        125
                    )
                }
            }
        }
    }

    private fun drawProHud(canvas: NativeCanvas, frame: PixelOsdFrame, layout: PixelOsdLayoutPlan) {
        val selected = frame.selectedControl.uppercase()
        val elements = layout.elements.filter {
            it.section == "pro-top" || it.section == "pro-bottom"
        }
        elements.forEach { element ->
            if (element.kind == PixelOsdElementKind.TEXT) {
                val key = when (element.id) {
                    "pro-dr" -> "DR"
                    "pro-wb" -> "WB"
                    "pro-mode" -> "EV"
                    "pro-af" -> "AF"
                    "pro-shutter-label", "pro-shutter-value" -> "TV"
                    "pro-aperture-label", "pro-aperture-value" -> "F"
                    "pro-iso-label", "pro-iso-value" -> "ISO"
                    else -> ""
                }
                val selectedThis = selected == key ||
                    (key == "TV" && selected == "SHUTTER") ||
                    (key == "F" && selected == "APERTURE")
                val textColor = when {
                    selectedThis -> PixelOsdPalette.BLUE
                    (key == "EV" && frame.manualExposure) ||
                        (key == "TV" && frame.manualShutter) ||
                        (key == "ISO" && frame.manualIso) ||
                        (key == "WB" && frame.manualWhiteBalance) -> PixelOsdPalette.AMBER
                    else -> PixelOsdPalette.WHITE
                }
                if (selectedThis) {
                    val scale = pixelScale.coerceAtLeast(1)
                    drawPixelSelectionArc(
                        canvas,
                        (element.bounds.left / scale).roundToInt() - 2,
                        (element.bounds.top / scale).roundToInt() - 2,
                        (element.bounds.right / scale).roundToInt() + 1,
                        (element.bounds.bottom / scale).roundToInt() + 2,
                        PixelOsdPalette.BLUE,
                        frame.selectedControlProgress
                    )
                }
                drawPixelOsdText(element, textColor)
            } else if (element.kind == PixelOsdElementKind.ICON) {
                drawPixelOsdIcon(canvas, element)
            } else if (element.kind == PixelOsdElementKind.RULE) {
                val scale = pixelScale.coerceAtLeast(1)
                val x = (element.bounds.left / scale).roundToInt()
                val top = (element.bounds.top / scale).roundToInt()
                val bottom = (element.bounds.bottom / scale).roundToInt()
                val thickness = (element.bounds.width / scale).roundToInt().coerceAtLeast(1)
                for (offset in 0 until thickness) {
                    drawVLine(canvas, x + offset, top, bottom, PixelOsdPalette.WHITE, 160)
                }
            }
        }
    }

    /** Rounded pixel brackets mark the currently selected Pro setting value. */
    private fun drawPixelSelectionArc(
        canvas: NativeCanvas,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        color: Int,
        progress: Float
    ) {
        if (right <= left || bottom <= top) return
        val fraction = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
        val steps = (64 * fraction).roundToInt().coerceIn(0, 64)
        if (steps == 0) return
        val centerX = (left + right) * 0.5f
        val centerY = (top + bottom) * 0.5f
        val radiusX = (right - left) * 0.5f + 2f
        val radiusY = (bottom - top) * 0.5f + 2f
        for (index in 0 until steps) {
            val angle = (-90.0 + index * 360.0 / 64.0) * Math.PI / 180.0
            val x = (centerX + cos(angle).toFloat() * radiusX).roundToInt()
            val y = (centerY + sin(angle).toFloat() * radiusY).roundToInt()
            drawPixel(canvas, x, y, color, 220)
        }
    }

    private fun drawDashedRect(
        canvas: NativeCanvas,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        color: Int
    ) {
        if (right <= left || bottom <= top) return
        var x = left
        while (x <= right) {
            val end = (x + 1).coerceAtMost(right)
            drawHLine(canvas, x, end, top, color, 255)
            drawHLine(canvas, x, end, bottom, color, 255)
            x += 4
        }
        var y = top
        while (y <= bottom) {
            val end = (y + 1).coerceAtMost(bottom)
            drawVLine(canvas, left, y, end, color, 255)
            drawVLine(canvas, right, y, end, color, 255)
            y += 4
        }
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

    private fun drawPixel(
        canvas: NativeCanvas,
        x: Int,
        y: Int,
        color: Int,
        alpha: Int = 255
    ) {
        if (x !in 0 until logicWidth || y !in 0 until logicHeight) return
        fillPaint.color = color
        fillPaint.alpha = alpha.coerceIn(0, 255)
        canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, fillPaint)
        fillPaint.alpha = 255
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

    private fun drawZoomBar(canvas: NativeCanvas, frame: PixelOsdFrame, layout: PixelOsdLayoutPlan) {
        val bounds = layout.zoomTrackBounds ?: return
        val alpha = (frame.zoomBarAlpha.coerceIn(0f, 1f) * 255f).roundToInt()
        if (alpha <= 0) return
        val scale = pixelScale.coerceAtLeast(1)
        val trackLeft = (bounds.left / scale).roundToInt()
        val trackRight = (bounds.right / scale).roundToInt()
        val trackTop = (bounds.top / scale).roundToInt()
        val trackBottom = (bounds.bottom / scale).roundToInt()
        val vertical = frame.zoomBarPosition == "middle_left"
        if ((!vertical && trackRight - trackLeft < 8) || (vertical && trackRight - trackLeft < 4) ||
            trackBottom <= trackTop
        ) return

        val minimum = frame.zoomMinimumRatio
        val maximum = frame.zoomMaximumRatio
        val ratioPosition = logarithmicZoomPosition(frame.zoomRatio, minimum, maximum)
        val horizontalSpan = (trackRight - trackLeft).coerceAtLeast(1)
        val verticalSpan = (trackBottom - trackTop).coerceAtLeast(1)

        val borderThickness = (layout.unitPx * 0.12f / scale).roundToInt().coerceAtLeast(1)
        for (offset in 0 until borderThickness) {
            drawHLine(canvas, trackLeft, trackRight, trackTop + offset, PixelOsdPalette.WHITE, alpha)
            drawHLine(canvas, trackLeft, trackRight, trackBottom - offset, PixelOsdPalette.WHITE, alpha)
            drawVLine(canvas, trackLeft + offset, trackTop, trackBottom, PixelOsdPalette.WHITE, alpha)
            drawVLine(canvas, trackRight - offset, trackTop, trackBottom, PixelOsdPalette.WHITE, alpha)
        }

        if (vertical) {
            val handleHeight = (layout.unitPx * 0.32f / scale).roundToInt().coerceAtLeast(2)
            val usable = (trackBottom - trackTop - handleHeight).coerceAtLeast(0)
            val handleTop = (trackBottom - handleHeight - ratioPosition * usable).roundToInt().coerceIn(
                (trackTop + borderThickness).coerceAtMost(trackBottom - borderThickness),
                (trackBottom - borderThickness - handleHeight + 1)
                    .coerceAtLeast(trackTop + borderThickness)
            )
            for (x in (trackLeft + borderThickness) until (trackRight - borderThickness + 1)) {
                drawVLine(canvas, x, handleTop, handleTop + handleHeight - 1, PixelOsdPalette.WHITE, alpha)
            }
        } else {
            val handleWidth = (layout.unitPx * 0.32f / scale).roundToInt().coerceAtLeast(2)
            val usable = (trackRight - trackLeft - handleWidth).coerceAtLeast(0)
            val handleLeft = (trackLeft + ratioPosition * usable).roundToInt().coerceIn(
                trackLeft + borderThickness,
                (trackRight - borderThickness - handleWidth + 1).coerceAtLeast(trackLeft + borderThickness)
            )
            for (y in (trackTop + borderThickness) until (trackBottom - borderThickness + 1)) {
                drawHLine(canvas, handleLeft, handleLeft + handleWidth - 1, y, PixelOsdPalette.WHITE, alpha)
            }
        }

        layout.elements.filter { it.section == "zoom" && it.kind == PixelOsdElementKind.TEXT }
            .forEach { drawPixelOsdText(it, PixelOsdPalette.WHITE, frame.zoomBarAlpha) }
    }

    private fun ensureTextFonts() {
        if (loadedTypeface == null) {
            loadedTypeface = runCatching { FujiTypography.androidTypeface(appContext) }
                .getOrElse { Typeface.create("monospace", Typeface.NORMAL) }
        }
        if (fallbackTypeface == null) {
            fallbackTypeface = runCatching { FujiTypography.shareTechTypeface(appContext) }
                .getOrElse { Typeface.create("monospace", Typeface.NORMAL) }
        }
        if (fontMetrics == null) {
            fontMetrics = PixelOsdFontMetrics.load(appContext, loadedTypeface ?: Typeface.DEFAULT)
        }
    }

    private fun typefaceForGlyph(glyph: String): Typeface {
        ensureTextFonts()
        val primary = loadedTypeface ?: Typeface.DEFAULT
        val fallback = fallbackTypeface ?: Typeface.create("monospace", Typeface.NORMAL)
        glyphCheckPaint.typeface = primary
        fallbackGlyphCheckPaint.typeface = fallback
        return when {
            glyphCheckPaint.hasGlyph(glyph) -> primary
            fallbackGlyphCheckPaint.hasGlyph(glyph) -> fallback
            else -> systemFallbackTypeface
        }
    }

    private fun measurePixelTextWidthPx(value: String, capHeightPx: Int): Float {
        if (value.isEmpty()) return 0f
        ensureTextFonts()
        textPaint.textSize = (fontMetrics ?: return 0f).textSizeForCapHeight(capHeightPx.toFloat())
        var width = 0f
        var index = 0
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            val count = Character.charCount(codePoint)
            textPaint.typeface = typefaceForGlyph(value.substring(index, index + count))
            width += textPaint.measureText(value, index, index + count)
            index += count
        }
        return width
    }

    private fun measurePixelTextWidthLogic(value: String): Int {
        val cap = (currentLayout?.t1CapHeightPx ?: 18).coerceAtLeast(1)
        return kotlin.math.ceil(measurePixelTextWidthPx(value, cap) / pixelScale.coerceAtLeast(1)).toInt()
    }

    private fun drawPixelOsdText(
        element: PixelOsdLayoutElement,
        color: Int,
        alpha: Float = 1f
    ) {
        val text = element.text ?: return
        drawPixelTextPhysical(
            text = text,
            leftPx = element.bounds.left,
            topPx = element.bounds.top,
            capHeightPx = element.capHeightPx,
            color = color,
            alpha = alpha
        )
    }

    private fun drawPixelOsdIcon(
        canvas: NativeCanvas,
        element: PixelOsdLayoutElement,
        monochromeColor: Int = PixelOsdPalette.WHITE
    ) {
        val sprite = element.sprite ?: return
        drawSpriteScaled(
            canvas = canvas,
            name = sprite,
            leftPx = element.bounds.left,
            topPx = element.bounds.top,
            widthPx = element.bounds.width,
            heightPx = element.bounds.height,
            monochromeColor = monochromeColor
        )
    }

    private fun drawSpriteScaled(
        canvas: NativeCanvas,
        name: String,
        leftPx: Float,
        topPx: Float,
        widthPx: Float,
        heightPx: Float,
        monochromeColor: Int = PixelOsdPalette.WHITE
    ) {
        val rows = PixelOsdSprites.get(name) ?: return
        val destinationHeight = (heightPx / pixelScale).roundToInt().coerceAtLeast(1)
        val destinationWidth = (widthPx / pixelScale).roundToInt().coerceAtLeast(1)
        val left = (leftPx / pixelScale).roundToInt()
        val top = (topPx / pixelScale).roundToInt()
        for (destinationY in 0 until destinationHeight) {
            val y = top + destinationY
            if (y !in 0 until logicHeight) continue
            val sourceY = (destinationY * rows.size / destinationHeight).coerceIn(0, rows.lastIndex)
            val sourceRow = rows[sourceY]
            for (destinationX in 0 until destinationWidth) {
                val x = left + destinationX
                if (x !in 0 until logicWidth) continue
                val sourceX = (destinationX * sourceRow.length / destinationWidth).coerceIn(0, sourceRow.lastIndex)
                val color = when (sourceRow[sourceX]) {
                    'W' -> monochromeColor
                    'G' -> PixelOsdPalette.GREEN
                    'R' -> PixelOsdPalette.RED
                    'A' -> PixelOsdPalette.AMBER
                    'B' -> PixelOsdPalette.BLUE
                    else -> null
                }
                if (color != null) drawPixel(canvas, x, y, color)
            }
        }
    }

    private fun drawBatteryIcon(
        canvas: NativeCanvas,
        bounds: PixelOsdLayoutBounds,
        percent: Int,
        color: Int,
        unitPx: Int,
        charging: Boolean,
        critical: Boolean
    ) {
        val scale = pixelScale.coerceAtLeast(1)
        val left = (bounds.left / scale).roundToInt()
        val top = (bounds.top / scale).roundToInt()
        val width = (bounds.width / scale).roundToInt().coerceAtLeast(8)
        val height = (bounds.height / scale).roundToInt().coerceAtLeast(4)
        val thickness = (unitPx * 0.12f / scale).roundToInt().coerceAtLeast(1)
        for (offset in 0 until thickness) {
            drawHLine(canvas, left, left + width - thickness - 1, top + offset, color, 255)
            drawHLine(canvas, left, left + width - thickness - 1, top + height - 1 - offset, color, 255)
            drawVLine(canvas, left + offset, top, top + height - 1, color, 255)
            drawVLine(canvas, left + width - thickness - 1 + offset, top + 1, top + height - 2, color, 255)
        }
        val cells = pixelBatteryCellCount(percent)
        val innerTop = top + thickness + 1
        val innerBottom = (top + height - thickness - 1).coerceAtLeast(innerTop)
        val innerLeft = left + thickness + 1
        val innerRight = left + width - thickness - 2
        val innerWidth = (innerRight - innerLeft + 1).coerceAtLeast(1)
        val cellGap = 1
        val cellWidth = ((innerWidth - cellGap * 3) / 4).coerceAtLeast(1)
        for (cell in 0 until cells) {
            val cellLeft = innerLeft + cell * (cellWidth + cellGap)
            for (x in cellLeft until (cellLeft + cellWidth).coerceAtMost(innerRight + 1)) {
                for (y in innerTop..innerBottom) {
                    drawPixel(canvas, x, y, color)
                }
            }
        }
        if (charging) {
            val bolt = PixelOsdSprites.get("flash")
            if (bolt != null) {
                drawSprite(
                    canvas,
                    "flash",
                    left + ((width - bolt.first().length) / 2).coerceAtLeast(0),
                    top + ((height - bolt.size) / 2).coerceAtLeast(0)
                )
            }
        } else if (critical && color == PixelOsdPalette.RED) {
            drawLine(canvas, left, top + height - 1, left + width - 1, top, PixelOsdPalette.RED)
        }
    }

    private fun drawPixelTextPhysical(
        text: String,
        leftPx: Float,
        topPx: Float,
        capHeightPx: Int,
        color: Int,
        alpha: Float = 1f
    ) {
        if (text.isEmpty()) return
        ensureTextFonts()
        val target = textNativeCanvas ?: return
        val metrics = fontMetrics ?: return
        val textSizePx = metrics.textSizeForCapHeight(capHeightPx.toFloat())
        val baselinePx = topPx + capHeightPx
        val strokeWidth = maxOf(1.5f, capHeightPx * 0.06f)
        drawPixelTextPass(
            target, text, leftPx, baselinePx, PixelOsdPalette.SHADOW,
            Paint.Style.STROKE, alpha, textSizePx, strokeWidth
        )
        drawPixelTextPass(
            target, text, leftPx, baselinePx, color,
            Paint.Style.FILL, alpha, textSizePx, 0f
        )
    }

    private fun drawPixelText(
        canvas: NativeCanvas,
        text: String,
        x: Int,
        baseline: Int,
        color: Int,
        alpha: Float = 1f
    ) {
        val capHeight = (currentLayout?.t1CapHeightPx ?: 18).coerceAtLeast(1)
        val baselinePx = baseline * pixelScale
        drawPixelTextPhysical(
            text = text,
            leftPx = (x * pixelScale).toFloat(),
            topPx = (baselinePx - capHeight).toFloat(),
            capHeightPx = capHeight,
            color = color,
            alpha = alpha
        )
    }

    private fun drawPixelTextPass(
        canvas: NativeCanvas,
        text: String,
        x: Float,
        baseline: Float,
        color: Int,
        style: Paint.Style,
        alpha: Float,
        textSizePx: Float,
        strokeWidthPx: Float
    ) {
        ensureTextFonts()
        textPaint.textSize = textSizePx
        textPaint.style = style
        textPaint.strokeWidth = strokeWidthPx
        textPaint.color = color
        textPaint.alpha = ((color ushr 24) * alpha.coerceIn(0f, 1f)).roundToInt()
        var cursorX = x
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val count = Character.charCount(codePoint)
            val glyph = text.substring(index, index + count)
            textPaint.typeface = typefaceForGlyph(glyph)
            canvas.drawText(text, index, index + count, cursorX, baseline, textPaint)
            cursorX += textPaint.measureText(text, index, index + count)
            index += count
        }
        textPaint.alpha = 255
        textPaint.style = Paint.Style.FILL
        textPaint.strokeWidth = 0f
    }

}

@Composable
fun PixelOsdLayer(
    modifier: Modifier = Modifier,
    frame: PixelOsdFrame,
    maxRefreshHz: Int = 120,
    performanceMode: Boolean = false,
    reducedMotion: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val renderer = remember(context.applicationContext) {
        PixelOsdRenderer(context.applicationContext)
    }
    val faceTarget = frame.faceCenter ?: Offset(0.5f, 0.5f)
    val animatedFaceCenter by animateOffsetAsState(
        targetValue = faceTarget,
        animationSpec = tween(durationMillis = when {
            reducedMotion -> 0
            performanceMode -> 80
            else -> 120
        }),
        label = "face box display interpolation"
    )
    val animatedSelectedControlProgress by animateFloatAsState(
        targetValue = frame.selectedControlProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = when {
            reducedMotion -> 0
            performanceMode -> 100
            else -> 180
        }),
        label = "selected Pro control arc interpolation"
    )
    val faceSizeTarget = frame.faceSize ?: Offset.Zero
    val animatedFaceSize by animateOffsetAsState(
        targetValue = faceSizeTarget,
        animationSpec = tween(durationMillis = when {
            reducedMotion -> 0
            performanceMode -> 80
            else -> 120
        }),
        label = "face box size interpolation"
    )
    val eyeTarget = frame.eyeCenter ?: Offset.Zero
    val animatedEyeCenter by animateOffsetAsState(
        targetValue = eyeTarget,
        animationSpec = tween(durationMillis = when {
            reducedMotion -> 0
            performanceMode -> 80
            else -> 120
        }),
        label = "eye box display interpolation"
    )
    val afTarget = frame.centerAfPoint ?: Offset(0.5f, 0.5f)
    val animatedAfPoint by animateOffsetAsState(
        targetValue = afTarget,
        animationSpec = tween(durationMillis = when {
            reducedMotion -> 0
            performanceMode -> 80
            else -> 120
        }),
        label = "center AF display interpolation"
    )
    val animatedFrame = frame.copy(
        faceCenter = frame.faceCenter?.let { animatedFaceCenter },
        faceSize = frame.faceSize?.let { animatedFaceSize },
        eyeCenter = frame.eyeCenter?.let { animatedEyeCenter },
        centerAfPoint = frame.centerAfPoint?.let { animatedAfPoint },
        selectedControlProgress = animatedSelectedControlProgress
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

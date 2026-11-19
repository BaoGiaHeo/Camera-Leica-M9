package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.sin

private object PixelOsdPalette {
    const val WHITE = 0xFFF2F2F2.toInt()
    const val GREEN = 0xFF5CFF7A.toInt()
    const val RED = 0xFFFF4A3D.toInt()
    const val AMBER = 0xFFFFB300.toInt()
    const val BLUE = 0xFF3DA5FF.toInt()
    const val SHADOW = 0xCC000000.toInt()
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
    val faceFocused: Boolean = false,
    val focusLocked: Boolean = false,
    val focusPoints: List<Offset> = emptyList(),
    val showLevel: Boolean = false,
    val horizonRollDegrees: Float = 0f,
    val showZebra: Boolean = false,
    val zebraPoints: List<Offset> = emptyList()
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

    fun render(widthPx: Int, heightPx: Int, frame: PixelOsdFrame) {
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
        if (lastFrame == frame) return

        val canvas = nativeCanvas ?: return
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawFrame(canvas, frame)
        lastFrame = frame
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
            val color = if (frame.faceFocused) PixelOsdPalette.GREEN else PixelOsdPalette.WHITE
            drawCornerFrame(canvas, cx - 14, cy - 17, cx + 14, cy + 17, color, 5)
            drawSprite(canvas, "eye", cx - 3, cy - 5)
        }

        if (frame.focusLocked) {
            drawSprite(canvas, "lock", 5, 5)
            drawPixelText(canvas, "AF-L", 14, 12, PixelOsdPalette.GREEN)
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

    private fun drawPixelText(canvas: NativeCanvas, text: String, x: Int, baseline: Int, color: Int) {
        if (loadedTypeface == null) {
            loadedTypeface = runCatching { FujiTypography.androidTypeface(appContext) }
                .getOrElse { Typeface.create("monospace", Typeface.NORMAL) }
        }
        textPaint.typeface = loadedTypeface
        textPaint.textSize = 8f
        textPaint.style = Paint.Style.STROKE
        textPaint.strokeWidth = 2f
        textPaint.color = PixelOsdPalette.SHADOW
        canvas.drawText(text, x.toFloat(), baseline.toFloat(), textPaint)
        textPaint.style = Paint.Style.FILL
        textPaint.strokeWidth = 0f
        textPaint.color = color
        canvas.drawText(text, x.toFloat(), baseline.toFloat(), textPaint)
    }
}

@Composable
fun PixelOsdLayer(
    modifier: Modifier = Modifier,
    frame: PixelOsdFrame
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val renderer = remember(context.applicationContext) {
        PixelOsdRenderer(context.applicationContext)
    }
    Canvas(modifier) {
        val widthPx = size.width.roundToInt()
        val heightPx = size.height.roundToInt()
        renderer.render(widthPx, heightPx, frame)
        drawContext.canvas.nativeCanvas.let { native ->
            renderer.drawScaled(native, widthPx, heightPx)
        }
    }
}

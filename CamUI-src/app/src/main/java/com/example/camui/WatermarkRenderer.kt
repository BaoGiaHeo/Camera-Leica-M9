package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object WatermarkRenderer {
    private const val TAG = "FUJI_WATERMARK_RENDER"
    private const val BLACK = 0xFF0A0A0A.toInt()
    private const val WHITE = 0xFFF2F2F2.toInt()
    private const val GREY = 0xFF8E8E8E.toInt()

    private data class Fonts(
        val regular: Typeface,
        val medium: Typeface,
        val semibold: Typeface,
        val regularCapRatio: Float,
        val mediumCapRatio: Float,
        val semiboldCapRatio: Float
    )
    private data class Fragment(val text: String, val typeface: Typeface, val color: Int, val capRatio: Float)

    private fun loadFonts(context: Context): Fonts {
        val regular = context.resources.getFont(R.font.saira_semicondensed_regular)
        val medium = context.resources.getFont(R.font.saira_semicondensed_medium)
        val semibold = context.resources.getFont(R.font.saira_semicondensed_semibold)
        return Fonts(
            regular, medium, semibold,
            readCapRatio(context, R.font.saira_semicondensed_regular, regular),
            readCapRatio(context, R.font.saira_semicondensed_medium, medium),
            readCapRatio(context, R.font.saira_semicondensed_semibold, semibold)
        )
    }

    private fun readCapRatio(context: Context, fontId: Int, typeface: Typeface): Float {
        val parsed = runCatching {
            val bytes = context.resources.openRawResource(fontId).use { it.readBytes() }
            parseCapRatio(bytes)
        }.getOrNull()
        if (parsed != null) return parsed
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 100f
        }
        val bounds = Rect()
        paint.getTextBounds("H", 0, 1, bounds)
        Log.w(TAG, "SAIRA_OS2_CAPHEIGHT_FALLBACK fontId=" + fontId + " capAt100=" + bounds.height())
        return (bounds.height() / 100f).coerceIn(0.45f, 0.9f)
    }

    private fun parseCapRatio(bytes: ByteArray): Float? {
        if (bytes.size < 12) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val count = buffer.getShort(4).toInt() and 0xFFFF
        var os2 = -1
        var head = -1
        var os2Length = 0
        for (index in 0 until count) {
            val pos = 12 + index * 16
            if (pos + 16 > bytes.size) return null
            val tag = String(bytes, pos, 4, Charsets.US_ASCII)
            val offset = buffer.getInt(pos + 8).toLong() and 0xFFFFFFFFL
            val length = buffer.getInt(pos + 12).toLong() and 0xFFFFFFFFL
            if (offset > Int.MAX_VALUE || length > Int.MAX_VALUE) continue
            when (tag) {
                "OS/2" -> { os2 = offset.toInt(); os2Length = length.toInt() }
                "head" -> head = offset.toInt()
            }
        }
        if (os2 < 0 || head < 0 || os2Length < 90 || os2 + 90 > bytes.size || head + 20 > bytes.size) return null
        val unitsPerEm = buffer.getShort(head + 18).toInt() and 0xFFFF
        val version = buffer.getShort(os2).toInt() and 0xFFFF
        if (version < 2 || unitsPerEm <= 0) return null
        val capHeight = buffer.getShort(os2 + 88).toInt()
        if (capHeight <= 0 || capHeight >= unitsPerEm) return null
        return capHeight.toFloat() / unitsPerEm
    }

    fun outputDimensions(
        sourceWidth: Int,
        sourceHeight: Int,
        settings: WatermarkLayoutSettings,
        metadata: WatermarkPhotoMetadata
    ): Pair<Int, Int> {
        val geometry = WatermarkGeometry.build(sourceWidth, sourceHeight, settings, metadata)
        return geometry.canvasWidth to geometry.canvasHeight
    }

    /** Shared by the preview and final photo. The photograph itself is never resized. */
    fun render(
        context: Context,
        source: Bitmap,
        settings: WatermarkLayoutSettings,
        metadata: WatermarkPhotoMetadata
    ): Bitmap {
        require(!source.isRecycled && source.width > 0 && source.height > 0)
        val geometry = WatermarkGeometry.build(source.width, source.height, settings, metadata)
        val output = Bitmap.createBitmap(geometry.canvasWidth, geometry.canvasHeight, Bitmap.Config.ARGB_8888)
        try {
            draw(context, Canvas(output), source, geometry, settings, metadata)
            Log.i(
                TAG,
                "RENDER layout=" + settings.layout.id +
                    " source=" + source.width + "x" + source.height +
                    " output=" + output.width + "x" + output.height +
                    " S=" + geometry.shortSide +
                    " sizeStep=" + settings.sizeStep +
                    " lines=" + geometry.visibleTextLines
            )
            return output
        } catch (failure: Throwable) {
            output.recycle()
            throw failure
        }
    }

    /** Preview and export use the same layout/drawing functions; only preview source pixels are reduced. */
    fun renderPreview(
        context: Context,
        source: Bitmap,
        settings: WatermarkLayoutSettings,
        metadata: WatermarkPhotoMetadata,
        maximumLongSide: Int = 1600
    ): Bitmap {
        val factor = min(1f, maximumLongSide.toFloat() / max(source.width, source.height))
        val previewSource = if (factor < 0.999f) {
            Bitmap.createScaledBitmap(
                source,
                (source.width * factor).roundToInt().coerceAtLeast(1),
                (source.height * factor).roundToInt().coerceAtLeast(1),
                true
            )
        } else source
        return try {
            render(context, previewSource, settings, metadata)
        } finally {
            if (previewSource !== source && !previewSource.isRecycled) previewSource.recycle()
        }
    }

    private fun draw(
        context: Context,
        canvas: Canvas,
        source: Bitmap,
        geometry: WatermarkLayoutGeometry,
        settings: WatermarkLayoutSettings,
        metadata: WatermarkPhotoMetadata
    ) {
        val fonts = loadFonts(context)
        val isDark = settings.invert || settings.layout == WatermarkLayoutType.BLACK
        val primary = if (isDark) WHITE else BLACK
        val secondary = if (isDark) 0xFF9A9A9A.toInt() else GREY

        if (geometry.layout == WatermarkLayoutType.CARD) drawCardBackground(canvas, source, geometry, isDark)
        else canvas.drawColor(if (isDark) Color.BLACK else Color.WHITE)

        drawSourceAtNativeSize(canvas, source, geometry.photoRect)
        when (geometry.layout) {
            WatermarkLayoutType.BAR -> drawBar(context, canvas, geometry, settings, metadata, fonts, primary, secondary)
            WatermarkLayoutType.CAPTION -> drawCaption(canvas, geometry, settings, metadata, fonts, primary, secondary)
            WatermarkLayoutType.CARD -> drawCard(context, canvas, geometry, settings, metadata, fonts)
            WatermarkLayoutType.GALLERY -> drawGallery(context, canvas, geometry, settings, metadata, fonts)
            WatermarkLayoutType.BLACK -> drawBlack(context, canvas, geometry, settings, metadata, fonts)
        }
    }

    private fun drawSourceAtNativeSize(canvas: Canvas, source: Bitmap, rect: WatermarkRect) {
        require(rect.width.roundToInt() == source.width && rect.height.roundToInt() == source.height) {
            "Watermark photo rectangle must preserve source pixel dimensions"
        }
        canvas.drawBitmap(
            source,
            null,
            RectF(rect.left, rect.top, rect.right, rect.bottom),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
    }

    private fun drawCardBackground(canvas: Canvas, source: Bitmap, geometry: WatermarkLayoutGeometry, isDark: Boolean) {
        canvas.drawColor(if (isDark) Color.BLACK else Color.WHITE)
        val smallWidth = (geometry.canvasWidth / 18).coerceAtLeast(32)
        val smallHeight = (geometry.canvasHeight / 18).coerceAtLeast(32)
        val small = Bitmap.createScaledBitmap(source, smallWidth, smallHeight, true)
        try {
            val matrix = ColorMatrix().apply {
                setSaturation(1.15f)
                postConcat(ColorMatrix(floatArrayOf(
                    0.92f, 0f, 0f, 0f, 0f,
                    0f, 0.92f, 0f, 0f, 0f,
                    0f, 0f, 0.92f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(matrix)
            }
            canvas.drawBitmap(
                small, null,
                RectF(0f, 0f, geometry.canvasWidth.toFloat(), geometry.canvasHeight.toFloat()),
                paint
            )
            canvas.drawRect(
                0f, 0f, geometry.canvasWidth.toFloat(), geometry.canvasHeight.toFloat(),
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (isDark) 0x99000000.toInt() else 0x14FFFFFF }
            )
        } finally {
            if (!small.isRecycled) small.recycle()
        }
        geometry.cardRect?.let { card ->
            canvas.drawRoundRect(
                RectF(card.left, card.top, card.right, card.bottom),
                geometry.shortSide * 0.012f,
                geometry.shortSide * 0.012f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (isDark) Color.BLACK else Color.WHITE }
            )
        }
    }

    private fun drawLogo(context: Context, canvas: Canvas, rect: WatermarkRect, white: Boolean) {
        val logo = ContextCompat.getDrawable(
            context,
            if (white) R.drawable.fujifilm_logo_white else R.drawable.fujifilm_logo
        ) ?: return
        logo.setBounds(rect.left.roundToInt(), rect.top.roundToInt(), rect.right.roundToInt(), rect.bottom.roundToInt())
        logo.draw(canvas)
    }

    private fun fitPaint(
        text: String,
        typeface: Typeface,
        color: Int,
        capHeightPx: Float,
        maxWidth: Float,
        capRatio: Float,
        align: Paint.Align = Paint.Align.LEFT
    ): Paint? {
        if (text.isBlank() || maxWidth <= 0f) return null
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            this.color = color
            textAlign = align
            textSize = (capHeightPx / capRatio.coerceIn(0.4f, 0.9f)).coerceAtLeast(1f)
        }
        val minimum = paint.textSize * 0.70f
        while (paint.measureText(text) > maxWidth && paint.textSize > minimum + 0.25f) {
            paint.textSize = (paint.textSize * 0.97f).coerceAtLeast(minimum)
        }
        return paint.takeIf { it.measureText(text) <= maxWidth }
    }

    private fun fitTwoLineBadge(
        text: String,
        typeface: Typeface,
        maxWidth: Float,
        capHeightPx: Float,
        capRatio: Float
    ): List<String> {
        val words = text.replace('\n', ' ').trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            textSize = (capHeightPx / capRatio.coerceIn(0.4f, 0.9f)).coerceAtLeast(1f)
        }
        val lines = mutableListOf<String>()
        var truncated = false
        for (word in words) {
            if (lines.isEmpty()) {
                lines.add(word)
                continue
            }
            val candidate = lines.last() + " " + word
            if (paint.measureText(candidate) <= maxWidth) {
                lines[lines.lastIndex] = candidate
            } else if (lines.size < 2) {
                lines.add(word)
            } else {
                truncated = true
                break
            }
        }
        if (truncated && lines.isNotEmpty()) {
            var last = lines.last().trimEnd()
            while (last.isNotEmpty() && paint.measureText("$last…") > maxWidth) {
                last = last.dropLast(1).trimEnd()
            }
            lines[lines.lastIndex] = if (last.isEmpty()) "…" else "$last…"
        }
        return lines.take(2)
    }

    private fun drawMeasuredText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        maxWidth: Float,
        capHeightPx: Float,
        typeface: Typeface,
        color: Int,
        capRatio: Float,
        align: Paint.Align = Paint.Align.CENTER
    ): Boolean {
        val paint = fitPaint(text, typeface, color, capHeightPx, maxWidth, capRatio, align) ?: return false
        val cap = Rect()
        paint.getTextBounds("H", 0, 1, cap)
        val baseline = centerY - (cap.top + cap.bottom) * 0.5f
        canvas.drawText(text, centerX, baseline, paint)
        return true
    }

    private fun drawRichLine(
        canvas: Canvas,
        fragments: List<Fragment>,
        centerX: Float,
        centerY: Float,
        maxWidth: Float,
        capHeight: Float
    ): Boolean {
        val visible = fragments.filter { it.text.isNotBlank() }
        if (visible.isEmpty()) return false
        val paints = visible.map { fragment ->
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                typeface = fragment.typeface
                color = fragment.color
                textSize = capHeight / fragment.capRatio.coerceIn(0.4f, 0.9f)
            }
        }
        var total = visible.indices.sumOf { paints[it].measureText(visible[it].text).toDouble() }.toFloat()
        if (total > maxWidth) {
            val factor = maxWidth / total
            if (factor < 0.85f) return false
            paints.forEach { it.textSize *= factor }
            total = visible.indices.sumOf { paints[it].measureText(visible[it].text).toDouble() }.toFloat()
        }
        val cap = Rect()
        paints.first().getTextBounds("H", 0, 1, cap)
        val baseline = centerY - (cap.top + cap.bottom) * 0.5f
        var x = centerX - total * 0.5f
        visible.indices.forEach { index ->
            canvas.drawText(visible[index].text, x, baseline, paints[index])
            x += paints[index].measureText(visible[index].text)
        }
        return true
    }

    private fun specs(metadata: WatermarkPhotoMetadata, settings: WatermarkLayoutSettings): String =
        WatermarkGeometry.buildSpecs(metadata, settings)

    private fun drawBar(
        context: Context, canvas: Canvas, geometry: WatermarkLayoutGeometry,
        settings: WatermarkLayoutSettings, metadata: WatermarkPhotoMetadata,
        fonts: Fonts, primary: Int, secondary: Int
    ) {
        val footer = geometry.footerRect ?: return
        canvas.drawRect(
            RectF(footer.left, footer.top, footer.right, footer.bottom),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (settings.invert) Color.BLACK else Color.WHITE }
        )
        drawLogo(context, canvas, geometry.logoRect ?: return, settings.invert)
        val s = geometry.shortSide
        val lines = listOf(
            specs(metadata, settings),
            metadata.cameraName.takeIf { settings.fields.cameraName }.orEmpty(),
            metadata.visibleLocation(settings)
        ).filter { it.isNotBlank() }
        if (lines.isEmpty()) return
        val cap = s * 0.0235f * settings.sizeScale
        val step = s * 0.0406f * settings.sizeScale
        val blockHeight = cap + (lines.size - 1) * step
        val top = footer.centerY - blockHeight * 0.5f
        val right = footer.right - s * 0.063f
        lines.forEachIndexed { index, line ->
            drawMeasuredText(
                canvas, line, right, top + cap * 0.5f + index * step,
                s * 0.54f, cap, fonts.medium,
                if (index == 0) primary else secondary,
                fonts.mediumCapRatio, Paint.Align.RIGHT
            )
        }
    }

    private fun drawCaption(
        canvas: Canvas, geometry: WatermarkLayoutGeometry, settings: WatermarkLayoutSettings,
        metadata: WatermarkPhotoMetadata, fonts: Fonts, primary: Int, secondary: Int
    ) {
        val footerTop = geometry.photoRect.bottom
        val centerX = geometry.canvasWidth * 0.5f
        val s = geometry.shortSide
        val location = metadata.visibleLocation(settings)
        drawRichLine(
            canvas,
            listOf(
                Fragment("Shot on ", fonts.regular, secondary, fonts.regularCapRatio),
                Fragment(metadata.cameraName, fonts.semibold, primary, fonts.semiboldCapRatio),
                Fragment(" | ", fonts.regular, secondary, fonts.regularCapRatio),
                Fragment("FUJIFILM", fonts.semibold, primary, fonts.semiboldCapRatio)
            ),
            centerX, footerTop + s * 0.078f, geometry.canvasWidth * 0.90f, s * 0.025f * settings.sizeScale
        )
        val f = settings.fields
        val line2 = buildList {
            if (f.focalLength) metadata.focal(settings).takeIf { it.isNotBlank() }?.let(::add)
            if (f.aperture) metadata.aperture.takeIf { it.isNotBlank() }?.let(::add)
            if (f.shutter) metadata.shutter.takeIf { it.isNotBlank() }?.let(::add)
            if (f.iso) metadata.iso.takeIf { it.isNotBlank() }?.let { add("ISO" + it.removePrefix("ISO").trim()) }
        }.joinToString("  ")
        drawMeasuredText(
            canvas, line2, centerX, footerTop + s * 0.125f,
            geometry.canvasWidth * 0.90f, s * 0.019f * settings.sizeScale,
            fonts.regular, secondary, fonts.regularCapRatio
        )
        if (location.isNotBlank()) {
            drawMeasuredText(
                canvas, location, centerX, footerTop + s * 0.172f,
                geometry.canvasWidth * 0.90f, s * 0.019f * settings.sizeScale,
                fonts.regular, secondary, fonts.regularCapRatio
            )
        }
    }

    private fun drawCard(
        context: Context, canvas: Canvas, geometry: WatermarkLayoutGeometry,
        settings: WatermarkLayoutSettings, metadata: WatermarkPhotoMetadata, fonts: Fonts
    ) {
        val meta = geometry.metadataRect ?: return
        drawLogo(context, canvas, geometry.logoRect ?: return, settings.invert)
        val s = geometry.shortSide
        val f = settings.fields
        val line = listOf(
            metadata.focal(settings).takeIf { f.focalLength }.orEmpty(),
            metadata.aperture.takeIf { f.aperture }.orEmpty(),
            metadata.shutter.takeIf { f.shutter }.orEmpty(),
            metadata.iso.removePrefix("ISO").trim().let { if (f.iso && it.isNotBlank()) "ISO " + it else "" }
        ).filter { it.isNotBlank() }.joinToString(" ")
        val cap = s * 0.032f * settings.sizeScale
        val right = meta.right - s * 0.049f
        val linePaint = fitPaint(line, fonts.medium, 0xFF3C3C3C.toInt(), cap, s * 0.52f, fonts.mediumCapRatio, Paint.Align.RIGHT)
        if (linePaint != null) {
            val capRect = Rect()
            linePaint.getTextBounds("H", 0, 1, capRect)
            val centerY = meta.top + meta.height * 0.62f
            val baseline = centerY - (capRect.top + capRect.bottom) * 0.5f
            val lineWidth = linePaint.measureText(line)
            val dotSize = s * 0.03f
            val dotX = right - lineWidth - s * 0.033f - dotSize * 0.5f
            canvas.drawCircle(dotX, centerY, dotSize * 0.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF19B88F.toInt() })
            canvas.drawText(line, right, baseline, linePaint)
        }
    }

    private fun drawGallery(
        context: Context, canvas: Canvas, geometry: WatermarkLayoutGeometry,
        settings: WatermarkLayoutSettings, metadata: WatermarkPhotoMetadata, fonts: Fonts
    ) {
        val photo = geometry.photoRect
        drawLogo(context, canvas, geometry.logoRect ?: return, settings.invert)
        val s = geometry.shortSide
        drawRichLine(
            canvas,
            listOf(
                Fragment("Shot on ", fonts.regular, 0xFF4A4A4A.toInt(), fonts.regularCapRatio),
                Fragment(metadata.cameraName, fonts.semibold, 0xFF111111.toInt(), fonts.semiboldCapRatio),
                Fragment(" | ", fonts.regular, 0xFF9A9A9A.toInt(), fonts.regularCapRatio),
                Fragment(if (settings.fields.date) metadata.date else "", fonts.regular, 0xFF4A4A4A.toInt(), fonts.regularCapRatio)
            ),
            geometry.canvasWidth * 0.5f, photo.bottom + s * 0.14f * settings.sizeScale,
            geometry.canvasWidth * 0.88f, s * 0.021f * settings.sizeScale
        )
        val location = metadata.visibleLocation(settings)
        if (location.isNotBlank()) {
            drawMeasuredText(
                canvas, location, geometry.canvasWidth * 0.5f, photo.bottom + s * 0.189f * settings.sizeScale,
                geometry.canvasWidth * 0.86f, s * 0.019f * settings.sizeScale,
                fonts.regular, 0xFF9C9C9C.toInt(), fonts.regularCapRatio
            )
        }
    }

    private fun drawBlack(
        context: Context, canvas: Canvas, geometry: WatermarkLayoutGeometry,
        settings: WatermarkLayoutSettings, metadata: WatermarkPhotoMetadata, fonts: Fonts
    ) {
        val footer = geometry.footerRect ?: return
        val s = geometry.shortSide
        val filter = metadata.filterName.takeIf { settings.fields.filter && it.isNotBlank() && !it.equals("STD", true) }
        val badge = geometry.badgeRect?.takeIf { filter != null }
        if (filter != null && badge != null) {
            val badgeColour = when (metadata.filterCategory.lowercase()) {
                "classic", "vintage", "cổ điển" -> 0xFF2F8F7A.toInt()
                "monochrome", "black & white", "đen trắng" -> 0xFF6B6B6B.toInt()
                else -> 0xFF2FA05A.toInt()
            }
            canvas.drawRoundRect(
                RectF(badge.left, badge.top, badge.right, badge.bottom),
                s * 0.006f, s * 0.006f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeColour }
            )
            val badgeLines = fitTwoLineBadge(
                text = when {
                    filter.contains("classic negative", true) || filter.contains("classic neg", true) -> "CLASSIC\nNEG."
                    filter.contains("classic chrome", true) -> "CLASSIC\nCHROME"
                    else -> filter.replace('_', ' ').replace('-', ' ').uppercase(java.util.Locale.ROOT)
                },
                typeface = fonts.semibold,
                maxWidth = badge.width * 0.88f,
                capHeightPx = s * 0.013f * settings.sizeScale,
                capRatio = fonts.semiboldCapRatio
            )
            badgeLines.forEachIndexed { index, line ->
                drawMeasuredText(
                    canvas, line, badge.centerX,
                    badge.centerY + (index - (badgeLines.size - 1) / 2f) * s * 0.018f,
                    badge.width * 0.88f, s * 0.013f * settings.sizeScale,
                    fonts.semibold, Color.WHITE, fonts.semiboldCapRatio
                )
            }
        }
        val leftStart = badge?.right?.plus(s * 0.016f) ?: footer.left + s * 0.01f
        val logoHeight = s * 0.022f * settings.sizeScale
        val logoWidth = logoHeight * 6.2f
        val logoCenterX = leftStart + logoWidth * 0.5f
        val logoCenterY = footer.centerY - s * 0.017f * settings.sizeScale
        drawLogo(
            context, canvas,
            WatermarkRect(logoCenterX - logoWidth / 2f, logoCenterY - logoHeight / 2f,
                logoCenterX + logoWidth / 2f, logoCenterY + logoHeight / 2f),
            true
        )
        if (filter != null) {
            drawMeasuredText(
                canvas, filter, logoCenterX, footer.centerY + s * 0.022f * settings.sizeScale,
                logoWidth * 1.65f, s * 0.014f * settings.sizeScale,
                fonts.regular, 0xFF9A9A9A.toInt(), fonts.regularCapRatio
            )
        }
        val f = settings.fields
        val specs = listOf(
            metadata.focal(settings).takeIf { f.focalLength }.orEmpty(),
            metadata.aperture.takeIf { f.aperture }.orEmpty(),
            metadata.shutter.removeSuffix("s").takeIf { f.shutter }.orEmpty(),
            metadata.iso.removePrefix("ISO").trim().let { if (f.iso && it.isNotBlank()) "ISO " + it else "" }
        ).filter { it.isNotBlank() }.joinToString(" ")
        val right = footer.right - s * 0.01f
        drawMeasuredText(
            canvas, specs, right, footer.centerY - s * 0.017f * settings.sizeScale,
            s * 0.60f, s * 0.0175f * settings.sizeScale,
            fonts.semibold, 0xFFF2F2F2.toInt(), fonts.semiboldCapRatio, Paint.Align.RIGHT
        )
        val lens = metadata.lensName.takeIf { f.lens }.orEmpty()
        val location = metadata.visibleLocation(settings)
        val lower = listOf(lens, location).filter { it.isNotBlank() }.joinToString(" · ")
        if (lower.isNotBlank()) {
            drawMeasuredText(
                canvas, lower, right, footer.centerY + s * 0.022f * settings.sizeScale,
                s * 0.60f, s * 0.014f * settings.sizeScale,
                fonts.regular, 0xFF9A9A9A.toInt(), fonts.regularCapRatio, Paint.Align.RIGHT
            )
        }
    }
}

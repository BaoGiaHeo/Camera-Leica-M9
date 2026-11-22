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

    private val fontsLock = Any()
    @Volatile
    private var cachedFonts: Fonts? = null

    private fun loadFonts(context: Context): Fonts {
        cachedFonts?.let { return it }
        return synchronized(fontsLock) {
            cachedFonts ?: run {
                val regular = context.resources.getFont(R.font.saira_semicondensed_regular)
                val medium = context.resources.getFont(R.font.saira_semicondensed_medium)
                val semibold = context.resources.getFont(R.font.saira_semicondensed_semibold)
                Fonts(
                    regular, medium, semibold,
                    readCapRatio(context, R.font.saira_semicondensed_regular, regular),
                    readCapRatio(context, R.font.saira_semicondensed_medium, medium),
                    readCapRatio(context, R.font.saira_semicondensed_semibold, semibold)
                ).also { cachedFonts = it }
            }
        }
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
        val isDark = when (settings.layout) {
            WatermarkLayoutType.BLACK -> !settings.invert
            WatermarkLayoutType.DOT_DARK -> !settings.invert
            WatermarkLayoutType.DOT_LIGHT -> settings.invert
            else -> settings.invert
        }
        val primary = if (isDark) WHITE else BLACK
        val secondary = if (isDark) 0xFF9A9A9A.toInt() else GREY
        canvas.drawColor(if (isDark) Color.BLACK else Color.WHITE)
        drawSourceAtNativeSize(canvas, source, geometry.photoRect)
        when (geometry.layout) {
            WatermarkLayoutType.BAR -> drawBar(context, canvas, geometry, settings, metadata, fonts, primary, secondary)
            WatermarkLayoutType.CAPTION -> drawCaption(canvas, geometry, settings, metadata, fonts)
            WatermarkLayoutType.GALLERY -> drawGallery(context, canvas, geometry, settings, metadata, fonts)
            WatermarkLayoutType.BLACK -> drawBlack(context, canvas, geometry, settings, metadata, fonts)
            WatermarkLayoutType.DOT_LIGHT,
            WatermarkLayoutType.DOT_DARK -> drawDot(context, canvas, geometry, settings, metadata, fonts, isDark, primary, secondary)
            WatermarkLayoutType.SPLIT -> drawSplit(context, canvas, geometry, settings, metadata, fonts, isDark, primary, secondary)
            WatermarkLayoutType.PALETTE -> drawPalette(context, canvas, source, geometry, settings, metadata, fonts, isDark, primary, secondary)
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

    private fun drawLogo(context: Context, canvas: Canvas, rect: WatermarkRect, white: Boolean) {
        val logo = ContextCompat.getDrawable(
            context,
            if (white) R.drawable.fujifilm_logo_white else R.drawable.fujifilm_logo
        ) ?: return
        logo.setBounds(rect.left.roundToInt(), rect.top.roundToInt(), rect.right.roundToInt(), rect.bottom.roundToInt())
        logo.draw(canvas)
    }

    private fun drawLetterSpacedText(
        canvas: Canvas,
        text: String,
        left: Float,
        centerY: Float,
        maxWidth: Float,
        capHeightPx: Float,
        typeface: Typeface,
        color: Int,
        capRatio: Float,
        spacingFraction: Float
    ) {
        if (text.isBlank() || maxWidth <= 0f) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            this.color = color
            textSize = (capHeightPx / capRatio.coerceIn(0.4f, 0.9f)).coerceAtLeast(1f)
        }
        val tracking = paint.textSize * spacingFraction.coerceAtLeast(0f)
        val totalWidth = text.sumOf { paint.measureText(it.toString()).toDouble() }.toFloat() +
            max(0, text.length - 1) * tracking
        if (totalWidth > maxWidth) paint.textSize *= (maxWidth / totalWidth).coerceAtLeast(0.70f)
        val cap = Rect()
        paint.getTextBounds("H", 0, 1, cap)
        val baseline = centerY - (cap.top + cap.bottom) * 0.5f
        var x = left
        text.forEachIndexed { index, char ->
            val glyph = char.toString()
            canvas.drawText(glyph, x, baseline, paint)
            x += paint.measureText(glyph)
            if (index < text.lastIndex) x += tracking
        }
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
        return paint
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
        var renderedText = text
        if (paint.measureText(renderedText) > maxWidth) {
            val suffix = "…"
            while (renderedText.isNotEmpty() &&
                paint.measureText(renderedText + suffix) > maxWidth
            ) {
                renderedText = renderedText.dropLast(1)
            }
            renderedText = renderedText.trimEnd() + suffix
        }
        if (renderedText.isEmpty() || paint.measureText(renderedText) > maxWidth) return false
        val cap = Rect()
        paint.getTextBounds("H", 0, 1, cap)
        val baseline = centerY - (cap.top + cap.bottom) * 0.5f
        canvas.drawText(renderedText, centerX, baseline, paint)
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
            if (factor < 0.70f) return false
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
        metadata: WatermarkPhotoMetadata, fonts: Fonts
    ) {
        val footerTop = geometry.photoRect.bottom
        val centerX = geometry.canvasWidth * 0.5f
        val s = geometry.shortSide
        val location = metadata.visibleLocation(settings)
        val primary = if (settings.invert) WHITE else BLACK
        val secondary = if (settings.invert) 0xFF9A9A9A.toInt() else 0xFF6E6E6E.toInt()
        drawRichLine(
            canvas,
            listOf(
                Fragment("Shot on ", fonts.regular, secondary, fonts.regularCapRatio),
                Fragment(
                    metadata.cameraName.takeIf { settings.fields.cameraName }.orEmpty(),
                    fonts.semibold, primary, fonts.semiboldCapRatio
                ),
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

    private fun drawGallery(
        context: Context, canvas: Canvas, geometry: WatermarkLayoutGeometry,
        settings: WatermarkLayoutSettings, metadata: WatermarkPhotoMetadata, fonts: Fonts
    ) {
        val photo = geometry.photoRect
        drawLogo(context, canvas, geometry.logoRect ?: return, settings.invert)
        val s = geometry.shortSide
        val primary = if (settings.invert) WHITE else BLACK
        val secondary = if (settings.invert) 0xFF9A9A9A.toInt() else 0xFF6E6E6E.toInt()
        drawRichLine(
            canvas,
            listOf(
                Fragment("Shot on ", fonts.regular, secondary, fonts.regularCapRatio),
                Fragment(metadata.cameraName.takeIf { settings.fields.cameraName }.orEmpty(), fonts.semibold, primary, fonts.semiboldCapRatio),
                Fragment(" | ", fonts.regular, secondary, fonts.regularCapRatio),
                Fragment(if (settings.fields.date) metadata.date else "", fonts.regular, secondary, fonts.regularCapRatio)
            ),
            geometry.canvasWidth * 0.5f, photo.bottom + s * 0.14f * settings.sizeScale,
            geometry.canvasWidth * 0.88f, s * 0.021f * settings.sizeScale
        )
        val location = metadata.visibleLocation(settings)
        if (location.isNotBlank()) {
            drawMeasuredText(
                canvas, location, geometry.canvasWidth * 0.5f, photo.bottom + s * 0.189f * settings.sizeScale,
                geometry.canvasWidth * 0.86f, s * 0.019f * settings.sizeScale,
                fonts.regular, secondary, fonts.regularCapRatio
            )
        }
    }

    private fun drawDot(
        context: Context,
        canvas: Canvas,
        geometry: WatermarkLayoutGeometry,
        settings: WatermarkLayoutSettings,
        metadata: WatermarkPhotoMetadata,
        fonts: Fonts,
        isDark: Boolean,
        primary: Int,
        secondary: Int
    ) {
        val footer = geometry.footerRect ?: return
        canvas.drawRect(
            RectF(footer.left, footer.top, footer.right, footer.bottom),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (isDark) 0xFF0F0F12.toInt() else Color.WHITE }
        )
        drawLogo(context, canvas, geometry.logoRect ?: return, isDark)
        val dot = geometry.badgeRect
        if (dot != null) {
            canvas.drawCircle(
                dot.centerX, dot.centerY, dot.width * 0.5f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isDark) 0xFFF5F5F5.toInt() else 0xFF36B38A.toInt()
                }
            )
        }
        val focal = metadata.focal(settings)
        val specs = listOf(
            focal.takeIf { settings.fields.focalLength }.orEmpty(),
            metadata.aperture.takeIf { settings.fields.aperture }.orEmpty(),
            metadata.shutter.takeIf { settings.fields.shutter }.orEmpty(),
            metadata.iso.takeIf { settings.fields.iso }?.let { value ->
                "ISO " + value.removePrefix("ISO").trim()
            }.orEmpty()
        ).filter { it.isNotBlank() }.joinToString(" ")
        val line = listOf(
            specs,
            metadata.lensName.takeIf { settings.fields.lens }.orEmpty()
        ).filter { it.isNotBlank() }.joinToString("  ")
        val meta = geometry.metadataRect ?: return
        drawMeasuredText(
            canvas, line, meta.right, footer.centerY,
            meta.width, geometry.shortSide * 0.025f * settings.sizeScale,
            fonts.medium, if (isDark) 0xFFF0F0F0.toInt() else 0xFF2A2A2A.toInt(),
            fonts.mediumCapRatio, Paint.Align.RIGHT
        )
    }

    private fun drawSplit(
        context: Context,
        canvas: Canvas,
        geometry: WatermarkLayoutGeometry,
        settings: WatermarkLayoutSettings,
        metadata: WatermarkPhotoMetadata,
        fonts: Fonts,
        isDark: Boolean,
        primary: Int,
        secondary: Int
    ) {
        val footer = geometry.footerRect ?: return
        canvas.drawRect(
            RectF(footer.left, footer.top, footer.right, footer.bottom),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (isDark) 0xFF0F0F12.toInt() else Color.WHITE }
        )
        val s = geometry.shortSide
        val left = s * 0.044f * settings.sizeScale
        val model = metadata.cameraName.takeIf { settings.fields.cameraName }.orEmpty()
        val dateTime = listOf(
            metadata.date.takeIf { settings.fields.date }.orEmpty(),
            metadata.time.takeIf { settings.fields.time }.orEmpty()
        ).filter { it.isNotBlank() }.joinToString(" ")
        val leftCenterY = footer.top + footer.height * 0.5f
        if (model.isNotBlank()) {
            drawMeasuredText(
                canvas, model, left, leftCenterY - s * 0.015f * settings.sizeScale,
                footer.width * 0.34f, s * 0.019f * settings.sizeScale,
                fonts.semibold, primary, fonts.semiboldCapRatio, Paint.Align.LEFT
            )
        }
        if (dateTime.isNotBlank()) {
            drawMeasuredText(
                canvas, dateTime, left, leftCenterY + s * 0.014f * settings.sizeScale,
                footer.width * 0.34f, s * 0.014f * settings.sizeScale,
                fonts.regular, secondary, fonts.regularCapRatio, Paint.Align.LEFT
            )
        }
        val logo = geometry.logoRect
        if (logo == null) return
        drawLogo(context, canvas, logo, isDark)
        val dividerX = logo.right + s * 0.022f * settings.sizeScale
        val specsRect = geometry.metadataRect ?: return
        val specsLeft = dividerX + s * 0.022f * settings.sizeScale
        val line1 = buildSplitSpecs(metadata, settings)
        val line2 = metadata.author.takeIf { it.isNotBlank() }
            ?: metadata.visibleLocation(settings)
        val middleY = footer.centerY
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) 0xFF777777.toInt() else 0xFFD6D6D6.toInt()
            strokeWidth = max(1f, s * 0.0012f * settings.sizeScale)
        }
        canvas.drawLine(
            dividerX, middleY - s * 0.025f * settings.sizeScale,
            dividerX, middleY + s * 0.025f * settings.sizeScale,
            dividerPaint
        )
        if (line1.isNotBlank()) {
            drawMeasuredText(
                canvas, line1, specsLeft, middleY - s * 0.009f * settings.sizeScale,
                max(1f, specsRect.right - specsLeft), s * 0.019f * settings.sizeScale,
                fonts.semibold, primary, fonts.semiboldCapRatio, Paint.Align.LEFT
            )
        }
        if (line2.isNotBlank()) {
            drawMeasuredText(
                canvas, line2, specsLeft, middleY + s * 0.018f * settings.sizeScale,
                max(1f, specsRect.right - specsLeft), s * 0.014f * settings.sizeScale,
                fonts.regular, secondary, fonts.regularCapRatio, Paint.Align.LEFT
            )
        }
    }

    private fun buildSplitSpecs(
        metadata: WatermarkPhotoMetadata,
        settings: WatermarkLayoutSettings
    ): String = listOf(
        metadata.focal(settings).takeIf { settings.fields.focalLength }.orEmpty(),
        metadata.aperture.takeIf { settings.fields.aperture }.orEmpty(),
        metadata.shutter.removeSuffix("s").takeIf { settings.fields.shutter }.orEmpty(),
        metadata.iso.removePrefix("ISO").trim().takeIf { settings.fields.iso && it.isNotBlank() }
            ?.let { "ISO$it" }.orEmpty()
    ).filter { it.isNotBlank() }.joinToString(" ")

    private fun drawPalette(
        context: Context,
        canvas: Canvas,
        source: Bitmap,
        geometry: WatermarkLayoutGeometry,
        settings: WatermarkLayoutSettings,
        metadata: WatermarkPhotoMetadata,
        fonts: Fonts,
        isDark: Boolean,
        primary: Int,
        secondary: Int
    ) {
        val footer = geometry.footerRect ?: return
        canvas.drawRect(
            RectF(footer.left, footer.top, footer.right, footer.bottom),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (isDark) 0xFF0F0F12.toInt() else Color.WHITE }
        )
        val logo = geometry.logoRect ?: return
        drawLogo(context, canvas, logo, isDark)
        val s = geometry.shortSide
        val model = metadata.cameraName.takeIf { settings.fields.cameraName }.orEmpty()
        if (model.isNotBlank()) {
            drawMeasuredText(
                canvas, model, geometry.photoRect.left,
                s * 0.205f * settings.sizeScale,
                geometry.sourceWidth * 0.82f,
                s * 0.024f * settings.sizeScale,
                fonts.regular, if (isDark) 0xFF9A9A9A.toInt() else 0xFF7E7E7E.toInt(),
                fonts.regularCapRatio, Paint.Align.LEFT
            )
        }
        val title = metadata.title.takeIf { it.isNotBlank() }
            ?: metadata.visibleLocation(settings)
        val photoBottom = geometry.photoRect.bottom
        if (title.isNotBlank()) {
            drawMeasuredText(
                canvas, title, geometry.photoRect.left,
                photoBottom + s * 0.103f * settings.sizeScale,
                geometry.sourceWidth * 0.92f,
                s * 0.037f * settings.sizeScale,
                fonts.regular, primary, fonts.regularCapRatio, Paint.Align.LEFT
            )
        }
        val dateText = metadata.date.takeIf { settings.fields.date }.orEmpty()
        if (dateText.isNotBlank()) {
            drawLetterSpacedText(
                canvas, context.getString(R.string.watermark_photographed_in, dateText.uppercase()),
                geometry.photoRect.left, photoBottom + s * 0.178f * settings.sizeScale,
                geometry.sourceWidth * 0.92f, s * 0.017f * settings.sizeScale,
                fonts.regular, if (isDark) 0xFF9A9A9A.toInt() else 0xFF8C8C8C.toInt(),
                fonts.regularCapRatio, spacingFraction = 0.22f
            )
        }
        val f = settings.fields
        val focal = metadata.focal(settings)
        val specs = listOf(
            focal.takeIf { f.focalLength }.orEmpty(),
            metadata.aperture.takeIf { f.aperture }.orEmpty(),
            metadata.shutter.removeSuffix("s").takeIf { f.shutter }.orEmpty(),
            metadata.iso.removePrefix("ISO").trim().takeIf { f.iso && it.isNotBlank() }
                ?.let { "ISO$it" }.orEmpty()
        ).filter { it.isNotBlank() }.joinToString("   ")
        if (specs.isNotBlank()) {
            drawMeasuredText(
                canvas, specs, geometry.photoRect.left,
                photoBottom + s * 0.211f * settings.sizeScale,
                geometry.sourceWidth * 0.92f,
                s * 0.016f * settings.sizeScale,
                fonts.regular, if (isDark) 0xFF9A9A9A.toInt() else 0xFF4A4A4A.toInt(),
                fonts.regularCapRatio, Paint.Align.LEFT
            )
        }
        val coordinates = metadata.coordinates.takeIf {
            settings.locationEnabled && settings.fields.location && it.isNotBlank()
        }.orEmpty()
        if (coordinates.isNotBlank()) {
            drawMeasuredText(
                canvas, coordinates, geometry.photoRect.left,
                photoBottom + s * 0.239f * settings.sizeScale,
                geometry.sourceWidth * 0.92f,
                s * 0.016f * settings.sizeScale,
                fonts.regular, if (isDark) 0xFF9A9A9A.toInt() else 0xFF4A4A4A.toInt(),
                fonts.regularCapRatio, Paint.Align.LEFT
            )
        }
        geometry.paletteRect?.let { rect ->
            val colors = extractPaletteColors(source, 5)
            val swatchWidth = rect.width / 5f
            colors.forEachIndexed { index, color ->
                val left = rect.left + index * swatchWidth
                canvas.drawRect(
                    left, rect.top, if (index == 4) rect.right else left + swatchWidth, rect.bottom,
                    Paint().apply { this.color = color }
                )
            }
        }
    }

    private fun extractPaletteColors(bitmap: Bitmap, count: Int): List<Int> {
        val sampleW = min(128, bitmap.width).coerceAtLeast(1)
        val sampleH = min(128, bitmap.height).coerceAtLeast(1)
        val pixels = IntArray(sampleW * sampleH)
        val sample = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)
        try {
            sample.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
        } finally {
            if (sample !== bitmap && !sample.isRecycled) sample.recycle()
        }
        val labs = pixels.map(::rgbToOklab).filter { it[0] > 0.08f && it[0] < 0.96f }
            .ifEmpty { pixels.map(::rgbToOklab) }
        if (labs.isEmpty()) return List(count) { Color.GRAY }
        val sorted = labs.sortedBy { it[0] }
        var centroids = List(count) { index ->
            sorted[(index * (sorted.size - 1) / (count - 1).coerceAtLeast(1))]
        }.map { it.copyOf() }
        repeat(8) {
            val sums = Array(count) { FloatArray(3) }
            val totals = IntArray(count)
            labs.forEach { lab ->
                var best = 0
                var distance = Float.MAX_VALUE
                centroids.forEachIndexed { index, center ->
                    val d0 = lab[0] - center[0]
                    val d1 = lab[1] - center[1]
                    val d2 = lab[2] - center[2]
                    val d = d0 * d0 + d1 * d1 + d2 * d2
                    if (d < distance) { distance = d; best = index }
                }
                for (axis in 0..2) sums[best][axis] += lab[axis]
                totals[best]++
            }
            centroids = centroids.mapIndexed { index, old ->
                if (totals[index] == 0) old else FloatArray(3) { axis -> sums[index][axis] / totals[index] }
            }
        }
        return centroids.sortedBy { it[0] }.map(::oklabToRgb)
    }

    private fun rgbToOklab(color: Int): FloatArray {
        fun linear(v: Int): Float {
            val s = v / 255f
            return if (s <= 0.04045f) s / 12.92f else ((s + 0.055f) / 1.055f).toDouble().let { Math.pow(it, 2.4).toFloat() }
        }
        val r = linear(Color.red(color))
        val g = linear(Color.green(color))
        val b = linear(Color.blue(color))
        val l = 0.41222147f * r + 0.53633254f * g + 0.05144599f * b
        val m = 0.21190350f * r + 0.68069955f * g + 0.10739696f * b
        val s = 0.08830246f * r + 0.28171884f * g + 0.62997870f * b
        val lRoot = Math.cbrt(l.toDouble()).toFloat()
        val mRoot = Math.cbrt(m.toDouble()).toFloat()
        val sRoot = Math.cbrt(s.toDouble()).toFloat()
        return floatArrayOf(
            0.21045426f * lRoot + 0.79361779f * mRoot - 0.00407205f * sRoot,
            1.97799850f * lRoot - 2.42859221f * mRoot + 0.45059371f * sRoot,
            0.02590404f * lRoot + 0.78277177f * mRoot - 0.80867577f * sRoot
        )
    }

    private fun oklabToRgb(lab: FloatArray): Int {
        val lRoot = lab[0] + 0.39633778f * lab[1] + 0.21580376f * lab[2]
        val mRoot = lab[0] - 0.10556135f * lab[1] - 0.06385417f * lab[2]
        val sRoot = lab[0] - 0.08948418f * lab[1] - 1.29148555f * lab[2]
        val l = lRoot * lRoot * lRoot
        val m = mRoot * mRoot * mRoot
        val s = sRoot * sRoot * sRoot
        val r = 4.07674166f * l - 3.30771159f * m + 0.23096993f * s
        val g = -1.26843800f * l + 2.60975740f * m - 0.34131940f * s
        val b = -0.00419609f * l - 0.70341861f * m + 1.70761470f * s
        fun encoded(v: Float): Int {
            val clamped = v.coerceIn(0f, 1f)
            val srgb = if (clamped <= 0.0031308f) clamped * 12.92f
                else 1.055f * clamped.toDouble().let { Math.pow(it, 1.0 / 2.4).toFloat() } - 0.055f
            return (srgb * 255f).roundToInt().coerceIn(0, 255)
        }
        return Color.rgb(encoded(r), encoded(g), encoded(b))
    }

    private fun splitFilterBadgeName(name: String): List<String> {
        val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        if (words.size == 1) {
            val single = words.first()
            if (single.length <= 11) return listOf(single)
            val split = (single.length + 1) / 2
            return listOf(single.substring(0, split), single.substring(split))
        }
        var bestIndex = 1
        var bestDifference = Int.MAX_VALUE
        for (index in 1 until words.size) {
            val leftLength = words.take(index).sumOf { it.length } + index - 1
            val rightLength = words.drop(index).sumOf { it.length } + words.size - index - 1
            val difference = kotlin.math.abs(leftLength - rightLength)
            if (difference < bestDifference) {
                bestDifference = difference
                bestIndex = index
            }
        }
        return listOf(words.take(bestIndex).joinToString(" "), words.drop(bestIndex).joinToString(" "))
    }

    private fun drawBlack(
        context: Context, canvas: Canvas, geometry: WatermarkLayoutGeometry,
        settings: WatermarkLayoutSettings, metadata: WatermarkPhotoMetadata, fonts: Fonts
    ) {
        val footer = geometry.footerRect ?: return
        val s = geometry.shortSide
        val filter = metadata.filterName.takeIf { settings.fields.filter && it.isNotBlank() && !it.equals("STD", true) }
        val badge = geometry.badgeRect
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
            val badgeLines = splitFilterBadgeName(filter).take(2)
            badgeLines.forEachIndexed { index, line ->
                drawMeasuredText(
                    canvas, line, badge.centerX,
                    badge.centerY + (index - (badgeLines.size - 1) / 2f) * s * 0.018f,
                    badge.width * 0.88f, s * 0.013f * settings.sizeScale,
                    fonts.semibold, if (settings.invert) BLACK else WHITE, fonts.semiboldCapRatio
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
            !settings.invert
        )
        if (filter != null) {
            drawMeasuredText(
                canvas, filter, logoCenterX, footer.centerY + s * 0.022f * settings.sizeScale,
                logoWidth * 1.65f, s * 0.014f * settings.sizeScale,
                fonts.regular, if (settings.invert) 0xFF6E6E6E.toInt() else 0xFF9A9A9A.toInt(),
                fonts.regularCapRatio
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
            fonts.semibold, if (settings.invert) BLACK else WHITE, fonts.semiboldCapRatio, Paint.Align.RIGHT
        )
        val lens = metadata.lensName.takeIf { f.lens }.orEmpty()
        val location = metadata.visibleLocation(settings)
        val lower = listOf(lens, location).filter { it.isNotBlank() }.joinToString(" · ")
        if (lower.isNotBlank()) {
            drawMeasuredText(
                canvas, lower, right, footer.centerY + s * 0.022f * settings.sizeScale,
                s * 0.60f, s * 0.014f * settings.sizeScale,
                fonts.regular, if (settings.invert) 0xFF6E6E6E.toInt() else 0xFF9A9A9A.toInt(),
                fonts.regularCapRatio, Paint.Align.RIGHT
            )
        }
    }
}

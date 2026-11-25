package com.example.camui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat

/**
 * Cached font adapter for Compose and the native OSD renderer.
 * Arial assets are used when present; Android sans-serif is the safe fallback.
 */
object FujiTypography {
    const val FONT_ASSET_PATH = "fonts/vcr_osd_mono_kk7.ttf.gz"
    const val FONT_RESOURCE_PATH = "res/font/vcr_osd_mono_kk7.ttf"

    const val GLYPH_TEST_CHARACTERS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" +
            "+-/:.,%°·●K" +
            "ĂÂĐÊÔƠƯÁÀẢÃẠẮẰẲẴẶẤẦẨẪẬẾỀỂỄỆỐỒỔỖỘỚỜỞỠỢỨỪỬỮỰÝỲỶỸỴ" +
            "ăâđêôơưáàảãạắằẳẵặấầẩẫậếềểễệốồổỗộớờởỡợứừửữựýỳỷỹỵ"

    private const val TAG = "FujiTypography"

    @Volatile private var cachedTypeface: Typeface? = null
    @Volatile private var cachedShareTechTypeface: Typeface? = null
    @Volatile private var cachedSystemMonoTypeface: Typeface? = null
    @Volatile private var cachedFontFamily: FontFamily? = null
    @Volatile private var cachedShareTechFontFamily: FontFamily? = null
    @Volatile private var bundledFontLoaded = false
    @Volatile private var fallbackWarningLogged = false
    @Volatile private var startupReportLogged = false

    private val lock = Any()

    /**
     * Application UI and OSD use Arial assets when present, otherwise sans-serif.
     * Watermark typography remains independent in WatermarkRenderer.
     */
    val uiFont: FontFamily = FontFamily.SansSerif

    @Volatile private var cachedUiTypeface: Typeface? = null
    @Volatile private var cachedUiFontFamily: FontFamily? = null

    fun uiFontFamily(context: Context): FontFamily {
        cachedUiFontFamily?.let { return it }
        return synchronized(lock) {
            cachedUiFontFamily ?: FontFamily(
                Font(AppFonts.regular(context), FontWeight.Normal),
                Font(AppFonts.bold(context), FontWeight.Bold)
            ).also { cachedUiFontFamily = it }
        }
    }

    fun uiTypeface(context: Context): Typeface {
        cachedUiTypeface?.let { return it }
        return synchronized(lock) {
            cachedUiTypeface ?: AppFonts.regular(context).also { cachedUiTypeface = it }
        }
    }

    fun withUiMissingGlyphFallback(context: Context, value: String): AnnotatedString {
        val primary = Paint().apply { typeface = uiTypeface(context) }
        val fallbackPaint = Paint().apply {
            typeface = cachedShareTechTypeface ?: Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val systemPaint = Paint().apply { typeface = systemMonoTypeface() }
        val fallbackFamily = shareTechComposeFamily()
        return buildAnnotatedString {
            append(value)
            var index = 0
            while (index < value.length) {
                val codePoint = value.codePointAt(index)
                val charCount = Character.charCount(codePoint)
                val glyph = String(Character.toChars(codePoint))
                if (!primary.hasGlyph(glyph)) {
                    val family = when {
                        fallbackPaint.hasGlyph(glyph) -> fallbackFamily
                        systemPaint.hasGlyph(glyph) -> FontFamily.SansSerif
                        else -> FontFamily.Monospace
                    }
                    addStyle(SpanStyle(fontFamily = family), index, index + charCount)
                }
                index += charCount
            }
        }
    }


    data class GlyphCoverageReport(
        val testedGlyphs: Int,
        val missingFromKk7: List<String>,
        val missingAfterFallback: List<String>,
        val missingAfterSystemFallback: List<String>
    )

    fun androidTypeface(context: Context): Typeface {
        cachedTypeface?.let { return it }
        val loaded = synchronized(lock) {
            cachedTypeface ?: AppFonts.regular(context).also {
                bundledFontLoaded = false
                cachedTypeface = it
                if (!fallbackWarningLogged) {
                    Log.i(TAG, "Arial asset not found in source; OSD uses system sans-serif fallback.")
                    fallbackWarningLogged = true
                }
            }
        }
        logStartupDiagnostics(context.applicationContext, loaded)
        return loaded
    }

    fun shareTechTypeface(context: Context): Typeface {
        cachedShareTechTypeface?.let { return it }
        return synchronized(lock) {
            cachedShareTechTypeface ?: AppFonts.regular(context).also { cachedShareTechTypeface = it }
        }
    }

    fun systemMonoTypeface(): Typeface {
        cachedSystemMonoTypeface?.let { return it }
        return synchronized(lock) {
            cachedSystemMonoTypeface ?: Typeface.create("sans-serif", Typeface.NORMAL)
                .also { cachedSystemMonoTypeface = it }
        }
    }

    fun isBundledFontLoaded(context: Context): Boolean {
        androidTypeface(context)
        return bundledFontLoaded
    }

    fun glyphCoverage(context: Context): GlyphCoverageReport =
        computeGlyphCoverage(androidTypeface(context), shareTechTypeface(context))

    /**
     * Uses the system sans-serif family for glyphs not covered by the selected font.
     */
    fun withMissingGlyphFallback(value: String): AnnotatedString {
        val primary = cachedTypeface ?: Typeface.create("monospace", Typeface.NORMAL)
        val primaryPaint = Paint().apply { typeface = primary }
        val fallbackPaint = Paint().apply {
            typeface = cachedShareTechTypeface ?: Typeface.create("monospace", Typeface.NORMAL)
        }
        val systemPaint = Paint().apply { typeface = systemMonoTypeface() }
        val fallbackFamily = shareTechComposeFamily()
        return buildAnnotatedString {
            append(value)
            var index = 0
            while (index < value.length) {
                val codePoint = value.codePointAt(index)
                val charCount = Character.charCount(codePoint)
                val glyph = String(Character.toChars(codePoint))
                if (!primaryPaint.hasGlyph(glyph)) {
                    val family = when {
                        fallbackPaint.hasGlyph(glyph) -> fallbackFamily
                        systemPaint.hasGlyph(glyph) -> FontFamily.Monospace
                        else -> FontFamily.Monospace
                    }
                    addStyle(SpanStyle(fontFamily = family), index, index + charCount)
                }
                index += charCount
            }
        }
    }

    fun composeFamily(context: Context): FontFamily {
        cachedFontFamily?.let { return it }
        val typeface = androidTypeface(context)
        return synchronized(lock) {
            cachedFontFamily ?: FontFamily(typeface).also { cachedFontFamily = it }
        }
    }

    private fun shareTechComposeFamily(): FontFamily {
        cachedShareTechFontFamily?.let { return it }
        return synchronized(lock) {
            cachedShareTechFontFamily ?: FontFamily.SansSerif.also { cachedShareTechFontFamily = it }
        }
    }

    private fun computeGlyphCoverage(primary: Typeface, fallback: Typeface): GlyphCoverageReport {
        val primaryPaint = Paint().apply { typeface = primary }
        val fallbackPaint = Paint().apply { typeface = fallback }
        val glyphs = linkedSetOf<String>()
        var index = 0
        while (index < GLYPH_TEST_CHARACTERS.length) {
            val codePoint = GLYPH_TEST_CHARACTERS.codePointAt(index)
            if (!Character.isWhitespace(codePoint)) {
                glyphs += String(Character.toChars(codePoint))
            }
            index += Character.charCount(codePoint)
        }
        val systemPaint = Paint().apply { typeface = systemMonoTypeface() }
        val missingPrimary = glyphs.filterNot(primaryPaint::hasGlyph)
        val missingAfterFallback = missingPrimary.filterNot(fallbackPaint::hasGlyph)
        val missingAfterSystem = missingAfterFallback.filterNot(systemPaint::hasGlyph)
        return GlyphCoverageReport(glyphs.size, missingPrimary, missingAfterFallback, missingAfterSystem)
    }

    private fun logStartupDiagnostics(context: Context, primary: Typeface) {
        if (startupReportLogged) return
        synchronized(lock) {
            if (startupReportLogged) return
            val fallback = AppFonts.regular(context)
            cachedShareTechTypeface = fallback
            val report = computeGlyphCoverage(primary, fallback)
            Log.i(
                TAG,
                "OSD typeface=${primary.toString()}; source=$FONT_RESOURCE_PATH; " +
                    "bundledKK7=$bundledFontLoaded; glyphs=${report.testedGlyphs}; " +
                    "KK7Missing=${report.missingFromKk7.joinToString("")}; " +
                    "ShareTechMissing=${report.missingAfterFallback.joinToString("")}; " +
                    "systemMonoUnresolved=${report.missingAfterSystemFallback.joinToString("")}"
            )
            startupReportLogged = true
        }
    }

    internal fun clearCacheForTests() {
        synchronized(lock) {
            cachedTypeface = null
            cachedShareTechTypeface = null
            cachedSystemMonoTypeface = null
            cachedFontFamily = null
            cachedShareTechFontFamily = null
            cachedUiFontFamily = null
            bundledFontLoaded = false
            fallbackWarningLogged = false
            startupReportLogged = false
        }
    }
}

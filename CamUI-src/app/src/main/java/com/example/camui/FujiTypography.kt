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
 * The one typography source for Compose text and the native pixel OSD renderer.
 *
 * KK7 remains the primary face. The Gradle resource task inflates the checked-in
 * compressed source into the Android resource res/font/vcr_osd_mono_kk7.ttf.
 * Share Tech Mono is the per-glyph fallback for characters not present in KK7.
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
     * Saira is the application UI face. KK7 remains isolated to the viewfinder OSD
     * through composeFamily()/androidTypeface(); watermark typography is rendered
     * by WatermarkRenderer and is intentionally kept independent.
     */
    val osdFont: FontFamily by lazy {
        FontFamily(Font(R.font.vcr_osd_mono_kk7, FontWeight.Normal))
    }

    val uiFont: FontFamily by lazy {
        FontFamily(
            Font(R.font.saira_semicondensed_semibold, FontWeight.SemiBold),
            Font(R.font.saira_semicondensed_bold, FontWeight.Bold)
        )
    }

    // WatermarkRenderer continues to use these same three Saira faces and weights.
    val watermarkFont: FontFamily by lazy {
        FontFamily(
            Font(R.font.saira_semicondensed_regular, FontWeight.Normal),
            Font(R.font.saira_semicondensed_medium, FontWeight.Medium),
            Font(R.font.saira_semicondensed_semibold, FontWeight.SemiBold)
        )
    }

    @Volatile private var cachedUiTypeface: Typeface? = null

    fun uiTypeface(context: Context): Typeface {
        cachedUiTypeface?.let { return it }
        return synchronized(lock) {
            cachedUiTypeface ?: runCatching {
                ResourcesCompat.getFont(context.applicationContext, R.font.saira_semicondensed_semibold)
                    ?: error("Saira Semi Condensed SemiBold font resource is missing.")
            }.getOrElse {
                Log.w(TAG, "Saira UI font could not be loaded; using sans-serif fallback.", it)
                Typeface.create("sans-serif-condensed", Typeface.BOLD)
            }.also { cachedUiTypeface = it }
        }
    }

    fun withUiMissingGlyphFallback(context: Context, value: String): AnnotatedString {
        val primary = Paint().apply { typeface = uiTypeface(context) }
        val fallbackTypeface = shareTechTypeface(context)
        val fallbackPaint = Paint().apply { typeface = fallbackTypeface }
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
                        systemPaint.hasGlyph(glyph) -> FontFamily.Monospace
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

    data class UiGlyphCoverageReport(
        val testedGlyphs: Int,
        val missingFromSaira: List<String>,
        val missingAfterFallback: List<String>,
        val missingAfterSystemFallback: List<String>
    )

    fun androidTypeface(context: Context): Typeface {
        cachedTypeface?.let { return it }
        val loaded = synchronized(lock) {
            cachedTypeface ?: runCatching {
                ResourcesCompat.getFont(context.applicationContext, R.font.vcr_osd_mono_kk7)
                    ?: error("Android returned a null KK7 font resource.")
            }.onSuccess {
                bundledFontLoaded = true
            }.onFailure { error ->
                bundledFontLoaded = false
                if (!fallbackWarningLogged) {
                    Log.w(TAG, "KK7 font resource could not be loaded; using system monospace fallback.", error)
                    fallbackWarningLogged = true
                }
            }.getOrElse {
                Typeface.create("monospace", Typeface.NORMAL)
            }.also { cachedTypeface = it }
        }
        logStartupDiagnostics(context.applicationContext, loaded)
        return loaded
    }

    fun shareTechTypeface(context: Context): Typeface {
        cachedShareTechTypeface?.let { return it }
        return synchronized(lock) {
            cachedShareTechTypeface ?: runCatching {
                ResourcesCompat.getFont(context.applicationContext, R.font.share_tech_mono)
                    ?: error("Android returned a null Share Tech Mono font resource.")
            }.getOrElse { error ->
                Log.w(TAG, "Share Tech Mono fallback resource could not be loaded.", error)
                Typeface.create("monospace", Typeface.NORMAL)
            }.also { cachedShareTechTypeface = it }
        }
    }

    fun systemMonoTypeface(): Typeface {
        cachedSystemMonoTypeface?.let { return it }
        return synchronized(lock) {
            cachedSystemMonoTypeface ?: Typeface.create("monospace", Typeface.NORMAL)
                .also { cachedSystemMonoTypeface = it }
        }
    }

    fun isBundledFontLoaded(context: Context): Boolean {
        androidTypeface(context)
        return bundledFontLoaded
    }

    fun glyphCoverage(context: Context): GlyphCoverageReport =
        computeGlyphCoverage(androidTypeface(context), shareTechTypeface(context))

    fun uiGlyphCoverage(context: Context): UiGlyphCoverageReport {
        val coverage = computeGlyphCoverage(uiTypeface(context), shareTechTypeface(context))
        return UiGlyphCoverageReport(
            testedGlyphs = coverage.testedGlyphs,
            missingFromSaira = coverage.missingFromKk7,
            missingAfterFallback = coverage.missingAfterFallback,
            missingAfterSystemFallback = coverage.missingAfterSystemFallback
        )
    }

    /**
     * Selects Share Tech Mono for each code point missing from KK7. Combining
     * marks are kept as separate spans, so decomposed Vietnamese text is not
     * silently dropped even when the primary bitmap face lacks those marks.
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

    @Suppress("UNUSED_PARAMETER")
    fun composeFamily(context: Context): FontFamily = uiFont

    private fun shareTechComposeFamily(): FontFamily {
        cachedShareTechFontFamily?.let { return it }
        return synchronized(lock) {
            cachedShareTechFontFamily ?: FontFamily(Font(R.font.share_tech_mono))
                .also { cachedShareTechFontFamily = it }
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
            val fallback = runCatching {
                ResourcesCompat.getFont(context, R.font.share_tech_mono)
                    ?: Typeface.create("monospace", Typeface.NORMAL)
            }.getOrElse { Typeface.create("monospace", Typeface.NORMAL) }
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
            bundledFontLoaded = false
            fallbackWarningLogged = false
            startupReportLogged = false
        }
    }
}

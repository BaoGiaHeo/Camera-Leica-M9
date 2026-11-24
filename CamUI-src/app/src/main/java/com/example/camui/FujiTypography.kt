package com.example.camui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily

/**
 * Shared typography provider. Arial assets are optional and loaded once by AppFonts;
 * Android SansSerif is the safe fallback when either file is not present.
 */
object FujiTypography {
    const val FONT_ASSET_PATH = AppFonts.REGULAR_ASSET
    const val FONT_BOLD_ASSET_PATH = AppFonts.BOLD_ASSET
    const val FONT_RESOURCE_PATH = "assets/" + FONT_ASSET_PATH

    const val GLYPH_TEST_CHARACTERS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" +
            "+-/:.,%°·●K" +
            "ĂÂĐÊÔƠƯÁÀẢÃẠẮẰẲẴẶẤẦẨẪẬẾỀỂỄỆỐỒỔỖỘỚỜỞỠỢỨỪỬỮỰÝỲỶỸỴ" +
            "ăâđêôơưáàảãạắằẳẵặấầẩẫậếềểễệốồổỗộớờởỡợứừửữựýỳỹỵ"

    /** Source-compatible property for legacy call sites without a Context. */
    val uiFont: FontFamily get() = FontFamily.SansSerif

    data class GlyphCoverageReport(
        val testedGlyphs: Int,
        val missingFromKk7: List<String>,
        val missingAfterFallback: List<String>,
        val missingAfterSystemFallback: List<String>
    )

    fun uiTypeface(context: Context): Typeface = AppFonts.regular(context)

    fun uiBoldTypeface(context: Context): Typeface = AppFonts.bold(context)

    fun androidTypeface(context: Context): Typeface = AppFonts.regular(context)

    /** Legacy name retained for callers; it now resolves to the shared Arial face. */
    fun shareTechTypeface(context: Context): Typeface = AppFonts.regular(context)

    /** Legacy method name retained for callers; this is now SansSerif, not monospace. */
    fun systemMonoTypeface(): Typeface = Typeface.SANS_SERIF

    fun isBundledFontLoaded(context: Context): Boolean =
        AppFonts.isRegularAssetLoaded(context)

    fun glyphCoverage(context: Context): GlyphCoverageReport =
        computeGlyphCoverage(AppFonts.regular(context), AppFonts.regular(context))

    fun composeFamily(context: Context): FontFamily = AppFonts.family(context)

    fun withUiMissingGlyphFallback(context: Context, value: String): AnnotatedString =
        withGlyphFallback(value, AppFonts.regular(context), AppFonts.family(context))

    fun withMissingGlyphFallback(value: String): AnnotatedString {
        val face = Typeface.SANS_SERIF
        return withGlyphFallback(value, face, FontFamily.SansSerif)
    }

    private fun withGlyphFallback(
        value: String,
        primary: Typeface,
        fallbackFamily: FontFamily
    ): AnnotatedString {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = primary }
        return buildAnnotatedString {
            append(value)
            var index = 0
            while (index < value.length) {
                val codePoint = value.codePointAt(index)
                val count = Character.charCount(codePoint)
                val glyph = String(Character.toChars(codePoint))
                if (!paint.hasGlyph(glyph)) {
                    addStyle(SpanStyle(fontFamily = fallbackFamily), index, index + count)
                }
                index += count
            }
        }
    }

    private fun computeGlyphCoverage(primary: Typeface, fallback: Typeface): GlyphCoverageReport {
        val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = primary }
        val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = fallback }
        val systemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.SANS_SERIF }
        val glyphs = linkedSetOf<String>()
        var index = 0
        while (index < GLYPH_TEST_CHARACTERS.length) {
            val codePoint = GLYPH_TEST_CHARACTERS.codePointAt(index)
            if (!Character.isWhitespace(codePoint)) glyphs += String(Character.toChars(codePoint))
            index += Character.charCount(codePoint)
        }
        val missingPrimary = glyphs.filterNot(primaryPaint::hasGlyph)
        val missingAfterFallback = missingPrimary.filterNot(fallbackPaint::hasGlyph)
        val missingAfterSystem = missingAfterFallback.filterNot(systemPaint::hasGlyph)
        return GlyphCoverageReport(
            testedGlyphs = glyphs.size,
            missingFromKk7 = missingPrimary,
            missingAfterFallback = missingAfterFallback,
            missingAfterSystemFallback = missingAfterSystem
        )
    }

    internal fun clearCacheForTests() {
        AppFonts.clearCacheForTests()
    }
}

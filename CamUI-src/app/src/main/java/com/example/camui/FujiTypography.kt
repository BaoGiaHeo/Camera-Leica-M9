package com.example.camui

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * The single typography entry point for the FujiFilm Mode UI.
 *
 * The exact KK7 TTF supplied for this project is stored as a normal Android asset.
 * It is copied byte-for-byte into the app cache before Typeface loads it. If
 * the asset is unavailable or damaged,
 * Android's monospace face is used and a warning is written to Logcat.
 */
object FujiTypography {
    const val FONT_ASSET_PATH = "fonts/vcr_osd_mono_kk7.ttf.gz"
    const val FONT_CACHE_NAME = "vcr_osd_mono_kk7.ttf"

    private const val TAG = "FujiTypography"

    @Volatile
    private var cachedTypeface: Typeface? = null

    @Volatile
    private var cachedFontFamily: FontFamily? = null

    @Volatile
    private var bundledFontLoaded = false

    @Volatile
    private var fallbackWarningLogged = false

    private val lock = Any()

    fun androidTypeface(context: Context): Typeface {
        cachedTypeface?.let { return it }
        synchronized(lock) {
            cachedTypeface?.let { return it }
            val loaded = runCatching {
                loadBundledTypeface(context.applicationContext).also { bundledFontLoaded = true }
            }
                .onFailure { error ->
                    bundledFontLoaded = false
                    if (!fallbackWarningLogged) {
                        Log.w(TAG, "KK7 VCR OSD Mono could not be loaded; using system monospace fallback.", error)
                        fallbackWarningLogged = true
                    }
                }
                .getOrElse { Typeface.create("monospace", Typeface.NORMAL) }
            cachedTypeface = loaded
            return loaded
        }
    }

    fun isBundledFontLoaded(context: Context): Boolean {
        androidTypeface(context)
        return bundledFontLoaded
    }

    /**
     * KK7 is missing the middle dot and prime/double-prime punctuation glyphs.
     * Only those individual code points use a monospace fallback. Geometric
     * OSD symbols such as the focus dot are rendered by the pixel-sprite layer.
     */
    fun withMissingGlyphFallback(value: String): AnnotatedString {
        return buildAnnotatedString {
            append(value)
            value.forEachIndexed { index, character ->
                if (character == '·' || character == '′' || character == '″') {
                    addStyle(
                        SpanStyle(fontFamily = FontFamily.Monospace),
                        index,
                        index + 1
                    )
                }
            }
        }
    }

    fun composeFamily(context: Context): FontFamily {
        cachedFontFamily?.let { return it }
        synchronized(lock) {
            cachedFontFamily?.let { return it }
            return FontFamily(androidTypeface(context)).also { cachedFontFamily = it }
        }
    }

    private fun loadBundledTypeface(context: Context): Typeface {
        val output = File(context.cacheDir, FONT_CACHE_NAME)
        if (!output.isFile || output.length() == 0L) {
            val temporary = File(context.cacheDir, "$FONT_CACHE_NAME.tmp")
            runCatching { temporary.delete() }
            context.assets.open(FONT_ASSET_PATH).use { compressed ->
                GZIPInputStream(compressed).use { input ->
                    temporary.outputStream().use { destination -> input.copyTo(destination) }
                }
            }
            check(temporary.isFile && temporary.length() > 0L) {
                "Decompressed KK7 font is empty."
            }
            if (!temporary.renameTo(output)) {
                temporary.copyTo(output, overwrite = true)
                temporary.delete()
            }
        }
        return Typeface.createFromFile(output)
    }

    /** Allows instrumentation tests to force a reload after changing the test asset. */
    internal fun clearCacheForTests() {
        synchronized(lock) {
            cachedTypeface = null
            cachedFontFamily = null
            bundledFontLoaded = false
            fallbackWarningLogged = false
        }
    }
}

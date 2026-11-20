package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FujiTypographyGlyphCoverageTest {
    @Test
    fun reportsKk7CoverageAndShareTechFallbackForVietnameseAndOsdGlyphs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue("Bundled KK7 resource did not load", FujiTypography.isBundledFontLoaded(context))
        val report = FujiTypography.glyphCoverage(context)
        Log.i(
            "FujiTypographyTest",
            "tested=${report.testedGlyphs}; KK7 missing=${report.missingFromKk7.joinToString("")}; " +
                "unresolved=${report.missingAfterFallback.joinToString("")}"
        )
        assertTrue("Glyph test did not run", report.testedGlyphs > 0)
        assertTrue("Share Tech fallback is missing required test glyphs: ${report.missingAfterFallback}", report.missingAfterFallback.isEmpty())
    }

    @Test
    fun kk7BitmapRenderingDiffersFromSystemDefault() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val kk7 = FujiTypography.androidTypeface(context)
        val sample = "FUJIFILM 1/125 F2.8 ISO400 EV+0.7"
        val kk7Bitmap = renderSample(sample, kk7)
        val systemBitmap = renderSample(sample, Typeface.create("sans-serif", Typeface.NORMAL))
        try {
            var differentPixels = 0
            for (y in 0 until kk7Bitmap.height) {
                for (x in 0 until kk7Bitmap.width) {
                    if (kk7Bitmap.getPixel(x, y) != systemBitmap.getPixel(x, y)) differentPixels++
                }
            }
            assertTrue("KK7 bitmap is indistinguishable from system sans-serif", differentPixels > 0)
        } finally {
            kk7Bitmap.recycle()
            systemBitmap.recycle()
        }
    }

    private fun renderSample(text: String, typeface: Typeface): Bitmap {
        val bitmap = Bitmap.createBitmap(420, 44, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            this.typeface = typeface
        }
        canvas.drawText(text, 4f, 30f, paint)
        return bitmap
    }
}

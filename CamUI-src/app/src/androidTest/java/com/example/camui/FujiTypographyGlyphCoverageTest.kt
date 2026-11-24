package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FujiTypographyGlyphCoverageTest {
    @Test
    fun arialOrSansSerifFallbackCoversVietnameseAndCameraGlyphs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val regular = FujiTypography.uiTypeface(context)
        val report = FujiTypography.glyphCoverage(context)
        assertTrue("Glyph test did not run", report.testedGlyphs > 0)
        assertTrue(
            "Required Vietnamese/OSD glyphs remain unresolved: ${report.missingAfterSystemFallback}",
            report.missingAfterSystemFallback.isEmpty()
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = regular }
        assertTrue("Primary typeface cannot render Vietnamese Đ", paint.hasGlyph("Đ"))
        assertTrue("Primary typeface cannot render camera symbol °", paint.hasGlyph("°"))
    }

    @Test
    fun regularAndBoldFacesRenderWithoutRequiringBundledFontAssets() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val regular = FujiTypography.uiTypeface(context)
        val bold = FujiTypography.uiBoldTypeface(context)
        val sample = "FUJIFILM 1/125 F2.8 ISO400 EV+0.7"
        val regularBitmap = renderSample(sample, regular)
        val boldBitmap = renderSample(sample, bold)
        try {
            assertTrue("Regular text rendering produced no glyphs", countLightPixels(regularBitmap) > 0)
            assertTrue("Bold text rendering produced no glyphs", countLightPixels(boldBitmap) > 0)
            assertTrue("Arial/SansSerif regular and bold faces are indistinguishable", differentPixels(regularBitmap, boldBitmap) > 0)
        } finally {
            regularBitmap.recycle()
            boldBitmap.recycle()
        }
    }

    private fun renderSample(text: String, typeface: android.graphics.Typeface): Bitmap {
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

    private fun countLightPixels(bitmap: Bitmap): Int {
        var count = 0
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            if ((bitmap.getPixel(x, y) and 0x00FFFFFF) != 0) count++
        }
        return count
    }

    private fun differentPixels(first: Bitmap, second: Bitmap): Int {
        var count = 0
        for (y in 0 until first.height) for (x in 0 until first.width) {
            if (first.getPixel(x, y) != second.getPixel(x, y)) count++
        }
        return count
    }
}

package com.example.camui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WatermarkTypographyAssetTest {
    @Test
    fun sharedWatermarkFontsLoadWithOrWithoutOptionalArialAssets() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val regular = AppFonts.regular(context)
        val bold = AppFonts.bold(context)
        assertNotNull(regular)
        assertNotNull(bold)
        assertEquals(Typeface.NORMAL, regular.style)
        assertEquals(Typeface.BOLD, bold.style)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = regular }
        assertTrue("Watermark font cannot render Vietnamese text", paint.hasGlyph("Đã lưu ảnh"))
    }

    @Test
    fun composeAndWatermarkUseTheSameSharedTypefaceFamily() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val family = FujiTypography.composeFamily(context)
        assertNotNull(family)
        assertTrue(AppFonts.regular(context).isSupportedGlyph("A"))
        assertTrue(AppFonts.bold(context).isSupportedGlyph("F"))
    }

    private fun Typeface.isSupportedGlyph(glyph: String): Boolean =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = this@isSupportedGlyph }.hasGlyph(glyph)
}

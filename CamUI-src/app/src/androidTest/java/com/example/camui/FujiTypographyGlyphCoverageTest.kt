package com.example.camui

import android.content.Context
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
        val report = FujiTypography.glyphCoverage(context)
        Log.i(
            "FujiTypographyTest",
            "tested=${report.testedGlyphs}; KK7 missing=${report.missingFromKk7.joinToString("")}; " +
                "unresolved=${report.missingAfterFallback.joinToString("")}"
        )
        assertTrue("Glyph test did not run", report.testedGlyphs > 0)
    }
}

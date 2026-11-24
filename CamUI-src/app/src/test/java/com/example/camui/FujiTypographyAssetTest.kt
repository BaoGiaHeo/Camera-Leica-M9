package com.example.camui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FujiTypographyAssetTest {
    @Test
    fun optionalArialAssetPathsAreStableAndDoNotRequireBundledFonts() {
        assertEquals("fonts/arial.ttf", FujiTypography.FONT_ASSET_PATH)
        assertEquals("fonts/arial_bold.ttf", FujiTypography.FONT_BOLD_ASSET_PATH)

        val regular = File("src/main/assets/${FujiTypography.FONT_ASSET_PATH}")
        val bold = File("src/main/assets/${FujiTypography.FONT_BOLD_ASSET_PATH}")

        // The user supplies these two assets separately. Their absence is valid:
        // AppFonts must use Typeface.SANS_SERIF / SansSerif Bold without failing startup.
        if (regular.exists()) {
            assertTrue("Arial regular asset is unexpectedly small", regular.length() > 4_000L)
        }
        if (bold.exists()) {
            assertTrue("Arial bold asset is unexpectedly small", bold.length() > 4_000L)
        }
    }

    @Test
    fun xmlThemeDoesNotDependOnRemovedVcrFontResource() {
        val theme = File("src/main/res/values/themes.xml").readText()
        assertTrue("Theme must retain a safe SansSerif system fallback", theme.contains("sans-serif"))
        assertFalse("Theme must not reference the removed VCR font", theme.contains("@font/vcr_osd_mono_kk7"))
    }
}

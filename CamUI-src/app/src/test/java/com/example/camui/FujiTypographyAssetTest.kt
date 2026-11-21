package com.example.camui

import java.io.File
import java.util.zip.GZIPInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FujiTypographyAssetTest {
    @Test
    fun bundledKk7FontAssetExistsAndInflatesToTrueType() {
        val asset = File("src/main/assets/${FujiTypography.FONT_ASSET_PATH}")
        assertTrue("Missing bundled font asset: ${asset.path}", asset.isFile)
        assertTrue("Bundled font asset is unexpectedly small", asset.length() > 4_000L)

        val signature = GZIPInputStream(asset.inputStream()).use { input ->
            ByteArray(4).also { buffer ->
                var offset = 0
                while (offset < buffer.size) {
                    val count = input.read(buffer, offset, buffer.size - offset)
                    check(count > 0) { "Truncated bundled font asset" }
                    offset += count
                }
            }
        }
        assertArrayEquals(byteArrayOf(0, 1, 0, 0), signature)
        val fallbackFont = File("src/main/res/font/share_tech_mono.ttf")
        assertTrue("Missing Share Tech Mono fallback font", fallbackFont.isFile)
        assertTrue("Share Tech Mono fallback font is unexpectedly small", fallbackFont.length() > 10_000L)
        val license = File("src/main/assets/fonts/ShareTechMono-OFL.txt")
        assertTrue("Missing Share Tech Mono OFL notice", license.isFile)
        assertTrue("Share Tech Mono OFL notice is incomplete", license.readText().contains("SIL OPEN FONT LICENSE Version 1.1"))
        assertTrue(FujiTypography.GLYPH_TEST_CHARACTERS.contains("ĂÂĐÊÔƠƯ"))
        assertTrue(FujiTypography.GLYPH_TEST_CHARACTERS.contains("ăâđêôơư"))
        val theme = File("src/main/res/values/themes.xml").readText()
        assertTrue(
            "Android UI theme must use the same Saira Regular face as WatermarkRenderer",
            theme.contains("@font/saira_semicondensed_regular")
        )
        assertFalse(
            "KK7 must never be the default font for Android UI widgets",
            theme.contains("@font/vcr_osd_mono_kk7")
        )

        val typography = File("src/main/java/com/example/camui/FujiTypography.kt").readText()
        listOf(
            "Font(R.font.saira_semicondensed_regular, FontWeight.Normal)",
            "Font(R.font.saira_semicondensed_medium, FontWeight.Medium)",
            "Font(R.font.saira_semicondensed_semibold, FontWeight.SemiBold)",
            "Font(R.font.saira_semicondensed_bold, FontWeight.Bold)"
        ).forEach { weight ->
            assertTrue("Application UI is missing Watermark font weight: $weight", typography.contains(weight))
        }

        val osdRenderer = File("src/main/java/com/example/camui/PixelOsdLayer.kt").readText()
        assertTrue(
            "The live OSD renderer must continue to load KK7 independently",
            osdRenderer.contains("FujiTypography.androidTypeface(appContext)")
        )
    }
}

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
        assertTrue("Android UI theme must use Saira SemiBold", theme.contains("@font/saira_semicondensed_semibold"))
        assertFalse("Android UI theme must not assign the KK7 LCD font", theme.contains("@font/vcr_osd_mono_kk7"))

        val typography = File("src/main/java/com/example/camui/FujiTypography.kt").readText()
        assertTrue("FujiTypography must declare a dedicated KK7 OSD family", typography.contains("val osdFont"))
        assertTrue("FujiTypography must declare a dedicated Saira UI family", typography.contains("val uiFont"))
        assertTrue("FujiTypography must keep a separate watermark family", typography.contains("val watermarkFont"))
        assertTrue("Compose UI family must resolve to Saira", typography.contains("fun composeFamily(context: Context): FontFamily = uiFont"))

        val pixelOsd = File("src/main/java/com/example/camui/PixelOsdLayer.kt").readText()
        assertTrue("LCD renderer must load KK7, not the UI family", pixelOsd.contains("FujiTypography.androidTypeface"))
    }
}

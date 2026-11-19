package com.example.camui

import java.io.File
import java.util.zip.GZIPInputStream
import org.junit.Assert.assertArrayEquals
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
    }
}

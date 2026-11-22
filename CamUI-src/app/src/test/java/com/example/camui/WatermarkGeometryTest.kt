package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkGeometryTest {
    private val metadata = WatermarkPhotoMetadata(
        cameraName = "X-S10",
        lensName = "35mm",
        focal35mm = "33mm",
        aperture = "f/1.4",
        shutter = "1/100s",
        iso = "ISO 100",
        date = "2025.06.10",
        time = "17:10:00",
        location = "Lake",
        coordinates = "52°13′14″N 124°04′53″W",
        filterName = "PROVIA",
        title = "Nature's poetry",
        author = "Mike"
    )

    @Test
    fun dotLayoutsUseRequired1920By1280Geometry() {
        val light = WatermarkGeometry.build(
            1920, 1280, WatermarkLayoutSettings(layout = WatermarkLayoutType.DOT_LIGHT), metadata
        )
        val dark = WatermarkGeometry.build(
            1920, 1280, WatermarkLayoutSettings(layout = WatermarkLayoutType.DOT_DARK), metadata
        )
        assertEquals(1982, light.canvasWidth)
        assertEquals(1536, light.canvasHeight)
        assertEquals(31f, light.photoRect.left, 0.6f)
        assertEquals(31f, light.photoRect.top, 0.6f)
        assertEquals(233f, light.logoRect!!.width, 1.5f)
        assertEquals(41f, light.badgeRect!!.width, 0.8f)
        assertEquals(1982, dark.canvasWidth)
        assertEquals(1536, dark.canvasHeight)
        assertEquals(225f, light.footerRect!!.height, 1.0f)
        assertEquals(225f, dark.footerRect!!.height, 1.0f)
    }

    @Test
    fun splitLayoutUsesRequiredBottomBandGeometry() {
        val geometry = WatermarkGeometry.build(
            1920, 1280, WatermarkLayoutSettings(layout = WatermarkLayoutType.SPLIT), metadata
        )
        assertEquals(1920, geometry.canvasWidth)
        assertEquals(1421, geometry.canvasHeight)
        assertEquals(141f, geometry.footerRect!!.height, 0.6f)
        assertEquals(192f, geometry.logoRect!!.width, 1.0f)
        assertEquals(31f, geometry.logoRect!!.height, 1.0f)
    }

    @Test
    fun paletteLayoutUsesRequiredCanvasLogoAndSwatchGeometry() {
        val geometry = WatermarkGeometry.build(
            1920, 1280, WatermarkLayoutSettings(layout = WatermarkLayoutType.PALETTE), metadata
        )
        assertEquals(2036, geometry.canvasWidth)
        assertEquals(2002, geometry.canvasHeight)
        assertEquals(58f, geometry.photoRect.left, 0.6f)
        assertEquals(344f, geometry.photoRect.top, 0.6f)
        assertEquals(404f, geometry.logoRect!!.width, 1.0f)
        assertEquals(73f, geometry.paletteRect!!.height, 1.0f)
        assertEquals(922f, geometry.paletteRect!!.width, 1.5f)
    }

    @Test
    fun allEightLayoutsPreserveSourcePixelDimensions() {
        WatermarkLayoutType.values().forEach { layout ->
            val geometry = WatermarkGeometry.build(
                1920, 1280, WatermarkLayoutSettings(layout = layout), metadata
            )
            assertEquals(layout, geometry.layout)
            assertEquals(1920f, geometry.photoRect.width, 0.001f)
            assertEquals(1280f, geometry.photoRect.height, 0.001f)
            assertTrue(geometry.canvasWidth >= geometry.photoRect.right)
            assertTrue(geometry.canvasHeight >= geometry.photoRect.bottom)
            assertTrue(geometry.canvasWidth <= 65_535)
            assertTrue(geometry.canvasHeight <= 65_535)
        }
    }

    @Test
    fun retiredCardPreferenceMigratesToDotLightAndLegacyLayoutsRemainStable() {
        assertEquals(WatermarkLayoutType.DOT_LIGHT, WatermarkLayoutType.fromPreference("card"))
        assertEquals(WatermarkLayoutType.DOT_LIGHT, WatermarkLayoutType.fromPreference("c"))
        assertEquals(WatermarkLayoutType.GALLERY, WatermarkLayoutType.fromPreference("d"))
        assertEquals(WatermarkLayoutType.BLACK, WatermarkLayoutType.fromPreference("e"))
        assertEquals(WatermarkLayoutType.DOT_DARK, WatermarkLayoutType.fromPreference("dot_dark"))
        assertEquals(WatermarkLayoutType.SPLIT, WatermarkLayoutType.fromPreference("split"))
        assertEquals(WatermarkLayoutType.PALETTE, WatermarkLayoutType.fromPreference("palette"))
        assertFalse(WatermarkLayoutType.values().any { it.name == "CARD" })
    }
}

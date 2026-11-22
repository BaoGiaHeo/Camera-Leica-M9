package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkLayoutMigrationTest {
    @Test
    fun exposesExactlyEightSupportedWatermarkLayouts() {
        assertEquals(
            setOf("A", "B", "D", "E", "F", "G", "H", "I"),
            WatermarkLayoutType.values().map { it.id }.toSet()
        )
        assertEquals(8, WatermarkLayoutType.values().size)
        assertFalse(WatermarkLayoutType.values().any { it.name == "CARD" })
    }

    @Test
    fun legacyCardPreferenceMigratesToDotLight() {
        assertEquals(WatermarkLayoutType.DOT_LIGHT, WatermarkLayoutType.fromPreference("card"))
        assertEquals(WatermarkLayoutType.DOT_LIGHT, WatermarkLayoutType.fromPreference("CARD"))
        assertEquals("dot_light", WatermarkLayoutType.preferenceValue(WatermarkLayoutType.DOT_LIGHT))
    }

    @Test
    fun everyLayoutPreservesOriginalPhotoPixelDimensions() {
        val metadata = WatermarkPhotoMetadata(
            cameraName = "Camera X", focal35mm = "33mm",
            aperture = "f/1.4", shutter = "1/100s", iso = "ISO 100"
        )
        WatermarkLayoutType.values().forEach { style ->
            val geometry = WatermarkGeometry.build(
                1920, 1280, WatermarkLayoutSettings(layout = style), metadata
            )
            assertEquals(style, geometry.layout)
            assertEquals(1920f, geometry.photoRect.width, 0.001f)
            assertEquals(1280f, geometry.photoRect.height, 0.001f)
            assertTrue(geometry.canvasWidth >= 1920)
            assertTrue(geometry.canvasHeight >= 1280)
            assertTrue(geometry.footerRect != null)
        }
    }

    @Test
    fun dotStylesUseTheRequestedShortSideFooterFraction() {
        val metadata = WatermarkPhotoMetadata(cameraName = "Camera X")
        val light = WatermarkGeometry.build(1920, 1280, WatermarkLayoutSettings(layout = WatermarkLayoutType.DOT_LIGHT), metadata)
        val dark = WatermarkGeometry.build(1920, 1280, WatermarkLayoutSettings(layout = WatermarkLayoutType.DOT_DARK), metadata)
        assertEquals(1280f * 0.176f, light.footerRect!!.height, 1.1f)
        assertEquals(light.footerRect!!.height, dark.footerRect!!.height, 0.001f)
    }

    @Test
    fun sizeSettingMovesInFivePercentStepsAndClampsAtTwoSteps() {
        assertEquals(0.90f, WatermarkLayoutSettings(sizeStep = -2).sizeScale, 0.0001f)
        assertEquals(1.00f, WatermarkLayoutSettings(sizeStep = 0).sizeScale, 0.0001f)
        assertEquals(1.10f, WatermarkLayoutSettings(sizeStep = 2).sizeScale, 0.0001f)
        assertEquals(1.10f, WatermarkLayoutSettings(sizeStep = 9).sizeScale, 0.0001f)
    }
}

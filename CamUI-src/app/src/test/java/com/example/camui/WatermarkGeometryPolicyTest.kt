package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkGeometryPolicyTest {
    @Test
    fun footerHeightsUseSpecifiedOneTwoAndThreeRowFractions() {
        assertEquals(0.075f, watermarkFooterFractionForRows(1), 0.0001f)
        assertEquals(0.095f, watermarkFooterFractionForRows(2), 0.0001f)
        assertEquals(0.11f, watermarkFooterFractionForRows(3), 0.0001f)
        assertEquals(0.11f, watermarkFooterFractionForRows(4), 0.0001f)
    }

    @Test
    fun logoSizesUseSpecifiedShortSideFractions() {
        assertEquals(0.020f, watermarkLogoHeightFraction("S"), 0.0001f)
        assertEquals(0.026f, watermarkLogoHeightFraction("M"), 0.0001f)
        assertEquals(0.034f, watermarkLogoHeightFraction("L"), 0.0001f)
        assertEquals(0.026f, watermarkLogoHeightFraction(null), 0.0001f)
    }

    @Test
    fun autoStyleChoosesLightPlateForDarkImagesAndDarkPlateForBrightImages() {
        assertTrue(watermarkAutoUsesLightPlate(0.1f))
        assertTrue(watermarkAutoUsesLightPlate(0.5f))
        assertFalse(watermarkAutoUsesLightPlate(0.8f))
    }

    @Test
    fun nonFiniteLuminanceUsesDarkPlateFallback() {
        assertFalse(watermarkAutoUsesLightPlate(Float.NaN))
        assertFalse(watermarkAutoUsesLightPlate(Float.POSITIVE_INFINITY))
    }
}

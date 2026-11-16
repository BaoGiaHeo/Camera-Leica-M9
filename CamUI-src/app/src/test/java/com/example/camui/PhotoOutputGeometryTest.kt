package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoOutputGeometryTest {
    @Test
    fun landscape21By9CropAnd250MpOutputKeepTheRequestedRatio() {
        val target = 250_000_000L
        val geometry = PhotoOutputGeometry.calculate(4000, 3000, target, 21f / 9f)

        assertEquals(0, geometry.cropLeft)
        assertEquals(4000, geometry.cropWidth)
        assertTrue(geometry.cropTop > 0)
        assertTrue(geometry.cropHeight < 3000)
        assertTrue(geometry.outputPixels <= target)
        assertTrue(geometry.outputPixels >= target * 995L / 1000L)
        assertTrue(kotlin.math.abs(
            geometry.outputWidth.toDouble() / geometry.outputHeight - 21.0 / 9.0
        ) < 0.0003)
    }

    @Test
    fun portrait21By9CropAnd250MpOutputKeepTheRequestedRatio() {
        val target = 250_000_000L
        val geometry = PhotoOutputGeometry.calculate(3000, 4000, target, 9f / 21f)

        assertEquals(0, geometry.cropTop)
        assertTrue(geometry.cropLeft > 0)
        assertTrue(geometry.cropWidth < 3000)
        assertEquals(4000, geometry.cropHeight)
        assertTrue(geometry.outputPixels <= target)
        assertTrue(geometry.outputPixels >= target * 995L / 1000L)
        assertTrue(kotlin.math.abs(
            geometry.outputWidth.toDouble() / geometry.outputHeight - 9.0 / 21.0
        ) < 0.0003)
    }

    @Test
    fun unchangedSourceRatioDoesNotCropAndOutputStaysWithinBudget() {
        val geometry = PhotoOutputGeometry.calculate(4000, 3000, 60_000_000L, 4f / 3f)

        assertEquals(0, geometry.cropLeft)
        assertEquals(0, geometry.cropTop)
        assertEquals(4000, geometry.cropWidth)
        assertEquals(3000, geometry.cropHeight)
        assertTrue(geometry.outputPixels <= 60_000_000L)
        assertTrue(kotlin.math.abs(
            geometry.outputWidth.toDouble() / geometry.outputHeight - 4.0 / 3.0
        ) < 0.0003)
    }

    @Test
    fun commonLandscapeAndPortraitRatiosStayWithinThePixelBudget() {
        val ratios = listOf(3f / 2f, 4f / 3f, 16f / 9f, 1f, 21f / 9f)
        ratios.forEach { ratio ->
            val landscape = PhotoOutputGeometry.calculate(4000, 3000, 108_000_000L, ratio)
            val portrait = PhotoOutputGeometry.calculate(3000, 4000, 108_000_000L, 1f / ratio)
            assertTrue(landscape.outputPixels <= 108_000_000L)
            assertTrue(portrait.outputPixels <= 108_000_000L)
            assertTrue(kotlin.math.abs(
                landscape.outputWidth.toDouble() / landscape.outputHeight - ratio
            ) < 0.0005)
            assertTrue(kotlin.math.abs(
                portrait.outputWidth.toDouble() / portrait.outputHeight - 1.0 / ratio
            ) < 0.0005)
        }
    }

    @Test
    fun lunaDream15OutputReachesTheReal50MegapixelTarget() {
        val geometry = PhotoOutputGeometry.calculate(4000, 3000, 50_000_000L, 4f / 3f)

        assertTrue(geometry.outputPixels <= 50_000_000L)
        assertTrue(geometry.outputPixels >= 49_750_000L)
    }

    @Test
    fun lunaDream20OutputReachesTheReal108MegapixelTarget() {
        val geometry = PhotoOutputGeometry.calculate(4000, 3000, 108_000_000L, 4f / 3f)

        assertTrue(geometry.outputPixels <= 108_000_000L)
        assertTrue(geometry.outputPixels >= 107_460_000L)
    }

    @Test
    fun missingOrInvalidAspectRatioFallsBackToSourceRatio() {
        val missing = PhotoOutputGeometry.calculate(4000, 3000, 60_000_000L)
        val invalid = PhotoOutputGeometry.calculate(4000, 3000, 60_000_000L, Float.NaN)
        assertEquals(4.0 / 3.0, missing.requestedAspectRatio, 0.000001)
        assertEquals(4.0 / 3.0, invalid.requestedAspectRatio, 0.000001)
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidSourceDimensions() {
        PhotoOutputGeometry.calculate(0, 3000, 60_000_000L, 16f / 9f)
    }
}

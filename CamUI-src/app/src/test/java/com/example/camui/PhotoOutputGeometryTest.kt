package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PhotoOutputGeometryTest {
    @Test
    fun landscape21By9KeepsTheWholeSourceAndAddsSideBars() {
        val target = 60_000_000L
        val geometry = PhotoOutputGeometry.calculate(4000, 3000, target, 21f / 9f)
        assertEquals(0, geometry.imageTop)
        assertEquals(geometry.outputHeight, geometry.imageHeight)
        assertTrue(geometry.imageLeft > 0)
        assertTrue(geometry.imageWidth < geometry.outputWidth)
        assertTrue(abs(geometry.imageWidth.toDouble() / geometry.imageHeight - 4.0 / 3.0) < 0.001)
        assertTrue(abs(geometry.outputWidth.toDouble() / geometry.outputHeight - 21.0 / 9.0) < 0.001)
        assertTrue(geometry.outputPixels <= target)
        assertTrue(geometry.outputPixels >= target * 995L / 1000L)
    }

    @Test
    fun portrait21By9KeepsTheWholeSourceAndAddsTopBottomBars() {
        val target = 60_000_000L
        val geometry = PhotoOutputGeometry.calculate(3000, 4000, target, 9f / 21f)
        assertEquals(0, geometry.imageLeft)
        assertEquals(geometry.outputWidth, geometry.imageWidth)
        assertTrue(geometry.imageTop > 0)
        assertTrue(geometry.imageHeight < geometry.outputHeight)
        assertTrue(abs(geometry.imageWidth.toDouble() / geometry.imageHeight - 3.0 / 4.0) < 0.001)
        assertTrue(abs(geometry.outputWidth.toDouble() / geometry.outputHeight - 9.0 / 21.0) < 0.001)
        assertTrue(geometry.outputPixels <= target)
        assertTrue(geometry.outputPixels >= target * 995L / 1000L)
    }

    @Test
    fun matchingAspectUsesTheEntireCanvas() {
        val geometry = PhotoOutputGeometry.calculate(4000, 3000, 60_000_000L, 4f / 3f)
        assertEquals(0, geometry.imageLeft)
        assertEquals(0, geometry.imageTop)
        assertEquals(geometry.outputWidth, geometry.imageWidth)
        assertEquals(geometry.outputHeight, geometry.imageHeight)
        assertTrue(geometry.outputPixels <= 60_000_000L)
    }

    @Test
    fun allRequestedRatiosKeepTheFullImageWithoutDistortion() {
        val ratios = listOf(16f / 9f, 21f / 9f, 3f / 4f, 1f)
        ratios.forEach { ratio ->
            val geometry = PhotoOutputGeometry.calculate(4000, 3000, 108_000_000L, ratio)
            assertTrue(geometry.outputPixels <= 108_000_000L)
            assertTrue(geometry.outputPixels >= 107_460_000L)
            assertTrue(abs(geometry.outputWidth.toDouble() / geometry.outputHeight - ratio) < 0.001)
            assertTrue(geometry.imageLeft >= 0 && geometry.imageTop >= 0)
            assertTrue(geometry.imageLeft + geometry.imageWidth <= geometry.outputWidth)
            assertTrue(geometry.imageTop + geometry.imageHeight <= geometry.outputHeight)
            assertTrue(abs(geometry.imageWidth.toDouble() / geometry.imageHeight - 4.0 / 3.0) < 0.001)
        }
    }

    @Test
    fun lunaDream15OutputStillReachesThe50MegapixelTarget() {
        val geometry = PhotoOutputGeometry.calculate(4000, 3000, 50_000_000L, 4f / 3f)
        assertTrue(geometry.outputPixels <= 50_000_000L)
        assertTrue(geometry.outputPixels >= 49_750_000L)
    }

    @Test
    fun lunaDream20OutputStillReachesThe108MegapixelTarget() {
        val geometry = PhotoOutputGeometry.calculate(4000, 3000, 108_000_000L, 4f / 3f)
        assertTrue(geometry.outputPixels <= 108_000_000L)
        assertTrue(geometry.outputPixels >= 107_460_000L)
    }

    @Test
    fun missingOrInvalidRatioUsesTheSourceRatio() {
        val default = PhotoOutputGeometry.calculate(4000, 3000, 60_000_000L)
        val invalid = PhotoOutputGeometry.calculate(4000, 3000, 60_000_000L, Float.NaN)
        assertEquals(4.0 / 3.0, default.requestedAspectRatio, 0.000001)
        assertEquals(4.0 / 3.0, invalid.requestedAspectRatio, 0.000001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDimensionsAreRejected() {
        PhotoOutputGeometry.calculate(0, 3000, 60_000_000L, 16f / 9f)
    }
}

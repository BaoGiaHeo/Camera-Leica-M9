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

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidSourceDimensions() {
        PhotoOutputGeometry.calculate(0, 3000, 60_000_000L, 16f / 9f)
    }
}

package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PhotoOutputGeometryTest {
    @Test
    fun portraitAndLandscapeLabelsStillMatchTheSelectedFrame() {
        assertEquals("9:16", aspectRatioDisplayLabel(0, false))
        assertEquals("16:9", aspectRatioDisplayLabel(0, true))
        assertEquals("3:4", aspectRatioDisplayLabel(1, false))
        assertEquals("4:3", aspectRatioDisplayLabel(1, true))
        assertEquals("1:1", aspectRatioDisplayLabel(2, false))
        assertEquals("1:1", aspectRatioDisplayLabel(2, true))
        assertEquals("9:21", aspectRatioDisplayLabel(3, false))
        assertEquals("21:9", aspectRatioDisplayLabel(3, true))
    }

    @Test
    fun allRatiosUseHeightOverWidthAsThePortraitCoordinateSystem() {
        assertEquals(16f / 9f, aspectRatioForOrientation(0, false), 0.0001f)
        assertEquals(4f / 3f, aspectRatioForOrientation(1, false), 0.0001f)
        assertEquals(1f, aspectRatioForOrientation(2, false), 0.0001f)
        assertEquals(21f / 9f, aspectRatioForOrientation(3, false), 0.0001f)

        // Physical landscape reverses height/width. CameraScreen passes this selected
        // ratio into the crop pipeline; null is reserved for callers that request no crop.
        assertEquals(9f / 16f, aspectRatioForOrientation(0, true), 0.0001f)
        assertEquals(3f / 4f, aspectRatioForOrientation(1, true), 0.0001f)
        assertEquals(1f, aspectRatioForOrientation(2, true), 0.0001f)
        assertEquals(9f / 21f, aspectRatioForOrientation(3, true), 0.0001f)
    }

    @Test
    fun portraitThreeByFourUsesAWholeHeightAndCenteredWidthCrop() {
        val geometry = PhotoOutputGeometry.calculate(4000, 3000, 60_000_000L, 4f / 3f)
        assertEquals(2250, geometry.sourceWidth)
        assertEquals(3000, geometry.sourceHeight)
        assertEquals(875, geometry.sourceLeft)
        assertEquals(0, geometry.sourceTop)
        assertTrue(abs(geometry.outputWidth.toDouble() / geometry.outputHeight - 3.0 / 4.0) < 0.001)
    }

    @Test
    fun portraitSquareUsesACenteredCrop() {
        val geometry = PhotoOutputGeometry.calculate(4000, 3000, 60_000_000L, 1f)
        assertEquals(3000, geometry.sourceWidth)
        assertEquals(3000, geometry.sourceHeight)
        assertEquals(500, geometry.sourceLeft)
        assertEquals(0, geometry.sourceTop)
        assertTrue(abs(geometry.outputWidth.toDouble() / geometry.outputHeight - 1.0) < 0.001)
    }

    @Test
    fun portraitNineBySixteenUsesTheCorrectHeightOverWidthRatio() {
        val geometry = PhotoOutputGeometry.calculate(4000, 3000, 60_000_000L, 16f / 9f)
        assertEquals(1687, geometry.sourceWidth)
        assertEquals(3000, geometry.sourceHeight)
        assertTrue(abs(geometry.outputWidth.toDouble() / geometry.outputHeight - 9.0 / 16.0) < 0.001)
        assertTrue(abs(geometry.sourceWidth.toDouble() / geometry.sourceHeight - 9.0 / 16.0) < 0.001)
    }

    @Test
    fun portraitNineByTwentyOneUsesTheCorrectHeightOverWidthRatio() {
        val geometry = PhotoOutputGeometry.calculate(4000, 3000, 60_000_000L, 21f / 9f)
        assertEquals(1286, geometry.sourceWidth)
        assertEquals(3000, geometry.sourceHeight)
        assertTrue(abs(geometry.outputWidth.toDouble() / geometry.outputHeight - 9.0 / 21.0) < 0.001)
        assertTrue(abs(geometry.sourceWidth.toDouble() / geometry.sourceHeight - 9.0 / 21.0) < 0.001)
    }

    @Test
    fun landscapeNullRatioPreservesTheEntireSourceWithoutCropping() {
        val geometry = PhotoOutputGeometry.calculate(4000, 3000, 60_000_000L, null)
        assertEquals(0, geometry.sourceLeft)
        assertEquals(0, geometry.sourceTop)
        assertEquals(4000, geometry.sourceWidth)
        assertEquals(3000, geometry.sourceHeight)
        assertTrue(abs(geometry.outputWidth.toDouble() / geometry.outputHeight - 4.0 / 3.0) < 0.001)
        assertTrue(geometry.outputPixels <= 60_000_000L)
        assertTrue(geometry.outputPixels >= 59_700_000L)
    }

    @Test
    fun allPortraitRatiosCropToTheRequestedOutputWithoutBackdropOrPadding() {
        val ratios = listOf(4f / 3f, 1f, 16f / 9f, 21f / 9f)
        ratios.forEach { heightOverWidth ->
            val geometry = PhotoOutputGeometry.calculate(4000, 3000, 50_000_000L, heightOverWidth)
            val expectedWidthOverHeight = 1.0 / heightOverWidth
            assertTrue(geometry.outputPixels <= 50_000_000L)
            assertTrue(geometry.outputPixels >= 49_750_000L)
            assertTrue(abs(geometry.outputWidth.toDouble() / geometry.outputHeight - expectedWidthOverHeight) < 0.001)
            assertTrue(geometry.sourceLeft >= 0 && geometry.sourceTop >= 0)
            assertTrue(geometry.sourceLeft + geometry.sourceWidth <= 4000)
            assertTrue(geometry.sourceTop + geometry.sourceHeight <= 3000)
            assertTrue(abs(geometry.sourceWidth.toDouble() / geometry.sourceHeight - expectedWidthOverHeight) < 0.001)
        }
    }

    @Test
    fun lunaDreamOutputTargetsReachTheRequestedMegapixelBudgets() {
        val luna15 = PhotoOutputGeometry.calculate(4000, 3000, 50_000_000L, 4f / 3f)
        val luna20 = PhotoOutputGeometry.calculate(4000, 3000, 150_000_000L, 4f / 3f)
        val luna25 = PhotoOutputGeometry.calculate(4000, 3000, 150_000_000L, 4f / 3f)
        val luna30 = PhotoOutputGeometry.calculate(4000, 3000, 200_000_000L, 4f / 3f)

        assertTrue(luna15.outputPixels <= 50_000_000L)
        assertTrue(luna15.outputPixels >= 49_750_000L)
        assertTrue(luna20.outputPixels <= 150_000_000L)
        assertTrue(luna20.outputPixels >= 149_250_000L)
        assertTrue(luna25.outputPixels <= 150_000_000L)
        assertTrue(luna25.outputPixels >= 149_250_000L)
        assertTrue(luna30.outputPixels <= 200_000_000L)
        assertTrue(luna30.outputPixels >= 199_000_000L)
        assertEquals(luna20.outputPixels, luna25.outputPixels)
        assertTrue(luna30.outputPixels > luna20.outputPixels)
    }

    @Test
    fun missingOrInvalidRatioPreservesTheSourceAspectRatio() {
        val default = PhotoOutputGeometry.calculate(4000, 3000, 60_000_000L)
        val invalid = PhotoOutputGeometry.calculate(4000, 3000, 60_000_000L, Float.NaN)
        assertEquals(4.0 / 3.0, default.requestedWidthOverHeight, 0.000001)
        assertEquals(4.0 / 3.0, invalid.requestedWidthOverHeight, 0.000001)
        assertEquals(0, invalid.sourceLeft)
        assertEquals(0, invalid.sourceTop)
        assertEquals(4000, invalid.sourceWidth)
        assertEquals(3000, invalid.sourceHeight)
    }

    @Test
    fun landscapeSelectedRatiosCropToTheRequestedWideFrame() {
        val ratios = listOf(
            (3f / 4f) to (4.0 / 3.0),
            (9f / 16f) to (16.0 / 9.0),
            (9f / 21f) to (21.0 / 9.0),
            1f to 1.0
        )
        ratios.forEach { (heightOverWidth, expectedWidthOverHeight) ->
            val geometry = PhotoOutputGeometry.calculate(
                sourceWidth = 4000,
                sourceHeight = 3000,
                targetPixels = 12_000_000L,
                targetHeightOverWidth = heightOverWidth
            )
            assertTrue(
                "output ratio for target h/w=$heightOverWidth",
                abs(geometry.outputWidth.toDouble() / geometry.outputHeight - expectedWidthOverHeight) < 0.001
            )
            assertTrue(
                "source crop ratio for target h/w=$heightOverWidth",
                abs(geometry.sourceWidth.toDouble() / geometry.sourceHeight - expectedWidthOverHeight) < 0.001
            )
            assertTrue(geometry.sourceLeft >= 0 && geometry.sourceTop >= 0)
            assertTrue(geometry.sourceLeft + geometry.sourceWidth <= 4000)
            assertTrue(geometry.sourceTop + geometry.sourceHeight <= 3000)
            assertTrue(abs(geometry.sourceLeft - (4000 - geometry.sourceWidth) / 2) <= 1)
            assertTrue(abs(geometry.sourceTop - (3000 - geometry.sourceHeight) / 2) <= 1)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDimensionsAreRejected() {
        PhotoOutputGeometry.calculate(0, 3000, 60_000_000L, 16f / 9f)
    }
}

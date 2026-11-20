package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DigitalZoomProcessorTest {
    @Test
    fun twoTimesDigitalCropIsCenteredAndPreservesOutputAspectRatio() {
        val crop = calculateDigitalZoomCropBounds(4000, 3000, 2f)
        assertNotNull(crop)
        crop!!
        assertEquals(1000, crop.left)
        assertEquals(750, crop.top)
        assertEquals(3000, crop.right)
        assertEquals(2250, crop.bottom)
        assertEquals(2000, crop.width)
        assertEquals(1500, crop.height)
    }

    @Test
    fun cropGeometrySupportsTheMaximumTwoTimesDigitalStage() {
        val crop = calculateDigitalZoomCropBounds(8064, 6048, 2f)
        assertNotNull(crop)
        crop!!
        assertEquals(4032, crop.width)
        assertEquals(3024, crop.height)
        assertEquals(2016, crop.left)
        assertEquals(1512, crop.top)
    }

    @Test
    fun invalidDimensionsOrNonDigitalRatiosAreRejected() {
        assertNull(calculateDigitalZoomCropBounds(0, 3000, 2f))
        assertNull(calculateDigitalZoomCropBounds(3000, 3000, 1f))
        assertNull(calculateDigitalZoomCropBounds(3000, 3000, Float.NaN))
        assertNull(calculateDigitalZoomCropBounds(3000, 3000, Float.POSITIVE_INFINITY))
    }

    @Test
    fun extremeCropKeepsAValidMinimumDecodeRegion() {
        val crop = calculateDigitalZoomCropBounds(8064, 6048, 100f)
        assertNotNull(crop)
        crop!!
        assertEquals(80, crop.width)
        assertEquals(60, crop.height)
        assertEquals(3992, crop.left)
        assertEquals(2994, crop.top)
    }
}

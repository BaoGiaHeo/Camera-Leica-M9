package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoOutputMemoryPolicyTest {
    @Test
    fun keepsRequestedTargetWhenHeapBudgetIsEnough() {
        assertEquals(
            100_000_000L,
            adaptivePhotoOutputTargetPixels(
                requestedPixels = 100_000_000L,
                usableHeapBytes = 300_000_000L,
                minimumPixels = 50_000_000L
            )
        )
    }

    @Test
    fun reducesTargetToSafeRgb565HeapBudget() {
        assertEquals(
            72_000_000L,
            adaptivePhotoOutputTargetPixels(
                requestedPixels = 200_000_000L,
                usableHeapBytes = 160_000_000L,
                minimumPixels = 50_000_000L
            )
        )
    }

    @Test
    fun rejectsAdaptiveTargetBelowMinimum() {
        assertNull(
            adaptivePhotoOutputTargetPixels(
                requestedPixels = 200_000_000L,
                usableHeapBytes = 80_000_000L,
                minimumPixels = 50_000_000L
            )
        )
    }

    @Test
    fun neverReturnsMorePixelsThanRequested() {
        assertEquals(
            50_000_000L,
            adaptivePhotoOutputTargetPixels(
                requestedPixels = 50_000_000L,
                usableHeapBytes = 2_000_000_000L,
                minimumPixels = 50_000_000L
            )
        )
    }
}

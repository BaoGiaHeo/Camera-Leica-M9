package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoInspectorGeometryTest {
    @Test
    fun normalAndHalfTurnKeepPixelDimensions() {
        assertEquals(PhotoInspectorPixelDimensions(4000, 3000), PhotoInspectorGeometry.fromEncoded(4000, 3000, 1))
        assertEquals(PhotoInspectorPixelDimensions(4000, 3000), PhotoInspectorGeometry.fromEncoded(4000, 3000, 3))
    }

    @Test
    fun quarterTurnsSwapPixelDimensions() {
        for (orientation in listOf(5, 6, 7, 8)) {
            assertEquals(
                "EXIF orientation $orientation",
                PhotoInspectorPixelDimensions(3000, 4000),
                PhotoInspectorGeometry.fromEncoded(4000, 3000, orientation)
            )
        }
    }

    @Test
    fun invalidEncodedDimensionsAreRejected() {
        assertNull(PhotoInspectorGeometry.fromEncoded(0, 3000, 1))
        assertNull(PhotoInspectorGeometry.fromEncoded(4000, -1, 6))
    }
}

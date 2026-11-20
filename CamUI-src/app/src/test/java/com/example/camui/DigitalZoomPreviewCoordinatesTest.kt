package com.example.camui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class DigitalZoomPreviewCoordinatesTest {
    @Test
    fun oneTimesZoomLeavesPointsAndSizeUnchanged() {
        val point = Offset(0.2f, 0.7f)
        val size = Offset(0.3f, 0.4f)
        assertEquals(point, cameraPointToDigitalPreview(point, 1f))
        assertEquals(point, digitalPreviewPointToCamera(point, 1f))
        assertEquals(size, cameraBoxSizeToDigitalPreview(size, 1f))
    }

    @Test
    fun digitalCropMovesDisplayedPointsOutwardFromImageCentre() {
        assertEquals(
            Offset(0.1f, 0.9f),
            cameraPointToDigitalPreview(Offset(0.3f, 0.7f), 2f)
        )
        assertEquals(Offset(0.5f, 0.5f), cameraPointToDigitalPreview(Offset(0.5f, 0.5f), 4f))
    }

    @Test
    fun inverseMappingReturnsTheOriginalCameraPoint() {
        val source = Offset(0.32f, 0.64f)
        val displayed = cameraPointToDigitalPreview(source, 2.4f)
        assertEquals(source.x, digitalPreviewPointToCamera(displayed, 2.4f).x, 0.0001f)
        assertEquals(source.y, digitalPreviewPointToCamera(displayed, 2.4f).y, 0.0001f)
    }

    @Test
    fun pointsAndFaceBoxesStayInsideTheViewfinderBounds() {
        assertEquals(Offset(0f, 1f), cameraPointToDigitalPreview(Offset(0f, 1f), 3f))
        assertEquals(Offset(0f, 1f), digitalPreviewPointToCamera(Offset(-1f, 2f), 2f))
        assertEquals(Offset(0.9f, 1.2f), cameraBoxSizeToDigitalPreview(Offset(0.3f, 0.4f), 3f))
    }

    @Test
    fun invalidCropRatioFallsBackToNativePreviewCoordinates() {
        val point = Offset(0.15f, 0.85f)
        assertEquals(point, cameraPointToDigitalPreview(point, Float.NaN))
        assertEquals(point, digitalPreviewPointToCamera(point, 0.2f))
    }
}

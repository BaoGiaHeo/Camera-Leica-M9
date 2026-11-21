package com.example.camui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class DigitalZoomPreviewCoordinatesTest {
    private fun assertOffsetNear(expected: Offset, actual: Offset, tolerance: Float = 0.0001f) {
        assertEquals(expected.x, actual.x, tolerance)
        assertEquals(expected.y, actual.y, tolerance)
    }

    @Test
    fun oneTimesZoomLeavesPointsAndSizeUnchanged() {
        val point = Offset(0.2f, 0.7f)
        val size = Offset(0.3f, 0.4f)
        assertOffsetNear(point, cameraPointToDigitalPreview(point, 1f))
        assertOffsetNear(point, digitalPreviewPointToCamera(point, 1f))
        assertOffsetNear(size, cameraBoxSizeToDigitalPreview(size, 1f))
    }

    @Test
    fun digitalCropMovesDisplayedPointsOutwardFromImageCentre() {
        assertOffsetNear(
            Offset(0.1f, 0.9f),
            cameraPointToDigitalPreview(Offset(0.3f, 0.7f), 2f)
        )
        assertOffsetNear(Offset(0.5f, 0.5f), cameraPointToDigitalPreview(Offset(0.5f, 0.5f), 4f))
    }

    @Test
    fun inverseMappingReturnsTheOriginalCameraPoint() {
        val source = Offset(0.32f, 0.64f)
        val displayed = cameraPointToDigitalPreview(source, 2.4f)
        assertOffsetNear(source, digitalPreviewPointToCamera(displayed, 2.4f))
    }

    @Test
    fun pointsAndFaceBoxesStayInsideTheViewfinderBounds() {
        assertOffsetNear(Offset(0f, 1f), cameraPointToDigitalPreview(Offset(0f, 1f), 3f))
        assertOffsetNear(Offset(0f, 1f), digitalPreviewPointToCamera(Offset(-1f, 2f), 2f))
        assertOffsetNear(Offset(0.9f, 1.2f), cameraBoxSizeToDigitalPreview(Offset(0.3f, 0.4f), 3f))
    }

    @Test
    fun invalidCropRatioFallsBackToNativePreviewCoordinates() {
        val point = Offset(0.15f, 0.85f)
        assertOffsetNear(point, cameraPointToDigitalPreview(point, Float.NaN))
        assertOffsetNear(point, digitalPreviewPointToCamera(point, 0.2f))
    }
}

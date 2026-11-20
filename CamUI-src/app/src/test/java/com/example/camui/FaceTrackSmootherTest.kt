package com.example.camui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceTrackSmootherTest {
    @Test
    fun waitsThreeFramesBeforeShowingAndEightMissesBeforeHiding() {
        val tracker = FaceTrackSmoother()
        val center = Offset(0.5f, 0.5f)
        val size = Offset(0.2f, 0.3f)

        assertNull(tracker.onFaceDetected(center, size, null))
        assertNull(tracker.onFaceDetected(center, size, null))
        assertEquals(center, tracker.onFaceDetected(center, size, null)?.center)

        repeat(7) { assertFalse(tracker.onNoFace()) }
        assertEquals(center, tracker.current()?.center)
        assertTrue(tracker.onNoFace())
        assertNull(tracker.current())
    }

    @Test
    fun smoothsFaceMovementAndUsesTheEyeCenterWhenProvided() {
        val tracker = FaceTrackSmoother()
        val initial = Offset(0.4f, 0.4f)
        repeat(2) { tracker.onFaceDetected(initial, Offset(0.2f, 0.3f), null) }
        val first = tracker.onFaceDetected(initial, Offset(0.2f, 0.3f), Offset(0.4f, 0.35f))
        assertEquals(initial, first?.center)
        assertEquals(Offset(0.4f, 0.35f), first?.eyeCenter)

        val moved = tracker.onFaceDetected(Offset(0.8f, 0.6f), Offset(0.4f, 0.5f), Offset(0.8f, 0.55f))
        assertEquals(0.552f, moved!!.center.x, 0.0001f)
        assertEquals(0.476f, moved.center.y, 0.0001f)
        assertEquals(0.276f, moved.size.x, 0.0001f)
        assertEquals(0.552f, moved.eyeCenter!!.x, 0.0001f)
        assertEquals(0.426f, moved.eyeCenter.y, 0.0001f)
    }

    @Test
    fun faceSelectionUsesTapTargetThenTrackingContinuityAndLargestFallback() {
        data class Face(val name: String)
        val large = FaceTrackCandidate(
            value = Face("large"),
            center = Offset(0.82f, 0.5f),
            size = Offset(0.28f, 0.36f),
            area = 0.1008f
        )
        val small = FaceTrackCandidate(
            value = Face("small"),
            center = Offset(0.24f, 0.5f),
            size = Offset(0.12f, 0.16f),
            area = 0.0192f,
            trackingId = 7
        )
        assertEquals(
            "small",
            chooseFaceTrackCandidate(listOf(large, small), null, Offset(0.25f, 0.5f))?.value?.name
        )
        assertEquals(
            "small",
            chooseFaceTrackCandidate(
                listOf(large, small.copy(center = Offset(0.4f, 0.5f))),
                FaceTrackSnapshot(Offset(0.4f, 0.5f), small.size, null, trackingId = 7),
                null
            )?.value?.name
        )
        assertEquals(
            "large",
            chooseFaceTrackCandidate(listOf(large, small), null, null)?.value?.name
        )
    }

    @Test
    fun faceIouPrefersTheSameBoundingBoxAcrossSmallMovement() {
        val overlap = faceIntersectionOverUnion(
            Offset(0.5f, 0.5f),
            Offset(0.2f, 0.3f),
            Offset(0.52f, 0.51f),
            Offset(0.2f, 0.3f)
        )
        val separate = faceIntersectionOverUnion(
            Offset(0.5f, 0.5f),
            Offset(0.2f, 0.3f),
            Offset(0.9f, 0.9f),
            Offset(0.2f, 0.3f)
        )
        assertTrue(overlap > 0.5f)
        assertEquals(0f, separate, 0.0001f)
    }

    @Test
    fun mapsImageProxyCropIntoMlKitRotatedCoordinates() {
        val zero = orientImageCrop(640, 480, 40, 20, 600, 460, 0)
        assertEquals(40, zero?.left)
        assertEquals(20, zero?.top)
        assertEquals(560, zero?.width)
        assertEquals(440, zero?.height)

        val ninety = orientImageCrop(640, 480, 40, 20, 600, 460, 90)
        assertEquals(20, ninety?.left)
        assertEquals(40, ninety?.top)
        assertEquals(440, ninety?.width)
        assertEquals(560, ninety?.height)

        val oneEighty = orientImageCrop(640, 480, 40, 20, 600, 460, 180)
        assertEquals(40, oneEighty?.left)
        assertEquals(20, oneEighty?.top)
        assertEquals(560, oneEighty?.width)
        assertEquals(440, oneEighty?.height)

        val twoSeventy = orientImageCrop(640, 480, 40, 20, 600, 460, 270)
        assertEquals(20, twoSeventy?.left)
        assertEquals(40, twoSeventy?.top)
        assertEquals(440, twoSeventy?.width)
        assertEquals(560, twoSeventy?.height)
        assertEquals(null, orientImageCrop(640, 480, 20, 30, 20, 40, 90))
    }

    @Test
    fun rotatesAndMirrorsCoordinatesForAllSensorOrientations() {
        val rotated90 = rotateFacePoint(0.2f, 0.8f, 90, false)
        assertEquals(0.2f, rotated90.x, 0.0001f)
        assertEquals(0.2f, rotated90.y, 0.0001f)
        val rotated180 = rotateFacePoint(0.2f, 0.8f, 180, false)
        assertEquals(0.8f, rotated180.x, 0.0001f)
        assertEquals(0.2f, rotated180.y, 0.0001f)
        val rotated270 = rotateFacePoint(0.2f, 0.8f, 270, false)
        assertEquals(0.8f, rotated270.x, 0.0001f)
        assertEquals(0.8f, rotated270.y, 0.0001f)
        val mirrored90 = rotateFacePoint(0.2f, 0.8f, 90, true)
        assertEquals(0.8f, mirrored90.x, 0.0001f)
        assertEquals(0.2f, mirrored90.y, 0.0001f)
        val rotatedSize = rotateFaceSize(0.2f, 0.3f, 90)
        assertEquals(0.3f, rotatedSize.x, 0.0001f)
        assertEquals(0.2f, rotatedSize.y, 0.0001f)
        val unchangedSize = rotateFaceSize(0.2f, 0.3f, 0)
        assertEquals(0.2f, unchangedSize.x, 0.0001f)
        assertEquals(0.3f, unchangedSize.y, 0.0001f)
    }
}

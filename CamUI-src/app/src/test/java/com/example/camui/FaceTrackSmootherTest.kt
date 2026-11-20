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
        assertNull(tracker.current())
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
        assertEquals(0.552f, moved.size.x, 0.0001f)
        assertEquals(0.552f, moved.eyeCenter!!.x, 0.0001f)
    }

    @Test
    fun rotatesAndMirrorsCoordinatesForAllSensorOrientations() {
        assertEquals(Offset(0.8f, 0.2f), rotateFacePoint(0.2f, 0.8f, 90, false))
        assertEquals(Offset(0.8f, 0.2f), rotateFacePoint(0.2f, 0.8f, 180, false).let {
            rotateFacePoint(it.x, it.y, 180, false)
        })
        assertEquals(Offset(0.8f, 0.8f), rotateFacePoint(0.2f, 0.8f, 270, false))
        assertEquals(Offset(0.2f, 0.2f), rotateFacePoint(0.2f, 0.8f, 90, true))
        assertEquals(Offset(0.3f, 0.2f), rotateFaceSize(0.2f, 0.3f, 90))
        assertEquals(Offset(0.2f, 0.3f), rotateFaceSize(0.2f, 0.3f, 0))
    }
}

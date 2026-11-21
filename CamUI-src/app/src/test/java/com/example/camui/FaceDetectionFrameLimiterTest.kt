package com.example.camui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceDetectionFrameLimiterTest {
    @Test
    fun firstFrameRunsImmediately() {
        assertTrue(shouldRunFaceDetectionFrame(1_000L, 0L))
    }

    @Test
    fun defaultLimitDoesNotRunMoreOftenThanTwentyFourFramesPerSecond() {
        assertFalse(shouldRunFaceDetectionFrame(1_041L, 1_000L))
        assertTrue(shouldRunFaceDetectionFrame(1_042L, 1_000L))
    }

    @Test
    fun slowDevicesCanUseAFifteenFramePerSecondLimit() {
        assertFalse(shouldRunFaceDetectionFrame(1_066L, 1_000L, maxFramesPerSecond = 15))
        assertTrue(shouldRunFaceDetectionFrame(1_067L, 1_000L, maxFramesPerSecond = 15))
    }

    @Test
    fun invalidFpsIsClampedAndNonMonotonicTimeIsRejected() {
        assertFalse(shouldRunFaceDetectionFrame(1_010L, 1_000L, maxFramesPerSecond = 100))
        assertFalse(shouldRunFaceDetectionFrame(900L, 1_000L))
        assertTrue(shouldRunFaceDetectionFrame(1_017L, 1_000L, maxFramesPerSecond = 60))
    }
}

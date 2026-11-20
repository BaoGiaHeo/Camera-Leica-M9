package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CameraFrameMetricsDiagnosticsTest {
    @Test
    fun computesJankAgainstTheSelectedDisplayRefreshBudget() {
        val frames = listOf(
            CameraFrameSample(8_000_000L, 1_000_000L),
            CameraFrameSample(8_200_000L, 1_500_000L),
            CameraFrameSample(9_000_000L, 2_000_000L),
            CameraFrameSample(20_000_000L, 2_500_000L)
        )
        val atSixty = summarizeCameraFrameMetrics(frames, 60f, 66_666_666L, 1)!!
        val atOneTwenty = summarizeCameraFrameMetrics(frames, 120f, 33_333_333L, 0)!!
        assertEquals(1, atSixty.jankyFrameCount)
        assertEquals(25.0, atSixty.jankyPercent, 0.001)
        assertEquals(2, atOneTwenty.jankyFrameCount)
        assertEquals(50.0, atOneTwenty.jankyPercent, 0.001)
        assertEquals(1, atSixty.droppedCallbackReports)
        assertEquals(2.5, atSixty.p95DrawMillis, 0.001)
    }

    @Test
    fun ignoresAnEmptySamplingWindowAndUsesSafeRefreshFallback() {
        assertNull(summarizeCameraFrameMetrics(emptyList(), 120f, 5_000_000_000L))
        val one = summarizeCameraFrameMetrics(
            listOf(CameraFrameSample(10_000_000L, 1_000_000L)),
            Float.NaN,
            1_000_000_000L
        )
        assertNotNull(one)
        assertEquals(60f, one!!.refreshRateHz)
        assertEquals(1f, one.measuredFramesPerSecond, 0.001f)
    }
}

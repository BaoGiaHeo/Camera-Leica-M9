package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraMultiFrameProgressTest {
    @Test
    fun captureStageWinsWhileFramesAreStillBeingCollected() {
        assertEquals(
            MultiFrameStage.CAPTURE,
            resolveMultiFrameStage("LunaDream 1.5: thu khung 4/5", 18, capturing = true)
        )
        assertEquals(
            MultiFrameStage.CAPTURE,
            resolveMultiFrameStage("NightShot: thu khung 12/30 · hàng đợi 2", 20, capturing = true)
        )
    }

    @Test
    fun processingLabelsResolveToExpectedPhases() {
        assertEquals(
            MultiFrameStage.ALIGN,
            resolveMultiFrameStage("LunaDream 1.5: căn chỉnh và khử nhiễu", 27, capturing = false)
        )
        assertEquals(
            MultiFrameStage.MERGE,
            resolveMultiFrameStage("NightShotMode: căn chỉnh và gộp 30 khung", 44, capturing = false)
        )
        assertEquals(
            MultiFrameStage.TONE,
            resolveMultiFrameStage("LunaDream 2.0 Render · khử nhiễu và phục hồi chi tiết", 83, capturing = false)
        )
        assertEquals(
            MultiFrameStage.SAVE,
            resolveMultiFrameStage("Đang đóng watermark ở độ phân giải cuối", 90, capturing = false)
        )
    }

    @Test
    fun elapsedTimeUsesClockStyleAndNeverGoesNegative() {
        assertEquals("0:00", multiFrameElapsedLabel(0))
        assertEquals("0:04", multiFrameElapsedLabel(4))
        assertEquals("1:05", multiFrameElapsedLabel(65))
        assertEquals("0:00", multiFrameElapsedLabel(-8))
    }

    @Test
    fun thermalAndBatteryTemperatureUseSeparateThresholds() {
        assertEquals(0xFF26302F, multiFrameThermalColor(0))
        assertEquals(0xFFE5A13B, multiFrameThermalColor(1))
        assertEquals(0xFFFF4A3D, multiFrameThermalColor(4))

        assertEquals(0xFF55D58A, multiFrameTemperatureColor(39))
        assertEquals(0xFFE5A13B, multiFrameTemperatureColor(40))
        assertEquals(0xFFFF4A3D, multiFrameTemperatureColor(44))
    }
}

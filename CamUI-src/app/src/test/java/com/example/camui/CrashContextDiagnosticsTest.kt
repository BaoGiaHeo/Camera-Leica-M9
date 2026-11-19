package com.example.camui

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashContextDiagnosticsTest {
    @After
    fun reset() {
        CrashContextDiagnostics.resetForTests()
    }

    @Test
    fun reportIncludesRoutePanelModeRatioOrientationLanguageAndStyle() {
        CrashContextDiagnostics.updateRoute("settings")
        CrashContextDiagnostics.updateCameraState(
            mode = "Pro",
            panel = "FILM_FILTERS",
            aspectRatio = "ratio_index=2",
            orientation = "LANDSCAPE",
            language = "vi",
            uiStyle = "LCD"
        )

        val report = CrashContextDiagnostics.snapshot()

        assertTrue(report.contains("Route: settings"))
        assertTrue(report.contains("Panel: FILM_FILTERS"))
        assertTrue(report.contains("Mode: Pro"))
        assertTrue(report.contains("Aspect ratio: ratio_index=2"))
        assertTrue(report.contains("Orientation: LANDSCAPE"))
        assertTrue(report.contains("Language: vi"))
        assertTrue(report.contains("UI style: LCD"))
    }

    @Test
    fun eventRingBufferKeepsOnlyTheMostRecentTwentyEntries() {
        repeat(25) { CrashContextDiagnostics.recordUiEvent("EVENT_$it") }

        val report = CrashContextDiagnostics.snapshot()

        assertFalse(report.contains("EVENT_0"))
        assertFalse(report.contains("EVENT_4"))
        assertTrue(report.contains("EVENT_5"))
        assertTrue(report.contains("EVENT_24"))
    }
}

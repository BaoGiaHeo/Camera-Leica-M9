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
    fun eventRingBufferKeepsOnlyTheMostRecentFiftyEntries() {
        repeat(65) { CrashContextDiagnostics.recordUiEvent("EVENT_$it") }

        val report = CrashContextDiagnostics.snapshot()

        assertFalse(report.contains("EVENT_14"))
        assertTrue(report.contains("EVENT_15"))
        assertTrue(report.contains("EVENT_64"))
        assertFalse(report.contains("EVENT_0"))
    }
}

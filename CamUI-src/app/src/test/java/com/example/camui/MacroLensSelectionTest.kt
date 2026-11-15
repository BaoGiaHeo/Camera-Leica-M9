package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MacroLensSelectionTest {
    private val main = MacroLensSpec(
        id = "main",
        facing = "BACK",
        equivalent35Mm = 26f,
        minimumFocusDiopter = 2f
    )
    private val macro = MacroLensSpec(
        id = "macro",
        facing = "BACK",
        equivalent35Mm = 50f,
        minimumFocusDiopter = 10f
    )

    @Test
    fun selectsRearCloseFocusLensUsingCamera2BackFacingValue() {
        val candidate = MacroLensSelection.find(
            logicalMainFocusDiopter = 2f,
            lenses = listOf(main, macro),
            minZoom = 1f,
            maxZoom = 5f
        )

        assertEquals("macro", candidate?.physicalCameraId)
        assertEquals(50f / 26f, candidate?.zoomRatio ?: 0f, 0.0001f)
        assertEquals(10f, candidate?.closestFocusDiopter ?: 0f, 0.0001f)
    }

    @Test
    fun frontFacingLensIsNeverClassifiedAsRearMacro() {
        val frontMacro = macro.copy(facing = "FRONT")
        assertNull(
            MacroLensSelection.find(
                logicalMainFocusDiopter = 2f,
                lenses = listOf(main, frontMacro),
                minZoom = 1f,
                maxZoom = 5f
            )
        )
    }

    @Test
    fun lensOutsideSelectableZoomRangeIsRejected() {
        assertNull(
            MacroLensSelection.find(
                logicalMainFocusDiopter = 2f,
                lenses = listOf(main, macro),
                minZoom = 1f,
                maxZoom = 1.5f
            )
        )
    }

    @Test
    fun lensMustHaveMateriallyCloserFocusDistance() {
        val ordinaryTele = macro.copy(minimumFocusDiopter = 2.2f)
        assertNull(
            MacroLensSelection.find(
                logicalMainFocusDiopter = 2f,
                lenses = listOf(main, ordinaryTele),
                minZoom = 1f,
                maxZoom = 5f
            )
        )
    }

    @Test
    fun invalidMetadataCannotProduceCandidate() {
        val invalid = macro.copy(equivalent35Mm = Float.NaN)
        assertNull(
            MacroLensSelection.find(
                logicalMainFocusDiopter = 2f,
                lenses = listOf(main, invalid),
                minZoom = 1f,
                maxZoom = 5f
            )
        )
    }
}

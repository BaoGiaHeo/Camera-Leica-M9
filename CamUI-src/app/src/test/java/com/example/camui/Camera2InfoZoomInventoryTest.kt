package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Camera2InfoZoomInventoryTest {

    @Test
    fun independentRearTelephotoIsDetectedWhileMainCameraIsActive() {
        val mainWide = camera(id = "0", equivalent35Mm = 26f)
        val independentTele = camera(id = "2", equivalent35Mm = 78f)

        val assessment = Camera2Info.opticalZoomAssessment(
            logical = mainWide,
            physical = emptyList(),
            inventory = listOf(mainWide, independentTele)
        )

        assertTrue(assessment.hasOpticalTelephoto)
        assertEquals(3f, assessment.opticalRatio!!, 0.001f)
        assertEquals(26f, assessment.mainEquivalent35Mm!!, 0.001f)
        assertEquals(78f, assessment.longestEquivalent35Mm!!, 0.001f)
    }

    @Test
    fun frontTelephotoDoesNotEnableRearOpticalZoom() {
        val mainWide = camera(id = "0", equivalent35Mm = 26f)
        val frontTele = camera(id = "1", equivalent35Mm = 104f, facing = "FRONT")

        val assessment = Camera2Info.opticalZoomAssessment(
            logical = mainWide,
            physical = emptyList(),
            inventory = listOf(mainWide, frontTele)
        )

        assertFalse(assessment.hasOpticalTelephoto)
        assertEquals(1f, assessment.opticalRatio!!, 0.001f)
    }

    @Test
    fun independentTenTimesPeriscopeIsDetectedFromFullInventory() {
        val mainWide = camera(id = "0", equivalent35Mm = 26f)
        val periscope = camera(id = "3", equivalent35Mm = 260f)

        val assessment = Camera2Info.opticalZoomAssessment(
            logical = mainWide,
            physical = emptyList(),
            inventory = listOf(mainWide, periscope)
        )

        assertTrue(assessment.hasOpticalTelephoto)
        assertEquals(10f, assessment.opticalRatio!!, 0.001f)
    }

    private fun camera(
        id: String,
        equivalent35Mm: Float,
        facing: String = "BACK"
    ) = CamDetails(
        id = id,
        parentId = null,
        facing = facing,
        hardwareLevel = "FULL",
        isLogicalMulti = false,
        physicalIds = emptySet(),
        focalLengthsMm = listOf(4.5f),
        apertures = emptyList(),
        eq35Mm = equivalent35Mm,
        sensorSizeMm = null,
        pixelArray = null,
        isoRange = null,
        exposureRange = null,
        evRange = null,
        maxDigitalZoom = 10f,
        controlZoomRatioMin = 1f,
        controlZoomRatioMax = 10f,
        minFocusDiopter = null,
        hasFlash = false,
        supportsRaw = false,
        hasOis = false,
        sensorOrientation = 90,
        eq35MmMaximum = equivalent35Mm
    )
}

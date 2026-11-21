package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class WatermarkMetadataPolicyTest {
    @Test
    fun exposureMasterSwitchDoesNotHideIndividuallyEnabledSupplementalFields() {
        assertEquals(
            "Classic Chrome  ·  NIGHT  ·  © Baoaka06  ·  12.0 MP",
            combineWatermarkMetadataLines(
                exposureFields = listOf("24 mm", "F1.8", "1/125s", "ISO 400", "+0.3 EV"),
                supplementalFields = listOf("Classic Chrome", "NIGHT", "© Baoaka06", "12.0 MP"),
                showExposureFields = false
            )
        )
    }

    @Test
    fun exposureFieldsAreIncludedOnlyWhenTheMasterSwitchIsEnabled() {
        assertEquals(
            "24 mm  ·  F1.8  ·  ISO 400  ·  FILTER",
            combineWatermarkMetadataLines(
                exposureFields = listOf("24 mm", "F1.8", "ISO 400"),
                supplementalFields = listOf("FILTER"),
                showExposureFields = true
            )
        )
    }

    @Test
    fun missingFieldsDoNotCreateNullOrPlaceholderLabels() {
        assertEquals(
            "NIGHT  ·  12.0 MP",
            combineWatermarkMetadataLines(
                exposureFields = listOf("", " "),
                supplementalFields = listOf("", "NIGHT", "12.0 MP"),
                showExposureFields = true
            )
        )
    }

    @Test
    fun dataCardAndExposureDialRespectTheGlobalExifAndIndividualSwitches() {
        val values = WatermarkExposureValues(
            focal = "24 mm",
            aperture = "F1.8",
            shutter = "1/125s",
            iso = "200"
        )
        assertEquals(
            emptyList<Pair<String, String>>(),
            visibleWatermarkDataCardItems(values, false, true, true, true, true)
        )
        assertEquals(
            listOf("APERTURE" to "F1.8", "ISO" to "ISO 200"),
            visibleWatermarkDataCardItems(values, true, false, true, false, true)
        )
        assertEquals(
            emptyList<Pair<String, String>>(),
            visibleWatermarkExposureDialItems(values, false, true, true, true)
        )
        assertEquals(
            listOf("SS" to "1/125s"),
            visibleWatermarkExposureDialItems(values, true, false, true, false)
        )
    }

    @Test
    fun standardLayoutExposureFieldsRespectTheSameGlobalAndPerFieldSwitches() {
        val values = WatermarkExposureValues(
            focal = "24 mm · 35 mm eq.",
            aperture = "F1.8",
            shutter = "1/125s",
            iso = "200"
        )
        assertEquals(
            emptyList<String>(),
            visibleWatermarkExposureFields(values, false, true, true, true, true)
        )
        assertEquals(
            listOf("24 mm · 35 mm eq.", "ISO 200"),
            visibleWatermarkExposureFields(values, true, true, false, false, true)
        )
    }
}

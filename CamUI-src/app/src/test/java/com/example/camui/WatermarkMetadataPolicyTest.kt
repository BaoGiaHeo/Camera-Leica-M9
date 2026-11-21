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
                supplementalFields = listOf(null.orEmpty(), "NIGHT", "12.0 MP"),
                showExposureFields = true
            )
        )
    }
}

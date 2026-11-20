package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatermarkGpsFormatterTest {
    @Test
    fun convertsExifRationalsToDecimalCoordinates() {
        val result = formatWatermarkGpsCoordinates(
            mapOf(
                "GPSLatitude" to "10/1, 30/1, 0/1",
                "GPSLatitudeRef" to "N",
                "GPSLongitude" to "106/1, 45/1, 0/1",
                "GPSLongitudeRef" to "E"
            )
        )
        assertEquals("10.50000, 106.75000", result)
    }

    @Test
    fun appliesSouthAndWestSigns() {
        val result = formatWatermarkGpsCoordinates(
            mapOf(
                "GPSLatitude" to "10/1, 30/1, 0/1",
                "GPSLatitudeRef" to "S",
                "GPSLongitude" to "106/1, 45/1, 0/1",
                "GPSLongitudeRef" to "W"
            )
        )
        assertEquals("-10.50000, -106.75000", result)
    }

    @Test
    fun hidesMissingMalformedOrOutOfRangeCoordinates() {
        assertNull(formatWatermarkGpsCoordinates(emptyMap()))
        assertNull(formatWatermarkGpsCoordinates(mapOf("GPSLatitude" to "1,2", "GPSLongitude" to "2,3")))
        assertNull(formatWatermarkGpsCoordinates(
            mapOf(
                "GPSLatitude" to "91/1,0/1,0/1",
                "GPSLatitudeRef" to "N",
                "GPSLongitude" to "1/1,0/1,0/1",
                "GPSLongitudeRef" to "E"
            )
        ))
    }
}

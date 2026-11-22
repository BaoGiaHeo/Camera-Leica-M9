package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkStudioLayoutTest {
    @Test
    fun studioExposesExactlyTheEightProductionWatermarkLayoutsInOrder() {
        assertEquals(
            listOf("bar", "caption", "gallery", "black", "dot_light", "dot_dark", "split", "palette"),
            watermarkStudioLayouts.map { it.first }
        )
        assertEquals(8, watermarkStudioLayouts.size)
        assertEquals(
            WatermarkLayoutType.values().map { WatermarkLayoutType.preferenceValue(it) },
            watermarkStudioLayouts.map { it.first }
        )
    }

    @Test
    fun everyProductionLayoutHasAVisibleNonEmptyLabel() {
        assertTrue(watermarkStudioLayouts.all { (id, title) ->
            id.isNotBlank() && title.isNotBlank()
        })
    }

    @Test
    fun retiredCardLayoutDoesNotRemainInTheStudioCatalog() {
        assertTrue(watermarkStudioLayouts.none { it.first.contains("card", ignoreCase = true) })
        assertTrue(WatermarkLayoutType.values().none { it.name == "CARD" })
        assertEquals(WatermarkLayoutType.DOT_LIGHT, WatermarkLayoutType.fromPreference("card"))
    }
}

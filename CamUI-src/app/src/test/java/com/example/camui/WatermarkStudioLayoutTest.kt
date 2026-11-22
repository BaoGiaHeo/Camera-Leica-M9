package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkStudioLayoutTest {
    @Test
    fun studioExposesExactlyEightSupportedWatermarkStyles() {
        assertEquals(
            listOf("bar", "caption", "gallery", "black", "dot_light", "dot_dark", "split", "palette"),
            watermarkStudioLayouts.map { it.first }
        )
        assertEquals(8, watermarkStudioLayouts.size)
        assertTrue(watermarkStudioLayouts.none { it.first.contains("card", ignoreCase = true) })
    }

    @Test
    fun everyStudioLayoutHasAVisibleNonEmptyLabel() {
        assertTrue(watermarkStudioLayouts.all { (id, title) ->
            id.isNotBlank() && title.isNotBlank()
        })
    }

    @Test
    fun allWatermarkStylesUseTheSharedSairaFooterFontContract() {
        assertTrue(watermarkStudioLayouts.none { it.first == "lcd_osd" })
        assertTrue(watermarkStudioLayouts.none { it.first == "classic_bar_kk7" })
        assertTrue(watermarkStudioLayouts.none { it.first.contains("card", ignoreCase = true) })
    }
}

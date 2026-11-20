package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkStudioLayoutTest {
    @Test
    fun studioExposesExactlyTheTwelveRequestedLayoutsInTheRequestedOrder() {
        assertEquals(
            listOf(
                "classic_bar",
                "center_stack",
                "data_card",
                "film_strip",
                "polaroid",
                "minimal_overlay",
                "corner_stamp",
                "glass_card",
                "blur_frame",
                "lcd_osd",
                "viewfinder",
                "exposure_dial"
            ),
            watermarkStudioLayouts.map { it.first }
        )
        assertEquals(12, watermarkStudioLayouts.size)
    }

    @Test
    fun everyStudioLayoutHasAVisibleNonEmptyLabel() {
        assertTrue(watermarkStudioLayouts.all { (id, title) ->
            id.isNotBlank() && title.isNotBlank()
        })
    }

    @Test
    fun dateStampAndLcdOdsAreTheOnlyNamedLegacyFontLayouts() {
        val deliberateOdsFontLayout = "lcd_osd"
        assertTrue(watermarkStudioLayouts.any { it.first == deliberateOdsFontLayout })
        assertTrue(watermarkStudioLayouts.none { it.first == "classic_bar_kk7" })
    }
}

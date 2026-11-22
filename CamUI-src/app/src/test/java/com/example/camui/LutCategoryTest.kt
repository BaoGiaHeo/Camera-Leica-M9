package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class LutCategoryTest {
    @Test
    fun importsRequestedCameraAndLogGroups() {
        assertEquals("Fujifilm", catalogLutCategory("Fujifilm/Provia/Look.cube"))
        assertEquals("Leica", catalogLutCategory("Leica/Classic/Look.cube"))
        assertEquals("Panasonic Lumix", catalogLutCategory("Panasonic Lumix/Standard/Look.cube"))
        assertEquals("Sony", catalogLutCategory("Sony/CineTone/Look.cube"))
        assertEquals("Cinema / Log", catalogLutCategory("Panasonic Lumix/V-Log/Look.cube"))
        assertEquals("Cinema / Log", catalogLutCategory("Sony/S-Log3/Look.cube"))
        assertEquals("Cinema / Log", catalogLutCategory("Cinema-Log/Cineon/Look.cube"))
        assertEquals("Hald CLUT", catalogLutCategory("Hald CLUT/Level 16/Hald.png"))
        assertEquals("Other", catalogLutCategory("Misc/Look.cube"))
    }
}

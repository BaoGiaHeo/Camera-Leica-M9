package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraLookCatalogTest {
    @Test
    fun keepsAllFortySixBuiltInLookNamesAndTheirCategories() {
        val catalog = builtInCameraLookCatalog()
        assertEquals(46, catalog.size)
        assertEquals(46, catalog.map { it.second }.distinct().size)
        assertEquals(
            setOf("fujifilm", "color_film", "classic", "bw", "leica", "hasselblad", "zeiss", "sony", "cinema_log", "basic"),
            catalog.map { it.first }.toSet()
        )
        assertTrue(catalog.all { (category, name) -> category.isNotBlank() && name.isNotBlank() })
    }
}

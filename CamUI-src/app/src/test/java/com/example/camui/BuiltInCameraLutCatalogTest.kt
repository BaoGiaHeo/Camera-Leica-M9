package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInCameraLutCatalogTest {
    @Test
    fun containsExactlyFortySixUniqueLutProfiles() {
        assertTrue(builtInCameraLutCatalogIsValid())
        assertEquals(46, builtInCameraLutDefinitions.size)
        assertEquals(46, builtInCameraLutDefinitions.map { it.id }.toSet().size)
        assertEquals(46, builtInCameraLutDefinitions.map { it.archivePath }.toSet().size)
    }

    @Test
    fun everyPresetUsesAnInternalGeneratedThreeDimensionalProfile() {
        assertTrue(builtInCameraLutDefinitions.all { it.archivePath.startsWith("generated/") })
        assertTrue(builtInCameraLookGroupDefinitions.isNotEmpty())
        assertEquals(46, builtInCameraLookCatalog().size)
    }

    @Test
    fun visibleNamesAreEnglishAndNonEmpty() {
        builtInCameraLutDefinitions.forEach { definition ->
            assertTrue(definition.name.isNotBlank())
            assertFalse(definition.name.any { it in '\u4E00'..'\u9FFF' })
            assertTrue(definition.categoryName.isNotBlank())
        }
    }

    @Test
    fun everyPresetIdRoundTripsThroughStablePrefix() {
        builtInCameraLutDefinitions.forEach { definition ->
            val preset = builtInCameraLutPreset(definition.id)
            assertEquals(definition.id, builtInCameraLutId(preset))
            assertEquals(definition, builtInCameraLutDefinition(definition.id))
        }
    }
}

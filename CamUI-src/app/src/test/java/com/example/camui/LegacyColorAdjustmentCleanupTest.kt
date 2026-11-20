package com.example.camui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyColorAdjustmentCleanupTest {
    @Test
    fun identifiesOldFilterAdjustmentPreferenceNames() {
        assertTrue(
            LegacyColorAdjustmentCleanup.isObsoletePreferenceKey(
                LegacyColorAdjustmentCleanup.LEGACY_FILTER_PREFIX + "provia.x"
            )
        )
        assertTrue(
            LegacyColorAdjustmentCleanup.isObsoletePreferenceKey(
                "A." + LegacyColorAdjustmentCleanup.LEGACY_FILTER_PREFIX + "provia.y"
            )
        )
        assertTrue(
            LegacyColorAdjustmentCleanup.isObsoletePreferenceKey(
                LegacyColorAdjustmentCleanup.LEGACY_SETTINGS_PREFIX + "guide_enabled"
            )
        )
        assertTrue(
            LegacyColorAdjustmentCleanup.isObsoletePreferenceKey(
                LegacyColorAdjustmentCleanup.LEGACY_FILTER_PREFIX.uppercase() + "PROVIA.X"
            )
        )
    }

    @Test
    fun preservesUnrelatedCaptureAndFilmSettings() {
        assertFalse(LegacyColorAdjustmentCleanup.isObsoletePreferenceKey("selected_filter"))
        assertFalse(LegacyColorAdjustmentCleanup.isObsoletePreferenceKey("film_custom_highlight"))
        assertFalse(LegacyColorAdjustmentCleanup.isObsoletePreferenceKey("capture_ratio_index"))
        assertFalse(LegacyColorAdjustmentCleanup.isObsoletePreferenceKey("legacy_color_panel_cleanup_v1"))
        assertFalse(LegacyColorAdjustmentCleanup.isObsoletePreferenceKey("A.film_custom_saturation"))
    }
}

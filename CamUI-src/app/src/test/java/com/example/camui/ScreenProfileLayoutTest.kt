package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenProfileLayoutTest {
    @Test
    fun layoutMetricsUseWindowDpAndClampToTouchSafeRanges() {
        val compact = ScreenProfile(
            2400, 1080, 440, 2.75f, 1f, 872.7f, 392.7f,
            ScreenAspectClass.TWENTY_NINE, ScreenWindowSizeClass.EXPANDED, 120f,
            android.graphics.Rect(), android.graphics.Rect(), android.graphics.Rect(),
            android.graphics.Rect(), emptyList(), emptyList()
        ).layoutFor()
        assertEquals(41.23f, compact.topPlateHeight.value, 0.1f)
        assertTrue(compact.leftColumnWidth.value in 54f..70f)
        assertTrue(compact.rightColumnWidth.value in 188f..232f)
        assertTrue(compact.proControlHeight.value >= 64f)
    }

    @Test
    fun largeWindowValuesAreClamped() {
        val profile = ScreenProfile(
            3200, 1440, 560, 3.5f, 1.3f, 914.2f, 411.4f,
            ScreenAspectClass.TWENTY_NINE, ScreenWindowSizeClass.EXPANDED, 144f,
            android.graphics.Rect(), android.graphics.Rect(), android.graphics.Rect(),
            android.graphics.Rect(), emptyList(), emptyList()
        ).layoutFor(1600f, 900f)
        assertEquals(52f, profile.topPlateHeight.value, 0.001f)
        assertEquals(70f, profile.leftColumnWidth.value, 0.001f)
        assertEquals(232f, profile.rightColumnWidth.value, 0.001f)
        assertEquals(92f, profile.proControlHeight.value, 0.001f)
    }

    @Test
    fun landscapeWindowSizeClassUsesAvailableWidth() {
        assertEquals(ScreenWindowSizeClass.EXPANDED, ScreenProfile.classifyWindowSize(872.7f))
        assertEquals(ScreenWindowSizeClass.MEDIUM, ScreenProfile.classifyWindowSize(700f))
        assertEquals(ScreenWindowSizeClass.COMPACT, ScreenProfile.classifyWindowSize(599f))
    }

    @Test
    fun aspectClassificationRecognizesTabletAndFoldableRatios() {
        assertEquals(ScreenAspectClass.SIXTEEN_TEN, ScreenProfile.classifyAspect(1600f, 1000f))
        assertEquals(ScreenAspectClass.NEAR_SQUARE, ScreenProfile.classifyAspect(2480f, 2200f))
        assertEquals(ScreenAspectClass.TWENTY_TWO_TEN, ScreenProfile.classifyAspect(2200f, 1000f))
    }
}

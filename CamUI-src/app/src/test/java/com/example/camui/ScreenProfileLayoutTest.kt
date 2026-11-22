package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenProfileLayoutTest {
    @Test
    fun layoutMetricsUseWindowDpAndClampToTouchSafeRanges() {
        val compact = ScreenProfile(
            2400, 1080, 440, 2.75f, 1f, 872.7f, 392.7f,
            ScreenAspectClass.TWENTY_NINE, ScreenWindowSizeClass.COMPACT, 120f,
            android.graphics.Rect(), android.graphics.Rect(), android.graphics.Rect(),
            android.graphics.Rect(), emptyList(), emptyList()
        ).layoutFor()
        assertEquals(68.85f, compact.topPlateHeight.value, 0.2f)
        assertTrue(compact.leftColumnWidth.value in 96f..132f)
        assertTrue(compact.rightColumnWidth.value in 150f..200f)
        assertTrue(compact.proControlHeight.value >= 56f)
    }

    @Test
    fun largeWindowValuesAreClamped() {
        val profile = ScreenProfile(
            3200, 1440, 560, 3.5f, 1.3f, 914.2f, 411.4f,
            ScreenAspectClass.TWENTY_NINE, ScreenWindowSizeClass.COMPACT, 144f,
            android.graphics.Rect(), android.graphics.Rect(), android.graphics.Rect(),
            android.graphics.Rect(), emptyList(), emptyList()
        ).layoutFor(1600f, 900f)
        assertEquals(71.7f, profile.topPlateHeight.value, 0.2f)
        assertEquals(132f, profile.leftColumnWidth.value, 0.001f)
        assertEquals(200f, profile.rightColumnWidth.value, 0.001f)
        assertEquals(84f, profile.proControlHeight.value, 0.001f)
    }

    @Test
    fun topPlateNeverUsesTheOldClipProneCompactHeightAcrossFontScales() {
        val densities = listOf(280, 320, 420, 440, 480, 560)
        val fontScales = listOf(0.85f, 1f, 1.3f)
        densities.forEach { dpi ->
            fontScales.forEach { fontScale ->
                val density = dpi / 160f
                val profile = ScreenProfile(
                    2400, 1080, dpi, density, fontScale,
                    2400f / density, 1080f / density,
                    ScreenProfile.classifyAspect(2400f / density, 1080f / density),
                    ScreenWindowSizeClass.COMPACT, 120f,
                    android.graphics.Rect(), android.graphics.Rect(), android.graphics.Rect(),
                    android.graphics.Rect(), emptyList(), emptyList()
                )
                assertTrue(
                    "Top plate too short for dpi=$dpi fontScale=$fontScale: ${profile.layoutFor().topPlateHeight}",
                    profile.layoutFor().topPlateHeight.value >= 66f
                )
            }
        }
    }

    @Test
    fun aspectClassificationRecognizesTabletAndFoldableRatios() {
        assertEquals(ScreenAspectClass.SIXTEEN_TEN, ScreenProfile.classifyAspect(1600f, 1000f))
        assertEquals(ScreenAspectClass.NEAR_SQUARE, ScreenProfile.classifyAspect(2480f, 2200f))
        assertEquals(ScreenAspectClass.TWENTY_TWO_TEN, ScreenProfile.classifyAspect(2200f, 1000f))
    }
}

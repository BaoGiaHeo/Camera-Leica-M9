package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenProfileLayoutTest {
    @Test
    fun referenceLandscapeUsesFujiDesignDimensions() {
        val reference = ScreenProfile(
            924, 427, 440, 2.75f, 1f, 924f, 427f,
            ScreenAspectClass.TWENTY_NINE, ScreenWindowSizeClass.EXPANDED, 120f,
            android.graphics.Rect(), android.graphics.Rect(), android.graphics.Rect(),
            android.graphics.Rect(), emptyList(), emptyList()
        ).layoutFor()
        assertEquals(55f, reference.topPlateHeight.value, 0.001f)
        assertEquals(98f, reference.leftColumnWidth.value, 0.001f)
        assertEquals(310f, reference.rightColumnWidth.value, 0.001f)
        assertEquals(60f, reference.proControlHeight.value, 0.001f)
    }

    @Test
    fun compactLandscapeClampsDesignScaleAtPointEightFive() {
        val compact = ScreenProfile(
            640, 360, 440, 2.75f, 1f, 640f, 360f,
            ScreenAspectClass.SIXTEEN_NINE, ScreenWindowSizeClass.MEDIUM, 120f,
            android.graphics.Rect(), android.graphics.Rect(), android.graphics.Rect(),
            android.graphics.Rect(), emptyList(), emptyList()
        ).layoutFor()
        assertEquals(46.75f, compact.topPlateHeight.value, 0.01f)
        assertEquals(83.3f, compact.leftColumnWidth.value, 0.01f)
        assertEquals(263.5f, compact.rightColumnWidth.value, 0.01f)
        assertEquals(51f, compact.proControlHeight.value, 0.01f)
    }

    @Test
    fun largeWindowValuesUseMaximumDesignScale() {
        val profile = ScreenProfile(
            3200, 1440, 560, 3.5f, 1.3f, 914.2f, 411.4f,
            ScreenAspectClass.TWENTY_NINE, ScreenWindowSizeClass.EXPANDED, 144f,
            android.graphics.Rect(), android.graphics.Rect(), android.graphics.Rect(),
            android.graphics.Rect(), emptyList(), emptyList()
        ).layoutFor(1600f, 900f)
        assertEquals(68.75f, profile.topPlateHeight.value, 0.001f)
        assertEquals(122.5f, profile.leftColumnWidth.value, 0.001f)
        assertEquals(387.5f, profile.rightColumnWidth.value, 0.001f)
        assertEquals(75f, profile.proControlHeight.value, 0.001f)
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

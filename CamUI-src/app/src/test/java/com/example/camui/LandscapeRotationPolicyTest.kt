package com.example.camui

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LandscapeRotationPolicyTest {
    @Test
    fun portraitQuadrantsNeverProducePortraitCaptureRotation() {
        listOf(0f, 20f, 44f, 180f, 220f, 314f, 340f).forEach { degrees ->
            assertNull(
                "Portrait sensor angle $degrees must not select a portrait rotation",
                LandscapeRotationPolicy.landscapeCandidate(degrees)
            )
        }
    }

    @Test
    fun landscapeQuadrantsMapOnlyToNinetyOrTwoSeventy() {
        assertEquals(Surface.ROTATION_270, LandscapeRotationPolicy.landscapeCandidate(90f))
        assertEquals(Surface.ROTATION_270, LandscapeRotationPolicy.landscapeCandidate(130f))
        assertEquals(Surface.ROTATION_90, LandscapeRotationPolicy.landscapeCandidate(230f))
        assertEquals(Surface.ROTATION_90, LandscapeRotationPolicy.landscapeCandidate(270f))
    }
}

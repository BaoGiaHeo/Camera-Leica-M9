package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraAfAreaSizeTest {
    @Test
    fun rejectsStaleOrObsoleteFocusRequestResults() {
        val point = Offset(0.42f, 0.61f)
        assertEquals(
            true,
            shouldAcceptFocusRequestResult(7, 7, point, point, sameCamera = true, requestStillActive = true)
        )
        assertEquals(
            false,
            shouldAcceptFocusRequestResult(6, 7, point, point, sameCamera = true, requestStillActive = true)
        )
        assertEquals(
            false,
            shouldAcceptFocusRequestResult(7, 7, point, Offset(0.5f, 0.5f), sameCamera = true, requestStillActive = true)
        )
        assertEquals(
            false,
            shouldAcceptFocusRequestResult(7, 7, point, point, sameCamera = false, requestStillActive = true)
        )
        assertEquals(
            false,
            shouldAcceptFocusRequestResult(7, 7, point, point, sameCamera = true, requestStillActive = false)
        )
    }

    @Test
    fun mapsStoredPreferenceToPixelReticleScale() {
        assertEquals(CameraAfAreaSize.SMALL, CameraAfAreaSize.fromPreference("s"))
        assertEquals(CameraAfAreaSize.MEDIUM, CameraAfAreaSize.fromPreference("m"))
        assertEquals(CameraAfAreaSize.LARGE, CameraAfAreaSize.fromPreference("l"))
        assertEquals(CameraAfAreaSize.SMALL, CameraAfAreaSize.fromPreference("SMALL"))
        assertEquals(CameraAfAreaSize.LARGE, CameraAfAreaSize.fromPreference("large"))
        assertEquals(CameraAfAreaSize.MEDIUM, CameraAfAreaSize.fromPreference(null))
        assertEquals(0, CameraAfAreaSize.SMALL.pixelScale)
        assertEquals(1, CameraAfAreaSize.MEDIUM.pixelScale)
        assertEquals(2, CameraAfAreaSize.LARGE.pixelScale)
        assertEquals(0.12f, CameraAfAreaSize.SMALL.focusPointSize, 0.0001f)
        assertEquals(0.18f, CameraAfAreaSize.MEDIUM.focusPointSize, 0.0001f)
        assertEquals(0.26f, CameraAfAreaSize.LARGE.focusPointSize, 0.0001f)
    }
}

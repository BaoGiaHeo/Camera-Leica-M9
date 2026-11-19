package com.example.camui

import android.view.Surface

/**
 * Returns only a landscape CameraX target rotation.
 * Portrait quadrants return null so the camera keeps its last landscape rotation.
 */
internal object LandscapeRotationPolicy {
    fun landscapeCandidate(filteredDegrees: Float): Int? {
        val degrees = ((filteredDegrees % 360f) + 360f) % 360f
        return when {
            degrees >= 315f || degrees < 45f -> null
            degrees < 135f -> Surface.ROTATION_270
            degrees < 225f -> null
            else -> Surface.ROTATION_90
        }
    }
}

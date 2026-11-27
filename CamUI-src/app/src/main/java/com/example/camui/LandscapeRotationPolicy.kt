package com.example.camui

import android.view.Surface

/**
 * Maps all four physical handset quadrants to CameraX target rotations.
 * OrientationEventListener degrees are clockwise from the natural portrait posture.
 */
internal object DeviceRotationPolicy {
    fun rotationCandidate(filteredDegrees: Float): Int {
        val degrees = ((filteredDegrees % 360f) + 360f) % 360f
        return when {
            degrees >= 315f || degrees < 45f -> Surface.ROTATION_0
            degrees < 135f -> Surface.ROTATION_270
            degrees < 225f -> Surface.ROTATION_180
            else -> Surface.ROTATION_90
        }
    }
}

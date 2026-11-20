package com.example.camui

import androidx.compose.ui.geometry.Offset
import java.util.Locale

/**
 * Independent centre AF target sizing. Metering mode is deliberately not used
 * to determine the reticle size.
 */
enum class CameraAfAreaSize(
    val preferenceValue: String,
    val pixelScale: Int,
    val focusPointSize: Float
) {
    SMALL("s", 0, 0.12f),
    MEDIUM("m", 1, 0.18f),
    LARGE("l", 2, 0.26f);

    companion object {
        fun fromPreference(value: String?): CameraAfAreaSize =
            when (value?.lowercase(Locale.ROOT)) {
                "s", "small" -> SMALL
                "l", "large" -> LARGE
                else -> MEDIUM
            }
    }
}


/** Rejects AF/AE lock callbacks from stale touches, a different camera, or an unlocked state. */
internal fun shouldAcceptFocusLockResult(
    requestGeneration: Int,
    currentGeneration: Int,
    requestedCenter: Offset,
    currentCenter: Offset,
    sameCamera: Boolean,
    manualLockStillActive: Boolean
): Boolean =
    requestGeneration == currentGeneration &&
        requestedCenter == currentCenter &&
        sameCamera &&
        manualLockStillActive

package com.example.camui

import java.util.Locale

/**
 * Independent centre AF target sizing. Metering mode is deliberately not used
 * to determine the reticle size.
 */
enum class CameraAfAreaSize(val preferenceValue: String, val pixelScale: Int) {
    SMALL("s", 0),
    MEDIUM("m", 1),
    LARGE("l", 2);

    companion object {
        fun fromPreference(value: String?): CameraAfAreaSize =
            when (value?.lowercase(Locale.ROOT)) {
                "s", "small" -> SMALL
                "l", "large" -> LARGE
                else -> MEDIUM
            }
    }
}

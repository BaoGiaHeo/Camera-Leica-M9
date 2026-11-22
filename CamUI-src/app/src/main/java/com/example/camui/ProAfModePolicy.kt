package com.example.camui

import android.hardware.camera2.CameraCharacteristics

internal fun camera2AfModeForProSelection(
    selection: String,
    availableModes: IntArray?
): Int? {
    val modes = availableModes?.toSet().orEmpty()
    val preferred = when (selection.uppercase()) {
        "AF-S" -> CameraCharacteristics.CONTROL_AF_MODE_AUTO
        "AF-C" -> CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        "MF" -> CameraCharacteristics.CONTROL_AF_MODE_OFF
        else -> return null
    }
    if (preferred in modes) return preferred
    if (selection.equals("AF-C", ignoreCase = true) &&
        CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO in modes
    ) {
        return CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO
    }
    return null
}

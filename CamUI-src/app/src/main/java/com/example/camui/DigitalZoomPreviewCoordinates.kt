package com.example.camui

import androidx.compose.ui.geometry.Offset

/**
 * Maps normalized camera-preview coordinates into the centred digital crop shown by
 * the viewfinder. Camera coordinates remain unchanged; only overlay coordinates move.
 */
internal fun cameraPointToDigitalPreview(point: Offset, digitalCropRatio: Float): Offset {
    val scale = digitalCropRatio.takeIf { it.isFinite() && it >= 1f } ?: 1f
    return Offset(
        ((point.x - 0.5f) * scale + 0.5f).coerceIn(0f, 1f),
        ((point.y - 0.5f) * scale + 0.5f).coerceIn(0f, 1f)
    )
}

/**
 * Inverts the displayed digital crop before passing a point to PreviewView's
 * metering-point factory, which maps against the unmodified CameraX stream.
 */
internal fun digitalPreviewPointToCamera(point: Offset, digitalCropRatio: Float): Offset {
    val scale = digitalCropRatio.takeIf { it.isFinite() && it >= 1f } ?: 1f
    return Offset(
        ((point.x - 0.5f) / scale + 0.5f).coerceIn(0f, 1f),
        ((point.y - 0.5f) / scale + 0.5f).coerceIn(0f, 1f)
    )
}

/** Enlarges the displayed face rectangle by the same digital crop factor. */
internal fun cameraBoxSizeToDigitalPreview(size: Offset, digitalCropRatio: Float): Offset {
    val scale = digitalCropRatio.takeIf { it.isFinite() && it >= 1f } ?: 1f
    return Offset(
        (size.x * scale).coerceAtLeast(0f),
        (size.y * scale).coerceAtLeast(0f)
    )
}

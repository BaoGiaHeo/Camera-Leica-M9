package com.example.camui

/** Upright pixel dimensions derived from the encoded image bounds and EXIF orientation. */
data class PhotoInspectorPixelDimensions(val width: Int, val height: Int)

object PhotoInspectorGeometry {
    /**
     * EXIF orientations 5–8 rotate or transpose the encoded frame by 90 degrees,
     * so the dimensions shown in the Inspector must swap axes.
     */
    fun fromEncoded(width: Int, height: Int, exifOrientation: Int): PhotoInspectorPixelDimensions? {
        if (width <= 0 || height <= 0) return null
        val swapsAxes = exifOrientation in 5..8
        return if (swapsAxes) {
            PhotoInspectorPixelDimensions(height, width)
        } else {
            PhotoInspectorPixelDimensions(width, height)
        }
    }
}

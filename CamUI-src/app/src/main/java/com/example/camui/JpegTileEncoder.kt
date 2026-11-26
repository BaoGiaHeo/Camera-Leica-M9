package com.example.camui

/**
 * Native JPEG stripe encoder used only by LunaDream 3.0.
 *
 * The input JPEG must already have EXIF Orientation=1. The native side decodes
 * source scanlines, resamples one 64-row output stripe at a time, and streams all
 * stripes into one JPEG. It never allocates a destination-sized Bitmap.
 */
internal object JpegTileEncoder {
    init {
        System.loadLibrary("fuji_jpeg_tile")
    }

    /**
     * Returns null on success, or a Vietnamese failure reason on error.
     *
     * deadlineElapsedRealtimeMs uses Android SystemClock.elapsedRealtime() time base.
     */
    external fun encode(
        sourcePath: String,
        outputPath: String,
        targetWidth: Int,
        targetHeight: Int,
        cropLeft: Int,
        cropTop: Int,
        cropWidth: Int,
        cropHeight: Int,
        quality: Int,
        deadlineElapsedRealtimeMs: Long
    ): String?
}

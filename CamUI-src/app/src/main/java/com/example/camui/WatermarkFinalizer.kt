package com.example.camui

import android.content.Context
import android.net.Uri
import android.util.Log

/**
 * Last safety check at the final-save boundary. The sink verifies the EXIF marker
 * and the watermark-region pixel sample. If the marker is absent, this finalizer
 * attempts one final WatermarkStage/ImageSink pass and records a red warning if it
 * still cannot prove the selected watermark state.
 */
internal object WatermarkFinalizer {
    private const val TAG = "FUJI_WATERMARK_FINALIZER"

    fun ensure(
        context: Context,
        uri: Uri,
        focal: String,
        aperture: String,
        shutter: String,
        iso: String,
        captureTimestampMillis: Long,
        onStageDuration: ((String, Long) -> Unit)? = null
    ): Boolean {
        val enabled = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
            .getBoolean("watermark_enabled", true)
        val expected = if (enabled) "FM-WM=1" else "FM-WM=0"
        val existing = ImageSink.readWatermarkMarker(context, uri)
        if (existing == expected) {
            return true
        }

        val rendered = runCatching {
            applyCameraWatermark(
                context = context,
                uri = uri,
                focal = focal,
                aperture = aperture,
                shutter = shutter,
                iso = iso,
                preservePhotoAspectRatio = true,
                applyFilter = true,
                captureTimestampMillis = captureTimestampMillis,
                onStageDuration = onStageDuration
            )
        }.onFailure {
            Log.e(TAG, "Watermark finalizer failed for $uri", it)
        }.getOrDefault(false)

        val actual = ImageSink.readWatermarkMarker(context, uri)
        val verified = actual == expected
        if (!verified) {
            val message = "Watermark bật=$enabled nhưng marker cuối không khớp: expected=$expected actual=$actual"
            CaptureDiagnostics.recordWarning(context, message)
            Log.e(TAG, "RED_WARNING $message uri=$uri")
            return false
        }
        Log.i(TAG, "FINALIZER_OK uri=$uri marker=$actual rendered=$rendered")
        return verified && rendered
    }
}

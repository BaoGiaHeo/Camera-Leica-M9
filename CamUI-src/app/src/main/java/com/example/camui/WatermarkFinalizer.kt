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
        onStageDuration: ((String, Long) -> Unit)? = null,
        forceSkipWatermark: Boolean = false
    ): Boolean {
        val enabled = !forceSkipWatermark && context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
            .getBoolean("watermark_enabled", false)
        val expected = if (enabled) "FM-WM=1" else "FM-WM=0"
        val existing = ImageSink.readWatermarkMarker(context, uri)
        if (!enabled && existing == "FM-WM=0") {
            Log.i(TAG, "FINALIZER_OK uri=$uri marker=FM-WM=0 intentionalSkip=true")
            return true
        }
        if (enabled && existing == "FM-WM=1" && ImageSink.verifyStoredWatermark(context, uri)) {
            Log.i(TAG, "FINALIZER_OK uri=$uri marker=FM-WM=1 pixelRegionVerified=true")
            return true
        }
        if (enabled && existing == "FM-WM=1") {
            val warning = "CẢNH BÁO ĐỎ: EXIF FM-WM=1 tồn tại nhưng vùng pixel watermark không xác minh được; đang thử đóng watermark lại."
            CaptureDiagnostics.recordWarning(context, warning)
            Log.e(TAG, "$warning uri=$uri")
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
                onStageDuration = onStageDuration,
                forceSkipWatermark = forceSkipWatermark
            )
        }.onFailure {
            Log.e(TAG, "Watermark finalizer failed for $uri", it)
        }.getOrDefault(false)

        val actual = ImageSink.readWatermarkMarker(context, uri)
        val pixelVerified = !enabled || (actual == "FM-WM=1" && ImageSink.verifyStoredWatermark(context, uri))
        val verified = actual == expected && pixelVerified
        if (!verified) {
            val message = "CẢNH BÁO ĐỎ: watermark cuối không được xác minh: enabled=$enabled expected=$expected actual=$actual pixelVerified=$pixelVerified rendered=$rendered"
            CaptureDiagnostics.recordWarning(context, message)
            Log.e(TAG, "$message uri=$uri")
            return false
        }
        Log.i(TAG, "FINALIZER_OK uri=$uri marker=$actual rendered=$rendered pixelVerified=$pixelVerified")
        return rendered && verified
    }
}

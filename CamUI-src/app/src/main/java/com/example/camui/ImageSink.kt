package com.example.camui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Final JPEG payload. The constructor is private; production instances are created
 * by WatermarkStage only after a deliberate print or deliberate skip decision.
 */
internal class WatermarkedFrame private constructor(
    internal val uri: Uri,
    internal val bitmap: Bitmap?,
    internal val sourceExif: Map<String, String>,
    internal val width: Int,
    internal val height: Int,
    internal val watermarkPrinted: Boolean,
    internal val intentionalSkip: Boolean,
    internal val verificationRegion: Rect?,
    internal val verificationBackgroundColor: Int
) {
    companion object {
        internal fun fromWatermarkStage(
            uri: Uri,
            bitmap: Bitmap?,
            sourceExif: Map<String, String>,
            width: Int,
            height: Int,
            watermarkPrinted: Boolean,
            intentionalSkip: Boolean,
            verificationRegion: Rect?,
            verificationBackgroundColor: Int
        ): WatermarkedFrame = WatermarkedFrame(
            uri = uri,
            bitmap = bitmap,
            sourceExif = sourceExif,
            width = width,
            height = height,
            watermarkPrinted = watermarkPrinted,
            intentionalSkip = intentionalSkip,
            verificationRegion = verificationRegion,
            verificationBackgroundColor = verificationBackgroundColor
        )
    }
}

/** The only production factory for a WatermarkedFrame. */
internal object WatermarkStage {
    fun printed(
        uri: Uri,
        bitmap: Bitmap,
        sourceExif: Map<String, String>,
        verificationRegion: Rect?,
        verificationBackgroundColor: Int
    ): WatermarkedFrame {
        require(!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0)
        return WatermarkedFrame.fromWatermarkStage(
            uri = uri,
            bitmap = bitmap,
            sourceExif = sourceExif,
            width = bitmap.width,
            height = bitmap.height,
            watermarkPrinted = true,
            intentionalSkip = false,
            verificationRegion = verificationRegion,
            verificationBackgroundColor = verificationBackgroundColor
        )
    }

    fun intentionallySkipped(
        uri: Uri,
        sourceExif: Map<String, String>,
        width: Int,
        height: Int
    ): WatermarkedFrame {
        require(width > 0 && height > 0)
        return WatermarkedFrame.fromWatermarkStage(
            uri = uri,
            bitmap = null,
            sourceExif = sourceExif,
            width = width,
            height = height,
            watermarkPrinted = false,
            intentionalSkip = true,
            verificationRegion = null,
            verificationBackgroundColor = android.graphics.Color.BLACK
        )
    }

    fun filteredWithoutWatermark(
        uri: Uri,
        bitmap: Bitmap,
        sourceExif: Map<String, String>
    ): WatermarkedFrame {
        require(!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0)
        return WatermarkedFrame.fromWatermarkStage(
            uri = uri,
            bitmap = bitmap,
            sourceExif = sourceExif,
            width = bitmap.width,
            height = bitmap.height,
            watermarkPrinted = false,
            intentionalSkip = true,
            verificationRegion = null,
            verificationBackgroundColor = android.graphics.Color.BLACK
        )
    }
}

/**
 * One final JPEG sink. Encoders used to build intermediate merge/resize/rollback files
 * are separate pipeline intermediates and must not publish a final camera photo.
 */
internal object ImageSink {
    private const val TAG = "FUJI_IMAGE_SINK"
    private const val WATERMARK_COMMENT = "FM-WM=1"
    private const val SKIPPED_COMMENT = "FM-WM=0"

    data class Result(
        val written: Boolean,
        val watermarkPrinted: Boolean,
        val marker: String,
        val regionVerified: Boolean?,
        val encodeDurationMs: Long,
        val writeDurationMs: Long
    )

    fun readWatermarkMarker(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use {
            ExifInterface(it.fileDescriptor).getAttribute(ExifInterface.TAG_USER_COMMENT)
        }
    }.getOrNull()

    fun write(
        context: Context,
        frame: WatermarkedFrame,
        onStageDuration: ((String, Long) -> Unit)? = null
    ): Result {
        val marker = if (frame.watermarkPrinted) WATERMARK_COMMENT else SKIPPED_COMMENT
        if (frame.intentionalSkip && frame.bitmap == null) {
            val start = android.os.SystemClock.elapsedRealtime()
            writeExif(context, frame.uri, frame.sourceExif, frame.width, frame.height, marker)
            val elapsed = android.os.SystemClock.elapsedRealtime() - start
            onStageDuration?.invoke("Ghi file", elapsed)
            Log.i(TAG, "FINAL_SKIP uri=${frame.uri} marker=$marker reason=watermark-off-and-no-filter")
            return Result(true, false, marker, null, 0L, elapsed)
        }

        val bitmap = frame.bitmap ?: error("WatermarkedFrame has no image payload")
        check(frame.watermarkPrinted || frame.intentionalSkip) { "Final JPEG must be printed or watermark must be intentionally skipped" }
        val temp = File.createTempFile("fuji-image-sink-", ".jpg", context.cacheDir)
        var encodeMs = 0L
        var writeMs = 0L
        try {
            val encodeStarted = android.os.SystemClock.elapsedRealtime()
            FileOutputStream(temp).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 96, output)) {
                    "ImageSink could not encode final JPEG"
                }
                output.flush()
                output.fd.sync()
            }
            encodeMs = android.os.SystemClock.elapsedRealtime() - encodeStarted
            onStageDuration?.invoke("Mã hóa", encodeMs)

            val writeStarted = android.os.SystemClock.elapsedRealtime()
            val written = context.contentResolver.openOutputStream(frame.uri, "wt")?.use { destination ->
                temp.inputStream().use { it.copyTo(destination) }
                true
            } ?: false
            check(written) { "ImageSink could not write final JPEG to MediaStore" }
            writeExif(context, frame.uri, frame.sourceExif, frame.width, frame.height, marker)
            context.contentResolver.update(
                frame.uri,
                ContentValues().apply { put(MediaStore.Images.Media.ORIENTATION, 0) },
                null,
                null
            )
            writeMs = android.os.SystemClock.elapsedRealtime() - writeStarted
            onStageDuration?.invoke("Ghi file", writeMs)

            val regionVerified = if (frame.watermarkPrinted) {
                frame.verificationRegion?.let { verifyWatermarkRegion(context, frame.uri, it, frame.verificationBackgroundColor) }
            } else null
            if (regionVerified == false) {
                CaptureDiagnostics.recordWarning(
                    context,
                    "CẢNH BÁO ĐỎ: watermark bật nhưng vùng watermark sau lưu không có đủ biến thiên điểm ảnh; uri=${frame.uri}"
                )
                Log.e(TAG, "RED_WARNING watermark region not verified uri=${frame.uri}")
            }
            Log.i(
                TAG,
                "FINAL_WRITE uri=${frame.uri} marker=$marker watermark=${frame.watermarkPrinted} " +
                    "size=${frame.width}x${frame.height} encode=${encodeMs}ms write=${writeMs}ms " +
                    "regionVerified=$regionVerified"
            )
            return Result(true, frame.watermarkPrinted && regionVerified != false, marker, regionVerified, encodeMs, writeMs)
        } catch (failure: Throwable) {
            Log.e(TAG, "Final JPEG sink failed uri=${frame.uri}", failure)
            throw failure
        } finally {
            runCatching { temp.delete() }
        }
    }

    private fun writeExif(
        context: Context,
        uri: Uri,
        sourceExif: Map<String, String>,
        width: Int,
        height: Int,
        marker: String
    ) {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "rw")
            ?: error("Cannot open final image EXIF descriptor")
        descriptor.use {
            ExifInterface(it.fileDescriptor).apply {
                sourceExif.forEach { (tag, value) ->
                    runCatching { setAttribute(tag, value) }
                        .onFailure { Log.d(TAG, "Skipped unsupported EXIF tag $tag", it) }
                }
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, width.toString())
                setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, height.toString())
                setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
                setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
                setAttribute(ExifInterface.TAG_USER_COMMENT, marker)
                saveAttributes()
            }
        }
        val verification = context.contentResolver.openFileDescriptor(uri, "r")?.use {
            ExifInterface(it.fileDescriptor).getAttribute(ExifInterface.TAG_USER_COMMENT)
        }
        check(verification == marker) { "Final EXIF watermark marker mismatch: expected=$marker actual=$verification" }
    }

    private fun verifyWatermarkRegion(
        context: Context,
        uri: Uri,
        requested: Rect,
        expectedBackgroundColor: Int
    ): Boolean? {
        val source = context.contentResolver.openInputStream(uri) ?: return false
        source.use { input ->
            val decoder = BitmapRegionDecoder.newInstance(input, false) ?: return false
            try {
                val safe = Rect(
                    requested.left.coerceIn(0, decoder.width - 1),
                    requested.top.coerceIn(0, decoder.height - 1),
                    requested.right.coerceIn(1, decoder.width),
                    requested.bottom.coerceIn(1, decoder.height)
                )
                if (safe.width() < 2 || safe.height() < 2) return false
                val sample = BitmapFactoryOptions.regionSample(safe.width(), safe.height())
                val bitmap = decoder.decodeRegion(safe, sample) ?: return false
                try {
                    val strideX = (bitmap.width / 64).coerceAtLeast(1)
                    val strideY = (bitmap.height / 32).coerceAtLeast(1)
                    val expectedRed = android.graphics.Color.red(expectedBackgroundColor)
                    val expectedGreen = android.graphics.Color.green(expectedBackgroundColor)
                    val expectedBlue = android.graphics.Color.blue(expectedBackgroundColor)
                    var different = 0
                    var sampled = 0
                    var y = 0
                    while (y < bitmap.height) {
                        var x = 0
                        while (x < bitmap.width) {
                            val pixel = bitmap.getPixel(x, y)
                            val delta = kotlin.math.abs(android.graphics.Color.red(pixel) - expectedRed) +
                                kotlin.math.abs(android.graphics.Color.green(pixel) - expectedGreen) +
                                kotlin.math.abs(android.graphics.Color.blue(pixel) - expectedBlue)
                            if (delta > 48) different++
                            sampled++
                            x += strideX
                        }
                        y += strideY
                    }
                    // Compare the decoded footer region against the pre-render blank-canvas color.
                    return sampled > 8 && different >= 4
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            } finally {
                decoder.recycle()
            }
        }
    }

    private object BitmapFactoryOptions {
        fun regionSample(width: Int, height: Int): android.graphics.BitmapFactory.Options {
            var sample = 1
            while (maxOf(width, height) / sample > 640) sample *= 2
            return android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        }
    }
}

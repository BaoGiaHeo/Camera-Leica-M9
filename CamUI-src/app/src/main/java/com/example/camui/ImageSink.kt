package com.example.camui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Locale

/**
 * Final JPEG payload. The constructor is private; production instances are created
 * by WatermarkStage only after a deliberate print or deliberate skip decision.
 */
private const val WATERMARK_SAMPLE_COLUMNS = 64
private const val WATERMARK_SAMPLE_ROWS = 32

internal class WatermarkedFrame private constructor(
    internal val uri: Uri,
    internal val bitmap: Bitmap?,
    internal val encodedJpeg: File?,
    internal val sourceExif: Map<String, String>,
    internal val width: Int,
    internal val height: Int,
    internal val watermarkPrinted: Boolean,
    internal val intentionalSkip: Boolean,
    internal val verificationRegion: Rect?,
    internal val verificationBackgroundColor: Int,
    internal val verificationBaseline: String?
) {
    companion object {
        internal fun fromWatermarkStage(
            uri: Uri,
            bitmap: Bitmap?,
            encodedJpeg: File?,
            sourceExif: Map<String, String>,
            width: Int,
            height: Int,
            watermarkPrinted: Boolean,
            intentionalSkip: Boolean,
            verificationRegion: Rect?,
            verificationBackgroundColor: Int,
            verificationBaseline: String?
        ): WatermarkedFrame = WatermarkedFrame(
            uri = uri,
            bitmap = bitmap,
            encodedJpeg = encodedJpeg,
            sourceExif = sourceExif,
            width = width,
            height = height,
            watermarkPrinted = watermarkPrinted,
            intentionalSkip = intentionalSkip,
            verificationRegion = verificationRegion,
            verificationBackgroundColor = verificationBackgroundColor,
            verificationBaseline = verificationBaseline
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
        verificationBackgroundColor: Int,
        unwatermarkedBitmap: Bitmap
    ): WatermarkedFrame {
        require(!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0)
        require(!unwatermarkedBitmap.isRecycled &&
            unwatermarkedBitmap.width == bitmap.width &&
            unwatermarkedBitmap.height == bitmap.height
        ) { "Watermark baseline must match the rendered output dimensions" }
        val baseline = verificationRegion?.let { encodeWatermarkBaseline(unwatermarkedBitmap, bitmap, it) }
        return WatermarkedFrame.fromWatermarkStage(
            uri = uri,
            bitmap = bitmap,
            encodedJpeg = null,
            sourceExif = sourceExif,
            width = bitmap.width,
            height = bitmap.height,
            watermarkPrinted = true,
            intentionalSkip = false,
            verificationRegion = verificationRegion,
            verificationBackgroundColor = verificationBackgroundColor,
            verificationBaseline = baseline
        )
    }

    /**
     * Creates a final-save payload from a JPEG already encoded by the native tile path.
     * The output still has to pass through ImageSink; native code only writes a cache file.
     */
    fun encodedTileJpeg(
        uri: Uri,
        encodedJpeg: File,
        sourceExif: Map<String, String>,
        width: Int,
        height: Int
    ): WatermarkedFrame {
        require(encodedJpeg.isFile && encodedJpeg.length() > 0L) {
            "Native tile JPEG file is missing or empty"
        }
        require(width > 0 && height > 0)
        val exif = ExifInterface(encodedJpeg.absolutePath)
        val marker = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
        val description = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION).orEmpty()
        val match = Regex(
            "FM-WM-RECT=(-?\\d+),(-?\\d+),(-?\\d+),(-?\\d+);BG=(-?\\d+);BASE=([A-Za-z0-9+/=]+)"
        ).find(description)
        val printed = marker == "FM-WM=1"
        val skipped = marker == "FM-WM=0"
        require(printed || skipped) {
            "Native tile JPEG must carry a WatermarkStage decision marker; actual=$marker"
        }
        if (printed) require(match != null) {
            "Watermark is marked printed but its pixel-verification sample is missing"
        }
        val values = match?.groupValues
        return WatermarkedFrame.fromWatermarkStage(
            uri = uri,
            bitmap = null,
            encodedJpeg = encodedJpeg,
            sourceExif = sourceExif,
            width = width,
            height = height,
            watermarkPrinted = printed,
            intentionalSkip = skipped,
            verificationRegion = values?.let {
                Rect(it[1].toInt(), it[2].toInt(), it[3].toInt(), it[4].toInt())
            },
            verificationBackgroundColor = values?.get(5)?.toInt()
                ?: android.graphics.Color.BLACK,
            verificationBaseline = values?.get(6)
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
            encodedJpeg = null,
            sourceExif = sourceExif,
            width = width,
            height = height,
            watermarkPrinted = false,
            intentionalSkip = true,
            verificationRegion = null,
            verificationBackgroundColor = android.graphics.Color.BLACK,
            verificationBaseline = null
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
            encodedJpeg = null,
            sourceExif = sourceExif,
            width = bitmap.width,
            height = bitmap.height,
            watermarkPrinted = false,
            intentionalSkip = true,
            verificationRegion = null,
            verificationBackgroundColor = android.graphics.Color.BLACK,
            verificationBaseline = null
        )
    }

    private fun encodeWatermarkBaseline(
        unwatermarkedBitmap: Bitmap,
        renderedBitmap: Bitmap,
        region: Rect
    ): String {
        val safe = Rect(
            region.left.coerceIn(0, unwatermarkedBitmap.width - 1),
            region.top.coerceIn(0, unwatermarkedBitmap.height - 1),
            region.right.coerceIn(1, unwatermarkedBitmap.width),
            region.bottom.coerceIn(1, unwatermarkedBitmap.height)
        )
        require(safe.width() > 1 && safe.height() > 1) { "Watermark verification region is empty" }
        require(
            renderedBitmap.width == unwatermarkedBitmap.width &&
                renderedBitmap.height == unwatermarkedBitmap.height
        ) { "Rendered watermark dimensions do not match source bitmap" }
        val samples = ByteArrayOutputStream()
        var changedSamples = 0
        DataOutputStream(samples).use { output ->
            output.writeShort(0) // Replaced with the number of encoded changed-pixel samples below.
            for (row in 0 until WATERMARK_SAMPLE_ROWS) {
                val y = safe.top + ((row + 0.5f) * safe.height() / WATERMARK_SAMPLE_ROWS)
                    .toInt().coerceAtMost(safe.height() - 1)
                for (column in 0 until WATERMARK_SAMPLE_COLUMNS) {
                    val x = safe.left + ((column + 0.5f) * safe.width() / WATERMARK_SAMPLE_COLUMNS)
                        .toInt().coerceAtMost(safe.width() - 1)
                    val before = unwatermarkedBitmap.getPixel(x, y)
                    val after = renderedBitmap.getPixel(x, y)
                    val delta = kotlin.math.abs(android.graphics.Color.red(before) - android.graphics.Color.red(after)) +
                        kotlin.math.abs(android.graphics.Color.green(before) - android.graphics.Color.green(after)) +
                        kotlin.math.abs(android.graphics.Color.blue(before) - android.graphics.Color.blue(after))
                    if (delta < 36) continue
                    val sampleIndex = row * WATERMARK_SAMPLE_COLUMNS + column
                    output.writeShort(sampleIndex)
                    output.writeByte(android.graphics.Color.red(after))
                    output.writeByte(android.graphics.Color.green(after))
                    output.writeByte(android.graphics.Color.blue(after))
                    changedSamples++
                }
            }
        }
        require(changedSamples >= 8) {
            "Watermark renderer did not change enough pixels in its verification region"
        }
        val payload = samples.toByteArray()
        payload[0] = ((changedSamples ushr 8) and 0xff).toByte()
        payload[1] = (changedSamples and 0xff).toByte()
        return Base64.encodeToString(payload, Base64.NO_WRAP)
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
        if (frame.intentionalSkip && frame.bitmap == null && frame.encodedJpeg == null) {
            val start = android.os.SystemClock.elapsedRealtime()
            writeExif(context, frame.uri, frame.sourceExif, frame.width, frame.height, marker, null)
            val elapsed = android.os.SystemClock.elapsedRealtime() - start
            onStageDuration?.invoke("Ghi file", elapsed)
            Log.i(TAG, "FINAL_SKIP uri=${frame.uri} marker=$marker reason=watermark-off-and-no-filter")
            return Result(true, false, marker, null, 0L, elapsed)
        }

        val bitmap = frame.bitmap
        val encodedJpeg = frame.encodedJpeg
        check(bitmap != null || encodedJpeg?.isFile == true) {
            "WatermarkedFrame has neither a bitmap nor a native encoded JPEG"
        }
        check(frame.watermarkPrinted || frame.intentionalSkip) {
            "Final JPEG must be printed or watermark must be intentionally skipped"
        }
        val temp = if (bitmap != null) {
            File.createTempFile("fuji-image-sink-", ".jpg", context.cacheDir)
        } else null
        var encodeMs = 0L
        var writeMs = 0L
        try {
            if (bitmap != null && temp != null) {
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
            } else {
                check(encodedJpeg != null && encodedJpeg.length() > 0L) {
                    "Native encoded JPEG is missing or empty"
                }
            }

            val finalInput = encodedJpeg ?: temp ?: error("ImageSink has no encoded JPEG source")
            val writeStarted = android.os.SystemClock.elapsedRealtime()
            val written = context.contentResolver.openOutputStream(frame.uri, "wt")?.use { destination ->
                finalInput.inputStream().use { it.copyTo(destination) }
                destination.flush()
                true
            } ?: false
            check(written) { "ImageSink could not write final JPEG to MediaStore" }
            // Verify the actual JPEG bytes after replacement and BEFORE writing FM-WM=1.
            // An EXIF marker can no longer make a missing watermark look successful.
            val regionVerified = if (frame.watermarkPrinted) {
                frame.verificationRegion?.let { region -> frame.verificationBaseline?.let { baseline -> verifyWatermarkRegion(context, frame.uri, region, baseline) } } == true
            } else null
            val verifiedMarker = when {
                frame.watermarkPrinted && regionVerified == true -> WATERMARK_COMMENT
                frame.intentionalSkip -> SKIPPED_COMMENT
                else -> "FM-WM=UNVERIFIED"
            }
            writeExif(
                context,
                frame.uri,
                frame.sourceExif,
                frame.width,
                frame.height,
                verifiedMarker,
                frame.verificationRegion.takeIf { regionVerified == true },
                frame.verificationBackgroundColor,
                frame.verificationBaseline
            )
            context.contentResolver.update(
                frame.uri,
                ContentValues().apply { put(MediaStore.Images.Media.ORIENTATION, 0) },
                null,
                null
            )
            writeMs = android.os.SystemClock.elapsedRealtime() - writeStarted
            onStageDuration?.invoke("Ghi file", writeMs)

            if (frame.watermarkPrinted && regionVerified != true) {
                val message = "CẢNH BÁO ĐỎ: watermark được yêu cầu nhưng vùng ảnh đã lưu chưa xác minh; marker=$verifiedMarker uri=${frame.uri}"
                CaptureDiagnostics.recordWarning(context, message)
                Log.e(TAG, message)
            }
            Log.i(
                TAG,
                "FINAL_WRITE uri=${frame.uri} marker=$verifiedMarker watermark=${frame.watermarkPrinted} " +
                    "size=${frame.width}x${frame.height} encode=${encodeMs}ms write=${writeMs}ms " +
                    "regionVerified=$regionVerified"
            )
            return Result(
                written = true,
                watermarkPrinted = frame.watermarkPrinted && regionVerified == true,
                marker = verifiedMarker,
                regionVerified = regionVerified,
                encodeDurationMs = encodeMs,
                writeDurationMs = writeMs
            )
        } catch (failure: Throwable) {
            Log.e(TAG, "Final JPEG sink failed uri=${frame.uri}", failure)
            throw failure
        } finally {
            runCatching { temp?.delete() }
        }
    }

    private fun writeExif(
        context: Context,
        uri: Uri,
        sourceExif: Map<String, String>,
        width: Int,
        height: Int,
        marker: String,
        verificationRegion: Rect?,
        verificationBackgroundColor: Int = android.graphics.Color.BLACK,
        verificationBaseline: String? = null
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
                val originalDescription = sourceExif[ExifInterface.TAG_IMAGE_DESCRIPTION].orEmpty()
                    .lineSequence()
                    .filterNot { it.startsWith("FM-WM-RECT=") }
                    .joinToString("\n")
                val regionDescription = verificationRegion?.let {
                    val baseline = verificationBaseline ?: error("Watermark verification baseline is missing")
                    "FM-WM-RECT=${it.left},${it.top},${it.right},${it.bottom};BG=$verificationBackgroundColor;BASE=$baseline"
                }
                val description = listOfNotNull(
                    originalDescription.takeIf { it.isNotBlank() },
                    regionDescription
                ).joinToString("\n")
                if (description.isNotBlank()) {
                    setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, description)
                }
                saveAttributes()
            }
        }
        val verification = context.contentResolver.openFileDescriptor(uri, "r")?.use {
            ExifInterface(it.fileDescriptor).getAttribute(ExifInterface.TAG_USER_COMMENT)
        }
        check(verification == marker) { "Final EXIF watermark marker mismatch: expected=$marker actual=$verification" }
    }

    /** Re-reads the persisted watermark rectangle from EXIF and verifies image pixels. */
    fun verifyStoredWatermark(context: Context, uri: Uri): Boolean {
        val spec = runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                val description = ExifInterface(descriptor.fileDescriptor)
                    .getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION).orEmpty()
                val match = Regex("FM-WM-RECT=(-?\\d+),(-?\\d+),(-?\\d+),(-?\\d+);BG=(-?\\d+);BASE=([A-Za-z0-9+/=]+)")
                    .find(description) ?: return@use null
                val values = match.groupValues.drop(1)
                val rect = Rect(values[0].toInt(), values[1].toInt(), values[2].toInt(), values[3].toInt())
                rect to values[5]
            }
        }.getOrNull() ?: return false
        return verifyWatermarkRegion(context, uri, spec.first, spec.second) == true
    }

    private fun verifyWatermarkRegion(
        context: Context,
        uri: Uri,
        requested: Rect,
        baselineBase64: String
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
                    val baseline = runCatching { Base64.decode(baselineBase64, Base64.NO_WRAP) }.getOrNull()
                        ?: return false
                    if (baseline.size < 2) return false
                    var verifiedSamples = 0
                    var totalSamples = 0
                    DataInputStream(ByteArrayInputStream(baseline)).use { input ->
                        val sampleCount = input.readUnsignedShort()
                        val expectedSize = 2 + sampleCount * 5
                        if (sampleCount < 8 || baseline.size != expectedSize) return false
                        repeat(sampleCount) {
                            val sampleIndex = input.readUnsignedShort()
                            val row = sampleIndex / WATERMARK_SAMPLE_COLUMNS
                            val column = sampleIndex % WATERMARK_SAMPLE_COLUMNS
                            if (row !in 0 until WATERMARK_SAMPLE_ROWS ||
                                column !in 0 until WATERMARK_SAMPLE_COLUMNS
                            ) return false
                            val expectedRed = input.readUnsignedByte()
                            val expectedGreen = input.readUnsignedByte()
                            val expectedBlue = input.readUnsignedByte()
                            val x = ((column + 0.5f) * bitmap.width / WATERMARK_SAMPLE_COLUMNS)
                                .toInt().coerceIn(0, bitmap.width - 1)
                            val y = ((row + 0.5f) * bitmap.height / WATERMARK_SAMPLE_ROWS)
                                .toInt().coerceIn(0, bitmap.height - 1)
                            val actual = bitmap.getPixel(x, y)
                            val delta = kotlin.math.abs(android.graphics.Color.red(actual) - expectedRed) +
                                kotlin.math.abs(android.graphics.Color.green(actual) - expectedGreen) +
                                kotlin.math.abs(android.graphics.Color.blue(actual) - expectedBlue)
                            if (delta <= 90) verifiedSamples++
                            totalSamples++
                        }
                    }
                    // Require at least 70% of changed source/render sample points to survive JPEG encode.
                    return totalSamples >= 8 && verifiedSamples * 100 >= totalSamples * 70
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

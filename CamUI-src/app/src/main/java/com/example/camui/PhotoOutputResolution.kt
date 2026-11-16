package com.example.camui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PhotoOutputResult(
    val width: Int,
    val height: Int,
    val requestedPixels: Long,
    val actualPixels: Long
) {
    // Integer dimensions on non-integer aspect ratios may fall a fraction below the target.
    val reachedRequestedSize: Boolean get() = actualPixels >= requestedPixels * 995L / 1000L
}

/**
 * LunaDream output sizing. It preserves the source frame ratio and reduces the
 * requested target only when the current device heap cannot safely hold the output bitmap.
 * Resampling sets pixel dimensions; it does not create optical detail.
 */
object PhotoOutputResolution {
    private const val TAG = "FUJI_LUNADREAM_RESAMPLE"
    private const val MIN_OUTPUT_PIXELS = 10_000_000L
    private const val RESERVED_HEAP_BYTES = 64L * 1024L * 1024L

    fun interpolateToMp(
        context: Context,
        uri: Uri,
        targetPixels: Long,
        targetAspectRatio: Float? = null
    ): PhotoOutputResult? {
        require(targetPixels in MIN_OUTPUT_PIXELS..250_000_000L) {
            "Unsupported output pixel target"
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val temp = File(context.cacheDir, "fuji-lunadream-$stamp.jpg")
        val backup = File(context.cacheDir, "fuji-lunadream-original-$stamp.jpg")
        var source: Bitmap? = null
        var output: Bitmap? = null
        var replacing = false
        return try {
            val tags = readExif(context, uri)
            source = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(
                    input,
                    null,
                    BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                        inDither = true
                    }
                )
            } ?: error("Cannot decode original JPEG")

            val src = source!!
            require(src.width > 0 && src.height > 0) { "Invalid source size" }

            // Keep geometry independent from bitmap allocation so 21:9, 16:9,
            // 4:3, 3:2, square and portrait crops can be regression-tested without
            // allocating a 250MP bitmap in a unit test.
            val geometry = PhotoOutputGeometry.calculate(
                src.width,
                src.height,
                targetPixels,
                targetAspectRatio
            )
            val sourceRect = Rect(
                geometry.cropLeft,
                geometry.cropTop,
                geometry.cropLeft + geometry.cropWidth,
                geometry.cropTop + geometry.cropHeight
            )
            val cropWidth = geometry.cropWidth
            val cropHeight = geometry.cropHeight

            // Never silently lower the requested MP count. The caller either receives the
            // requested output size or an explicit failure with the original photo untouched.
            // Keep 64 MiB free for JPEG/EXIF/MediaStore and transient Android allocations.
            val runtime = Runtime.getRuntime()
            val usedHeap = (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)
            val freeHeapHeadroom = (runtime.maxMemory() - usedHeap).coerceAtLeast(0L)
            val usableHeap = (freeHeapHeadroom - RESERVED_HEAP_BYTES).coerceAtLeast(0L)

            val outputGeometry = PhotoOutputGeometry.calculate(
                src.width,
                src.height,
                targetPixels,
                targetAspectRatio
            )
            val width = outputGeometry.outputWidth
            val height = outputGeometry.outputHeight
            val outputPixels = outputGeometry.outputPixels
            val outputBytes = outputPixels * 2L // RGB_565.
            require(outputPixels >= targetPixels * 995L / 1000L) {
                "Requested MP geometry was not reached; original photo retained"
            }
            require(outputBytes <= usableHeap) {
                "Not enough free heap for the requested output; original photo retained"
            }

            if (src.width == width && src.height == height) {
                return PhotoOutputResult(width, height, targetPixels, outputPixels)
            }

            output = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            Canvas(output!!).drawBitmap(
                src,
                sourceRect,
                Rect(0, 0, width, height),
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
                    isAntiAlias = true
                }
            )
            // Release the source before JPEG compression; the resized output is now complete.
            if (!src.isRecycled) src.recycle()
            source = null
            FileOutputStream(temp).use { stream ->
                check(output!!.compress(Bitmap.CompressFormat.JPEG, 98, stream)) {
                    "JPEG encoding failed"
                }
                stream.flush()
                stream.fd.sync()
            }
            check(temp.length() > 0L)
            writeExif(temp, tags)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(backup).use { stream ->
                    input.copyTo(stream)
                    stream.fd.sync()
                }
            } ?: error("Cannot back up source image")

            replacing = true
            val copied = context.contentResolver.openOutputStream(uri, "wt")?.use { dest ->
                temp.inputStream().use { it.copyTo(dest) }
                true
            } ?: false
            check(copied) { "MediaStore replacement failed" }

            context.contentResolver.openFileDescriptor(uri, "rw")?.use { fd ->
                ExifInterface(fd.fileDescriptor).apply {
                    setAttribute(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL.toString()
                    )
                    setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, width.toString())
                    setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, height.toString())
                    setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
                    setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
                    saveAttributes()
                }
            }
            context.contentResolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.Images.Media.ORIENTATION, 0)
                    put(MediaStore.Images.Media.WIDTH, width)
                    put(MediaStore.Images.Media.HEIGHT, height)
                },
                null,
                null
            )

            val actualAspect = width.toDouble() / height.toDouble()
            Log.i(
                TAG,
                "Requested=${targetPixels / 1_000_000L}MP actual=${outputPixels / 1_000_000L}MP " +
                    "source=${src.width}x${src.height}, crop=${cropWidth}x${cropHeight}, " +
                    "requestedAspect=${"%.5f".format(Locale.US, geometry.requestedAspectRatio)}, " +
                    "dimensions=${width}x${height} aspect=${"%.5f".format(Locale.US, actualAspect)}"
            )
            check(outputPixels >= targetPixels * 995L / 1000L) {
                "Output verification fell below the requested megapixel target"
            }
            Log.i(TAG, "Exact requested output accepted: requested=${targetPixels} actual=${outputPixels}")
            PhotoOutputResult(width, height, targetPixels, outputPixels)
        } catch (failure: Throwable) {
            if (replacing && backup.exists()) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { dest ->
                        backup.inputStream().use { it.copyTo(dest) }
                    } ?: error("Rollback failed")
                }.onFailure {
                    Log.e(TAG, "Original restore failed", it)
                }
            }
            Log.e(TAG, "LunaDream output sizing skipped; original retained", failure)
            null
        } finally {
            source?.let { if (!it.isRecycled) it.recycle() }
            output?.let { if (!it.isRecycled) it.recycle() }
            runCatching { temp.delete() }
            runCatching { backup.delete() }
        }
    }

    private fun readExif(context: Context, uri: Uri): Map<String, String> {
        val keys = listOf(
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_SOFTWARE,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_ISO_SPEED_RATINGS,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_FLASH
        )
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                val exif = ExifInterface(fd.fileDescriptor)
                keys.mapNotNull { key ->
                    exif.getAttribute(key)?.let { key to it }
                }.toMap()
            }.orEmpty()
        }.getOrDefault(emptyMap())
    }

    private fun writeExif(file: File, tags: Map<String, String>) {
        ExifInterface(file.absolutePath).apply {
            tags.forEach { (key, value) -> setAttribute(key, value) }
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            saveAttributes()
        }
    }
}

package com.example.camui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
    private const val MIN_OUTPUT_PIXELS = 4L
    private const val RESERVED_HEAP_BYTES = 64L * 1024L * 1024L

    /**
     * Preserve a normal camera image's pixel count while placing the complete image
     * inside the selected aspect ratio. This gives ordinary photos the same no-crop
     * behavior as HDMP/LunaDream output.
     */
    fun applyAspectRatio(
        context: Context,
        uri: Uri,
        targetAspectRatio: Float
    ): PhotoOutputResult? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
            bounds.outWidth > 0 && bounds.outHeight > 0
        } ?: false
        if (!decoded) return null
        val sourcePixels = bounds.outWidth.toLong() * bounds.outHeight.toLong()
        if (sourcePixels !in MIN_OUTPUT_PIXELS..250_000_000L) return null
        return interpolateToMp(context, uri, sourcePixels, targetAspectRatio)
    }

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
            val sourceWidth = src.width
            val sourceHeight = src.height
            require(sourceWidth > 0 && sourceHeight > 0) { "Invalid source size" }

            // Geometry is independently unit-tested. The whole source is fitted into
            // the target canvas, leaving black bars where the selected ratio is wider/taller.
            val geometry = PhotoOutputGeometry.calculate(
                sourceWidth,
                sourceHeight,
                targetPixels,
                targetAspectRatio
            )
            val imageRect = Rect(
                geometry.imageLeft,
                geometry.imageTop,
                geometry.imageLeft + geometry.imageWidth,
                geometry.imageTop + geometry.imageHeight
            )

            // Never silently lower the requested MP count. The caller either receives the
            // requested output size or an explicit failure with the original photo untouched.
            // Keep 64 MiB free for JPEG/EXIF/MediaStore and transient Android allocations.
            val runtime = Runtime.getRuntime()
            val usedHeap = (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)
            val freeHeapHeadroom = (runtime.maxMemory() - usedHeap).coerceAtLeast(0L)
            val usableHeap = (freeHeapHeadroom - RESERVED_HEAP_BYTES).coerceAtLeast(0L)

            val width = geometry.outputWidth
            val height = geometry.outputHeight
            val outputPixels = geometry.outputPixels
            val outputBytes = outputPixels * 2L // RGB_565.
            require(outputPixels >= targetPixels * 995L / 1000L) {
                "Requested MP geometry was not reached; original photo retained"
            }
            require(outputBytes <= usableHeap) {
                "Not enough free heap for the requested output; original photo retained"
            }

            if (sourceWidth == width && sourceHeight == height &&
                imageRect.left == 0 && imageRect.top == 0 &&
                imageRect.width() == width && imageRect.height() == height
            ) {
                return PhotoOutputResult(width, height, targetPixels, outputPixels)
            }

            output = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            Canvas(output!!).apply {
                drawAmbientBackdrop(this, src, width, height)
                drawBitmap(
                    src,
                    null,
                    imageRect,
                    Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
                        isAntiAlias = true
                    }
                )
            }
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
                    "source=${sourceWidth}x${sourceHeight}, fitted=${geometry.imageWidth}x${geometry.imageHeight}, " +
                    "bars=${geometry.imageLeft},${geometry.imageTop}, " +
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

    /**
     * Creates a low-detail ambient fill behind the untouched, uniformly fitted photo.
     * This avoids solid-black bars without cropping, zooming, or stretching the main image.
     */
    private fun drawAmbientBackdrop(
        canvas: Canvas,
        source: Bitmap,
        outputWidth: Int,
        outputHeight: Int
    ) {
        val ambientSize = 128
        val ambient = Bitmap.createBitmap(ambientSize, ambientSize, Bitmap.Config.RGB_565)
        try {
            Canvas(ambient).drawBitmap(
                source,
                null,
                Rect(0, 0, ambientSize, ambientSize),
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
            )
            canvas.drawBitmap(
                ambient,
                null,
                Rect(0, 0, outputWidth, outputHeight),
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply { alpha = 232 }
            )
        } finally {
            if (!ambient.isRecycled) ambient.recycle()
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

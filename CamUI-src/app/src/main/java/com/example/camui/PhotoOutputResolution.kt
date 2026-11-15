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
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PhotoOutputResult(
    val width: Int,
    val height: Int,
    val requestedPixels: Long,
    val actualPixels: Long
) {
    val reachedRequestedSize: Boolean get() = actualPixels >= requestedPixels
}

/**
 * HDMP algorithmic resampling. It preserves the source frame ratio and reduces the
 * requested target only when the current device heap cannot safely hold the output bitmap.
 * This is interpolation, not optical detail recovery.
 */
object PhotoOutputResolution {
    private const val TAG = "FUJI_HDMP_RESAMPLE"
    private const val MIN_OUTPUT_PIXELS = 10_000_000L
    private const val RESERVED_HEAP_BYTES = 96L * 1024L * 1024L
    private const val HEAP_BUDGET_FRACTION = 0.75

    fun interpolateToMp(
        context: Context,
        uri: Uri,
        targetPixels: Long
    ): PhotoOutputResult? {
        require(targetPixels in MIN_OUTPUT_PIXELS..250_000_000L) {
            "Unsupported output pixel target"
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val temp = File(context.cacheDir, "fuji-hdmp-$stamp.jpg")
        val backup = File(context.cacheDir, "fuji-hdmp-original-$stamp.jpg")
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
            val sourcePixels = src.width.toLong() * src.height.toLong()
            require(sourcePixels > 0L) { "Invalid source size" }

            // Budget against the heap still available now, not the process' theoretical max heap.
            // The source bitmap is already allocated; reserve extra space for JPEG encoding,
            // EXIF writing, Compose/camera state and transient framework allocations.
            val runtime = Runtime.getRuntime()
            val usedHeap = (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)
            val freeHeapHeadroom = (runtime.maxMemory() - usedHeap).coerceAtLeast(0L)
            val usableHeap = (
                (freeHeapHeadroom - RESERVED_HEAP_BYTES).coerceAtLeast(0L) *
                    HEAP_BUDGET_FRACTION
                ).toLong()
            val maxPixelsForHeap = usableHeap / 2L // RGB_565 = two bytes per pixel.
            val actualTargetPixels = minOf(targetPixels, maxPixelsForHeap)
            require(actualTargetPixels >= MIN_OUTPUT_PIXELS) {
                "Insufficient safe memory for a 10MP output; original photo retained"
            }

            val aspectRatio = src.width.toDouble() / src.height.coerceAtLeast(1)
            val scale = sqrt(actualTargetPixels.toDouble() / sourcePixels.toDouble())
            var height = (src.height * scale).roundToInt().coerceAtLeast(2)
            var width = (height * aspectRatio).roundToInt().coerceAtLeast(2)

            // Keep the exact source aspect ratio as closely as integer pixel dimensions allow.
            while (width.toLong() * height.toLong() > actualTargetPixels) {
                val currentAspect = width.toDouble() / height.toDouble()
                if (currentAspect > aspectRatio && width > 2) width--
                else if (height > 2) height--
                else if (width > 2) width--
                else error("Cannot fit output dimensions into memory budget")
            }
            val outputPixels = width.toLong() * height.toLong()
            val outputBytes = outputPixels * 2L
            require(outputBytes <= usableHeap) {
                "Insufficient safe heap for requested output resolution"
            }

            if (src.width == width && src.height == height) {
                return PhotoOutputResult(width, height, targetPixels, outputPixels)
            }

            output = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            Canvas(output!!).drawBitmap(
                src,
                null,
                Rect(0, 0, width, height),
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
                    isAntiAlias = true
                }
            )
            FileOutputStream(temp).use { stream ->
                check(output!!.compress(Bitmap.CompressFormat.JPEG, 96, stream)) {
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
                    "dimensions=${width}x${height} aspect=${"%.5f".format(Locale.US, actualAspect)}"
            )
            if (actualTargetPixels < targetPixels) {
                Log.w(
                    TAG,
                    "Output size reduced by memory guard: requested=${targetPixels}, actual=${outputPixels}"
                )
            }
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
            Log.e(TAG, "HDMP interpolation skipped; keep source", failure)
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

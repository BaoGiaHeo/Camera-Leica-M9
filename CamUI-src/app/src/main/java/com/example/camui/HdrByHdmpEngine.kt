package com.example.camui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * HDRbyHDMP is a separate, device-independent software tone-mapping engine.
 *
 * It uses the captured exposure only (not a bracketed multi-exposure stack). It can balance
 * recorded shadow/highlight values, but cannot recover detail clipped by the sensor. The 2x
 * and 10x profiles are internal processing-strength presets, not measured quality multipliers.
 */
internal object HdrByHdmpEngine {
    private const val TAG = "HDR_BY_HDMP"
    private const val STRIPE_HEIGHT = 48

    fun process(
        context: Context,
        uri: Uri,
        qualityMultiplier: Int,
        onProgress: (Int) -> Unit = {}
    ): Boolean {
        val profile = if (qualityMultiplier >= 10) 1f else 0.32f
        val temp = File(context.cacheDir, "hdrbyhdmp-${System.nanoTime()}.jpg")
        val backup = File(context.cacheDir, "hdrbyhdmp-original-${System.nanoTime()}.jpg")
        var bitmap: Bitmap? = null
        var replaced = false
        return try {
            val attributes = readExif(context, uri)
            context.contentResolver.openInputStream(uri)?.use { input ->
// JPEG_INTERMEDIATE_ALLOWLIST: HDRbyHDMP staging/backup/rollback write; final camera capture is finalized through ImageSink.
                FileOutputStream(backup).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            } ?: error("HDRbyHDMP cannot read source image")

            bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(
                    input,
                    null,
                    BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                        inMutable = true
                        inDither = true
                    }
                )
            } ?: error("HDRbyHDMP cannot decode source image")

            val frame = bitmap!!
            val width = frame.width
            val height = frame.height
            require(width >= 3 && height >= 3) { "HDRbyHDMP source is too small" }

            var previous = IntArray(width)
            var current = IntArray(width)
            var next = IntArray(width)
            var previousLuma = FloatArray(width)
            var currentLuma = FloatArray(width)
            var nextLuma = FloatArray(width)
            val output = IntArray(width)

            fun fillLuminance(source: IntArray, destination: FloatArray) {
                for (x in source.indices) {
                    val pixel = source[x]
                    destination[x] = (
                        Color.red(pixel) * 0.2126f +
                            Color.green(pixel) * 0.7152f +
                            Color.blue(pixel) * 0.0722f
                        ) / 255f
                }
            }

            frame.getPixels(current, 0, width, 0, 0, width, 1)
            current.copyInto(previous)
            fillLuminance(current, currentLuma)
            currentLuma.copyInto(previousLuma)
            if (height > 1) {
                frame.getPixels(next, 0, width, 0, 1, width, 1)
                fillLuminance(next, nextLuma)
            } else {
                current.copyInto(next)
                currentLuma.copyInto(nextLuma)
            }

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = current[x]
                    val alpha = Color.alpha(pixel)
                    val red = Color.red(pixel).toFloat()
                    val green = Color.green(pixel).toFloat()
                    val blue = Color.blue(pixel).toFloat()
                    val luma = currentLuma[x]

                    val left = currentLuma[max(0, x - 1)]
                    val right = currentLuma[min(width - 1, x + 1)]
                    val up = previousLuma[x]
                    val down = nextLuma[x]
                    val localMean = (left + right + up + down) * 0.25f

                    val shadowLift = 0.105f * profile * (1f - luma) * (1f - luma)
                    val highlightAmount = ((luma - 0.70f) / 0.30f).coerceIn(0f, 1f)
                    val highlightCompression = 0.09f * profile * highlightAmount * highlightAmount
                    val localContrast = (luma - localMean).coerceIn(-0.08f, 0.08f) * (0.12f * profile)
                    val mapped = (luma + shadowLift - highlightCompression + localContrast)
                        .coerceIn(0f, 1f)
                    val scale = if (luma > 0.015f) {
                        (mapped / luma).coerceIn(0.65f, 1.45f)
                    } else if (mapped > luma) {
                        1f + (mapped - luma) * 8f
                    } else 1f

                    output[x] = Color.argb(
                        alpha,
                        (red * scale).toInt().coerceIn(0, 255),
                        (green * scale).toInt().coerceIn(0, 255),
                        (blue * scale).toInt().coerceIn(0, 255)
                    )
                }
                frame.setPixels(output, 0, width, 0, y, width, 1)

                if (y + 1 < height) {
                    val recycled = previous
                    previous = current
                    current = next
                    next = recycled

                    val recycledLuma = previousLuma
                    previousLuma = currentLuma
                    currentLuma = nextLuma
                    nextLuma = recycledLuma

                    if (y + 2 < height) {
                        frame.getPixels(next, 0, width, 0, y + 2, width, 1)
                        fillLuminance(next, nextLuma)
                    } else {
                        current.copyInto(next)
                        currentLuma.copyInto(nextLuma)
                    }
                }

                if (y % STRIPE_HEIGHT == 0 || y == height - 1) {
                    runCatching { onProgress(((y + 1) * 100 / height).coerceIn(0, 100)) }
                }
            }

// JPEG_INTERMEDIATE_ALLOWLIST: HDRbyHDMP staging/backup/rollback write; final camera capture is finalized through ImageSink.
            FileOutputStream(temp).use { stream ->
// JPEG_INTERMEDIATE_ALLOWLIST: HDRbyHDMP staging/backup/rollback write; final camera capture is finalized through ImageSink.
                check(frame.compress(Bitmap.CompressFormat.JPEG, 98, stream)) {
                    "HDRbyHDMP JPEG encode failed"
                }
                stream.flush()
                stream.fd.sync()
            }
            writeExif(temp, attributes)
            replaced = true
// JPEG_INTERMEDIATE_ALLOWLIST: HDRbyHDMP staging/backup/rollback write; final camera capture is finalized through ImageSink.
            val written = context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                temp.inputStream().use { it.copyTo(destination) }
                true
            } ?: false
            check(written) { "HDRbyHDMP cannot write processed image" }
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
                ExifInterface(descriptor.fileDescriptor).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                    saveAttributes()
                }
            }
            context.contentResolver.update(
                uri,
                ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.ORIENTATION, 0)
                },
                null,
                null
            )
            Log.i(TAG, "Applied profile=${qualityMultiplier}x to ${width}x${height} using bounded scanlines")
            true
        } catch (failure: Throwable) {
            if (replaced && backup.exists()) runCatching {
// JPEG_INTERMEDIATE_ALLOWLIST: HDRbyHDMP staging/backup/rollback write; final camera capture is finalized through ImageSink.
                context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                    backup.inputStream().use { it.copyTo(destination) }
                } ?: error("HDRbyHDMP rollback stream unavailable")
            }.onFailure { Log.e(TAG, "HDRbyHDMP rollback failed", it) }
            Log.e(TAG, "HDRbyHDMP failed; original image retained when possible", failure)
            false
        } finally {
            bitmap?.let { if (!it.isRecycled) it.recycle() }
            runCatching { temp.delete() }
            runCatching { backup.delete() }
        }
    }

    private fun readExif(context: Context, uri: Uri): Map<String, String> {
        val tags = listOf(
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
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                val exif = ExifInterface(descriptor.fileDescriptor)
                tags.mapNotNull { tag -> exif.getAttribute(tag)?.let { tag to it } }.toMap()
            }.orEmpty()
        }.getOrDefault(emptyMap())
    }

    private fun writeExif(file: File, attributes: Map<String, String>) {
        ExifInterface(file.absolutePath).apply {
            attributes.forEach { (tag, value) -> setAttribute(tag, value) }
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            saveAttributes()
        }
    }
}

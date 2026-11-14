package com.example.camui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as PixelColor
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * HDMP is Fujifilm Mode's in-app, non-ML image-processing engine.
 * Edge-aware denoising, adaptive high-pass detail recovery and luminance sharpening are
 * processed as bounded row tiles. Resolution interpolation is a separate final stage.
 */
internal object HdmpEngine {
    private const val TAG = "FUJI_HDMP_ENGINE"
    private const val STRIPE_HEIGHT = 64

    fun process(
        context: Context,
        uri: Uri,
        workerCount: Int,
        onProgress: (Int) -> Unit = {}
    ): Boolean {
        val workers = workerCount.coerceIn(1, 8)
        val stamp = System.currentTimeMillis().toString()
        val processedFile = File(context.cacheDir, "hdmp-processed-$stamp.jpg")
        val backupFile = File(context.cacheDir, "hdmp-original-$stamp.jpg")
        var source: Bitmap? = null
        var output: Bitmap? = null
        var replacing = false
        return try {
            val exif = readExif(context, uri)
            source = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(
                    input,
                    null,
                    BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                        inDither = true
                    }
                )
            } ?: error("HDMP could not decode source image")
            val src = source!!
            val width = src.width
            val height = src.height
            require(width >= 3 && height >= 3) { "HDMP source is too small" }
            output = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val dst = output!!
            val stripeCount = (height + STRIPE_HEIGHT - 1) / STRIPE_HEIGHT
            val completed = AtomicInteger(0)
            val executor = Executors.newFixedThreadPool(workers)
            try {
                val tasks = ArrayList<Callable<Unit>>(stripeCount)
                for (top in 0 until height step STRIPE_HEIGHT) {
                    val bottom = min(height, top + STRIPE_HEIGHT)
                    tasks += Callable {
                        val inputTop = max(0, top - 1)
                        val inputBottom = min(height, bottom + 1)
                        val inputHeight = inputBottom - inputTop
                        val input = IntArray(width * inputHeight)
                        src.getPixels(input, 0, width, 0, inputTop, width, inputHeight)
                        val outputPixels = IntArray(width * (bottom - top))
                        val strength = if (workers >= 8) 0.42f else 0.36f

                        fun luma(pixel: Int): Float =
                            (PixelColor.red(pixel) * 0.2126f +
                                PixelColor.green(pixel) * 0.7152f +
                                PixelColor.blue(pixel) * 0.0722f) / 255f

                        for (y in top until bottom) {
                            val localY = y - inputTop
                            val row = localY * width
                            val upRow = max(0, localY - 1) * width
                            val downRow = min(inputHeight - 1, localY + 1) * width
                            val outRow = (y - top) * width
                            for (x in 0 until width) {
                                val center = input[row + x]
                                val centerLuma = luma(center)
                                val left = input[row + max(0, x - 1)]
                                val right = input[row + min(width - 1, x + 1)]
                                val up = input[upRow + x]
                                val down = input[downRow + x]

                                // Edge-aware cross-neighbour smoothing suppresses similar-pixel noise.
                                var weightSum = 2.5f
                                var redSum = PixelColor.red(center) * 2.5f
                                var greenSum = PixelColor.green(center) * 2.5f
                                var blueSum = PixelColor.blue(center) * 2.5f
                                for (neighbor in intArrayOf(left, right, up, down)) {
                                    val difference = abs(luma(neighbor) - centerLuma)
                                    val weight = if (difference < 0.10f) {
                                        (1f - difference * 8f).coerceAtLeast(0.12f)
                                    } else 0.08f
                                    weightSum += weight
                                    redSum += PixelColor.red(neighbor) * weight
                                    greenSum += PixelColor.green(neighbor) * weight
                                    blueSum += PixelColor.blue(neighbor) * weight
                                }
                                val denoiseMix = 0.16f
                                val baseRed = PixelColor.red(center) * (1f - denoiseMix) +
                                    (redSum / weightSum) * denoiseMix
                                val baseGreen = PixelColor.green(center) * (1f - denoiseMix) +
                                    (greenSum / weightSum) * denoiseMix
                                val baseBlue = PixelColor.blue(center) * (1f - denoiseMix) +
                                    (blueSum / weightSum) * denoiseMix
                                val baseLuma = (baseRed * 0.2126f + baseGreen * 0.7152f +
                                    baseBlue * 0.0722f) / 255f

                                val neighbourLuma = (luma(left) + luma(right) +
                                    luma(up) + luma(down)) * 0.25f
                                val detail = baseLuma - neighbourLuma
                                // Ignore tiny high-frequency differences so sharpening does not amplify noise.
                                val recoveredDetail = if (abs(detail) < 0.012f) 0f else {
                                    detail * strength * if (abs(detail) < 0.035f) 0.72f else 1f
                                }
                                val targetLuma = (baseLuma + recoveredDetail).coerceIn(0f, 1f)
                                val scale = if (baseLuma > 0.015f) {
                                    (targetLuma / baseLuma).coerceIn(0.72f, 1.28f)
                                } else 1f
                                outputPixels[outRow + x] = PixelColor.rgb(
                                    (baseRed * scale).toInt().coerceIn(0, 255),
                                    (baseGreen * scale).toInt().coerceIn(0, 255),
                                    (baseBlue * scale).toInt().coerceIn(0, 255)
                                )
                            }
                        }
                        synchronized(dst) {
                            dst.setPixels(outputPixels, 0, width, 0, top, width, bottom - top)
                        }
                        val done = completed.incrementAndGet()
                        runCatching { onProgress((done * 100 / stripeCount).coerceIn(0, 100)) }
                    }
                }
                executor.invokeAll(tasks).forEach { it.get() }
            } finally {
                executor.shutdownNow()
            }

            FileOutputStream(processedFile).use { stream ->
                check(dst.compress(Bitmap.CompressFormat.JPEG, 96, stream)) {
                    "HDMP JPEG encoding failed"
                }
                stream.flush()
                stream.fd.sync()
            }
            writeExif(processedFile, exif)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(backupFile).use { stream ->
                    input.copyTo(stream)
                    stream.fd.sync()
                }
            } ?: error("HDMP could not create an original-image backup")
            replacing = true
            val copied = context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                processedFile.inputStream().use { it.copyTo(destination) }
                true
            } ?: false
            check(copied) { "HDMP could not update MediaStore image" }
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
                ExifInterface(descriptor.fileDescriptor).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                    saveAttributes()
                }
            }
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(android.provider.MediaStore.Images.Media.ORIENTATION, 0) },
                null,
                null
            )
            Log.i(TAG, "Processed ${width}x${height} with $workers parallel workers")
            true
        } catch (failure: Throwable) {
            if (replacing && backupFile.exists()) runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                    backupFile.inputStream().use { it.copyTo(destination) }
                } ?: error("HDMP rollback stream unavailable")
            }.onFailure { Log.e(TAG, "Could not restore source after HDMP failure", it) }
            Log.e(TAG, "HDMP processing skipped; original image retained when possible", failure)
            false
        } finally {
            source?.let { if (!it.isRecycled) it.recycle() }
            output?.let { if (!it.isRecycled) it.recycle() }
            runCatching { processedFile.delete() }
            runCatching { backupFile.delete() }
        }
    }

    private fun readExif(context: Context, uri: Uri): Map<String, String> {
        val keys = listOf(
            ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED, ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL,
            ExifInterface.TAG_SOFTWARE, ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_ISO_SPEED_RATINGS, ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_WHITE_BALANCE, ExifInterface.TAG_FLASH
        )
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                val metadata = ExifInterface(descriptor.fileDescriptor)
                keys.mapNotNull { key -> metadata.getAttribute(key)?.let { key to it } }.toMap()
            }.orEmpty()
        }.getOrDefault(emptyMap())
    }

    private fun writeExif(file: File, attributes: Map<String, String>) {
        ExifInterface(file.absolutePath).apply {
            attributes.forEach { (key, value) -> setAttribute(key, value) }
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            saveAttributes()
        }
    }
}

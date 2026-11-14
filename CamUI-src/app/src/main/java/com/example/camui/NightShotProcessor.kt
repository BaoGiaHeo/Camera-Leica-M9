package com.example.camui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * NightShot uses a multi-frame running average to reduce sensor/JPEG noise.
 * Star Trails uses a per-channel lighten/max composite so bright star positions
 * accumulate into trails. Processing is row-based and keeps only two RGB_565
 * frames in memory at a time.
 */
object NightShotProcessor {
    private const val TAG = "FUJI_NIGHTSHOT"
    private const val MAX_OUTPUT_PIXELS = 12_000_000L
    private val EXIF_TAGS = listOf(
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

    enum class MergeMode { NIGHT_AVERAGE, STAR_TRAILS }

    fun copyUriToFile(context: Context, uri: Uri, destination: File): Boolean =
        runCatching {
            destination.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            } ?: error("Cannot read the first camera frame")
            destination.length() > 0L
        }.onFailure { Log.e(TAG, "Could not copy first frame", it) }.getOrDefault(false)

    suspend fun captureFrameToFile(
        capture: ImageCapture,
        destination: File,
        callbackExecutor: Executor
    ): Boolean = suspendCancellableCoroutine { continuation ->
        runCatching {
            destination.parentFile?.mkdirs()
            if (destination.exists()) destination.delete()
            val options = ImageCapture.OutputFileOptions.Builder(destination).build()
            capture.takePicture(
                options,
                callbackExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val success = destination.exists() && destination.length() > 0L
                        if (continuation.isActive) continuation.resume(success)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Frame capture failed", exception)
                        runCatching { destination.delete() }
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
            )
        }.onFailure {
            Log.e(TAG, "Could not start frame capture", it)
            runCatching { destination.delete() }
            if (continuation.isActive) continuation.resume(false)
        }
        continuation.invokeOnCancellation { runCatching { destination.delete() } }
    }

    fun mergeFramesIntoUri(
        context: Context,
        frames: List<File>,
        destinationUri: Uri,
        mode: MergeMode
    ): Boolean {
        if (frames.size < 2 || frames.any { !it.exists() || it.length() <= 0L }) return false
        val prefix = if (mode == MergeMode.STAR_TRAILS) "star-" else "night-"
        val tempOutput = File(context.cacheDir, "fuji-" + prefix + System.nanoTime() + ".jpg")
        var base: Bitmap? = null
        return try {
            val firstExif = readExif(frames.first())
            base = decodeUpright(frames.first(), null, null)
                ?: error("Could not decode first frame")
            val width = base!!.width
            val height = base!!.height
            val oldRow = IntArray(width)
            val newRow = IntArray(width)

            for (frameIndex in 1 until frames.size) {
                val current = decodeUpright(frames[frameIndex], width, height)
                    ?: error("Could not decode frame " + (frameIndex + 1))
                try {
                    for (y in 0 until height) {
                        base!!.getPixels(oldRow, 0, width, 0, y, width, 1)
                        current.getPixels(newRow, 0, width, 0, y, width, 1)
                        for (x in 0 until width) {
                            val a = oldRow[x]
                            val b = newRow[x]
                            val ar = Color.red(a)
                            val ag = Color.green(a)
                            val ab = Color.blue(a)
                            val br = Color.red(b)
                            val bg = Color.green(b)
                            val bb = Color.blue(b)
                            val r: Int
                            val g: Int
                            val blue: Int
                            if (mode == MergeMode.STAR_TRAILS) {
                                r = max(ar, br)
                                g = max(ag, bg)
                                blue = max(ab, bb)
                            } else {
                                val divisor = frameIndex + 1
                                r = (ar * frameIndex + br) / divisor
                                g = (ag * frameIndex + bg) / divisor
                                blue = (ab * frameIndex + bb) / divisor
                            }
                            oldRow[x] = Color.rgb(r, g, blue)
                        }
                        base!!.setPixels(oldRow, 0, width, 0, y, width, 1)
                    }
                } finally {
                    current.recycle()
                }
            }

            FileOutputStream(tempOutput).use { output ->
                check(base!!.compress(Bitmap.CompressFormat.JPEG, 96, output)) {
                    "Could not encode merged image"
                }
                output.flush()
                output.fd.sync()
            }
            writeExif(tempOutput, firstExif)
            context.contentResolver.openOutputStream(destinationUri, "wt")?.use { output ->
                tempOutput.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not write merged image to MediaStore")
            context.contentResolver.update(
                destinationUri,
                ContentValues().apply { put(MediaStore.Images.Media.ORIENTATION, 0) },
                null,
                null
            )
            Log.i(TAG, "Merged " + frames.size + " frames as " + mode +
                " at " + width + "x" + height)
            true
        } catch (failure: Throwable) {
            Log.e(TAG, "Multi-frame merge failed; original capture remains available", failure)
            false
        } finally {
            base?.recycle()
            runCatching { tempOutput.delete() }
        }
    }

    fun saveStarTrails(context: Context, frames: List<File>): Uri? {
        if (frames.size < 2) return null
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "FUJI_STAR_TRAILS_" + stamp + ".jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FujifilmMode")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return null
        val merged = mergeFramesIntoUri(context, frames, uri, MergeMode.STAR_TRAILS)
        if (!merged) {
            runCatching { context.contentResolver.delete(uri, null, null) }
            return null
        }
        val published = context.contentResolver.update(
            uri,
            ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
                put(MediaStore.Images.Media.ORIENTATION, 0)
            },
            null,
            null
        )
        if (published <= 0) {
            runCatching { context.contentResolver.delete(uri, null, null) }
            return null
        }
        return uri
    }

    private fun decodeUpright(file: File, targetWidth: Int?, targetHeight: Int?): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull()
        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        ) ?: ExifInterface.ORIENTATION_NORMAL
        val swapsAxes = orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270
        val visibleWidth = if (swapsAxes) bounds.outHeight else bounds.outWidth
        val visibleHeight = if (swapsAxes) bounds.outWidth else bounds.outHeight

        val wantedWidth: Int
        val wantedHeight: Int
        if (targetWidth != null && targetHeight != null) {
            wantedWidth = targetWidth
            wantedHeight = targetHeight
        } else {
            val pixels = visibleWidth.toLong() * visibleHeight.toLong()
            val factor = if (pixels > MAX_OUTPUT_PIXELS) {
                sqrt(MAX_OUTPUT_PIXELS.toDouble() / pixels.toDouble())
            } else 1.0
            wantedWidth = (visibleWidth * factor).toInt().coerceAtLeast(1)
            wantedHeight = (visibleHeight * factor).toInt().coerceAtLeast(1)
        }

        val scaleByWidth = visibleWidth.toFloat() / wantedWidth.toFloat()
        val scaleByHeight = visibleHeight.toFloat() / wantedHeight.toFloat()
        val scale = min(scaleByWidth, scaleByHeight).coerceAtLeast(1f)
        var sampleSize = 1
        while (sampleSize * 2 <= scale) sampleSize *= 2

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
            inDither = true
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
        }
        var upright = if (matrix.isIdentity) {
            decoded
        } else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                if (it !== decoded) decoded.recycle()
            }
        }
        if (upright.width != wantedWidth || upright.height != wantedHeight) {
            val scaled = Bitmap.createScaledBitmap(upright, wantedWidth, wantedHeight, true)
            if (scaled !== upright) upright.recycle()
            upright = scaled
        }
        if (upright.config != Bitmap.Config.RGB_565) {
            val compact = upright.copy(Bitmap.Config.RGB_565, false) ?: return upright
            upright.recycle()
            upright = compact
        }
        return upright
    }

    private fun readExif(file: File): Map<String, String> = runCatching {
        val exif = ExifInterface(file.absolutePath)
        EXIF_TAGS.mapNotNull { tag -> exif.getAttribute(tag)?.let { tag to it } }.toMap()
    }.getOrDefault(emptyMap())

    private fun writeExif(file: File, tags: Map<String, String>) {
        runCatching {
            ExifInterface(file.absolutePath).apply {
                tags.forEach { (tag, value) -> setAttribute(tag, value) }
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL.toString()
                )
                saveAttributes()
            }
        }.onFailure { Log.w(TAG, "Could not restore every EXIF tag", it) }
    }
}

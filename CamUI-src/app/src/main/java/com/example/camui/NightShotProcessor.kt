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
        mode: MergeMode,
        onProgress: ((Int) -> Unit)? = null
    ): Boolean {
        if (frames.size < 2 || frames.any { !it.exists() || it.length() <= 0L }) return false
        val prefix = if (mode == MergeMode.STAR_TRAILS) "star-" else "night-"
        val tempOutput = File(context.cacheDir, "fuji-" + prefix + System.nanoTime() + ".jpg")
        var base: Bitmap? = null
        var alignmentReference: Bitmap? = null
        return try {
            val firstExif = readExif(frames.first())
            base = decodeUpright(frames.first(), null, null)
                ?: error("Could not decode first frame")
            val width = base!!.width
            val height = base!!.height
            val oldRow = IntArray(width)
            val newRow = IntArray(width)
            // Keep a small, immutable reference image for alignment. Using the changing
            // accumulated output as a reference would allow drift to build up over a burst.
            if (mode == MergeMode.NIGHT_AVERAGE) {
                alignmentReference = makeAlignmentThumbnail(base!!)
            }

            val totalMergedFrames = (frames.size - 1).coerceAtLeast(1)
            for (frameIndex in 1 until frames.size) {
                val current = decodeUpright(frames[frameIndex], width, height)
                    ?: error("Could not decode frame " + (frameIndex + 1))
                try {
                    val shift = if (mode == MergeMode.NIGHT_AVERAGE) {
                        estimateGlobalTranslation(alignmentReference!!, current)
                    } else {
                        0 to 0
                    }
                    val shiftX = shift.first.coerceIn(-width / 12, width / 12)
                    val shiftY = shift.second.coerceIn(-height / 12, height / 12)
                    if (mode == MergeMode.NIGHT_AVERAGE && (shiftX != 0 || shiftY != 0)) {
                        Log.d(TAG, "Aligned frame ${frameIndex + 1}: dx=$shiftX dy=$shiftY")
                    }

                    val progressRowInterval = (height / 8).coerceAtLeast(1)
                    for (y in 0 until height) {
                        if (y % progressRowInterval == 0) {
                            val frameFraction = (frameIndex - 1 + y.toFloat() / height) /
                                totalMergedFrames.toFloat()
                            runCatching { onProgress?.invoke((5 + frameFraction * 90f).toInt()) }
                        }
                        val sourceY = (y + shiftY).coerceIn(0, height - 1)
                        base!!.getPixels(oldRow, 0, width, 0, y, width, 1)
                        current.getPixels(newRow, 0, width, 0, sourceY, width, 1)
                        for (x in 0 until width) {
                            val sourceX = (x + shiftX).coerceIn(0, width - 1)
                            val a = oldRow[x]
                            val b = newRow[sourceX]
                            val ar = Color.red(a)
                            val ag = Color.green(a)
                            val ab = Color.blue(a)
                            val br = Color.red(b)
                            val bg = Color.green(b)
                            val bb = Color.blue(b)
                            if (mode == MergeMode.STAR_TRAILS) {
                                // Max/lighten accumulation draws the path of stars, while a small
                                // luminance threshold prevents tiny sensor noise from winning every
                                // pixel simply because more frames were captured.
                                val oldLuma = luma(ar, ag, ab)
                                val newLuma = luma(br, bg, bb)
                                if (newLuma >= STAR_TRAIL_MIN_LUMA &&
                                    newLuma >= oldLuma + STAR_TRAIL_MIN_DELTA
                                ) {
                                    oldRow[x] = Color.rgb(max(ar, br), max(ag, bg), max(ab, bb))
                                }
                            } else {
                                val divisor = frameIndex + 1
                                oldRow[x] = Color.rgb(
                                    (ar * frameIndex + br) / divisor,
                                    (ag * frameIndex + bg) / divisor,
                                    (ab * frameIndex + bb) / divisor
                                )
                            }
                        }
                        base!!.setPixels(oldRow, 0, width, 0, y, width, 1)
                    }
                } finally {
                    current.recycle()
                }
            }

            runCatching { onProgress?.invoke(96) }
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
            runCatching { onProgress?.invoke(100) }
            Log.i(TAG, "Merged " + frames.size + " frames as " + mode +
                " at " + width + "x" + height)
            true
        } catch (failure: Throwable) {
            Log.e(TAG, "Multi-frame merge failed; original capture remains available", failure)
            false
        } finally {
            alignmentReference?.recycle()
            base?.recycle()
            runCatching { tempOutput.delete() }
        }
    }

    private const val ALIGNMENT_MAX_DIMENSION = 160
    private const val ALIGNMENT_MAX_SHIFT = 6
    private const val STAR_TRAIL_MIN_LUMA = 22
    private const val STAR_TRAIL_MIN_DELTA = 5

    private fun luma(red: Int, green: Int, blue: Int): Int =
        (red * 3 + green * 6 + blue) / 10

    private fun makeAlignmentThumbnail(source: Bitmap): Bitmap {
        val scale = min(
            1f,
            ALIGNMENT_MAX_DIMENSION.toFloat() / max(source.width, source.height).toFloat()
        )
        val width = (source.width * scale).toInt().coerceAtLeast(32)
        val height = (source.height * scale).toInt().coerceAtLeast(32)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    /**
     * Estimates a small global translation using luminance gradients on a thumbnail.
     * Gradients are less sensitive to exposure changes between auto-exposed night frames.
     * The returned shift tells the merger which source pixel to read for each reference pixel.
     */
    private fun estimateGlobalTranslation(
        referenceThumbnail: Bitmap,
        currentFrame: Bitmap
    ): Pair<Int, Int> {
        val thumbnail = makeAlignmentThumbnail(currentFrame)
        try {
            val width = referenceThumbnail.width
            val height = referenceThumbnail.height
            if (thumbnail.width != width || thumbnail.height != height ||
                width < 28 || height < 28
            ) return 0 to 0

            val referencePixels = IntArray(width * height)
            val currentPixels = IntArray(width * height)
            referenceThumbnail.getPixels(referencePixels, 0, width, 0, 0, width, height)
            thumbnail.getPixels(currentPixels, 0, width, 0, 0, width, height)

            var bestDx = 0
            var bestDy = 0
            var bestScore = Double.POSITIVE_INFINITY
            var secondBestScore = Double.POSITIVE_INFINITY
            val border = ALIGNMENT_MAX_SHIFT + 2
            val step = if (min(width, height) > 96) 3 else 2

            for (dy in -ALIGNMENT_MAX_SHIFT..ALIGNMENT_MAX_SHIFT) {
                for (dx in -ALIGNMENT_MAX_SHIFT..ALIGNMENT_MAX_SHIFT) {
                    var totalError = 0L
                    var samples = 0
                    for (y in border until height - border step step) {
                        for (x in border until width - border - 1 step step) {
                            val refIndex = y * width + x
                            val currentIndex = (y + dy) * width + x + dx
                            val refRight = luma(referencePixels[refIndex + 1])
                            val refDown = luma(referencePixels[refIndex + width])
                            val curRight = luma(currentPixels[currentIndex + 1])
                            val curDown = luma(currentPixels[currentIndex + width])
                            val refGradient = kotlin.math.abs(
                                luma(referencePixels[refIndex]) - refRight
                            ) + kotlin.math.abs(luma(referencePixels[refIndex]) - refDown)
                            val currentGradient = kotlin.math.abs(
                                luma(currentPixels[currentIndex]) - curRight
                            ) + kotlin.math.abs(luma(currentPixels[currentIndex]) - curDown)
                            totalError += kotlin.math.abs(refGradient - currentGradient).toLong()
                            samples++
                        }
                    }
                    if (samples == 0) continue
                    val score = totalError.toDouble() / samples
                    if (score < bestScore) {
                        secondBestScore = bestScore
                        bestScore = score
                        bestDx = dx
                        bestDy = dy
                    } else if (score < secondBestScore) {
                        secondBestScore = score
                    }
                }
            }

            // On a textureless scene, every offset looks alike. Avoid applying a random shift.
            if (!bestScore.isFinite() || bestScore < 0.0 ||
                secondBestScore - bestScore < 0.15
            ) return 0 to 0

            val scaleX = currentFrame.width.toFloat() / width
            val scaleY = currentFrame.height.toFloat() / height
            return (bestDx * scaleX).toInt() to (bestDy * scaleY).toInt()
        } finally {
            thumbnail.recycle()
        }
    }

    private fun luma(color: Int): Int =
        luma(Color.red(color), Color.green(color), Color.blue(color))

    fun saveStarTrails(
        context: Context,
        frames: List<File>,
        onProgress: ((Int) -> Unit)? = null
    ): Uri? {
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
        val merged = mergeFramesIntoUri(context, frames, uri, MergeMode.STAR_TRAILS, onProgress)
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

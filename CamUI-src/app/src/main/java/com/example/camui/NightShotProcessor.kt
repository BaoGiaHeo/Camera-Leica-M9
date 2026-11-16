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
import kotlin.math.pow
import kotlin.math.roundToInt
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
    private const val MAX_OUTPUT_PIXELS = 60_000_000L
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

    enum class MergeMode { NIGHT_AVERAGE, MILKY_WAY_STACK, STAR_TRAILS, HDR_BRACKET, FOCUS_STACK }

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
        val prefix = when (mode) {
            MergeMode.STAR_TRAILS -> "star-"
            MergeMode.MILKY_WAY_STACK -> "milky-way-"
            MergeMode.HDR_BRACKET -> "hdr-"
            MergeMode.FOCUS_STACK -> "focus-"
            else -> "night-"
        }
        val tempOutput = File(context.cacheDir, "fuji-" + prefix + System.nanoTime() + ".jpg")
        var base: Bitmap? = null
        var alignmentReference: Bitmap? = null
        return try {
            val firstExif = readExif(frames.first())
            val stackPixelLimit = if (mode == MergeMode.HDR_BRACKET || mode == MergeMode.FOCUS_STACK) {
                16_000_000L
            } else {
                MAX_OUTPUT_PIXELS
            }
            base = decodeUpright(frames.first(), null, null, stackPixelLimit)
                ?: error("Could not decode first frame")
            val width = base!!.width
            val height = base!!.height
            val oldRow = IntArray(width)
            val newRow = IntArray(width)
            val upperRow = IntArray(width)
            val lowerRow = IntArray(width)
            // Focus stacking keeps the best local sharpness score. HDR bracketing instead
            // keeps a running per-pixel exposure weight and blends all useful exposures;
            // selecting only one "mid-tone winner" can create abrupt, noisy transitions.
            val bestScores = if (mode == MergeMode.FOCUS_STACK) ByteArray(width * height) else null
            val exposureWeights = if (mode == MergeMode.HDR_BRACKET) ShortArray(width * height) else null
            // Keep a small, immutable reference image for alignment. Using the changing
            // accumulated output as a reference would allow drift to build up over a burst.
            if (mode != MergeMode.STAR_TRAILS) {
                alignmentReference = makeAlignmentThumbnail(base!!)
            }
            if (bestScores != null || exposureWeights != null) {
                for (y in 0 until height) {
                    base!!.getPixels(newRow, 0, width, 0, y, width, 1)
                    if (bestScores != null) {
                        base!!.getPixels(upperRow, 0, width, 0, (y - 1).coerceAtLeast(0), width, 1)
                        base!!.getPixels(lowerRow, 0, width, 0, (y + 1).coerceAtMost(height - 1), width, 1)
                    }
                    for (x in 0 until width) {
                        val index = y * width + x
                        if (bestScores != null) {
                            bestScores[index] = focusSharpnessScore(newRow, upperRow, lowerRow, x, width).toByte()
                        }
                        if (exposureWeights != null) {
                            exposureWeights[index] = exposureMidtoneScore(luma(newRow[x]))
                                .coerceAtLeast(1).toShort()
                        }
                    }
                }
            }

            val totalMergedFrames = (frames.size - 1).coerceAtLeast(1)
            for (frameIndex in 1 until frames.size) {
                val current = decodeUpright(frames[frameIndex], width, height, stackPixelLimit)
                    ?: error("Could not decode frame " + (frameIndex + 1))
                try {
                    val shift = if (mode != MergeMode.STAR_TRAILS) {
                        estimateGlobalTranslation(alignmentReference!!, current)
                    } else {
                        0 to 0
                    }
                    val shiftX = shift.first.coerceIn(-width / 12, width / 12)
                    val shiftY = shift.second.coerceIn(-height / 12, height / 12)
                    if (mode != MergeMode.STAR_TRAILS && (shiftX != 0 || shiftY != 0)) {
                        Log.d(TAG, "Aligned frame ${frameIndex + 1} for $mode: dx=$shiftX dy=$shiftY")
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
                        if (mode == MergeMode.FOCUS_STACK) {
                            current.getPixels(upperRow, 0, width, 0, (sourceY - 1).coerceAtLeast(0), width, 1)
                            current.getPixels(lowerRow, 0, width, 0, (sourceY + 1).coerceAtMost(height - 1), width, 1)
                        }
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
                            } else if (mode == MergeMode.NIGHT_AVERAGE || mode == MergeMode.MILKY_WAY_STACK) {
                                val divisor = frameIndex + 1
                                oldRow[x] = Color.rgb(
                                    (ar * frameIndex + br) / divisor,
                                    (ag * frameIndex + bg) / divisor,
                                    (ab * frameIndex + bb) / divisor
                                )
                            } else if (mode == MergeMode.HDR_BRACKET) {
                                val scoreIndex = y * width + x
                                val currentWeight = exposureMidtoneScore(luma(b)).coerceAtLeast(1)
                                val previousWeight = exposureWeights!![scoreIndex].toInt().coerceAtLeast(1)
                                val combinedWeight = previousWeight + currentWeight
                                // Exposure-weighted fusion preserves information from more than
                                // one bracket frame instead of hard-switching each pixel to a
                                // single frame. The weight sum fits in Short for a 3-frame bracket.
                                oldRow[x] = Color.rgb(
                                    (ar * previousWeight + br * currentWeight) / combinedWeight,
                                    (ag * previousWeight + bg * currentWeight) / combinedWeight,
                                    (ab * previousWeight + bb * currentWeight) / combinedWeight
                                )
                                exposureWeights[scoreIndex] = combinedWeight.toShort()
                            } else {
                                val sourceXLeft = (sourceX - 1).coerceAtLeast(0)
                                val sourceXRight = (sourceX + 1).coerceAtMost(width - 1)
                                val left = luma(newRow[sourceXLeft])
                                val right = luma(newRow[sourceXRight])
                                val up = luma(upperRow[sourceX])
                                val down = luma(lowerRow[sourceX])
                                val score = (kotlin.math.abs(left - right) + kotlin.math.abs(up - down))
                                    .coerceIn(0, 255)
                                val scoreIndex = y * width + x
                                val previousScore = bestScores!![scoreIndex].toInt() and 0xFF
                                if (score > previousScore) {
                                    oldRow[x] = b
                                    bestScores[scoreIndex] = score.toByte()
                                }
                            }
                        }
                        base!!.setPixels(oldRow, 0, width, 0, y, width, 1)
                    }
                } finally {
                    current.recycle()
                }
            }

            if (mode == MergeMode.NIGHT_AVERAGE) {
                // Frame averaging reduces random noise but does not recover brightness by itself.
                // Milky Way stacking deliberately skips this night-scene lift to preserve the sky background.
                // Lift dark mid-tones after alignment/averaging, with a smooth gamma curve that
                // preserves highlights better than multiplying every channel by a fixed gain.
                brightenNightResult(base!!)
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

    private fun exposureMidtoneScore(luminance: Int): Int =
        (255 - kotlin.math.abs(luminance - 128) * 2).coerceIn(0, 255)

    private fun focusSharpnessScore(
        centerRow: IntArray,
        upperRow: IntArray,
        lowerRow: IntArray,
        x: Int,
        width: Int
    ): Int {
        val left = luma(centerRow[(x - 1).coerceAtLeast(0)])
        val right = luma(centerRow[(x + 1).coerceAtMost(width - 1)])
        val up = luma(upperRow[x])
        val down = luma(lowerRow[x])
        return (kotlin.math.abs(left - right) + kotlin.math.abs(up - down)).coerceIn(0, 255)
    }

    private fun brightenNightResult(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return

        // Estimate scene brightness from a sparse luminance histogram to keep this pass cheap.
        val histogram = IntArray(256)
        val sampleRow = IntArray(width)
        var samples = 0
        val step = 8
        var y = 0
        while (y < height) {
            bitmap.getPixels(sampleRow, 0, width, 0, y, width, 1)
            var x = 0
            while (x < width) {
                val color = sampleRow[x]
                histogram[luma(color).coerceIn(0, 255)]++
                samples++
                x += step
            }
            y += step
        }
        if (samples == 0) return
        val medianThreshold = (samples + 1) / 2
        var cumulative = 0
        var medianLuma = 0
        for (value in histogram.indices) {
            cumulative += histogram[value]
            if (cumulative >= medianThreshold) {
                medianLuma = value
                break
            }
        }

        // Only lift scenes that are actually dark. A target median around 76/255 makes
        // dim environments visibly readable without forcing already bright scenes brighter.
        if (medianLuma >= 82) return
        val safeMedian = medianLuma.coerceAtLeast(1).toDouble() / 255.0
        val targetMedian = 76.0 / 255.0
        val gamma = (kotlin.math.ln(targetMedian) / kotlin.math.ln(safeMedian))
            .coerceIn(0.34, 0.92)
        val lookup = IntArray(256) { value ->
            (255.0 * (value / 255.0).pow(gamma)).roundToInt().coerceIn(0, 255)
        }

        val row = IntArray(width)
        val progressInterval = (height / 8).coerceAtLeast(1)
        for (rowIndex in 0 until height) {
            bitmap.getPixels(row, 0, width, 0, rowIndex, width, 1)
            for (x in 0 until width) {
                val color = row[x]
                val red = Color.red(color)
                val green = Color.green(color)
                val blue = Color.blue(color)
                val oldLuma = luma(red, green, blue)
                if (oldLuma > 0) {
                    val newLuma = lookup[oldLuma]
                    val gain = (newLuma.toFloat() / oldLuma.toFloat()).coerceAtMost(8f)
                    row[x] = Color.rgb(
                        (red * gain).roundToInt().coerceIn(0, 255),
                        (green * gain).roundToInt().coerceIn(0, 255),
                        (blue * gain).roundToInt().coerceIn(0, 255)
                    )
                }
            }
            bitmap.setPixels(row, 0, width, 0, rowIndex, width, 1)
            if (rowIndex % progressInterval == 0) {
                Log.d(TAG, "Adaptive night tone map row $rowIndex/$height; median=$medianLuma gamma=$gamma")
            }
        }
        Log.i(TAG, "Adaptive night brightness applied: median=$medianLuma gamma=$gamma")
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

            // Compute each thumbnail's gradient once. The search below evaluates hundreds of
            // candidate shifts; recalculating RGB luminance inside every candidate multiplied
            // the same Color conversions by the number of tested offsets.
            val referenceGradients = buildGradientMap(referencePixels, width, height)
            val currentGradients = buildGradientMap(currentPixels, width, height)

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
                            totalError += kotlin.math.abs(
                                referenceGradients[refIndex] - currentGradients[currentIndex]
                            ).toLong()
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

    /** Precompute the same two-axis luma-gradient score used by the translation matcher. */
    private fun buildGradientMap(pixels: IntArray, width: Int, height: Int): IntArray {
        val luminance = IntArray(pixels.size) { index -> luma(pixels[index]) }
        val gradients = IntArray(pixels.size)
        for (y in 0 until height) {
            val row = y * width
            val downRow = min(height - 1, y + 1) * width
            for (x in 0 until width) {
                val index = row + x
                val rightIndex = row + min(width - 1, x + 1)
                gradients[index] =
                    kotlin.math.abs(luminance[index] - luminance[rightIndex]) +
                        kotlin.math.abs(luminance[index] - luminance[downRow + x])
            }
        }
        return gradients
    }

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

    /**
     * Creates an HDR exposure-fusion or focus-stack JPEG while retaining every source frame.
     * The merge is bounded to 16MP and memory-aware; if it fails, source frames remain saved.
     */
    fun mergeBracketUrisIntoNewImage(
        context: Context,
        frameUris: List<Uri>,
        mode: MergeMode,
        onProgress: ((Int) -> Unit)? = null
    ): Uri? {
        if (mode != MergeMode.HDR_BRACKET && mode != MergeMode.FOCUS_STACK) return null
        if (frameUris.size < 2) return null
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val prefix = if (mode == MergeMode.HDR_BRACKET) "FUJI_HDR_MERGED_" else "FUJI_FOCUS_STACK_"
        val folder = if (mode == MergeMode.HDR_BRACKET) "HDR-Bracketing" else "Focus-Bracketing"
        val outputUri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, prefix + stamp + ".jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FujifilmMode/" + folder)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        ) ?: return null
        val tempDirectory = File(context.cacheDir, "fuji-bracket-" + System.nanoTime())
        val tempFrames = frameUris.indices.map { index ->
            File(tempDirectory, "frame-" + index.toString().padStart(2, '0') + ".jpg")
        }
        return try {
            tempDirectory.mkdirs()
            val copied = frameUris.indices.all { index ->
                copyUriToFile(context, frameUris[index], tempFrames[index])
            }
            if (!copied) error("Could not stage every source bracket frame")
            val merged = mergeFramesIntoUri(context, tempFrames, outputUri, mode, onProgress)
            if (!merged) error("Bracket merge did not finish")
            val published = context.contentResolver.update(
                outputUri,
                ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                    put(MediaStore.Images.Media.ORIENTATION, 0)
                },
                null,
                null
            )
            if (published <= 0) error("Could not publish merged bracket image")
            outputUri
        } catch (failure: Throwable) {
            Log.e(TAG, "Bracket merge failed; source frames remain saved", failure)
            runCatching { context.contentResolver.delete(outputUri, null, null) }
            null
        } finally {
            tempFrames.forEach { runCatching { it.delete() } }
            runCatching { tempDirectory.delete() }
        }
    }

    private fun decodeUpright(
        file: File,
        targetWidth: Int?,
        targetHeight: Int?,
        pixelLimit: Long = MAX_OUTPUT_PIXELS
    ): Bitmap? {
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
            // Two RGB_565 full-resolution frames are live during merging. Reserve headroom
            // for decode/orientation buffers; 60MP is a ceiling, not a guaranteed output size.
            val heapSafePixelCap = (
                Runtime.getRuntime().maxMemory().toDouble() * 0.22 / 4.0
            ).toLong().coerceAtLeast(4_000_000L)
            val effectivePixelCap = min(pixelLimit, heapSafePixelCap)
            val factor = if (pixels > effectivePixelCap) {
                sqrt(effectivePixelCap.toDouble() / pixels.toDouble())
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

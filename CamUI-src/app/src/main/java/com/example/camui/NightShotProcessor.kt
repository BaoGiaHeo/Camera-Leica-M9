package com.example.camui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.SystemClock
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
    private const val MAX_OUTPUT_PIXELS = 108_000_000L
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

    enum class MergeMode { NIGHT_AVERAGE, LUNA_DREAM_1_5, LUNA_DREAM_2_0, MILKY_WAY_STACK, STAR_TRAILS, HDR_BRACKET, FOCUS_STACK }

    private data class GrayLevel(
        val width: Int,
        val height: Int,
        val pixels: IntArray
    )

    private data class FlowTrack(val dx: Float, val dy: Float)

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
        val rawCompanion = File(
            destination.parentFile,
            destination.nameWithoutExtension + "-discard.dng"
        )
        fun clearTemporaryFiles() {
            runCatching { destination.delete() }
            runCatching { rawCompanion.delete() }
        }
        runCatching {
            destination.parentFile?.mkdirs()
            clearTemporaryFiles()
            if (capture.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW_JPEG) {
                // RawDream keeps the first full DNG as the original RAW master. Additional
                // burst requests still need a JPEG for LunaDream fusion; their temporary DNGs
                // are discarded after CameraX confirms both output callbacks.
                val rawOptions = ImageCapture.OutputFileOptions.Builder(rawCompanion).build()
                val jpegOptions = ImageCapture.OutputFileOptions.Builder(destination).build()
                var rawSaved = false
                var jpegSaved = false
                fun finishIfReady() {
                    if (rawSaved && jpegSaved && continuation.isActive) {
                        val success = destination.exists() && destination.length() > 0L
                        runCatching { rawCompanion.delete() }
                        continuation.resume(success)
                    }
                }
                capture.takePicture(
                    rawOptions,
                    jpegOptions,
                    callbackExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            if (outputFileResults.imageFormat == ImageFormat.RAW_SENSOR) {
                                rawSaved = rawCompanion.exists() && rawCompanion.length() > 0L
                            } else {
                                jpegSaved = destination.exists() && destination.length() > 0L
                            }
                            finishIfReady()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "RAW+JPEG burst frame capture failed", exception)
                            clearTemporaryFiles()
                            if (continuation.isActive) continuation.resume(false)
                        }
                    }
                )
            } else {
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
                            clearTemporaryFiles()
                            if (continuation.isActive) continuation.resume(false)
                        }
                    }
                )
            }
        }.onFailure {
            Log.e(TAG, "Could not start frame capture", it)
            clearTemporaryFiles()
            if (continuation.isActive) continuation.resume(false)
        }
        continuation.invokeOnCancellation { clearTemporaryFiles() }
    }


    /**
     * Single-frame LunaDream 1.5 restoration. It applies the same restrained luminance
     * detail recovery to one camera JPEG and never captures or fuses additional frames.
     * The original URI is backed up before replacement and restored on write failure.
     */
    fun processLunaDreamSingleFrame(context: Context, uri: Uri): Boolean =
        processLunaDreamSingleFrameInternal(context, uri, lunaDream20 = false)

    /**
     * One-shot LunaDream 2.0 detail restoration for RawDream 2.0.
     * RawDream's capture session is RAW+JPEG, so this mode processes its JPEG partner
     * directly instead of issuing extra JPEG-only captures through the RAW+JPEG use case.
     */
    fun processLunaDream20SingleFrame(context: Context, uri: Uri): Boolean =
        processLunaDreamSingleFrameInternal(context, uri, lunaDream20 = true)

    private fun processLunaDreamSingleFrameInternal(
        context: Context,
        uri: Uri,
        lunaDream20: Boolean
    ): Boolean {
        val stamp = System.nanoTime()
        val modeLabel = if (lunaDream20) "LunaDream 2.0" else "LunaDream 1.5"
        val sourceFile = File(context.cacheDir, "luna-single-source-$stamp.jpg")
        val processedFile = File(context.cacheDir, "luna-single-processed-$stamp.jpg")
        val backupFile = File(context.cacheDir, "luna-single-original-$stamp.jpg")
        var bitmap: Bitmap? = null
        var replacing = false
        var stage = "copy source"
        return try {
            check(copyUriToFile(context, uri, sourceFile)) { "Cannot read $modeLabel source" }
            stage = "read metadata and decode"
            val exif = readExif(sourceFile)
            bitmap = decodeUpright(sourceFile, null, null, 50_000_000L)
                ?: error("Cannot decode $modeLabel source within the safe memory budget")
            stage = "single-frame detail recovery"
            if (lunaDream20) applyLunaDream20DetailRecovery(bitmap!!)
            else applyLunaDreamDetailRecovery(bitmap!!)
            stage = "encode processed JPEG"
            FileOutputStream(processedFile).use { output ->
                check(bitmap!!.compress(Bitmap.CompressFormat.JPEG, 97, output)) {
                    "$modeLabel JPEG encoding failed"
                }
                output.flush()
                output.fd.sync()
            }
            check(processedFile.exists() && processedFile.length() > 0L) {
                "$modeLabel produced an empty output file"
            }
            writeExif(processedFile, exif)
            stage = "backup original"
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            } ?: error("Cannot back up original $modeLabel photo")
            check(backupFile.length() > 0L) { "Original backup is empty" }
            stage = "publish processed JPEG"
            replacing = true
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                processedFile.inputStream().use { it.copyTo(output) }
                output.flush()
            } ?: error("Cannot write $modeLabel photo")
            stage = "verify published JPEG"
            val verification = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val verificationReadable = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, verification)
                true
            } ?: false
            check(verificationReadable) { "Cannot reopen processed $modeLabel photo" }
            check(verification.outWidth >= 3 && verification.outHeight >= 3) {
                "Published $modeLabel image is not a decodable JPEG"
            }
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.ORIENTATION, 0) },
                null,
                null
            )
            Log.i(TAG, "$modeLabel processed and verified one frame at ${verification.outWidth}x${verification.outHeight}")
            true
        } catch (failure: Throwable) {
            if (replacing && backupFile.exists()) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        backupFile.inputStream().use { it.copyTo(output) }
                        output.flush()
                    } ?: error("Rollback stream unavailable")
                }.onFailure { Log.e(TAG, "$modeLabel rollback failed", it) }
            }
            Log.e(TAG, "$modeLabel failed during $stage; original retained", failure)
            false
        } finally {
            bitmap?.let { if (!it.isRecycled) it.recycle() }
            listOf(sourceFile, processedFile, backupFile).forEach { runCatching { it.delete() } }
        }
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
            MergeMode.LUNA_DREAM_1_5 -> "luna15-"
            MergeMode.LUNA_DREAM_2_0 -> "luna-dream-"
            MergeMode.MILKY_WAY_STACK -> "milky-way-"
            MergeMode.HDR_BRACKET -> "hdr-"
            MergeMode.FOCUS_STACK -> "focus-"
            else -> "night-"
        }
        val tempOutput = File(context.cacheDir, "fuji-" + prefix + System.nanoTime() + ".jpg")
        val mergeStartedAt = SystemClock.elapsedRealtime()
        var decodeMs = 0L
        var alignmentMs = 0L
        var fusionMs = 0L
        var detailRecoveryMs = 0L
        var encodeMs = 0L
        var publishMs = 0L
        var base: Bitmap? = null
        var alignmentReference: Bitmap? = null
        return try {
            val firstExif = readExif(frames.first())
            val stackPixelLimit = when (mode) {
                MergeMode.HDR_BRACKET, MergeMode.FOCUS_STACK -> 16_000_000L
                MergeMode.NIGHT_AVERAGE, MergeMode.MILKY_WAY_STACK -> 60_000_000L
                else -> MAX_OUTPUT_PIXELS
            }
            val firstDecodeStartedAt = SystemClock.elapsedRealtime()
            base = decodeUpright(frames.first(), null, null, stackPixelLimit)
                ?: error("Could not decode first frame")
            decodeMs += SystemClock.elapsedRealtime() - firstDecodeStartedAt
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
                val frameDecodeStartedAt = SystemClock.elapsedRealtime()
                val current = decodeUpright(frames[frameIndex], width, height, stackPixelLimit)
                    ?: error("Could not decode frame " + (frameIndex + 1))
                decodeMs += SystemClock.elapsedRealtime() - frameDecodeStartedAt
                try {
                    val alignmentStartedAt = SystemClock.elapsedRealtime()
                    val shift = when (mode) {
                        MergeMode.STAR_TRAILS -> 0 to 0
                        MergeMode.LUNA_DREAM_2_0 ->
                            estimatePyramidalLucasKanade(alignmentReference!!, current)
                        MergeMode.LUNA_DREAM_1_5 ->
                            estimateGlobalTranslation(alignmentReference!!, current)
                        else -> estimateGlobalTranslation(alignmentReference!!, current)
                    }
                    alignmentMs += SystemClock.elapsedRealtime() - alignmentStartedAt
                    val shiftX = shift.first.coerceIn(-width / 12, width / 12)
                    val shiftY = shift.second.coerceIn(-height / 12, height / 12)
                    if (mode != MergeMode.STAR_TRAILS && (shiftX != 0 || shiftY != 0)) {
                        Log.d(TAG, "Aligned frame ${frameIndex + 1} for $mode: dx=$shiftX dy=$shiftY")
                    }

                    val fusionStartedAt = SystemClock.elapsedRealtime()
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
                            // Large multi-frame stacks visit many millions of pixels.
                            // Bit shifts preserve the same channel values without Color.* calls.
                            val ar = (a ushr 16) and 0xFF
                            val ag = (a ushr 8) and 0xFF
                            val ab = a and 0xFF
                            val br = (b ushr 16) and 0xFF
                            val bg = (b ushr 8) and 0xFF
                            val bb = b and 0xFF
                            if (mode == MergeMode.STAR_TRAILS) {
                                // Max/lighten accumulation draws the path of stars, while a small
                                // luminance threshold prevents tiny sensor noise from winning every
                                // pixel simply because more frames were captured.
                                val oldLuma = luma(ar, ag, ab)
                                val newLuma = luma(br, bg, bb)
                                if (newLuma >= STAR_TRAIL_MIN_LUMA &&
                                    newLuma >= oldLuma + STAR_TRAIL_MIN_DELTA
                                ) {
                                    oldRow[x] = packRgb(max(ar, br), max(ag, bg), max(ab, bb))
                                }
                            } else if (mode == MergeMode.LUNA_DREAM_1_5 ||
                                mode == MergeMode.LUNA_DREAM_2_0
                            ) {
                                // Both LunaDream modes use motion-aware temporal fusion. 1.5
                                // uses five exposures and global translation; 2.0 keeps its
                                // existing eight-frame Lucas-Kanade path.
                                val oldLuma = luma(ar, ag, ab)
                                val newLuma = luma(br, bg, bb)
                                val lumaDelta = kotlin.math.abs(oldLuma - newLuma)
                                val colorDelta = max(
                                    kotlin.math.abs(ar - br),
                                    max(kotlin.math.abs(ag - bg), kotlin.math.abs(ab - bb))
                                )
                                val threshold = (16 + (oldLuma + newLuma) / 28).coerceIn(20, 38)
                                when {
                                    lumaDelta <= threshold && colorDelta <= threshold * 2 -> {
                                        val divisor = frameIndex + 1
                                        oldRow[x] = packRgb(
                                            (ar * frameIndex + br) / divisor,
                                            (ag * frameIndex + bg) / divisor,
                                            (ab * frameIndex + bb) / divisor
                                        )
                                    }
                                    lumaDelta <= threshold * 2 && colorDelta <= threshold * 3 -> {
                                        // Transitional motion gets only a small contribution.
                                        oldRow[x] = packRgb(
                                            (ar * 7 + br) / 8,
                                            (ag * 7 + bg) / 8,
                                            (ab * 7 + bb) / 8
                                        )
                                    }
                                    // Large changes are treated as moving-object/ghost pixels.
                                    else -> Unit
                                }
                            } else if (mode == MergeMode.NIGHT_AVERAGE || mode == MergeMode.MILKY_WAY_STACK) {
                                val divisor = frameIndex + 1
                                oldRow[x] = packRgb(
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
                                oldRow[x] = packRgb(
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
                    fusionMs += SystemClock.elapsedRealtime() - fusionStartedAt
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
            } else if (mode == MergeMode.LUNA_DREAM_1_5) {
                val detailStartedAt = SystemClock.elapsedRealtime()
                applyLunaDreamNoiseReduction(base!!, strength = 1)
                applyLunaDreamDetailRecovery(base!!)
                detailRecoveryMs = SystemClock.elapsedRealtime() - detailStartedAt
            } else if (mode == MergeMode.LUNA_DREAM_2_0) {
                // Keep the 2.0 eight-frame alignment/fusion strategy; add a restrained
                // edge-aware spatial denoise pass before its existing detail restoration.
                val detailStartedAt = SystemClock.elapsedRealtime()
                applyLunaDreamNoiseReduction(base!!, strength = 2)
                applyLunaDream20DetailRecovery(base!!)
                detailRecoveryMs = SystemClock.elapsedRealtime() - detailStartedAt
            }

            runCatching { onProgress?.invoke(96) }
            val encodeStartedAt = SystemClock.elapsedRealtime()
            FileOutputStream(tempOutput).use { output ->
                check(base!!.compress(Bitmap.CompressFormat.JPEG, 96, output)) {
                    "Could not encode merged image"
                }
                output.flush()
                output.fd.sync()
            }
            writeExif(tempOutput, firstExif)
            encodeMs = SystemClock.elapsedRealtime() - encodeStartedAt
            val publishStartedAt = SystemClock.elapsedRealtime()
            context.contentResolver.openOutputStream(destinationUri, "wt")?.use { output ->
                tempOutput.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not write merged image to MediaStore")
            context.contentResolver.update(
                destinationUri,
                ContentValues().apply { put(MediaStore.Images.Media.ORIENTATION, 0) },
                null,
                null
            )
            publishMs = SystemClock.elapsedRealtime() - publishStartedAt
            runCatching { onProgress?.invoke(100) }
            Log.i(
                TAG,
                "Merged " + frames.size + " frames as " + mode + " at " + width + "x" + height +
                    "; decode=" + decodeMs + "ms align=" + alignmentMs +
                    "ms fusion=" + fusionMs + "ms detail=" + detailRecoveryMs + "ms encode=" + encodeMs +
                    "ms publish=" + publishMs + "ms total=" +
                    (SystemClock.elapsedRealtime() - mergeStartedAt) + "ms"
            )
            true
        } catch (failure: Throwable) {
            Log.e(
                TAG,
                "Multi-frame merge failed for $mode after " +
                    (SystemClock.elapsedRealtime() - mergeStartedAt) +
                    "ms; decode=$decodeMs align=$alignmentMs fusion=$fusionMs " +
                    "detail=$detailRecoveryMs encode=$encodeMs publish=$publishMs; " +
                    "original capture remains available",
                failure
            )
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
    private const val LUNA_LK_MAX_FEATURES = 120
    private const val LUNA_LK_WINDOW_RADIUS = 3
    private const val LUNA_LK_ITERATIONS = 7

    /**
     * Sparse pyramidal Lucas-Kanade on a small luminance thumbnail. The robust consensus
     * estimates the dominant camera translation while rejecting independently moving objects.
     * Full-resolution pixels are only touched during the final frame fusion.
     */
    private fun estimatePyramidalLucasKanade(
        referenceThumbnail: Bitmap,
        currentFrame: Bitmap
    ): Pair<Int, Int> {
        val currentThumbnail = makeAlignmentThumbnail(currentFrame)
        try {
            val width = referenceThumbnail.width
            val height = referenceThumbnail.height
            if (currentThumbnail.width != width || currentThumbnail.height != height ||
                width < 48 || height < 48
            ) return 0 to 0

            val referencePyramid = buildGrayPyramid(referenceThumbnail)
            val currentPyramid = buildGrayPyramid(currentThumbnail)
            if (referencePyramid.size < 2 || currentPyramid.size != referencePyramid.size) {
                return estimateGlobalTranslation(referenceThumbnail, currentFrame)
            }

            val features = detectShiTomasiFeatures(referencePyramid.first())
            val tracks = ArrayList<FlowTrack>(features.size)
            for ((x, y) in features) {
                trackPyramidalLucasKanade(referencePyramid, currentPyramid, x, y)?.let {
                    tracks.add(FlowTrack(it.first, it.second))
                }
            }

            val translation = robustTranslationConsensus(tracks)
                ?: return estimateGlobalTranslation(referenceThumbnail, currentFrame)

            val scaleX = currentFrame.width.toFloat() / width
            val scaleY = currentFrame.height.toFloat() / height
            return (translation.first * scaleX).roundToInt() to
                (translation.second * scaleY).roundToInt()
        } finally {
            currentThumbnail.recycle()
        }
    }

    private fun buildGrayPyramid(bitmap: Bitmap): List<GrayLevel> {
        val colors = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(colors, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var width = bitmap.width
        var height = bitmap.height
        var gray = IntArray(colors.size) { index -> luma(colors[index]) }
        val pyramid = ArrayList<GrayLevel>(3)
        pyramid.add(GrayLevel(width, height, gray))

        while (pyramid.size < 3 && min(width, height) >= 48) {
            val nextWidth = width / 2
            val nextHeight = height / 2
            val next = IntArray(nextWidth * nextHeight)
            for (y in 0 until nextHeight) {
                val sourceRow = y * 2 * width
                val targetRow = y * nextWidth
                for (x in 0 until nextWidth) {
                    val source = sourceRow + x * 2
                    next[targetRow + x] = (
                        gray[source] + gray[source + 1] +
                            gray[source + width] + gray[source + width + 1]
                        ) / 4
                }
            }
            width = nextWidth
            height = nextHeight
            gray = next
            pyramid.add(GrayLevel(width, height, gray))
        }
        return pyramid
    }

    private fun detectShiTomasiFeatures(level: GrayLevel): List<Pair<Int, Int>> {
        data class Candidate(val x: Int, val y: Int, val score: Float)

        val width = level.width
        val height = level.height
        val pixels = level.pixels
        val candidates = ArrayList<Candidate>()
        val radius = 2
        for (y in radius + 1 until height - radius - 1 step 2) {
            for (x in radius + 1 until width - radius - 1 step 2) {
                var xx = 0f
                var xy = 0f
                var yy = 0f
                for (dy in -radius..radius) {
                    val row = (y + dy) * width
                    for (dx in -radius..radius) {
                        val index = row + x + dx
                        val gx = (pixels[index + 1] - pixels[index - 1]) * 0.5f
                        val gy = (pixels[index + width] - pixels[index - width]) * 0.5f
                        xx += gx * gx
                        xy += gx * gy
                        yy += gy * gy
                    }
                }
                val trace = xx + yy
                val discriminant = kotlin.math.sqrt(
                    ((xx - yy) * (xx - yy) + 4f * xy * xy).coerceAtLeast(0f)
                )
                val score = (trace - discriminant) * 0.5f
                if (score > 40f) candidates.add(Candidate(x, y, score))
            }
        }

        candidates.sortByDescending { it.score }
        val selected = ArrayList<Pair<Int, Int>>(LUNA_LK_MAX_FEATURES)
        val minDistanceSquared = 36
        for (candidate in candidates) {
            if (selected.none { (x, y) ->
                    val dx = x - candidate.x
                    val dy = y - candidate.y
                    dx * dx + dy * dy < minDistanceSquared
                }
            ) {
                selected.add(candidate.x to candidate.y)
                if (selected.size >= LUNA_LK_MAX_FEATURES) break
            }
        }
        return selected
    }

    private fun trackPyramidalLucasKanade(
        reference: List<GrayLevel>,
        current: List<GrayLevel>,
        originalX: Int,
        originalY: Int
    ): Pair<Float, Float>? {
        val maxLevel = min(reference.size, current.size) - 1
        var flowX = 0f
        var flowY = 0f
        val radius = LUNA_LK_WINDOW_RADIUS

        for (level in maxLevel downTo 0) {
            if (level < maxLevel) {
                flowX *= 2f
                flowY *= 2f
            }
            val scale = (1 shl level).toFloat()
            val referenceLevel = reference[level]
            val currentLevel = current[level]
            val pointX = originalX / scale
            val pointY = originalY / scale
            if (pointX < radius + 2 || pointY < radius + 2 ||
                pointX >= referenceLevel.width - radius - 2 ||
                pointY >= referenceLevel.height - radius - 2
            ) return null

            var meanError = Float.MAX_VALUE
            for (iteration in 0 until LUNA_LK_ITERATIONS) {
                var a11 = 0f
                var a12 = 0f
                var a22 = 0f
                var b1 = 0f
                var b2 = 0f
                var errorSum = 0f
                var samples = 0

                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val rx = pointX + dx
                        val ry = pointY + dy
                        val template = sampleGray(referenceLevel, rx, ry)
                        val gradientX = (
                            sampleGray(referenceLevel, rx + 1f, ry) -
                                sampleGray(referenceLevel, rx - 1f, ry)
                            ) * 0.5f
                        val gradientY = (
                            sampleGray(referenceLevel, rx, ry + 1f) -
                                sampleGray(referenceLevel, rx, ry - 1f)
                            ) * 0.5f
                        val warped = sampleGrayBilinear(
                            currentLevel,
                            rx + flowX,
                            ry + flowY
                        )
                        val residual = warped - template
                        a11 += gradientX * gradientX
                        a12 += gradientX * gradientY
                        a22 += gradientY * gradientY
                        b1 += gradientX * residual
                        b2 += gradientY * residual
                        errorSum += kotlin.math.abs(residual)
                        samples++
                    }
                }

                val determinant = a11 * a22 - a12 * a12
                if (!determinant.isFinite() || determinant < 1e-3f) return null
                val deltaX = (-a22 * b1 + a12 * b2) / determinant
                val deltaY = (a12 * b1 - a11 * b2) / determinant
                if (!deltaX.isFinite() || !deltaY.isFinite()) return null
                flowX += deltaX
                flowY += deltaY
                meanError = errorSum / samples.coerceAtLeast(1)
                if (kotlin.math.abs(deltaX) + kotlin.math.abs(deltaY) < 0.02f) break
                if (kotlin.math.abs(flowX) > 8f || kotlin.math.abs(flowY) > 8f) return null
            }
            if (meanError > 48f) return null
        }

        return if (flowX.isFinite() && flowY.isFinite()) flowX to flowY else null
    }

    private fun sampleGray(level: GrayLevel, x: Float, y: Float): Float {
        val px = x.roundToInt().coerceIn(0, level.width - 1)
        val py = y.roundToInt().coerceIn(0, level.height - 1)
        return level.pixels[py * level.width + px].toFloat()
    }

    private fun sampleGrayBilinear(level: GrayLevel, x: Float, y: Float): Float {
        if (x < 0f || y < 0f || x >= level.width - 1 || y >= level.height - 1) {
            return 0f
        }
        val x0 = x.toInt()
        val y0 = y.toInt()
        val xWeight = x - x0
        val yWeight = y - y0
        val index = y0 * level.width + x0
        val top = level.pixels[index] * (1f - xWeight) +
            level.pixels[index + 1] * xWeight
        val bottom = level.pixels[index + level.width] * (1f - xWeight) +
            level.pixels[index + level.width + 1] * xWeight
        return top * (1f - yWeight) + bottom * yWeight
    }

    /** RANSAC-style translation consensus; independently moving features are discarded. */
    private fun robustTranslationConsensus(tracks: List<FlowTrack>): Pair<Float, Float>? {
        if (tracks.size < 8) return null
        var bestInliers = emptyList<FlowTrack>()
        val threshold = 1.8f
        for (candidate in tracks) {
            val inliers = tracks.filter {
                kotlin.math.abs(it.dx - candidate.dx) <= threshold &&
                    kotlin.math.abs(it.dy - candidate.dy) <= threshold
            }
            if (inliers.size > bestInliers.size) bestInliers = inliers
        }
        if (bestInliers.size < max(8, tracks.size / 5)) return null

        fun median(values: List<Float>): Float {
            val sorted = values.sorted()
            val middle = sorted.size / 2
            return if (sorted.size % 2 == 0) {
                (sorted[middle - 1] + sorted[middle]) * 0.5f
            } else sorted[middle]
        }

        return median(bestInliers.map { it.dx }) to median(bestInliers.map { it.dy })
    }

    /**
     * Edge-aware residual noise reduction after temporal fusion. It smooths small,
     * low-contrast sensor/JPEG fluctuations while rejecting neighbours across strong
     * luminance or colour boundaries, so texture and hair edges are less likely to smear.
     * The pass is row-streamed to avoid another full-resolution bitmap allocation.
     */
    private fun applyLunaDreamNoiseReduction(bitmap: Bitmap, strength: Int) {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return

        var previousRow = IntArray(width)
        var currentRow = IntArray(width)
        var nextRow = IntArray(width)
        val outputRow = IntArray(width)
        bitmap.getPixels(currentRow, 0, width, 0, 0, width, 1)
        currentRow.copyInto(previousRow)
        bitmap.getPixels(nextRow, 0, width, 0, 1, width, 1)

        val baseBlend = if (strength >= 2) 0.30f else 0.23f
        for (y in 0 until height) {
            for (x in 0 until width) {
                val center = currentRow[x]
                val centerR = (center ushr 16) and 0xFF
                val centerG = (center ushr 8) and 0xFF
                val centerB = center and 0xFF
                val centerLuma = luma(centerR, centerG, centerB)
                val rangeLimit = if (centerLuma < 72) {
                    if (strength >= 2) 27 else 23
                } else {
                    if (strength >= 2) 20 else 17
                }

                var sumR = 0
                var sumG = 0
                var sumB = 0
                var weightSum = 0
                for (dy in -1..1) {
                    val row = when (dy) {
                        -1 -> previousRow
                        0 -> currentRow
                        else -> nextRow
                    }
                    val ny = (y + dy).coerceIn(0, height - 1)
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val neighbor = row[nx]
                        val nr = (neighbor ushr 16) and 0xFF
                        val ng = (neighbor ushr 8) and 0xFF
                        val nb = neighbor and 0xFF
                        val luminanceDelta = kotlin.math.abs(luma(nr, ng, nb) - centerLuma)
                        val colorDelta = max(
                            kotlin.math.abs(nr - centerR),
                            max(kotlin.math.abs(ng - centerG), kotlin.math.abs(nb - centerB))
                        )
                        val spatialWeight = if (dx == 0 || dy == 0) 2 else 1
                        val weight = when {
                            luminanceDelta <= rangeLimit && colorDelta <= rangeLimit * 2 ->
                                spatialWeight
                            luminanceDelta <= rangeLimit * 2 && colorDelta <= rangeLimit * 3 -> 1
                            else -> 0
                        }
                        if (weight > 0) {
                            sumR += nr * weight
                            sumG += ng * weight
                            sumB += nb * weight
                            weightSum += weight
                        }
                    }
                }

                if (weightSum == 0) {
                    outputRow[x] = center
                } else {
                    val shadowBoost = (1.22f - centerLuma / 255f * 0.42f).coerceIn(0.80f, 1.22f)
                    val blend = (baseBlend * shadowBoost).coerceIn(0.12f, 0.38f)
                    val averageR = sumR.toFloat() / weightSum
                    val averageG = sumG.toFloat() / weightSum
                    val averageB = sumB.toFloat() / weightSum
                    outputRow[x] = packRgb(
                        (centerR * (1f - blend) + averageR * blend).roundToInt().coerceIn(0, 255),
                        (centerG * (1f - blend) + averageG * blend).roundToInt().coerceIn(0, 255),
                        (centerB * (1f - blend) + averageB * blend).roundToInt().coerceIn(0, 255)
                    )
                }
            }
            bitmap.setPixels(outputRow, 0, width, 0, y, width, 1)
            if (y + 1 < height) {
                val recycled = previousRow
                previousRow = currentRow
                currentRow = nextRow
                nextRow = recycled
                if (y + 2 < height) bitmap.getPixels(nextRow, 0, width, 0, y + 2, width, 1)
                else currentRow.copyInto(nextRow)
            }
        }
    }

    /**
     * Small luminance-only high-pass restores edge acutance after multi-frame denoising.
     * The correction is capped to avoid halos and does not independently sharpen RGB channels.
     */
    private fun applyLunaDreamDetailRecovery(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return

        var previousRow = IntArray(width)
        var currentRow = IntArray(width)
        var nextRow = IntArray(width)
        val outputRow = IntArray(width)
        bitmap.getPixels(currentRow, 0, width, 0, 0, width, 1)
        currentRow.copyInto(previousRow)
        bitmap.getPixels(nextRow, 0, width, 0, 1, width, 1)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val center = currentRow[x]
                val centerLuma = luma(center)
                val leftLuma = luma(currentRow[(x - 1).coerceAtLeast(0)])
                val rightLuma = luma(currentRow[(x + 1).coerceAtMost(width - 1)])
                val upLuma = luma(previousRow[x])
                val downLuma = luma(nextRow[x])
                val localAverage = (leftLuma + rightLuma + upLuma + downLuma) * 0.25f
                // Limit edge boost and avoid dark/bright overshoot that can create black halos.
                val edgeContrast = kotlin.math.abs(centerLuma - localAverage)
                val detail = if (centerLuma.toFloat() in 18f..238f && edgeContrast in 2f..42f) {
                    ((centerLuma - localAverage) * 0.08f).coerceIn(-3f, 3f)
                } else 0f
                val targetLuma = (centerLuma + detail).coerceIn(0f, 255f)
                val gain = if (centerLuma > 1f) targetLuma / centerLuma else 1f
                val red = (center ushr 16) and 0xFF
                val green = (center ushr 8) and 0xFF
                val blue = center and 0xFF
                outputRow[x] = packRgb(
                    (red * gain).roundToInt().coerceIn(0, 255),
                    (green * gain).roundToInt().coerceIn(0, 255),
                    (blue * gain).roundToInt().coerceIn(0, 255)
                )
            }
            bitmap.setPixels(outputRow, 0, width, 0, y, width, 1)
            if (y + 1 < height) {
                val recycled = previousRow
                previousRow = currentRow
                currentRow = nextRow
                nextRow = recycled
                if (y + 2 < height) {
                    bitmap.getPixels(nextRow, 0, width, 0, y + 2, width, 1)
                } else {
                    currentRow.copyInto(nextRow)
                }
            }
        }
    }

    /**
     * LunaDream 2.0 detail pass.
     * It increases local acutance and fine texture contrast on captured pixels only:
     * no synthetic texture is generated. A bounded local luminance range avoids dark
     * outlines and bright halos around high-contrast edges.
     */
    private fun applyLunaDream20DetailRecovery(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return

        var previousRow = IntArray(width)
        var currentRow = IntArray(width)
        var nextRow = IntArray(width)
        val outputRow = IntArray(width)
        bitmap.getPixels(currentRow, 0, width, 0, 0, width, 1)
        currentRow.copyInto(previousRow)
        bitmap.getPixels(nextRow, 0, width, 0, 1, width, 1)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val center = currentRow[x]
                val centerLuma = luma(center).toFloat()
                val leftLuma = luma(currentRow[(x - 1).coerceAtLeast(0)]).toFloat()
                val rightLuma = luma(currentRow[(x + 1).coerceAtMost(width - 1)]).toFloat()
                val upLuma = luma(previousRow[x]).toFloat()
                val downLuma = luma(nextRow[x]).toFloat()
                val crossMean = (leftLuma + rightLuma + upLuma + downLuma) * 0.25f

                val upLeft = luma(previousRow[(x - 1).coerceAtLeast(0)]).toFloat()
                val upRight = luma(previousRow[(x + 1).coerceAtMost(width - 1)]).toFloat()
                val downLeft = luma(nextRow[(x - 1).coerceAtLeast(0)]).toFloat()
                val downRight = luma(nextRow[(x + 1).coerceAtMost(width - 1)]).toFloat()
                val diagonalMean = (upLeft + upRight + downLeft + downRight) * 0.25f

                val edgeDelta = centerLuma - crossMean
                val fineDelta = centerLuma - diagonalMean
                val edgeMagnitude = kotlin.math.abs(edgeDelta)
                val correction = if (centerLuma in 18f..238f && edgeMagnitude >= 2.5f && edgeMagnitude <= 58f) {
                    edgeDelta * 0.24f +
                        if (kotlin.math.abs(fineDelta) >= 3.5f) fineDelta * 0.10f else 0f
                } else 0f

                val localMin = minOf(centerLuma, leftLuma, rightLuma, upLuma, downLuma, upLeft, upRight, downLeft, downRight)
                val localMax = maxOf(centerLuma, leftLuma, rightLuma, upLuma, downLuma, upLeft, upRight, downLeft, downRight)
                val correctionLimit = minOf(7f, (edgeMagnitude * 0.48f).coerceAtLeast(2f))
                val clampedCorrection = correction.coerceIn(-correctionLimit, correctionLimit)
                val targetLuma = (centerLuma + clampedCorrection)
                    .coerceIn((localMin - 3f).coerceAtLeast(0f), (localMax + 3f).coerceAtMost(255f))
                val gain = if (centerLuma > 1f) targetLuma / centerLuma else 1f
                val red = (center ushr 16) and 0xFF
                val green = (center ushr 8) and 0xFF
                val blue = center and 0xFF
                outputRow[x] = packRgb(
                    (red * gain).roundToInt().coerceIn(0, 255),
                    (green * gain).roundToInt().coerceIn(0, 255),
                    (blue * gain).roundToInt().coerceIn(0, 255)
                )
            }
            bitmap.setPixels(outputRow, 0, width, 0, y, width, 1)
            if (y + 1 < height) {
                val recycled = previousRow
                previousRow = currentRow
                currentRow = nextRow
                nextRow = recycled
                if (y + 2 < height) {
                    bitmap.getPixels(nextRow, 0, width, 0, y + 2, width, 1)
                } else {
                    currentRow.copyInto(nextRow)
                }
            }
        }
    }

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
        luma((color ushr 16) and 0xFF, (color ushr 8) and 0xFF, color and 0xFF)

    /** Equivalent to Color.rgb for clamped channels. */
    private fun packRgb(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or ((red and 0xFF) shl 16) or ((green and 0xFF) shl 8) or (blue and 0xFF)

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
        pixelLimit: Long = MAX_OUTPUT_PIXELS,
        heapBytesPerPixel: Double = 4.0
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
                Runtime.getRuntime().maxMemory().toDouble() * 0.42 / heapBytesPerPixel.coerceAtLeast(2.0)
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

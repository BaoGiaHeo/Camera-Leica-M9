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
import android.os.SystemClock
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
/**
 * Returns the largest output target that can fit in the currently available RGB_565
 * heap budget. The 10% margin protects encoder and MediaStore allocations.
 */
internal fun adaptivePhotoOutputTargetPixels(
    requestedPixels: Long,
    usableHeapBytes: Long,
    minimumPixels: Long
): Long? {
    if (requestedPixels <= 0L || usableHeapBytes <= 0L || minimumPixels <= 0L) return null
    val safeBytes = (usableHeapBytes.toDouble() * 0.90).toLong()
    val safePixels = (safeBytes / 2L).coerceAtMost(requestedPixels)
    return safePixels.takeIf { it >= minimumPixels }
}

object PhotoOutputResolution {
    private const val TAG = "FUJI_LUNADREAM_RESAMPLE"
    private const val MIN_OUTPUT_PIXELS = 4L
    private const val RESERVED_HEAP_BYTES = 64L * 1024L * 1024L

    /**
     * Center-crop the upright JPEG to the selected frame ratio while preserving the
     * source pixel density. The crop must not be upscaled back to the uncropped pixel
     * count: that wastes heap and can cause high-resolution captures to silently keep
     * their original 4:3 frame.
     */
    fun applyAspectRatio(
        context: Context,
        uri: Uri,
        targetHeightOverWidth: Float?
    ): PhotoOutputResult? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
            bounds.outWidth > 0 && bounds.outHeight > 0
        } ?: false
        if (!decoded) return null
        val sourcePixels = bounds.outWidth.toLong() * bounds.outHeight.toLong()
        if (sourcePixels !in MIN_OUTPUT_PIXELS..250_000_000L) return null
        val crop = PhotoOutputGeometry.calculate(
            bounds.outWidth,
            bounds.outHeight,
            sourcePixels,
            targetHeightOverWidth
        )
        val nativeCropPixels = crop.sourceCropPixels
        if (nativeCropPixels !in MIN_OUTPUT_PIXELS..sourcePixels) return null
        Log.i(
            TAG,
            "Aspect crop budget: source=${bounds.outWidth}x${bounds.outHeight} " +
                "crop=${crop.sourceWidth}x${crop.sourceHeight} " +
                "nativeCropPixels=$nativeCropPixels"
        )
        return interpolateToMp(context, uri, nativeCropPixels, targetHeightOverWidth)
    }

    fun interpolateToMp(
        context: Context,
        uri: Uri,
        targetPixels: Long,
        targetHeightOverWidth: Float? = null,
        adaptiveMinimumPixels: Long? = null
    ): PhotoOutputResult? {
        require(targetPixels in MIN_OUTPUT_PIXELS..250_000_000L) {
            "Unsupported output pixel target"
        }
        if (adaptiveMinimumPixels != null) {
            require(adaptiveMinimumPixels == Tuning.LUNA_DREAM_30_MIN_OUTPUT_PIXELS) {
                "LunaDream 3.0 tile output must use its strict 200MP acceptance target"
            }
            return interpolateToMpByTiles(context, uri, targetPixels, targetHeightOverWidth)
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val temp = File(context.cacheDir, "fuji-lunadream-$stamp.jpg")
        val backup = File(context.cacheDir, "fuji-lunadream-original-$stamp.jpg")
        var source: Bitmap? = null
        var output: Bitmap? = null
        var replacing = false
        val stageWatchdog = if (adaptiveMinimumPixels != null) {
            LunaDreamStageWatchdog(context, "LunaDream 3.0 upscale")
        } else null
        return try {
            stageWatchdog?.beginStage("đọc EXIF và giải mã ảnh nguồn", Tuning.LUNA_DREAM_30_DECODE_BUDGET_MS)
            val tags = readExif(context, uri)
            source = decodeExifOrientedBitmap(context, uri)
                ?: error("Cannot decode original JPEG")
            stageWatchdog?.checkpoint()

            val src = source!!
            val sourceWidth = src.width
            val sourceHeight = src.height
            require(sourceWidth > 0 && sourceHeight > 0) { "Invalid source size" }

            // Measure remaining heap after the source RGB_565 bitmap has been decoded.
            // LunaDream 3.0 may reduce only its output pixel target when the desired size
            // cannot fit; every other mode keeps the strict exact-target behavior.
            val runtime = Runtime.getRuntime()
            val usedHeap = (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)
            val freeHeapHeadroom = (runtime.maxMemory() - usedHeap).coerceAtLeast(0L)
            val usableHeap = (freeHeapHeadroom - RESERVED_HEAP_BYTES).coerceAtLeast(0L)
            val effectiveTargetPixels = if (adaptiveMinimumPixels != null) {
                adaptivePhotoOutputTargetPixels(targetPixels, usableHeap, adaptiveMinimumPixels)
                    ?: error(
                        "Not enough heap for LunaDream 3.0 minimum output " +
                            "${adaptiveMinimumPixels / 1_000_000L}MP; original photo retained"
                    )
            } else {
                targetPixels
            }
            if (effectiveTargetPixels < targetPixels) {
                Log.w(
                    TAG,
                    "Adaptive memory output: requested=${targetPixels / 1_000_000L}MP " +
                        "safeTarget=${effectiveTargetPixels / 1_000_000L}MP " +
                        "usableHeapBytes=$usableHeap"
                )
            }

            // Geometry is independently unit-tested. Portrait uses a center crop from
            // the original Bitmap; null target ratio preserves the complete landscape frame.
            val geometry = PhotoOutputGeometry.calculate(
                sourceWidth,
                sourceHeight,
                effectiveTargetPixels,
                targetHeightOverWidth,
                dimensionAlignment = if (effectiveTargetPixels >= 200_000_000L) 16 else 1
            )
            val sourceRect = Rect(
                geometry.sourceLeft,
                geometry.sourceTop,
                geometry.sourceLeft + geometry.sourceWidth,
                geometry.sourceTop + geometry.sourceHeight
            )
            val destinationRect = Rect(0, 0, geometry.outputWidth, geometry.outputHeight)

            val width = geometry.outputWidth
            val height = geometry.outputHeight
            val outputPixels = geometry.outputPixels
            val outputBytes = outputPixels * 2L // RGB_565.
            require(outputPixels >= effectiveTargetPixels * 995L / 1000L) {
                "Selected MP geometry was not reached; original photo retained"
            }
            require(outputBytes <= usableHeap) {
                "Not enough free heap for the requested output; original photo retained"
            }

            if (sourceWidth == width && sourceHeight == height &&
                sourceRect.left == 0 && sourceRect.top == 0 &&
                sourceRect.width() == sourceWidth && sourceRect.height() == sourceHeight
            ) {
                stageWatchdog?.beginStage("xác minh JPEG đầu ra", Tuning.LUNA_DREAM_30_VERIFY_BUDGET_MS)
                verifyStoredOutput(context, uri, width, height)
                stageWatchdog?.finish()
                return PhotoOutputResult(width, height, targetPixels, outputPixels)
            }

            stageWatchdog?.beginStage("upscale và dựng ảnh đầu ra", Tuning.LUNA_DREAM_30_DETAIL_BUDGET_MS)
            output = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            Canvas(output!!).drawBitmap(
                src,
                sourceRect,
                destinationRect,
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
                    isAntiAlias = true
                }
            )
            // Release the source before JPEG compression; the resized output is now complete.
            if (!src.isRecycled) src.recycle()
            source = null
            stageWatchdog?.checkpoint()
            stageWatchdog?.beginStage("mã hóa JPEG", Tuning.LUNA_DREAM_30_ENCODE_BUDGET_MS)
// JPEG_INTERMEDIATE_ALLOWLIST: resolution-conversion staging/backup/rollback JPEG; final capture is written by ImageSink.
            FileOutputStream(temp).use { stream ->
// JPEG_INTERMEDIATE_ALLOWLIST: resolution-conversion staging/backup/rollback JPEG; final capture is written by ImageSink.
                check(output!!.compress(Bitmap.CompressFormat.JPEG, 98, stream)) {
                    "JPEG encoding failed"
                }
                stream.flush()
                stream.fd.sync()
            }
            check(temp.length() > 0L)
            writeExif(temp, tags)
            stageWatchdog?.checkpoint()
            stageWatchdog?.beginStage("ghi JPEG và tạo bản khôi phục", Tuning.LUNA_DREAM_30_PUBLISH_BUDGET_MS)

            context.contentResolver.openInputStream(uri)?.use { input ->
// JPEG_INTERMEDIATE_ALLOWLIST: resolution-conversion staging/backup/rollback JPEG; final capture is written by ImageSink.
                FileOutputStream(backup).use { stream ->
                    input.copyTo(stream)
                    stream.fd.sync()
                }
            } ?: error("Cannot back up source image")

            replacing = true
// JPEG_INTERMEDIATE_ALLOWLIST: resolution-conversion staging/backup/rollback JPEG; final capture is written by ImageSink.
            val copied = context.contentResolver.openOutputStream(uri, "wt")?.use { dest ->
                temp.inputStream().use { it.copyTo(dest) }
                true
            } ?: false
            check(copied) { "MediaStore replacement failed" }
            stageWatchdog?.checkpoint()

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
            if (uri.authority != context.packageName + ".fileprovider") {
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
            }

            stageWatchdog?.beginStage("xác minh JPEG đã ghi", Tuning.LUNA_DREAM_30_VERIFY_BUDGET_MS)
            verifyStoredOutput(context, uri, width, height)
            val actualAspect = width.toDouble() / height.toDouble()
            Log.i(
                TAG,
                "Requested=${targetPixels / 1_000_000L}MP actual=${outputPixels / 1_000_000L}MP " +
                    "source=${sourceWidth}x${sourceHeight}, crop=${geometry.sourceWidth}x${geometry.sourceHeight} " +
                    "at=${geometry.sourceLeft},${geometry.sourceTop}, " +
                    "requestedWidthOverHeight=${"%.5f".format(Locale.US, geometry.requestedWidthOverHeight)}, " +
                    "dimensions=${width}x${height} aspect=${"%.5f".format(Locale.US, actualAspect)}"
            )
            check(outputPixels >= effectiveTargetPixels * 995L / 1000L) {
                "Output verification fell below the safe memory-adaptive megapixel target"
            }
            Log.i(TAG, "Exact requested output accepted: requested=${targetPixels} actual=${outputPixels}")
            stageWatchdog?.finish()
            PhotoOutputResult(width, height, targetPixels, outputPixels)
        } catch (failure: Throwable) {
            if (replacing && backup.exists()) {
                runCatching {
// JPEG_INTERMEDIATE_ALLOWLIST: resolution-conversion staging/backup/rollback JPEG; final capture is written by ImageSink.
                    context.contentResolver.openOutputStream(uri, "wt")?.use { dest ->
                        backup.inputStream().use { it.copyTo(dest) }
                    } ?: error("Rollback failed")
                }.onFailure {
                    Log.e(TAG, "Original restore failed", it)
                }
            }
            Log.e(TAG, "LunaDream output sizing skipped; original retained", failure)
            if (stageWatchdog != null) {
                val stage = stageWatchdog.currentStage
                val reason = failure.message?.takeIf { it.isNotBlank() } ?: failure.javaClass.simpleName
                val message = "LunaDream 3.0 dừng ở bước $stage: $reason"
                CaptureDiagnostics.recordWarning(context, message)
                CrashContextDiagnostics.recordUiEvent("LUNADREAM_30_STAGE_FAILED stage=$stage reason=$reason")
                Log.e(TAG, message, failure)
            }
            null
        } finally {
            source?.let { if (!it.isRecycled) it.recycle() }
            output?.let { if (!it.isRecycled) it.recycle() }
            runCatching { temp.delete() }
            runCatching { backup.delete() }
        }
    }

    /**
     * LunaDream 3.0 path: decode only source scanlines, resample a bounded 64-row stripe,
     * and stream those stripes into one JPEG through libjpeg-turbo. No source-sized or
     * destination-sized Bitmap is allocated on the Kotlin heap.
     */
    private fun interpolateToMpByTiles(
        context: Context,
        uri: Uri,
        targetPixels: Long,
        targetHeightOverWidth: Float?
    ): PhotoOutputResult? {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val sourceBackup = File(context.cacheDir, "fuji-lunadream-30-source-$stamp.jpg")
        val encodedTemp = File(context.cacheDir, "fuji-lunadream-30-tile-$stamp.jpg")
        val watchdog = LunaDreamStageWatchdog(context, "LunaDream 3.0 tile upscale")
        var replacing = false
        return try {
            watchdog.beginStage("đọc EXIF và sao lưu JPEG nguồn", Tuning.LUNA_DREAM_30_EXIF_BUDGET_MS)
            val tags = readExif(context, uri)
            val orientation = context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                ExifInterface(descriptor.fileDescriptor).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: error("Không đọc được EXIF Orientation của ảnh nguồn.")
            check(orientation == ExifInterface.ORIENTATION_NORMAL) {
                "Ảnh nguồn chưa chuẩn hóa hướng (EXIF=$orientation); không được nâng theo tile sai chiều."
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(sourceBackup).use { backup ->
                    input.copyTo(backup)
                    backup.flush()
                    backup.fd.sync()
                }
            } ?: error("Không mở được JPEG nguồn để sao lưu.")
            check(sourceBackup.length() > 0L) { "JPEG nguồn rỗng; không thể chạy tile encoder." }

            watchdog.beginStage("đọc kích thước và tính hình học tile", Tuning.LUNA_DREAM_30_DECODE_BUDGET_MS)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            sourceBackup.inputStream().use { input -> BitmapFactory.decodeStream(input, null, bounds) }
            val sourceWidth = bounds.outWidth
            val sourceHeight = bounds.outHeight
            check(sourceWidth >= 2 && sourceHeight >= 2) {
                "Không đọc được kích thước JPEG nguồn."
            }
            check(sourceWidth.toLong() * sourceHeight.toLong() <= 250_000_000L) {
                "Ảnh nguồn vượt giới hạn pixel an toàn của JPEG decoder."
            }
            val geometry = PhotoOutputGeometry.calculate(
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                targetPixels = targetPixels,
                targetHeightOverWidth = targetHeightOverWidth,
                dimensionAlignment = 16
            )
            val width = geometry.outputWidth
            val height = geometry.outputHeight
            val outputPixels = geometry.outputPixels
            check(outputPixels >= Tuning.LUNA_DREAM_30_MIN_OUTPUT_PIXELS * 995L / 1000L) {
                "Kích thước hình học thấp hơn nghiệm thu 200MP: ${width}x$height."
            }
            check(width <= 65_000 && height <= 65_000) {
                "Kích thước đầu ra vượt giới hạn JPEG."
            }
            watchdog.checkpoint()

            watchdog.beginStage("nâng ảnh và mã hóa JPEG theo tile 64 hàng", Tuning.LUNA_DREAM_30_ENCODE_BUDGET_MS)
            val deadlineMs = SystemClock.elapsedRealtime() +
                Tuning.LUNA_DREAM_30_ENCODE_BUDGET_MS * Tuning.LUNA_DREAM_30_STAGE_WATCHDOG_FACTOR
            val nativeFailure = JpegTileEncoder.encode(
                sourcePath = sourceBackup.absolutePath,
                outputPath = encodedTemp.absolutePath,
                targetWidth = width,
                targetHeight = height,
                cropLeft = geometry.sourceLeft,
                cropTop = geometry.sourceTop,
                cropWidth = geometry.sourceWidth,
                cropHeight = geometry.sourceHeight,
                quality = 98,
                deadlineElapsedRealtimeMs = deadlineMs
            )
            check(nativeFailure == null) {
                nativeFailure ?: "Native JPEG tile encoder không hoàn tất."
            }
            check(encodedTemp.isFile && encodedTemp.length() > 0L) {
                "Native JPEG tile encoder không tạo được file."
            }
            watchdog.checkpoint()

            watchdog.beginStage("ghi JPEG đã mã hóa và xác minh", Tuning.LUNA_DREAM_30_PUBLISH_BUDGET_MS)
            writeExif(encodedTemp, tags)
            replacing = true
            val written = context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                encodedTemp.inputStream().use { it.copyTo(destination) }
                destination.flush()
                true
            } ?: false
            check(written) { "MediaStore không nhận được JPEG đã nâng theo tile." }

            if (uri.authority != context.packageName + ".fileprovider") {
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
            }
            watchdog.beginStage("xác minh JPEG 200MP đã lưu", Tuning.LUNA_DREAM_30_VERIFY_BUDGET_MS)
            verifyStoredOutput(context, uri, width, height)
            watchdog.finish()
            Log.i(
                TAG,
                "LUNADREAM_30_TILE_SUCCESS source=${sourceWidth}x$sourceHeight " +
                    "crop=${geometry.sourceWidth}x${geometry.sourceHeight} " +
                    "output=${width}x$height pixels=$outputPixels tileRows=64 " +
                    "bytes=${encodedTemp.length()}"
            )
            CrashContextDiagnostics.recordUiEvent(
                "LUNADREAM_30_TILE_SUCCESS output=${width}x$height pixels=$outputPixels tileRows=64"
            )
            PhotoOutputResult(width, height, targetPixels, outputPixels)
        } catch (failure: Throwable) {
            val stage = watchdog.currentStage
            val reason = failure.message?.takeIf { it.isNotBlank() } ?: failure.javaClass.simpleName
            val message = "LunaDream 3.0 dừng ở bước $stage: $reason"
            if (replacing && sourceBackup.isFile && sourceBackup.length() > 0L) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                        sourceBackup.inputStream().use { it.copyTo(destination) }
                        destination.flush()
                    } ?: error("Không mở được MediaStore để khôi phục ảnh gốc.")
                }.onFailure { rollbackFailure ->
                    Log.e(TAG, "LunaDream 3.0 rollback failed: ${rollbackFailure.message}", rollbackFailure)
                    CaptureDiagnostics.recordWarning(
                        context,
                        "LunaDream 3.0 không khôi phục được ảnh gốc: ${rollbackFailure.message ?: rollbackFailure.javaClass.simpleName}"
                    )
                }
            }
            CaptureDiagnostics.recordWarning(context, message)
            CrashContextDiagnostics.recordUiEvent("LUNADREAM_30_TILE_FAILED stage=$stage reason=$reason")
            LunaDreamDiagnostics.record(context, "TILE_UPSCALE_FAILED", "stage=$stage; reason=$reason", failure)
            Log.e(TAG, message, failure)
            throw IllegalStateException(message, failure)
        } finally {
            runCatching { sourceBackup.delete() }
            runCatching { encodedTemp.delete() }
        }
    }

    /**
     * Reopen the final MediaStore item and verify the data that other apps will read.
     * The in-memory output dimensions alone are not sufficient because stream replacement,
     * EXIF writes or MediaStore metadata can fail independently.
     */
    private fun verifyStoredOutput(
        context: Context,
        uri: Uri,
        expectedWidth: Int,
        expectedHeight: Int
    ) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val readable = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
            bounds.outWidth > 0 && bounds.outHeight > 0
        } ?: false
        check(readable) { "Could not read the saved image dimensions from MediaStore" }
        check(bounds.outWidth == expectedWidth && bounds.outHeight == expectedHeight) {
            "Saved dimensions mismatch: expected ${expectedWidth}x${expectedHeight}, got ${bounds.outWidth}x${bounds.outHeight}"
        }
        val orientation = context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            ExifInterface(descriptor.fileDescriptor).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: error("Could not reopen saved EXIF metadata")
        check(orientation == ExifInterface.ORIENTATION_NORMAL) {
            "Saved EXIF orientation is not normalized: $orientation"
        }
        val actualAspect = bounds.outWidth.toDouble() / bounds.outHeight.toDouble()
        val expectedAspect = expectedWidth.toDouble() / expectedHeight.toDouble()
        val tolerance = maxOf(0.0005, 2.0 / minOf(expectedWidth, expectedHeight).toDouble())
        check(kotlin.math.abs(actualAspect - expectedAspect) <= tolerance) {
            "Saved aspect ratio mismatch: expected $expectedAspect, got $actualAspect"
        }
        Log.i(
            TAG,
            "Stored output verified: ${bounds.outWidth}x${bounds.outHeight}, " +
                "EXIF orientation=$orientation, aspect=${"%.5f".format(Locale.US, actualAspect)}"
        )
    }

    /**
     * BitmapFactory returns the stored pixel matrix and does not apply JPEG EXIF orientation.
     * Normalize it before measuring/fitting, then write ORIENTATION_NORMAL on the output.
     * Without this step portrait captures can be sized as landscape (and vice versa).
     */
    private fun decodeExifOrientedBitmap(context: Context, uri: Uri): Bitmap? {
        val orientation = runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                ExifInterface(descriptor.fileDescriptor).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(
                input,
                null,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                    inDither = true
                }
            )
        } ?: return null

        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return decoded
        }

        val oriented = Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height, matrix, true
        )
        if (oriented !== decoded && !decoded.isRecycled) decoded.recycle()
        return oriented
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
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_IMAGE_DESCRIPTION,
            ExifInterface.TAG_USER_COMMENT,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_GPS_DATESTAMP
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

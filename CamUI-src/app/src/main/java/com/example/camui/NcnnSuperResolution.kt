package com.example.camui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

/**
 * Real-ESRGAN NCNN x4plus photo adapter targeting approximately 211 MP.
 *
 * The native layer receives RGB_565 bitmaps and runs the general-photo x4plus model on overlapped tiles.
 * Input is normalized to about 13.19 MP so the x4 output targets approximately 211 MP.
 * AI reconstruction estimates detail; the final pixel count is not optical sensor detail.
 */
object NcnnSuperResolution {
    private const val TAG = "FUJI_NCNN_SR"
    private const val MODEL_PARAM = "realesrgan-x4plus.param"
    private const val MODEL_BIN = "realesrgan-x4plus.bin"
    private const val TARGET_OUTPUT_PIXELS = 211_000_000L
    private const val MIN_OUTPUT_PIXELS = 16_000_000L
    private const val TILE_SIZE = 256
    @Volatile private var lastOutputPixels: Long = 0L

    fun lastOutputMegapixels(): Long = lastOutputPixels / 1_000_000L

    init {
        System.loadLibrary("fujisr")
    }

    private external fun nativeUpscaleInto(
        sourceBitmap: Bitmap,
        outputBitmap: Bitmap,
        modelParamPath: String,
        modelBinPath: String,
        tileSize: Int
    ): Boolean

    /**
     * Called on Dispatchers.IO after orientation, film look, frame and watermark are complete.
     * The original MediaStore JPEG is left untouched unless the entire native pass and JPEG
     * encoding succeeds.
     */
    fun enhanceCapturedPhoto(
        context: Context,
        uri: Uri,
        requestedOutputPixels: Long = TARGET_OUTPUT_PIXELS
    ): Boolean {
        val uniqueName = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val tempOutput = File(context.cacheDir, "fuji-ai-$uniqueName.jpg")
        val originalBackup = File(context.cacheDir, "fuji-original-$uniqueName.jpg")
        var sourceBitmap: Bitmap? = null
        var modelInput: Bitmap? = null
        var outputBitmap: Bitmap? = null
        var replacementStarted = false

        return try {
            val paramFile = copyModelAsset(context, MODEL_PARAM)
            val binFile = copyModelAsset(context, MODEL_BIN)
            val originalExif = readImportantExif(context, uri)

            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
                inDither = true
            }
            sourceBitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: error("Cannot decode captured JPEG")
            require(sourceBitmap!!.width > 0 && sourceBitmap!!.height > 0) {
                "Invalid source image dimensions"
            }

            val src = sourceBitmap!!
            val originalWidth = src.width
            val originalHeight = src.height
            val inputPixels = originalWidth.toLong() * originalHeight.toLong()
            // A fixed 211MP RGB_565 output needs about 422MB in one bitmap. Reserve memory
            // for NCNN and the camera UI, then scale the x4 model input to the current heap.
            val runtime = Runtime.getRuntime()
            val usedHeapBytes = runtime.totalMemory() - runtime.freeMemory()
            val freeHeapBytes = (runtime.maxMemory() - usedHeapBytes).coerceAtLeast(0L)
            val safeOutputBytes = minOf(
                (runtime.maxMemory().toDouble() * 0.64).toLong(),
                (freeHeapBytes.toDouble() * 0.70).toLong()
            )
            val safeOutputPixels = (safeOutputBytes / 2L / 1_000_000L) * 1_000_000L
            val requestedTarget = requestedOutputPixels.coerceIn(MIN_OUTPUT_PIXELS, TARGET_OUTPUT_PIXELS)
            val effectiveTargetPixels = minOf(requestedTarget, safeOutputPixels)
            require(effectiveTargetPixels >= MIN_OUTPUT_PIXELS) {
                "Not enough free heap for minimum safe AI output"
            }
            val modelInputTargetPixels = effectiveTargetPixels / 16L
            val scale = sqrt(modelInputTargetPixels.toDouble() / inputPixels.toDouble())
            var scaledWidth = (originalWidth * scale).toInt().coerceAtLeast(1)
            var scaledHeight = (originalHeight * scale).toInt().coerceAtLeast(1)
            while (scaledWidth.toLong() * scaledHeight.toLong() > modelInputTargetPixels) {
                if (scaledWidth >= scaledHeight) scaledWidth-- else scaledHeight--
            }
            modelInput = if (scaledWidth != originalWidth || scaledHeight != originalHeight) {
                Bitmap.createScaledBitmap(src, scaledWidth, scaledHeight, true).also {
                    if (it !== src) src.recycle()
                    Log.i(TAG, "Normalized AI input: ${originalWidth}x${originalHeight} -> ${it.width}x${it.height}")
                }
            } else {
                src
            }
            sourceBitmap = null

            val outputWidth = modelInput!!.width * 4
            val outputHeight = modelInput!!.height * 4
            val outputPixels = outputWidth.toLong() * outputHeight.toLong()
            require(outputPixels <= effectiveTargetPixels) { "Rounded NCNN output exceeds heap target" }
            val outputBytes = outputPixels * 2L
            require(outputBytes <= safeOutputBytes) { "NCNN output exceeds free heap budget" }
            lastOutputPixels = outputPixels
            Log.i(TAG, "AI target ${requestedTarget / 1_000_000L}MP, actual ${outputPixels / 1_000_000L}MP, free heap ${freeHeapBytes / (1024L * 1024L)}MB")
            outputBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.RGB_565)

            val inferenceOk = nativeUpscaleInto(
                modelInput!!,
                outputBitmap!!,
                paramFile.absolutePath,
                binFile.absolutePath,
                TILE_SIZE
            )
            if (!inferenceOk) error("NCNN inference failed")
            modelInput!!.recycle()
            modelInput = null

            FileOutputStream(tempOutput).use { output ->
                check(outputBitmap!!.compress(Bitmap.CompressFormat.JPEG, 96, output)) {
                    "Could not encode AI output JPEG"
                }
                output.flush()
                output.fd.sync()
            }
            outputBitmap!!.recycle()
            outputBitmap = null
            check(tempOutput.length() > 0L) { "AI output file is empty" }

            // Reapply the important camera EXIF tags after Bitmap JPEG encoding.
            writeImportantExif(tempOutput, originalExif)

            // Keep a byte-for-byte rollback copy until MediaStore publication completes.
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(originalBackup).use { backup ->
                    input.copyTo(backup)
                    backup.fd.sync()
                }
            } ?: error("Cannot create rollback copy of the original camera JPEG")

            replacementStarted = true
            val copied = context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                tempOutput.inputStream().use { input -> input.copyTo(output) }
                true
            } ?: false
            check(copied) { "Could not replace the MediaStore JPEG with AI output" }

            context.contentResolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
                ExifInterface(descriptor.fileDescriptor).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                    saveAttributes()
                }
            }
            context.contentResolver.update(
                uri,
                android.content.ContentValues().apply {
                    put(MediaStore.Images.Media.ORIENTATION, 0)
                },
                null,
                null
            )
            Log.i(TAG, "Real-ESRGAN x4plus saved: ${outputWidth}x${outputHeight} (${outputPixels / 1_000_000L} MP)")
            true
        } catch (failure: Throwable) {
            Log.e(TAG, "Real-ESRGAN failed for requested ${requestedOutputPixels / 1_000_000L}MP", failure)
            if (replacementStarted && originalBackup.exists()) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        originalBackup.inputStream().use { input -> input.copyTo(output) }
                    } ?: error("Cannot restore the original MediaStore JPEG")
                }.onFailure {
                    Log.e(TAG, "Rollback of original JPEG failed", it)
                }
            }
            Log.e(TAG, "AI super-resolution failed; original camera JPEG rollback was attempted", failure)
            false
        } finally {
            sourceBitmap?.let { if (!it.isRecycled) it.recycle() }
            modelInput?.let { if (!it.isRecycled) it.recycle() }
            outputBitmap?.let { if (!it.isRecycled) it.recycle() }
            runCatching { tempOutput.delete() }
            runCatching { originalBackup.delete() }
        }
    }

    private fun copyModelAsset(context: Context, assetName: String): File {
        val modelDir = File(context.noBackupFilesDir, "ncnn-models").apply { mkdirs() }
        val target = File(modelDir, assetName)
        if (!target.exists() || target.length() == 0L) {
            val temp = File(modelDir, "$assetName.part")
            context.assets.open(assetName).use { input ->
                FileOutputStream(temp).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            check(temp.renameTo(target)) { "Could not install NCNN model asset: $assetName" }
        }
        return target
    }

    private fun readImportantExif(context: Context, uri: Uri): Map<String, String> {
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
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_IMAGE_DESCRIPTION,
            ExifInterface.TAG_USER_COMMENT
        )
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                val exif = ExifInterface(descriptor.fileDescriptor)
                tags.mapNotNull { tag -> exif.getAttribute(tag)?.let { tag to it } }.toMap()
            }.orEmpty()
        }.getOrDefault(emptyMap())
    }

    private fun writeImportantExif(file: File, values: Map<String, String>) {
        ExifInterface(file.absolutePath).apply {
            values.forEach { (tag, value) -> setAttribute(tag, value) }
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            saveAttributes()
        }
    }
}

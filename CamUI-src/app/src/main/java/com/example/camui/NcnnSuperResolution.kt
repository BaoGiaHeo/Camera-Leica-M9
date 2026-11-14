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
 * The native layer receives RGB_565 bitmaps and runs the photo-oriented x4plus model on overlapped tiles.
 * The model input is normalized to roughly 13.19 MP so its x4 output targets about 211 MP.
 * This is a pixel-count target, not 211 MP of optical sensor detail; AI reconstruction is estimated.
 */
object NcnnSuperResolution {
    private const val TAG = "FUJI_NCNN_SR"
    private const val MODEL_PARAM = "realesrgan-x4plus.param"
    private const val MODEL_BIN = "realesrgan-x4plus.bin"
    private const val TARGET_MODEL_INPUT_PIXELS = 13_187_500L
    private const val TARGET_OUTPUT_PIXELS = 211_000_000L
    private const val TILE_SIZE = 256

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
    fun enhanceCapturedPhoto(context: Context, uri: Uri): Boolean {
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
            val scale = sqrt(TARGET_MODEL_INPUT_PIXELS.toDouble() / inputPixels.toDouble())
            val scaledWidth = (originalWidth * scale).toInt().coerceAtLeast(1)
            val scaledHeight = (originalHeight * scale).toInt().coerceAtLeast(1)
            modelInput = if (scaledWidth != originalWidth || scaledHeight != originalHeight) {
                Bitmap.createScaledBitmap(src, scaledWidth, scaledHeight, true).also {
                    if (it !== src) src.recycle()
                    Log.i(TAG, "AI model input normalized: ${originalWidth}x${originalHeight} -> ${it.width}x${it.height}")
                }
            } else src
            sourceBitmap = null // modelInput now owns the reference

            val outputWidth = modelInput!!.width * 4
            val outputHeight = modelInput!!.height * 4
            require(outputWidth.toLong() * outputHeight.toLong() <= TARGET_OUTPUT_PIXELS) {
                "AI output exceeds the 211 MP target budget"
            }
            val requiredOutputBytes = outputWidth.toLong() * outputHeight.toLong() * 2L
            val heapBudget = Runtime.getRuntime().maxMemory()
            require(requiredOutputBytes < heapBudget * 0.78) {
                "211 MP AI output exceeds this device's safe memory budget"
            }
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
            Log.i(TAG, "Real-ESRGAN x4 photo model saved: ${outputWidth}x${outputHeight} (~${outputWidth.toLong() * outputHeight / 1_000_000L} MP)")
            true
        } catch (failure: Throwable) {
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

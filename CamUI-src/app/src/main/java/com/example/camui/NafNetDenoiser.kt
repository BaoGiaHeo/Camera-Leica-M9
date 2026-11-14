package com.example.camui

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * On-device NAFNet-SIDD width-32 denoiser.
 *
 * The model consumes RGB float32 NCHW data in [0, 1] and returns a same-size
 * restored image. Tiles overlap by a 64-pixel context margin; only each tile's
 * center is copied to the output to suppress grid seams.
 */
object NafNetDenoiser {
    private const val TAG = "FUJI_NAFNET"
    private const val MODEL_ASSET = "nafnet-sidd-w32.onnx"
    private const val TILE_SIZE = 512
    private const val HALO = 32
    private const val CORE_SIZE = TILE_SIZE - HALO * 2
    private const val MODEL_BYTES_MIN = 100L * 1024L * 1024L

    private val inferenceLock = Any()
    @Volatile private var ortEnvironment: OrtEnvironment? = null
    @Volatile private var ortSession: OrtSession? = null
    @Volatile private var usesNnapi = false

    /**
     * Replaces the MediaStore JPEG only after a complete inference and successful encode.
     * The original file is backed up first and restored if MediaStore publication fails.
     */
    fun enhanceCapturedPhoto(
        context: Context,
        uri: Uri,
        strength: Float,
        onProgress: (Int) -> Unit = {}
    ): Boolean = synchronized(inferenceLock) {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val tempOutput = File(context.cacheDir, "nafnet-$stamp.jpg")
        val originalBackup = File(context.cacheDir, "nafnet-original-$stamp.jpg")
        var sourceBitmap: Bitmap? = null
        var outputBitmap: Bitmap? = null
        var replacementStarted = false

        try {
            val originalExif = readImportantExif(context, uri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val originalWidth = bounds.outWidth
            val originalHeight = bounds.outHeight
            require(originalWidth > 0 && originalHeight > 0) { "Invalid input JPEG dimensions" }

            val maxPixels = safeModelPixelBudget(context)
            val sourcePixels = originalWidth.toLong() * originalHeight.toLong()
            var sampleSize = 1
            while (sourcePixels / (sampleSize.toLong() * sampleSize.toLong()) > maxPixels * 2L &&
                sampleSize < 8
            ) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inDither = false
                inSampleSize = sampleSize
            }
            var decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: error("Could not decode captured JPEG")
            if (decoded.config != Bitmap.Config.ARGB_8888) {
                val converted = decoded.copy(Bitmap.Config.ARGB_8888, false)
                    ?: error("Could not convert image to ARGB_8888")
                decoded.recycle()
                decoded = converted
            }
            val decodedPixels = decoded.width.toLong() * decoded.height.toLong()
            sourceBitmap = if (decodedPixels > maxPixels) {
                val scale = sqrt(maxPixels.toDouble() / decodedPixels.toDouble())
                val scaled = Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                )
                if (scaled !== decoded) decoded.recycle()
                scaled
            } else decoded

            val workingSource = sourceBitmap ?: error("Input bitmap unavailable")
            outputBitmap = workingSource.copy(Bitmap.Config.ARGB_8888, true)
                ?: error("Could not allocate denoising output")
            val model = ensureModelFile(context)
            val session = getSession(model)
            val inputName = session.inputNames.firstOrNull() ?: error("NAFNet has no input node")
            val outputName = session.outputNames.firstOrNull() ?: error("NAFNet has no output node")
            val inputBuffer = ByteBuffer.allocateDirect(TILE_SIZE * TILE_SIZE * 3 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            val tilePixels = IntArray(TILE_SIZE * TILE_SIZE)
            val sourceRow = IntArray(CORE_SIZE)
            val outputRow = IntArray(CORE_SIZE)
            val strengthValue = strength.coerceIn(0f, 1f)
            val tilesX = ceil(workingSource.width.toDouble() / CORE_SIZE).toInt()
            val tilesY = ceil(workingSource.height.toDouble() / CORE_SIZE).toInt()
            val totalTiles = (tilesX * tilesY).coerceAtLeast(1)
            var completedTiles = 0

            for (tileY in 0 until workingSource.height step CORE_SIZE) {
                for (tileX in 0 until workingSource.width step CORE_SIZE) {
                    // Copy a 512x512 tile with replicated borders.
                    val requestedLeft = tileX - HALO
                    val sourceLeft = requestedLeft.coerceAtLeast(0)
                    val destinationLeft = sourceLeft - requestedLeft
                    val validWidth = (workingSource.width - sourceLeft)
                        .coerceAtMost(TILE_SIZE - destinationLeft)
                    for (ty in 0 until TILE_SIZE) {
                        val sourceY = (tileY + ty - HALO).coerceIn(0, workingSource.height - 1)
                        workingSource.getPixels(
                            tilePixels,
                            ty * TILE_SIZE + destinationLeft,
                            TILE_SIZE,
                            sourceLeft,
                            sourceY,
                            validWidth,
                            1
                        )
                        val rowOffset = ty * TILE_SIZE
                        val first = tilePixels[rowOffset + destinationLeft]
                        val last = tilePixels[rowOffset + destinationLeft + validWidth - 1]
                        for (x in 0 until destinationLeft) tilePixels[rowOffset + x] = first
                        for (x in destinationLeft + validWidth until TILE_SIZE) {
                            tilePixels[rowOffset + x] = last
                        }
                    }

                    inputBuffer.clear()
                    val planeSize = TILE_SIZE * TILE_SIZE
                    for (channel in 0..2) {
                        for (pixel in tilePixels) {
                            val value = when (channel) {
                                0 -> Color.red(pixel)
                                1 -> Color.green(pixel)
                                else -> Color.blue(pixel)
                            }
                            inputBuffer.put(value / 255f)
                        }
                    }
                    inputBuffer.rewind()

                    val tensor = OnnxTensor.createTensor(
                        requireNotNull(ortEnvironment),
                        inputBuffer,
                        longArrayOf(1L, 3L, TILE_SIZE.toLong(), TILE_SIZE.toLong())
                    )
                    var tileResult: OrtSession.Result? = null
                    try {
                        tileResult = session.run(mapOf(inputName to tensor))
                        val outputTensor = tileResult.get(outputName).orElseThrow {
                            IllegalStateException("NAFNet output node '$outputName' is missing")
                        } as OnnxTensor
                        val outputFloats: FloatBuffer = outputTensor.floatBuffer
                        require(outputFloats.capacity() >= planeSize * 3) {
                            "Unexpected NAFNet output tensor size: " + outputFloats.capacity()
                        }

                        val coreWidth = (workingSource.width - tileX).coerceAtMost(CORE_SIZE)
                        val coreHeight = (workingSource.height - tileY).coerceAtMost(CORE_SIZE)
                        for (localY in 0 until coreHeight) {
                            val imageY = tileY + localY
                            workingSource.getPixels(
                                sourceRow, 0, coreWidth, tileX, imageY, coreWidth, 1
                            )
                            for (localX in 0 until coreWidth) {
                                val modelIndex = (localY + HALO) * TILE_SIZE + localX + HALO
                                val original = sourceRow[localX]
                                val originalR = Color.red(original) / 255f
                                val originalG = Color.green(original) / 255f
                                val originalB = Color.blue(original) / 255f
                                val denoisedR = outputFloats.get(modelIndex).coerceIn(0f, 1f)
                                val denoisedG = outputFloats.get(planeSize + modelIndex).coerceIn(0f, 1f)
                                val denoisedB = outputFloats.get(planeSize * 2 + modelIndex).coerceIn(0f, 1f)
                                val r = ((originalR * (1f - strengthValue) + denoisedR * strengthValue) * 255f)
                                    .toInt().coerceIn(0, 255)
                                val g = ((originalG * (1f - strengthValue) + denoisedG * strengthValue) * 255f)
                                    .toInt().coerceIn(0, 255)
                                val b = ((originalB * (1f - strengthValue) + denoisedB * strengthValue) * 255f)
                                    .toInt().coerceIn(0, 255)
                                outputRow[localX] = Color.argb(Color.alpha(original), r, g, b)
                            }
                            outputBitmap!!.setPixels(
                                outputRow, 0, coreWidth, tileX, imageY, coreWidth, 1
                            )
                        }
                    } finally {
                        tileResult?.close()
                        tensor.close()
                    }
                    completedTiles++
                    onProgress((completedTiles * 100 / totalTiles).coerceIn(0, 100))
                }
            }

            sourceBitmap?.recycle()
            sourceBitmap = null
            var finalBitmap = outputBitmap ?: error("NAFNet produced no image")
            outputBitmap = null
            if (finalBitmap.width != originalWidth || finalBitmap.height != originalHeight) {
                val restoredSize = Bitmap.createScaledBitmap(
                    finalBitmap, originalWidth, originalHeight, true
                )
                finalBitmap.recycle()
                finalBitmap = restoredSize
                Log.i(TAG, "Large JPEG processed within RAM budget and restored to \${originalWidth}x\${originalHeight}")
            }

            FileOutputStream(tempOutput).use { output ->
                check(finalBitmap.compress(Bitmap.CompressFormat.JPEG, 97, output)) {
                    "Could not encode NAFNet output JPEG"
                }
                output.flush()
                output.fd.sync()
            }
            finalBitmap.recycle()
            check(tempOutput.length() > 0L) { "NAFNet output JPEG is empty" }
            writeImportantExif(tempOutput, originalExif)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(originalBackup).use { backup ->
                    input.copyTo(backup)
                    backup.fd.sync()
                }
            } ?: error("Could not create a rollback copy of the camera JPEG")

            replacementStarted = true
            val copied = context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                tempOutput.inputStream().use { input -> input.copyTo(output) }
                true
            } ?: false
            check(copied) { "Could not replace the MediaStore image" }

            context.contentResolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
                ExifInterface(descriptor.fileDescriptor).apply {
                    setAttribute(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL.toString()
                    )
                    saveAttributes()
                }
            }
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.ORIENTATION, 0) },
                null,
                null
            )
            Log.i(
                TAG,
                "NAFNet-SIDD denoising completed strength=$strengthValue " +
                    "size=\${originalWidth}x\${originalHeight} backend=\${if (usesNnapi) "NNAPI/CPU" else "CPU"}"
            )
            onProgress(100)
            true
        } catch (failure: Throwable) {
            if (replacementStarted && originalBackup.exists()) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        originalBackup.inputStream().use { input -> input.copyTo(output) }
                    } ?: error("Could not restore the original JPEG")
                }.onFailure { Log.e(TAG, "Original JPEG rollback failed", it) }
            }
            Log.e(TAG, "NAFNet failed; captured JPEG remains the source image", failure)
            false
        } finally {
            sourceBitmap?.let { if (!it.isRecycled) it.recycle() }
            outputBitmap?.let { if (!it.isRecycled) it.recycle() }
            runCatching { tempOutput.delete() }
            runCatching { originalBackup.delete() }
        }
    }

    private fun safeModelPixelBudget(context: Context): Long {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryBytes = manager.memoryClass.toLong() * 1024L * 1024L
        // Two ARGB bitmaps plus model/runtime activation memory; adapt to the device's app heap.
        return (memoryBytes / 44L).coerceIn(6_000_000L, 20_000_000L)
    }

    private fun ensureModelFile(context: Context): File {
        val directory = File(context.noBackupFilesDir, "nafnet-models").apply { mkdirs() }
        val target = File(directory, MODEL_ASSET)
        if (target.exists() && target.length() >= MODEL_BYTES_MIN) return target
        val temporary = File(directory, "$MODEL_ASSET.part")
        temporary.delete()
        context.assets.open(MODEL_ASSET).use { input ->
            FileOutputStream(temporary).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
        check(temporary.length() >= MODEL_BYTES_MIN) { "Bundled NAFNet ONNX model is incomplete" }
        check(temporary.renameTo(target)) { "Could not install NAFNet model file" }
        return target
    }

    private fun getSession(modelFile: File): OrtSession {
        ortSession?.let { return it }
        synchronized(this) {
            ortSession?.let { return it }
            val environment = OrtEnvironment.getEnvironment()
            ortEnvironment = environment
            val options = OrtSession.SessionOptions()
            try {
                options.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(2, 4))
                val accelerated = runCatching {
                    options.addNnapi()
                    environment.createSession(modelFile.absolutePath, options)
                }
                if (accelerated.isSuccess) {
                    usesNnapi = true
                    return accelerated.getOrThrow().also { ortSession = it }
                }
            } finally {
                options.close()
            }

            val cpuOptions = OrtSession.SessionOptions()
            try {
                cpuOptions.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(2, 4))
                val cpuSession = environment.createSession(modelFile.absolutePath, cpuOptions)
                usesNnapi = false
                ortSession = cpuSession
                return cpuSession
            } finally {
                cpuOptions.close()
            }
        }
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

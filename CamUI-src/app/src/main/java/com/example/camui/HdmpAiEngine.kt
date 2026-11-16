package com.example.camui

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.security.MessageDigest
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * HDMP-owned AI coordinator.
 *
 * NAFNet is used only as a conservative denoising/restoration assistant. It never writes to
 * MediaStore and never owns the final image. HDMP validates, blends, commits and then continues
 * with the deterministic HdmpEngine. The GAN-based Real-ESRGAN path is deliberately not invoked:
 * it can synthesize texture and conflicts with the no-independent-generation requirement.
 *
 * Stage A changes (same network, same inference maths, same blend constants):
 *  - AI input, blend base and AI output are now 8-bit per channel (ARGB_8888). The previous
 *    RGB_565 path quantised every channel to 5/6 bits, which hid the denoising and caused banding.
 *  - JPEG is decoded one full-width band per tile row instead of one region per tile.
 *  - Retries only happen for memory-class failures. Deterministic failures stop immediately.
 *  - Model SHA-256 is verified once per model file instead of on every photo.
 *  - Redundant fsync / EXIF rewrite / original copy removed.
 *  - A per-phase timing summary is logged and appended to Result.detail.
 */
internal object HdmpAiEngine {

    private const val TAG = "FUJI_HDMP_AI"
    private const val TILE_SIZE = 256
    private const val HALO = 32
    private const val CORE_SIZE = TILE_SIZE - HALO * 2
    private const val MODEL_MIN_BYTES = 1_000_000L

    // Output is ARGB_8888 (4 bytes/pixel). 0.44 keeps exactly the same maximum pixel count as the
    // earlier RGB_565 output at 0.22 of the Java heap, so no image size that worked before is rejected.
    private const val OUTPUT_BYTES_PER_PIXEL = 4L
    private const val MAX_OUTPUT_HEAP_FRACTION = 0.44

    enum class Model {
        NAFNET_WIDTH64,
        NAFNETLIB_WIDTH32,
        CLASSICAL_ONLY
    }

    data class Result(
        val model: Model,
        val applied: Boolean,
        val detail: String
    )

    private data class ModelSpec(
        val assetName: String,
        val manifestName: String,
        val blend: Float,
        val deltaLimit: Float
    )

    private data class RunTiming(
        val width: Int,
        val height: Int,
        val tiles: Int,
        val workers: Int,
        val setupMs: Long,
        val decodeMs: Long,
        val prepMs: Long,
        val inferMs: Long,
        val postMs: Long,
        val throttleMs: Long,
        val encodeMs: Long,
        val totalMs: Long
    ) {
        fun summary(): String =
            "${width}x${height}, $tiles tile, $workers luồng; " +
                "khởi tạo=${setupMs}ms, giải mã=${decodeMs}ms, nạp=${prepMs}ms, " +
                "AI=${inferMs}ms, ghép=${postMs}ms, nghỉ nhiệt=${throttleMs}ms, " +
                "nén=${encodeMs}ms, chạy mô hình=${totalMs}ms"
    }

    private val primarySpec = ModelSpec(
        assetName = "nafnet_sidd_width64.onnx",
        manifestName = "nafnet_sidd_width64.onnx",
        blend = 0.45f,
        deltaLimit = 0.12f
    )

    private val fallbackSpec = ModelSpec(
        assetName = "nafnetlib_sidd_width32.onnx",
        manifestName = "nafnetlib_sidd_width32.onnx",
        blend = 0.38f,
        deltaLimit = 0.10f
    )

    /** Derive a conservative ONNX thread budget from CPU, free RAM and thermal status. */
    fun resolveWorkerCount(context: Context, requested: Int): Int {
        val cpuLimit = Runtime.getRuntime().availableProcessors().coerceIn(1, 8)
        val memoryLimit = runCatching {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memory = ActivityManager.MemoryInfo()
            manager.getMemoryInfo(memory)
            when {
                memory.lowMemory || memory.availMem < 512L * 1024L * 1024L -> 1
                memory.availMem < 1_024L * 1024L * 1024L -> 2
                memory.availMem < 2_048L * 1024L * 1024L -> 4
                else -> 8
            }
        }.getOrDefault(2)
        val batteryTempTenths = readBatteryTemperatureTenths(context)
        val thermalStatus = readThermalStatus(context)
        val thermalLimit = when {
            thermalStatus >= PowerManager.THERMAL_STATUS_EMERGENCY || batteryTempTenths >= 450 -> 1
            thermalStatus >= PowerManager.THERMAL_STATUS_CRITICAL || batteryTempTenths >= 430 -> 2
            thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE || batteryTempTenths >= 420 -> 3
            thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE || batteryTempTenths > 400 -> 4
            thermalStatus >= PowerManager.THERMAL_STATUS_LIGHT || batteryTempTenths >= 390 -> 6
            else -> 8
        }
        val resolved = min(requested.coerceIn(1, 8), min(cpuLimit, min(memoryLimit, thermalLimit)))
            .coerceAtLeast(1)
        Log.i(
            TAG,
            "workers requested=$requested cpu=$cpuLimit memory=$memoryLimit " +
                "thermal=$thermalLimit (status=$thermalStatus batteryTemp=${batteryTempTenths / 10.0}C) -> $resolved"
        )
        return resolved
    }

    private fun readThermalStatus(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return PowerManager.THERMAL_STATUS_NONE
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return PowerManager.THERMAL_STATUS_NONE
        return runCatching { powerManager.currentThermalStatus }
            .getOrDefault(PowerManager.THERMAL_STATUS_NONE)
    }

    private fun readBatteryTemperatureTenths(context: Context): Int {
        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        return batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    }

    /** Brief between-tile duty-cycle adjustment; an active native ONNX tile is never interrupted. */
    private fun thermalTileThrottleMs(context: Context): Long {
        val status = readThermalStatus(context)
        val batteryTemp = readBatteryTemperatureTenths(context)
        return when {
            status >= PowerManager.THERMAL_STATUS_CRITICAL || batteryTemp >= 430 -> 180L
            status >= PowerManager.THERMAL_STATUS_SEVERE || batteryTemp >= 420 -> 120L
            status >= PowerManager.THERMAL_STATUS_MODERATE || batteryTemp > 400 -> 70L
            status >= PowerManager.THERMAL_STATUS_LIGHT || batteryTemp >= 390 -> 25L
            else -> 0L
        }
    }

    private fun outputFitsMemory(width: Int, height: Int): Boolean {
        val outputBytes = width.toLong() * height.toLong() * OUTPUT_BYTES_PER_PIXEL
        val safeBytes =
            (Runtime.getRuntime().maxMemory().toDouble() * MAX_OUTPUT_HEAP_FRACTION).toLong()
        return outputBytes <= safeBytes
    }

    /**
     * Only memory-class failures are worth retrying with fewer threads. A deterministic failure
     * (validation, shape, missing model, encode) would simply fail again after a full extra pass.
     */
    private fun isRetryable(failure: Throwable): Boolean {
        var cause: Throwable? = failure
        var depth = 0
        while (cause != null && depth < 6) {
            if (cause is OutOfMemoryError) return true
            if (cause is OrtException) {
                val message = cause.message.orEmpty().lowercase()
                if ("alloc" in message || "out of memory" in message) return true
            }
            cause = cause.cause
            depth++
        }
        return false
    }

    fun process(
        context: Context,
        uri: Uri,
        workerCount: Int,
        selectedModel: Model = Model.NAFNET_WIDTH64,
        onAdaptiveWorkerCount: (Int) -> Unit = {},
        onTileProgress: (Model, Int, Int) -> Unit = { _, _, _ -> },
        onProgress: (Model, Int) -> Unit = { _, _ -> }
    ): Result {
        require(selectedModel != Model.CLASSICAL_ONLY) { "An AI model must be selected" }
        val stamp = System.nanoTime().toString()
        val original = File(context.cacheDir, "hdmp-ai-original-$stamp.jpg")
        val rendered = File(context.cacheDir, "hdmp-ai-rendered-$stamp.jpg")
        val spec = if (selectedModel == Model.NAFNET_WIDTH64) primarySpec else fallbackSpec
        val modelLabel = if (selectedModel == Model.NAFNET_WIDTH64) "HDMP AI 2" else "HDMP AI 1.5"
        val jobStartedAt = SystemClock.elapsedRealtime()

        return try {
            copyUriToFile(context, uri, original)
            val copyMs = SystemClock.elapsedRealtime() - jobStartedAt

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(original.absolutePath, bounds)
            val width = bounds.outWidth
            val height = bounds.outHeight
            require(width >= 3 && height >= 3) { "HDMP AI source dimensions are invalid" }

            check(outputFitsMemory(width, height)) {
                "Ảnh vượt giới hạn bộ nhớ an toàn; hãy giảm độ phân giải đầu ra rồi thử lại."
            }

            val effectiveWorkerCount = resolveWorkerCount(context, workerCount)
            runCatching { onAdaptiveWorkerCount(effectiveWorkerCount) }
            Log.i(TAG, "Locked model=$selectedModel, workers=$effectiveWorkerCount")

            // The 10s/20s values are UI targets only. Never abort or replace the selected
            // model when a device needs longer: inference continues tile-by-tile.
            // The network stays fixed. Fewer-thread retries happen only after a memory-class
            // failure; deterministic failures stop at once instead of repeating a full pass.
            val workerPlan = mutableListOf(effectiveWorkerCount)
            var retryWorkers = effectiveWorkerCount
            while (retryWorkers > 1 && workerPlan.size < 4) {
                retryWorkers = (retryWorkers / 2).coerceAtLeast(1)
                workerPlan += retryWorkers
            }
            while (workerPlan.size < 4) workerPlan += 1

            var timing: RunTiming? = null
            var lastFailure: Throwable? = null
            for ((index, attemptWorkers) in workerPlan.withIndex()) {
                runCatching { onProgress(selectedModel, 0) }
                rendered.delete()
                try {
                    timing = runModel(
                        context = context,
                        source = original,
                        destination = rendered,
                        spec = spec,
                        workerCount = attemptWorkers,
                        model = selectedModel,
                        onTileProgress = onTileProgress,
                        onProgress = onProgress
                    )
                    break
                } catch (failure: Throwable) {
                    lastFailure = failure
                    Log.w(TAG, "$modelLabel inference attempt ${index + 1} failed", failure)
                    if (!isRetryable(failure) || index == workerPlan.lastIndex) break
                    Log.w(TAG, "$modelLabel retrying the same selected network with fewer workers")
                    System.gc()
                    SystemClock.sleep(150L)
                }
            }

            val finishedTiming = timing ?: throw IllegalStateException(
                "$modelLabel could not complete",
                lastFailure
            )

            val commitStartedAt = SystemClock.elapsedRealtime()
            check(commitRendered(context, uri, original, rendered)) {
                "$modelLabel output could not be committed"
            }
            val commitMs = SystemClock.elapsedRealtime() - commitStartedAt
            preserveOriginalForComparison(context, uri, original)

            val totalMs = SystemClock.elapsedRealtime() - jobStartedAt
            val report = "${finishedTiming.summary()}, đọc=${copyMs}ms, ghi=${commitMs}ms, tổng=${totalMs}ms"
            Log.i(TAG, "$modelLabel timing: $report")
            Result(
                selectedModel,
                true,
                "$modelLabel đã xử lý hoàn tất bằng đúng mô hình đã chọn. [$report]"
            )
        } catch (failure: Throwable) {
            Log.e(TAG, "$modelLabel processing failed; no classical fallback is permitted", failure)
            Result(
                selectedModel,
                false,
                "$modelLabel chưa thể hoàn tất. Ảnh gốc được giữ nguyên; không chuyển sang thuật toán thường."
            )
        } finally {
            runCatching { original.delete() }
            runCatching { rendered.delete() }
        }
    }

    /** Runs the selected network over the whole image. Throws on any failure; never returns a partial result. */
    private fun runModel(
        context: Context,
        source: File,
        destination: File,
        spec: ModelSpec,
        workerCount: Int,
        model: Model,
        onTileProgress: (Model, Int, Int) -> Unit,
        onProgress: (Model, Int) -> Unit
    ): RunTiming {
        var sourceStream: FileInputStream? = null
        var decoder: BitmapRegionDecoder? = null
        var outputBitmap: Bitmap? = null
        var session: OrtSession? = null
        var options: OrtSession.SessionOptions? = null
        val runStartedAt = SystemClock.elapsedRealtime()

        try {
            val modelFile = verifiedModelFile(context, spec)
                ?: error("Model file missing or failed verification: ${spec.assetName}")

            val openedStream = FileInputStream(source)
            sourceStream = openedStream
            val openedDecoder = BitmapRegionDecoder.newInstance(openedStream, false)
                ?: error("Unable to open the source image for AI tiles")
            decoder = openedDecoder

            val width = openedDecoder.width
            val height = openedDecoder.height
            check(outputFitsMemory(width, height)) { "AI output exceeds the safe memory limit" }

            // 8-bit per channel output. RGB_565 is intentionally not used any more.
            val finalOutput = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            finalOutput.setHasAlpha(false)
            outputBitmap = finalOutput

            val env = OrtEnvironment.getEnvironment()
            options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(workerCount.coerceIn(1, 8))
                setInterOpNumThreads(1)
            }
            val openedSession = env.createSession(modelFile.absolutePath, options)
            session = openedSession
            val inputName = openedSession.inputNames.firstOrNull() ?: error("ONNX input missing")
            val outputName = openedSession.outputNames.firstOrNull() ?: error("ONNX output missing")
            val setupMs = SystemClock.elapsedRealtime() - runStartedAt

            val rows = ceil(height.toDouble() / CORE_SIZE).toInt()
            val columns = ceil(width.toDouble() / CORE_SIZE).toInt()
            val totalTiles = rows * columns

            val plane = TILE_SIZE * TILE_SIZE
            val patchPixels = IntArray(plane)
            val inputBuffer = ByteBuffer.allocateDirect(plane * 3 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            val outputPixels = IntArray(CORE_SIZE * CORE_SIZE)
            // One full-width band (at most TILE_SIZE rows) is decoded per tile row.
            val bandPixels = IntArray(width * min(height, TILE_SIZE))
            val decodeOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    inPreferredColorSpace =
                        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB)
                }
            }

            var decodeNs = 0L
            var prepNs = 0L
            var inferNs = 0L
            var postNs = 0L
            var throttleNs = 0L

            var tileIndex = 0
            var lastThermalSampleAt = 0L
            var thermalThrottleMs = 0L

            for (coreTop in 0 until height step CORE_SIZE) {
                // All output tiles must be rendered by the selected AI model.
                val patchTop = coreTop - HALO
                val bandTop = patchTop.coerceAtLeast(0)
                val bandBottom = min(height, patchTop + TILE_SIZE)

                val decodeStartedAt = System.nanoTime()
                val band = openedDecoder.decodeRegion(Rect(0, bandTop, width, bandBottom), decodeOptions)
                    ?: error("Unable to decode AI band")
                val bandWidth = band.width
                val bandHeight = band.height
                try {
                    check(bandWidth * bandHeight <= bandPixels.size) { "Unexpected AI band size" }
                    band.getPixels(bandPixels, 0, bandWidth, 0, 0, bandWidth, bandHeight)
                } finally {
                    band.recycle()
                }
                decodeNs += System.nanoTime() - decodeStartedAt

                for (coreLeft in 0 until width step CORE_SIZE) {
                    val patchLeft = coreLeft - HALO

                    val prepStartedAt = System.nanoTime()
                    // Edge-replicated TILE_SIZE x TILE_SIZE patch cut from the decoded band.
                    for (py in 0 until TILE_SIZE) {
                        val globalY = (patchTop + py).coerceIn(0, height - 1)
                        val bandY = (globalY - bandTop).coerceIn(0, bandHeight - 1)
                        val rowBase = bandY * bandWidth
                        for (px in 0 until TILE_SIZE) {
                            val globalX = (patchLeft + px).coerceIn(0, width - 1)
                            val bandX = globalX.coerceIn(0, bandWidth - 1)
                            patchPixels[py * TILE_SIZE + px] = bandPixels[rowBase + bandX]
                        }
                    }

                    inputBuffer.clear()
                    HdmpAiColorLayout.writeRgb(patchPixels, inputBuffer)
                    inputBuffer.flip()

                    val inputTensor = OnnxTensor.createTensor(
                        env,
                        inputBuffer,
                        longArrayOf(1L, 3L, TILE_SIZE.toLong(), TILE_SIZE.toLong())
                    )
                    prepNs += System.nanoTime() - prepStartedAt

                    try {
                        val inferStartedAt = System.nanoTime()
                        openedSession.run(mapOf(inputName to inputTensor)).use { result ->
                            inferNs += System.nanoTime() - inferStartedAt
                            val postStartedAt = System.nanoTime()

                            val outputTensor = result.get(outputName).orElseThrow {
                                IllegalStateException("ONNX output tensor missing")
                            } as? OnnxTensor ?: error("ONNX output is not a tensor")
                           

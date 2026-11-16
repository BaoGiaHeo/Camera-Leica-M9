package com.example.camui

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import ai.onnxruntime.providers.NNAPIFlags
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
import java.util.EnumSet
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * HDMP-owned AI coordinator.
 *
 * NAFNet is used as a conservative denoising/restoration stage. It never writes to MediaStore:
 * this engine validates, blends, and commits the result. The deterministic HdmpEngine remains a
 * separate classical path. The GAN-based Real-ESRGAN path is deliberately not invoked because it
 * can synthesize texture and conflicts with the no-independent-generation requirement.
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
        val backend: String,
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
            "${width}x${height}, $tiles tile, $workers luồng, EP=$backend; " +
                "khởi tạo=${setupMs}ms, giải mã=${decodeMs}ms, nạp=${prepMs}ms, " +
                "AI=${inferMs}ms, ghép=${postMs}ms, nghỉ nhiệt=${throttleMs}ms, " +
                "nén=${encodeMs}ms, chạy mô hình=${totalMs}ms"
    }

    // The previous blend values suppressed much of the model's denoising correction.
    // These stronger, bounded blends let the trained restoration model contribute more while
    // keeping a per-channel correction ceiling to limit halos and invented texture.
    private val primarySpec = ModelSpec(
        assetName = "nafnet_sidd_width64.onnx",
        manifestName = "nafnet_sidd_width64.onnx",
        blend = 0.68f,
        deltaLimit = 0.18f
    )

    private val fallbackSpec = ModelSpec(
        assetName = "nafnetlib_sidd_width32.onnx",
        manifestName = "nafnetlib_sidd_width32.onnx",
        blend = 0.58f,
        deltaLimit = 0.15f
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
        // Android already reduces CPU/GPU clocks under light and moderate thermal load.
        // Avoid injecting a sleep after every tile; only severe heat receives a short,
        // periodic cooldown (see the tile loop below).
        return when {
            status >= PowerManager.THERMAL_STATUS_CRITICAL || batteryTemp >= 430 -> 30L
            status >= PowerManager.THERMAL_STATUS_SEVERE || batteryTemp >= 420 -> 18L
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
            // Do not repeat an identical single-worker attempt after memory pressure.
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
        var reusableInputTensor: OnnxTensor? = null
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
            val availableProviders = runCatching {
                OrtEnvironment.getAvailableProviders().map { it.name }.sorted()
            }.getOrDefault(emptyList())
            Log.i(TAG, "ONNX Runtime available execution providers=$availableProviders")
            val providerPlan = buildList {
                // These NAFNet exports are float models. Start with XNNPACK's optimized
                // Arm CPU kernels; NNAPI performance and partitioning vary by device/driver.
                if ("XNNPACK" in availableProviders) add("XNNPACK")
                if ("NNAPI" in availableProviders) add("NNAPI_FP16")
                add("CPU")
            }
            var openedSession: OrtSession? = null
            var activeBackend = ""
            var lastSessionFailure: Throwable? = null

            // Prefer NNAPI with FP16 relaxation and NNAPI's CPU implementation disabled.
            // Unsupported operators can still use ORT's CPU kernels. If the device/driver
            // cannot create that session, try XNNPACK, then the regular optimized CPU EP.
            for (backend in providerPlan) {
                val candidateOptions = OrtSession.SessionOptions()
                try {
                    candidateOptions.setIntraOpNumThreads(workerCount.coerceIn(1, 8))
                    candidateOptions.setInterOpNumThreads(1)
                    candidateOptions.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)
                    candidateOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                    candidateOptions.setMemoryPatternOptimization(true)
                    candidateOptions.setCPUArenaAllocator(true)
                    candidateOptions.setLoggerId(TAG)
                    when (backend) {
                        "NNAPI_FP16" -> candidateOptions.addNnapi(
                            EnumSet.of(NNAPIFlags.USE_FP16, NNAPIFlags.CPU_DISABLED)
                        )
                        "XNNPACK" -> {
                            // XNNPACK owns the compute thread pool; avoid a second ORT pool
                            // competing for the same cores and disable busy-wait spinning.
                            candidateOptions.setIntraOpNumThreads(1)
                            candidateOptions.addConfigEntry("session.intra_op.allow_spinning", "0")
                            candidateOptions.addXnnpack(
                                mapOf("intra_op_num_threads" to workerCount.coerceIn(1, 8).toString())
                            )
                        }
                        // CPU is ONNX Runtime's default fallback provider.
                    }
                    openedSession = env.createSession(modelFile.absolutePath, candidateOptions)
                    options = candidateOptions
                    activeBackend = backend
                    Log.i(TAG, "ONNX session created with requested provider=$backend; available=$availableProviders")
                    break
                } catch (failure: Throwable) {
                    lastSessionFailure = failure
                    runCatching { candidateOptions.close() }
                    Log.w(TAG, "Could not create ONNX session with $backend; trying next provider", failure)
                }
            }

            val activeSession = openedSession ?: throw IllegalStateException(
                "No ONNX execution provider could create the selected model session",
                lastSessionFailure
            )
            session = activeSession
            val inputName = activeSession.inputNames.firstOrNull() ?: error("ONNX input missing")
            val outputName = activeSession.outputNames.firstOrNull() ?: error("ONNX output missing")
            val setupMs = SystemClock.elapsedRealtime() - runStartedAt

            val rows = ceil(height.toDouble() / CORE_SIZE).toInt()
            val columns = ceil(width.toDouble() / CORE_SIZE).toInt()
            val totalTiles = rows * columns

            val plane = TILE_SIZE * TILE_SIZE
            val patchPixels = IntArray(plane)
            val inputBuffer = ByteBuffer.allocateDirect(plane * 3 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            // The input tensor wraps this stable direct buffer and is reused for every tile.
            // ORT run() is synchronous here, so the buffer is rewritten only after each run ends.
            val inputTensor = OnnxTensor.createTensor(
                env,
                inputBuffer,
                longArrayOf(1L, 3L, TILE_SIZE.toLong(), TILE_SIZE.toLong())
            )
            reusableInputTensor = inputTensor
            val inputFeed = mapOf(inputName to inputTensor)
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
                    // Edge-replicated patch extraction. Copy whole contiguous row segments
                    // and fill only the left/right borders instead of clamping every pixel.
                    for (py in 0 until TILE_SIZE) {
                        val globalY = (patchTop + py).coerceIn(0, height - 1)
                        val bandY = (globalY - bandTop).coerceIn(0, bandHeight - 1)
                        val rowBase = bandY * bandWidth
                        val patchRow = py * TILE_SIZE
                        val sourceLeft = patchLeft.coerceAtLeast(0)
                        val sourceRight = (patchLeft + TILE_SIZE).coerceAtMost(width)
                        val copyWidth = sourceRight - sourceLeft
                        val destinationLeft = sourceLeft - patchLeft

                        if (destinationLeft > 0) {
                            val edge = bandPixels[rowBase + sourceLeft]
                            patchPixels.fill(edge, patchRow, patchRow + destinationLeft)
                        }
                        System.arraycopy(
                            bandPixels,
                            rowBase + sourceLeft,
                            patchPixels,
                            patchRow + destinationLeft,
                            copyWidth
                        )
                        val destinationRight = destinationLeft + copyWidth
                        if (destinationRight < TILE_SIZE) {
                            val edge = bandPixels[rowBase + sourceRight - 1]
                            patchPixels.fill(edge, patchRow + destinationRight, patchRow + TILE_SIZE)
                        }
                    }

                    inputBuffer.clear()
                    HdmpAiColorLayout.writeRgb(patchPixels, inputBuffer)
                    inputBuffer.flip()
                    prepNs += System.nanoTime() - prepStartedAt

                    val inferStartedAt = System.nanoTime()
                    activeSession.run(inputFeed).use { result ->
                            inferNs += System.nanoTime() - inferStartedAt
                            val postStartedAt = System.nanoTime()

                            val outputTensor = result.get(outputName).orElseThrow {
                                IllegalStateException("ONNX output tensor missing")
                            } as? OnnxTensor ?: error("ONNX output is not a tensor")
                            val tensorInfo = outputTensor.info as? TensorInfo
                                ?: error("ONNX output metadata is invalid")
                            check(
                                tensorInfo.shape.contentEquals(
                                    longArrayOf(1L, 3L, TILE_SIZE.toLong(), TILE_SIZE.toLong())
                                )
                            ) { "Unexpected NAFNet output tensor shape" }
                            val values = outputTensor.floatBuffer
                            check(values.remaining() == plane * 3) { "Unexpected NAFNet output buffer size" }

                            val tileWidth = min(width, coreLeft + CORE_SIZE) - coreLeft
                            val tileHeight = min(height, coreTop + CORE_SIZE) - coreTop
                            // Validate model output and blend it in one pass. The old path
                            // scanned every output pixel once for validation and a second time
                            // for blending, adding a full-image memory pass after inference.
                            var absoluteDeltaSum = 0.0
                            var validatedPixels = 0
                            val coreOffsetX = coreLeft - patchLeft
                            val coreOffsetY = coreTop - patchTop
                            for (localY in 0 until tileHeight) {
                                val patchY = coreOffsetY + localY
                                val outputRow = localY * CORE_SIZE
                                val patchRow = patchY * TILE_SIZE
                                for (localX in 0 until tileWidth) {
                                    val index = patchRow + coreOffsetX + localX
                                    val predictedR = values.get(index)
                                    val predictedG = values.get(plane + index)
                                    val predictedB = values.get(plane * 2 + index)
                                    check(
                                        predictedR.isFinite() &&
                                            predictedG.isFinite() &&
                                            predictedB.isFinite()
                                    ) { "AI produced NaN/Infinity" }

                                    val sourcePixel = patchPixels[index]
                                    val sourceR = (sourcePixel ushr 16) and 0xFF
                                    val sourceG = (sourcePixel ushr 8) and 0xFF
                                    val sourceB = sourcePixel and 0xFF
                                    val delta = max(
                                        kotlin.math.abs(predictedR - sourceR / 255f),
                                        max(
                                            kotlin.math.abs(predictedG - sourceG / 255f),
                                            kotlin.math.abs(predictedB - sourceB / 255f)
                                        )
                                    )
                                    absoluteDeltaSum += delta.toDouble()
                                    validatedPixels++
                                    outputPixels[outputRow + localX] = blendConservatively(
                                        sourcePixel,
                                        predictedR,
                                        predictedG,
                                        predictedB,
                                        spec
                                    )
                                }
                            }
                            val meanDelta = absoluteDeltaSum / validatedPixels.coerceAtLeast(1)
                            check(meanDelta <= spec.deltaLimit * 2.5f) {
                                "AI output deviates too far from the source image"
                            }

                            finalOutput.setPixels(
                                outputPixels,
                                0,
                                CORE_SIZE,
                                coreLeft,
                                coreTop,
                                tileWidth,
                                tileHeight
                            )
                            postNs += System.nanoTime() - postStartedAt
                    }

                    tileIndex++
                    runCatching { onTileProgress(model, tileIndex, totalTiles) }
                    val progressStep = (totalTiles / 100).coerceAtLeast(1)
                    if (tileIndex == totalTiles || tileIndex % progressStep == 0) {
                        runCatching {
                            onProgress(model, (tileIndex * 100 / totalTiles).coerceIn(0, 100))
                        }
                    }

                    // Sample thermal state only at tile boundaries. ONNX inference is not preempted.
                    val thermalNow = SystemClock.elapsedRealtime()
                    if (lastThermalSampleAt == 0L || thermalNow - lastThermalSampleAt >= 1_000L) {
                        thermalThrottleMs = thermalTileThrottleMs(context)
                        lastThermalSampleAt = thermalNow
                    }
                    if (thermalThrottleMs > 0L && tileIndex % 8 == 0) {
                        val sleepStartedAt = System.nanoTime()
                        SystemClock.sleep(thermalThrottleMs)
                        throttleNs += System.nanoTime() - sleepStartedAt
                    }
                }
            }

            val encodeStartedAt = SystemClock.elapsedRealtime()
            FileOutputStream(destination).use { stream ->
                check(finalOutput.compress(Bitmap.CompressFormat.JPEG, 97, stream)) {
                    "HDMP AI output JPEG encoding failed"
                }
                stream.flush()
            }
            check(destination.exists() && destination.length() > 0L) { "HDMP AI output is empty" }

            val checkBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destination.absolutePath, checkBounds)
            check(checkBounds.outWidth == width && checkBounds.outHeight == height) {
                "HDMP AI output dimensions changed"
            }

            writeExif(destination, readExif(source))
            val encodeMs = SystemClock.elapsedRealtime() - encodeStartedAt

            return RunTiming(
                width = width,
                height = height,
                tiles = totalTiles,
                workers = workerCount,
                backend = activeBackend,
                setupMs = setupMs,
                decodeMs = decodeNs / 1_000_000L,
                prepMs = prepNs / 1_000_000L,
                inferMs = inferNs / 1_000_000L,
                postMs = postNs / 1_000_000L,
                throttleMs = throttleNs / 1_000_000L,
                encodeMs = encodeMs,
                totalMs = SystemClock.elapsedRealtime() - runStartedAt
            )
        } finally {
            runCatching { reusableInputTensor?.close() }
            runCatching { session?.close() }
            runCatching { options?.close() }
            runCatching { decoder?.recycle() }
            runCatching { sourceStream?.close() }
            outputBitmap?.let { if (!it.isRecycled) it.recycle() }
        }
    }

    private fun blendConservatively(
        original: Int,
        predictedR: Float,
        predictedG: Float,
        predictedB: Float,
        spec: ModelSpec
    ): Int {
        val sourceR = (original ushr 16) and 0xFF
        val sourceG = (original ushr 8) and 0xFF
        val sourceB = original and 0xFF

        val sourceRf = sourceR / 255f
        val sourceGf = sourceG / 255f
        val sourceBf = sourceB / 255f
        val red = ((sourceRf + ((predictedR - sourceRf) * spec.blend)
            .coerceIn(-spec.deltaLimit, spec.deltaLimit)).coerceIn(0f, 1f) * 255f)
            .roundToInt().coerceIn(0, 255)
        val green = ((sourceGf + ((predictedG - sourceGf) * spec.blend)
            .coerceIn(-spec.deltaLimit, spec.deltaLimit)).coerceIn(0f, 1f) * 255f)
            .roundToInt().coerceIn(0, 255)
        val blue = ((sourceBf + ((predictedB - sourceBf) * spec.blend)
            .coerceIn(-spec.deltaLimit, spec.deltaLimit)).coerceIn(0f, 1f) * 255f)
            .roundToInt().coerceIn(0, 255)

        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }

    /**
     * Returns the verified model file. The SHA-256 of the cached file is computed only when the
     * file is new or changed (size / modification time differ from the last successful check).
     */
    private fun verifiedModelFile(context: Context, spec: ModelSpec): File? {
        val expectedHash = runCatching {
            context.assets.open("models/manifest.sha256").bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    val parts = line.trim().split(Regex("\\s+"), limit = 2)
                    if (parts.size == 2 && parts[1].trim() == spec.manifestName) {
                        parts[0].lowercase()
                    } else null
                }.firstOrNull()
            }
        }.getOrNull() ?: return null
        if (expectedHash.length != 64) return null

        val directory = File(context.noBackupFilesDir, "hdmp-ai-models")
        if (!directory.exists() && !directory.mkdirs()) return null
        val destination = File(directory, spec.assetName)
        val prefs = context.getSharedPreferences("hdmp_ai_model_cache", Context.MODE_PRIVATE)

        fun stampOf(file: File) = "$expectedHash:${file.length()}:${file.lastModified()}"

        if (destination.exists() && destination.length() >= MODEL_MIN_BYTES) {
            if (prefs.getString(spec.assetName, null) == stampOf(destination)) return destination
            if (sha256(destination) == expectedHash) {
                prefs.edit().putString(spec.assetName, stampOf(destination)).apply()
                return destination
            }
        }

        val temporary = File(directory, spec.assetName + ".partial")
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.assets.open("models/${spec.assetName}").use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                    output.fd.sync()
                }
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (temporary.length() < MODEL_MIN_BYTES || actualHash != expectedHash) {
                temporary.delete()
                return null
            }
            if (destination.exists()) destination.delete()
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
            val finished = destination.takeIf { it.exists() && it.length() >= MODEL_MIN_BYTES }
            if (finished != null) {
                prefs.edit().putString(spec.assetName, stampOf(finished)).apply()
            }
            finished
        } catch (failure: Throwable) {
            Log.w(TAG, "Model asset verification failed for ${spec.assetName}", failure)
            runCatching { temporary.delete() }
            null
        }
    }

    private fun sha256(file: File): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()

    /**
     * Retains only the latest AI source frame for the optional before/after viewer.
     * This is app-private storage; it does not add duplicate images to the user's gallery.
     * The cache copy is moved (not copied) because it is not needed after a successful commit.
     */
    private fun preserveOriginalForComparison(context: Context, uri: Uri, original: File) {
        runCatching {
            val directory = File(context.filesDir, "hdmp-comparison")
            check(directory.exists() || directory.mkdirs()) { "Could not create HDMP comparison directory" }
            val destination = File(directory, "latest-original.jpg")
            val temporary = File(directory, "latest-original.partial")
            if (temporary.exists()) temporary.delete()
            if (!original.renameTo(temporary)) {
                original.copyTo(temporary, overwrite = true)
            }
            check(temporary.length() > 0L) { "HDMP comparison backup is empty" }
            if (destination.exists() && !destination.delete()) {
                error("Could not replace previous HDMP comparison image")
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
            context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
                .edit()
                .putString("hdmp_compare_output_uri", uri.toString())
                .putString("hdmp_compare_original_path", destination.absolutePath)
                .apply()
        }.onFailure {
            Log.w(TAG, "Could not retain original for before/after comparison", it)
        }
    }

    /** Decode the retained original only when it belongs to the currently displayed result. */
    fun loadOriginalForComparison(context: Context, outputUri: Uri): Bitmap? {
        val prefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
        if (prefs.getString("hdmp_compare_output_uri", null) != outputUri.toString()) return null
        val path = prefs.getString("hdmp_compare_original_path", null) ?: return null
        val source = File(path)
        if (!source.isFile || source.length() <= 0L) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        return BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )
    }

    private fun commitRendered(context: Context, uri: Uri, original: File, rendered: File): Boolean {
        var replacing = false
        return try {
            check(rendered.exists() && rendered.length() > 0L) { "AI output missing before commit" }
            replacing = true
            val written = context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                FileInputStream(rendered).use { input -> input.copyTo(destination) }
                true
            } ?: false
            check(written) { "MediaStore refused the HDMP AI output" }

            // The rendered file already carries ORIENTATION_NORMAL (see writeExif), so the
            // second full-file EXIF rewrite that used to happen here is not needed.
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.ORIENTATION, 0) },
                null,
                null
            )
            true
        } catch (failure: Throwable) {
            if (replacing && original.exists()) runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.use { destination ->
                    FileInputStream(original).use { input -> input.copyTo(destination) }
                } ?: error("Could not restore original photo after AI commit failure")
            }.onFailure { Log.e(TAG, "Original photo rollback failed", it) }
            Log.e(TAG, "Could not commit HDMP AI output", failure)
            false
        }
    }

    private fun copyUriToFile(context: Context, uri: Uri, destination: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
                output.fd.sync() // this copy is the rollback source, keep it durable
            }
        } ?: error("HDMP AI cannot read captured photo")
        check(destination.exists() && destination.length() > 0L) { "Captured photo backup is empty" }
    }

    private fun readExif(file: File): Map<String, String> {
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
            ExifInterface.TAG_FLASH
        )
        return runCatching {
            val exif = ExifInterface(file.absolutePath)
            tags.mapNotNull { tag -> exif.getAttribute(tag)?.let { tag to it } }.toMap()
        }.getOrDefault(emptyMap())
    }

    private fun writeExif(file: File, attributes: Map<String, String>) {
        ExifInterface(file.absolutePath).apply {
            attributes.forEach { (tag, value) -> setAttribute(tag, value) }
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            saveAttributes()
        }
    }
}

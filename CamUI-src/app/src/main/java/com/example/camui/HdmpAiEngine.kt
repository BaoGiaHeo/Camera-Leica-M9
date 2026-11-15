package com.example.camui

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.app.ActivityManager
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
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
import android.os.SystemClock

/**
 * HDMP-owned AI coordinator.
 *
 * NAFNet is used only as a conservative denoising/restoration assistant. It never writes to
 * MediaStore and never owns the final image. HDMP validates, blends, commits and then continues
 * with the deterministic HdmpEngine. The GAN-based Real-ESRGAN path is deliberately not invoked:
 * it can synthesize texture and conflicts with the no-independent-generation requirement.
 */
internal object HdmpAiEngine {
    private const val TAG = "FUJI_HDMP_AI"
    private const val TILE_SIZE = 256
    private const val HALO = 32
    private const val CORE_SIZE = TILE_SIZE - HALO * 2
    private const val AI2_TARGET_BUDGET_MS = 20_000L
    private const val AI2_MAX_BUDGET_MS = 30_000L
    private const val AI15_TARGET_BUDGET_MS = 10_000L
    private const val AI15_MAX_BUDGET_MS = 20_000L
    private const val MODEL_MIN_BYTES = 1_000_000L
    private const val MAX_OUTPUT_HEAP_FRACTION = 0.22

    private class UserRequestedFastException : RuntimeException(
        "User requested a safe switch to HDMP AI 1.5"
    )

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
        return min(requested.coerceIn(1, 8), min(cpuLimit, min(memoryLimit, thermalLimit)))
            .coerceAtLeast(1)
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

    fun process(
        context: Context,
        uri: Uri,
        workerCount: Int,
        preferFast: Boolean = false,
        onAdaptiveWorkerCount: (Int) -> Unit = {},
        shouldSwitchToFast: () -> Boolean = { false },
        continuePrimaryPastBudget: () -> Boolean = { false },
        shouldStopForThermal: () -> Boolean = { false },
        onTileProgress: (Model, Int, Int) -> Unit = { _, _, _ -> },
        onProgress: (Model, Int) -> Unit = { _, _ -> }
    ): Result {
        val stamp = System.nanoTime().toString()
        val original = File(context.cacheDir, "hdmp-ai-original-$stamp.jpg")
        val rendered = File(context.cacheDir, "hdmp-ai-rendered-$stamp.jpg")
        return try {
            copyUriToFile(context, uri, original)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(original.absolutePath, bounds)
            val width = bounds.outWidth
            val height = bounds.outHeight
            require(width >= 3 && height >= 3) { "HDMP AI source dimensions are invalid" }

            val outputBytes = width.toLong() * height.toLong() * 2L
            val runtime = Runtime.getRuntime()
            val safeHeap = (runtime.maxMemory().toDouble() * MAX_OUTPUT_HEAP_FRACTION).toLong()
            if (outputBytes > safeHeap) {
                return Result(
                    Model.CLASSICAL_ONLY,
                    false,
                    "Ảnh vượt giới hạn bộ nhớ an toàn của AI; HDMP cơ bản sẽ xử lý ảnh gốc."
                )
            }

            val effectiveWorkerCount = resolveWorkerCount(context, workerCount)
            runCatching { onAdaptiveWorkerCount(effectiveWorkerCount) }
            Log.i(TAG, "Adaptive ONNX worker count: $effectiveWorkerCount / requested $workerCount")

            val tileCount = ceil(width.toDouble() / CORE_SIZE).toLong() *
                ceil(height.toDouble() / CORE_SIZE).toLong()
            val primaryBudget = (AI2_TARGET_BUDGET_MS + tileCount * 45L)
                .coerceAtMost(AI2_MAX_BUDGET_MS)
            val fallbackBudget = (AI15_TARGET_BUDGET_MS + tileCount * 22L)
                .coerceAtMost(AI15_MAX_BUDGET_MS)

            if (!preferFast) runCatching { onProgress(Model.NAFNET_WIDTH64, 0) }
            val primary = if (preferFast) false else try {
                runModel(
                    context = context,
                    source = original,
                    destination = rendered,
                    spec = primarySpec,
                    workerCount = effectiveWorkerCount,
                    budgetMs = primaryBudget,
                    model = Model.NAFNET_WIDTH64,
                    shouldSwitchToFast = shouldSwitchToFast,
                    continuePastBudget = continuePrimaryPastBudget,
                    shouldStopForThermal = shouldStopForThermal,
                    onTileProgress = onTileProgress,
                    onProgress = onProgress
                )
            } catch (_: UserRequestedFastException) {
                false
            }
            val userRequestedFast = preferFast || shouldSwitchToFast()
            if (shouldStopForThermal() && primary && commitRendered(context, uri, original, rendered)) {
                preserveOriginalForComparison(context, uri, original)
                return Result(Model.NAFNET_WIDTH64, true, "Đã dừng theo nhiệt độ; đã lưu phần HDMP AI 2 hoàn tất, phần còn lại giữ ảnh gốc.")
            }
            if (!userRequestedFast && primary && commitRendered(context, uri, original, rendered)) {
                preserveOriginalForComparison(context, uri, original)
                return Result(Model.NAFNET_WIDTH64, true, "HDMP AI 2 đã xử lý và được HDMP kiểm tra.")
            }
            // Respect the model chosen before capture. An AI 2 timeout/failure does not silently
            // replace the selected model with AI 1.5 after the shutter has been pressed.
            rendered.delete()
            if (shouldStopForThermal()) {
                return Result(Model.CLASSICAL_ONLY, false, "Đã dừng theo nhiệt độ trước khi hoàn tất tile đầu tiên; giữ nguyên ảnh gốc.")
            }
            if (!userRequestedFast) {
                return Result(
                    Model.CLASSICAL_ONLY,
                    false,
                    "HDMP AI 2 chưa hoàn tất trong giới hạn 30 giây; giữ ảnh gốc để HDMP cơ bản xử lý."
                )
            }

            runCatching { onProgress(Model.NAFNETLIB_WIDTH32, 0) }
            val fallback = runModel(
                context = context,
                source = original,
                destination = rendered,
                spec = fallbackSpec,
                workerCount = effectiveWorkerCount,
                budgetMs = fallbackBudget,
                model = Model.NAFNETLIB_WIDTH32,
                shouldSwitchToFast = { false },
                continuePastBudget = { false },
                shouldStopForThermal = shouldStopForThermal,
                onTileProgress = onTileProgress,
                onProgress = onProgress
            )
            if (shouldStopForThermal() && fallback && commitRendered(context, uri, original, rendered)) {
                preserveOriginalForComparison(context, uri, original)
                return Result(Model.NAFNETLIB_WIDTH32, true, "Đã dừng theo nhiệt độ; đã lưu phần HDMP AI 1.5 hoàn tất, phần còn lại giữ ảnh gốc.")
            }
            if (fallback && commitRendered(context, uri, original, rendered)) {
                preserveOriginalForComparison(context, uri, original)
                return Result(
                    Model.NAFNETLIB_WIDTH32,
                    true,
                    if (userRequestedFast) {
                        "HDMP AI 1.5 được chọn và đã xử lý từ ảnh gốc; HDMP đã kiểm tra kết quả."
                    } else {
                        "HDMP AI 1.5 dự phòng đã xử lý và được HDMP kiểm tra."
                    }
                )
            }

            Result(
                Model.CLASSICAL_ONLY,
                false,
                "Hai model AI không khả dụng hoặc không hoàn tất; giữ ảnh gốc để HDMP cơ bản xử lý."
            )
        } catch (failure: Throwable) {
            Log.e(TAG, "HDMP AI coordinator failed; deterministic HDMP fallback will be used", failure)
            Result(Model.CLASSICAL_ONLY, false, "AI lỗi; chuyển an toàn sang HDMP cơ bản.")
        } finally {
            runCatching { original.delete() }
            runCatching { rendered.delete() }
        }
    }

    private fun runModel(
        context: Context,
        source: File,
        destination: File,
        spec: ModelSpec,
        workerCount: Int,
        budgetMs: Long,
        model: Model,
        shouldSwitchToFast: () -> Boolean,
        continuePastBudget: () -> Boolean,
        shouldStopForThermal: () -> Boolean,
        onTileProgress: (Model, Int, Int) -> Unit,
        onProgress: (Model, Int) -> Unit
    ): Boolean {
        var sourceStream: FileInputStream? = null
        var decoder: BitmapRegionDecoder? = null
        var outputBitmap: Bitmap? = null
        var session: OrtSession? = null
        var options: OrtSession.SessionOptions? = null
        return try {
            val modelFile = verifiedModelFile(context, spec) ?: return false
            val openedStream = FileInputStream(source)
            sourceStream = openedStream
            val openedDecoder = BitmapRegionDecoder.newInstance(openedStream, false) ?: return false
            decoder = openedDecoder

            val width = openedDecoder.width
            val height = openedDecoder.height
            val pixelCount = width.toLong() * height.toLong()
            val outputBytes = pixelCount * 2L
            val safeHeap = (Runtime.getRuntime().maxMemory().toDouble() * MAX_OUTPUT_HEAP_FRACTION).toLong()
            if (outputBytes > safeHeap) return false

            outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val env = OrtEnvironment.getEnvironment()
            options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(workerCount.coerceIn(1, 8))
                setInterOpNumThreads(1)
            }
            val openedSession = env.createSession(modelFile.absolutePath, options)
            session = openedSession
            val inputName = openedSession.inputNames.firstOrNull() ?: error("ONNX input missing")
            val outputName = openedSession.outputNames.firstOrNull() ?: error("ONNX output missing")

            val rows = ceil(height.toDouble() / CORE_SIZE).toInt()
            val columns = ceil(width.toDouble() / CORE_SIZE).toInt()
            val totalTiles = rows * columns
            val startedAt = SystemClock.elapsedRealtime()
            val patchPixels = IntArray(TILE_SIZE * TILE_SIZE)
            val inputBuffer = ByteBuffer.allocateDirect(TILE_SIZE * TILE_SIZE * 3 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            val outputPixels = IntArray(CORE_SIZE * CORE_SIZE)
            val decodeOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
                inDither = true
            }

            var tileIndex = 0
            var lastThermalSampleAt = 0L
            var thermalThrottleMs = 0L
            for (coreTop in 0 until height step CORE_SIZE) {
                for (coreLeft in 0 until width step CORE_SIZE) {
                    // Stop only between native inference tiles. Complete unprocessed areas
                    // from the original capture so a partial export never contains blank pixels.
                    if (shouldStopForThermal()) {
                        if (tileIndex == 0) return false
                        for (remainingTop in 0 until height step CORE_SIZE) {
                            for (remainingLeft in 0 until width step CORE_SIZE) {
                                val remainingIndex = (remainingTop / CORE_SIZE) * columns + (remainingLeft / CORE_SIZE)
                                if (remainingIndex < tileIndex) continue
                                val right = min(width, remainingLeft + CORE_SIZE)
                                val bottom = min(height, remainingTop + CORE_SIZE)
                                val originalTile = openedDecoder.decodeRegion(
                                    Rect(remainingLeft, remainingTop, right, bottom), decodeOptions
                                ) ?: error("Unable to restore untouched source tile")
                                try {
                                    val pixels = IntArray(originalTile.width * originalTile.height)
                                    originalTile.getPixels(pixels, 0, originalTile.width, 0, 0,
                                        originalTile.width, originalTile.height)
                                    outputBitmap!!.setPixels(pixels, 0, originalTile.width,
                                        remainingLeft, remainingTop, originalTile.width, originalTile.height)
                                } finally {
                                    originalTile.recycle()
                                }
                            }
                        }
                        FileOutputStream(destination).use { stream ->
                            check(outputBitmap!!.compress(Bitmap.CompressFormat.JPEG, 97, stream)) {
                                "HDMP AI partial output JPEG encoding failed"
                            }
                            stream.flush()
                            stream.fd.sync()
                        }
                        writeExif(destination, readExif(source))
                        runCatching { onTileProgress(model, tileIndex, totalTiles) }
                        runCatching { onProgress(model, (tileIndex * 100 / totalTiles).coerceIn(1, 99)) }
                        return true
                    }
                    // ONNX inference is native and cannot be safely interrupted mid-tile.
                    // Check the soft deadline between tiles, then restart fallback from the
                    // untouched original file if the primary model exceeded its budget.
                    if (model == Model.NAFNET_WIDTH64 && shouldSwitchToFast()) {
                        throw UserRequestedFastException()
                    }
                    if (SystemClock.elapsedRealtime() - startedAt > budgetMs && !continuePastBudget()) {
                        throw ModelTimeoutException(
                            if (model == Model.NAFNET_WIDTH64) {
                                "HDMP AI 2 exceeded its 30-second processing limit"
                            } else {
                                "HDMP AI 1.5 exceeded its 20-second processing limit"
                            }
                        )
                    }

                    val patchLeft = coreLeft - HALO
                    val patchTop = coreTop - HALO
                    val regionLeft = patchLeft.coerceAtLeast(0)
                    val regionTop = patchTop.coerceAtLeast(0)
                    val regionRight = min(width, patchLeft + TILE_SIZE)
                    val regionBottom = min(height, patchTop + TILE_SIZE)
                    val region = Rect(regionLeft, regionTop, regionRight, regionBottom)
                    val decoded = openedDecoder.decodeRegion(region, decodeOptions)
                        ?: error("Unable to decode AI tile")
                    try {
                        val regionPixels = IntArray(decoded.width * decoded.height)
                        decoded.getPixels(regionPixels, 0, decoded.width, 0, 0, decoded.width, decoded.height)
                        for (py in 0 until TILE_SIZE) {
                            val gy = (patchTop + py).coerceIn(0, height - 1)
                            val sourceY = (gy - regionTop).coerceIn(0, decoded.height - 1)
                            for (px in 0 until TILE_SIZE) {
                                val gx = (patchLeft + px).coerceIn(0, width - 1)
                                val sourceX = (gx - regionLeft).coerceIn(0, decoded.width - 1)
                                patchPixels[py * TILE_SIZE + px] =
                                    regionPixels[sourceY * decoded.width + sourceX]
                            }
                        }
                    } finally {
                        decoded.recycle()
                    }

                    inputBuffer.clear()
                    val plane = TILE_SIZE * TILE_SIZE
                    for (index in patchPixels.indices) {
                        inputBuffer.put(Color.blue(patchPixels[index]) / 255f)
                    }
                    for (index in patchPixels.indices) {
                        inputBuffer.put(Color.green(patchPixels[index]) / 255f)
                    }
                    for (index in patchPixels.indices) {
                        inputBuffer.put(Color.red(patchPixels[index]) / 255f)
                    }
                    inputBuffer.flip()

                    val inputTensor = OnnxTensor.createTensor(
                        env,
                        inputBuffer,
                        longArrayOf(1L, 3L, TILE_SIZE.toLong(), TILE_SIZE.toLong())
                    )
                    try {
                        openedSession.run(mapOf(inputName to inputTensor)).use { result ->
                            val outputTensor = result.get(outputName).orElseThrow {
                                IllegalStateException("ONNX output tensor missing")
                            } as? OnnxTensor ?: error("ONNX output is not a tensor")
                            val tensorInfo = outputTensor.info as? TensorInfo
                                ?: error("ONNX output metadata is invalid")
                            check(tensorInfo.shape.contentEquals(
                                longArrayOf(1L, 3L, TILE_SIZE.toLong(), TILE_SIZE.toLong())
                            )) { "Unexpected NAFNet output tensor shape" }

                            val values = outputTensor.floatBuffer
                            check(values.remaining() == plane * 3) { "Unexpected NAFNet output buffer size" }
                            validateTensor(values, patchPixels, spec.deltaLimit)
                            for (coreY in coreTop until min(height, coreTop + CORE_SIZE)) {
                                for (coreX in coreLeft until min(width, coreLeft + CORE_SIZE)) {
                                    val patchX = coreX - patchLeft
                                    val patchY = coreY - patchTop
                                    val index = patchY * TILE_SIZE + patchX
                                    val base = patchPixels[index]
                                    val outB = values.get(index)
                                    val outG = values.get(plane + index)
                                    val outR = values.get(plane * 2 + index)
                                    outputPixels[(coreY - coreTop) * CORE_SIZE + (coreX - coreLeft)] =
                                        blendConservatively(base, outR, outG, outB, spec)
                                }
                            }
                            val tileWidth = min(width, coreLeft + CORE_SIZE) - coreLeft
                            val tileHeight = min(height, coreTop + CORE_SIZE) - coreTop
                            outputBitmap!!.setPixels(
                                outputPixels,
                                0,
                                CORE_SIZE,
                                coreLeft,
                                coreTop,
                                tileWidth,
                                tileHeight
                            )
                        }
                    } finally {
                        inputTensor.close()
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
                    if (thermalThrottleMs > 0L) SystemClock.sleep(thermalThrottleMs)
                }
            }

            FileOutputStream(destination).use { stream ->
                check(outputBitmap!!.compress(Bitmap.CompressFormat.JPEG, 97, stream)) {
                    "HDMP AI output JPEG encoding failed"
                }
                stream.flush()
                stream.fd.sync()
            }
            check(destination.exists() && destination.length() > 0L) { "HDMP AI output is empty" }
            val checkBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destination.absolutePath, checkBounds)
            check(checkBounds.outWidth == width && checkBounds.outHeight == height) {
                "HDMP AI output dimensions changed"
            }
            writeExif(destination, readExif(source))
            Log.i(TAG, "${model} completed ${width} x ${height} in ${SystemClock.elapsedRealtime() - startedAt}ms")
            true
        } catch (switch: UserRequestedFastException) {
            throw switch
        } catch (timeout: ModelTimeoutException) {
            Log.w(TAG, "${model} timed out; trying controlled fallback", timeout)
            false
        } catch (failure: Throwable) {
            Log.w(TAG, "${model} unavailable or failed; trying controlled fallback", failure)
            false
        } finally {
            runCatching { session?.close() }
            runCatching { options?.close() }
            runCatching { decoder?.recycle() }
            runCatching { sourceStream?.close() }
            outputBitmap?.let { if (!it.isRecycled) it.recycle() }
        }
    }

    private fun validateTensor(values: FloatBuffer, source: IntArray, deltaLimit: Float) {
        val plane = TILE_SIZE * TILE_SIZE
        var absSum = 0.0
        var count = 0
        for (index in 0 until plane) {
            val b = values.get(index)
            val g = values.get(plane + index)
            val r = values.get(plane * 2 + index)
            check(b.isFinite() && g.isFinite() && r.isFinite()) { "AI produced NaN/Infinity" }
            val original = source[index]
            val delta = max(
                kotlin.math.abs(b - Color.blue(original) / 255f),
                max(
                    kotlin.math.abs(g - Color.green(original) / 255f),
                    kotlin.math.abs(r - Color.red(original) / 255f)
                )
            )
            absSum += delta.toDouble()
            count++
        }
        val meanDelta = absSum / count.coerceAtLeast(1)
        check(meanDelta <= deltaLimit * 2.5) { "AI output deviates too far from the source image" }
    }

    private fun blendConservatively(
        original: Int,
        predictedR: Float,
        predictedG: Float,
        predictedB: Float,
        spec: ModelSpec
    ): Int {
        fun channel(source: Int, predicted: Float): Int {
            val originalValue = source / 255f
            val adjustment = ((predicted - originalValue) * spec.blend)
                .coerceIn(-spec.deltaLimit, spec.deltaLimit)
            return ((originalValue + adjustment).coerceIn(0f, 1f) * 255f).roundToInt()
                .coerceIn(0, 255)
        }
        return Color.rgb(
            channel(Color.red(original), predictedR),
            channel(Color.green(original), predictedG),
            channel(Color.blue(original), predictedB)
        )
    }

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
        if (destination.exists() && destination.length() >= MODEL_MIN_BYTES &&
            sha256(destination) == expectedHash
        ) return destination

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
            destination.takeIf { it.exists() && it.length() >= MODEL_MIN_BYTES }
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
     */
    private fun preserveOriginalForComparison(context: Context, uri: Uri, original: File) {
        runCatching {
            val directory = File(context.filesDir, "hdmp-comparison")
            check(directory.exists() || directory.mkdirs()) { "Could not create HDMP comparison directory" }
            val destination = File(directory, "latest-original.jpg")
            val temporary = File(directory, "latest-original.partial")
            original.copyTo(temporary, overwrite = true)
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
                inPreferredConfig = Bitmap.Config.RGB_565
                inDither = true
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
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
                ExifInterface(descriptor.fileDescriptor).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                    saveAttributes()
                }
            }
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
                output.fd.sync()
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

    private class ModelTimeoutException(message: String) : Exception(message)
}

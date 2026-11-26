package com.example.camui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.random.Random

internal data class LunaDreamSelfTestReport(
    val success: Boolean,
    val outputWidth: Int,
    val outputHeight: Int,
    val outputMegapixels: Double,
    val stageTimesMs: Map<String, Long>,
    val peakHeapBytes: Long,
    val message: String
) {
    fun toText(): String = buildString {
        appendLine("FujiFilm Mode — LunaDream 3.0 Self-test")
        appendLine("Kết quả: ${if (success) "PASS" else "FAIL"}")
        appendLine("Thời điểm: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(java.util.Date())}")
        appendLine("Đầu ra: ${outputWidth}×${outputHeight} · ${String.format(Locale.US, "%.2f", outputMegapixels)} MP")
        appendLine("Peak heap: ${peakHeapBytes / (1024L * 1024L)} MB")
        appendLine("Thời gian từng bước:")
        stageTimesMs.forEach { (stage, duration) -> appendLine("  $stage: ${duration} ms") }
        appendLine()
        appendLine(message)
        appendLine()
        appendLine("Bài kiểm tra tạo 8 khung JPEG tổng hợp có dịch chuyển và nhiễu, chạy merge LunaDream 3.0, upscale và xác minh JPEG đã ghi.")
        appendLine("Bài kiểm tra này không thay thế việc kiểm tra chất lượng ảnh thực tế trên thiết bị.")
    }
}

internal object LunaDreamSelfTest {
    private const val TAG = "FUJI_LUNADREAM_SELFTEST"
    private const val FRAME_WIDTH = 640
    private const val FRAME_HEIGHT = 480
    private const val FRAME_COUNT = 8
    private const val TEST_OUTPUT_PIXELS = 50_000_000L

    fun run(context: Context): LunaDreamSelfTestReport {
        val startedAt = SystemClock.elapsedRealtime()
        val stages = linkedMapOf<String, Long>()
        val runtime = Runtime.getRuntime()
        var peakHeap = usedHeap(runtime)
        var width = 0
        var height = 0
        var megapixels = 0.0
        var message = "Self-test chưa hoàn tất."
        var success = false
        val directory = File(context.cacheDir, "lunadream_self_test")
        val frames = mutableListOf<File>()
        val output = File(directory, "lunadream-30-self-test-output.jpg")
        var outputUri: Uri? = null

        try {
            check(directory.exists() || directory.mkdirs()) { "Không tạo được thư mục cache self-test." }
            val generationStart = SystemClock.elapsedRealtime()
            repeat(FRAME_COUNT) { index ->
                val frame = File(directory, "frame-$index.jpg")
                createShiftedNoisyFrame(frame, index)
                frames += frame
            }
            stages["Tạo 8 khung"] = SystemClock.elapsedRealtime() - generationStart
            peakHeap = maxOf(peakHeap, usedHeap(runtime))

            check(output.exists() || output.createNewFile()) { "Không tạo được file JPEG tạm." }
            outputUri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                output
            )

            var mergeFailure = ""
            val mergeStart = SystemClock.elapsedRealtime()
            val merged = NightShotProcessor.mergeFramesIntoUri(
                context = context,
                frames = frames,
                destinationUri = outputUri,
                mode = NightShotProcessor.MergeMode.LUNA_DREAM_3_0,
                onFailure = { reason, failure ->
                    mergeFailure = reason + (failure?.let { ": ${it.message}" } ?: "")
                }
            )
            stages["Ghép LunaDream 3.0"] = SystemClock.elapsedRealtime() - mergeStart
            peakHeap = maxOf(peakHeap, usedHeap(runtime))
            check(merged) { "Ghép thất bại: ${mergeFailure.ifBlank { "không có thông tin lỗi" }}" }

            val upscaleStart = SystemClock.elapsedRealtime()
            val resized = PhotoOutputResolution.interpolateToMp(
                context = context,
                uri = outputUri,
                targetPixels = TEST_OUTPUT_PIXELS,
                targetHeightOverWidth = null,
                adaptiveMinimumPixels = TEST_OUTPUT_PIXELS
            ) ?: error("Upscale/encode 50MP thất bại hoặc không đủ bộ nhớ an toàn.")
            stages["Upscale + mã hóa JPEG"] = SystemClock.elapsedRealtime() - upscaleStart
            peakHeap = maxOf(peakHeap, usedHeap(runtime))

            val verifyStart = SystemClock.elapsedRealtime()
            val dimensions = CaptureDiagnostics.readImageDimensions(context, outputUri)
            width = dimensions.first
            height = dimensions.second
            check(width > 0 && height > 0) { "Không đọc lại được kích thước JPEG đầu ra." }
            val actualPixels = width.toLong() * height.toLong()
            megapixels = actualPixels / 1_000_000.0
            check(actualPixels >= TEST_OUTPUT_PIXELS * 995L / 1000L) {
                "Kích thước đầu ra thấp hơn ngưỡng self-test 50MP: ${width}x$height."
            }
            check(resized.width == width && resized.height == height) {
                "Kích thước PhotoOutputResolution trả về không khớp file JPEG."
            }
            stages["Đọc lại và xác minh"] = SystemClock.elapsedRealtime() - verifyStart
            peakHeap = maxOf(peakHeap, usedHeap(runtime))
            success = true
            message = "PASS: merge, upscale và JPEG encode thực tế đã hoàn tất; file cache đã được đọc lại và xác minh."
            LunaDreamDiagnostics.record(
                context,
                "SELF_TEST_SUCCESS",
                "frames=$FRAME_COUNT; output=${width}x$height; mp=${String.format(Locale.US, "%.2f", megapixels)}; stages=$stages; peakHeap=$peakHeap"
            )
            CrashContextDiagnostics.recordUiEvent("LUNADREAM_30_SELF_TEST_PASS output=${width}x$height stages=$stages")
        } catch (failure: Throwable) {
            message = "FAIL: ${failure.javaClass.simpleName}: ${failure.message ?: "không có chi tiết"}"
            Log.e(TAG, message, failure)
            LunaDreamDiagnostics.record(
                context,
                "SELF_TEST_FAILED",
                "stages=$stages; peakHeap=$peakHeap; reason=$message",
                failure
            )
            CrashContextDiagnostics.recordUiEvent("LUNADREAM_30_SELF_TEST_FAIL stages=$stages reason=$message")
            CaptureDiagnostics.recordWarning(context, "LunaDream 3.0 self-test: $message")
        } finally {
            frames.forEach { runCatching { it.delete() } }
            runCatching { output.delete() }
            runCatching { directory.delete() }
        }

        stages["Tổng thời gian"] = SystemClock.elapsedRealtime() - startedAt
        return LunaDreamSelfTestReport(
            success = success,
            outputWidth = width,
            outputHeight = height,
            outputMegapixels = megapixels,
            stageTimesMs = stages.toMap(),
            peakHeapBytes = peakHeap,
            message = message
        )
    }

    fun share(context: Context, report: LunaDreamSelfTestReport) {
        runCatching {
            val directory = File(context.cacheDir, "shared_diagnostics").apply {
                if (!exists()) check(mkdirs()) { "Không tạo được thư mục chia sẻ báo cáo." }
            }
            val file = File(directory, "lunadream-30-self-test.txt")
            file.writeText(report.toText(), Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "FujiFilm Mode — LunaDream 3.0 Self-test")
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newUri(context.contentResolver, file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Chia sẻ kết quả self-test"))
        }.onFailure {
            Log.e(TAG, "Could not share self-test report", it)
            android.widget.Toast.makeText(context, "Không thể chia sẻ báo cáo: ${it.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun createShiftedNoisyFrame(file: File, frameIndex: Int) {
        val random = Random(0x4F554A + frameIndex * 7919)
        val shiftX = (frameIndex % 5) - 2
        val shiftY = ((frameIndex * 3) % 5) - 2
        val pixels = IntArray(FRAME_WIDTH * FRAME_HEIGHT)
        for (y in 0 until FRAME_HEIGHT) {
            for (x in 0 until FRAME_WIDTH) {
                val sourceX = (x - shiftX).coerceIn(0, FRAME_WIDTH - 1)
                val sourceY = (y - shiftY).coerceIn(0, FRAME_HEIGHT - 1)
                val checker = if (((sourceX / 32) + (sourceY / 32)) % 2 == 0) 24 else -18
                val diagonal = ((sourceX * 3 + sourceY * 2) % 96) - 48
                val noise = random.nextInt(-10, 11)
                val red = (92 + checker + diagonal / 3 + noise).coerceIn(0, 255)
                val green = (118 + checker + diagonal / 4 + noise).coerceIn(0, 255)
                val blue = (142 + checker - diagonal / 5 + noise).coerceIn(0, 255)
                pixels[y * FRAME_WIDTH + x] = Color.rgb(red, green, blue)
            }
        }
        val bitmap = Bitmap.createBitmap(FRAME_WIDTH, FRAME_HEIGHT, Bitmap.Config.ARGB_8888)
        try {
            bitmap.setPixels(pixels, 0, FRAME_WIDTH, 0, 0, FRAME_WIDTH, FRAME_HEIGHT)
            // JPEG_INTERMEDIATE_ALLOWLIST: synthetic self-test input frame stored only in app cache.
            FileOutputStream(file).use { stream ->
                // JPEG_INTERMEDIATE_ALLOWLIST: synthetic self-test input frame encode; never published to the photo library.
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)) {
                    "Không mã hóa được khung self-test $frameIndex."
                }
                stream.flush()
                stream.fd.sync()
            }
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun usedHeap(runtime: Runtime): Long =
        (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)
}

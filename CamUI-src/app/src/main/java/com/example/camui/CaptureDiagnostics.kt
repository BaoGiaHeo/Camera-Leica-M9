package com.example.camui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

internal data class CaptureDiagnosticEntry(
    val timestamp: Long,
    val mode: String,
    val filter: String,
    val watermarkEnabled: Boolean,
    val watermarkPrinted: Boolean,
    val outputWidth: Int,
    val outputHeight: Int,
    val stageTimesMs: Map<String, Long>,
    val peakMemoryBytes: Long,
    val errorMessage: String? = null,
    val captureToThumbnailMs: Long? = null,
    val burstFramesPerSecond: Double? = null,
    val aeLocked: Boolean? = null,
    val afLocked: Boolean? = null
) {
    val megapixels: Double
        get() = outputWidth.toLong().coerceAtLeast(0L)
            .times(outputHeight.toLong().coerceAtLeast(0L)) / 1_000_000.0
}

internal object CaptureDiagnostics {
    private const val PREFS = "fujifilm_settings"
    private const val KEY = "capture_diagnostics_v1"
    private const val WARNING_KEY = "capture_diagnostics_warnings_v1"
    private const val TAG = "FUJI_CAPTURE_DIAGNOSTICS"
    private val lock = Any()
    private val peakHeap = AtomicLong(0L)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun resetPeakMemory() {
        peakHeap.set(currentHeapBytes())
    }

    fun samplePeakMemory(): Long {
        val current = currentHeapBytes()
        peakHeap.accumulateAndGet(current) { previous, next -> maxOf(previous, next) }
        return peakHeap.get()
    }

    fun peakMemoryBytes(): Long = peakHeap.get()

    private fun currentHeapBytes(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)
    }

    fun record(context: Context, entry: CaptureDiagnosticEntry) {
        runCatching {
            synchronized(lock) {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val old = runCatching {
                    JSONArray(prefs.getString(KEY, "[]") ?: "[]")
                }.getOrDefault(JSONArray())
                val next = JSONArray()
                val start = (old.length() - (Tuning.CAPTURE_DIAGNOSTICS_LIMIT - 1)).coerceAtLeast(0)
                for (index in start until old.length()) {
                    next.put(old.optJSONObject(index) ?: JSONObject())
                }
                val stages = JSONObject()
                entry.stageTimesMs.forEach { (name, duration) -> stages.put(name, duration) }
                next.put(JSONObject().apply {
                    put("timestamp", entry.timestamp)
                    put("mode", entry.mode)
                    put("filter", entry.filter)
                    put("watermarkEnabled", entry.watermarkEnabled)
                    put("watermarkPrinted", entry.watermarkPrinted)
                    put("width", entry.outputWidth)
                    put("height", entry.outputHeight)
                    put("stages", stages)
                    put("peakMemoryBytes", entry.peakMemoryBytes)
                    put("errorMessage", entry.errorMessage)
                    entry.captureToThumbnailMs?.let { put("captureToThumbnailMs", it) }
                    entry.burstFramesPerSecond?.let { put("burstFramesPerSecond", it) }
                    entry.aeLocked?.let { put("aeLocked", it) }
                    entry.afLocked?.let { put("afLocked", it) }
                })
                prefs.edit().putString(KEY, next.toString()).apply()
            }
        }.onFailure { android.util.Log.e(TAG, "Could not store capture diagnostic entry", it) }
    }

    fun read(context: Context): List<CaptureDiagnosticEntry> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val stagesJson = item.optJSONObject("stages") ?: JSONObject()
            val stages = buildMap {
                val keys = stagesJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, stagesJson.optLong(key, -1L))
                }
            }
            CaptureDiagnosticEntry(
                timestamp = item.optLong("timestamp"),
                mode = item.optString("mode", "Không rõ"),
                filter = item.optString("filter", "Không"),
                watermarkEnabled = item.optBoolean("watermarkEnabled"),
                watermarkPrinted = item.optBoolean("watermarkPrinted"),
                outputWidth = item.optInt("width"),
                outputHeight = item.optInt("height"),
                stageTimesMs = stages,
                peakMemoryBytes = item.optLong("peakMemoryBytes"),
                errorMessage = item.optString("errorMessage").takeIf { it.isNotBlank() && it != "null" },
                captureToThumbnailMs = item.optLong("captureToThumbnailMs", -1L).takeIf { it >= 0L },
                burstFramesPerSecond = if (item.has("burstFramesPerSecond")) item.optDouble("burstFramesPerSecond").takeIf { it.isFinite() } else null,
                aeLocked = if (item.has("aeLocked")) item.optBoolean("aeLocked") else null,
                afLocked = if (item.has("afLocked")) item.optBoolean("afLocked") else null
            )
        }.takeLast(Tuning.CAPTURE_DIAGNOSTICS_LIMIT).reversed()
    }.onFailure { android.util.Log.e(TAG, "Could not read capture diagnostic entries", it) }
        .getOrDefault(emptyList())

    /**
     * Adds timings discovered after a JPEG is saved (for example loadThumbnail completion)
     * to every capture row from the same shutter timestamp. Burst frames intentionally share
     * one capture timestamp, so one measured session metric is attached to all its frame rows.
     */
    fun updateCaptureMetrics(
        context: Context,
        timestamp: Long,
        captureToThumbnailMs: Long? = null,
        burstFramesPerSecond: Double? = null,
        aeLocked: Boolean? = null,
        afLocked: Boolean? = null
    ) {
        if (captureToThumbnailMs == null && burstFramesPerSecond == null &&
            aeLocked == null && afLocked == null
        ) return
        runCatching {
            synchronized(lock) {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val current = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
                var changed = false
                for (index in 0 until current.length()) {
                    val item = current.optJSONObject(index) ?: continue
                    if (item.optLong("timestamp", Long.MIN_VALUE) != timestamp) continue
                    captureToThumbnailMs?.let { item.put("captureToThumbnailMs", it) }
                    burstFramesPerSecond?.let { item.put("burstFramesPerSecond", it) }
                    aeLocked?.let { item.put("aeLocked", it) }
                    afLocked?.let { item.put("afLocked", it) }
                    changed = true
                }
                if (changed) prefs.edit().putString(KEY, current.toString()).apply()
            }
        }.onFailure { android.util.Log.e(TAG, "Could not update capture metrics", it) }
    }

    fun recordWarning(context: Context, warning: String) {
        val safe = warning.replace('\n', ' ').take(300)
        CrashContextDiagnostics.recordUiEvent("RED_WARNING $safe")
        runCatching {
            synchronized(lock) {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val old = runCatching { JSONArray(prefs.getString(WARNING_KEY, "[]") ?: "[]") }
                    .getOrDefault(JSONArray())
                val next = JSONArray()
                val start = (old.length() - 29).coerceAtLeast(0)
                for (index in start until old.length()) next.put(old.optString(index))
                next.put(dateFormat.format(Date()) + " RED: " + safe)
                prefs.edit().putString(WARNING_KEY, next.toString()).apply()
            }
        }.onFailure { android.util.Log.e(TAG, "Could not store watermark warning", it) }
    }

    fun readWarnings(context: Context): List<String> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(WARNING_KEY, "[]") ?: "[]"
        val array = JSONArray(raw)
        (0 until array.length()).map { array.optString(it) }.takeLast(30).reversed()
    }.getOrDefault(emptyList())

    fun exportText(context: Context): String {
        val entries = read(context)
        val warnings = readWarnings(context)
        return buildString {
            appendLine("FujiFilm Mode — Nhật ký chẩn đoán")
            appendLine("Thiết bị: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Phiên bản: ${BuildConfig.VERSION_NAME}")
            appendLine("Số lần chụp: ${entries.size}")
            appendLine(UiRecompositionCounters.snapshot())
            appendLine("Cảnh báo gần đây: ${warnings.size}")
            warnings.forEach { appendLine("! $it") }
            appendLine()
            appendLine(CrashContextDiagnostics.snapshot())
            appendLine()
            entries.forEachIndexed { index, entry ->
                appendLine("#${index + 1} ${dateFormat.format(Date(entry.timestamp))}")
                appendLine("Chế độ: ${entry.mode} | Bộ lọc: ${entry.filter}")
                appendLine(
                    "WM: " + when {
                        entry.watermarkEnabled && entry.watermarkPrinted -> "ĐÃ IN"
                        !entry.watermarkEnabled -> "TẮT CÓ CHỦ Ý"
                        else -> "KHÔNG IN"
                    }
                )
                appendLine("Watermark: ${if (entry.watermarkEnabled) "BẬT" else "TẮT"}")
                appendLine(
                    "Bấm→thumbnail: ${entry.captureToThumbnailMs?.let { "${it}ms" } ?: "Chưa đo"} | " +
                        "Burst: ${entry.burstFramesPerSecond?.let { String.format(Locale.US, "%.2f khung/s", it) } ?: "—"} | " +
                        "AE-L: ${entry.aeLocked?.let { if (it) "BẬT" else "TẮT" } ?: "—"} | " +
                        "AF-L: ${entry.afLocked?.let { if (it) "BẬT" else "TẮT" } ?: "—"}"
                )
                appendLine(
                    "Đầu ra: ${entry.outputWidth}×${entry.outputHeight} · " +
                        String.format(Locale.US, "%.2f MP", entry.megapixels)
                )
                appendLine(
                    "Mẫu heap khi hoàn tất: ${entry.peakMemoryBytes / (1024L * 1024L)} MB"
                )
                appendLine("Thời gian giai đoạn (ms):")
                listOf("Chụp", "Ghép", "Nâng", "Bộ lọc", "Watermark", "Mã hóa", "Ghi file").forEach { stage ->
                    appendLine("  $stage: ${entry.stageTimesMs[stage]?.takeIf { it >= 0L }?.toString() ?: "Chưa đo"}")
                }
                appendLine()
            }
            if (entries.isEmpty()) appendLine("Chưa có bản ghi chụp.")
        }
    }

    fun share(context: Context) {
        runCatching {
            val directory = File(context.cacheDir, "shared_diagnostics").apply {
                if (!exists()) check(mkdirs()) { "Không tạo được thư mục nhật ký" }
            }
            val file = File(directory, "fujifilm-mode-diagnostics.txt")
            file.writeText(exportText(context), Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "FujiFilm Mode — Nhật ký chẩn đoán")
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newUri(context.contentResolver, file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Chia sẻ nhật ký"))
        }.onFailure {
            android.util.Log.e(TAG, "Could not share diagnostic report", it)
            Toast.makeText(context, "Không thể chia sẻ nhật ký: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun readImageDimensions(context: Context, uri: Uri): Pair<Int, Int> {
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input, null, options)
            }
        }.onFailure { android.util.Log.w(TAG, "Could not read output dimensions", it) }
        return options.outWidth.coerceAtLeast(0) to options.outHeight.coerceAtLeast(0)
    }
}

@Composable
internal fun CaptureDiagnosticsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val entries = remember { CaptureDiagnostics.read(context) }
    val warnings = remember { CaptureDiagnostics.readWarnings(context) }
    val uiCounterSummary = remember { UiRecompositionCounters.snapshot() }
    var selfTestRunning by remember { mutableStateOf(false) }
    var selfTestReport by remember { mutableStateOf<LunaDreamSelfTestReport?>(null) }
    val scope = rememberCoroutineScope()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier.fillMaxSize(0.92f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF111315))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.capture_diagnostics_title),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.common_close),
                    color = Color(0xFFF0A030),
                    modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp)
                )
            }
            Text(
                stringResource(R.string.diagnostics_ui_counters, uiCounterSummary),
                color = Color(0xFFBFC7C9),
                fontSize = 9.sp
            )
            if (warnings.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF351919))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        stringResource(R.string.diagnostics_recent_warnings),
                        color = Color(0xFFFF6B62),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    warnings.take(5).forEach { warning ->
                        Text(warning, color = Color(0xFFFFB3AC), fontSize = 9.sp)
                    }
                }
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(entries) { entry ->
                    Column(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF24282B))
                            .padding(9.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            dateFormatText(entry.timestamp) + " · " + entry.mode,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Lọc: ${entry.filter} · WM: " +
                                when {
                                    entry.watermarkEnabled && entry.watermarkPrinted -> "ĐÃ IN"
                                    !entry.watermarkEnabled -> "TẮT CÓ CHỦ Ý"
                                    else -> "KHÔNG IN"
                                },
                            color = Color(0xFFD0D0D0),
                            fontSize = 10.sp
                        )
                        entry.errorMessage?.let { error ->
                            Text(
                                "LỖI: $error",
                                color = Color(0xFFFF5B52),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "${entry.outputWidth}×${entry.outputHeight} · " +
                                String.format(Locale.US, "%.2f MP", entry.megapixels) +
                                " · RAM đỉnh ${entry.peakMemoryBytes / (1024L * 1024L)} MB",
                            color = Color(0xFFD0D0D0),
                            fontSize = 10.sp
                        )
                        Text(
                            "Bấm→thumbnail: ${entry.captureToThumbnailMs?.let { "${it}ms" } ?: "Chưa đo"} · " +
                                "Burst: ${entry.burstFramesPerSecond?.let { String.format(Locale.US, "%.2f khung/s", it) } ?: "—"}",
                            color = Color(0xFFB0B0B0),
                            fontSize = 9.sp
                        )
                        Text(
                            "AE-L: ${entry.aeLocked?.let { if (it) "BẬT" else "TẮT" } ?: "—"} · " +
                                "AF-L: ${entry.afLocked?.let { if (it) "BẬT" else "TẮT" } ?: "—"}",
                            color = Color(0xFFB0B0B0),
                            fontSize = 9.sp
                        )
                        Text(
                            entry.stageTimesMs.entries.joinToString(" · ") { (name, millis) ->
                                "$name: ${if (millis >= 0L) millis.toString() + " ms" else "Chưa đo"}"
                            },
                            color = Color(0xFFB0B0B0),
                            fontSize = 9.sp
                        )
                    }
                }
            }
            if (entries.isEmpty()) {
                Text("Chưa có bản ghi chụp.", color = Color(0xFFBDBDBD), fontSize = 12.sp)
            }
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF303A40))
                    .clickable(enabled = !selfTestRunning) {
                        if (!selfTestRunning) {
                            selfTestRunning = true
                            selfTestReport = null
                            scope.launch {
                                val report = withContext(Dispatchers.IO) {
                                    LunaDreamSelfTest.run(context)
                                }
                                selfTestReport = report
                                selfTestRunning = false
                            }
                        }
                    }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (selfTestRunning) "ĐANG KIỂM TRA LUNADREAM 3.0…" else "CHẠY SELF-TEST LUNADREAM 3.0",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            selfTestReport?.let { report ->
                Text(
                    (if (report.success) "SELF-TEST PASS" else "SELF-TEST FAIL") +
                        " · ${report.outputWidth}×${report.outputHeight} · " +
                        String.format(Locale.US, "%.2f MP", report.outputMegapixels) +
                        " · RAM ${report.peakHeapBytes / (1024L * 1024L)} MB\n" + report.message,
                    color = if (report.success) Color(0xFF9FE0A8) else Color(0xFFFF8D84),
                    fontSize = 9.sp
                )
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF46545B))
                        .clickable { LunaDreamSelfTest.share(context, report) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("CHIA SẺ BÁO CÁO SELF-TEST (.TXT)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF0A030))
                    .clickable { CaptureDiagnostics.share(context) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.capture_diagnostics_share),
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun dateFormatText(timestamp: Long): String =
    SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))

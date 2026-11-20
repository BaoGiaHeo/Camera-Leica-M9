package com.example.camui

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import java.io.Closeable
import java.util.ArrayDeque
import java.util.Locale
import kotlin.math.ceil

internal data class CameraFrameSample(
    val totalDurationNanos: Long,
    val drawDurationNanos: Long
)

internal data class CameraFrameMetricsSummary(
    val frameCount: Int,
    val jankyFrameCount: Int,
    val droppedCallbackReports: Int,
    val refreshRateHz: Float,
    val measuredFramesPerSecond: Float,
    val p95DrawMillis: Double,
    val p95TotalMillis: Double
) {
    val jankyPercent: Double
        get() = if (frameCount == 0) 0.0 else jankyFrameCount * 100.0 / frameCount
}

internal fun summarizeCameraFrameMetrics(
    samples: List<CameraFrameSample>,
    refreshRateHz: Float,
    windowDurationNanos: Long,
    droppedCallbackReports: Int = 0
): CameraFrameMetricsSummary? {
    if (samples.isEmpty()) return null
    val safeHz = refreshRateHz.takeIf { it.isFinite() && it > 0f } ?: 60f
    val frameBudgetNanos = (1_000_000_000.0 / safeHz).toLong()
    fun percentile95(values: List<Long>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val index = (ceil(sorted.size * 0.95).toInt() - 1).coerceIn(0, sorted.lastIndex)
        return sorted[index] / 1_000_000.0
    }
    val total = samples.map { it.totalDurationNanos.coerceAtLeast(0L) }
    val draw = samples.map { it.drawDurationNanos.coerceAtLeast(0L) }
    val janky = total.count { it > frameBudgetNanos }
    val fps = if (windowDurationNanos > 0L) {
        (samples.size * 1_000_000_000.0 / windowDurationNanos).toFloat()
    } else {
        0f
    }
    return CameraFrameMetricsSummary(
        frameCount = samples.size,
        jankyFrameCount = janky,
        droppedCallbackReports = droppedCallbackReports.coerceAtLeast(0),
        refreshRateHz = safeHz,
        measuredFramesPerSecond = fps,
        p95DrawMillis = percentile95(draw),
        p95TotalMillis = percentile95(total)
    )
}

/**
 * Developer-only rolling 5-second Window FrameMetrics sampler.
 * It writes Logcat diagnostics only and never changes rendering or capture behavior.
 */
internal class CameraFrameMetricsDiagnostics private constructor(
    private val window: Window
) : Closeable {
    private data class TimedSample(
        val timestampNanos: Long,
        val sample: CameraFrameSample
    )

    private val thread = HandlerThread("FujiFrameMetrics").apply { start() }
    private val handler = Handler(thread.looper)
    private val lock = Any()
    private val samples = ArrayDeque<TimedSample>()
    private var droppedReports = 0
    private var lastReportNanos = 0L
    @Volatile private var closed = false

    private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, dropped ->
        if (closed) return@OnFrameMetricsAvailableListener
        val now = SystemClock.elapsedRealtimeNanos()
        val refreshRate = runCatching {
            window.decorView.display?.refreshRate ?: 60f
        }.getOrDefault(60f)
        val sample = CameraFrameSample(
            totalDurationNanos = metrics.getMetric(FrameMetrics.TOTAL_DURATION),
            drawDurationNanos = metrics.getMetric(FrameMetrics.DRAW_DURATION)
        )
        var report: CameraFrameMetricsSummary? = null
        synchronized(lock) {
            val windowStart = now - WINDOW_NANOS
            while (samples.isNotEmpty() && samples.first().timestampNanos < windowStart) {
                samples.removeFirst()
            }
            samples.addLast(TimedSample(now, sample))
            droppedReports += dropped.coerceAtLeast(0)
            if (lastReportNanos == 0L) {
                lastReportNanos = now
            } else if (now - lastReportNanos >= WINDOW_NANOS) {
                val snapshot = samples.map { it.sample }
                val measuredWindow = (samples.last().timestampNanos - samples.first().timestampNanos)
                    .coerceAtLeast(1L)
                report = summarizeCameraFrameMetrics(
                    samples = snapshot,
                    refreshRateHz = refreshRate,
                    windowDurationNanos = measuredWindow,
                    droppedCallbackReports = droppedReports
                )
                droppedReports = 0
                lastReportNanos = now
            }
        }
        report?.let(::logSummary)
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            window.addOnFrameMetricsAvailableListener(listener, handler)
            Log.i(TAG, "FrameMetrics sampling enabled; report window=5s")
        }
    }

    private fun logSummary(summary: CameraFrameMetricsSummary) {
        Log.i(
            TAG,
            String.format(
                Locale.US,
                "window=5s refresh=%.0fHz fps=%.1f frames=%d janky=%d (%.2f%%) " +
                    "p95Draw=%.3fms p95Total=%.3fms droppedCallbacks=%d",
                summary.refreshRateHz,
                summary.measuredFramesPerSecond,
                summary.frameCount,
                summary.jankyFrameCount,
                summary.jankyPercent,
                summary.p95DrawMillis,
                summary.p95TotalMillis,
                summary.droppedCallbackReports
            )
        )
    }

    override fun close() {
        if (closed) return
        closed = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { window.removeOnFrameMetricsAvailableListener(listener) }
        }
        thread.quitSafely()
    }

    companion object {
        private const val TAG = "FUJI_FRAME_METRICS"
        private const val WINDOW_NANOS = 5_000_000_000L

        fun attach(window: Window): CameraFrameMetricsDiagnostics? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                runCatching { CameraFrameMetricsDiagnostics(window) }
                    .onFailure { Log.w(TAG, "Unable to attach FrameMetrics listener", it) }
                    .getOrNull()
            } else {
                Log.i(TAG, "FrameMetrics unavailable below Android 7.0")
                null
            }
    }
}

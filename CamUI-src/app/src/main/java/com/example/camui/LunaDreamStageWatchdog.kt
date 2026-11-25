package com.example.camui

import android.content.Context
import android.os.SystemClock
import android.util.Log

/**
 * Cooperative stage watchdog for the 3.0 merge path.
 *
 * It checks elapsed time at stage transitions and progress checkpoints. Blocking
 * BitmapFactory/Bitmap.compress calls cannot be safely interrupted from another
 * thread; their overrun is detected immediately after the call returns.
 */
internal class LunaDreamStageWatchdog(
    private val context: Context,
    private val mode: String,
    private val stageMultiplier: Long = Tuning.LUNA_DREAM_30_STAGE_WATCHDOG_FACTOR,
    private val totalBudgetMs: Long = Tuning.LUNA_DREAM_30_MERGE_WATCHDOG_MS,
    private val enabled: Boolean = true
) {
    private var stageName: String = "initializing merge"
    private var stageBudgetMs: Long = totalBudgetMs
    private var stageStartedAtMs: Long = SystemClock.elapsedRealtime()
    private val mergeStartedAtMs: Long = stageStartedAtMs
    private var lastRecordedStage: String? = null

    fun beginStage(name: String, budgetMs: Long) {
        checkpoint()
        if (stageName != "initializing merge") {
            val now = SystemClock.elapsedRealtime()
            recordStageComplete(now - stageStartedAtMs, now - mergeStartedAtMs)
        }
        stageName = name
        stageBudgetMs = budgetMs.coerceAtLeast(1L)
        stageStartedAtMs = SystemClock.elapsedRealtime()
        lastRecordedStage = null
        CrashContextDiagnostics.recordUiEvent(
            "LUNADREAM_STAGE_START mode=$mode stage=$stageName budgetMs=$stageBudgetMs"
        )
        Log.i(
            "FUJI_LUNADREAM_WATCHDOG",
            "STAGE_START mode=$mode stage=$stageName budgetMs=$stageBudgetMs multiplier=$stageMultiplier"
        )
        LunaDreamDiagnostics.record(
            context,
            "STAGE_START",
            "mode=$mode; stage=$stageName; budgetMs=$stageBudgetMs; watchdogFactor=$stageMultiplier"
        )
    }

    fun checkpoint() {
        if (!enabled) return
        val now = SystemClock.elapsedRealtime()
        val stageElapsed = now - stageStartedAtMs
        val totalElapsed = now - mergeStartedAtMs
        if (stageElapsed > stageBudgetMs * stageMultiplier) {
            val reason = "LunaDream 3.0 dừng ở bước $stageName: vượt ngân sách ${stageBudgetMs * stageMultiplier}ms (thực tế ${stageElapsed}ms)"
            recordOverrun(stageElapsed, totalElapsed, reason)
            throw IllegalStateException(reason)
        }
        if (totalElapsed > totalBudgetMs) {
            val reason = "LunaDream 3.0 dừng ở bước $stageName: toàn bộ ghép ảnh vượt ngân sách ${totalBudgetMs}ms (thực tế ${totalElapsed}ms)"
            recordOverrun(stageElapsed, totalElapsed, reason)
            throw IllegalStateException(reason)
        }
    }

    fun finish() {
        if (!enabled) return
        checkpoint()
        val now = SystemClock.elapsedRealtime()
        recordStageComplete(now - stageStartedAtMs, now - mergeStartedAtMs)
    }

    private fun recordOverrun(stageElapsed: Long, totalElapsed: Long, reason: String) {
        if (lastRecordedStage == stageName) return
        lastRecordedStage = stageName
        Log.e(
            "FUJI_LUNADREAM_WATCHDOG",
            "WATCHDOG_TIMEOUT mode=$mode stage=$stageName stageMs=$stageElapsed totalMs=$totalElapsed reason=$reason"
        )
        CrashContextDiagnostics.recordUiEvent(
            "LUNADREAM_WATCHDOG_TIMEOUT mode=$mode stage=$stageName stageMs=$stageElapsed totalMs=$totalElapsed"
        )
        LunaDreamDiagnostics.record(
            context,
            "WATCHDOG_TIMEOUT",
            "mode=$mode; stage=$stageName; stageMs=$stageElapsed; totalMs=$totalElapsed; reason=$reason"
        )
        CaptureDiagnostics.recordWarning(context, reason)
    }

    private fun recordStageComplete(stageElapsed: Long, totalElapsed: Long) {
        CrashContextDiagnostics.recordUiEvent(
            "LUNADREAM_STAGE_COMPLETE mode=$mode stage=$stageName elapsedMs=$stageElapsed totalMs=$totalElapsed"
        )
        LunaDreamDiagnostics.record(
            context,
            "STAGE_COMPLETE",
            "mode=$mode; stage=$stageName; elapsedMs=$stageElapsed; totalElapsedMs=$totalElapsed; budgetMs=$stageBudgetMs"
        )
    }
}

package com.example.camui

/**
 * Waits for an explicit response when HDMP AI 2 reaches its soft time budget.
 *
 * The dialog is presented by the camera UI while the primary model is running.
 * Waiting here prevents the coordinator from starting Fast in the small race
 * between crossing the budget and the user selecting "continue".
 */
internal object HdmpAiPrimaryDecisionGate {
    const val DEFAULT_WAIT_MS = 120_000L
    const val POLL_INTERVAL_MS = 100L

    enum class Outcome {
        CONTINUE_PRIMARY,
        SWITCH_TO_FAST,
        STOP_FOR_THERMAL,
        NO_RESPONSE
    }

    fun await(
        shouldContinuePrimary: () -> Boolean,
        shouldSwitchToFast: () -> Boolean,
        shouldStopForThermal: () -> Boolean,
        timeoutMs: Long = DEFAULT_WAIT_MS,
        pollIntervalMs: Long = POLL_INTERVAL_MS,
        nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
        wait: (Long) -> Unit = { Thread.sleep(it) }
    ): Outcome {
        require(timeoutMs >= 0L) { "timeoutMs must not be negative" }
        require(pollIntervalMs > 0L) { "pollIntervalMs must be positive" }

        val startedAt = nowMs()
        while (true) {
            // An explicit Fast request and thermal protection always take priority.
            if (shouldSwitchToFast()) return Outcome.SWITCH_TO_FAST
            if (shouldStopForThermal()) return Outcome.STOP_FOR_THERMAL
            if (shouldContinuePrimary()) return Outcome.CONTINUE_PRIMARY

            val elapsed = (nowMs() - startedAt).coerceAtLeast(0L)
            if (elapsed >= timeoutMs) return Outcome.NO_RESPONSE

            try {
                wait(minOf(pollIntervalMs, timeoutMs - elapsed))
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return Outcome.NO_RESPONSE
            }
        }
    }
}

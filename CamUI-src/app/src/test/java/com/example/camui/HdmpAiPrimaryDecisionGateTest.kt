package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class HdmpAiPrimaryDecisionGateTest {
    @Test
    fun continueChoiceWinsWhenUserRespondsDuringGracePeriod() {
        var elapsedMs = 0L
        val outcome = HdmpAiPrimaryDecisionGate.await(
            shouldContinuePrimary = { elapsedMs >= 300L },
            shouldSwitchToFast = { false },
            shouldStopForThermal = { false },
            timeoutMs = 2_000L,
            pollIntervalMs = 100L,
            nowMs = { elapsedMs },
            wait = { elapsedMs += it }
        )

        assertEquals(HdmpAiPrimaryDecisionGate.Outcome.CONTINUE_PRIMARY, outcome)
    }

    @Test
    fun explicitFastChoiceIsHonored() {
        var elapsedMs = 0L
        val outcome = HdmpAiPrimaryDecisionGate.await(
            shouldContinuePrimary = { false },
            shouldSwitchToFast = { elapsedMs >= 200L },
            shouldStopForThermal = { false },
            timeoutMs = 2_000L,
            pollIntervalMs = 100L,
            nowMs = { elapsedMs },
            wait = { elapsedMs += it }
        )

        assertEquals(HdmpAiPrimaryDecisionGate.Outcome.SWITCH_TO_FAST, outcome)
    }

    @Test
    fun thermalStopTakesPriorityOverContinue() {
        val outcome = HdmpAiPrimaryDecisionGate.await(
            shouldContinuePrimary = { true },
            shouldSwitchToFast = { false },
            shouldStopForThermal = { true },
            timeoutMs = 1_000L
        )

        assertEquals(HdmpAiPrimaryDecisionGate.Outcome.STOP_FOR_THERMAL, outcome)
    }

    @Test
    fun noResponseUsesSafeFallbackAfterGracePeriod() {
        var elapsedMs = 0L
        val outcome = HdmpAiPrimaryDecisionGate.await(
            shouldContinuePrimary = { false },
            shouldSwitchToFast = { false },
            shouldStopForThermal = { false },
            timeoutMs = 350L,
            pollIntervalMs = 100L,
            nowMs = { elapsedMs },
            wait = { elapsedMs += it }
        )

        assertEquals(HdmpAiPrimaryDecisionGate.Outcome.NO_RESPONSE, outcome)
        assertEquals(350L, elapsedMs)
    }
}

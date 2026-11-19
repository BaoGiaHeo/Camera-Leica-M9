package com.example.camui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TonePadPublishGateTest {
    @Test
    fun parentUpdatesAreLimitedToFourPerSecond() {
        val gate = TonePadPublishGate(250L)

        assertTrue(gate.shouldPublish(1_000L))
        assertFalse(gate.shouldPublish(1_001L))
        assertFalse(gate.shouldPublish(1_100L))
        assertFalse(gate.shouldPublish(1_249L))
        assertTrue(gate.shouldPublish(1_250L))
        assertFalse(gate.shouldPublish(1_499L))
        assertTrue(gate.shouldPublish(1_500L))
    }

    @Test
    fun aFreshGatePublishesImmediately() {
        val gate = TonePadPublishGate()

        assertTrue(gate.shouldPublish(10_000L))
    }

    @Test
    fun invalidIntervalsAreRejected() {
        val result = runCatching { TonePadPublishGate(0L) }

        assertTrue(result.isFailure)
    }
}

package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomSmoothingPolicyTest {
    @Test
    fun zoomMovesMonotonicallyTowardIncreasingTarget() {
        var current = 1f
        repeat(180) {
            val next = advanceZoomTowardTarget(current, 2f, 1f / 60f)
            assertTrue("zoom must not move backwards", next >= current)
            assertTrue("zoom must not overshoot", next <= 2f)
            current = next
        }
        assertTrue(isZoomTargetReached(current, 2f, 0.01f))
    }

    @Test
    fun rapidTargetChangesDoNotRestartOrReverseTheControllerState() {
        var current = 1f
        val targets = listOf(1.3f, 1.6f, 1.9f, 2.0f)
        var lastTarget = 1f
        targets.forEach { target ->
            repeat(8) {
                val next = advanceZoomTowardTarget(current, target, 1f / 120f)
                assertTrue(next >= current)
                assertTrue(next <= target)
                current = next
            }
            lastTarget = target
        }
        assertTrue(current > 1f)
        assertTrue(current < lastTarget)
        repeat(240) { current = advanceZoomTowardTarget(current, lastTarget, 1f / 120f) }
        assertTrue(isZoomTargetReached(current, lastTarget, 0.002f))
    }

    @Test
    fun invalidDeltaAndTargetAreHandledWithoutNanPropagation() {
        assertEquals(1f, advanceZoomTowardTarget(1f, 2f, Float.NaN), 0f)
        assertEquals(1f, advanceZoomTowardTarget(1f, Float.NaN, 1f / 60f), 0f)
    }
}

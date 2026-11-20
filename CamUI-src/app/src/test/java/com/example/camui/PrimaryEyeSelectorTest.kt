package com.example.camui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimaryEyeSelectorTest {
    @Test
    fun choosesOneEyeAndKeepsItWhenYawIsInsideDeadZone() {
        val selector = PrimaryEyeSelector()
        val left = Offset(0.42f, 0.48f)
        val right = Offset(0.58f, 0.48f)

        assertEquals(left, selector.select(left, right, 0f, 0L, trackingId = 12))
        assertEquals(PrimaryEyeSide.LEFT, selector.currentSide())
        assertEquals(left, selector.select(left, right, 3f, 700L, trackingId = 12))
    }

    @Test
    fun changesEyeOnlyAfterYawPreferenceIsStableForHalfASecond() {
        val selector = PrimaryEyeSelector()
        val left = Offset(0.42f, 0.48f)
        val right = Offset(0.58f, 0.48f)

        selector.select(left, right, 0f, 0L, trackingId = 3)
        assertEquals(left, selector.select(left, right, 18f, 1_000L, trackingId = 3))
        assertEquals(left, selector.select(left, right, 18f, 1_499L, trackingId = 3))
        assertEquals(right, selector.select(left, right, 18f, 1_500L, trackingId = 3))
        assertEquals(PrimaryEyeSide.RIGHT, selector.currentSide())
    }

    @Test
    fun doesNotJumpToOtherEyeUntilSelectedEyeHasBeenMissingForElevenFrames() {
        val selector = PrimaryEyeSelector(lostFrameThreshold = 11)
        val left = Offset(0.42f, 0.48f)
        val right = Offset(0.58f, 0.48f)

        selector.select(left, right, 0f, 0L, trackingId = 9)
        repeat(11) { index ->
            assertNull(selector.select(null, right, 0f, (index + 1) * 100L, trackingId = 9))
        }
        assertEquals(right, selector.select(null, right, 0f, 1_200L, trackingId = 9))
        assertEquals(PrimaryEyeSide.RIGHT, selector.currentSide())
    }

    @Test
    fun tappingTheOtherEyeSwitchesImmediatelyWithoutYawDelay() {
        val selector = PrimaryEyeSelector()
        val left = Offset(0.42f, 0.48f)
        val right = Offset(0.58f, 0.48f)

        selector.select(left, right, 0f, 0L, trackingId = 7)
        assertEquals(PrimaryEyeSide.LEFT, selector.currentSide())
        selector.forceSelect(PrimaryEyeSide.RIGHT, 100L, trackingId = 7)
        assertEquals(PrimaryEyeSide.RIGHT, selector.currentSide())
        assertEquals(right, selector.select(left, right, 0f, 110L, trackingId = 7))
    }

    @Test
    fun trackingIdChangeResetsEyeIdentityForTheNewFace() {
        val selector = PrimaryEyeSelector()
        val left = Offset(0.42f, 0.48f)
        val right = Offset(0.58f, 0.48f)

        selector.select(left, right, 0f, 0L, trackingId = 1)
        assertEquals(left, selector.select(left, right, 0f, 100L, trackingId = 2))
        assertEquals(PrimaryEyeSide.LEFT, selector.currentSide())
    }
}

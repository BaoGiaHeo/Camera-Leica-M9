package com.example.camui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun doesNotJumpEyesWhenYawChangesWhileSelectedEyeIsStillVisible() {
        val selector = PrimaryEyeSelector()
        val left = Offset(0.42f, 0.48f)
        val right = Offset(0.58f, 0.48f)

        selector.select(left, right, 0f, 0L, trackingId = 3)
        assertEquals(left, selector.select(left, right, 25f, 1_000L, trackingId = 3))
        assertEquals(PrimaryEyeSide.LEFT, selector.currentSide())
    }

    @Test
    fun switchesWhenOtherEyeIsAtLeastTwentyFivePercentLargerForFiveHundredMs() {
        val selector = PrimaryEyeSelector()
        val left = Offset(0.42f, 0.48f)
        val right = Offset(0.58f, 0.48f)

        selector.select(left, right, 0f, 0L, trackingId = 3, leftEyeArea = 20f, rightEyeArea = 10f)
        assertEquals(left, selector.select(left, right, 0f, 800L, trackingId = 3, leftEyeArea = 10f, rightEyeArea = 12.5f))
        assertEquals(left, selector.select(left, right, 0f, 1_299L, trackingId = 3, leftEyeArea = 10f, rightEyeArea = 12.5f))
        assertEquals(right, selector.select(left, right, 0f, 1_300L, trackingId = 3, leftEyeArea = 10f, rightEyeArea = 12.5f))
        assertEquals(PrimaryEyeSide.RIGHT, selector.currentSide())
    }

    @Test
    fun doesNotSwitchForAnAdvantageBelowTwentyFivePercent() {
        val selector = PrimaryEyeSelector()
        val left = Offset(0.42f, 0.48f)
        val right = Offset(0.58f, 0.48f)

        selector.select(left, right, 0f, 0L, trackingId = 3, leftEyeArea = 10f, rightEyeArea = 10f)
        assertEquals(left, selector.select(left, right, 0f, 1_000L, trackingId = 3, leftEyeArea = 10f, rightEyeArea = 12.4f))
        assertEquals(left, selector.select(left, right, 0f, 2_000L, trackingId = 3, leftEyeArea = 10f, rightEyeArea = 12.4f))
        assertEquals(PrimaryEyeSide.LEFT, selector.currentSide())
    }

    @Test
    fun requiresTheSizeAdvantageToRemainContinuousForTheConfirmationWindow() {
        val selector = PrimaryEyeSelector()
        val left = Offset(0.42f, 0.48f)
        val right = Offset(0.58f, 0.48f)

        selector.select(left, right, 0f, 0L, trackingId = 3, leftEyeArea = 20f, rightEyeArea = 10f)
        selector.select(left, right, 0f, 800L, trackingId = 3, leftEyeArea = 10f, rightEyeArea = 13f)
        assertEquals(left, selector.select(left, right, 0f, 1_000L, trackingId = 3, leftEyeArea = 10f, rightEyeArea = 11f))
        assertEquals(left, selector.select(left, right, 0f, 1_200L, trackingId = 3, leftEyeArea = 10f, rightEyeArea = 13f))
        assertEquals(left, selector.select(left, right, 0f, 1_699L, trackingId = 3, leftEyeArea = 10f, rightEyeArea = 13f))
        assertEquals(right, selector.select(left, right, 0f, 1_700L, trackingId = 3, leftEyeArea = 10f, rightEyeArea = 13f))
    }

    @Test
    fun switchesToOtherEyeAfterSelectedEyeHasBeenMissingForMoreThanTenFrames() {
        val selector = PrimaryEyeSelector(lostFrameThreshold = 11)
        val left = Offset(0.42f, 0.48f)
        val right = Offset(0.58f, 0.48f)

        selector.select(left, right, 0f, 0L, trackingId = 9)
        repeat(10) { index ->
            assertNull(selector.select(null, right, 0f, (index + 1) * 100L, trackingId = 9))
        }
        assertEquals(right, selector.select(null, right, 0f, 1_100L, trackingId = 9))
        assertEquals(PrimaryEyeSide.RIGHT, selector.currentSide())
    }

    @Test
    fun tappingTheOtherEyeSwitchesImmediatelyWithoutSizeOrYawDelay() {
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
    fun initialSelectionPrioritizesEyeConfidenceAndContourSizeBeforePoseHint() {
        val selector = PrimaryEyeSelector()
        val left = Offset(0.42f, 0.48f)
        val right = Offset(0.58f, 0.48f)

        assertEquals(right, selector.select(left, right, 18f, 0L, trackingId = 1))
        selector.reset()
        assertEquals(right, selector.select(
            left, right, 0f, 1_000L, trackingId = 2,
            leftConfidence = 0.70f, rightConfidence = 0.90f
        ))
        selector.reset()
        assertEquals(right, selector.select(
            left, right, -20f, 2_000L, trackingId = 3,
            leftEyeArea = 10f, rightEyeArea = 12f
        ))
    }

    @Test
    fun eyeContourAreaRejectsMissingDegenerateAndNonFinitePoints() {
        assertEquals(null, eyeContourArea(null))
        assertEquals(null, eyeContourArea(emptyList()))
        assertEquals(null, eyeContourArea(listOf(Offset(1f, 1f), Offset(2f, 2f))))
        assertEquals(null, eyeContourArea(listOf(Offset(Float.NaN, 1f), Offset(2f, 2f), Offset(3f, 3f))))
    }

    @Test
    fun eyeContourAreaUsesTheBoundingRectangleForHysteresis() {
        assertEquals(
            24f,
            eyeContourArea(listOf(
                Offset(2f, 3f), Offset(8f, 3f), Offset(8f, 7f), Offset(2f, 7f)
            ))!!,
            0.0001f
        )
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

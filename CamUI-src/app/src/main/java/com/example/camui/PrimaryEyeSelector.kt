package com.example.camui

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * Keeps a single primary eye for a face track.
 *
 * The selected eye is never changed merely because head yaw fluctuates. While it is
 * visible, the other eye may replace it only when its measured contour area is at
 * least 25% larger continuously for 500 ms. If the selected landmark disappears,
 * failover occurs only after more than ten consecutive missing frames. All automatic
 * switches are separated by at least 800 ms; an explicit user tap bypasses that delay.
 */
internal enum class PrimaryEyeSide { LEFT, RIGHT }

internal class PrimaryEyeSelector(
    private val switchConfirmMs: Long = 500L,
    private val minSwitchIntervalMs: Long = 800L,
    private val lostFrameThreshold: Int = 11,
    private val yawDeadZoneDegrees: Float = 10f
) {
    private var selectedSide: PrimaryEyeSide? = null
    private var activeTrackingId: Int? = null
    private var missingSelectedFrames = 0
    private var pendingSide: PrimaryEyeSide? = null
    private var pendingSinceMs = 0L
    private var lastSwitchAtMs = Long.MIN_VALUE / 2

    @Synchronized
    fun select(
        leftEye: Offset?,
        rightEye: Offset?,
        headYawDegrees: Float,
        nowMs: Long,
        trackingId: Int? = null,
        userPreferredSide: PrimaryEyeSide? = null,
        leftEyeArea: Float? = null,
        rightEyeArea: Float? = null,
        leftConfidence: Float? = null,
        rightConfidence: Float? = null
    ): Offset? {
        if (trackingId != null && activeTrackingId != trackingId) {
            reset()
            activeTrackingId = trackingId
        }

        if (selectedSide == null) {
            val preferred = userPreferredSide ?: initialSide(
                leftEye = leftEye,
                rightEye = rightEye,
                leftEyeArea = leftEyeArea,
                rightEyeArea = rightEyeArea,
                leftConfidence = leftConfidence,
                rightConfidence = rightConfidence,
                headYawDegrees = headYawDegrees
            )
            selectedSide = when {
                preferred == PrimaryEyeSide.LEFT && leftEye != null -> PrimaryEyeSide.LEFT
                preferred == PrimaryEyeSide.RIGHT && rightEye != null -> PrimaryEyeSide.RIGHT
                leftEye != null -> PrimaryEyeSide.LEFT
                rightEye != null -> PrimaryEyeSide.RIGHT
                else -> preferred
            }
            lastSwitchAtMs = nowMs
        }

        val selected = pointFor(selectedSide, leftEye, rightEye)
        val alternateSide = opposite(selectedSide!!)
        val alternate = pointFor(alternateSide, leftEye, rightEye)
        if (selected != null) {
            missingSelectedFrames = 0
            val selectedArea = areaFor(selectedSide, leftEyeArea, rightEyeArea)
            val alternateArea = areaFor(alternateSide, leftEyeArea, rightEyeArea)
            val alternateIsSignificantlyLarger =
                selectedArea != null && alternateArea != null &&
                    alternateArea >= selectedArea * 1.25f

            if (alternate != null && alternateIsSignificantlyLarger &&
                nowMs - lastSwitchAtMs >= minSwitchIntervalMs
            ) {
                if (pendingSide != alternateSide) {
                    pendingSide = alternateSide
                    pendingSinceMs = nowMs
                } else if (nowMs - pendingSinceMs >= switchConfirmMs) {
                    selectedSide = alternateSide
                    lastSwitchAtMs = nowMs
                    pendingSide = null
                    return alternate
                }
            } else {
                pendingSide = null
                pendingSinceMs = 0L
            }
            return selected
        }

        missingSelectedFrames++
        pendingSide = null
        pendingSinceMs = 0L
        if (alternate != null && missingSelectedFrames >= lostFrameThreshold &&
            nowMs - lastSwitchAtMs >= minSwitchIntervalMs
        ) {
            selectedSide = alternateSide
            lastSwitchAtMs = nowMs
            missingSelectedFrames = 0
            return alternate
        }
        return null
    }

    /** User taps bypass size hysteresis and switch to the requested eye immediately. */
    @Synchronized
    fun forceSelect(side: PrimaryEyeSide, nowMs: Long, trackingId: Int? = null) {
        if (trackingId != null && activeTrackingId != null && trackingId != activeTrackingId) return
        if (trackingId != null) activeTrackingId = trackingId
        selectedSide = side
        missingSelectedFrames = 0
        pendingSide = null
        pendingSinceMs = 0L
        lastSwitchAtMs = nowMs
    }

    @Synchronized
    fun currentSide(): PrimaryEyeSide? = selectedSide

    @Synchronized
    fun reset() {
        selectedSide = null
        activeTrackingId = null
        missingSelectedFrames = 0
        pendingSide = null
        pendingSinceMs = 0L
        lastSwitchAtMs = Long.MIN_VALUE / 2
    }

    private fun initialSide(
        leftEye: Offset?,
        rightEye: Offset?,
        leftEyeArea: Float?,
        rightEyeArea: Float?,
        leftConfidence: Float?,
        rightConfidence: Float?,
        headYawDegrees: Float
    ): PrimaryEyeSide {
        if (!headYawDegrees.isFinite()) {
            // Continue with landmark confidence/area when pose is not available.
        } else if (headYawDegrees > yawDeadZoneDegrees) {
            return PrimaryEyeSide.RIGHT
        } else if (headYawDegrees < -yawDeadZoneDegrees) {
            return PrimaryEyeSide.LEFT
        }

        val leftScore = leftConfidence?.takeIf { it.isFinite() } ?: 0f
        val rightScore = rightConfidence?.takeIf { it.isFinite() } ?: 0f
        if (abs(leftScore - rightScore) >= 0.05f) {
            return if (leftScore > rightScore) PrimaryEyeSide.LEFT else PrimaryEyeSide.RIGHT
        }

        val leftArea = leftEyeArea?.takeIf { it.isFinite() && it > 0f }
        val rightArea = rightEyeArea?.takeIf { it.isFinite() && it > 0f }
        if (leftEye != null && rightEye != null && leftArea != null && rightArea != null) {
            if (leftArea >= rightArea * 1.05f) return PrimaryEyeSide.LEFT
            if (rightArea >= leftArea * 1.05f) return PrimaryEyeSide.RIGHT
        }
        return if (leftEye != null || rightEye == null) PrimaryEyeSide.LEFT else PrimaryEyeSide.RIGHT
    }

    private fun areaFor(
        side: PrimaryEyeSide,
        leftArea: Float?,
        rightArea: Float?
    ): Float? = (if (side == PrimaryEyeSide.LEFT) leftArea else rightArea)
        ?.takeIf { it.isFinite() && it > 0f }

    private fun pointFor(side: PrimaryEyeSide?, left: Offset?, right: Offset?): Offset? =
        when (side) {
            PrimaryEyeSide.LEFT -> left
            PrimaryEyeSide.RIGHT -> right
            null -> null
        }

    private fun opposite(side: PrimaryEyeSide): PrimaryEyeSide =
        if (side == PrimaryEyeSide.LEFT) PrimaryEyeSide.RIGHT else PrimaryEyeSide.LEFT
}

package com.example.camui

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * Keeps one eye landmark for a face track.
 *
 * Camera2 Face and ML Kit Face expose landmark positions but do not expose per-eye depth
 * or a calibrated confidence score. Head yaw is therefore used only as a stable side hint.
 * A side change must persist for switchConfirmMs and is rate-limited by minSwitchIntervalMs.
 * If the selected landmark disappears, the other eye is not adopted until lostFrameThreshold
 * consecutive observations have elapsed.
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
        userPreferredSide: PrimaryEyeSide? = null
    ): Offset? {
        if (trackingId != null && activeTrackingId != trackingId) {
            reset()
            activeTrackingId = trackingId
        }

        val preferred = userPreferredSide ?: when {
            !headYawDegrees.isFinite() || abs(headYawDegrees) <= yawDeadZoneDegrees ->
                selectedSide ?: if (leftEye != null) PrimaryEyeSide.LEFT else PrimaryEyeSide.RIGHT
            headYawDegrees > 0f -> PrimaryEyeSide.RIGHT
            else -> PrimaryEyeSide.LEFT
        }
        if (selectedSide == null) {
            selectedSide = when {
                preferred == PrimaryEyeSide.LEFT && leftEye != null -> PrimaryEyeSide.LEFT
                preferred == PrimaryEyeSide.RIGHT && rightEye != null -> PrimaryEyeSide.RIGHT
                leftEye != null -> PrimaryEyeSide.LEFT
                rightEye != null -> PrimaryEyeSide.RIGHT
                else -> preferred
            }
            lastSwitchAtMs = nowMs
        }

        var selected = pointFor(selectedSide, leftEye, rightEye)
        val alternateSide = opposite(selectedSide!!)
        val alternate = pointFor(alternateSide, leftEye, rightEye)
        if (selected != null) {
            missingSelectedFrames = 0
            if (preferred != selectedSide && alternate != null &&
                nowMs - lastSwitchAtMs >= minSwitchIntervalMs
            ) {
                if (pendingSide != preferred) {
                    pendingSide = preferred
                    pendingSinceMs = nowMs
                } else if (nowMs - pendingSinceMs >= switchConfirmMs) {
                    selectedSide = preferred
                    lastSwitchAtMs = nowMs
                    pendingSide = null
                    selected = pointFor(selectedSide, leftEye, rightEye)
                }
            } else {
                pendingSide = null
            }
            return selected
        }

        missingSelectedFrames++
        pendingSide = null
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

    /** User taps bypass yaw hysteresis and switch to the requested eye immediately. */
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

    private fun pointFor(side: PrimaryEyeSide?, left: Offset?, right: Offset?): Offset? =
        when (side) {
            PrimaryEyeSide.LEFT -> left
            PrimaryEyeSide.RIGHT -> right
            null -> null
        }

    private fun opposite(side: PrimaryEyeSide): PrimaryEyeSide =
        if (side == PrimaryEyeSide.LEFT) PrimaryEyeSide.RIGHT else PrimaryEyeSide.LEFT
}

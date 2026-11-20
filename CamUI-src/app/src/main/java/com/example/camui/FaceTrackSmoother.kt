package com.example.camui

import androidx.compose.ui.geometry.Offset

internal data class FaceTrackSnapshot(
    val center: Offset,
    val size: Offset,
    val eyeCenter: Offset?
)

/**
 * Small synchronized tracker used by both Camera2 face metadata and ML Kit fallback.
 * A face must be present for three consecutive frames; eight misses are required
 * before removing the OSD box. Movement is damped to suppress frame-to-frame jitter.
 */
internal class FaceTrackSmoother(
    private val showAfterFrames: Int = 3,
    private val hideAfterFrames: Int = 8,
    private val smoothing: Float = 0.38f
) {
    private var visibleFrames = 0
    private var missingFrames = 0
    private var snapshot: FaceTrackSnapshot? = null

    @Synchronized
    fun onFaceDetected(center: Offset, size: Offset, eyeCenter: Offset?): FaceTrackSnapshot? {
        visibleFrames++
        missingFrames = 0
        if (visibleFrames < showAfterFrames) return null

        val previous = snapshot
        val nextCenter = previous?.center?.let { smooth(it, center) } ?: center
        val nextSize = previous?.size?.let { smooth(it, size) } ?: size
        val nextEye = eyeCenter?.let { eye -> previous?.eyeCenter?.let { smooth(it, eye) } ?: eye }
        return FaceTrackSnapshot(nextCenter, nextSize, nextEye).also { snapshot = it }
    }

    /**
     * Returns true only on the frame that crosses the hide threshold.
     */
    @Synchronized
    fun onNoFace(): Boolean {
        visibleFrames = 0
        missingFrames++
        if (missingFrames < hideAfterFrames || snapshot == null) return false
        snapshot = null
        missingFrames = 0
        return true
    }

    @Synchronized
    fun current(): FaceTrackSnapshot? = snapshot

    @Synchronized
    fun reset() {
        visibleFrames = 0
        missingFrames = 0
        snapshot = null
    }

    private fun smooth(previous: Offset, current: Offset): Offset {
        val alpha = smoothing.coerceIn(0f, 1f)
        return Offset(
            previous.x + (current.x - previous.x) * alpha,
            previous.y + (current.y - previous.y) * alpha
        )
    }
}

/** Rotates normalized sensor coordinates into the landscape preview coordinate space. */
internal fun rotateFacePoint(
    x: Float,
    y: Float,
    rotationDegrees: Int,
    frontFacing: Boolean
): Offset {
    val safeX = x.coerceIn(0f, 1f)
    val safeY = y.coerceIn(0f, 1f)
    val rotated = when (((rotationDegrees % 360) + 360) % 360) {
        90 -> Offset(1f - safeY, safeX)
        180 -> Offset(1f - safeX, 1f - safeY)
        270 -> Offset(safeY, 1f - safeX)
        else -> Offset(safeX, safeY)
    }
    return if (frontFacing) Offset(1f - rotated.x, rotated.y) else rotated
}

internal fun rotateFaceSize(width: Float, height: Float, rotationDegrees: Int): Offset {
    val safeWidth = width.coerceIn(0f, 1f)
    val safeHeight = height.coerceIn(0f, 1f)
    return if (((rotationDegrees % 360) + 360) % 360 in setOf(90, 270)) {
        Offset(safeHeight, safeWidth)
    } else {
        Offset(safeWidth, safeHeight)
    }
}

/** Crop rectangle expressed in the rotated ImageInput coordinate system. */
internal data class OrientedImageCrop(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
)

/**
 * Maps ImageProxy.cropRect from the sensor-buffer coordinate system into the
 * coordinate space used by ML Kit after InputImage applies rotationDegrees.
 */
internal fun orientImageCrop(
    imageWidth: Int,
    imageHeight: Int,
    cropLeft: Int,
    cropTop: Int,
    cropRight: Int,
    cropBottom: Int,
    rotationDegrees: Int
): OrientedImageCrop? {
    if (imageWidth <= 0 || imageHeight <= 0) return null
    val left = cropLeft.coerceIn(0, imageWidth)
    val top = cropTop.coerceIn(0, imageHeight)
    val right = cropRight.coerceIn(left, imageWidth)
    val bottom = cropBottom.coerceIn(top, imageHeight)
    if (right <= left || bottom <= top) return null
    return when (((rotationDegrees % 360) + 360) % 360) {
        90 -> OrientedImageCrop(
            left = imageHeight - bottom,
            top = left,
            width = bottom - top,
            height = right - left
        )
        180 -> OrientedImageCrop(
            left = imageWidth - right,
            top = imageHeight - bottom,
            width = right - left,
            height = bottom - top
        )
        270 -> OrientedImageCrop(
            left = top,
            top = imageWidth - right,
            width = bottom - top,
            height = right - left
        )
        else -> OrientedImageCrop(
            left = left,
            top = top,
            width = right - left,
            height = bottom - top
        )
    }
}

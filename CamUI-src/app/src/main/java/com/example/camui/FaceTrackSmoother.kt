package com.example.camui

import androidx.compose.ui.geometry.Offset

internal data class FaceTrackSnapshot(
    val center: Offset,
    val size: Offset,
    val eyeCenter: Offset?,
    val trackingId: Int? = null
)

/**
 * Face-only tracking can use Camera2 FULL/SIMPLE. FACE+EYE requires per-eye contour
 * geometry for the 25%-larger/500ms rule; Camera2 Face exposes eye points but not
 * eye contours, so that mode must use the 640x480 ML Kit contour path.
 */
internal fun shouldUseMlKitFaceDetection(
    trackingMode: String,
    camera2FaceDetectionAvailable: Boolean
): Boolean = when (trackingMode) {
    "face_eye" -> true
    "face" -> !camera2FaceDetectionAvailable
    else -> false
}

internal data class FaceTrackCandidate<T>(
    val value: T,
    val center: Offset,
    val size: Offset,
    val area: Float,
    val trackingId: Int? = null
)

/**
 * Chooses the tapped face first, then preserves a hardware/ML tracking identity,
 * then uses rectangle IoU and nearest-centre continuity before falling back to
 * the largest visible face.
 */
internal fun <T> chooseFaceTrackCandidate(
    candidates: List<FaceTrackCandidate<T>>,
    previous: FaceTrackSnapshot?,
    preferredCenter: Offset?,
    preferredMaximumDistance: Float = 0.35f,
    continuityMaximumDistance: Float = 0.5f,
    minimumIou: Float = 0.02f
): FaceTrackCandidate<T>? {
    if (candidates.isEmpty()) return null

    fun distanceSquared(first: Offset, second: Offset): Float {
        val dx = first.x - second.x
        val dy = first.y - second.y
        return dx * dx + dy * dy
    }

    if (preferredCenter != null) {
        val nearestToTap = candidates.minByOrNull {
            distanceSquared(it.center, preferredCenter)
        }
        if (nearestToTap != null &&
            distanceSquared(nearestToTap.center, preferredCenter) <=
                preferredMaximumDistance * preferredMaximumDistance
        ) {
            return nearestToTap
        }
    }

    previous?.trackingId?.let { previousId ->
        candidates.firstOrNull { it.trackingId == previousId }?.let { return it }
    }

    if (previous != null) {
        val nearest = candidates.minByOrNull {
            distanceSquared(it.center, previous.center)
        }
        val bestIou = candidates.maxByOrNull {
            faceIntersectionOverUnion(previous.center, previous.size, it.center, it.size)
        }
        if (bestIou != null &&
            faceIntersectionOverUnion(previous.center, previous.size, bestIou.center, bestIou.size) >= minimumIou
        ) {
            return bestIou
        }
        if (nearest != null &&
            distanceSquared(nearest.center, previous.center) <=
                continuityMaximumDistance * continuityMaximumDistance
        ) {
            return nearest
        }
    }

    return candidates.maxByOrNull { it.area }
}

internal fun faceIntersectionOverUnion(
    firstCenter: Offset,
    firstSize: Offset,
    secondCenter: Offset,
    secondSize: Offset
): Float {
    val firstLeft = firstCenter.x - firstSize.x * 0.5f
    val firstTop = firstCenter.y - firstSize.y * 0.5f
    val firstRight = firstCenter.x + firstSize.x * 0.5f
    val firstBottom = firstCenter.y + firstSize.y * 0.5f
    val secondLeft = secondCenter.x - secondSize.x * 0.5f
    val secondTop = secondCenter.y - secondSize.y * 0.5f
    val secondRight = secondCenter.x + secondSize.x * 0.5f
    val secondBottom = secondCenter.y + secondSize.y * 0.5f
    val intersectionWidth = (minOf(firstRight, secondRight) - maxOf(firstLeft, secondLeft)).coerceAtLeast(0f)
    val intersectionHeight = (minOf(firstBottom, secondBottom) - maxOf(firstTop, secondTop)).coerceAtLeast(0f)
    val intersection = intersectionWidth * intersectionHeight
    val union = (firstSize.x * firstSize.y) + (secondSize.x * secondSize.y) - intersection
    return if (union > 0f) (intersection / union).coerceIn(0f, 1f) else 0f
}

/**
 * Small synchronized tracker used by both Camera2 face metadata and ML Kit fallback.
 * A face must be present for three consecutive frames; eight misses are required
 * before removing the OSD box. Movement is damped to suppress frame-to-frame jitter.
 */
/**
 * One-Euro low-pass filter for a normalized two-dimensional measurement.
 * The cutoff rises with motion speed, suppressing stationary jitter without
 * making a moving subject feel like it is dragging behind the preview.
 */
private class OneEuroOffsetFilter(
    private val minCutoffHz: Float = 1.5f,
    private val beta: Float = 0.08f,
    private val derivativeCutoffHz: Float = 1f
) {
    private var initialized = false
    private var previousRaw = Offset.Zero
    private var filtered = Offset.Zero
    private var filteredDerivative = Offset.Zero
    private var previousTimestampMs = 0L

    fun filter(value: Offset, timestampMs: Long): Offset {
        if (!initialized) {
            initialized = true
            previousRaw = value
            filtered = value
            filteredDerivative = Offset.Zero
            previousTimestampMs = timestampMs
            return value
        }

        val elapsedMs = (timestampMs - previousTimestampMs).coerceAtLeast(1L)
        val dt = (elapsedMs / 1_000f).coerceIn(1f / 120f, 0.2f)
        val derivative = Offset(
            (value.x - previousRaw.x) / dt,
            (value.y - previousRaw.y) / dt
        )
        val derivativeAlpha = smoothingAlpha(derivativeCutoffHz, dt)
        filteredDerivative = Offset(
            lowPass(derivative.x, filteredDerivative.x, derivativeAlpha),
            lowPass(derivative.y, filteredDerivative.y, derivativeAlpha)
        )
        val cutoffX = minCutoffHz + beta * kotlin.math.abs(filteredDerivative.x)
        val cutoffY = minCutoffHz + beta * kotlin.math.abs(filteredDerivative.y)
        val filteredX = lowPass(value.x, filtered.x, smoothingAlpha(cutoffX, dt))
        val filteredY = lowPass(value.y, filtered.y, smoothingAlpha(cutoffY, dt))

        previousRaw = value
        filtered = Offset(filteredX, filteredY)
        previousTimestampMs = timestampMs
        return filtered
    }

    fun reset() {
        initialized = false
        previousRaw = Offset.Zero
        filtered = Offset.Zero
        filteredDerivative = Offset.Zero
        previousTimestampMs = 0L
    }

    private fun smoothingAlpha(cutoffHz: Float, dt: Float): Float {
        val safeCutoff = cutoffHz.coerceAtLeast(0.01f)
        val tau = 1f / (2f * Math.PI.toFloat() * safeCutoff)
        return 1f / (1f + tau / dt)
    }

    private fun lowPass(input: Float, previous: Float, alpha: Float): Float =
        alpha * input + (1f - alpha) * previous
}

/**
 * Synchronized face tracker. A detection must persist for three frames and
 * eight misses are required to hide it. One-Euro filters are applied to the
 * face rectangle and eye target independently.
 */
internal class FaceTrackSmoother(
    private val showAfterFrames: Int = 3,
    private val hideAfterFrames: Int = 8,
    @Suppress("UNUSED_PARAMETER") smoothing: Float = 0.38f
) {
    private var visibleFrames = 0
    private var missingFrames = 0
    private var snapshot: FaceTrackSnapshot? = null
    private val centerFilter = OneEuroOffsetFilter()
    private val sizeFilter = OneEuroOffsetFilter(minCutoffHz = 1.1f, beta = 0.05f)
    private var eyeFilter: OneEuroOffsetFilter? = null

    @Synchronized
    fun onFaceDetected(
        center: Offset,
        size: Offset,
        eyeCenter: Offset?,
        trackingId: Int? = null,
        timestampMs: Long = System.nanoTime() / 1_000_000L
    ): FaceTrackSnapshot? {
        visibleFrames++
        missingFrames = 0
        if (visibleFrames < showAfterFrames) return null

        val filteredCenter = centerFilter.filter(center, timestampMs)
        val filteredSize = sizeFilter.filter(size, timestampMs)
        val filteredEye = eyeCenter?.let { eye ->
            val filter = eyeFilter ?: OneEuroOffsetFilter().also { eyeFilter = it }
            filter.filter(eye, timestampMs)
        }
        if (eyeCenter == null) eyeFilter?.reset()
        return FaceTrackSnapshot(filteredCenter, filteredSize, filteredEye, trackingId)
            .also { snapshot = it }
    }

    /** Returns true only on the frame that crosses the hide threshold. */
    @Synchronized
    fun onNoFace(): Boolean {
        visibleFrames = 0
        missingFrames++
        if (missingFrames < hideAfterFrames || snapshot == null) return false
        snapshot = null
        missingFrames = 0
        centerFilter.reset()
        sizeFilter.reset()
        eyeFilter?.reset()
        eyeFilter = null
        return true
    }

    @Synchronized
    fun current(): FaceTrackSnapshot? = snapshot

    @Synchronized
    fun reset() {
        visibleFrames = 0
        missingFrames = 0
        snapshot = null
        centerFilter.reset()
        sizeFilter.reset()
        eyeFilter?.reset()
        eyeFilter = null
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

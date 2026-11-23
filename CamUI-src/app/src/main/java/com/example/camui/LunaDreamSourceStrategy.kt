package com.example.camui

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Size

/**
 * Runtime sensor/output survey used to choose an honest LunaDream source strategy.
 *
 * Strategy A is reported as available only when Camera2 exposes an ultra-high-resolution
 * sensor capability and a >=190MP JPEG output through the maximum-resolution stream map.
 * The current CameraX still-capture path does not automatically use that stream map; callers
 * must keep availableStrategy separate from activeCaptureStrategy.
 */
internal data class LunaDreamSourceSurvey(
    val cameraId: String?,
    val sensorPixelArrayWidth: Int?,
    val sensorPixelArrayHeight: Int?,
    val sensorPixelArrayPixels: Long?,
    val defaultJpegWidth: Int?,
    val defaultJpegHeight: Int?,
    val defaultJpegPixels: Long?,
    val hasUltraHighResolutionCapability: Boolean,
    val maximumResolutionJpegWidth: Int?,
    val maximumResolutionJpegHeight: Int?,
    val maximumResolutionJpegPixels: Long?,
    val availableStrategy: String,
    val activeCaptureStrategy: String,
    val reason: String
) {
    fun toLogString(): String = buildString {
        append("cameraId=").append(cameraId ?: "unknown")
        append("; sensorPixelArray=").append(sensorPixelArrayWidth ?: 0)
            .append("x").append(sensorPixelArrayHeight ?: 0)
            .append(" (").append(sensorPixelArrayPixels ?: 0L).append(" px)")
        append("; defaultJpeg=").append(defaultJpegWidth ?: 0)
            .append("x").append(defaultJpegHeight ?: 0)
            .append(" (").append(defaultJpegPixels ?: 0L).append(" px)")
        append("; ultraHighResolutionCapability=").append(hasUltraHighResolutionCapability)
        append("; maximumResolutionJpeg=").append(maximumResolutionJpegWidth ?: 0)
            .append("x").append(maximumResolutionJpegHeight ?: 0)
            .append(" (").append(maximumResolutionJpegPixels ?: 0L).append(" px)")
        append("; availableStrategy=").append(availableStrategy)
        append("; activeCaptureStrategy=").append(activeCaptureStrategy)
        append("; reason=").append(reason)
    }
}

internal object LunaDreamSourceStrategy {
    private const val STRATEGY_A_MIN_PIXELS = 190_000_000L
    private const val STRATEGY_B_MIN_PIXELS = 40_000_000L

    fun inspect(context: Context, cameraId: String?): LunaDreamSourceSurvey {
        if (cameraId.isNullOrBlank()) {
            return unavailableSurvey(null, "Camera2 camera ID unavailable")
        }
        return runCatching {
            val manager = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val array = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            val arrayPixels = array?.let { it.width.toLong() * it.height.toLong() }
            val capabilities = characteristics
                .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                ?.toSet()
                .orEmpty()
            val hasUltraHighResolution = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                capabilities.contains(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
                )

            val defaultMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val defaultJpeg = largestJpegSize(defaultMap?.getOutputSizes(ImageFormat.JPEG))

            val maximumMap = if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hasUltraHighResolution
            ) {
                runCatching {
                    characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION
                    )
                }.getOrNull()
            } else null
            val maximumJpeg = largestJpegSize(maximumMap?.getOutputSizes(ImageFormat.JPEG))
            val maximumPixels = maximumJpeg?.let { it.width.toLong() * it.height.toLong() }
            val availableA = hasUltraHighResolution &&
                maximumPixels != null && maximumPixels >= STRATEGY_A_MIN_PIXELS
            val defaultPixels = defaultJpeg?.let { it.width.toLong() * it.height.toLong() }

            val availableStrategy = when {
                availableA -> "A"
                (arrayPixels ?: defaultPixels ?: 0L) >= STRATEGY_B_MIN_PIXELS -> "B"
                else -> "C"
            }

            // The current CameraX still path uses the default stream configuration. Do not
            // claim strategy A is active until a MAXIMUM_RESOLUTION session is explicitly bound.
            val activeStrategy = when {
                (arrayPixels ?: defaultPixels ?: 0L) >= STRATEGY_B_MIN_PIXELS -> "B"
                else -> "C"
            }
            val reason = when {
                availableA ->
                    "A is exposed by HAL but not activated by the current CameraX session; active path uses $activeStrategy"
                activeStrategy == "B" ->
                    "Default sensor/output is >=40MP; computational multi-frame SR is active"
                arrayPixels != null || defaultPixels != null ->
                    "Default sensor/output is below 40MP; cap computational enlargement at 2x"
                else -> "Sensor pixel metadata and JPEG output sizes are unavailable"
            }

            LunaDreamSourceSurvey(
                cameraId = cameraId,
                sensorPixelArrayWidth = array?.width,
                sensorPixelArrayHeight = array?.height,
                sensorPixelArrayPixels = arrayPixels,
                defaultJpegWidth = defaultJpeg?.width,
                defaultJpegHeight = defaultJpeg?.height,
                defaultJpegPixels = defaultPixels,
                hasUltraHighResolutionCapability = hasUltraHighResolution,
                maximumResolutionJpegWidth = maximumJpeg?.width,
                maximumResolutionJpegHeight = maximumJpeg?.height,
                maximumResolutionJpegPixels = maximumPixels,
                availableStrategy = availableStrategy,
                activeCaptureStrategy = activeStrategy,
                reason = reason
            )
        }.getOrElse { failure ->
            unavailableSurvey(cameraId, failure.javaClass.simpleName + ": " + (failure.message ?: "unknown"))
        }
    }

    private fun largestJpegSize(sizes: Array<Size>?): Size? =
        sizes?.asSequence()
            ?.filter { it.width > 0 && it.height > 0 }
            ?.maxByOrNull { it.width.toLong() * it.height.toLong() }

    private fun unavailableSurvey(cameraId: String?, reason: String) = LunaDreamSourceSurvey(
        cameraId = cameraId,
        sensorPixelArrayWidth = null,
        sensorPixelArrayHeight = null,
        sensorPixelArrayPixels = null,
        defaultJpegWidth = null,
        defaultJpegHeight = null,
        defaultJpegPixels = null,
        hasUltraHighResolutionCapability = false,
        maximumResolutionJpegWidth = null,
        maximumResolutionJpegHeight = null,
        maximumResolutionJpegPixels = null,
        availableStrategy = "C",
        activeCaptureStrategy = "C",
        reason = reason
    )
}

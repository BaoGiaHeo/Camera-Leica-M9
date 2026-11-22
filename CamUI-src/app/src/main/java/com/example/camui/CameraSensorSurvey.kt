package com.example.camui

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Size

internal data class CameraResolutionCapability(
    val cameraId: String,
    val pixelArrayWidth: Int,
    val pixelArrayHeight: Int,
    val activeArrayWidth: Int,
    val activeArrayHeight: Int,
    val maximumPixelArrayWidth: Int,
    val maximumPixelArrayHeight: Int,
    val maximumActiveArrayWidth: Int,
    val maximumActiveArrayHeight: Int,
    val maximumResolutionJpegSizes: List<Size>,
    val maximumResolutionRawSizes: List<Size>,
    val normalJpegSizes: List<Size>,
    val normalRawSizes: List<Size>,
    val maximumResolutionStreamAvailable: Boolean,
    val ultraHighResolutionCapability: Boolean,
    val exposureTimeRangeNs: LongRange?,
    val sensitivityRangeIso: IntRange?,
    val blackLevelPattern: String,
    val whiteLevel: Int?,
    val colorFilterArrangement: Int?,
    val noiseProfile: String,
    val lensIntrinsicCalibration: List<Float>,
    val lensDistortion: List<Float>,
    val opticalStabilizationModes: List<Int>,
    val hotPixelMapModes: List<Int>,
    val rollingShutterSkewNs: Long?
) {
    val normalPixelCount: Long get() = pixelArrayWidth.toLong() * pixelArrayHeight.toLong()
    val maximumPixelCount: Long get() = maximumPixelArrayWidth.toLong() * maximumPixelArrayHeight.toLong()

    fun strategy(): LunaDreamSourceStrategy {
        val maximumStreamPixels = (maximumResolutionJpegSizes + maximumResolutionRawSizes)
            .maxOfOrNull { it.width.toLong() * it.height.toLong() } ?: 0L
        return LunaDreamSensorCapability(
            normalPixelArrayWidth = pixelArrayWidth,
            normalPixelArrayHeight = pixelArrayHeight,
            maximumResolutionWidth = maximumPixelArrayWidth,
            maximumResolutionHeight = maximumPixelArrayHeight,
            maximumResolutionStreamAvailable = maximumResolutionStreamAvailable &&
                maximumStreamPixels >= 190_000_000L,
            ultraHighResolutionCapability = ultraHighResolutionCapability
        ).strategy()
    }

    fun diagnosticLine(): String = buildString {
        append("cameraId=").append(cameraId)
        append(" pixelArray=").append(pixelArrayWidth).append('x').append(pixelArrayHeight)
        append(" activeArray=").append(activeArrayWidth).append('x').append(activeArrayHeight)
        append(" maximumPixelArray=").append(maximumPixelArrayWidth).append('x').append(maximumPixelArrayHeight)
        append(" maximumActiveArray=").append(maximumActiveArrayWidth).append('x').append(maximumActiveArrayHeight)
        append(" maxResStreams=").append(maximumResolutionStreamAvailable)
        append(" ultraHighRes=").append(ultraHighResolutionCapability)
        append(" normalJpeg=").append(normalJpegSizes.joinToString())
        append(" normalRaw=").append(normalRawSizes.joinToString())
        append(" maxJpeg=").append(maximumResolutionJpegSizes.joinToString())
        append(" maxRaw=").append(maximumResolutionRawSizes.joinToString())
        append(" strategy=").append(strategy())
        append(" exposureNs=").append(exposureTimeRangeNs)
        append(" iso=").append(sensitivityRangeIso)
        append(" blackLevel=").append(blackLevelPattern)
        append(" whiteLevel=").append(whiteLevel)
        append(" cfa=").append(colorFilterArrangement)
        append(" noiseProfile=").append(noiseProfile)
        append(" intrinsics=").append(lensIntrinsicCalibration)
        append(" distortion=").append(lensDistortion)
        append(" ois=").append(opticalStabilizationModes)
        append(" hotPixelMap=").append(hotPixelMapModes)
        append(" rollingShutterSkewNs=").append(rollingShutterSkewNs)
    }
}

/** Reads public Camera2 metadata only; vendor-private sensor modes are not assumed. */
internal object CameraSensorSurvey {
    fun inspect(manager: CameraManager, cameraId: String): CameraResolutionCapability? = runCatching {
        val c = manager.getCameraCharacteristics(cameraId)
        val normalMap = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val pixelArray = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val active = c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val capabilities = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES).orEmpty()
        val ultra = capabilities.contains(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
        )

        val maximumMap: StreamConfigurationMap? = if (Build.VERSION.SDK_INT >= 31) {
            c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
        } else null
        val maximumPixels = if (Build.VERSION.SDK_INT >= 31) {
            c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION)
        } else null
        val maximumActive = if (Build.VERSION.SDK_INT >= 31) {
            c.get(CameraCharacteristics.SCALER_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION)
        } else null

        val maxJpeg = maximumMap?.let { outputSizes(it, ImageFormat.JPEG) }.orEmpty()
        val maxRaw = maximumMap?.let { outputSizes(it, ImageFormat.RAW_SENSOR) }.orEmpty()
        val jpeg = normalMap?.let { outputSizes(it, ImageFormat.JPEG) }.orEmpty()
        val raw = normalMap?.let { outputSizes(it, ImageFormat.RAW_SENSOR) }.orEmpty()
        val blackPattern = c.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN)
        val intrinsics = c.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
        val distortion = if (Build.VERSION.SDK_INT >= 28) {
            c.get(CameraCharacteristics.LENS_DISTORTION)
        } else null
        val noise = c.get(CameraCharacteristics.SENSOR_NOISE_PROFILE)
        val exposure = c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val sensitivity = c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)

        CameraResolutionCapability(
            cameraId = cameraId,
            pixelArrayWidth = pixelArray?.width ?: 0,
            pixelArrayHeight = pixelArray?.height ?: 0,
            activeArrayWidth = active?.width() ?: 0,
            activeArrayHeight = active?.height() ?: 0,
            maximumPixelArrayWidth = maximumPixels?.width ?: pixelArray?.width ?: 0,
            maximumPixelArrayHeight = maximumPixels?.height ?: pixelArray?.height ?: 0,
            maximumActiveArrayWidth = maximumActive?.width() ?: active?.width() ?: 0,
            maximumActiveArrayHeight = maximumActive?.height() ?: active?.height() ?: 0,
            maximumResolutionJpegSizes = maxJpeg,
            maximumResolutionRawSizes = maxRaw,
            normalJpegSizes = jpeg,
            normalRawSizes = raw,
            maximumResolutionStreamAvailable = maximumMap != null && (maxJpeg.isNotEmpty() || maxRaw.isNotEmpty()),
            ultraHighResolutionCapability = ultra,
            exposureTimeRangeNs = exposure?.let { it.lower..it.upper },
            sensitivityRangeIso = sensitivity?.let { it.lower..it.upper },
            blackLevelPattern = blackPattern?.let {
                (0..3).joinToString(",") { index -> blackPattern.getOffsetForIndex(index).toString() }
            } ?: "unavailable",
            whiteLevel = c.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL),
            colorFilterArrangement = c.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT),
            noiseProfile = noise?.joinToString(";") { it.toString() } ?: "unavailable",
            lensIntrinsicCalibration = intrinsics?.toList().orEmpty(),
            lensDistortion = distortion?.toList().orEmpty(),
            opticalStabilizationModes = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION).orEmpty().toList(),
            hotPixelMapModes = if (Build.VERSION.SDK_INT >= 23) {
                c.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_HOT_PIXEL_MAP_MODES).orEmpty().toList()
            } else emptyList(),
            rollingShutterSkewNs = c.get(CameraCharacteristics.SENSOR_ROLLING_SHUTTER_SKEW)
        )
    }.onFailure {
        android.util.Log.e("FUJI_SENSOR_SURVEY", "Camera2 capability inspection failed cameraId=$cameraId", it)
    }.getOrNull()

    private fun outputSizes(map: StreamConfigurationMap, format: Int): List<Size> =
        runCatching { map.getOutputSizes(format)?.toList().orEmpty() }.getOrDefault(emptyList())
}

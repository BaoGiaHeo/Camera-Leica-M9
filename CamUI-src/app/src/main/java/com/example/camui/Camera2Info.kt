package com.example.camui

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureResult
import android.util.Log
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Camera2 capability discovery for the active CameraX device.
 *
 * Camera IDs are always discovered from CameraManager. Logical-camera physical IDs are
 * queried when the vendor exposes them; they are not assumed to exist on every device.
 */
data class CamDetails(
    val id: String,
    val parentId: String?,
    val facing: String,
    val hardwareLevel: String,
    val isLogicalMulti: Boolean,
    val physicalIds: Set<String>,
    val focalLengthsMm: List<Float>,
    val apertures: List<Float>,
    val eq35Mm: Float?,
    val sensorSizeMm: String?,
    val pixelArray: String?,
    val isoRange: String?,
    val exposureRange: String?,
    val evRange: String?,
    val maxDigitalZoom: Float?,
    val controlZoomRatioMin: Float?,
    val controlZoomRatioMax: Float?,
    val minFocusDiopter: Float?,
    val hasFlash: Boolean,
    val supportsRaw: Boolean,
    val hasOis: Boolean,
    val sensorOrientation: Int?,
    val eq35MmMaximum: Float? = null
)

data class LiveReading(
    val iso: Int?,
    val shutterNs: Long?,
    val aperture: Float?,
    val focalMm: Float?,
    val focusDiopter: Float?,
    val evIndex: Int?,
    val evStops: Float?,
    val afState: Int?,
    val aeState: Int?,
    val wbGains: String?,
    val activePhysicalCameraId: String?
) {
    /** Examples: 1/125 or 0.8" */
    val shutterText: String
        get() {
            val ns = shutterNs ?: return "—"
            if (ns <= 0L) return "—"
            val seconds = ns / 1_000_000_000.0
            return if (seconds >= 0.5) {
                String.format(Locale.US, "%.1f", seconds) + "\""
            } else {
                "1/${(1.0 / seconds).roundToInt()}"
            }
        }

    /** Converts diopters to an approximate focus distance in centimeters. */
    val focusText: String
        get() {
            val distance = focusDiopter ?: return "—"
            return if (distance > 0.01f) "${(100f / distance).roundToInt()}cm" else "∞"
        }
}

object Camera2Info {

    /**
     * Lists logical cameras and, where exposed by the device, their physical cameras.
     * A failed characteristics lookup for one camera does not prevent other cameras
     * from being returned.
     */
    fun listCameras(context: Context): List<CamDetails> {
        val manager = context.applicationContext
            .getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val result = ArrayList<CamDetails>()

        val cameraIds = runCatching { manager.cameraIdList.toList() }.getOrDefault(emptyList())
        for (id in cameraIds) {
            val characteristics = runCatching { manager.getCameraCharacteristics(id) }.getOrNull()
                ?: continue
            val physicalIds = runCatching { characteristics.physicalCameraIds.toSet() }
                .getOrDefault(emptySet())

            result += details(id, null, characteristics, physicalIds)

            for (physicalId in physicalIds) {
                val physicalCharacteristics = runCatching {
                    manager.getCameraCharacteristics(physicalId)
                }.getOrNull() ?: continue
                result += details(physicalId, id, physicalCharacteristics, emptySet())
            }
        }
        val cameras = result.distinctBy { it.id to it.parentId }
        val rearLenses = cameras.filter { it.facing == "BACK" }
        val rearEquivalents = rearLenses.flatMap { lens ->
            listOfNotNull(lens.eq35Mm, lens.eq35MmMaximum)
        }.distinct()
        val deviceOpticalAssessment = assessOpticalZoom(rearEquivalents)
        Log.i(
            "FUJI_ZOOM_LENS_INVENTORY",
            "cameraEntries=${cameras.size} rearEntries=${rearLenses.size} " +
                "lenses35mm=${rearLenses.joinToString { lens ->
                    "${lens.id}(parent=${lens.parentId ?: "none"},eq=${lens.eq35Mm ?: "?"}..${lens.eq35MmMaximum ?: lens.eq35Mm ?: "?"})"
                }} " +
                "main35mm=${deviceOpticalAssessment.mainEquivalent35Mm} " +
                "longest35mm=${deviceOpticalAssessment.longestEquivalent35Mm} " +
                "opticalTele=${deviceOpticalAssessment.hasOpticalTelephoto} " +
                "opticalRatio=${deviceOpticalAssessment.opticalRatio}"
        )
        return cameras
    }

    /** Human-readable Camera2 inventory, suitable for Logcat. */
    fun dump(context: Context): String = buildString {
        for (camera in listCameras(context)) {
            val role = when {
                camera.parentId != null -> "physical (parent=${camera.parentId})"
                camera.isLogicalMulti -> "logical multi-camera, physical=${camera.physicalIds}"
                else -> "standalone camera"
            }
            appendLine("== Camera id=${camera.id} | ${camera.facing} | $role")
            appendLine("   hardware=${camera.hardwareLevel}")
            appendLine(
                "   focal(mm)=${camera.focalLengthsMm} | eq35=${camera.eq35Mm?.let { String.format(Locale.US, "%.1fmm", it) } ?: "?"}" +
                    "..${camera.eq35MmMaximum?.let { String.format(Locale.US, "%.1fmm", it) } ?: "?"}"
            )
            appendLine("   aperture=${camera.apertures}")
            appendLine("   sensor(mm)=${camera.sensorSizeMm} | pixels=${camera.pixelArray}")
            appendLine("   ISO=${camera.isoRange} | exposure(s)=${camera.exposureRange} | EV=${camera.evRange}")
            appendLine("   maxZoom=${camera.maxDigitalZoom} | controlZoomRatio=${camera.controlZoomRatioMin}..${camera.controlZoomRatioMax} | minFocus(D)=${camera.minFocusDiopter}")
            appendLine("   flash=${camera.hasFlash} | RAW=${camera.supportsRaw} | OIS=${camera.hasOis} | orientation=${camera.sensorOrientation}")
        }
    }

    /** Read per-frame values from Camera2 CaptureResult / TotalCaptureResult. */
    fun readLive(
        result: CaptureResult,
        characteristics: CameraCharacteristics? = null
    ): LiveReading {
        val evStep = characteristics
            ?.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
            ?.toFloat()
        val evIndex = result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)
        val gains = result.get(CaptureResult.COLOR_CORRECTION_GAINS)?.let {
            String.format(
                Locale.US,
                "R=%.2f G=%.2f/%.2f B=%.2f",
                it.red, it.greenEven, it.greenOdd, it.blue
            )
        }

        return LiveReading(
            iso = result.get(CaptureResult.SENSOR_SENSITIVITY),
            shutterNs = result.get(CaptureResult.SENSOR_EXPOSURE_TIME),
            aperture = result.get(CaptureResult.LENS_APERTURE),
            focalMm = result.get(CaptureResult.LENS_FOCAL_LENGTH),
            focusDiopter = result.get(CaptureResult.LENS_FOCUS_DISTANCE),
            evIndex = evIndex,
            evStops = if (evIndex != null && evStep != null) evIndex * evStep else null,
            afState = result.get(CaptureResult.CONTROL_AF_STATE),
            aeState = result.get(CaptureResult.CONTROL_AE_STATE),
            wbGains = gains,
            activePhysicalCameraId = result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID)
        )
    }

    /**
     * Assesses optical telephoto availability from the active logical camera and
     * its exposed physical lenses. A telephoto requires at least 1.8x the main
     * lens's 35mm-equivalent focal length.
     */
    fun opticalZoomAssessment(
        logical: CamDetails?,
        physical: List<CamDetails>
    ): OpticalZoomAssessment {
        val equivalents = buildList {
            logical?.eq35Mm?.let(::add)
            logical?.eq35MmMaximum?.let(::add)
            physical.forEach { lens ->
                lens.eq35Mm?.let(::add)
                lens.eq35MmMaximum?.let(::add)
            }
        }
        return assessOpticalZoom(
            equivalentFocalLengths35Mm = equivalents,
            mainEquivalent35Mm = logical?.eq35Mm
        )
    }

    /**
     * Returns the maximum zoom CameraX can actually accept. Devices without a
     * disclosed telephoto are hard-capped at 2x; telephoto devices are capped
     * at 50x and additionally constrained by the active CameraX zoom range.
     * Digital zoom beyond this value requires a separate crop pipeline.
     */
    fun cameraReportedZoomMax(logical: CamDetails?, cameraXMaxZoom: Float): Float {
        val cameraXMaximum = cameraXMaxZoom.takeIf { it.isFinite() && it >= 1f } ?: 1f
        val camera2Maximum = logical?.controlZoomRatioMax?.takeIf { it.isFinite() && it >= 1f }
        return if (camera2Maximum != null) minOf(cameraXMaximum, camera2Maximum) else cameraXMaximum
    }

    fun maxSelectableZoom(
        logical: CamDetails?,
        physical: List<CamDetails>,
        cameraXMaxZoom: Float
    ): Float = selectableCameraZoomMax(
        cameraReportedMax = cameraReportedZoomMax(logical, cameraXMaxZoom),
        assessment = opticalZoomAssessment(logical, physical)
    )

    /**
     * Generates quick zoom stops from the active logical camera's exposed physical lenses.
     * Ratios are derived from 35mm-equivalent focal lengths, then constrained by CameraX's
     * actual zoom range. The 2x quick stop is retained only when the device supports it.
     */
    fun opticalZoomStops(
        logical: CamDetails?,
        physical: List<CamDetails>,
        minZoom: Float,
        maxZoom: Float,
        quickMaxZoom: Float = 2f
    ): List<Float> {
        val min = minZoom.takeIf { it.isFinite() && it > 0f } ?: 1f
        val max = minOf(
            maxZoom.takeIf { it.isFinite() && it > 0f } ?: 1f,
            quickMaxZoom
        ).coerceAtLeast(min)

        val physicalEquivalents = physical.mapNotNull { it.eq35Mm?.takeIf { value -> value > 0f } }
        val baseEquivalent = physicalEquivalents.minByOrNull { abs(it - 26f) }
            ?: logical?.eq35Mm?.takeIf { it > 0f }

        val candidates = ArrayList<Float>()
        if (min < 0.99f) candidates += min
        candidates += 1f

        if (baseEquivalent != null && baseEquivalent > 0f) {
            physical.forEach { lens ->
                val equivalent = lens.eq35Mm
                if (equivalent != null && equivalent > 0f) {
                    val ratio = equivalent / baseEquivalent
                    if (abs(ratio - 1f) >= 0.06f) candidates += ratio
                }
            }
        }

        if (max >= 1.99f) candidates += 2f

        return candidates
            .filter { it.isFinite() && it >= min - 0.01f && it <= max + 0.01f }
            .map { (it.coerceIn(min, max) * 10f).roundToInt() / 10f }
            .map { it.coerceIn(min, max) }
            .distinctBy { (it * 100f).roundToInt() }
            .sorted()
            .ifEmpty { listOf(1f.coerceIn(min, max)) }
    }

    private fun details(
        id: String,
        parentId: String?,
        characteristics: CameraCharacteristics,
        physicalIds: Set<String>
    ): CamDetails {
        val focalLengths = characteristics
            .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.toList()
            ?: emptyList()
        val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val controlZoomRatioRange = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            runCatching { characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE) }.getOrNull()
        } else null
        val equivalentRange = calculateEquivalentFocalRange35Mm(
            focalLengthsMm = focalLengths,
            sensorWidthMm = sensorSize?.width,
            sensorHeightMm = sensorSize?.height
        )

        val pixels = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val iso = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exposure = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val ev = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        val evStep = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
        val capabilities = characteristics
            .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            ?.toList()
            ?: emptyList()
        val oisModes = characteristics
            .get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)

        return CamDetails(
            id = id,
            parentId = parentId,
            facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                else -> "UNKNOWN"
            },
            hardwareLevel = when (characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                else -> "UNKNOWN"
            },
            isLogicalMulti = physicalIds.isNotEmpty() ||
                capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA),
            physicalIds = physicalIds,
            focalLengthsMm = focalLengths,
            apertures = characteristics
                .get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                ?.toList()
                ?: emptyList(),
            eq35Mm = equivalentRange.shortest,
            eq35MmMaximum = equivalentRange.longest,
            sensorSizeMm = sensorSize?.let {
                String.format(Locale.US, "%.2f x %.2f", it.width, it.height)
            },
            pixelArray = pixels?.let { "${it.width} x ${it.height}" },
            isoRange = iso?.let { "${it.lower}..${it.upper}" },
            exposureRange = exposure?.let {
                String.format(Locale.US, "%.6f..%.3f", it.lower / 1e9, it.upper / 1e9)
            },
            evRange = ev?.let {
                "${it.lower}..${it.upper} (step ${evStep?.toFloat()?.let { step -> String.format(Locale.US, "%.2f", step) } ?: "?"} EV)"
            },
            maxDigitalZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM),
            controlZoomRatioMin = controlZoomRatioRange?.lower,
            controlZoomRatioMax = controlZoomRatioRange?.upper,
            minFocusDiopter = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE),
            hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true,
            supportsRaw = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
            hasOis = oisModes?.any {
                it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON
            } == true,
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
        )
    }
}

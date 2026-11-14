package com.example.camui

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Best-effort inventory of every camera ID that Android exposes to this application.
 *
 * This intentionally uses supported Camera2 APIs only. OEM-private IDs that the camera
 * service/firmware does not publish to third-party applications cannot be enumerated by
 * an ordinary app; those are reported as an access limitation, never fabricated.
 */
data class CameraHardwareEntry(
    val id: String,
    val source: String,
    val parentId: String?,
    val facing: String,
    val lensRole: String,
    val focalLengths: String,
    val equivalent35mm: Float?,
    val sensorPixels: String,
    val sensorSizeMm: String,
    val jpegMax: String,
    val rawMax: String,
    val hardwareLevel: String,
    val maxDigitalZoom: Float?,
    val physicalIds: List<String>,
    val vendorMetadataKeys: List<String>
)

data class CameraHardwareReport(
    val manufacturer: String,
    val model: String,
    val androidSdk: Int,
    val logicalIds: List<String>,
    val physicalIdCount: Int,
    val entries: List<CameraHardwareEntry>,
    val unreadableIds: List<String>,
    val concurrentCameraSetCount: Int,
    val vendorMetadataKeys: List<String>,
    val limitations: List<String>
) {
    val publicIdCount: Int get() = logicalIds.size
}

object CameraHardwareScanner {

    fun scan(context: Context): CameraHardwareReport {
        val manager = context.applicationContext
            .getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val logicalIds = runCatching { manager.cameraIdList.toList() }
            .getOrElse { emptyList() }
        val unreadableIds = mutableListOf<String>()
        val rawEntries = mutableListOf<RawEntry>()
        val allVendorKeys = linkedSetOf<String>()
        var physicalCount = 0

        for (cameraId in logicalIds) {
            val characteristics = runCatching {
                manager.getCameraCharacteristics(cameraId)
            }.getOrElse {
                unreadableIds += cameraId
                null
            } ?: continue

            val physicalIds = runCatching {
                characteristics.physicalCameraIds.toList().sorted()
            }.getOrDefault(emptyList())

            rawEntries += RawEntry(
                id = cameraId,
                source = "Camera ID công khai",
                parentId = null,
                characteristics = characteristics,
                physicalIds = physicalIds
            )

            physicalIds.forEach { physicalId ->
                val physicalCharacteristics = runCatching {
                    manager.getCameraCharacteristics(physicalId)
                }.getOrElse {
                    unreadableIds += physicalId
                    null
                } ?: return@forEach

                physicalCount++
                rawEntries += RawEntry(
                    id = physicalId,
                    source = "Physical ID từ Logical Camera",
                    parentId = cameraId,
                    characteristics = physicalCharacteristics,
                    physicalIds = emptyList()
                )
            }
        }

        val backCameraEquivalents = rawEntries
            .filter {
                it.characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
            }
            .mapNotNull { equivalent35mm(it.characteristics) }
            .filter { it > 0f }

        // A typical main-camera equivalent is near 24–28 mm. Pick the disclosed rear lens
        // closest to 26 mm as the reference; role labels remain heuristic, not OEM truth.
        val mainReference = backCameraEquivalents.minByOrNull { abs(it - 26f) }

        val entries = rawEntries
            .distinctBy { it.id to it.parentId }
            .map { raw ->
                val c = raw.characteristics
                val vendorKeys = runCatching { c.keys.map { it.name } }
                    .getOrDefault(emptyList())
                    .filter(::looksLikeVendorKey)
                    .distinct()
                    .sorted()
                allVendorKeys += vendorKeys

                val facingValue = c.get(CameraCharacteristics.LENS_FACING)
                val facing = when (facingValue) {
                    CameraCharacteristics.LENS_FACING_BACK -> "Sau"
                    CameraCharacteristics.LENS_FACING_FRONT -> "Trước"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "Ngoài"
                    else -> "Không rõ"
                }
                val equivalent = equivalent35mm(c)
                val role = classifyLensRole(facingValue, equivalent, mainReference)
                val focal = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?.joinToString(" / ") { String.format(Locale.US, "%.1f mm", it) }
                    ?: "Không công bố"
                val sensor = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val pixels = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                val streamMap = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val jpeg = runCatching {
                    streamMap?.getOutputSizes(ImageFormat.JPEG)
                        ?.maxByOrNull { it.width.toLong() * it.height.toLong() }
                        ?.let { "${it.width} × ${it.height}" }
                }.getOrNull() ?: "Không công bố"
                val raw = runCatching {
                    streamMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
                        ?.maxByOrNull { it.width.toLong() * it.height.toLong() }
                        ?.let { "${it.width} × ${it.height}" }
                }.getOrNull() ?: "Không công bố"
                val hardware = when (
                    c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                ) {
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                    else -> "UNKNOWN"
                }

                CameraHardwareEntry(
                    id = raw.id,
                    source = raw.source,
                    parentId = raw.parentId,
                    facing = facing,
                    lensRole = role,
                    focalLengths = focal,
                    equivalent35mm = equivalent,
                    sensorPixels = pixels?.let { "${it.width} × ${it.height}" } ?: "Không công bố",
                    sensorSizeMm = sensor?.let {
                        String.format(Locale.US, "%.2f × %.2f mm", it.width, it.height)
                    } ?: "Không công bố",
                    jpegMax = jpeg,
                    rawMax = raw,
                    hardwareLevel = hardware,
                    maxDigitalZoom = c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM),
                    physicalIds = raw.physicalIds,
                    vendorMetadataKeys = vendorKeys
                )
            }

        val concurrentCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { manager.concurrentCameraIds.size }.getOrDefault(0)
        } else {
            0
        }

        val limitations = buildList {
            add(
                "Camera ID công khai được lấy từ CameraManager.cameraIdList; Physical ID được lấy " +
                    "từ metadata LOGICAL_MULTI_CAMERA khi firmware công bố."
            )
            add(
                "Vendor metadata chỉ liệt kê các key mà HAL công khai trên những camera đã truy cập được; " +
                    "vendor key không đồng nghĩa với một Camera ID ẩn."
            )
            add(
                "Android SDK không có API công khai để liệt kê ID bị firmware/OEM ẩn hoàn toàn. " +
                    "Ứng dụng không dùng root, hidden-API reflection hoặc ID phỏng đoán để vượt giới hạn đó."
            )
            if (unreadableIds.isNotEmpty()) {
                add("Không đọc được characteristics của: ${unreadableIds.distinct().joinToString()}.")
            }
        }

        return CameraHardwareReport(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            androidSdk = Build.VERSION.SDK_INT,
            logicalIds = logicalIds,
            physicalIdCount = physicalCount,
            entries = entries,
            unreadableIds = unreadableIds.distinct(),
            concurrentCameraSetCount = concurrentCount,
            vendorMetadataKeys = allVendorKeys.toList().sorted(),
            limitations = limitations
        )
    }

    private data class RawEntry(
        val id: String,
        val source: String,
        val parentId: String?,
        val characteristics: CameraCharacteristics,
        val physicalIds: List<String>
    )

    private fun equivalent35mm(characteristics: CameraCharacteristics): Float? {
        val focal = characteristics
            .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.firstOrNull()
            ?: return null
        val sensor = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            ?: return null
        val diagonal = hypot(sensor.width, sensor.height)
        if (focal <= 0f || diagonal <= 0f) return null
        return focal * 43.27f / diagonal
    }

    private fun classifyLensRole(
        facing: Int?,
        equivalent35: Float?,
        mainReference: Float?
    ): String {
        if (facing == CameraCharacteristics.LENS_FACING_FRONT) return "Camera trước"
        if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) return "Camera ngoài"
        if (facing != CameraCharacteristics.LENS_FACING_BACK) return "Chưa xác định"
        if (equivalent35 == null || mainReference == null || mainReference <= 0f) {
            return "Camera sau · chưa đủ dữ liệu tiêu cự"
        }
        val ratio = equivalent35 / mainReference
        return when {
            ratio < 0.78f -> "Ultra-wide"
            ratio > 1.25f -> "Telephoto"
            ratio in 0.88f..1.12f -> "Main / Wide"
            ratio < 1f -> "Góc rộng phụ"
            else -> "Tele / tiêu cự dài"
        }
    }

    private fun looksLikeVendorKey(name: String): Boolean {
        val normalized = name.lowercase(Locale.US)
        return !normalized.startsWith("android.") &&
            (normalized.contains('.') || normalized.contains("vendor") ||
                normalized.contains("xiaomi") || normalized.contains("qti") ||
                normalized.contains("samsung") || normalized.contains("oplus"))
    }
}

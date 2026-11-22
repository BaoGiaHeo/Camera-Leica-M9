package com.example.camui

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Camera-owned capture state. Each logical Camera2 ID receives a separate key
 * namespace so switching lenses never aliases exposure/focus/zoom settings.
 */
internal data class CameraParamsSnapshot(
    val evIndex: Int = 0,
    val manualExposure: Boolean = false,
    val manualIsoEnabled: Boolean = false,
    val manualShutterEnabled: Boolean = false,
    val manualIso: Int = 400,
    val manualShutterNs: Long = 10_000_000L,
    val manualWhiteBalanceEnabled: Boolean = false,
    val whiteBalanceKelvin: Int = 5_200,
    val afMode: String = "AF-S",
    val manualFocusDiopters: Float = 0f,
    val zoomRatio: Float = 1f,
    val meteringIndex: Int = 0,
    val touchAfEnabled: Boolean = false,
    val afLocked: Boolean = false
) {
    fun sanitized(): CameraParamsSnapshot = copy(
        manualIso = manualIso.coerceAtLeast(50),
        manualShutterNs = manualShutterNs.coerceAtLeast(1_000_000L),
        whiteBalanceKelvin = whiteBalanceKelvin.coerceIn(2_000, 10_000),
        afMode = afMode.takeIf { it in setOf("AF-S", "AF-C", "MF") } ?: "AF-S",
        manualFocusDiopters = manualFocusDiopters
            .takeIf { it.isFinite() && it >= 0f } ?: 0f,
        zoomRatio = zoomRatio.takeIf { it.isFinite() && it > 0f } ?: 1f,
        meteringIndex = meteringIndex.coerceIn(0, 3)
    )
}

internal object CameraParamsStore {
    private const val ROOT = "camera_params_v1_"

    private object Field {
        const val EV = "ev"
        const val MANUAL_EXPOSURE = "manual_exposure"
        const val MANUAL_ISO_ENABLED = "manual_iso_enabled"
        const val MANUAL_SHUTTER_ENABLED = "manual_shutter_enabled"
        const val MANUAL_ISO = "manual_iso"
        const val MANUAL_SHUTTER_NS = "manual_shutter_ns"
        const val MANUAL_WB_ENABLED = "manual_wb_enabled"
        const val WB_KELVIN = "wb_kelvin"
        const val AF_MODE = "af_mode"
        const val MANUAL_FOCUS = "manual_focus_diopters"
        const val ZOOM = "zoom_ratio"
        const val METERING = "metering"
        const val TOUCH_AF = "touch_af"
        const val AF_LOCKED = "af_locked"
    }

    fun keyPrefix(cameraId: String): String {
        require(cameraId.isNotBlank()) { "cameraId must not be blank" }
        val token = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(cameraId.toByteArray(StandardCharsets.UTF_8))
        return ROOT + token + "_"
    }

    fun load(
        preferences: Map<String, Any?>,
        cameraId: String,
        fallback: CameraParamsSnapshot = legacySnapshot(preferences)
    ): CameraParamsSnapshot {
        val prefix = keyPrefix(cameraId)
        fun value(field: String) = preferences[prefix + field]
        fun int(field: String, default: Int) = (value(field) as? Number)?.toInt() ?: default
        fun long(field: String, default: Long) = (value(field) as? Number)?.toLong() ?: default
        fun float(field: String, default: Float) = (value(field) as? Number)?.toFloat() ?: default
        fun bool(field: String, default: Boolean) = value(field) as? Boolean ?: default
        fun string(field: String, default: String) = value(field) as? String ?: default

        return CameraParamsSnapshot(
            evIndex = int(Field.EV, fallback.evIndex),
            manualExposure = bool(Field.MANUAL_EXPOSURE, fallback.manualExposure),
            manualIsoEnabled = bool(Field.MANUAL_ISO_ENABLED, fallback.manualIsoEnabled),
            manualShutterEnabled = bool(Field.MANUAL_SHUTTER_ENABLED, fallback.manualShutterEnabled),
            manualIso = int(Field.MANUAL_ISO, fallback.manualIso),
            manualShutterNs = long(Field.MANUAL_SHUTTER_NS, fallback.manualShutterNs),
            manualWhiteBalanceEnabled = bool(Field.MANUAL_WB_ENABLED, fallback.manualWhiteBalanceEnabled),
            whiteBalanceKelvin = int(Field.WB_KELVIN, fallback.whiteBalanceKelvin),
            afMode = string(Field.AF_MODE, fallback.afMode),
            manualFocusDiopters = float(Field.MANUAL_FOCUS, fallback.manualFocusDiopters),
            zoomRatio = float(Field.ZOOM, fallback.zoomRatio),
            meteringIndex = int(Field.METERING, fallback.meteringIndex),
            touchAfEnabled = bool(Field.TOUCH_AF, fallback.touchAfEnabled),
            afLocked = bool(Field.AF_LOCKED, fallback.afLocked)
        ).sanitized()
    }

    /** Legacy global keys are read only as a migration fallback for first use of a camera. */
    fun legacySnapshot(preferences: Map<String, Any?>): CameraParamsSnapshot {
        fun int(key: String, default: Int) = (preferences[key] as? Number)?.toInt() ?: default
        fun long(key: String, default: Long) = (preferences[key] as? Number)?.toLong() ?: default
        fun float(key: String, default: Float) = (preferences[key] as? Number)?.toFloat() ?: default
        fun bool(key: String, default: Boolean) = preferences[key] as? Boolean ?: default
        fun string(key: String, default: String) = preferences[key] as? String ?: default
        return CameraParamsSnapshot(
            evIndex = int("pro_ev_index", 0),
            manualExposure = bool("pro_manual_exposure_enabled", false),
            manualIsoEnabled = bool("pro_manual_iso_enabled", false),
            manualShutterEnabled = bool("pro_manual_shutter_enabled", false),
            manualIso = int("pro_manual_iso", 400),
            manualShutterNs = long("pro_manual_shutter_ns", 10_000_000L),
            manualWhiteBalanceEnabled = bool("pro_manual_wb_enabled", false),
            whiteBalanceKelvin = int("pro_manual_wb_kelvin", 5_200),
            afMode = string("pro_af_mode", "AF-S"),
            manualFocusDiopters = float("pro_manual_focus", 0f),
            zoomRatio = float("camera_zoom_ratio", 1f),
            meteringIndex = int("camera_metering_index", 0),
            touchAfEnabled = bool("pro_touch_af_enabled", false),
            afLocked = bool("pro_af_locked", false)
        ).sanitized()
    }

    fun toPreferenceEntries(
        cameraId: String,
        snapshot: CameraParamsSnapshot
    ): Map<String, Any> {
        val clean = snapshot.sanitized()
        val prefix = keyPrefix(cameraId)
        return linkedMapOf(
            prefix + Field.EV to clean.evIndex,
            prefix + Field.MANUAL_EXPOSURE to clean.manualExposure,
            prefix + Field.MANUAL_ISO_ENABLED to clean.manualIsoEnabled,
            prefix + Field.MANUAL_SHUTTER_ENABLED to clean.manualShutterEnabled,
            prefix + Field.MANUAL_ISO to clean.manualIso,
            prefix + Field.MANUAL_SHUTTER_NS to clean.manualShutterNs,
            prefix + Field.MANUAL_WB_ENABLED to clean.manualWhiteBalanceEnabled,
            prefix + Field.WB_KELVIN to clean.whiteBalanceKelvin,
            prefix + Field.AF_MODE to clean.afMode,
            prefix + Field.MANUAL_FOCUS to clean.manualFocusDiopters,
            prefix + Field.ZOOM to clean.zoomRatio,
            prefix + Field.METERING to clean.meteringIndex,
            prefix + Field.TOUCH_AF to clean.touchAfEnabled,
            prefix + Field.AF_LOCKED to clean.afLocked
        )
    }
}

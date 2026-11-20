package com.example.camui

internal data class CameraExposureRequestState(
    val automatic: Boolean,
    val sensorSensitivityIso: Int?,
    val sensorExposureTimeNs: Long?,
    val sensorFrameDurationNs: Long?
)

/** Resolves dynamic ISO/Tv request values without requiring a CameraX rebind. */
internal fun resolveCameraExposureRequestState(
    manualIsoEnabled: Boolean,
    manualShutterEnabled: Boolean,
    manualIso: Int,
    manualShutterNs: Long,
    liveIso: Int,
    liveShutterNs: Long
): CameraExposureRequestState {
    if (!manualIsoEnabled && !manualShutterEnabled) {
        return CameraExposureRequestState(true, null, null, null)
    }
    val resolvedIso = (if (manualIsoEnabled) manualIso else liveIso)
        .takeIf { it > 0 } ?: 400
    val resolvedExposure = (if (manualShutterEnabled) manualShutterNs else liveShutterNs)
        .takeIf { it > 0L } ?: 16_666_667L
    return CameraExposureRequestState(
        automatic = false,
        sensorSensitivityIso = resolvedIso,
        sensorExposureTimeNs = resolvedExposure,
        sensorFrameDurationNs = maxOf(resolvedExposure, 10_000_000L)
    )
}

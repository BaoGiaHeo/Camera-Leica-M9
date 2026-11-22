package com.example.camui

internal enum class ProcessingPrimaryMode {
    NONE,
    LUNA_DREAM_15,
    LUNA_DREAM_20,
    NIGHT_SHOT,
    HDR_TONE_MAPPING,
    NATIVE_PROCESSING
}

internal enum class ProcessingModeLockReason {
    LUNA_DREAM_15,
    LUNA_DREAM_20,
    NIGHT_SHOT,
    HDR_TONE_MAPPING,
    NATIVE_PROCESSING
}

internal data class ProcessingModeSnapshot(
    val lunaDreamMode: String = "off",
    val nightShotMode: String = "off",
    val hdrToneMappingEnabled: Boolean = false,
    val nativeProcessingEnabled: Boolean = false,
    val livePhotoEnabled: Boolean = false
) {
    fun activePrimaryMode(): ProcessingPrimaryMode = when {
        nativeProcessingEnabled -> ProcessingPrimaryMode.NATIVE_PROCESSING
        hdrToneMappingEnabled -> ProcessingPrimaryMode.HDR_TONE_MAPPING
        nightShotMode != "off" -> ProcessingPrimaryMode.NIGHT_SHOT
        lunaDreamMode == "luna20" -> ProcessingPrimaryMode.LUNA_DREAM_20
        lunaDreamMode == "luna15" -> ProcessingPrimaryMode.LUNA_DREAM_15
        else -> ProcessingPrimaryMode.NONE
    }
}

internal object ProcessingModeController {
    fun transition(
        current: ProcessingModeSnapshot,
        requested: ProcessingPrimaryMode,
        enabled: Boolean,
        nightShotValue: String = "night"
    ): ProcessingModeSnapshot {
        if (!enabled) {
            return when (requested) {
                ProcessingPrimaryMode.LUNA_DREAM_15,
                ProcessingPrimaryMode.LUNA_DREAM_20 -> current.copy(lunaDreamMode = "off")
                ProcessingPrimaryMode.NIGHT_SHOT -> current.copy(nightShotMode = "off")
                ProcessingPrimaryMode.HDR_TONE_MAPPING -> current.copy(hdrToneMappingEnabled = false)
                ProcessingPrimaryMode.NATIVE_PROCESSING -> current.copy(nativeProcessingEnabled = false)
                ProcessingPrimaryMode.NONE -> current
            }
        }

        return when (requested) {
            ProcessingPrimaryMode.NONE -> current.copy(
                lunaDreamMode = "off",
                nightShotMode = "off",
                hdrToneMappingEnabled = false,
                nativeProcessingEnabled = false
            )
            ProcessingPrimaryMode.LUNA_DREAM_15 -> current.copy(
                lunaDreamMode = "luna15",
                nightShotMode = "off",
                hdrToneMappingEnabled = false,
                nativeProcessingEnabled = false
            )
            ProcessingPrimaryMode.LUNA_DREAM_20 -> current.copy(
                lunaDreamMode = "luna20",
                nightShotMode = "off",
                hdrToneMappingEnabled = false,
                nativeProcessingEnabled = false,
                livePhotoEnabled = false
            )
            ProcessingPrimaryMode.NIGHT_SHOT -> current.copy(
                lunaDreamMode = "off",
                nightShotMode = nightShotValue.takeIf { it != "off" } ?: "night",
                hdrToneMappingEnabled = false,
                nativeProcessingEnabled = false,
                livePhotoEnabled = false
            )
            ProcessingPrimaryMode.HDR_TONE_MAPPING -> current.copy(
                lunaDreamMode = "off",
                nightShotMode = "off",
                hdrToneMappingEnabled = true,
                nativeProcessingEnabled = false
            )
            ProcessingPrimaryMode.NATIVE_PROCESSING -> current.copy(
                lunaDreamMode = "off",
                nightShotMode = "off",
                hdrToneMappingEnabled = false,
                nativeProcessingEnabled = true,
                livePhotoEnabled = false
            )
        }
    }

    fun isLocked(
        state: ProcessingModeSnapshot,
        target: ProcessingPrimaryMode
    ): Boolean {
        val active = state.activePrimaryMode()
        if (active == ProcessingPrimaryMode.NONE || active == target) return false
        if (target == ProcessingPrimaryMode.NONE) return false
        // LunaDream 1.5 and 2.0 are alternatives within the same exclusive group.
        return active != target
    }

    fun lockReason(
        state: ProcessingModeSnapshot,
        target: ProcessingPrimaryMode
    ): ProcessingModeLockReason? {
        if (!isLocked(state, target)) return null
        return when (state.activePrimaryMode()) {
            ProcessingPrimaryMode.LUNA_DREAM_15 -> ProcessingModeLockReason.LUNA_DREAM_15
            ProcessingPrimaryMode.LUNA_DREAM_20 -> ProcessingModeLockReason.LUNA_DREAM_20
            ProcessingPrimaryMode.NIGHT_SHOT -> ProcessingModeLockReason.NIGHT_SHOT
            ProcessingPrimaryMode.HDR_TONE_MAPPING -> ProcessingModeLockReason.HDR_TONE_MAPPING
            ProcessingPrimaryMode.NATIVE_PROCESSING -> ProcessingModeLockReason.NATIVE_PROCESSING
            ProcessingPrimaryMode.NONE -> null
        }
    }

    fun livePhotoLocked(state: ProcessingModeSnapshot): Boolean =
        state.nativeProcessingEnabled ||
            state.lunaDreamMode == "luna20" ||
            state.nightShotMode != "off"
}

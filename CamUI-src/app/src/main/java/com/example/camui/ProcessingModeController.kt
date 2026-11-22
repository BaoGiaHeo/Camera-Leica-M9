package com.example.camui

internal enum class ProcessingPrimaryMode {
    NONE,
    LUNA_DREAM_15,
    LUNA_DREAM_20,
    LUNA_DREAM_25,
    LUNA_DREAM_30,
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
        lunaDreamMode == "luna30" -> ProcessingPrimaryMode.LUNA_DREAM_30
        lunaDreamMode == "luna25" -> ProcessingPrimaryMode.LUNA_DREAM_25
        lunaDreamMode == "luna20" -> ProcessingPrimaryMode.LUNA_DREAM_20
        lunaDreamMode == "luna15" -> ProcessingPrimaryMode.LUNA_DREAM_15
        else -> ProcessingPrimaryMode.NONE
    }
}

internal object ProcessingModeController {
    fun fromPreferences(preferences: android.content.SharedPreferences): ProcessingModeSnapshot =
        ProcessingModeSnapshot(
            lunaDreamMode = preferences.getString("lunadream_mode", "off") ?: "off",
            nightShotMode = preferences.getString("nightshot_mode", "off") ?: "off",
            hdrToneMappingEnabled = preferences.getBoolean("hdr_enabled", false),
            nativeProcessingEnabled = preferences.getBoolean("native_image_processing_enabled", false),
            livePhotoEnabled = preferences.getBoolean("live_photo_enabled", false)
        )

    fun persist(
        preferences: android.content.SharedPreferences,
        snapshot: ProcessingModeSnapshot
    ) {
        preferences.edit()
            .putString("lunadream_mode", snapshot.lunaDreamMode)
            .putString("nightshot_mode", snapshot.nightShotMode)
            .putBoolean("hdr_enabled", snapshot.hdrToneMappingEnabled)
            .putBoolean("hdr_coming_soon", false)
            .putBoolean("native_image_processing_enabled", snapshot.nativeProcessingEnabled)
            .putBoolean("live_photo_enabled", snapshot.livePhotoEnabled)
            .apply()
    }

    fun transitionFromPreferences(
        preferences: android.content.SharedPreferences,
        requested: ProcessingPrimaryMode,
        enabled: Boolean,
        nightShotValue: String = "night"
    ): ProcessingModeSnapshot {
        val next = transition(fromPreferences(preferences), requested, enabled, nightShotValue)
        persist(preferences, next)
        return next
    }

    fun transition(
        current: ProcessingModeSnapshot,
        requested: ProcessingPrimaryMode,
        enabled: Boolean,
        nightShotValue: String = "night"
    ): ProcessingModeSnapshot {
        if (!enabled) {
            return when (requested) {
                ProcessingPrimaryMode.LUNA_DREAM_15,
                ProcessingPrimaryMode.LUNA_DREAM_20,
                ProcessingPrimaryMode.LUNA_DREAM_25,
                ProcessingPrimaryMode.LUNA_DREAM_30 -> current.copy(lunaDreamMode = "off")
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
            ProcessingPrimaryMode.LUNA_DREAM_25 -> current.copy(
                lunaDreamMode = "luna25",
                nightShotMode = "off",
                hdrToneMappingEnabled = false,
                nativeProcessingEnabled = false,
                livePhotoEnabled = false
            )
            ProcessingPrimaryMode.LUNA_DREAM_30 -> current.copy(
                lunaDreamMode = "luna30",
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
            ProcessingPrimaryMode.LUNA_DREAM_25 -> ProcessingModeLockReason.LUNA_DREAM_25
            ProcessingPrimaryMode.LUNA_DREAM_30 -> ProcessingModeLockReason.LUNA_DREAM_30
            ProcessingPrimaryMode.NIGHT_SHOT -> ProcessingModeLockReason.NIGHT_SHOT
            ProcessingPrimaryMode.HDR_TONE_MAPPING -> ProcessingModeLockReason.HDR_TONE_MAPPING
            ProcessingPrimaryMode.NATIVE_PROCESSING -> ProcessingModeLockReason.NATIVE_PROCESSING
            ProcessingPrimaryMode.NONE -> null
        }
    }

    fun livePhotoLocked(state: ProcessingModeSnapshot): Boolean =
        state.nativeProcessingEnabled ||
            state.lunaDreamMode != "off" ||
            state.nightShotMode != "off"
}

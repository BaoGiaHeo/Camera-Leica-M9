package com.example.camui

enum class CameraLcdDisplayMode(val labelResource: Int) {
    STANDARD(R.string.display_mode_standard),
    FULL_INFO(R.string.display_mode_full_info),
    CLEAN(R.string.display_mode_clean),
    LEVEL_GRID(R.string.display_mode_level_grid);

    fun next(): CameraLcdDisplayMode {
        val modes = entries
        return modes[(ordinal + 1) % modes.size]
    }
}

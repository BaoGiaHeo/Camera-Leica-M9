package com.example.camui

enum class CameraLcdDisplayMode(val labelResource: Int) {
    STANDARD(R.string.display_mode_standard),
    FULL_INFO(R.string.display_mode_full_info),
    CLEAN(R.string.display_mode_clean),
    LEVEL_GRID(R.string.display_mode_level_grid);

    fun showsProHud(isProMode: Boolean): Boolean =
        isProMode && this != CLEAN && this != LEVEL_GRID

    fun next(): CameraLcdDisplayMode = when (this) {
        STANDARD -> CLEAN
        CLEAN -> LEVEL_GRID
        LEVEL_GRID -> STANDARD
        FULL_INFO -> STANDARD
    }
}

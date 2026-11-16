package com.example.camui

/**
 * Persisted HDMP selections for FujiFilm Mode 1.6.
 * AI 2 is the quality-first width64 model; AI 1.5 is the speed-first width32 model.
 */
internal object HdmpSelectionPolicy {
    val supportedOutputMegapixels = listOf(24, 48, 60, 108, 250)

    fun normalizeModel(value: String?): String = when (value?.lowercase()) {
        "off" -> "off"
        "ai15", "speed" -> "ai15"
        "luna", "lunadream", "luna_dream_2_0" -> "luna"
        else -> "ai2"
    }

    fun isAiEnabled(value: String?): Boolean =
        normalizeModel(value) == "ai2" || normalizeModel(value) == "ai15"

    fun isLunaDreamEnabled(value: String?): Boolean = normalizeModel(value) == "luna"

    fun normalizeOutputMegapixels(value: Int): Int =
        value.takeIf { it in supportedOutputMegapixels } ?: 60

    fun targetRenderSeconds(model: String): Int = when (normalizeModel(model)) {
        "ai15" -> 10
        "ai2" -> 20
        "luna" -> 60
        else -> 0
    }

    fun maximumRenderSeconds(model: String): Int = when (normalizeModel(model)) {
        "ai15" -> 20
        "ai2" -> 30
        "luna" -> 60
        else -> 0
    }
}

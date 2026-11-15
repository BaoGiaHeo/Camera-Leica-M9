package com.example.camui

/**
 * Persisted HDMP selections for FujiFilm Mode 1.6.
 * AI 2 is the quality-first width64 model; AI 1.5 is the speed-first width32 model.
 */
internal object HdmpSelectionPolicy {
    val supportedOutputMegapixels = listOf(24, 48, 60, 108, 250)

    fun normalizeModel(value: String?): String = when (value) {
        "ai15", "speed" -> "ai15"
        else -> "ai2"
    }

    fun normalizeOutputMegapixels(value: Int): Int =
        value.takeIf { it in supportedOutputMegapixels } ?: 60

    fun targetRenderSeconds(model: String): Int =
        if (normalizeModel(model) == "ai15") 10 else 20

    fun maximumRenderSeconds(model: String): Int =
        if (normalizeModel(model) == "ai15") 20 else 30
}

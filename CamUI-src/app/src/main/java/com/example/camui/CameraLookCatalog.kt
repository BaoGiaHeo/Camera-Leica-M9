package com.example.camui

/**
 * Pure built-in film-look definitions. Resource labels stay in CameraScreen;
 * this file deliberately has no Android or Compose dependency so the catalog
 * can be verified by local JVM tests.
 */
internal data class BuiltInCameraLookGroupDefinition(
    val id: String,
    val presets: List<String>
)

internal val builtInCameraLookGroupDefinitions = listOf(
    BuiltInCameraLookGroupDefinition("fujifilm", listOf(
        "Provia", "Velvia", "Astia", "Classic Chrome", "Classic Neg", "Eterna",
        "Pro Neg Std", "Pro Neg Hi", "Nostalgic Neg", "Reala Ace", "Bleach Bypass", "Acros"
    )),
    BuiltInCameraLookGroupDefinition("color_film", listOf(
        "Portra 160", "Portra 400", "Portra 800", "Gold 200", "Ektar 100",
        "Kodachrome 64", "ColorPlus 200", "CineStill 800T", "Vision3 250D",
        "Vision3 500T", "400H", "Ektachrome 100"
    )),
    BuiltInCameraLookGroupDefinition("classic", listOf(
        "Kodak Portra 400", "Agfa", "Kodak Gold", "Fuji 400"
    )),
    BuiltInCameraLookGroupDefinition("bw", listOf("Tri-X 400", "T-Max 400", "Monochrome")),
    BuiltInCameraLookGroupDefinition("leica", listOf("Leica Vivid", "Leica Natural", "Leica Mono")),
    BuiltInCameraLookGroupDefinition("hasselblad", listOf("Hasselblad HNCS")),
    BuiltInCameraLookGroupDefinition("zeiss", listOf("Zeiss Cool", "Zeiss Warm")),
    BuiltInCameraLookGroupDefinition("sony", listOf("Sony S-Cinetone", "Sony S-Log3")),
    BuiltInCameraLookGroupDefinition("cinema_log", listOf("Cine Tone", "Cinematic Log", "Soft Log")),
    BuiltInCameraLookGroupDefinition("basic", listOf("Vivid", "Natural", "Cool", "Warm"))
)

/** Stable names and category IDs for regression tests; imported custom LUTs are excluded. */
internal fun builtInCameraLookCatalog(): List<Pair<String, String>> =
    builtInCameraLookGroupDefinitions.flatMap { group -> group.presets.map { group.id to it } }

package com.example.camui

/**
 * Original look-group metadata retained as the source of the 46 selectable built-in looks.
 * Each definition is converted to a deterministic 3D LUT profile on a worker thread.
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

/** Stable names and category IDs for legacy regression tests. */
internal fun builtInCameraLookCatalog(): List<Pair<String, String>> =
    builtInCameraLookGroupDefinitions.flatMap { group -> group.presets.map { group.id to it } }

internal data class BuiltInCameraLutDefinition(
    val id: String,
    val name: String,
    val categoryId: String,
    val categoryName: String,
    /** "generated" marks a built-in 3D LUT profile generated from the app's colour recipe. */
    val archivePath: String
)

internal const val BUILT_IN_CAMERA_LUT_PREFIX = "builtin_camera_lut:"

internal fun builtInCameraLutPreset(id: String): String = BUILT_IN_CAMERA_LUT_PREFIX + id

internal fun builtInCameraLutId(preset: String): String? =
    preset.removePrefix(BUILT_IN_CAMERA_LUT_PREFIX)
        .takeIf { preset.startsWith(BUILT_IN_CAMERA_LUT_PREFIX) }

internal fun builtInCameraLutDefinition(id: String): BuiltInCameraLutDefinition? =
    builtInCameraLutDefinitions.firstOrNull { it.id == id }

private fun lookId(category: String, name: String): String =
    (category + "_" + name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_'))
        .trim('_')

/**
 * The 46 original in-app film looks remain selectable without an external ZIP asset.
 * A 3D LUT is deterministically generated from each profile name/category on a worker thread.
 */
internal val builtInCameraLutDefinitions: List<BuiltInCameraLutDefinition> =
    builtInCameraLookGroupDefinitions.flatMap { group ->
        group.presets.map { name ->
            BuiltInCameraLutDefinition(
                id = lookId(group.id, name),
                name = name,
                categoryId = group.id,
                categoryName = when (group.id) {
                    "fujifilm" -> "Fujifilm"
                    "color_film" -> "Phim màu"
                    "classic" -> "Cổ điển"
                    "bw" -> "Đen trắng"
                    "leica" -> "Leica"
                    "hasselblad" -> "Hasselblad"
                    "zeiss" -> "Zeiss"
                    "sony" -> "Sony"
                    "cinema_log" -> "Điện ảnh"
                    else -> "Cơ bản"
                },
                archivePath = "generated/" + group.id + "/" + lookId(group.id, name)
            )
        }
    }

internal fun builtInCameraLutCatalogIsValid(): Boolean =
    builtInCameraLutDefinitions.size == 46 &&
        builtInCameraLutDefinitions.map { it.id }.toSet().size == 46

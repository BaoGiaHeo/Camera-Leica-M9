package com.example.camui

/**
 * Legacy look-group metadata retained for UI compatibility. These names are not used as
 * selectable presets; selectable presets are supplied by the verified 41-entry LUT catalog.
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
    /** Exact relative entry path inside camera_lut_pack.zip. */
    val archivePath: String
)

internal const val BUILT_IN_CAMERA_LUT_PREFIX = "builtin_camera_lut:"

internal fun builtInCameraLutPreset(id: String): String = BUILT_IN_CAMERA_LUT_PREFIX + id

internal fun builtInCameraLutId(preset: String): String? =
    preset.removePrefix(BUILT_IN_CAMERA_LUT_PREFIX)
        .takeIf { preset.startsWith(BUILT_IN_CAMERA_LUT_PREFIX) }

internal fun builtInCameraLutDefinition(id: String): BuiltInCameraLutDefinition? =
    builtInCameraLutDefinitions.firstOrNull { it.id == id }

/**
 * Names are English translations of the seven supplied Chinese .cube filenames;
 * abbreviations in the .bin filenames are preserved rather than guessing expansions.
 */
internal val builtInCameraLutDefinitions = listOf(
    BuiltInCameraLutDefinition("fuji_acros", "Fujifilm Acros", "fujifilm", "Fujifilm", "lut_fuji_acros.bin"),
    BuiltInCameraLutDefinition("fuji_chrome", "Fujifilm Chrome", "fujifilm", "Fujifilm", "lut_fuji_chrome.bin"),
    BuiltInCameraLutDefinition("fuji_classic_negative", "Fujifilm Classic Negative", "fujifilm", "Fujifilm", "lut_fuji_cneg.bin"),
    BuiltInCameraLutDefinition("fuji_nostalgic_negative", "Fujifilm Nostalgic Negative", "fujifilm", "Fujifilm", "lut_fuji_nn.bin"),
    BuiltInCameraLutDefinition("fuji_pro_neg_hi", "Fujifilm Pro Neg Hi", "fujifilm", "Fujifilm", "lut_fuji_pnh.bin"),

    BuiltInCameraLutDefinition("hasselblad_hb", "Hasselblad HB", "hasselblad", "Hasselblad", "lut_hassel_hb.bin"),
    BuiltInCameraLutDefinition("hasselblad_hc", "Hasselblad HC", "hasselblad", "Hasselblad", "lut_hassel_hc.bin"),
    BuiltInCameraLutDefinition("hasselblad_hg", "Hasselblad HG", "hasselblad", "Hasselblad", "lut_hassel_hg.bin"),
    BuiltInCameraLutDefinition("hasselblad_hm", "Hasselblad HM", "hasselblad", "Hasselblad", "lut_hassel_hm.bin"),
    BuiltInCameraLutDefinition("hasselblad_hn", "Hasselblad HN", "hasselblad", "Hasselblad", "lut_hassel_hn.bin"),
    BuiltInCameraLutDefinition("hasselblad_hp", "Hasselblad HP", "hasselblad", "Hasselblad", "lut_hassel_hp.bin"),
    BuiltInCameraLutDefinition("hasselblad_hs", "Hasselblad HS", "hasselblad", "Hasselblad", "lut_hassel_hs.bin"),
    BuiltInCameraLutDefinition("hasselblad_hw", "Hasselblad HW", "hasselblad", "Hasselblad", "lut_hassel_hw.bin"),

    BuiltInCameraLutDefinition("kodak_100", "Kodak 100", "kodak", "Kodak", "lut_kodak_100.bin"),
    BuiltInCameraLutDefinition("kodak_200", "Kodak 200", "kodak", "Kodak", "lut_kodak_200.bin"),
    BuiltInCameraLutDefinition("kodak_400", "Kodak 400", "kodak", "Kodak", "lut_kodak_400.bin"),
    BuiltInCameraLutDefinition("kodak_64", "Kodak 64", "kodak", "Kodak", "lut_kodak_64.bin"),

    BuiltInCameraLutDefinition("arri_709", "ARRI 709", "cinema", "Cinema / ARRI", "lut_arri709.bin"),
    BuiltInCameraLutDefinition("arri_dune", "ARRI Dune", "cinema", "Cinema / ARRI", "lut_arridune.bin"),
    BuiltInCameraLutDefinition("arri_joker", "ARRI Joker", "cinema", "Cinema / ARRI", "lut_arrijoker.bin"),
    BuiltInCameraLutDefinition("arri_lala", "ARRI Lala", "cinema", "Cinema / ARRI", "lut_arrilala.bin"),
    BuiltInCameraLutDefinition("arri_reveal", "ARRI Reveal", "cinema", "Cinema / ARRI", "lut_arrireveal.bin"),
    BuiltInCameraLutDefinition("alexa", "ARRI Alexa", "cinema", "Cinema / ARRI", "lut_alexa.bin"),

    BuiltInCameraLutDefinition("r_bleach", "R Bleach", "creative_film", "Creative Film", "lut_r_bleach.bin"),
    BuiltInCameraLutDefinition("r_cross", "R Cross", "creative_film", "Creative Film", "lut_r_cross.bin"),
    BuiltInCameraLutDefinition("r_cross_2", "R Cross 2", "creative_film", "Creative Film", "lut_r_cross2.bin"),
    BuiltInCameraLutDefinition("r_hbw", "R HBW", "creative_film", "Creative Film", "lut_r_hbw.bin"),
    BuiltInCameraLutDefinition("r_negative", "R Negative", "creative_film", "Creative Film", "lut_r_neg.bin"),
    BuiltInCameraLutDefinition("r_positive", "R Positive", "creative_film", "Creative Film", "lut_r_pos.bin"),
    BuiltInCameraLutDefinition("r_retro", "R Retro", "creative_film", "Creative Film", "lut_r_retro.bin"),
    BuiltInCameraLutDefinition("r_soft", "R Soft", "creative_film", "Creative Film", "lut_r_soft.bin"),
    BuiltInCameraLutDefinition("r_standard", "R Standard", "creative_film", "Creative Film", "lut_r_std.bin"),
    BuiltInCameraLutDefinition("r_street", "R Street", "creative_film", "Creative Film", "lut_r_street.bin"),
    BuiltInCameraLutDefinition("p9_vibrant", "P9 Vibrant", "p9", "P9", "lut_p9vibrant.bin"),

    BuiltInCameraLutDefinition("hasselblad_classic_cube", "Hasselblad Classic", "hasselblad", "Hasselblad", "cubes/哈苏/哈苏 经典.cube"),
    BuiltInCameraLutDefinition("hasselblad_vivid_cube", "Hasselblad Vivid", "hasselblad", "Hasselblad", "cubes/哈苏/哈苏 鲜艳.cube"),
    BuiltInCameraLutDefinition("fujifilm_standard_cube", "Fujifilm Standard", "fujifilm", "Fujifilm", "cubes/富士/富士 标准.cube"),
    BuiltInCameraLutDefinition("fujifilm_classic_positive_cube", "Fujifilm Classic Positive", "fujifilm", "Fujifilm", "cubes/富士/富士 经典正片.cube"),
    BuiltInCameraLutDefinition("leica_cls_cube", "Leica CLS Film Look", "leica", "Leica", "cubes/徕卡/徕卡 CLS 胶片质感.cube"),
    BuiltInCameraLutDefinition("kodak_ultramax_400_cube", "Kodak Ultramax 400", "kodak", "Kodak", "cubes/柯达/柯达 Ultramax 400.cube"),
    BuiltInCameraLutDefinition("milk_tea_still_life_cube", "Milk Tea Still Life", "creative", "Creative", "cubes/风格/奶茶色静物.cube")
)

internal fun builtInCameraLutCatalogIsValid(): Boolean =
    builtInCameraLutDefinitions.size == 41 &&
        builtInCameraLutDefinitions.map { it.id }.toSet().size == 41 &&
        builtInCameraLutDefinitions.map { it.archivePath }.toSet().size == 41

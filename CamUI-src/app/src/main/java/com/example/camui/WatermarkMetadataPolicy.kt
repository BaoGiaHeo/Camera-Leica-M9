package com.example.camui

/**
 * Combines exposure EXIF and supplemental watermark fields.
 * The legacy EXIF master toggle gates only focal/aperture/shutter/ISO/EV;
 * explicitly enabled Filter, LD/NIGHT, Copyright and MP fields remain independent.
 */
internal fun combineWatermarkMetadataLines(
    exposureFields: List<String>,
    supplementalFields: List<String>,
    showExposureFields: Boolean
): String = buildList {
    if (showExposureFields) addAll(exposureFields)
    addAll(supplementalFields)
}.filter { it.isNotBlank() }
    .joinToString("  ·  ")

internal data class WatermarkExposureValues(
    val focal: String?,
    val aperture: String?,
    val shutter: String?,
    val iso: String?
)

internal fun visibleWatermarkExposureFields(
    values: WatermarkExposureValues,
    showExposureMaster: Boolean,
    showFocal: Boolean,
    showAperture: Boolean,
    showShutter: Boolean,
    showIso: Boolean
): List<String> = buildList {
    if (showExposureMaster && showFocal) values.focal?.let(::add)
    if (showExposureMaster && showAperture) values.aperture?.let(::add)
    if (showExposureMaster && showShutter) values.shutter?.let(::add)
    if (showExposureMaster && showIso) values.iso?.let { add("ISO $it") }
}.filter { it.isNotBlank() }

internal fun visibleWatermarkDataCardItems(
    values: WatermarkExposureValues,
    showExposureMaster: Boolean,
    showFocal: Boolean,
    showAperture: Boolean,
    showShutter: Boolean,
    showIso: Boolean
): List<Pair<String, String>> = buildList {
    if (showExposureMaster && showFocal) values.focal?.let { add("FOCAL" to it) }
    if (showExposureMaster && showAperture) values.aperture?.let { add("APERTURE" to it) }
    if (showExposureMaster && showShutter) values.shutter?.let { add("SHUTTER" to it) }
    if (showExposureMaster && showIso) values.iso?.let { add("ISO" to "ISO $it") }
}

internal fun visibleWatermarkExposureDialItems(
    values: WatermarkExposureValues,
    showExposureMaster: Boolean,
    showAperture: Boolean,
    showShutter: Boolean,
    showIso: Boolean
): List<Pair<String, String>> = buildList {
    if (showExposureMaster && showAperture) values.aperture?.let { add("F" to it) }
    if (showExposureMaster && showShutter) values.shutter?.let { add("SS" to it) }
    if (showExposureMaster && showIso) values.iso?.let { add("ISO" to it) }
}

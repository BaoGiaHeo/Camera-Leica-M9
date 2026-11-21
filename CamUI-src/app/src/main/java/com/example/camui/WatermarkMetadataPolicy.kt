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

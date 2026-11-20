package com.example.camui

import java.util.Locale

private val knownDeviceMarketingNames = mapOf(
    "25053rt47c" to "REDMI Turbo 4 Pro"
)

/**
 * Resolves a human-readable camera model label in the priority requested by the
 * watermark specification. The caller reads Android system properties/settings;
 * this pure function keeps the precedence deterministic and unit-testable.
 */
internal fun resolveWatermarkDeviceName(
    customName: String?,
    marketName: String?,
    globalDeviceName: String?,
    manufacturer: String?,
    model: String?
): String {
    customName.cleanWatermarkName()?.let { return it }
    marketName.cleanWatermarkName()?.let { return it }
    globalDeviceName.cleanWatermarkName()?.let { return it }

    val safeManufacturer = manufacturer.cleanWatermarkName()
    val safeModel = model.cleanWatermarkName()
    val modelKey = safeModel?.filter(Char::isLetterOrDigit)?.lowercase(Locale.ROOT)
    modelKey?.let(knownDeviceMarketingNames::get)?.let { return it }

    if (safeModel == null) return safeManufacturer ?: "Camera"
    val modelWithoutManufacturer = if (
        safeManufacturer != null && safeModel.startsWith(safeManufacturer, ignoreCase = true)
    ) {
        safeModel.drop(safeManufacturer.length).trim()
    } else {
        safeModel
    }
    return listOfNotNull(safeManufacturer, modelWithoutManufacturer.takeIf { it.isNotBlank() })
        .joinToString(" ")
        .ifBlank { "Camera" }
}

private fun String?.cleanWatermarkName(): String? =
    this?.trim()?.replace(Regex("\\s+"), " ")?.takeIf { it.isNotBlank() }

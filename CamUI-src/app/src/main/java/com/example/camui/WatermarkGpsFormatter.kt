package com.example.camui

import java.util.Locale

/**
 * Converts EXIF GPS degree/minute/second rational coordinates into a compact
 * decimal latitude/longitude line. Missing or malformed tags are omitted.
 */
internal fun formatWatermarkGpsCoordinates(attributes: Map<String, String>): String? {
    fun coordinate(valueTag: String, refTag: String, negativeRefs: Set<String>): Double? {
        val raw = attributes[valueTag] ?: return null
        val components = raw.split(',').map { token ->
            val part = token.trim()
            val separator = part.indexOf('/')
            val numerator = if (separator >= 0) part.substring(0, separator).toDoubleOrNull()
                else part.toDoubleOrNull()
            val denominator = if (separator >= 0) part.substring(separator + 1).toDoubleOrNull()
                else 1.0
            if (numerator == null || denominator == null || denominator == 0.0) null
            else numerator / denominator
        }
        if (components.size != 3 || components.any { it == null || !it.isFinite() }) return null
        val degrees = components[0]!!
        val minutes = components[1]!!
        val seconds = components[2]!!
        if (degrees < 0.0 || minutes !in 0.0..60.0 || seconds !in 0.0..60.0) return null
        val magnitude = degrees + minutes / 60.0 + seconds / 3600.0
        val maximumDegrees = if (valueTag == "GPSLatitude") 90.0 else 180.0
        if (magnitude > maximumDegrees) return null
        val reference = attributes[refTag]?.trim()?.uppercase(Locale.ROOT) ?: return null
        return if (reference in negativeRefs) -magnitude else magnitude
    }

    val latitude = coordinate("GPSLatitude", "GPSLatitudeRef", setOf("S")) ?: return null
    val longitude = coordinate("GPSLongitude", "GPSLongitudeRef", setOf("W")) ?: return null
    return String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
}

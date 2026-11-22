package com.example.camui

import kotlin.math.min
import kotlin.math.roundToInt

internal enum class WatermarkLayoutType(val id: String) {
    BAR("A"),
    CAPTION("B"),
    GALLERY("C"),
    BLACK("D"),
    DOT_LIGHT("E"),
    DOT_DARK("F"),
    SPLIT("G"),
    PALETTE("H");

    companion object {
        fun fromPreference(value: String?): WatermarkLayoutType = when (value?.trim()?.lowercase()) {
            "caption", "b" -> CAPTION
            "gallery", "c", "d" -> GALLERY
            "black", "d-black" -> BLACK
            "dot_light", "dot-light", "dotlight", "e" -> DOT_LIGHT
            "dot_dark", "dot-dark", "dotdark", "f" -> DOT_DARK
            "split", "g" -> SPLIT
            "palette", "h" -> PALETTE
            // Backward compatibility: the retired CARD preset migrates to DOT LIGHT.
            "card", "c-card" -> DOT_LIGHT
            else -> BAR
        }

        fun preferenceValue(type: WatermarkLayoutType): String = when (type) {
            BAR -> "bar"
            CAPTION -> "caption"
            GALLERY -> "gallery"
            BLACK -> "black"
            DOT_LIGHT -> "dot_light"
            DOT_DARK -> "dot_dark"
            SPLIT -> "split"
            PALETTE -> "palette"
        }
    }
}

internal data class WatermarkFieldVisibility(
    val cameraName: Boolean = true,
    val lens: Boolean = true,
    val focalLength: Boolean = true,
    val aperture: Boolean = true,
    val shutter: Boolean = true,
    val iso: Boolean = true,
    val date: Boolean = true,
    val time: Boolean = false,
    val location: Boolean = true,
    val filter: Boolean = true
)

internal data class WatermarkLayoutSettings(
    val layout: WatermarkLayoutType = WatermarkLayoutType.BAR,
    val sizeStep: Int = 0,
    val invert: Boolean = false,
    val fields: WatermarkFieldVisibility = WatermarkFieldVisibility(),
    val locationEnabled: Boolean = true,
    val locationMode: String = "PLACE",
    val useTrueFocalLength: Boolean = false,
    val customCameraName: String = "",
    val saveMode: String = "watermarked"
) {
    val sizeScale: Float get() = 1f + sizeStep.coerceIn(-2, 2) * 0.05f
}

internal data class WatermarkPhotoMetadata(
    val cameraName: String = "",
    val lensName: String = "",
    val focal35mm: String = "",
    val trueFocalLength: String = "",
    val aperture: String = "",
    val shutter: String = "",
    val iso: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val coordinates: String = "",
    val filterName: String = "",
    val filterCategory: String = "Fujifilm",
    val filterBadge: String = "",
    val title: String = "",
    val author: String = ""
) {
    fun focal(settings: WatermarkLayoutSettings): String =
        if (settings.useTrueFocalLength) trueFocalLength.ifBlank { focal35mm }
        else focal35mm.ifBlank { trueFocalLength }

    fun visibleLocation(settings: WatermarkLayoutSettings): String =
        if (!settings.locationEnabled || !settings.fields.location) "" else
            if (settings.locationMode.equals("COORDS", true)) coordinates.ifBlank { location }
            else location.ifBlank { coordinates }
}

internal data class WatermarkRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) * 0.5f
    val centerY: Float get() = (top + bottom) * 0.5f
}

internal data class WatermarkLayoutGeometry(
    val layout: WatermarkLayoutType,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val shortSide: Float,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val photoRect: WatermarkRect,
    val footerRect: WatermarkRect? = null,
    val paletteRect: WatermarkRect? = null,
    val metadataRect: WatermarkRect? = null,
    val logoRect: WatermarkRect? = null,
    val badgeRect: WatermarkRect? = null,
    val visibleTextLines: Int = 1,
    val paletteBelowText: Boolean = false
) {
    val outputAspectRatio: Float get() = canvasWidth.toFloat() / canvasHeight.coerceAtLeast(1)
    val outputRatioLabel: String get() = canvasWidth.toString() + ":" + canvasHeight.toString()
}

internal object WatermarkGeometry {
    fun build(
        sourceWidth: Int,
        sourceHeight: Int,
        settings: WatermarkLayoutSettings,
        metadata: WatermarkPhotoMetadata
    ): WatermarkLayoutGeometry {
        require(sourceWidth > 0 && sourceHeight > 0)
        val s = min(sourceWidth, sourceHeight).toFloat()
        val scale = settings.sizeScale
        val lineCount = lineCount(metadata, settings)
        val photoBounds = WatermarkRect(0f, 0f, sourceWidth.toFloat(), sourceHeight.toFloat())

        return when (settings.layout) {
            WatermarkLayoutType.BAR -> {
                val barHeight = (s * (10.35f + (lineCount - 1) * 4.06f) * scale / 100f)
                    .roundToInt().coerceAtLeast(1)
                val footer = WatermarkRect(
                    0f, sourceHeight.toFloat(), sourceWidth.toFloat(),
                    (sourceHeight + barHeight).toFloat()
                )
                val logoHeight = s * 0.03f * scale
                val logoWidth = logoHeight * 6.2f
                val logoLeft = s * 0.068f
                WatermarkLayoutGeometry(
                    layout = WatermarkLayoutType.BAR,
                    sourceWidth = sourceWidth, sourceHeight = sourceHeight, shortSide = s,
                    canvasWidth = sourceWidth, canvasHeight = sourceHeight + barHeight,
                    photoRect = photoBounds,
                    footerRect = footer,
                    metadataRect = WatermarkRect(
                        sourceWidth * 0.56f, footer.top,
                        sourceWidth - s * 0.063f, footer.bottom
                    ),
                    logoRect = WatermarkRect(
                        logoLeft, footer.centerY - logoHeight / 2f,
                        logoLeft + logoWidth, footer.centerY + logoHeight / 2f
                    ),
                    visibleTextLines = lineCount
                )
            }

            WatermarkLayoutType.CAPTION -> {
                val side = s * 0.041f * scale
                val lower = s * 0.23f * scale
                val cw = sourceWidth + (side * 2f).roundToInt()
                val ch = sourceHeight + side.roundToInt() + lower.roundToInt()
                val photo = WatermarkRect(side, side, side + sourceWidth, side + sourceHeight)
                val footer = WatermarkRect(0f, photo.bottom, cw.toFloat(), ch.toFloat())
                WatermarkLayoutGeometry(
                    layout = WatermarkLayoutType.CAPTION,
                    sourceWidth = sourceWidth, sourceHeight = sourceHeight, shortSide = s,
                    canvasWidth = cw, canvasHeight = ch,
                    photoRect = photo, footerRect = footer, metadataRect = footer,
                    visibleTextLines = lineCount
                )
            }

            WatermarkLayoutType.GALLERY -> {
                val side = s * 0.05f * scale
                val lower = s * 0.246f * scale
                val cw = sourceWidth + (side * 2f).roundToInt()
                val ch = sourceHeight + side.roundToInt() + lower.roundToInt()
                val photo = WatermarkRect(side, side, side + sourceWidth, side + sourceHeight)
                val footer = WatermarkRect(0f, photo.bottom, cw.toFloat(), ch.toFloat())
                val logoH = s * 0.039f * scale
                val logoW = logoH * 6.2f
                val logoCenterY = photo.bottom + s * 0.074f * scale
                val logo = WatermarkRect(
                    (cw - logoW) / 2f, logoCenterY - logoH / 2f,
                    (cw + logoW) / 2f, logoCenterY + logoH / 2f
                )
                WatermarkLayoutGeometry(
                    layout = WatermarkLayoutType.GALLERY,
                    sourceWidth = sourceWidth, sourceHeight = sourceHeight, shortSide = s,
                    canvasWidth = cw, canvasHeight = ch,
                    photoRect = photo, footerRect = footer, metadataRect = footer,
                    logoRect = logo, visibleTextLines = lineCount
                )
            }

            WatermarkLayoutType.BLACK -> {
                val side = s * 0.032f * scale
                val lower = s * 0.14f * scale
                val cw = sourceWidth + (side * 2f).roundToInt()
                val ch = sourceHeight + side.roundToInt() + lower.roundToInt()
                val photo = WatermarkRect(side, side, side + sourceWidth, side + sourceHeight)
                val footer = WatermarkRect(0f, photo.bottom, cw.toFloat(), ch.toFloat())
                val badge = s * 0.076f * scale
                val bx = side + s * 0.01f
                val badgeRect = WatermarkRect(
                    bx, footer.centerY - badge / 2f,
                    bx + badge, footer.centerY + badge / 2f
                )
                WatermarkLayoutGeometry(
                    layout = WatermarkLayoutType.BLACK,
                    sourceWidth = sourceWidth, sourceHeight = sourceHeight, shortSide = s,
                    canvasWidth = cw, canvasHeight = ch,
                    photoRect = photo, footerRect = footer, metadataRect = footer,
                    badgeRect = badgeRect, visibleTextLines = lineCount
                )
            }

            WatermarkLayoutType.DOT_LIGHT,
            WatermarkLayoutType.DOT_DARK -> {
                val border = s * 0.024f * scale
                val footerHeight = s * 0.176f * scale
                val cw = sourceWidth + (border * 2f).roundToInt()
                val ch = sourceHeight + border.roundToInt() + footerHeight.roundToInt()
                val photo = WatermarkRect(
                    border, border, border + sourceWidth, border + sourceHeight
                )
                val footer = WatermarkRect(0f, photo.bottom, cw.toFloat(), ch.toFloat())
                val logoWidth = s * 0.182f * scale
                val logoHeight = logoWidth / 6.2f
                val logoLeft = photo.left + s * 0.053f * scale
                val logo = WatermarkRect(
                    logoLeft, footer.centerY - logoHeight / 2f,
                    logoLeft + logoWidth, footer.centerY + logoHeight / 2f
                )
                val dot = s * 0.032f * scale
                val specRight = photo.right - s * 0.054f * scale
                val specWidth = s * 0.54f * scale
                val dotCenterX = specRight - specWidth - s * 0.029f * scale - dot / 2f
                val dotRect = WatermarkRect(
                    dotCenterX - dot / 2f, footer.centerY - dot / 2f,
                    dotCenterX + dot / 2f, footer.centerY + dot / 2f
                )
                WatermarkLayoutGeometry(
                    layout = settings.layout,
                    sourceWidth = sourceWidth, sourceHeight = sourceHeight, shortSide = s,
                    canvasWidth = cw, canvasHeight = ch,
                    photoRect = photo, footerRect = footer,
                    metadataRect = WatermarkRect(
                        dotRect.right + s * 0.029f * scale, footer.top,
                        specRight, footer.bottom
                    ),
                    logoRect = logo, badgeRect = dotRect,
                    visibleTextLines = lineCount
                )
            }

            WatermarkLayoutType.SPLIT -> {
                val footerHeight = s * 0.11f * scale
                val ch = sourceHeight + footerHeight.roundToInt()
                val photo = photoBounds
                val footer = WatermarkRect(0f, sourceHeight.toFloat(), sourceWidth.toFloat(), ch.toFloat())
                val logoWidth = s * 0.15f * scale
                val logoHeight = logoWidth / 6.2f
                val specsWidth = sourceWidth * 0.38f
                val separatorWidth = maxOf(1f, s * 0.0012f * scale)
                val gapBeforeSeparator = s * 0.022f * scale
                val gapAfterSeparator = s * 0.022f * scale
                val rightInset = s * 0.0575f * scale
                val logoRight = sourceWidth - rightInset - specsWidth - gapAfterSeparator -
                    separatorWidth - gapBeforeSeparator
                val logoLeft = logoRight - logoWidth
                val logo = WatermarkRect(
                    logoLeft, footer.centerY - logoHeight / 2f,
                    logoRight, footer.centerY + logoHeight / 2f
                )
                WatermarkLayoutGeometry(
                    layout = WatermarkLayoutType.SPLIT,
                    sourceWidth = sourceWidth, sourceHeight = sourceHeight, shortSide = s,
                    canvasWidth = sourceWidth, canvasHeight = ch,
                    photoRect = photo, footerRect = footer,
                    metadataRect = WatermarkRect(
                        s * 0.044f * scale, footer.top,
                        sourceWidth - rightInset, footer.bottom
                    ),
                    logoRect = logo, visibleTextLines = lineCount
                )
            }

            WatermarkLayoutType.PALETTE -> {
                val side = s * 0.045f * scale
                val top = s * 0.269f * scale
                val bottom = s * 0.295f * scale
                val cw = sourceWidth + (side * 2f).roundToInt()
                val ch = sourceHeight + top.roundToInt() + bottom.roundToInt()
                val photo = WatermarkRect(
                    side, top, side + sourceWidth, top + sourceHeight
                )
                val footer = WatermarkRect(0f, photo.bottom, cw.toFloat(), ch.toFloat())
                val logoWidth = s * 0.316f * scale
                val logoHeight = logoWidth / 6.2f
                val logoTop = s * 0.082f * scale
                val logo = WatermarkRect(
                    photo.left, logoTop, photo.left + logoWidth, logoTop + logoHeight
                )
                val paletteBelow = sourceHeight >= sourceWidth * 0.9f
                val paletteWidth = if (paletteBelow) sourceWidth.toFloat()
                    else min(s * 0.72f * scale, sourceWidth * 0.48f)
                val paletteHeight = s * (if (paletteBelow) 0.045f else 0.057f) * scale
                val paletteTop = photo.bottom + s * (if (paletteBelow) 0.25f else 0.075f) * scale
                val paletteLeft = if (paletteBelow) photo.left else photo.right - paletteWidth
                val paletteRect = WatermarkRect(
                    paletteLeft, paletteTop, paletteLeft + paletteWidth, paletteTop + paletteHeight
                )
                WatermarkLayoutGeometry(
                    layout = WatermarkLayoutType.PALETTE,
                    sourceWidth = sourceWidth, sourceHeight = sourceHeight, shortSide = s,
                    canvasWidth = cw, canvasHeight = ch,
                    photoRect = photo, footerRect = footer, paletteRect = paletteRect,
                    metadataRect = footer, logoRect = logo,
                    paletteBelowText = paletteBelow,
                    visibleTextLines = lineCount
                )
            }
        }
    }

    fun buildSpecs(metadata: WatermarkPhotoMetadata, settings: WatermarkLayoutSettings): String {
        val fields = settings.fields
        return buildList {
            if (fields.focalLength) metadata.focal(settings).takeIf { it.isNotBlank() }?.let(::add)
            if (fields.aperture) metadata.aperture.takeIf { it.isNotBlank() }?.let(::add)
            if (fields.shutter) metadata.shutter.takeIf { it.isNotBlank() }?.let(::add)
            if (fields.iso) metadata.iso.takeIf { it.isNotBlank() }?.let {
                add(if (it.startsWith("ISO", true)) it else "ISO $it")
            }
        }.joinToString(" · ")
    }

    fun lineCount(metadata: WatermarkPhotoMetadata, settings: WatermarkLayoutSettings): Int {
        val specs = buildSpecs(metadata, settings)
        val model = metadata.cameraName.takeIf { settings.fields.cameraName }.orEmpty()
        val location = metadata.visibleLocation(settings)
        return listOf(specs, model, location).count { it.isNotBlank() }.coerceAtLeast(1)
    }
}

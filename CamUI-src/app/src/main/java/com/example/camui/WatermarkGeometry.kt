package com.example.camui

import kotlin.math.roundToInt

internal enum class WatermarkLayoutType(val id: String) {
    BAR("A"), CAPTION("B"), CARD("C"), GALLERY("D"), BLACK("E");

    companion object {
        fun fromPreference(value: String?): WatermarkLayoutType = when (value?.lowercase()) {
            "caption", "b" -> CAPTION
            "card", "c" -> CARD
            "gallery", "d" -> GALLERY
            "black", "e" -> BLACK
            else -> BAR
        }
        fun preferenceValue(type: WatermarkLayoutType): String = when (type) {
            BAR -> "bar"
            CAPTION -> "caption"
            CARD -> "card"
            GALLERY -> "gallery"
            BLACK -> "black"
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
    val saveMode: String = "watermarked",
    val storyNineSixteen: Boolean = false
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
    val filterBadge: String = ""
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
    val footerRect: WatermarkRect?,
    val cardRect: WatermarkRect?,
    val metadataRect: WatermarkRect?,
    val logoRect: WatermarkRect?,
    val badgeRect: WatermarkRect?,
    val visibleTextLines: Int
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
        val s = minOf(sourceWidth, sourceHeight).toFloat()
        val scale = settings.sizeScale
        val lineCount = lineCount(metadata, settings)

        return when (settings.layout) {
            WatermarkLayoutType.BAR -> {
                val barHeight = (s * (10.35f + (lineCount - 1) * 4.06f) * scale / 100f)
                    .roundToInt().coerceAtLeast(1)
                val footer = WatermarkRect(0f, sourceHeight.toFloat(), sourceWidth.toFloat(), (sourceHeight + barHeight).toFloat())
                val logoHeight = s * 0.03f * scale
                val logoWidth = logoHeight * 6.2f
                val logoLeft = s * 0.068f
                WatermarkLayoutGeometry(
                    WatermarkLayoutType.BAR, sourceWidth, sourceHeight, s, sourceWidth, sourceHeight + barHeight,
                    WatermarkRect(0f, 0f, sourceWidth.toFloat(), sourceHeight.toFloat()),
                    footer, null,
                    WatermarkRect(sourceWidth * 0.56f, footer.top, sourceWidth - s * 0.063f, footer.bottom),
                    WatermarkRect(logoLeft, footer.centerY - logoHeight / 2f, logoLeft + logoWidth, footer.centerY + logoHeight / 2f),
                    null, lineCount
                )
            }
            WatermarkLayoutType.CAPTION -> {
                val side = s * 0.041f * scale
                val top = side
                val lower = s * 0.23f * scale
                val cw = sourceWidth + (side * 2f).roundToInt()
                val ch = sourceHeight + side.roundToInt() + lower.roundToInt()
                val photo = WatermarkRect(side, top, side + sourceWidth, top + sourceHeight)
                val footer = WatermarkRect(0f, photo.bottom, cw.toFloat(), ch.toFloat())
                WatermarkLayoutGeometry(
                    WatermarkLayoutType.CAPTION, sourceWidth, sourceHeight, s, cw, ch,
                    photo, footer, null, footer, null, null, lineCount
                )
            }
            WatermarkLayoutType.CARD -> {
                val pad = s * 0.037f * scale
                val band = s * 0.287f * scale
                val cardW = sourceWidth + 2f * pad
                val cardH = sourceHeight + pad + band
                val outer = cardW * 0.10f
                val cw = if (settings.storyNineSixteen) (s * 9f / 16f).roundToInt() else (cardW + outer * 2f).roundToInt()
                val ch = if (settings.storyNineSixteen) s.roundToInt() else (cardH + outer * 2f).roundToInt()
                val cardLeft = if (settings.storyNineSixteen) ((cw - cardW).coerceAtLeast(0) * 0.5f) else outer
                val cardTop = if (settings.storyNineSixteen) ((ch - cardH).coerceAtLeast(0) * 0.5f) else outer
                val card = WatermarkRect(cardLeft, cardTop, cardLeft + cardW, cardTop + cardH)
                val photo = WatermarkRect(card.left + pad, card.top + pad, card.left + pad + sourceWidth, card.top + pad + sourceHeight)
                val meta = WatermarkRect(photo.left, photo.bottom, photo.right, photo.bottom + band)
                val logoHeight = s * 0.035f * scale
                val logoWidth = logoHeight * 6.2f
                val logoLeft = photo.left + s * 0.049f
                val logoTop = meta.top + (band * 0.62f) - logoHeight / 2f
                WatermarkLayoutGeometry(
                    WatermarkLayoutType.CARD, sourceWidth, sourceHeight, s, cw, ch,
                    photo, null, card, meta,
                    WatermarkRect(logoLeft, logoTop, logoLeft + logoWidth, logoTop + logoHeight),
                    null, lineCount
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
                val logo = WatermarkRect((cw - logoW) / 2f, logoCenterY - logoH / 2f, (cw + logoW) / 2f, logoCenterY + logoH / 2f)
                WatermarkLayoutGeometry(
                    WatermarkLayoutType.GALLERY, sourceWidth, sourceHeight, s, cw, ch,
                    photo, footer, null, footer, logo, null, lineCount
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
                val badgeRect = WatermarkRect(bx, footer.centerY - badge / 2f, bx + badge, footer.centerY + badge / 2f)
                WatermarkLayoutGeometry(
                    WatermarkLayoutType.BLACK, sourceWidth, sourceHeight, s, cw, ch,
                    photo, footer, null, footer, null, badgeRect, lineCount
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

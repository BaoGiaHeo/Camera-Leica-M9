package com.example.camui

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.InflaterInputStream
import kotlin.math.roundToInt

private const val HALD_MAX_EDGE = 2048
private const val HALD_MAX_INPUT_BYTES = 64 * 1024 * 1024
private const val HALD_MAX_DECODED_BYTES = 64 * 1024 * 1024

/** Panasonic VariCam VLT v1.0: 17³ RGB integer triplets, R changes fastest. */
internal fun parsePanasonicVlt(bytes: ByteArray): CubeLut {
    require(bytes.isNotEmpty() && bytes.size <= HALD_MAX_INPUT_BYTES) {
        "Panasonic VLT file is empty or exceeds the import size limit."
    }
    val lines = String(bytes, StandardCharsets.US_ASCII).removePrefix("\uFEFF").lineSequence().toList()
    require(lines.size >= 5 && lines[0].trim().equals(
        "# Panasonic VLT file version 1.0", ignoreCase = true
    )) { "Unsupported VLT header. Expected Panasonic VLT v1.0 ASCII." }
    require(lines[1].trim().startsWith("# Source VLT file ", ignoreCase = true)) {
        "Panasonic VLT source-file header is missing."
    }
    val match = Regex("""^\s*LUT_3D_SIZE\s+(\d+)\s*$""", RegexOption.IGNORE_CASE)
        .matchEntire(lines[2])
    val size = match?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?: error("Panasonic VLT header has no valid LUT_3D_SIZE.")
    require(size == 17) { "Panasonic VLT v1.0 must contain a 17×17×17 grid." }
    val values = FloatArray(size * size * size * 3)
    var row = 0
    for ((lineIndex, raw) in lines.drop(4).withIndex()) {
        val line = raw.substringBefore("#").trim()
        if (line.isEmpty()) continue
        val parts = line.split(Regex("\\s+"))
        require(parts.size == 3) {
            "VLT line " + (lineIndex + 5) + " must contain exactly three RGB channels."
        }
        require(row < size * size * size) { "VLT contains more than 4,913 RGB triplets." }
        for (channel in 0..2) {
            val value = parts[channel].toIntOrNull() ?: error("VLT channels must be integers.")
            require(value in 0..4095) { "VLT channels must be in the range 0..4095." }
            values[row * 3 + channel] = value / 4095f
        }
        row++
    }
    require(row == size * size * size) {
        "VLT expected " + (size * size * size) + " RGB triplets but received " + row + "."
    }
    return CubeLut(
        size = size,
        domainMin = floatArrayOf(0f, 0f, 0f),
        domainMax = floatArrayOf(1f, 1f, 1f),
        values = values,
        redFastest = true
    )
}

private fun readPngUInt32(bytes: ByteArray, offset: Int): Long {
    require(offset >= 0 && offset + 4 <= bytes.size) { "PNG chunk is truncated." }
    return ((bytes[offset].toLong() and 0xFF) shl 24) or
        ((bytes[offset + 1].toLong() and 0xFF) shl 16) or
        ((bytes[offset + 2].toLong() and 0xFF) shl 8) or
        (bytes[offset + 3].toLong() and 0xFF)
}

private fun paethPredictor(a: Int, b: Int, c: Int): Int {
    val p = a + b - c
    val pa = kotlin.math.abs(p - a)
    val pb = kotlin.math.abs(p - b)
    val pc = kotlin.math.abs(p - c)
    return when {
        pa <= pb && pa <= pc -> a
        pb <= pc -> b
        else -> c
    }
}

private fun resampleHaldCube(source: FloatArray, sourceSize: Int, outputSize: Int): FloatArray {
    require(sourceSize >= outputSize && outputSize >= 2)
    val output = FloatArray(outputSize * outputSize * outputSize * 3)
    fun value(red: Int, green: Int, blue: Int, channel: Int): Float =
        source[((blue * sourceSize + green) * sourceSize + red) * 3 + channel]
    fun mix(a: Float, b: Float, amount: Float): Float = a + (b - a) * amount
    for (blue in 0 until outputSize) {
        val bp = blue.toFloat() * (sourceSize - 1) / (outputSize - 1)
        val b0 = bp.toInt().coerceIn(0, sourceSize - 1)
        val b1 = (b0 + 1).coerceAtMost(sourceSize - 1)
        val bt = bp - b0
        for (green in 0 until outputSize) {
            val gp = green.toFloat() * (sourceSize - 1) / (outputSize - 1)
            val g0 = gp.toInt().coerceIn(0, sourceSize - 1)
            val g1 = (g0 + 1).coerceAtMost(sourceSize - 1)
            val gt = gp - g0
            for (red in 0 until outputSize) {
                val rp = red.toFloat() * (sourceSize - 1) / (outputSize - 1)
                val r0 = rp.toInt().coerceIn(0, sourceSize - 1)
                val r1 = (r0 + 1).coerceAtMost(sourceSize - 1)
                val rt = rp - r0
                val destination = ((blue * outputSize + green) * outputSize + red) * 3
                for (channel in 0..2) {
                    val c000 = value(r0, g0, b0, channel)
                    val c001 = value(r1, g0, b0, channel)
                    val c010 = value(r0, g1, b0, channel)
                    val c011 = value(r1, g1, b0, channel)
                    val c100 = value(r0, g0, b1, channel)
                    val c101 = value(r1, g0, b1, channel)
                    val c110 = value(r0, g1, b1, channel)
                    val c111 = value(r1, g1, b1, channel)
                    val low = mix(mix(c000, c001, rt), mix(c010, c011, rt), gt)
                    val high = mix(mix(c100, c101, rt), mix(c110, c111, rt), gt)
                    output[destination + channel] = mix(low, high, bt).coerceIn(0f, 1f)
                }
            }
        }
    }
    return output
}

/** Hald CLUT PNG decoder: RGB/RGBA, 8/16-bit, non-interlaced, PNG filters 0..4. */
internal fun parseHaldPng(bytes: ByteArray): CubeLut {
    require(bytes.size in 57..HALD_MAX_INPUT_BYTES) {
        "Hald PNG is empty or exceeds the import size limit."
    }
    val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    require(bytes.copyOfRange(0, 8).contentEquals(signature)) { "Invalid PNG signature." }
    var width = 0
    var height = 0
    var bitDepth = 0
    var colorType = -1
    var sawHeader = false
    var sawEnd = false
    val idat = ByteArrayOutputStream()
    var position = 8
    while (position + 12 <= bytes.size) {
        val lengthLong = readPngUInt32(bytes, position)
        require(lengthLong <= HALD_MAX_INPUT_BYTES.toLong()) { "PNG chunk exceeds safe size." }
        val length = lengthLong.toInt()
        val dataOffset = position + 8
        val crcOffset = dataOffset + length
        require(crcOffset + 4 <= bytes.size) { "PNG chunk is truncated." }
        val type = String(bytes, position + 4, 4, StandardCharsets.US_ASCII)
        when (type) {
            "IHDR" -> {
                require(!sawHeader && length == 13) { "Invalid PNG IHDR." }
                val w = readPngUInt32(bytes, dataOffset)
                val h = readPngUInt32(bytes, dataOffset + 4)
                require(w in 1..HALD_MAX_EDGE.toLong() && h in 1..HALD_MAX_EDGE.toLong()) {
                    "Hald PNG dimensions must not exceed 2,048×2,048."
                }
                width = w.toInt()
                height = h.toInt()
                bitDepth = bytes[dataOffset + 8].toInt() and 0xFF
                colorType = bytes[dataOffset + 9].toInt() and 0xFF
                val compression = bytes[dataOffset + 10].toInt() and 0xFF
                val filterMethod = bytes[dataOffset + 11].toInt() and 0xFF
                val interlace = bytes[dataOffset + 12].toInt() and 0xFF
                require(compression == 0 && filterMethod == 0) { "Unsupported PNG compression/filter method." }
                require(bitDepth == 8 || bitDepth == 16) { "Hald PNG must be 8-bit or 16-bit." }
                require(colorType == 2 || colorType == 6) { "Hald PNG must be RGB or RGBA." }
                require(interlace == 0) { "Interlaced Hald PNG is not supported." }
                sawHeader = true
            }
            "IDAT" -> {
                require(sawHeader && !sawEnd) { "PNG IDAT chunk is out of order." }
                require(idat.size().toLong() + length <= HALD_MAX_INPUT_BYTES.toLong()) {
                    "Compressed PNG data exceeds the safe size limit."
                }
                idat.write(bytes, dataOffset, length)
            }
            "IEND" -> {
                require(length == 0) { "Invalid PNG IEND." }
                sawEnd = true
                position = crcOffset + 4
                break
            }
        }
        position = crcOffset + 4
    }
    require(sawHeader && sawEnd && idat.size() > 0) { "PNG is missing IHDR, IDAT or IEND." }
    require(width == height) { "Hald CLUT image must be square." }
    val level = Math.cbrt(width.toDouble()).roundToInt()
    require(level >= 2 && level * level * level == width) {
        "Hald PNG edge length must be a perfect cube (L³)."
    }
    val sourceSize = level * level
    val pixelCount = width.toLong() * height.toLong()
    require(pixelCount == sourceSize.toLong() * sourceSize * sourceSize) {
        "Hald PNG dimensions do not match a 3D cube."
    }

    val channels = if (colorType == 6) 4 else 3
    val bytesPerSample = bitDepth / 8
    val bytesPerPixel = channels * bytesPerSample
    val rowBytes = width * bytesPerPixel
    val decodedLength = height.toLong() * (rowBytes + 1L)
    require(decodedLength <= HALD_MAX_DECODED_BYTES.toLong()) {
        "Decompressed Hald PNG exceeds 64 MB."
    }
    val decompressed = InflaterInputStream(ByteArrayInputStream(idat.toByteArray())).use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size().toLong() + count <= decodedLength) {
                "PNG expands beyond the scanline size declared in IHDR."
            }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }
    require(decompressed.size == decodedLength.toInt()) { "PNG scanline length does not match IHDR." }

    val sourceValues = FloatArray(pixelCount.toInt() * 3)
    var previous = ByteArray(rowBytes)
    var current = ByteArray(rowBytes)
    var offset = 0
    for (y in 0 until height) {
        val filter = decompressed[offset++].toInt() and 0xFF
        for (index in 0 until rowBytes) {
            val raw = decompressed[offset++].toInt() and 0xFF
            val left = if (index >= bytesPerPixel) current[index - bytesPerPixel].toInt() and 0xFF else 0
            val up = previous[index].toInt() and 0xFF
            val upLeft = if (index >= bytesPerPixel) previous[index - bytesPerPixel].toInt() and 0xFF else 0
            val predictor = when (filter) {
                0 -> 0
                1 -> left
                2 -> up
                3 -> (left + up) / 2
                4 -> paethPredictor(left, up, upLeft)
                else -> error("Unsupported PNG filter type $filter.")
            }
            current[index] = ((raw + predictor) and 0xFF).toByte()
        }
        val pixelBase = y * width * 3
        for (x in 0 until width) {
            // current is a single, unfiltered scanline; do not add the global row offset here.
            val source = x * bytesPerPixel
            val destination = pixelBase + x * 3
            for (channel in 0..2) {
                val sample = if (bitDepth == 8) {
                    (current[source + channel].toInt() and 0xFF).toFloat() / 255f
                } else {
                    val high = current[source + channel * 2].toInt() and 0xFF
                    val low = current[source + channel * 2 + 1].toInt() and 0xFF
                    ((high shl 8) or low).toFloat() / 65535f
                }
                sourceValues[destination + channel] = sample
            }
        }
        val swap = previous
        previous = current
        current = swap
    }

    val outputSize = minOf(sourceSize, 65)
    val outputValues = if (outputSize == sourceSize) sourceValues
        else resampleHaldCube(sourceValues, sourceSize, outputSize)
    return CubeLut(
        size = outputSize,
        domainMin = floatArrayOf(0f, 0f, 0f),
        domainMax = floatArrayOf(1f, 1f, 1f),
        values = outputValues,
        redFastest = true
    )
}

/** Convert a red-fastest VLT/Hald cube to standard blue-fastest .cube rows. */
internal fun encodeImportedCubeLut(cube: CubeLut, title: String): ByteArray {
    val output = StringBuilder()
    output.append("TITLE \"").append(title.replace('"', '\'')).append("\"\n")
    output.append("LUT_3D_SIZE ").append(cube.size).append('\n')
    output.append("DOMAIN_MIN 0.000000 0.000000 0.000000\n")
    output.append("DOMAIN_MAX 1.000000 1.000000 1.000000\n")
    for (red in 0 until cube.size) {
        for (green in 0 until cube.size) {
            for (blue in 0 until cube.size) {
                val voxel = if (cube.redFastest) {
                    (blue * cube.size + green) * cube.size + red
                } else {
                    (red * cube.size + green) * cube.size + blue
                }
                val base = voxel * 3
                for (channel in 0..2) {
                    if (channel > 0) output.append(' ')
                    val scaled = (cube.values[base + channel].coerceIn(0f, 1f) * 1_000_000f)
                        .roundToInt().coerceIn(0, 1_000_000)
                    output.append(scaled / 1_000_000).append('.')
                    val fraction = (scaled % 1_000_000).toString()
                    repeat(6 - fraction.length) { output.append('0') }
                    output.append(fraction)
                }
                output.append('\n')
            }
        }
    }
    return output.toString().toByteArray(StandardCharsets.UTF_8)
}

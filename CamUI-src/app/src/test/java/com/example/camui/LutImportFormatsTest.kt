package com.example.camui

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class LutImportFormatsTest {
    @Test
    fun importsPanasonicVltAndRoundTripsToStandardCubeOrdering() {
        val vlt = buildString {
            appendLine("# Panasonic VLT file version 1.0")
            appendLine("# Source VLT file Identity.vlt")
            appendLine("LUT_3D_SIZE 17")
            appendLine("# RGB values")
            for (blue in 0 until 17) {
                for (green in 0 until 17) {
                    for (red in 0 until 17) {
                        appendLine(
                            (red * 4095 / 16).toString() + " " +
                                (green * 4095 / 16).toString() + " " +
                                (blue * 4095 / 16).toString()
                        )
                    }
                }
            }
        }.toByteArray(Charsets.US_ASCII)

        val imported = parsePanasonicVlt(vlt)
        assertEquals(17, imported.size)
        val before = imported.sample(0.31f, 0.58f, 0.77f)
        val serialized = encodeImportedCubeLut(imported, "VLT identity")
        val restored = requireNotNull(parseCubeLutText(String(serialized, Charsets.UTF_8)))
        val after = restored.sample(0.31f, 0.58f, 0.77f)
        assertEquals(before.first, after.first, 0.00001f)
        assertEquals(before.second, after.second, 0.00001f)
        assertEquals(before.third, after.third, 0.00001f)
    }

    @Test
    fun importsHaldPng16BitWithCorrectHaldIndexAndChannelPrecision() {
        val edge = 8
        val cubeSize = 4
        val raw = ByteArrayOutputStream()
        for (y in 0 until edge) {
            raw.write(0)
            for (x in 0 until edge) {
                val index = y * edge + x
                val blue = index / (cubeSize * cubeSize)
                val green = (index / cubeSize) % cubeSize
                val red = index % cubeSize
                val values = intArrayOf(
                    red * 65535 / (cubeSize - 1),
                    green * 65535 / (cubeSize - 1),
                    blue * 65535 / (cubeSize - 1)
                )
                for (value in values) {
                    raw.write((value ushr 8) and 0xFF)
                    raw.write(value and 0xFF)
                }
            }
        }
        val compressed = ByteArrayOutputStream().also { stream ->
            DeflaterOutputStream(stream).use { it.write(raw.toByteArray()) }
        }.toByteArray()
        val ihdr = ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN)
            .putInt(edge).putInt(edge).put(16.toByte()).put(2.toByte())
            .put(0.toByte()).put(0.toByte()).put(0.toByte()).array()
        val png = PNG_SIGNATURE + pngChunk("IHDR", ihdr) +
            pngChunk("IDAT", compressed) + pngChunk("IEND", byteArrayOf())

        val imported = parseHaldPng(png)
        assertEquals(4, imported.size)
        val output = FloatArray(3)
        imported.sampleInto(0.3333f, 0.6667f, 1f, output)
        assertEquals(0.3333f, output[0], 0.0001f)
        assertEquals(0.6667f, output[1], 0.0001f)
        assertEquals(1f, output[2], 0.0001f)

        val restored = requireNotNull(
            parseCubeLutText(String(encodeImportedCubeLut(imported, "Hald identity"), Charsets.UTF_8))
        )
        val roundTrip = restored.sample(0.3333f, 0.6667f, 1f)
        assertEquals(output[0], roundTrip.first, 0.00001f)
        assertEquals(output[1], roundTrip.second, 0.00001f)
        assertEquals(output[2], roundTrip.third, 0.00001f)
    }

    @Test
    fun rejectsUnsupportedVltVersionsAndInvalidHaldPngSignatures() {
        val badVlt = """
            # Panasonic VLT file version 2.0
            # Source VLT file bad.vlt
            LUT_3D_SIZE 17
            # RGB
        """.trimIndent().toByteArray(Charsets.US_ASCII)
        try {
            parsePanasonicVlt(badVlt)
            throw AssertionError("VLT v2.0 must not be accepted as v1.0")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }

        try {
            parseHaldPng(byteArrayOf(1, 2, 3, 4))
            throw AssertionError("Invalid PNG signature must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    private fun pngChunk(type: String, payload: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        val data = type.toByteArray(Charsets.US_ASCII) + payload
        fun writeInt(value: Int) {
            output.write((value ushr 24) and 0xFF)
            output.write((value ushr 16) and 0xFF)
            output.write((value ushr 8) and 0xFF)
            output.write(value and 0xFF)
        }
        writeInt(payload.size)
        output.write(data)
        val crc = CRC32().apply { update(data) }.value.toInt()
        writeInt(crc)
        return output.toByteArray()
    }

    private companion object {
        val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
    }
}

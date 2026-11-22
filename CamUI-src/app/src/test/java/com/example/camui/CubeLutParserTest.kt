package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class CubeLutParserTest {
    @Test
    fun standardCubeUsesBlueAsFastestVaryingAxis() {
        val text = buildString {
            appendLine("TITLE \"axis identity\"")
            appendLine("LUT_3D_SIZE 2")
            appendLine("DOMAIN_MIN 0 0 0")
            appendLine("DOMAIN_MAX 1 1 1")
            for (red in 0..1) {
                for (green in 0..1) {
                    for (blue in 0..1) {
                        appendLine("$red $green $blue")
                    }
                }
            }
        }

        val lut = parseCubeLutText(text)
        assertNotNull(lut)
        val result = requireNotNull(lut).sample(0.25f, 0.5f, 0.75f)
        assertEquals(0.25f, result.first, 0.0001f)
        assertEquals(0.5f, result.second, 0.0001f)
        assertEquals(0.75f, result.third, 0.0001f)
    }

    @Test
    fun usesTetrahedralInterpolationAcrossAllSixFractionOrderings() {
        val text = buildString {
            appendLine("LUT_3D_SIZE 2")
            for (red in 0..1) {
                for (green in 0..1) {
                    for (blue in 0..1) {
                        val value = if (red == 1 && green == 1 && blue == 1) 1 else 0
                        appendLine("$value $value $value")
                    }
                }
            }
        }
        val lut = requireNotNull(parseCubeLutText(text))
        val samples = listOf(
            Triple(0.8f, 0.6f, 0.2f),
            Triple(0.8f, 0.2f, 0.6f),
            Triple(0.6f, 0.8f, 0.2f),
            Triple(0.2f, 0.8f, 0.6f),
            Triple(0.2f, 0.6f, 0.8f),
            Triple(0.6f, 0.2f, 0.8f)
        )
        samples.forEach { input ->
            val expected = minOf(input.first, input.second, input.third)
            val result = lut.sample(input.first, input.second, input.third)
            assertEquals(expected, result.first, 0.0001f)
            assertEquals(expected, result.second, 0.0001f)
            assertEquals(expected, result.third, 0.0001f)
        }
    }

    @Test
    fun acceptsOneDimensionalCubeWithoutThreeDimensionalHeader() {
        val text = """
            TITLE "One dimensional identity"
            LUT_1D_SIZE 2
            0 0 0
            1 1 1
        """.trimIndent()

        val result = requireNotNull(parseCubeLutText(text)).sample(0.2f, 0.4f, 0.8f)
        assertEquals(0.2f, result.first, 0.0001f)
        assertEquals(0.4f, result.second, 0.0001f)
        assertEquals(0.8f, result.third, 0.0001f)
    }

    @Test
    fun appliesOneDimensionalLutBeforeThreeDimensionalLut() {
        val rows = buildString {
            appendLine("LUT_1D_SIZE 2")
            appendLine("LUT_3D_SIZE 2")
            appendLine("0.1 0.2 0.3")
            appendLine("0.1 0.2 0.3")
            for (red in 0..1) {
                for (green in 0..1) {
                    for (blue in 0..1) {
                        appendLine("$red $green $blue")
                    }
                }
            }
        }
        val result = requireNotNull(parseCubeLutText(rows)).sample(0.6f, 0.4f, 0.2f)
        assertEquals(0.1f, result.first, 0.0001f)
        assertEquals(0.2f, result.second, 0.0001f)
        assertEquals(0.3f, result.third, 0.0001f)
    }

    @Test
    fun handlesCrLfCommentsAndScientificNotation() {
        val text = listOf(
            "TITLE \"Scientific identity\"",
            "LUT_3D_SIZE 2",
            "DOMAIN_MIN 0e0 0.0 0",
            "DOMAIN_MAX 1.0 1e0 1",
            "0e0 0 0 # red=0 green=0 blue=0",
            "0 0 1.0",
            "0 1e0 0",
            "0 1 1",
            "1e0 0 0",
            "1 0 1",
            "1 1 0",
            "1 1 1"
        ).joinToString("\r\n")
        val result = requireNotNull(parseCubeLutText(text)).sample(0.3f, 0.6f, 0.9f)
        assertEquals(0.3f, result.first, 0.0001f)
        assertEquals(0.6f, result.second, 0.0001f)
        assertEquals(0.9f, result.third, 0.0001f)
    }

    @Test
    fun rejectsNanInfinityAndOutOfRangeVolumeSizes() {
        val nan = "LUT_3D_SIZE 2\n" + List(8) { if (it == 0) "NaN 0 0" else "0 0 0" }.joinToString("\n")
        val infinity = "LUT_1D_SIZE 2\n0 0 0\nInfinity 1 1"
        val tooLarge = "LUT_3D_SIZE 130"
        assertNull(parseCubeLutText(nan))
        assertNull(parseCubeLutText(infinity))
        assertNull(parseCubeLutText(tooLarge))
    }

    @Test
    fun rejectsMissingAndExtraDataRows() {
        assertNull(parseCubeLutText("LUT_3D_SIZE 2\n0 0 0"))
        val extra = "LUT_1D_SIZE 2\n0 0 0\n1 1 1\n0.5 0.5 0.5"
        assertNull(parseCubeLutText(extra))
    }

    @Test
    fun permitsValidOutputValuesOutsideInputDomainBeforeSamplingClamp() {
        val lut = parseCubeLutText(
            "LUT_1D_SIZE 2\n-0.2 0.1 1.2\n1.2 0.9 -0.2"
        )
        assertNotNull(lut)
        val output = requireNotNull(lut).sample(0f, 0f, 0f)
        assertFalse(output.first.isNaN())
        assertEquals(0f, output.first, 0.0001f)
        assertEquals(0.1f, output.second, 0.0001f)
        assertEquals(1f, output.third, 0.0001f)
    }
}

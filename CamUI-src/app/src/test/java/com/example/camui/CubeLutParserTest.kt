package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
    fun sampleIntoMatchesAllocationFreeTetrahedralSample() {
        val text = buildString {
            appendLine("LUT_3D_SIZE 2")
            for (red in 0..1) {
                for (green in 0..1) {
                    for (blue in 0..1) {
                        val outRed = 0.10f + red * 0.40f + green * 0.20f + blue * 0.10f
                        val outGreen = 0.20f + red * 0.10f + green * 0.30f + blue * 0.20f
                        val outBlue = 0.05f + red * 0.20f + green * 0.10f + blue * 0.50f
                        appendLine("$outRed $outGreen $outBlue")
                    }
                }
            }
        }
        val lut = requireNotNull(parseCubeLutText(text))
        val output = FloatArray(3)
        lut.sampleInto(0.2f, 0.6f, 0.4f, output)
        val sampled = lut.sample(0.2f, 0.6f, 0.4f)
        assertEquals(sampled.first, output[0], 0.0001f)
        assertEquals(sampled.second, output[1], 0.0001f)
        assertEquals(sampled.third, output[2], 0.0001f)
        assertEquals(0.34f, output[0], 0.0001f)
        assertEquals(0.48f, output[1], 0.0001f)
        assertEquals(0.35f, output[2], 0.0001f)
    }

    @Test
    fun tetrahedralInterpolationUsesTheContainingSimplex() {
        val text = buildString {
            appendLine("LUT_3D_SIZE 2")
            for (red in 0..1) {
                for (green in 0..1) {
                    for (blue in 0..1) {
                        val product = (red * green).toFloat()
                        appendLine("$product 0 0")
                    }
                }
            }
        }
        val lut = requireNotNull(parseCubeLutText(text))
        val output = FloatArray(3)
        lut.sampleInto(0.25f, 0.75f, 0.50f, output)
        // G >= B >= R uses the G -> B -> R tetrahedron, producing a piecewise
        // linear value of 0.25 rather than the trilinear product 0.1875.
        assertEquals(0.25f, output[0], 0.0001f)
    }

    @Test
    fun classifiesImportedLutGroupsAndLogInputs() {
        assertEquals("Fujifilm", catalogLutCategory("bundleable/Fujifilm/lut_abc.cube"))
        assertEquals("Leica", catalogLutCategory("bundleable/Leica/Classic.cube"))
        assertEquals("Panasonic Lumix", catalogLutCategory("bundleable/Panasonic Lumix/Standard.cube"))
        assertEquals("Sony", catalogLutCategory("bundleable/Sony/CreativeLook_NT.cube"))
        assertEquals("Hald CLUT", catalogLutCategory("local-only/Hald CLUT/MyLook.cube"))
        assertEquals("Cinema / Log", catalogLutCategory("bundleable/Panasonic Lumix/V-Log/Look.cube"))
        assertEquals("Cinema / Log", catalogLutCategory("bundleable/Sony/CineAlta/S-Log3.cube"))
        assertEquals("Khác", catalogLutCategory("local-only/Other/Custom.cube"))
    }

    @Test
    fun parsesLittleEndianFloat32BinaryLutWithBlueFastestAxis() {
        val size = 33
        val bytes = ByteBuffer.allocate(size * size * size * 3 * 4)
            .order(ByteOrder.LITTLE_ENDIAN)
        for (red in 0 until size) {
            for (green in 0 until size) {
                for (blue in 0 until size) {
                    bytes.putFloat(red / 32f)
                    bytes.putFloat(green / 32f)
                    bytes.putFloat(blue / 32f)
                }
            }
        }

        val lut = requireNotNull(parseFloatBinaryLut(bytes.array()))
        val sampled = lut.sample(0.25f, 0.50f, 0.75f)
        assertEquals(33, lut.size)
        assertEquals(0.25f, sampled.first, 0.001f)
        assertEquals(0.50f, sampled.second, 0.001f)
        assertEquals(0.75f, sampled.third, 0.001f)
    }

    @Test
    fun rejectsUnsupportedFloat32BinaryLutSizes() {
        assertNull(parseFloatBinaryLut(ByteArray(1596)))
        assertNull(parseFloatBinaryLut(ByteArray(33 * 33 * 33 * 3 * 2)))
    }

    @Test
    fun classifiesChineseNamedCubeFilesIntoEnglishGroups() {
        assertEquals("Hasselblad", catalogLutCategory("assets/film_sims/哈苏 经典.cube"))
        assertEquals("Hasselblad", catalogLutCategory("assets/film_sims/哈苏 鲜艳.cube"))
        assertEquals("Fujifilm", catalogLutCategory("assets/film_sims/富士 标准.cube"))
        assertEquals("Fujifilm", catalogLutCategory("assets/film_sims/富士 经典正片.cube"))
        assertEquals("Leica", catalogLutCategory("assets/film_sims/徕卡 CLS 胶片质感.cube"))
        assertEquals("Kodak", catalogLutCategory("assets/film_sims/柯达 Ultramax 400.cube"))
        assertEquals("Creative", catalogLutCategory("assets/film_sims/奶茶色静物.cube"))
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

package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HdmpAiRuntimePolicyTest {
    @Test
    fun prefersNnapiThenXnnpackAndKeepsCpuLast() {
        assertEquals(
            listOf("NNAPI_FP16", "XNNPACK", "CPU"),
            HdmpAiRuntimePolicy.providerOrder(setOf("CPU", "XNNPACK", "NNAPI"))
        )
    }

    @Test
    fun keepsXnnpackFallbackWhenNnapiIsUnavailable() {
        assertEquals(
            listOf("XNNPACK", "CPU"),
            HdmpAiRuntimePolicy.providerOrder(setOf("XNNPACK"))
        )
    }

    @Test
    fun flatAreasKeepFullAiBlendForDenoising() {
        val flat = 0xFF777777.toInt()
        assertEquals(1f, HdmpAiRuntimePolicy.detailPreservationScale(flat, flat, flat, flat, flat), 0.0001f)
    }

    @Test
    fun highContrastFineDetailReducesAiBlendToProtectStrands() {
        val black = 0xFF101010.toInt()
        val strand = 0xFFE0E0E0.toInt()
        val scale = HdmpAiRuntimePolicy.detailPreservationScale(strand, black, black, black, black)
        assertEquals(0.60f, scale, 0.0001f)
        assertTrue(scale < 1f)
    }
}

package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Test

class HdmpSelectionPolicyTest {
    @Test
    fun onlyTwoModelSelectionsArePersisted() {
        assertEquals("ai2", HdmpSelectionPolicy.normalizeModel("quality"))
        assertEquals("ai2", HdmpSelectionPolicy.normalizeModel("balanced"))
        assertEquals("ai2", HdmpSelectionPolicy.normalizeModel("ai2"))
        assertEquals("ai15", HdmpSelectionPolicy.normalizeModel("speed"))
        assertEquals("ai15", HdmpSelectionPolicy.normalizeModel("ai15"))
    }

    @Test
    fun outputMegapixelsAreRestrictedToSupportedChoices() {
        assertEquals(listOf(24, 48, 60, 108, 250), HdmpSelectionPolicy.supportedOutputMegapixels)
        assertEquals(60, HdmpSelectionPolicy.normalizeOutputMegapixels(0))
        assertEquals(108, HdmpSelectionPolicy.normalizeOutputMegapixels(108))
        assertEquals(250, HdmpSelectionPolicy.normalizeOutputMegapixels(250))
    }

    @Test
    fun modelTargetsAndLimitsMatchThe16Specification() {
        assertEquals(20, HdmpSelectionPolicy.targetRenderSeconds("ai2"))
        assertEquals(30, HdmpSelectionPolicy.maximumRenderSeconds("ai2"))
        assertEquals(10, HdmpSelectionPolicy.targetRenderSeconds("ai15"))
        assertEquals(20, HdmpSelectionPolicy.maximumRenderSeconds("ai15"))
    }
}

package com.example.camui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraAfLabelContractTest {
    @Test
    fun cameraHudDoesNotExposeTheRemovedTapAfInstruction() {
        val source = File("src/main/java/com/example/camui/CameraScreen.kt")
        assertTrue("CameraScreen source is missing", source.isFile)
        val text = source.readText()

        assertFalse(
            "The instructional TAP AF hint must not appear in the camera HUD.",
            text.contains("TAP AF · HOLD LOCK")
        )
        assertTrue(
            "The camera HUD must use the AF-L resource for a locked state.",
            text.contains("R.string.camera_af_locked")
        )
    }

    @Test
    fun afLabelsAreDefinedConsistentlyForEnglishAndVietnamese() {
        val english = readStrings(File("src/main/res/values/strings.xml"))
        val vietnamese = readStrings(File("src/main/res/values-vi/strings.xml"))

        assertEquals("AF", english["camera_af_normal"])
        assertEquals("AF-L", english["camera_af_locked"])
        assertEquals(english["camera_af_normal"], vietnamese["camera_af_normal"])
        assertEquals(english["camera_af_locked"], vietnamese["camera_af_locked"])
    }

    private fun readStrings(file: File): Map<String, String> {
        assertTrue("String resource is missing: ${file.path}", file.isFile)
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
        val nodes = document.getElementsByTagName("string")
        return buildMap {
            for (index in 0 until nodes.length) {
                val element = nodes.item(index) as org.w3c.dom.Element
                put(element.getAttribute("name"), element.textContent)
            }
        }
    }
}

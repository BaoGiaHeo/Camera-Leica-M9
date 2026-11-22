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
        val pixelRenderer = File("src/main/java/com/example/camui/PixelOsdLayer.kt")
        assertTrue("Pixel OSD renderer is missing", pixelRenderer.isFile)
        val pixelText = pixelRenderer.readText()
        val layoutSource = File("src/main/java/com/example/camui/PixelOsdLayoutEngine.kt")
        assertTrue("OSD layout engine is missing", layoutSource.isFile)
        val layoutText = layoutSource.readText()
        assertTrue(
            "The Pro right column must show AF-L only while AF/AE lock is active.",
            layoutText.contains("addText(\"pro-lock\", \"pro-right\", \"AF-L\"") &&
                pixelText.contains("element.id == \"pro-lock\" -> PixelOsdPalette.AMBER")
        )
        assertTrue(
            "The focus status must use the green state after successful autofocus.",
            pixelText.contains("element.id == \"status-focus\" && frame.centerAfStatus == 1 -> PixelOsdPalette.GREEN")
        )
    }

    @Test
    fun removedTapAfInstructionIsAbsentFromAllKotlinUiSources() {
        val sourceRoot = File("src/main/java")
        assertTrue("Kotlin source root is missing", sourceRoot.isDirectory)
        val forbidden = listOf("TAP AF · HOLD LOCK", "TAP AF · HOLD TO LOCK")
        val matches = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (forbidden.any(line::contains)) {
                        "${file.relativeTo(sourceRoot).path}:${index + 1}: $line"
                    } else null
                }.asSequence()
            }
            .toList()
        assertTrue(matches.joinToString("\n"), matches.isEmpty())
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

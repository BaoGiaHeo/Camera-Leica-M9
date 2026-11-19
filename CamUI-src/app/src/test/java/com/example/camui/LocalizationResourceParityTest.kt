package com.example.camui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LocalizationResourceParityTest {
    @Test
    fun englishAndVietnameseResourceKeysStayInParity() {
        val english = readStringResources(File("src/main/res/values/strings.xml"))
        val vietnamese = readStringResources(File("src/main/res/values-vi/strings.xml"))

        assertEquals(
            "Vietnamese resources must define exactly the same keys as English",
            english.keys,
            vietnamese.keys
        )
    }

    @Test
    fun formatPlaceholdersStayInParityForEveryResource() {
        val english = readStringResources(File("src/main/res/values/strings.xml"))
        val vietnamese = readStringResources(File("src/main/res/values-vi/strings.xml"))

        assertEquals(english.keys, vietnamese.keys)
        english.forEach { (key, englishValue) ->
            val englishPlaceholders = PLACEHOLDER.findAll(englishValue).map { it.value }.toList()
            val vietnamesePlaceholders = PLACEHOLDER.findAll(vietnamese.getValue(key))
                .map { it.value }
                .toList()
            assertEquals(
                "Format placeholders differ for resource '$key'",
                englishPlaceholders,
                vietnamesePlaceholders
            )
        }
    }

    @Test
    fun localizationWhitelistExistsAndContainsNoDuplicateEntries() {
        val whitelist = File("../i18n_whitelist.txt")
        assertTrue("Missing canonical localization whitelist: ${whitelist.path}", whitelist.isFile)
        val entries = whitelist.readLines()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        assertTrue("Localization whitelist must not be empty", entries.isNotEmpty())
        assertEquals(
            "Localization whitelist contains duplicate entries",
            entries.size,
            entries.toSet().size
        )
    }

    private fun readStringResources(file: File): Map<String, String> {
        assertTrue("String resource is missing: ${file.path}", file.isFile)
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val document = factory.newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        return buildMap {
            for (index in 0 until nodes.length) {
                val element = nodes.item(index) as Element
                val name = element.getAttribute("name")
                check(name.isNotBlank()) { "String resource has an empty name in ${file.path}" }
                check(!containsKey(name)) { "Duplicate string resource '$name' in ${file.path}" }
                put(name, element.textContent)
            }
        }
    }

    private companion object {
        val PLACEHOLDER = Regex("%[0-9]+\\$[sd]")
    }
}

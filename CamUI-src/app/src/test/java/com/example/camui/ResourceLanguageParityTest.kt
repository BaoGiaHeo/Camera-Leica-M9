package com.example.camui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ResourceLanguageParityTest {
    private fun stringValues(relativePath: String): Map<String, String> {
        val file = File(relativePath)
        check(file.isFile) { "Android string resource file not found: ${file.absolutePath}" }
        val xml = file.readText(Charsets.UTF_8)
        return Regex("""<string\s+name="([^"]+)"[^>]*>([\s\S]*?)</string>""")
            .findAll(xml)
            .associate { match -> match.groupValues[1] to match.groupValues[2] }
    }

    @Test
    fun englishAndVietnameseStringKeysStayInParity() {
        val english = stringValues("src/main/res/values/strings.xml")
        val vietnamese = stringValues("src/main/res/values-vi/strings.xml")
        assertEquals(english.keys, vietnamese.keys)
    }

    @Test
    fun resourcePlaceholdersStayInParity() {
        val english = stringValues("src/main/res/values/strings.xml")
        val vietnamese = stringValues("src/main/res/values-vi/strings.xml")
        fun placeholders(value: String): List<String> =
            Regex("""%(?:\d+\$)?[sdif]""").findAll(value).map { it.value }.sorted().toList()

        english.keys.forEach { key ->
            assertEquals(
                "Placeholder mismatch for string $key",
                placeholders(english.getValue(key)),
                placeholders(vietnamese.getValue(key))
            )
        }
    }

    @Test
    fun obsoleteSystemLanguageChoiceIsNotExposedAsAStringResource() {
        val english = stringValues("src/main/res/values/strings.xml")
        val vietnamese = stringValues("src/main/res/values-vi/strings.xml")
        assertFalse(english.containsKey("language_system"))
        assertFalse(vietnamese.containsKey("language_system"))
    }
}

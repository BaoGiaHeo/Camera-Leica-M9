package com.example.camui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeScrollAuditTest {
    private data class ScrollBlock(val kind: String, val start: Int, val end: Int, val line: Int)

    @Test
    fun noVerticallyScrollableContainerIsNestedInsideAnother() {
        val sourceRoot = File("src/main/java")
        assertTrue("Kotlin source root is missing: ${sourceRoot.path}", sourceRoot.isDirectory)

        val issues = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val source = file.readText()
                nestedScrollIssues(source, file.relativeTo(sourceRoot).path).asSequence()
            }
            .toList()

        assertTrue(issues.joinToString(separator = "\n"), issues.isEmpty())
    }

    @Test
    fun noUnboundedScrollWorkaroundsExist() {
        val sourceRoot = File("src/main/java")
        val offenders = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val lines = file.readLines()
                lines.mapIndexedNotNull { index, line ->
                    if (line.contains("heightIn(max = Dp.Infinity)") ||
                        line.contains("wrapContentSize(unbounded = true)")
                    ) "${file.relativeTo(sourceRoot).path}:${index + 1}: $line" else null
                }.asSequence()
            }
            .toList()

        assertTrue(offenders.joinToString(separator = "\n"), offenders.isEmpty())
    }

    private fun nestedScrollIssues(source: String, fileName: String): List<String> {
        val masked = maskStringsAndComments(source)
        val pattern = Regex("verticalScroll\\s*\\(|LazyColumn\\s*\\(|LazyVerticalGrid\\s*\\(|LazyVerticalStaggeredGrid\\s*\\(")
        val blocks = pattern.findAll(masked).mapNotNull { match ->
            val open = masked.indexOf('{', match.range.last + 1)
            if (open < 0) return@mapNotNull null
            var depth = 0
            var end = -1
            for (index in open until masked.length) {
                when (masked[index]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            end = index
                            break
                        }
                    }
                }
            }
            if (end < 0) null else {
                val line = source.substring(0, match.range.first).count { it == '\n' } + 1
                ScrollBlock(match.value.substringBefore('('), match.range.first, end, line)
            }
        }.toList()

        val issues = mutableListOf<String>()
        for (parent in blocks) {
            for (child in blocks) {
                if (child.start > parent.start && child.start < parent.end) {
                    issues += "$fileName:${child.line}: ${child.kind} is nested in ${parent.kind} opened at line ${parent.line}"
                }
            }
        }
        return issues
    }

    private fun maskStringsAndComments(input: String): String {
        val output = StringBuilder(input.length)
        var index = 0
        var inLineComment = false
        var inBlockComment = false
        var inString = false
        var inTripleString = false
        var inChar = false

        while (index < input.length) {
            val current = input[index]
            val next = input.getOrNull(index + 1)
            val third = input.getOrNull(index + 2)

            when {
                inLineComment -> {
                    output.append(if (current == '\n') '\n' else ' ')
                    if (current == '\n') inLineComment = false
                    index++
                }
                inBlockComment -> {
                    if (current == '*' && next == '/') {
                        output.append("  ")
                        inBlockComment = false
                        index += 2
                    } else {
                        output.append(if (current == '\n') '\n' else ' ')
                        index++
                    }
                }
                inTripleString -> {
                    if (current == '"' && next == '"' && third == '"') {
                        output.append("   ")
                        inTripleString = false
                        index += 3
                    } else {
                        output.append(if (current == '\n') '\n' else ' ')
                        index++
                    }
                }
                inString -> {
                    if (current == '\\') {
                        output.append("  ")
                        index = (index + 2).coerceAtMost(input.length)
                    } else if (current == '"') {
                        output.append(' ')
                        inString = false
                        index++
                    } else {
                        output.append(if (current == '\n') '\n' else ' ')
                        index++
                    }
                }
                inChar -> {
                    if (current == '\\') {
                        output.append("  ")
                        index = (index + 2).coerceAtMost(input.length)
                    } else {
                        output.append(if (current == '\n') '\n' else ' ')
                        if (current == '\'') inChar = false
                        index++
                    }
                }
                current == '/' && next == '/' -> {
                    output.append("  ")
                    inLineComment = true
                    index += 2
                }
                current == '/' && next == '*' -> {
                    output.append("  ")
                    inBlockComment = true
                    index += 2
                }
                current == '"' && next == '"' && third == '"' -> {
                    output.append("   ")
                    inTripleString = true
                    index += 3
                }
                current == '"' -> {
                    output.append(' ')
                    inString = true
                    index++
                }
                current == '\'' -> {
                    output.append(' ')
                    inChar = true
                    index++
                }
                else -> {
                    output.append(current)
                    index++
                }
            }
        }
        return output.toString()
    }
}

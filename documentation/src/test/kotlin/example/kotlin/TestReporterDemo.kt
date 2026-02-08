/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.kotlin

import org.junit.jupiter.api.MediaType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestReporter
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

// tag::user_guide[]
class TestReporterDemo {
    @Test
    fun reportSingleValue(testReporter: TestReporter) {
        testReporter.publishEntry("a status message")
    }

    @Test
    fun reportKeyValuePair(testReporter: TestReporter) {
        testReporter.publishEntry("a key", "a value")
    }

    @Test
    fun reportMultipleKeyValuePairs(testReporter: TestReporter) {
        testReporter.publishEntry(
            mapOf(
                "user name" to "dk38",
                "award year" to "1974"
            )
        )
    }

    @Test
    fun reportFiles(
        testReporter: TestReporter,
        @TempDir tempDir: Path
    ) {
        testReporter.publishFile("test1.txt", MediaType.TEXT_PLAIN_UTF_8) { file ->
            Files.write(file, listOf("Test 1"))
        }

        val existingFile = Files.write(tempDir.resolve("test2.txt"), listOf("Test 2"))
        testReporter.publishFile(existingFile, MediaType.TEXT_PLAIN_UTF_8)

        testReporter.publishDirectory("test3") { dir ->
            Files.write(dir.resolve("nested1.txt"), listOf("Nested content 1"))
            Files.write(dir.resolve("nested2.txt"), listOf("Nested content 2"))
        }

        val existingDir = Files.createDirectory(tempDir.resolve("test4"))
        Files.write(existingDir.resolve("nested1.txt"), listOf("Nested content 1"))
        Files.write(existingDir.resolve("nested2.txt"), listOf("Nested content 2"))
        testReporter.publishDirectory(existingDir)
    }
}
// end::user_guide[]

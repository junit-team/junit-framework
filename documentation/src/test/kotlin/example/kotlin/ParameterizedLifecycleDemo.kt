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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.AfterParameterizedClassInvocation
import org.junit.jupiter.params.BeforeParameterizedClassInvocation
import org.junit.jupiter.params.Parameter
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files
import java.nio.file.Path

// tag::example[]
@ParameterizedClass
@MethodSource("textFiles")
@TestInstance(PER_CLASS)
class TextFileTests {
    @Parameter
    lateinit var textFile: TextFile

    fun textFiles(): List<TextFile> =
        listOf(
            TextFile("file1", "first content"),
            TextFile("file2", "second content")
        )

    @BeforeParameterizedClassInvocation
    fun beforeInvocation(
        textFile: TextFile,
        @TempDir tempDir: Path
    ) { // <1>
        val filePath = tempDir.resolve(textFile.fileName)
        textFile.path = Files.writeString(filePath, textFile.content)
    }

    @AfterParameterizedClassInvocation
    fun afterInvocation(textFile: TextFile) { // <3>
        val actualContent = Files.readString(textFile.path)
        assertEquals(textFile.content, actualContent, "Content must not have changed")
        // Custom cleanup logic, if necessary
        // File will be deleted automatically by @TempDir support
    }

    @Test
    fun test() {
        assertTrue(Files.exists(textFile.path)) // <2>
    }

    @Test
    fun anotherTest() {
        // ...
    }
}

class TextFile(
    val fileName: String,
    val content: String
) {
    lateinit var path: Path

    override fun toString(): String = fileName
}
// end::example[]

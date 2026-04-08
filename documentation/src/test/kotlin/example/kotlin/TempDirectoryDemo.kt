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

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import example.kotlin.TempDirectoryDemo.InMemoryTempDirDemo.JimfsTempDirFactory
import example.util.ListWriter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.AnnotatedElementContext
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.io.TempDirDeletionStrategy
import org.junit.jupiter.api.io.TempDirFactory
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

@Suppress("ClassName")
class TempDirectoryDemo {
    // tag::user_guide_parameter_injection[]
    @Test
    fun writeItemsToFile(
        @TempDir tempDir: Path
    ) {
        val file = tempDir.resolve("test.txt")

        ListWriter(file).write("a", "b", "c")

        assertEquals(listOf("a,b,c"), Files.readAllLines(file))
    }
    // end::user_guide_parameter_injection[]

    // tag::user_guide_multiple_directories[]
    @Test
    fun copyFileFromSourceToTarget(
        @TempDir source: Path,
        @TempDir target: Path
    ) {
        val sourceFile = source.resolve("test.txt")
        ListWriter(sourceFile).write("a", "b", "c")

        val targetFile = Files.copy(sourceFile, target.resolve("test.txt"))

        assertNotEquals(sourceFile, targetFile)
        assertEquals(listOf("a,b,c"), Files.readAllLines(targetFile))
    }
    // end::user_guide_multiple_directories[]

    // tag::user_guide_field_injection[]
    class SharedTempDirectoryDemo {
        @Test
        fun writeItemsToFile() {
            val file = sharedTempDir.resolve("test.txt")

            ListWriter(file).write("a", "b", "c")

            assertEquals(listOf("a,b,c"), Files.readAllLines(file))
        }

        @Test
        fun anotherTestThatUsesTheSameTempDir() {
            // use sharedTempDir
        }

        companion object {
            @TempDir
            @JvmStatic
            lateinit var sharedTempDir: Path
        }
    }
    // end::user_guide_field_injection[]

    // tag::user_guide_cleanup_mode[]
    class CleanupModeDemo {
        @Test
        fun fileTest(
            @TempDir(cleanup = ON_SUCCESS) tempDir: Path
        ) {
            // perform test
        }
    }
    // end::user_guide_cleanup_mode[]

    // tag::user_guide_factory_name_prefix[]
    class TempDirFactoryDemo {
        @Test
        fun factoryTest(
            @TempDir(factory = Factory::class) tempDir: Path
        ) {
            assertTrue(tempDir.fileName.toString().startsWith("factoryTest"))
        }

        class Factory : TempDirFactory {
            override fun createTempDirectory(
                elementContext: AnnotatedElementContext,
                extensionContext: ExtensionContext
            ): Path = Files.createTempDirectory(extensionContext.requiredTestMethod.name)
        }
    }
    // end::user_guide_factory_name_prefix[]

    // tag::user_guide_factory_jimfs[]
    class InMemoryTempDirDemo {
        @Test
        fun test(
            @TempDir(factory = JimfsTempDirFactory::class) tempDir: Path
        ) {
            // perform test
        }

        class JimfsTempDirFactory : TempDirFactory {
            private val fileSystem: FileSystem = Jimfs.newFileSystem(Configuration.unix())

            override fun createTempDirectory(
                elementContext: AnnotatedElementContext,
                extensionContext: ExtensionContext
            ): Path = Files.createTempDirectory(fileSystem.getPath("/"), "junit-")

            override fun close() {
                fileSystem.close()
            }
        }
    }
    // end::user_guide_factory_jimfs[]

    // tag::user_guide_deletion_strategy[]
    class DeletionStrategyDemo {
        @Test
        fun test(
            @TempDir(deletionStrategy = TempDirDeletionStrategy.IgnoreFailures::class)
            tempDir: Path
        ) {
            // perform test
        }
    }
    // end::user_guide_deletion_strategy[]

    // tag::user_guide_composed_annotation[]
    @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.RUNTIME)
    @TempDir(factory = JimfsTempDirFactory::class)
    annotation class JimfsTempDir
    // end::user_guide_composed_annotation[]

    // tag::user_guide_composed_annotation_usage[]
    class JimfsTempDirAnnotationDemo {
        @Test
        fun test(
            @JimfsTempDir tempDir: Path
        ) {
            // perform test
        }
    }
    // end::user_guide_composed_annotation_usage[]
}

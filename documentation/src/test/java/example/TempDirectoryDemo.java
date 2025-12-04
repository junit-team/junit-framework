/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import example.TempDirectoryDemo.InMemoryTempDirDemo.JimfsTempDirFactory;
import example.util.ListWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirFactory;

class TempDirectoryDemo {

	// tag::user_guide_parameter_injection[]
	@Test
	void writeItemsToFile(@TempDir Path tempDir) throws IOException {
		Path file = tempDir.resolve("test.txt");

		new ListWriter(file).write("a", "b", "c");

		assertEquals(List.of("a,b,c"), Files.readAllLines(file));
	}
	// end::user_guide_parameter_injection[]

	// tag::user_guide_multiple_directories[]
	@Test
	void copyFileFromSourceToTarget(@TempDir Path source, @TempDir Path target) throws IOException {
		Path sourceFile = source.resolve("test.txt");
		new ListWriter(sourceFile).write("a", "b", "c");

		Path targetFile = Files.copy(sourceFile, target.resolve("test.txt"));

		assertNotEquals(sourceFile, targetFile);
		assertEquals(List.of("a,b,c"), Files.readAllLines(targetFile));
	}
	// end::user_guide_multiple_directories[]

	@SuppressWarnings("JUnitMalformedDeclaration")
	static
	// tag::user_guide_field_injection[]
	class SharedTempDirectoryDemo {

		@TempDir
		static Path sharedTempDir;

		@Test
		void writeItemsToFile() throws IOException {
			Path file = sharedTempDir.resolve("test.txt");

			new ListWriter(file).write("a", "b", "c");

			assertEquals(List.of("a,b,c"), Files.readAllLines(file));
		}

		@Test
		void anotherTestThatUsesTheSameTempDir() {
			// use sharedTempDir
		}

	}
	// end::user_guide_field_injection[]

	@SuppressWarnings("JUnitMalformedDeclaration")
	static
	// tag::user_guide_cleanup_mode[]
	class CleanupModeDemo {

		@Test
		void fileTest(@TempDir(cleanup = ON_SUCCESS) Path tempDir) {
			// perform test
		}

	}
	// end::user_guide_cleanup_mode[]

	@SuppressWarnings("JUnitMalformedDeclaration")
	static
	// tag::user_guide_factory_name_prefix[]
	class TempDirFactoryDemo {

		@Test
		void factoryTest(@TempDir(factory = Factory.class) Path tempDir) {
			assertTrue(tempDir.getFileName().toString().startsWith("factoryTest"));
		}

		static class Factory implements TempDirFactory {

			@Override
			public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
					throws IOException {
				return Files.createTempDirectory(extensionContext.getRequiredTestMethod().getName());
			}

		}

	}
	// end::user_guide_factory_name_prefix[]

	@SuppressWarnings("JUnitMalformedDeclaration")
	static
	// tag::user_guide_factory_jimfs[]
	class InMemoryTempDirDemo {

		@Test
		void test(@TempDir(factory = JimfsTempDirFactory.class) Path tempDir) {
			// perform test
		}

		static class JimfsTempDirFactory implements TempDirFactory {

			private final FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());

			@Override
			public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
					throws IOException {
				return Files.createTempDirectory(fileSystem.getPath("/"), "junit-");
			}

			@Override
			public void close() throws IOException {
				fileSystem.close();
			}

		}

	}
	// end::user_guide_factory_jimfs[]

	// tag::user_guide_composed_annotation[]
	@Target({ ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.PARAMETER })
	@Retention(RetentionPolicy.RUNTIME)
	@TempDir(factory = JimfsTempDirFactory.class)
	@interface JimfsTempDir {
	}
	// end::user_guide_composed_annotation[]

	@SuppressWarnings("JUnitMalformedDeclaration")
	static
	// tag::user_guide_composed_annotation_usage[]
	class JimfsTempDirAnnotationDemo {

		@Test
		void test(@JimfsTempDir Path tempDir) {
			// perform test
		}

	}
	// end::user_guide_composed_annotation_usage[]

}

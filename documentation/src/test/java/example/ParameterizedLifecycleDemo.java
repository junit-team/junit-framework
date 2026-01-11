/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.AfterParameterizedClassInvocation;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

public class ParameterizedLifecycleDemo {

	@Nested
	// tag::example[]
	@ParameterizedClass
	@MethodSource("textFiles")
	class TextFileTests {

		static List<TextFile> textFiles() {
			return List.of(
				// tag::custom_line_break[]
				new TextFile("file1", "first content"),
				// tag::custom_line_break[]
				new TextFile("file2", "second content")
			// tag::custom_line_break[]
			);
		}

		@Parameter
		TextFile textFile;

		@BeforeParameterizedClassInvocation
		static void beforeInvocation(TextFile textFile, @TempDir Path tempDir) throws Exception {
			var filePath = tempDir.resolve(textFile.fileName); // <1>
			textFile.path = Files.writeString(filePath, textFile.content);
		}

		//end::user_guide[]
		@SuppressWarnings("DataFlowIssue")
		//tag::user_guide[]
		@AfterParameterizedClassInvocation
		static void afterInvocation(TextFile textFile) throws Exception {
			var actualContent = Files.readString(textFile.path); // <3>
			assertEquals(textFile.content, actualContent, "Content must not have changed");
			// Custom cleanup logic, if necessary
			// File will be deleted automatically by @TempDir support
		}

		//end::user_guide[]
		@SuppressWarnings("DataFlowIssue")
		//tag::user_guide[]
		@Test
		void test() {
			assertTrue(Files.exists(textFile.path)); // <2>
		}

		@Test
		void anotherTest() {
			// ...
		}

		static class TextFile {

			final String fileName;
			final String content;
			// end::example[]
			@Nullable
			// tag::example[]
			Path path;

			TextFile(String fileName, String content) {
				this.fileName = fileName;
				this.content = content;
			}

			@Override
			public String toString() {
				return fileName;
			}
		}
	}
	// end::example[]

}

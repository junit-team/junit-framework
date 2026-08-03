/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.configuration.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.tools.DiagnosticCollector;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationMetadataAnnotationProcessorTests {

	// TODO: Use an in memory solution, faster
	@TempDir
	Path tempDir;

	@Test
	void test() {
		var sourceDirectory = Path.of("src/test/java");
		var type = OneConfigurationProperty.class;
		var sourceFile = sourceDirectory.resolve(type.getTypeName().replace('.', '/') + ".java");
		var processor = new ConfigurationMetadataAnnotationProcessor();
		var compiler = ToolProvider.getSystemJavaCompiler();
		var out = new StringWriter();
		var listener = new DiagnosticCollector<>();
		var fileManager = compiler.getStandardFileManager(listener, Locale.ROOT, Charset.defaultCharset());
		var options = List.of("-d", tempDir.toString());
		var classes = Set.of(type.getTypeName());
		var compilationUnits = Set.of(new JavaSourceFileObject(sourceFile));
		var task = compiler.getTask(out, fileManager, listener, options, classes, compilationUnits);
		task.setProcessors(Set.of(processor));
		var result = task.call();
		if (!result || !listener.getDiagnostics().isEmpty()) {
			System.out.println(out);
			listener.getDiagnostics().forEach(System.out::println);
			fail();
		}
		var expectedMetadataPath = "META-INF/junit-platform-configuration-metadata.json";
		assertThat(tempDir.resolve(expectedMetadataPath)).exists().content().isEqualTo("""
				{}
				""");
	}

	private static class JavaSourceFileObject extends SimpleJavaFileObject {

		private final Path sourceFile;

		JavaSourceFileObject(Path sourceFile) {
			super(sourceFile.toUri(), Kind.SOURCE);
			this.sourceFile = sourceFile;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
			return Files.readString(sourceFile);
		}
	}
}

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

import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Processor;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

class TestCompiler {

	private final Path sourceDirectory;
	// TODO: Use an in memory solution, faster
	private final Path outputDirectory;
	private final Processor processor;

	TestCompiler(Path sourceDirectory, Path outputDirectory, Processor processor) {
		this.sourceDirectory = sourceDirectory;
		this.outputDirectory = outputDirectory;
		this.processor = processor;
	}

	void compile(Type type) {
		var options = List.of("-d", outputDirectory.toString());
		var listener = new DiagnosticCollector<>();
		var additionalOutput = new StringWriter();
		var task = ToolProvider.getSystemJavaCompiler().getTask( //
			additionalOutput, //
			fileManagerOf(ToolProvider.getSystemJavaCompiler(), listener), //
			listener, //
			options, //
			classesOf(type), //
			compilationUnitOf(type) //
		);
		task.setProcessors(Set.of(processor));
		var result = task.call();
		if (!result || !listener.getDiagnostics().isEmpty()) {
			var diagnostics = listener.getDiagnostics().stream() //
					.map(Objects::toString) //
					.collect(Collectors.joining("\n"));
			fail("""
					Compilation of %s was not successful.

					Javac output:

					%s

					Diagnostics:

					%s
					""".formatted(type, additionalOutput, diagnostics));
		}
	}

	private static StandardJavaFileManager fileManagerOf(JavaCompiler compiler, DiagnosticCollector<Object> listener) {
		return compiler.getStandardFileManager(listener, Locale.ROOT, Charset.defaultCharset());
	}

	private static Set<String> classesOf(Type type) {
		return Set.of(type.getTypeName());
	}

	private Set<JavaSourceFileObject> compilationUnitOf(Type type) {
		var resourceName = type.getTypeName().replace('.', '/');
		var javaFileName = resourceName + ".java";
		var resolvedJavaFileName = sourceDirectory.resolve(javaFileName);
		return Set.of(new JavaSourceFileObject(resolvedJavaFileName));
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

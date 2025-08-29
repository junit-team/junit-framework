/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.tests;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.tests.process.OutputFiles;
import org.junit.platform.tests.process.ProcessStarter;

class OutputAttachingExtension implements ParameterResolver, AfterTestExecutionCallback {

	private static final Namespace NAMESPACE = Namespace.create(OutputAttachingExtension.class);

	private static final MediaType MEDIA_TYPE = MediaType.create("text", "plain", ProcessStarter.OUTPUT_ENCODING);

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return parameterContext.isAnnotated(FilePrefix.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		var outputDir = extensionContext.getStore(NAMESPACE).computeIfAbsent("outputDir", __ -> {
			try {
				return new OutputDir(Files.createTempDirectory("output"));
			}
			catch (Exception e) {
				throw new ParameterResolutionException("Failed to create temp directory", e);
			}
		}, OutputDir.class);
		var prefix = parameterContext.findAnnotation(FilePrefix.class) //
				.map(FilePrefix::value) //
				.orElseThrow();
		return outputDir.toOutputFiles(prefix);
	}

	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		var outputDir = context.getStore(NAMESPACE).get("outputDir", OutputDir.class);
		if (outputDir != null) {
			try (var stream = Files.list(outputDir.root()).filter(Files::isRegularFile).sorted()) {
				stream.filter(OutputAttachingExtension::notEmpty).forEach(file -> {
					var fileName = file.getFileName().toString();
					context.publishFile(fileName, MEDIA_TYPE,
						target -> Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING));
				});
			}
		}
	}

	private static boolean notEmpty(Path file) {
		try {
			return Files.size(file) > 0;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@SuppressWarnings("try")
	record OutputDir(Path root) implements AutoCloseable {

		@Override
		public void close() throws Exception {
			try (var stream = Files.walk(root).sorted(Comparator.<Path> naturalOrder().reversed())) {
				stream.forEach(path -> {
					try {
						Files.delete(path);
					}
					catch (IOException e) {
						throw new UncheckedIOException("Failed to delete " + path, e);
					}
				});
			}
		}

		private OutputFiles toOutputFiles(String prefix) {
			return new OutputFiles(root.resolve(prefix + "-stdout.txt"), root.resolve(prefix + "-stderr.txt"));
		}
	}

}

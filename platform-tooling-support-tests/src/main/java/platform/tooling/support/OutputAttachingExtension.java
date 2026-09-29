/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.platform.tests.process.OutputFiles;
import org.junit.platform.tests.process.ProcessStarter;

class OutputAttachingExtension implements ParameterResolver, AfterTestExecutionCallback, TestExecutionExceptionHandler {

	private static final Namespace NAMESPACE = Namespace.create(OutputAttachingExtension.class);
	private static final String STDERR_SUFFIX = "-stderr.txt";
	private static final int STDERR_HEAD_LINES = 20;

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
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		var outputDir = context.getStore(NAMESPACE).get("outputDir", OutputDir.class);
		if (outputDir != null) {
			HeadContent head;
			try {
				head = readHeadOfLargestStderrFile(outputDir.root());
			}
			catch (Exception e) {
				throwable.addSuppressed(e);
				throw throwable;
			}
			if (head != null) {
				throw new RuntimeException(
					"First %d lines from %s:\n%s".formatted(STDERR_HEAD_LINES, head.file().getFileName(), head.lines()),
					throwable);
			}
		}
		throw throwable;
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

	private static @Nullable HeadContent readHeadOfLargestStderrFile(Path root) {
		try (var stream = Files.list(root).filter(Files::isRegularFile).sorted()) {
			return stream.filter(file -> file.getFileName().toString().endsWith(STDERR_SUFFIX)).max(
				comparingLong(OutputAttachingExtension::fileSize).thenComparing(
					file -> file.getFileName().toString())).filter(OutputAttachingExtension::notEmpty).map(
						file -> new HeadContent(file, readHead(file))).orElse(null);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to inspect output files in " + root, e);
		}
	}

	private static boolean notEmpty(Path file) {
		return fileSize(file) > 0;
	}

	private static long fileSize(Path file) {
		try {
			return Files.size(file);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to get file size for " + file, e);
		}
	}

	private static String readHead(Path file) {
		try (var lines = Files.lines(file, ProcessStarter.OUTPUT_ENCODING)) {
			return lines.limit(STDERR_HEAD_LINES).collect(joining(System.lineSeparator()));
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to read " + file, e);
		}
	}

	private record HeadContent(Path file, String lines) {
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
			return new OutputFiles(root.resolve(prefix + "-stdout.txt"), root.resolve(prefix + STDERR_SUFFIX));
		}
	}

}

/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings
class DefaultTestReporterTests {

	@TempDir
	Path tempDir;

	@Mock
	ExtensionContext extensionContext;

	@Captor
	ArgumentCaptor<ThrowingConsumer<Path>> actionCaptor;

	@InjectMocks
	DefaultTestReporter testReporter;

	@Test
	void copiesExistingFileToTarget() throws Throwable {
		testReporter.publishFile(Files.writeString(tempDir.resolve("source"), "content"), MediaType.TEXT_PLAIN_UTF_8);

		verify(extensionContext).publishFile(eq("source"), eq(MediaType.TEXT_PLAIN_UTF_8), actionCaptor.capture());
		actionCaptor.getValue().accept(tempDir.resolve("target"));

		assertThat(tempDir.resolve("target")).usingCharset(UTF_8).hasContent("content");
	}

	@Test
	void executesCustomActionWithTargetFile() throws Throwable {
		testReporter.publishFile("target", MediaType.APPLICATION_OCTET_STREAM,
			file -> Files.write(file, "content".getBytes()));

		verify(extensionContext).publishFile(eq("target"), eq(MediaType.APPLICATION_OCTET_STREAM),
			actionCaptor.capture());
		actionCaptor.getValue().accept(tempDir.resolve("target"));

		assertThat(tempDir.resolve("target")).hasContent("content");
	}

	@Test
	void copiesExistingDirectoryToTarget() throws Throwable {
		var source = Files.createDirectory(tempDir.resolve("source"));
		Files.writeString(source.resolve("source1"), "content1");
		var sourceSubDir = Files.createDirectory(source.resolve("subDir"));
		Files.writeString(sourceSubDir.resolve("source2"), "content2");
		Files.writeString(Files.createDirectory(sourceSubDir.resolve("subSubDir")).resolve("source3"), "content3");

		testReporter.publishDirectory(source);

		verify(extensionContext).publishDirectory(eq("source"), actionCaptor.capture());

		var target = tempDir.resolve("target");
		actionCaptor.getValue().accept(target);

		assertThat(target).isDirectory();
		assertThat(target.resolve("source1")).usingCharset(UTF_8).hasContent("content1");
		var targetSubDir = target.resolve("subDir");
		assertThat(targetSubDir.resolve("source2")).usingCharset(UTF_8).hasContent("content2");
		assertThat(targetSubDir.resolve("subSubDir").resolve("source3")).usingCharset(UTF_8).hasContent("content3");
	}

	@Test
	void executesCustomActionWithTargetDirectory() throws Throwable {
		testReporter.publishDirectory("target",
			dir -> Files.writeString(Files.createDirectory(dir).resolve("file"), "content", Charset.defaultCharset()));

		verify(extensionContext).publishDirectory(eq("target"), actionCaptor.capture());

		var target = tempDir.resolve("target");
		actionCaptor.getValue().accept(target);

		assertThat(target.resolve("file")).hasContent("content");
	}

	@Test
	@SuppressWarnings("DataFlowIssue") // publishFile() parameters are not @Nullable
	void failsWhenPublishingNullFile() {
		assertPreconditionViolationNotNullFor("file", () -> testReporter.publishFile(null, MediaType.TEXT_PLAIN));
	}

	@Test
	@SuppressWarnings("DataFlowIssue") // publishFile() parameters are not @Nullable
	void failsWhenPublishingFileWithNullMediaType() {
		assertPreconditionViolationNotNullFor("mediaType",
			() -> testReporter.publishFile(Path.of("test"), (MediaType) null));
	}

	@Test
	void failsWhenPublishingMissingFile() {
		var missingFile = tempDir.resolve("missingFile");

		assertPreconditionViolationFor(() -> testReporter.publishFile(missingFile, MediaType.APPLICATION_OCTET_STREAM))//
				.withMessage("file must exist: " + missingFile);
	}

	@Test
	void failsWhenPublishingDirectoryAsFile() {
		assertPreconditionViolationFor(() -> testReporter.publishFile(tempDir, MediaType.APPLICATION_OCTET_STREAM))//
				.withMessage("file must be a regular file: " + tempDir);
	}

	@Test
	@SuppressWarnings("DataFlowIssue") // publishDirectory() parameters are not @Nullable
	void failsWhenPublishingNullDirectory() {
		assertPreconditionViolationNotNullFor("directory", () -> testReporter.publishDirectory(null));
	}

	@Test
	void failsWhenPublishingMissingDirectory() {
		var missingDir = tempDir.resolve("missingDir");

		assertPreconditionViolationFor(() -> testReporter.publishDirectory(missingDir))//
				.withMessage("directory must exist: " + missingDir);
	}

	@Test
	void failsWhenPublishingFileAsDirectory() throws Exception {
		var dir = Files.createFile(tempDir.resolve("source"));

		assertPreconditionViolationFor(() -> testReporter.publishDirectory(dir))//
				.withMessage("path must represent a directory: " + dir);
	}

}

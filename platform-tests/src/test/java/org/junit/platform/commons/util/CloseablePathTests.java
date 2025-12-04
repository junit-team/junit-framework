/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.commons.util.CloseablePath.JAR_URI_SCHEME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.test.ConcurrencyTestingUtils;
import org.junit.platform.commons.util.CloseablePath.FileSystemProvider;
import org.junit.platform.engine.support.hierarchical.OpenTest4JAwareThrowableCollector;

class CloseablePathTests {

	URI uri;
	URI jarUri;

	List<CloseablePath> paths = new ArrayList<>();

	@BeforeEach
	void createUris() throws Exception {
		uri = getClass().getResource("/jartest.jar").toURI();
		jarUri = URI.create(JAR_URI_SCHEME + ':' + uri);
	}

	@AfterEach
	void closeAllPaths() {
		closeAll(paths);
	}

	@Test
	void parsesJarUri() throws Exception {
		FileSystemProvider fileSystemProvider = mock();

		FileSystem fileSystem = mock();
		when(fileSystemProvider.newFileSystem(any())).thenReturn(fileSystem);

		URI jarFileWithEntry = URI.create("jar:file:/example.jar!/com/example/Example.class");
		CloseablePath.create(jarFileWithEntry, fileSystemProvider).close();

		URI jarFileUri = URI.create("jar:file:/example.jar");
		verify(fileSystemProvider).newFileSystem(jarFileUri);
		verifyNoMoreInteractions(fileSystemProvider);
	}

	@Test
	void parsesRecursiveJarUri() throws Exception {
		FileSystemProvider fileSystemProvider = mock();

		FileSystem fileSystem = mock();
		when(fileSystemProvider.newFileSystem(any())).thenReturn(fileSystem);

		URI jarNestedFileWithEntry = URI.create(
			"jar:nested:file:/example.jar!/BOOT-INF/classes!/com/example/Example.class");
		CloseablePath.create(jarNestedFileWithEntry, fileSystemProvider).close();

		URI jarNestedFile = URI.create("jar:nested:file:/example.jar!/BOOT-INF/classes");
		verify(fileSystemProvider).newFileSystem(jarNestedFile);
		verifyNoMoreInteractions(fileSystemProvider);
	}

	@Test
	void createsAndClosesJarFileSystemOnceWhenCalledConcurrently() throws Exception {
		var numThreads = 50;

		FileSystemProvider fileSystemProvider = mock();
		when(fileSystemProvider.newFileSystem(any())) //
				.thenAnswer(invocation -> FileSystems.newFileSystem((URI) invocation.getArgument(0), Map.of()));

		paths = ConcurrencyTestingUtils.executeConcurrently(numThreads,
			() -> CloseablePath.create(uri, fileSystemProvider));
		verify(fileSystemProvider, only()).newFileSystem(jarUri);

		// Close all but the first path
		closeAll(paths.subList(1, numThreads));
		assertDoesNotThrow(() -> FileSystems.getFileSystem(jarUri), "FileSystem should still be open");

		// Close last remaining path
		paths.getFirst().close();
		assertThrows(FileSystemNotFoundException.class, () -> FileSystems.getFileSystem(jarUri),
			"FileSystem should have been closed");
	}

	@Test
	@SuppressWarnings("resource")
	void closingIsIdempotent() throws Exception {
		var path1 = CloseablePath.create(uri);
		paths.add(path1);
		var path2 = CloseablePath.create(uri);
		paths.add(path2);

		path1.close();
		path1.close();
		assertDoesNotThrow(() -> FileSystems.getFileSystem(jarUri), "FileSystem should still be open");

		path2.close();
		assertThrows(FileSystemNotFoundException.class, () -> FileSystems.getFileSystem(jarUri),
			"FileSystem should have been closed");
	}

	private static void closeAll(List<CloseablePath> paths) {
		var throwableCollector = new OpenTest4JAwareThrowableCollector();
		paths.forEach(closeablePath -> throwableCollector.execute(closeablePath::close));
		throwableCollector.assertEmpty();
	}
}

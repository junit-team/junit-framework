/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.junit.platform.commons.test.ConcurrencyTestingUtils.executeConcurrently;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.jimfs.Jimfs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.platform.commons.util.CloseablePath.FileSystemProvider;
import org.junit.platform.engine.support.hierarchical.OpenTest4JAwareThrowableCollector;

class CloseablePathTests {

	@AutoClose
	FileSystem jimfs = Jimfs.newFileSystem();

	List<CloseablePath> paths = new ArrayList<>();

	Path jarOnDefaultFileSystem;

	@BeforeEach
	void createUris() throws Exception {
		jarOnDefaultFileSystem = Path.of(requireNonNull(getClass().getResource("/jartest.jar")).toURI());
		assertThat(jarOnDefaultFileSystem).hasFileSystem(FileSystems.getDefault());
	}

	@AfterEach
	void closeAllPaths() {
		closeAll(paths);
	}

	@Test
	void parsesJarUri() throws Exception {
		FileSystemProvider fileSystemProvider = mock();

		FileSystem fileSystem = mock();
		when(fileSystemProvider.newFileSystem(any(Path.class))).thenReturn(fileSystem);

		var jarFileWithEntry = URI.create("jar:file:/example.jar!/com/example/Example.class");
		CloseablePath.create(jarFileWithEntry, fileSystemProvider).close();

		var jarFile = Path.of("/example.jar");
		verify(fileSystemProvider).newFileSystem(jarFile);
		verifyNoMoreInteractions(fileSystemProvider);
	}

	@Test
	void parsesRecursiveJarUri() throws Exception {
		FileSystemProvider fileSystemProvider = mock();

		FileSystem fileSystem = mock();
		when(fileSystemProvider.newFileSystem(any(URI.class))).thenReturn(fileSystem);

		URI jarNestedFileWithEntry = URI.create(
			"jar:nested:file:/example.jar!/BOOT-INF/classes!/com/example/Example.class");
		CloseablePath.create(jarNestedFileWithEntry, fileSystemProvider).close();

		URI jarNestedFile = URI.create("jar:nested:file:/example.jar!/BOOT-INF/classes");
		verify(fileSystemProvider).newFileSystem(jarNestedFile);
		verifyNoMoreInteractions(fileSystemProvider);
	}

	@Test
	void createsSeparateFileSystemsForJarFilesOnDefaultFileSystem() throws Exception {
		var numThreads = 50;
		var fileSystemProvider = spy(FileSystemProvider.DEFAULT);

		paths = executeConcurrently(numThreads,
			() -> CloseablePath.create(jarOnDefaultFileSystem.toUri(), fileSystemProvider));

		verify(fileSystemProvider, times(numThreads)).newFileSystem(jarOnDefaultFileSystem);
		assertThrows(FileSystemNotFoundException.class, () -> FileSystems.getFileSystem(jarUri(jarOnDefaultFileSystem)),
			"Uses separate file systems");
		assertThat(paths) //
				.extracting(it -> it.getPath().resolve("META-INF/MANIFEST.MF")) //
				.allSatisfy(Files::exists);
	}

	@Test
	void createsAndClosesJarFileSystemOnceWhenCalledConcurrentlyWithNonDefaultFileSystem() throws Exception {
		var numThreads = 50;
		var tempDir = Files.createTempDirectory(jimfs.getPath("/"), "junit-");
		var jar = Files.copy(jarOnDefaultFileSystem, tempDir.resolve("jartest.jar"));
		var jarUri = jarUri(jar);

		var fileSystemProvider = spy(FileSystemProvider.DEFAULT);

		paths = executeConcurrently(numThreads, () -> CloseablePath.create(jarUri, fileSystemProvider));
		verify(fileSystemProvider, only()).newFileSystem(URI.create("jar:" + jar.toUri()));

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
		var tempDir = Files.createTempDirectory(jimfs.getPath("/"), "junit-");
		var original = Files.copy(jarOnDefaultFileSystem, tempDir.resolve("original.jar"));
		var path1 = CloseablePath.create(jarUri(original));
		paths.add(path1);
		var path2 = CloseablePath.create(jarUri(original));
		paths.add(path2);

		path1.close();
		path1.close();
		assertDoesNotThrow(() -> FileSystems.getFileSystem(jarUri(original)), "FileSystem should still be open");

		path2.close();
		assertThrows(FileSystemNotFoundException.class, () -> FileSystems.getFileSystem(jarUri(original)),
			"FileSystem should have been closed");
	}

	@Test
	@DisabledOnOs(WINDOWS)
	void supportsSymlinkedJarsPointingToSameJar() throws Exception {
		var tempDir = Files.createTempDirectory(jimfs.getPath("/"), "junit-");
		var original = Files.copy(jarOnDefaultFileSystem, tempDir.resolve("original.jar"));
		var a = Files.createSymbolicLink(tempDir.resolve("a.jar"), original);
		var b = Files.createSymbolicLink(tempDir.resolve("b.jar"), original);

		var pathA = CloseablePath.create(a.toUri());
		paths.add(pathA);
		var pathB = CloseablePath.create(b.toUri());
		paths.add(pathB);

		assertThat(pathA.getPath().getFileSystem()).isEqualTo(pathB.getPath().getFileSystem());

		pathA.close();
		assertDoesNotThrow(() -> Files.walk(pathB.getPath()).close(), "FileSystem should still be open");
	}

	@Test
	@DisabledOnOs(WINDOWS)
	void resolvesSymlinkedPaths() throws Exception {
		var tempDir = Files.createTempDirectory(jimfs.getPath("/"), "junit-");
		var original = Files.copy(jarOnDefaultFileSystem, tempDir.resolve("original.jar"));
		var withSymlink = Files.createSymbolicLink(tempDir.resolve("a.jar"), original);

		var pathA = CloseablePath.create(jarUri(withSymlink));
		paths.add(pathA);
		var pathB = CloseablePath.create(jarUri(original));
		paths.add(pathB);

		assertThat(pathA.getPath().getFileSystem()).isEqualTo(pathB.getPath().getFileSystem());

		// Path a and b both resolve to the same file system so we know they
		// have the same cache key in ClosablePath. Now we check that
		// ZipFileSystemProvider stored the file system created for a with the
		// absolute real path. This implies that ClosablePath uses the same
		// cache key as ZipFileSystemProvider.
		var createdFileSystem = FileSystems.getFileSystem(jarUri(original));
		assertThat(createdFileSystem.toString()).isEqualTo(withSymlink.toString());
	}

	@Test
	void resolvesSpecialNameIdenticallyToZipFileSystemProvider() throws Exception {
		var tempDir = Files.createTempDirectory(jimfs.getPath("/"), "junit-");
		var original = Files.copy(jarOnDefaultFileSystem, tempDir.resolve("original.jar"));

		// Creates a path like `/tmp/junit-12345689/../junit-12345689/original.jar
		var withSpecialNames = tempDir.resolve("..", tempDir.getFileName().toString(),
			original.getFileName().toString());

		var pathA = CloseablePath.create(jarUri(withSpecialNames));
		paths.add(pathA);
		var pathB = CloseablePath.create(jarUri(original));
		paths.add(pathB);

		assertThat(pathA.getPath().getFileSystem()).isEqualTo(pathB.getPath().getFileSystem());

		// Path a and b both resolve to the same file system so we know they
		// have the same cache key in ClosablePath. Now we check that
		// ZipFileSystemProvider stored the file system created for a with the
		// absolute real path. This implies that ClosablePath uses the same
		// cache key as ZipFileSystemProvider.
		var createdFileSystem = FileSystems.getFileSystem(jarUri(original));
		assertThat(createdFileSystem.toString()).isEqualTo(withSpecialNames.toString());
	}

	private static URI jarUri(Path path) {
		return URI.create("jar:" + path.toUri() + "!/");
	}

	private static void closeAll(List<CloseablePath> paths) {
		var throwableCollector = new OpenTest4JAwareThrowableCollector();
		paths.forEach(closeablePath -> throwableCollector.execute(closeablePath::close));
		throwableCollector.assertEmpty();
	}
}

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.commons.test.ConcurrencyTestingUtils.executeConcurrently;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.CloseablePath.FileSystemProvider;
import org.junit.platform.commons.util.CloseablePath.JarUri;
import org.junit.platform.engine.support.hierarchical.OpenTest4JAwareThrowableCollector;

class CloseablePathTests {

	@AutoClose
	FileSystem jimfs = Jimfs.newFileSystem(Configuration.unix());

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
	void parsesRecursiveJarUri() {
		var jarNestedFileWithEntry = JarUri.parse(
			URI.create("jar:nested:file:/example.jar!/BOOT-INF/classes!/com/example/Example.class"));
		assertThat(jarNestedFileWithEntry.nestedUrl()).isEqualTo(
			URI.create("nested:file:/example.jar!/BOOT-INF/classes"));
		assertThat(jarNestedFileWithEntry.entry()).isEqualTo("/com/example/Example.class");

		var jarNestedFile = JarUri.parse(URI.create("jar:nested:file:/example.jar!/BOOT-INF/classes"));
		assertThat(jarNestedFile.nestedUrl()).isEqualTo(URI.create("nested:file:/example.jar"));
		assertThat(jarNestedFile.entry()).isEqualTo("/BOOT-INF/classes");
	}

	@Test
	void createsSeparateFileSystemsForJarFilesOnDefaultFileSystem() throws Exception {
		var numThreads = 50;
		var fileSystemProvider = spy(FileSystemProvider.DEFAULT);

		paths = executeConcurrently(numThreads,
			() -> CloseablePath.create(jarUri(jarOnDefaultFileSystem), fileSystemProvider));

		verify(fileSystemProvider, times(numThreads)).newFileSystem(jarOnDefaultFileSystem);
		assertThrows(FileSystemNotFoundException.class, () -> FileSystems.getFileSystem(jarUri(jarOnDefaultFileSystem)),
			"Uses separate file systems");
		assertThat(paths) //
				.extracting(it -> it.getPath().resolve("META-INF/MANIFEST.MF")) //
				.allSatisfy(Files::exists);
	}

	@Test
	void createsSeparateFileSystemsForJarFilesOnNonDefaultFileSystem() throws Exception {
		var numThreads = 50;
		var tempDir = Files.createTempDirectory(jimfs.getPath("/"), "junit-");
		var jar = Files.copy(jarOnDefaultFileSystem, tempDir.resolve("jartest.jar"));
		var jarUri = jarUri(jar);

		var fileSystemProvider = spy(FileSystemProvider.DEFAULT);

		paths = executeConcurrently(numThreads, () -> CloseablePath.create(jarUri, fileSystemProvider));
		verify(fileSystemProvider, times(numThreads)).newFileSystem(any(Path.class));
		assertThrows(FileSystemNotFoundException.class, () -> FileSystems.getFileSystem(jarUri),
			"Uses separate file systems");
		assertThat(paths) //
				.extracting(it -> it.getPath().resolve("META-INF/MANIFEST.MF")) //
				.allSatisfy(Files::exists);
	}

	@Test
	void closingIsIdempotent() throws Exception {
		var tempDir = Files.createTempDirectory(jimfs.getPath("/"), "junit-");
		var original = Files.copy(jarOnDefaultFileSystem, tempDir.resolve("original.jar"));
		var path1 = CloseablePath.create(jarUri(original));
		paths.add(path1);
		var path2 = CloseablePath.create(jarUri(original));
		paths.add(path2);

		assertThrows(FileSystemNotFoundException.class, () -> FileSystems.getFileSystem(jarUri(original)),
			"Uses separate file systems");

		path1.close();
		path1.close();
		assertThat(path1.getPath().getFileSystem().isOpen()).isFalse();
		assertThat(path2.getPath().getFileSystem().isOpen()).isTrue();

		path2.close();
		assertThat(path2.getPath().getFileSystem().isOpen()).isFalse();
	}

	@Test
	void resolvesPercentEncodedJarEntry() throws Exception {
		var tempDir = Files.createTempDirectory(jimfs.getPath("/"), "junit-");
		var jar = tempDir.resolve("unicode.jar");
		try (var out = new ZipOutputStream(Files.newOutputStream(jar))) {
			out.putNextEntry(new ZipEntry("com/example/café/Example.class"));
			out.closeEntry();
		}

		var path = CloseablePath.create(URI.create(jarUri(jar) + "com/example/caf%c3%a9/"));
		paths.add(path);

		assertThat(path.getPath()).exists();
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

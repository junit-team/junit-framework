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

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @since 1.0
 */
final class CloseablePath implements Closeable {

	private static final String FILE_URI_SCHEME = "file";
	static final String JAR_URI_SCHEME = "jar";
	private static final String JAR_FILE_EXTENSION = ".jar";
	private static final String JAR_URI_SEPARATOR = "!/";

	private static final Closeable NULL_CLOSEABLE = () -> {
	};

	private final AtomicBoolean closed = new AtomicBoolean();

	private final Path path;
	private final Closeable delegate;

	static CloseablePath create(URI uri) {
		return create(uri, FileSystemProvider.DEFAULT);
	}

	static CloseablePath create(URI uri, FileSystemProvider fileSystemProvider) {
		if (JAR_URI_SCHEME.equals(uri.getScheme())) {
			return createForJarScheme(uri, fileSystemProvider);
		}
		if (FILE_URI_SCHEME.equals(uri.getScheme()) && uri.getPath().endsWith(JAR_FILE_EXTENSION)) {
			var fileSystem = fileSystemProvider.newFileSystem(Path.of(uri));
			var root = fileSystem.getRootDirectories().iterator().next();
			return new CloseablePath(root, fileSystem);
		}
		return new CloseablePath(Path.of(uri), NULL_CLOSEABLE);
	}

	private static CloseablePath createForJarScheme(URI uri, FileSystemProvider fileSystemProvider) {
		var jarUri = JarUri.parse(uri);
		var fileSystem = fileSystemProvider.newFileSystem(Path.of(jarUri.nestedUrl));
		return new CloseablePath(fileSystem.getPath(jarUri.entry), fileSystem);
	}

	record JarUri(URI nestedUrl, String entry) {
		static JarUri parse(URI uri) {
			// Parsing: jar:<url>!/[<entry>], see java.net.JarURLConnection
			Preconditions.condition(JAR_URI_SCHEME.equals(uri.getScheme()),
				() -> "Unsupported URI scheme: " + uri.getScheme());
			var schemeSpecificPart = uri.getRawSchemeSpecificPart();
			int lastJarUriSeparator = schemeSpecificPart.lastIndexOf(JAR_URI_SEPARATOR);
			var nestedUri = URI.create(schemeSpecificPart.substring(0, lastJarUriSeparator));
			var jarEntry = schemeSpecificPart.substring(lastJarUriSeparator + 1);
			return new JarUri(nestedUri, jarEntry);
		}
	}

	private CloseablePath(Path path, Closeable delegate) {
		this.path = path;
		this.delegate = delegate;
	}

	public Path getPath() {
		return path;
	}

	@Override
	public void close() throws IOException {
		if (closed.compareAndSet(false, true)) {
			delegate.close();
		}
	}

	interface FileSystemProvider {

		@SuppressWarnings("Convert2Lambda") // to support spying using Mockito
		FileSystemProvider DEFAULT = new FileSystemProvider() {
			@Override
			public FileSystem newFileSystem(Path path) {
				try {
					return FileSystems.newFileSystem(path, Map.of());
				}
				catch (IOException e) {
					throw new UncheckedIOException("Failed to create file system for " + path, e);
				}
			}
		};

		FileSystem newFileSystem(Path path);
	}
}

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

import static java.util.Collections.emptyMap;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

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

	private static final ConcurrentMap<URI, ManagedFileSystem> MANAGED_FILE_SYSTEMS = new ConcurrentHashMap<>();

	private final AtomicBoolean closed = new AtomicBoolean();

	private final Path path;
	private final Closeable delegate;

	static CloseablePath create(URI uri) throws URISyntaxException {
		return create(uri, it -> FileSystems.newFileSystem(it, emptyMap()));
	}

	static CloseablePath create(URI uri, FileSystemProvider fileSystemProvider) throws URISyntaxException {
		if (JAR_URI_SCHEME.equals(uri.getScheme())) {
			// Parsing: jar:<url>!/[<entry>], see java.net.JarURLConnection
			String uriString = uri.toString();
			int lastJarUriSeparator = uriString.lastIndexOf(JAR_URI_SEPARATOR);
			String jarUri = uriString.substring(0, lastJarUriSeparator);
			String jarEntry = uriString.substring(lastJarUriSeparator + 1);
			return createForJarFileSystem(new URI(jarUri), fileSystem -> fileSystem.getPath(jarEntry),
				fileSystemProvider);
		}
		if (FILE_URI_SCHEME.equals(uri.getScheme()) && uri.getPath().endsWith(JAR_FILE_EXTENSION)) {
			return createForJarFileSystem(new URI(JAR_URI_SCHEME + ':' + uri),
				fileSystem -> fileSystem.getRootDirectories().iterator().next(), fileSystemProvider);
		}
		return new CloseablePath(Path.of(uri), NULL_CLOSEABLE);
	}

	private static CloseablePath createForJarFileSystem(URI jarUri, Function<FileSystem, Path> pathProvider,
			FileSystemProvider fileSystemProvider) {
		ManagedFileSystem managedFileSystem = MANAGED_FILE_SYSTEMS.compute(jarUri,
			(__, oldValue) -> oldValue == null ? new ManagedFileSystem(jarUri, fileSystemProvider) : oldValue.retain());
		Path path = pathProvider.apply(managedFileSystem.fileSystem);
		return new CloseablePath(path,
			() -> MANAGED_FILE_SYSTEMS.compute(jarUri, (__, ___) -> managedFileSystem.release()));
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

	private static class ManagedFileSystem {

		private final AtomicInteger referenceCount = new AtomicInteger(1);
		private final FileSystem fileSystem;
		private final URI jarUri;

		ManagedFileSystem(URI jarUri, FileSystemProvider fileSystemProvider) {
			this.jarUri = jarUri;
			try {
				fileSystem = fileSystemProvider.newFileSystem(jarUri);
			}
			catch (IOException e) {
				throw new UncheckedIOException("Failed to create file system for " + jarUri, e);
			}
		}

		private ManagedFileSystem retain() {
			referenceCount.incrementAndGet();
			return this;
		}

		private @Nullable ManagedFileSystem release() {
			if (referenceCount.decrementAndGet() == 0) {
				close();
				return null;
			}
			return this;
		}

		private void close() {
			try {
				fileSystem.close();
			}
			catch (IOException e) {
				throw new UncheckedIOException("Failed to close file system for " + jarUri, e);
			}
		}
	}

	interface FileSystemProvider {
		FileSystem newFileSystem(URI uri) throws IOException;
	}
}

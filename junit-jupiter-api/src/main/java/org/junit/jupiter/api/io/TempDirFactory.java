/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.io;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * {@code TempDirFactory} defines the SPI for creating temporary directories
 * programmatically.
 *
 * <p>A temporary directory factory is typically used to gain control over the
 * temporary directory creation, like defining the parent directory or the file
 * system that should be used.
 *
 * <p>Implementations must provide a no-args constructor and should not make any
 * assumptions regarding when and how many times they are instantiated, but they
 * can assume that {@link #createTempDirectory(AnnotatedElementContext, ExtensionContext)}
 * and {@link #close()} will both be called once per instance, in this order,
 * and from the same thread.
 *
 * <p>A {@link TempDirFactory} can be configured <em>globally</em> for the
 * entire test suite via the {@value TempDir#DEFAULT_FACTORY_PROPERTY_NAME}
 * configuration parameter (see the User Guide for details) or <em>locally</em>
 * for a test class field or method parameter via the {@link TempDir @TempDir}
 * annotation.
 *
 * @since 5.10
 * @see TempDir @TempDir
 */
@FunctionalInterface
@API(status = MAINTAINED, since = "5.13.3")
public interface TempDirFactory extends Closeable {

	/**
	 * Create a new temporary directory.
	 *
	 * <p>Depending on the implementation, the resulting {@link Path} may or may
	 * not be associated with the {@link java.nio.file.FileSystems#getDefault()
	 * default FileSystem}.
	 *
	 * @param elementContext the context of the field or parameter where
	 * {@code @TempDir} is declared; never {@code null}
	 * @param extensionContext the current extension context; never {@code null}
	 * @return the path to the newly created temporary directory; never {@code null}
	 * @throws Exception in case of failures
	 */
	Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
			throws Exception;

	/**
	 * {@inheritDoc}
	 */
	@Override
	default void close() throws IOException {
	}

	/**
	 * Standard {@link TempDirFactory} implementation which delegates to
	 * {@link Files#createTempDirectory} using {@code "junit-"} as prefix.
	 *
	 * <p>The created temporary directory is always created in the default
	 * file system with the system's default temporary directory as its parent.
	 *
	 * @see Files#createTempDirectory(java.lang.String, java.nio.file.attribute.FileAttribute[])
	 */
	class Standard implements TempDirFactory {

		public static final TempDirFactory INSTANCE = new Standard();

		private static final String TEMP_DIR_PREFIX = "junit-";

		public Standard() {
		}

		@Override
		public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
				throws IOException {
			return Files.createTempDirectory(TEMP_DIR_PREFIX);
		}

	}

}

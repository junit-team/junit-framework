/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.reporting.testutil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {

	public static Path findPath(Path rootDir, String syntaxAndPattern) {
		var matcher = rootDir.getFileSystem().getPathMatcher(syntaxAndPattern);
		try (var files = Files.walk(rootDir)) {
			return files.filter(matcher::matches).findFirst() //
					.orElseThrow(() -> new AssertionError(
						"Failed to find file matching '%s' in %s".formatted(syntaxAndPattern, rootDir)));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private FileUtils() {
	}
}

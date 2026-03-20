/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * A {@link TempDirDeletionStrategy} for testing that simulates a deletion
 * failure for any path ending in {@link #UNDELETABLE_PATH}.
 */
@NullMarked
public class FailingTempDirDeletionStrategy implements TempDirDeletionStrategy {

	/**
	 * A path segment that, when present at the end of a path, causes deletion
	 * to fail with a simulated {@link java.io.IOException}.
	 */
	public static final Path UNDELETABLE_PATH = Path.of("undeletable");

	@Override
	public DeletionResult delete(Path tempDir, AnnotatedElementContext elementContext,
			ExtensionContext extensionContext) throws IOException {

		return Standard.INSTANCE.delete(tempDir, path -> {
			if (path.endsWith(UNDELETABLE_PATH)) {
				throw new IOException("Simulated failure");
			}
			else {
				Files.delete(path);
			}
		});
	}
}

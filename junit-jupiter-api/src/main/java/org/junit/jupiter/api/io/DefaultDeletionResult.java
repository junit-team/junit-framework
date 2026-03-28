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

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.io.TempDirDeletionStrategy.DeletionException;
import org.junit.jupiter.api.io.TempDirDeletionStrategy.DeletionFailure;
import org.junit.jupiter.api.io.TempDirDeletionStrategy.DeletionResult;
import org.junit.platform.commons.util.Preconditions;

record DefaultDeletionResult(Path rootDir, List<DeletionFailure> failures) implements DeletionResult {

	DefaultDeletionResult(Path rootDir, List<DeletionFailure> failures) {
		this.rootDir = rootDir;
		this.failures = List.copyOf(failures);
	}

	@Override
	public Optional<DeletionException> toException() {
		if (isSuccessful()) {
			return Optional.empty();
		}
		var joinedPaths = failures().stream() //
				.map(DeletionFailure::path) //
				.sorted() //
				.distinct() //
				.map(path -> relativizeSafely(rootDir(), path).toString()) //
				.map(path -> path.isEmpty() ? "<root>" : path) //
				.collect(joining(", "));
		var exception = new DeletionException("Failed to delete temp directory " + rootDir().toAbsolutePath()
				+ ". The following paths could not be deleted (see suppressed exceptions for details): " + joinedPaths);
		failures().stream() //
				.sorted(comparing(DeletionFailure::path)) //
				.map(DeletionFailure::cause) //
				.forEach(exception::addSuppressed);
		return Optional.of(exception);
	}

	private static Path relativizeSafely(Path rootDir, Path path) {
		try {
			return rootDir.relativize(path);
		}
		catch (IllegalArgumentException e) {
			return path;
		}
	}

	static final class Builder implements DeletionResult.Builder {

		private final Path rootDir;
		private final List<DeletionFailure> failures = new ArrayList<>();

		Builder(Path rootDir) {
			this.rootDir = rootDir;
		}

		@Override
		public Builder addFailure(Path path, Exception cause) {
			Preconditions.notNull(path, "path must not be null");
			Preconditions.notNull(cause, "cause must not be null");
			failures.add(new DefaultDeletionFailure(path, cause));
			return this;
		}

		@Override
		public DefaultDeletionResult build() {
			return new DefaultDeletionResult(rootDir, failures);
		}

	}

	record DefaultDeletionFailure(Path path, Exception cause) implements DeletionFailure {
	}
}

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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.io.TempDirDeletionStrategy.DeletionFailure;
import org.junit.jupiter.api.io.TempDirDeletionStrategy.DeletionResult;

record DefaultDeletionResult(List<DeletionFailure> failures) implements DeletionResult {

	DefaultDeletionResult(List<DeletionFailure> failures) {
		this.failures = List.copyOf(failures);
	}

	static final class Builder implements DeletionResult.Builder {

		private final List<DeletionFailure> failures = new ArrayList<>();

		@Override
		public Builder addFailure(Path path, Exception cause) {
			failures.add(new DefaultDeletionFailure(path, cause));
			return this;
		}

		@Override
		public DefaultDeletionResult build() {
			return new DefaultDeletionResult(failures);
		}

	}

	record DefaultDeletionFailure(Path path, Exception cause) implements DeletionFailure {
	}
}

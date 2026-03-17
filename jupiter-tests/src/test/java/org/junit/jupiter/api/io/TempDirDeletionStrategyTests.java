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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.api.io.TempDirDeletionStrategy.DeletionException;
import org.junit.jupiter.api.io.TempDirDeletionStrategy.IgnoreFailures;
import org.junit.platform.commons.logging.LogRecordListener;

/**
 * @since 6.1
 */
class TempDirDeletionStrategyTests {

	@Nested
	class IgnoreFailuresTests {

		@Test
		void logsAndIgnoresFailures(@TempDir Path tempDir, @TrackLogRecords LogRecordListener log) throws Exception {
			var undeletableDir = Files.createDirectory(tempDir.resolve("undeletable"));
			var strategy = new IgnoreFailures(new FailingTempDirDeletionStrategy());

			var result = strategy.delete(undeletableDir, mock(), mock());

			assertThat(result.isSuccessful());

			var loggedWarnings = log.stream(IgnoreFailures.class, Level.WARNING).toList();

			assertThat(loggedWarnings) //
					.extracting(LogRecord::getMessage) //
					.containsExactly(
						"Failed to delete temp directory %s. The following paths could not be deleted (see suppressed exceptions for details): <root>".formatted(
							undeletableDir.toAbsolutePath()));

			var exception = loggedWarnings.getFirst().getThrown();
			assertThat(exception).isInstanceOf(DeletionException.class);
			assertThat(exception.getSuppressed()).hasSize(1);
			assertThat(exception.getSuppressed()[0]) //
					.isInstanceOf(IOException.class) //
					.hasMessage("Simulated failure");
		} //

	}

}

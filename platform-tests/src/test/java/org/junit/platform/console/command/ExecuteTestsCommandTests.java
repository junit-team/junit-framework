/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * @since 1.10
 */
class ExecuteTestsCommandTests {

	private final TestExecutionSummary summary = mock();
	private final ConsoleTestExecutor consoleTestExecutor = mock();
	private final ExecuteTestsCommand command = new ExecuteTestsCommand((__, ___) -> consoleTestExecutor);

	@BeforeEach
	void setUp() {
		when(consoleTestExecutor.execute(any(), any(), anyBoolean())).thenReturn(summary);
	}

	@Test
	void hasStatusCode0ForNoTotalFailures() {
		when(summary.getTotalFailureCount()).thenReturn(0L);

		command.execute();

		assertThat(command.getExitCode()).isEqualTo(0);
	}

	@Test
	void hasStatusCode1ForForAnyFailure() {
		when(summary.getTotalFailureCount()).thenReturn(1L);

		command.execute();

		assertThat(command.getExitCode()).isEqualTo(1);
	}

	/**
	 * @since 1.3
	 */
	@Test
	void hasStatusCode2ForNoTestsAndHasOptionFailIfNoTestsFound() {
		when(summary.getTestsFoundCount()).thenReturn(0L);

		command.execute("--fail-if-no-tests");

		assertThat(command.getExitCode()).isEqualTo(2);
	}

	/**
	 * @since 1.3
	 */
	@Test
	void hasStatusCode0ForTestsAndHasOptionFailIfNoTestsFound() {
		when(summary.getTestsFoundCount()).thenReturn(1L);
		when(summary.getTotalFailureCount()).thenReturn(0L);

		command.execute("--fail-if-no-tests");

		assertThat(command.getExitCode()).isEqualTo(0);
	}

	/**
	 * @since 1.3
	 */
	@Test
	void hasStatusCode0ForNoTestsAndNotFailIfNoTestsFound() {
		when(summary.getTestsFoundCount()).thenReturn(0L);

		command.execute();

		assertThat(command.getExitCode()).isEqualTo(0);
	}

	@Test
	void parseValidXmlReportsDirs() {
		var dir = Path.of("build", "test-results");
		// @formatter:off
		assertAll(
				() -> assertEquals(Optional.empty(), parseArgs().getReportsDir()),
				() -> assertEquals(Optional.of(dir), parseArgs("--reports-dir", "build/test-results").getReportsDir()),
				() -> assertEquals(Optional.of(dir), parseArgs("--reports-dir=build/test-results").getReportsDir())
		);
		// @formatter:on
	}

	@Test
	void parseValidFailFast() {
		// @formatter:off
		assertAll(
				() -> assertFalse(parseArgs().isFailFast()),
				() -> assertTrue(parseArgs("--fail-fast").isFailFast())
		);
		// @formatter:on
	}

	private ExecuteTestsCommand parseArgs(String... args) {
		command.parseArgs(args);
		return command;
	}

}

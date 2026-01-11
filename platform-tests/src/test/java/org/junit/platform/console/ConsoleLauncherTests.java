/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @since 1.0
 */
class ConsoleLauncherTests {

	private final StringWriter stringWriter = new StringWriter();
	private final PrintWriter printSink = new PrintWriter(stringWriter);

	@ParameterizedTest(name = "cmd={0}")
	@EmptySource
	@MethodSource("commandsWithEmptyOptionExitCodes")
	void displayHelp(String command) {
		var exitCode = ConsoleLauncher.run(printSink, printSink, command, "--help").getExitCode();

		assertEquals(0, exitCode);
		assertThat(output()).contains("--help");
	}

	@ParameterizedTest(name = "cmd={0}")
	@EmptySource
	@MethodSource("commandsWithEmptyOptionExitCodes")
	void displayVersion(String command) {
		var exitCode = ConsoleLauncher.run(printSink, printSink, command, "--version").getExitCode();

		assertEquals(0, exitCode);
		assertThat(output()).contains("JUnit Platform Console Launcher");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("commandsWithEmptyOptionExitCodes")
	void displayBanner(String command) {
		ConsoleLauncher.run(printSink, printSink, command);

		assertThat(output()).contains("Thanks for using JUnit!");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("commandsWithEmptyOptionExitCodes")
	void disableBanner(String command, int expectedExitCode) {
		var exitCode = ConsoleLauncher.run(printSink, printSink, command, "--disable-banner").getExitCode();

		assertEquals(expectedExitCode, exitCode);
		assertThat(output()).doesNotContain("Thanks for using JUnit!");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("commandsWithEmptyOptionExitCodes")
	void executeWithUnknownCommandLineOption(String command) {
		var exitCode = ConsoleLauncher.run(printSink, printSink, command, "--all").getExitCode();

		assertEquals(-1, exitCode);
		assertThat(output()).contains("Unknown option: '--all'").contains("Usage:");
	}

	private String output() {
		return stringWriter.toString();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("commandsWithEmptyOptionExitCodes")
	void executeWithoutCommandLineOptions(String command, int expectedExitCode) {
		var actualExitCode = ConsoleLauncher.run(printSink, printSink, command).getExitCode();

		assertEquals(expectedExitCode, actualExitCode);
	}

	static Stream<Arguments> commandsWithEmptyOptionExitCodes() {
		return Stream.of( //
			arguments("execute", -1), //
			arguments("discover", -1), //
			arguments("engines", 0) //
		);
	}

}

/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.console.command.StdStreamTestCase;
import org.junit.platform.console.subpackage.FailingTestCase;

/**
 * @since 1.0
 */
class ConsoleLauncherIntegrationTests {

	@Test
	void executeWithoutSubcommandFailsAndPrintsHelpInformation() {
		var result = new ConsoleLauncherWrapper().execute(-1);
		assertAll("empty args array results in display of help information and an exception stacktrace", //
			() -> assertThat(result.err).contains("help information"), //
			() -> assertThat(result.err).contains("Missing required subcommand") //
		);
	}

	@Test
	void executeWithoutExcludeClassnameOptionDoesNotExcludeClassesAndMustIncludeAllClassesMatchingTheStandardClassnamePattern() {
		String[] args = { "execute", "-e", "junit-jupiter", "-p", "org.junit.platform.console.subpackage" };
		assertEquals(9, new ConsoleLauncherWrapper().execute(args).getTestsFoundCount());
	}

	@Test
	void executeWithExcludeClassnameOptionExcludesClasses() {
		String[] args = { "execute", "-e", "junit-jupiter", "-p", "org.junit.platform.console.subpackage",
				"--exclude-classname", "^org\\.junit\\.platform\\.console\\.subpackage\\..*" };
		var result = new ConsoleLauncherWrapper().execute(args);
		assertAll("all subpackage test classes are excluded by the class name filter", //
			() -> assertArrayEquals(args, result.args), //
			() -> assertEquals(0, result.code), //
			() -> assertEquals(0, result.getTestsFoundCount()) //
		);
	}

	@Test
	void executeWithExcludeMethodNameOptionExcludesMethods() {
		var line = "execute -e junit-jupiter -p org.junit.platform.console.subpackage --exclude-methodname"
				+ " ^org\\.junit\\.platform\\.console\\.subpackage\\..+#test";
		var args = line.split(" ");
		var result = new ConsoleLauncherWrapper().execute(args);
		assertAll("all subpackage test methods are excluded by the method name filter", //
			() -> assertArrayEquals(args, result.args), //
			() -> assertEquals(0, result.code), //
			() -> assertEquals(0, result.getTestsFoundCount()) //
		);
	}

	@ParameterizedTest
	@ValueSource(strings = { //
			"execute -e junit-jupiter -o java.base", //
			"execute -e junit-jupiter --select-module java.base" //
	})
	void executeSelectingModuleNames(String line) {
		var args = line.split(" ");
		assertEquals(0, new ConsoleLauncherWrapper().execute(args).getTestsFoundCount());
	}

	@Test
	void executeScanModules() {
		String[] args = { "execute", "-e", "junit-jupiter", "--scan-modules" };
		assertEquals(0, new ConsoleLauncherWrapper().execute(args).getTestsFoundCount());
	}

	@ParameterizedTest
	@FieldSource("redirectStreamArguments")
	void executeWithRedirectedStdStream(String redirectedStream, int outputFileSize, @TempDir Path tempDir)
			throws IOException {

		var outputFile = tempDir.resolve("output.txt");
		var line = "execute -e junit-jupiter --select-class %s %s %s".formatted(StdStreamTestCase.class.getName(),
			redirectedStream, outputFile);
		var args = line.split(" ");
		new ConsoleLauncherWrapper().execute(args);

		assertTrue(Files.exists(outputFile), "File does not exist.");
		assertEquals(outputFileSize, Files.size(outputFile), "Invalid file size.");
	}

	static List<Arguments> redirectStreamArguments = List.of(
		arguments("--redirect-stdout", StdStreamTestCase.getStdoutOutputFileSize()),
		arguments("--redirect-stderr", StdStreamTestCase.getStderrOutputFileSize()));

	@Test
	void executeWithRedirectedStdStreamsToSameFile(@TempDir Path tempDir) throws IOException {
		var outputFile = tempDir.resolve("output.txt");
		var line = "execute -e junit-jupiter --select-class %s --redirect-stdout %s --redirect-stderr %s".formatted(
			StdStreamTestCase.class.getName(), outputFile, outputFile);
		var args = line.split(" ");
		new ConsoleLauncherWrapper().execute(args);

		assertTrue(Files.exists(outputFile), "File does not exist.");
		assertEquals(StdStreamTestCase.getStdoutOutputFileSize() + StdStreamTestCase.getStderrOutputFileSize(),
			Files.size(outputFile), "Invalid file size.");
	}

	@Test
	void stopsAfterFirstFailingTest() {
		var result = new ConsoleLauncherWrapper().execute(1, "execute", "-e", "junit-jupiter", "--select-class",
			FailingTestCase.class.getName(), "--fail-fast", "--disable-ansi-colors");

		assertThat(result.getTestsStartedCount()).isEqualTo(1);
		assertThat(result.getTestsFailedCount()).isEqualTo(1);
		assertThat(result.getTestsSkippedCount()).isEqualTo(1);

		assertThat(result.out).endsWith("%nTest execution was cancelled due to --fail-fast mode.%n%n".formatted());
	}

}

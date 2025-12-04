/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.command;

import static org.junit.platform.console.command.ExitCode.NO_TESTS_FOUND;
import static org.junit.platform.console.command.ExitCode.SUCCESS;
import static org.junit.platform.console.command.ExitCode.TEST_FAILED;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.platform.console.options.TestConsoleOutputOptions;
import org.junit.platform.console.options.TestConsoleOutputOptionsMixin;
import org.junit.platform.console.options.TestDiscoveryOptions;
import org.junit.platform.console.options.TestDiscoveryOptionsMixin;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(//
		name = "execute", //
		description = "Execute tests" //
)
class ExecuteTestsCommand extends BaseCommand<TestExecutionSummary> implements CommandLine.IExitCodeGenerator {
	private final ConsoleTestExecutor.Factory consoleTestExecutorFactory;

	@Mixin
	TestDiscoveryOptionsMixin discoveryOptions;

	@Mixin
	TestConsoleOutputOptionsMixin testOutputOptions;

	@ArgGroup(validate = false, order = 6, heading = "%n@|bold REPORTING|@%n%n")
	ReportingOptions reportingOptions;

	ExecuteTestsCommand(ConsoleTestExecutor.Factory consoleTestExecutorFactory) {
		this.consoleTestExecutorFactory = consoleTestExecutorFactory;
	}

	@Override
	protected TestExecutionSummary execute(PrintWriter out) {
		return consoleTestExecutorFactory.create(toTestDiscoveryOptions(), toTestConsoleOutputOptions()) //
				.execute(out, getReportsDir(), isFailFast());
	}

	Optional<Path> getReportsDir() {
		return getReportingOptions().flatMap(ReportingOptions::getReportsDir);
	}

	boolean isFailFast() {
		return getReportingOptions().map(options -> options.failFast).orElse(false);
	}

	private Optional<ReportingOptions> getReportingOptions() {
		return Optional.ofNullable(reportingOptions);
	}

	TestDiscoveryOptions toTestDiscoveryOptions() {
		return this.discoveryOptions == null //
				? new TestDiscoveryOptions() //
				: this.discoveryOptions.toTestDiscoveryOptions();
	}

	TestConsoleOutputOptions toTestConsoleOutputOptions() {
		TestConsoleOutputOptions testOutputOptions = this.testOutputOptions.toTestConsoleOutputOptions();
		testOutputOptions.setAnsiColorOutputDisabled(this.ansiColorOption.isDisableAnsiColors());
		return testOutputOptions;
	}

	@Override
	public int getExitCode() {
		TestExecutionSummary executionResult = commandSpec.commandLine().getExecutionResult();
		boolean failIfNoTests = getReportingOptions().map(it -> it.failIfNoTests).orElse(false);
		if (failIfNoTests && executionResult.getTestsFoundCount() == 0) {
			return NO_TESTS_FOUND;
		}
		return executionResult.getTotalFailureCount() == 0 ? SUCCESS : TEST_FAILED;
	}

	static class ReportingOptions {

		@Option(names = "--fail-if-no-tests", description = "Fail and return exit status code 2 if no tests are found.")
		private boolean failIfNoTests;

		/**
		 * @since 6.0
		 */
		@Option(names = "--fail-fast", description = "Stops test execution after the first failed test.")
		private boolean failFast;

		@Option(names = "--reports-dir", paramLabel = "DIR", description = "Enable report output into a specified local directory (will be created if it does not exist).")
		private @Nullable Path reportsDir;

		Optional<Path> getReportsDir() {
			return Optional.ofNullable(reportsDir);
		}
	}

}

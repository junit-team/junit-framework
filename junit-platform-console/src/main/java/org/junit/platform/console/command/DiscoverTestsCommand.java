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

import java.io.PrintWriter;

import org.junit.platform.console.options.TestConsoleOutputOptions;
import org.junit.platform.console.options.TestConsoleOutputOptionsMixin;
import org.junit.platform.console.options.TestDiscoveryOptions;
import org.junit.platform.console.options.TestDiscoveryOptionsMixin;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(//
		name = "discover", //
		description = "Discover tests" //
)
class DiscoverTestsCommand extends BaseCommand<Void> {

	private final ConsoleTestExecutor.Factory consoleTestExecutorFactory;

	@Mixin
	TestDiscoveryOptionsMixin discoveryOptions;

	@Mixin
	TestConsoleOutputOptionsMixin testOutputOptions;

	DiscoverTestsCommand(ConsoleTestExecutor.Factory consoleTestExecutorFactory) {
		this.consoleTestExecutorFactory = consoleTestExecutorFactory;
	}

	@Override
	protected Void execute(PrintWriter out) {
		TestDiscoveryOptions discoveryOptions = this.discoveryOptions.toTestDiscoveryOptions();
		TestConsoleOutputOptions testOutputOptions = this.testOutputOptions.toTestConsoleOutputOptions();
		testOutputOptions.setAnsiColorOutputDisabled(this.ansiColorOption.isDisableAnsiColors());
		this.consoleTestExecutorFactory.create(discoveryOptions, testOutputOptions).discover(out);
		return null;
	}

}

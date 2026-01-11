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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.commons.util.ClassLoaderUtils.getDefaultClassLoader;
import static org.junit.platform.launcher.core.LauncherFactoryForTestingPurposesOnly.createLauncher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.platform.console.options.Details;
import org.junit.platform.console.options.TestConsoleOutputOptions;
import org.junit.platform.console.options.TestDiscoveryOptions;
import org.junit.platform.engine.support.hierarchical.DemoHierarchicalTestEngine;

/**
 * @since 1.0
 */
class ConsoleTestExecutorTests {

	private static final Runnable FAILING_BLOCK = () -> fail("should fail");
	private static final Runnable SUCCEEDING_TEST = () -> {
	};

	private final StringWriter stringWriter = new StringWriter();
	private final TestDiscoveryOptions discoveryOptions = new TestDiscoveryOptions();
	private final TestConsoleOutputOptions outputOptions = new TestConsoleOutputOptions();
	private final DemoHierarchicalTestEngine dummyTestEngine = new DemoHierarchicalTestEngine();

	{
		discoveryOptions.setScanClasspath(true);
	}

	@Test
	void printsSummary() {
		dummyTestEngine.addTest("succeedingTest", SUCCEEDING_TEST);
		dummyTestEngine.addTest("failingTest", FAILING_BLOCK);

		var task = new ConsoleTestExecutor(discoveryOptions, outputOptions, () -> createLauncher(dummyTestEngine));
		task.execute(new PrintWriter(stringWriter), Optional.empty(), false);

		assertThat(stringWriter.toString()).contains("Test run finished after", "2 tests found", "0 tests skipped",
			"2 tests started", "0 tests aborted", "1 tests successful", "1 tests failed");
	}

	@Test
	void printsDetailsIfTheyAreNotHidden() {
		outputOptions.setDetails(Details.FLAT);

		dummyTestEngine.addTest("failingTest", FAILING_BLOCK);

		var task = new ConsoleTestExecutor(discoveryOptions, outputOptions, () -> createLauncher(dummyTestEngine));
		task.execute(new PrintWriter(stringWriter), Optional.empty(), false);

		assertThat(stringWriter.toString()).contains("Test execution started.");
	}

	@Test
	void printsNoDetailsIfTheyAreHidden() {
		outputOptions.setDetails(Details.NONE);

		dummyTestEngine.addTest("failingTest", FAILING_BLOCK);

		var task = new ConsoleTestExecutor(discoveryOptions, outputOptions, () -> createLauncher(dummyTestEngine));
		task.execute(new PrintWriter(stringWriter), Optional.empty(), false);

		assertThat(stringWriter.toString()).doesNotContain("Test execution started.");
	}

	@Test
	void printsFailuresEvenIfDetailsAreHidden() {
		outputOptions.setDetails(Details.NONE);

		dummyTestEngine.addTest("failingTest", FAILING_BLOCK);
		dummyTestEngine.addContainer("failingContainer", FAILING_BLOCK);

		var task = new ConsoleTestExecutor(discoveryOptions, outputOptions, () -> createLauncher(dummyTestEngine));
		task.execute(new PrintWriter(stringWriter), Optional.empty(), false);

		assertThat(stringWriter.toString()).contains("Failures (2)", "failingTest", "failingContainer");
	}

	@Test
	void usesCustomClassLoaderIfAdditionalClassPathEntriesArePresent() {
		discoveryOptions.setAdditionalClasspathEntries(List.of(Path.of(".")));

		var oldClassLoader = getDefaultClassLoader();
		dummyTestEngine.addTest("failingTest",
			() -> assertSame(oldClassLoader, getDefaultClassLoader(), "should fail"));

		var task = new ConsoleTestExecutor(discoveryOptions, outputOptions, () -> createLauncher(dummyTestEngine));
		task.execute(new PrintWriter(stringWriter), Optional.empty(), false);

		assertThat(stringWriter.toString()).contains("failingTest", "should fail", "1 tests failed");
	}

	@Test
	void usesSameClassLoaderIfNoAdditionalClassPathEntriesArePresent() {
		discoveryOptions.setAdditionalClasspathEntries(List.of());

		var oldClassLoader = getDefaultClassLoader();
		dummyTestEngine.addTest("failingTest",
			() -> assertNotSame(oldClassLoader, getDefaultClassLoader(), "should fail"));

		var task = new ConsoleTestExecutor(discoveryOptions, outputOptions, () -> createLauncher(dummyTestEngine));
		task.execute(new PrintWriter(stringWriter), Optional.empty(), false);

		assertThat(stringWriter.toString()).contains("failingTest", "should fail", "1 tests failed");
	}

}

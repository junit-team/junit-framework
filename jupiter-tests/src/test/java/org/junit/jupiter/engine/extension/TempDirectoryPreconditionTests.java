/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.Events;

/**
 * Integration tests for preconditions and assertions in the {@link TempDirectory}
 * extension.
 *
 * @since 5.9
 */
class TempDirectoryPreconditionTests extends AbstractJupiterTestEngineTests {

	@Test
	@DisplayName("Valid and invalid @TempDir parameter types")
	void parameterTypes() {
		EngineExecutionResults executionResults = executeTestsForClass(ParameterTypeTestCase.class);
		Events tests = executionResults.testEvents();
		tests.assertStatistics(stats -> stats.started(2).failed(1).succeeded(1));
		tests.succeeded().assertEventsMatchExactly(event(test("validTempDirType"), finishedSuccessfully()));
		// @formatter:off
		tests.failed().assertEventsMatchExactly(event(test("invalidTempDirType"),
			finishedWithFailure(instanceOf(ParameterResolutionException.class), message("""
					Failed to resolve parameter [java.lang.String text] in method \
					[void org.junit.jupiter.engine.extension.TempDirectoryPreconditionTests$ParameterTypeTestCase.invalidTempDirType(java.lang.String)]: \
					Can only resolve @TempDir parameter of type java.nio.file.Path or java.io.File but was: java.lang.String\
					"""))));
		// @formatter:on
	}

	@Test
	@DisplayName("final static @TempDir fields are not supported")
	void finalStaticFieldIsNotSupported() {
		EngineExecutionResults executionResults = executeTestsForClass(FinalStaticFieldTestCase.class);
		Events containers = executionResults.containerEvents();
		containers.assertStatistics(stats -> stats.started(2).failed(1).succeeded(1));
		containers.succeeded().assertEventsMatchExactly(event(container("junit-jupiter"), finishedSuccessfully()));
		containers.failed().assertEventsMatchExactly(event(container("FinalStaticFieldTestCase"),
			finishedWithFailure(instanceOf(ExtensionConfigurationException.class), message("""
					@TempDir field [static final java.nio.file.Path \
					org.junit.jupiter.engine.extension.TempDirectoryPreconditionTests$FinalStaticFieldTestCase.path] \
					must not be declared as final.\
					"""))));
	}

	@Test
	@DisplayName("final instance @TempDir fields are not supported")
	void finalInstanceFieldIsNotSupported() {
		EngineExecutionResults executionResults = executeTestsForClass(FinalInstanceFieldTestCase.class);
		Events tests = executionResults.testEvents();
		tests.assertStatistics(stats -> stats.started(1).failed(1).succeeded(0));
		tests.failed().assertEventsMatchExactly(
			event(test("test()"), finishedWithFailure(instanceOf(ExtensionConfigurationException.class), message("""
					@TempDir field [final java.nio.file.Path \
					org.junit.jupiter.engine.extension.TempDirectoryPreconditionTests$FinalInstanceFieldTestCase.path] \
					must not be declared as final.\
					"""))));
	}

	// -------------------------------------------------------------------

	static class ParameterTypeTestCase {

		@Test
		void validTempDirType(@TempDir File file, @TempDir Path path) {
		}

		@Test
		void invalidTempDirType(@TempDir String text) {
		}
	}

	static class FinalStaticFieldTestCase {

		static final @TempDir Path path = Path.of(".");

		@Test
		void test() {
		}
	}

	static class FinalInstanceFieldTestCase {

		final @TempDir Path path = Path.of(".");

		@Test
		void test() {
		}
	}

}

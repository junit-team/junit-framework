/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.ConcurrencyTestingUtils.executeConcurrently;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.fakes.TestDescriptorStub;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.0
 */
class SummaryGenerationTests {

	private final SummaryGeneratingListener listener = new SummaryGeneratingListener();
	private final TestPlan testPlan = TestPlan.from(true, List.of(), mock(), dummyOutputDirectoryCreator());

	@Test
	void emptyReport() {
		listener.testPlanExecutionStarted(testPlan);
		listener.testPlanExecutionFinished(testPlan);

		assertEquals(0, listener.getSummary().getTestsFailedCount());

		var summaryString = summaryAsString();
		assertAll("summary", //
			() -> assertTrue(summaryString.contains("Test run finished after"), "test run"), //

			() -> assertTrue(summaryString.contains("0 containers found"), "containers found"), //
			() -> assertTrue(summaryString.contains("0 containers skipped"), "containers skipped"), //
			() -> assertTrue(summaryString.contains("0 containers started"), "containers started"), //
			() -> assertTrue(summaryString.contains("0 containers aborted"), "containers aborted"), //
			() -> assertTrue(summaryString.contains("0 containers successful"), "containers successful"), //
			() -> assertTrue(summaryString.contains("0 containers failed"), "containers failed"), //

			() -> assertTrue(summaryString.contains("0 tests found"), "tests found"), //
			() -> assertTrue(summaryString.contains("0 tests skipped"), "tests skipped"), //
			() -> assertTrue(summaryString.contains("0 tests started"), "tests started"), //
			() -> assertTrue(summaryString.contains("0 tests aborted"), "tests aborted"), //
			() -> assertTrue(summaryString.contains("0 tests successful"), "tests successful"), //
			() -> assertTrue(summaryString.contains("0 tests failed"), "tests failed") //
		);

		assertEquals("", failuresAsString());
	}

	@Test
	void reportingCorrectCounts() {
		var successfulContainer = createContainerIdentifier("c1");
		var failedContainer = createContainerIdentifier("c2");
		var abortedContainer = createContainerIdentifier("c3");
		var skippedContainer = createContainerIdentifier("c4");

		var successfulTest = createTestIdentifier("t1");
		var failedTest = createTestIdentifier("t2");
		var abortedTest = createTestIdentifier("t3");
		var skippedTest = createTestIdentifier("t4");

		listener.testPlanExecutionStarted(testPlan);

		listener.executionSkipped(skippedContainer, "skipped");
		listener.executionSkipped(skippedTest, "skipped");

		listener.executionStarted(successfulContainer);
		listener.executionFinished(successfulContainer, TestExecutionResult.successful());

		listener.executionStarted(successfulTest);
		listener.executionFinished(successfulTest, TestExecutionResult.successful());

		listener.executionStarted(failedContainer);
		listener.executionFinished(failedContainer, TestExecutionResult.failed(new RuntimeException("failed")));

		listener.executionStarted(failedTest);
		listener.executionFinished(failedTest, TestExecutionResult.failed(new RuntimeException("failed")));

		listener.executionStarted(abortedContainer);
		listener.executionFinished(abortedContainer, TestExecutionResult.aborted(new RuntimeException("aborted")));

		listener.executionStarted(abortedTest);
		listener.executionFinished(abortedTest, TestExecutionResult.aborted(new RuntimeException("aborted")));

		listener.testPlanExecutionFinished(testPlan);

		var summaryString = summaryAsString();
		try {
			assertAll("summary", //
				() -> assertTrue(summaryString.contains("4 containers found"), "containers found"), //
				() -> assertTrue(summaryString.contains("1 containers skipped"), "containers skipped"), //
				() -> assertTrue(summaryString.contains("3 containers started"), "containers started"), //
				() -> assertTrue(summaryString.contains("1 containers aborted"), "containers aborted"), //
				() -> assertTrue(summaryString.contains("1 containers successful"), "containers successful"), //
				() -> assertTrue(summaryString.contains("1 containers failed"), "containers failed"), //

				() -> assertTrue(summaryString.contains("4 tests found"), "tests found"), //
				() -> assertTrue(summaryString.contains("1 tests skipped"), "tests skipped"), //
				() -> assertTrue(summaryString.contains("3 tests started"), "tests started"), //
				() -> assertTrue(summaryString.contains("1 tests aborted"), "tests aborted"), //
				() -> assertTrue(summaryString.contains("1 tests successful"), "tests successful"), //
				() -> assertTrue(summaryString.contains("1 tests failed"), "tests failed") //
			);
		}
		catch (AssertionError error) {
			System.err.println(summaryString);
			throw error;
		}
	}

	@Test
	void canGetListOfFailures() {
		var failedException = new RuntimeException("Pow!");
		var testDescriptor = new TestDescriptorStub(UniqueId.root("root", "1"), "failingTest") {

			@Override
			public Optional<TestSource> getSource() {
				return Optional.of(ClassSource.from(Object.class));
			}
		};
		var failingTest = TestIdentifier.from(testDescriptor);
		listener.testPlanExecutionStarted(testPlan);
		listener.executionStarted(failingTest);
		listener.executionFinished(failingTest, TestExecutionResult.failed(failedException));
		listener.testPlanExecutionFinished(testPlan);
		final var failures = listener.getSummary().getFailures();
		assertThat(failures).hasSize(1);
		assertThat(failures.getFirst().getException()).isEqualTo(failedException);
		assertThat(failures.getFirst().getTestIdentifier()).isEqualTo(failingTest);
	}

	@Test
	void reportingCorrectFailures() {
		var iaeCausedBy = new IllegalArgumentException("Illegal Argument Exception");
		var failedException = new RuntimeException("Runtime Exception", iaeCausedBy);
		var npeSuppressed = new NullPointerException("Null Pointer Exception");
		failedException.addSuppressed(npeSuppressed);

		var testDescriptor = new TestDescriptorStub(UniqueId.root("root", "2"), "failingTest") {

			@Override
			public Optional<TestSource> getSource() {
				return Optional.of(ClassSource.from(Object.class));
			}
		};
		var failed = TestIdentifier.from(testDescriptor);
		var aborted = TestIdentifier.from(new TestDescriptorStub(UniqueId.root("root", "3"), "abortedTest"));

		listener.testPlanExecutionStarted(testPlan);
		listener.executionStarted(failed);
		listener.executionFinished(failed, TestExecutionResult.failed(failedException));
		listener.executionStarted(aborted);
		listener.executionFinished(aborted, TestExecutionResult.aborted(new RuntimeException("aborted")));
		listener.testPlanExecutionFinished(testPlan);

		// An aborted test is not a failure
		assertEquals(1, listener.getSummary().getTestsFailedCount());

		var failuresString = failuresAsString();
		assertAll("failures", //
			() -> assertTrue(failuresString.contains("Failures (1)"), "test failures"), //
			() -> assertTrue(failuresString.contains(Object.class.getName()), "source"), //
			() -> assertTrue(failuresString.contains("failingTest"), "display name"), //
			() -> assertTrue(failuresString.contains("=> " + failedException), "main exception"), //
			() -> assertTrue(failuresString.contains("Caused by: " + iaeCausedBy), "Caused by exception"), //
			() -> assertTrue(failuresString.contains("Suppressed: " + npeSuppressed), "Suppressed exception") //
		);
	}

	@Test
	public void reportingCircularFailure() {
		var iaeCausedBy = new IllegalArgumentException("Illegal Argument Exception");
		var failedException = new RuntimeException("Runtime Exception", iaeCausedBy);
		var npeSuppressed = new NullPointerException("Null Pointer Exception");
		failedException.addSuppressed(npeSuppressed);
		npeSuppressed.addSuppressed(iaeCausedBy);

		var testDescriptor = new TestDescriptorStub(UniqueId.root("root", "2"), "failingTest") {

			@Override
			public Optional<TestSource> getSource() {
				return Optional.of(ClassSource.from(Object.class));
			}
		};
		var failed = TestIdentifier.from(testDescriptor);

		listener.testPlanExecutionStarted(testPlan);
		listener.executionStarted(failed);
		listener.executionFinished(failed, TestExecutionResult.failed(failedException));
		listener.testPlanExecutionFinished(testPlan);

		assertEquals(1, listener.getSummary().getTestsFailedCount());

		var failuresString = failuresAsString();
		assertAll("failures", //
			() -> assertTrue(failuresString.contains("Suppressed: " + npeSuppressed), "Suppressed exception"), //
			() -> assertTrue(failuresString.contains("Circular reference: " + iaeCausedBy), "Circular reference"), //
			() -> assertFalse(failuresString.contains("Caused by: "),
				"'Caused by: ' omitted because of Circular reference") //
		);
	}

	@RepeatedTest(10)
	void reportingConcurrentlyFinishedTests() throws Exception {
		var numThreads = 250;
		var testIdentifier = TestIdentifier.from(new TestDescriptorStub(UniqueId.root("root", "2"), "failingTest") {
			@Override
			public Optional<TestSource> getSource() {
				return Optional.of(ClassSource.from(Object.class));
			}
		});
		var result = TestExecutionResult.failed(new RuntimeException());

		listener.testPlanExecutionStarted(testPlan);
		executeConcurrently(numThreads, () -> {
			listener.executionStarted(testIdentifier);
			listener.executionFinished(testIdentifier, result);
		});
		listener.testPlanExecutionFinished(testPlan);

		assertThat(listener.getSummary().getFailures()).hasSize(numThreads);
	}

	private TestIdentifier createTestIdentifier(String uniqueId) {
		var identifier = TestIdentifier.from(new TestDescriptorStub(UniqueId.root("test", uniqueId), uniqueId));
		testPlan.addInternal(identifier);
		return identifier;
	}

	private TestIdentifier createContainerIdentifier(String uniqueId) {
		var identifier = TestIdentifier.from(new TestDescriptorStub(UniqueId.root("container", uniqueId), uniqueId) {

			@Override
			public Type getType() {
				return Type.CONTAINER;
			}
		});
		testPlan.addInternal(identifier);
		return identifier;
	}

	private String summaryAsString() {
		var summaryWriter = new StringWriter();
		listener.getSummary().printTo(new PrintWriter(summaryWriter));
		return summaryWriter.toString();
	}

	private String failuresAsString() {
		var failuresWriter = new StringWriter();
		listener.getSummary().printFailuresTo(new PrintWriter(failuresWriter));
		return failuresWriter.toString();
	}

}

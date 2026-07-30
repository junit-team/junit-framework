/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.output;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.fakes.TestDescriptorStub;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.3.2
 */
class VerboseTreePrintingListenerTests {

	private static final String EOL = System.lineSeparator();
	private static final Pattern DURATION = Pattern.compile("duration: (\\d+) ms");
	private static final Pattern LABEL = Pattern.compile("(?:tags|uniqueId|parent|source|duration|status): ");
	private static final int NUM_THREADS = 4;
	private static final int TESTS_PER_THREAD = 50;

	@Test
	void executionSkipped() {
		var stringWriter = new StringWriter();
		listener(stringWriter).executionSkipped(newTestIdentifier(), "Test" + EOL + "disabled");
		var lines = lines(stringWriter);

		assertLinesMatch(List.of( //
			"+-- %c ool test", //
			"|      tags: []", //
			"|  uniqueId: [engine:demo-engine]", //
			"|    parent: []", //
			"|    reason: Test", //
			"|              disabled", //
			"|    status: [S] SKIPPED"), List.of(lines));
	}

	@Test
	void reportingEntryPublished() {
		var stringWriter = new StringWriter();
		listener(stringWriter).reportingEntryPublished(newTestIdentifier(), ReportEntry.from("foo", "bar"));
		var lines = lines(stringWriter);

		assertLinesMatch(List.of( //
			"\\|   reports: ReportEntry \\[timestamp = .+, foo = 'bar'\\]"), List.of(lines));
	}

	@Test
	void fileEntryPublished() {
		var stringWriter = new StringWriter();
		listener(stringWriter).fileEntryPublished(newTestIdentifier(),
			FileEntry.from(Path.of("test.txt"), "text/plain"));
		var lines = lines(stringWriter);

		assertLinesMatch(List.of( //
			"\\|   reports: FileEntry \\[timestamp = .+, path = test.txt, mediaType = 'text/plain'\\]"),
			List.of(lines));
	}

	@Test
	void executionFinishedWithFailure() {
		var stringWriter = new StringWriter();
		var listener = listener(stringWriter);
		var testIdentifier = newTestIdentifier();
		listener.executionStarted(testIdentifier);
		discardOutput(stringWriter);
		listener.executionFinished(testIdentifier, failed(new AssertionError("Boom!")));
		var lines = lines(stringWriter);

		assertLinesMatch(List.of( //
			"|    caught: java.lang.AssertionError: Boom!", //
			">> STACKTRACE >>", //
			"\\|  duration: \\d+ ms", //
			"|    status: [X] FAILED"), List.of(lines));
	}

	@Test
	void failureMessageWithFormatSpecifier() {
		var stringWriter = new StringWriter();
		var listener = listener(stringWriter);
		var testIdentifier = newTestIdentifier();
		listener.executionStarted(testIdentifier);
		discardOutput(stringWriter);
		listener.executionFinished(testIdentifier, failed(new AssertionError("%crash")));
		var lines = lines(stringWriter);

		assertLinesMatch(List.of( //
			"|    caught: java.lang.AssertionError: %crash", //
			">> STACKTRACE >>", //
			"\\|  duration: \\d+ ms", //
			"|    status: [X] FAILED"), List.of(lines));
	}

	@Test
	void indentationIsDerivedFromTheNumberOfAncestorsInTheTestPlan() {
		var engine = new TestDescriptorStub(UniqueId.forEngine("demo-engine"), "demo-engine");
		var container = new TestDescriptorStub(engine.getUniqueId().append("class", "DemoClass"), "DemoClass");
		var test = new TestDescriptorStub(container.getUniqueId().append("method", "demoTest()"), "demoTest()");
		engine.addChild(container);
		container.addChild(test);

		var stringWriter = new StringWriter();
		var listener = listener(stringWriter, engine);
		var engineIdentifier = TestIdentifier.from(engine);
		var containerIdentifier = TestIdentifier.from(container);
		var testIdentifier = TestIdentifier.from(test);
		listener.executionStarted(engineIdentifier);
		listener.executionStarted(containerIdentifier);
		listener.executionStarted(testIdentifier);
		listener.executionFinished(testIdentifier, successful());
		listener.executionFinished(containerIdentifier, successful());
		listener.executionFinished(engineIdentifier, successful());
		var lines = lines(stringWriter);

		assertLinesMatch(List.of( //
			"+-- demo-engine", //
			"| +-- DemoClass", //
			"| | +-- demoTest()", //
			"| | |      tags: []", //
			"| | |  uniqueId: [engine:demo-engine]/[class:DemoClass]/[method:demoTest()]", //
			"| | |    parent: [engine:demo-engine]/[class:DemoClass]", //
			"\\| \\| \\|  duration: \\d+ ms", //
			"| | |    status: [OK] SUCCESSFUL", //
			"\\| '-- DemoClass finished after \\d+ ms\\.", //
			"'-- demo-engine finished after \\d+ ms\\."), List.of(lines));
	}

	@Test
	void indentationIsNotAffectedByOverlappingExecutions() {
		var engine = new TestDescriptorStub(UniqueId.forEngine("demo-engine"), "demo-engine");
		var first = new TestDescriptorStub(engine.getUniqueId().append("class", "FirstClass"), "FirstClass");
		var second = new TestDescriptorStub(engine.getUniqueId().append("class", "SecondClass"), "SecondClass");
		engine.addChild(first);
		engine.addChild(second);
		first.addChild(new TestDescriptorStub(first.getUniqueId().append("method", "firstTest()"), "firstTest()"));
		second.addChild(new TestDescriptorStub(second.getUniqueId().append("method", "secondTest()"), "secondTest()"));

		var stringWriter = new StringWriter();
		var listener = listener(stringWriter, engine);

		listener.executionStarted(TestIdentifier.from(engine));
		listener.executionStarted(TestIdentifier.from(first));
		listener.executionStarted(TestIdentifier.from(second));
		var lines = lines(stringWriter);

		assertLinesMatch(List.of( //
			"+-- demo-engine", //
			"| +-- FirstClass", //
			"| +-- SecondClass"), List.of(lines));
	}

	@Test
	void reportsItsOwnDurationWhenExecutionsOverlap() throws Exception {
		var engine = new TestDescriptorStub(UniqueId.forEngine("demo-engine"), "demo-engine");
		var slow = new TestDescriptorStub(engine.getUniqueId().append("method", "slow()"), "slow()");
		var fast = new TestDescriptorStub(engine.getUniqueId().append("method", "fast()"), "fast()");
		engine.addChild(slow);
		engine.addChild(fast);

		var stringWriter = new StringWriter();
		var listener = listener(stringWriter, engine);
		var slowIdentifier = TestIdentifier.from(slow);
		var fastIdentifier = TestIdentifier.from(fast);

		listener.executionStarted(slowIdentifier);
		Thread.sleep(100);
		listener.executionStarted(fastIdentifier);
		listener.executionFinished(fastIdentifier, successful());
		listener.executionFinished(slowIdentifier, successful());

		assertThat(reportedDurations(stringWriter)).hasSize(2).anyMatch(duration -> duration >= 100);
	}

	@Test
	void linesAreNotInterleavedWhenExecutionsArePrintedConcurrently() throws Exception {
		var engine = new TestDescriptorStub(UniqueId.forEngine("demo-engine"), "demo-engine");
		var identifiers = new ArrayList<TestIdentifier>();
		for (int i = 0; i < TESTS_PER_THREAD * NUM_THREADS; i++) {
			var test = new TestDescriptorStub(engine.getUniqueId().append("method", "test" + i + "()"),
				"test" + i + "()");
			engine.addChild(test);
			identifiers.add(TestIdentifier.from(test));
		}

		var stringWriter = new StringWriter();
		var listener = listener(stringWriter, engine);
		var barrier = new CyclicBarrier(NUM_THREADS);
		var executor = Executors.newFixedThreadPool(NUM_THREADS);
		try {
			for (int thread = 0; thread < NUM_THREADS; thread++) {
				int offset = thread * TESTS_PER_THREAD;
				executor.submit(() -> {
					await(barrier);
					for (int i = offset; i < offset + TESTS_PER_THREAD; i++) {
						var testIdentifier = identifiers.get(i);
						listener.executionStarted(testIdentifier);
						listener.executionFinished(testIdentifier, successful());
					}
				});
			}
		}
		finally {
			executor.shutdown();
			assertTrue(executor.awaitTermination(30, SECONDS), "Executor was not terminated");
		}

		assertThat(stringWriter.toString().lines()) //
				.allSatisfy(line -> assertThat(labelsIn(line)).describedAs("labels in <%s>", line).isLessThan(2)) //
				.filteredOn(line -> line.contains("uniqueId: ")) //
				.hasSize(TESTS_PER_THREAD * NUM_THREADS);
	}

	private VerboseTreePrintingListener listener(StringWriter stringWriter) {
		return listener(stringWriter, TEST_DESCRIPTOR);
	}

	private VerboseTreePrintingListener listener(StringWriter stringWriter, TestDescriptor engineDescriptor) {
		var listener = new VerboseTreePrintingListener(new PrintWriter(stringWriter), ColorPalette.NONE, 16,
			Theme.ASCII);
		listener.testPlanExecutionStarted(
			TestPlan.from(true, Set.of(engineDescriptor), mock(), dummyOutputDirectoryCreator()));
		discardOutput(stringWriter);
		return listener;
	}

	private static final TestDescriptor TEST_DESCRIPTOR = new TestDescriptorStub(UniqueId.forEngine("demo-engine"),
		"%c ool test");

	private static TestIdentifier newTestIdentifier() {
		return TestIdentifier.from(TEST_DESCRIPTOR);
	}

	private static void discardOutput(StringWriter stringWriter) {
		stringWriter.getBuffer().setLength(0);
	}

	private String[] lines(StringWriter stringWriter) {
		return stringWriter.toString().split(EOL);
	}

	private static List<Long> reportedDurations(StringWriter stringWriter) {
		Matcher matcher = DURATION.matcher(stringWriter.toString());
		return matcher.results().map(result -> Long.valueOf(result.group(1))).toList();
	}

	private static long labelsIn(String line) {
		return LABEL.matcher(line).results().count();
	}

	private static void await(CyclicBarrier barrier) {
		try {
			barrier.await();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}

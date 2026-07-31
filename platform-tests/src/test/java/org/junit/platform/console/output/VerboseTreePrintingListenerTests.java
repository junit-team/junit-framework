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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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

	private final Clock clock = mock();
	private final StringWriter stringWriter = new StringWriter();
	private final VerboseTreePrintingListener listener = new VerboseTreePrintingListener(new PrintWriter(stringWriter),
		ColorPalette.NONE, 16, Theme.ASCII, clock);

	@Test
	void executionSkipped() {
		var engine = new TestDescriptorStub(UniqueId.forEngine("demo-engine"), "%c ool test");
		listener.testPlanExecutionStarted(testPlan(engine));

		listener.executionSkipped(TestIdentifier.from(engine), "Test%ndisabled".formatted());

		assertOutput("""
				Test plan execution started. Number of static tests: 1
				.
				+-- %c ool test
				|      tags: []
				|  uniqueId: [engine:demo-engine]
				|    parent: []
				|    reason: Test
				|              disabled
				|    status: [S] SKIPPED
				""");
	}

	@Test
	void reportingEntryPublished() {
		var engine = new TestDescriptorStub(UniqueId.forEngine("demo-engine"), "%c ool test");
		listener.testPlanExecutionStarted(testPlan(engine));

		listener.reportingEntryPublished(TestIdentifier.from(engine), ReportEntry.from("foo", "bar"));

		assertOutput("""
				Test plan execution started. Number of static tests: 1
				.
				\\|   reports: ReportEntry \\[timestamp = .+, foo = 'bar'\\]
				""");
	}

	@Test
	void fileEntryPublished() {
		var engine = new TestDescriptorStub(UniqueId.forEngine("demo-engine"), "%c ool test");
		listener.testPlanExecutionStarted(testPlan(engine));

		listener.fileEntryPublished(TestIdentifier.from(engine), FileEntry.from(Path.of("test.txt"), "text/plain"));

		assertOutput("""
				Test plan execution started. Number of static tests: 1
				.
				\\|   reports: FileEntry \\[timestamp = .+, path = test.txt, mediaType = 'text/plain'\\]
				""");
	}

	@Test
	void executionFinishedWithFailure() {
		var engine = new TestDescriptorStub(UniqueId.forEngine("demo-engine"), "%c ool test");
		listener.testPlanExecutionStarted(testPlan(engine));
		var testIdentifier = TestIdentifier.from(engine);

		when(clock.instant()).thenReturn(Instant.EPOCH, Instant.EPOCH.plusMillis(42));

		listener.executionStarted(testIdentifier);
		listener.executionFinished(testIdentifier, failed(new AssertionError("Boom!")));

		assertOutput("""
				Test plan execution started. Number of static tests: 1
				.
				+-- %c ool test
				|      tags: []
				|  uniqueId: [engine:demo-engine]
				|    parent: []
				|    caught: java.lang.AssertionError: Boom!
				>> STACKTRACE >>
				|  duration: 42 ms
				|    status: [X] FAILED
				""");
	}

	@Test
	void failureMessageWithFormatSpecifier() {
		var engine = new TestDescriptorStub(UniqueId.forEngine("demo-engine"), "%c ool test");
		listener.testPlanExecutionStarted(testPlan(engine));
		var testIdentifier = TestIdentifier.from(engine);

		when(clock.instant()).thenReturn(Instant.EPOCH, Instant.EPOCH.plusMillis(42));

		listener.executionStarted(testIdentifier);
		listener.executionFinished(testIdentifier, failed(new AssertionError("%crash")));

		assertOutput("""
				Test plan execution started. Number of static tests: 1
				.
				+-- %c ool test
				|      tags: []
				|  uniqueId: [engine:demo-engine]
				|    parent: []
				|    caught: java.lang.AssertionError: %crash
				>> STACKTRACE >>
				|  duration: 42 ms
				|    status: [X] FAILED""");
	}

	@Test
	void indentationIsDerivedFromTheNumberOfAncestorsInTheTestPlan() {
		var engine = new TestDescriptorStub(UniqueId.forEngine("demo-engine"), "demo-engine");
		var container = new TestDescriptorStub(engine.getUniqueId().append("class", "DemoClass"), "DemoClass");
		var test = new TestDescriptorStub(container.getUniqueId().append("method", "demoTest()"), "demoTest()");
		engine.addChild(container);
		container.addChild(test);
		listener.testPlanExecutionStarted(testPlan(engine));
		var engineIdentifier = TestIdentifier.from(engine);
		var containerIdentifier = TestIdentifier.from(container);
		var testIdentifier = TestIdentifier.from(test);

		when(clock.instant()).thenReturn( //
			Instant.EPOCH, // engine started
			Instant.EPOCH, // container started
			Instant.EPOCH, // test started
			Instant.EPOCH.plusMillis(7), // test finished
			Instant.EPOCH.plusMillis(10), // container finished
			Instant.EPOCH.plusMillis(15)); // engine finished

		listener.executionStarted(engineIdentifier);
		listener.executionStarted(containerIdentifier);
		listener.executionStarted(testIdentifier);
		listener.executionFinished(testIdentifier, successful());
		listener.executionFinished(containerIdentifier, successful());
		listener.executionFinished(engineIdentifier, successful());

		assertOutput("""
				Test plan execution started. Number of static tests: 1
				.
				+-- demo-engine
				| +-- DemoClass
				| | +-- demoTest()
				| | |      tags: []
				| | |  uniqueId: [engine:demo-engine]/[class:DemoClass]/[method:demoTest()]
				| | |    parent: [engine:demo-engine]/[class:DemoClass]
				| | |  duration: 7 ms
				| | |    status: [OK] SUCCESSFUL
				| '-- DemoClass finished after 10 ms.
				'-- demo-engine finished after 15 ms.
				""");
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
		listener.testPlanExecutionStarted(testPlan(engine));

		when(clock.instant()).thenReturn(Instant.EPOCH);

		listener.executionStarted(TestIdentifier.from(engine));
		listener.executionStarted(TestIdentifier.from(first));
		listener.executionStarted(TestIdentifier.from(second));

		assertOutput("""
				Test plan execution started. Number of static tests: 2
				.
				+-- demo-engine
				| +-- FirstClass
				| +-- SecondClass
				""");
	}

	@Test
	void reportsItsOwnDurationWhenExecutionsOverlap() {
		var engine = new TestDescriptorStub(UniqueId.forEngine("demo-engine"), "demo-engine");
		var slow = new TestDescriptorStub(engine.getUniqueId().append("method", "slow()"), "slow()");
		var fast = new TestDescriptorStub(engine.getUniqueId().append("method", "fast()"), "fast()");
		engine.addChild(slow);
		engine.addChild(fast);
		listener.testPlanExecutionStarted(testPlan(engine));
		var slowIdentifier = TestIdentifier.from(slow);
		var fastIdentifier = TestIdentifier.from(fast);

		when(clock.instant()).thenReturn( //
			Instant.EPOCH, // slow started
			Instant.EPOCH.plusMillis(100), // fast started
			Instant.EPOCH.plusMillis(300), // fast finished -> 200 ms
			Instant.EPOCH.plusMillis(700)); // slow finished -> 700 ms

		listener.executionStarted(slowIdentifier);
		listener.executionStarted(fastIdentifier);
		listener.executionFinished(fastIdentifier, successful());
		listener.executionFinished(slowIdentifier, successful());

		assertOutput("""
				Test plan execution started. Number of static tests: 2
				.
				| +-- slow()
				| |      tags: []
				| |  uniqueId: [engine:demo-engine]/[method:slow()]
				| |    parent: [engine:demo-engine]
				| +-- fast()
				| |      tags: []
				| |  uniqueId: [engine:demo-engine]/[method:fast()]
				| |    parent: [engine:demo-engine]
				| |  duration: 200 ms
				| |    status: [OK] SUCCESSFUL
				| |  duration: 700 ms
				| |    status: [OK] SUCCESSFUL
				""");
	}

	@Test
	@Timeout(10)
	void linesAreNotInterleavedWhenExecutionsArePrintedConcurrently() throws Exception {
		final int threadCount = 4;
		final int testsPerThread = 50;

		var engine = new TestDescriptorStub(UniqueId.forEngine("demo-engine"), "demo-engine");
		var testDescriptors = IntStream.range(0, testsPerThread * threadCount) //
				.mapToObj(i -> new TestDescriptorStub(engine.getUniqueId().append("method", "test" + i + "()"),
					"test" + i + "()")) //
				.toList();
		testDescriptors.forEach(engine::addChild);
		var identifiers = testDescriptors.stream().map(TestIdentifier::from).toList();
		listener.testPlanExecutionStarted(testPlan(engine));

		when(clock.instant()).thenReturn(Instant.EPOCH);

		var barrier = new CyclicBarrier(threadCount);
		try (var executor = Executors.newFixedThreadPool(threadCount)) {
			var futures = IntStream.range(0, threadCount) //
					.mapToObj(thread -> executor.submit((Callable<Void>) () -> {
						barrier.await();
						identifiers.subList(thread * testsPerThread, (thread + 1) * testsPerThread) //
								.forEach(identifier -> {
									listener.executionStarted(identifier);
									listener.executionFinished(identifier, successful());
								});
						return null;
					})) //
					.toList();
			for (Future<?> future : futures) {
				future.get();
			}
		}

		var label = Pattern.compile("(?:tags|uniqueId|parent|source|duration|status): ");
		assertThat(stringWriter.toString().lines()) //
				.allSatisfy(line -> assertThat(label.matcher(line).results()) //
						.describedAs("labels in <%s>", line).hasSizeLessThan(2)) //
				.filteredOn(line -> line.contains("uniqueId: ")) //
				.hasSize(testsPerThread * threadCount);
	}

	private static TestPlan testPlan(TestDescriptor engineDescriptor) {
		return TestPlan.from(true, Set.of(engineDescriptor), mock(), dummyOutputDirectoryCreator());
	}

	private void assertOutput(String expectedOutput) {
		assertLinesMatch(expectedOutput.lines(), stringWriter.toString().lines());
	}

}

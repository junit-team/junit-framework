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
import org.junit.platform.fakes.EngineTestDescriptorStub;
import org.junit.platform.fakes.TestDescriptorStub;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.3.2
 */
class VerboseTreePrintingListenerTests {

	private final Clock clock = mock();
	private final StringWriter output = new StringWriter();
	private final VerboseTreePrintingListener listener = new VerboseTreePrintingListener( //
		new PrintWriter(output), //
		ColorPalette.NONE, 16, //
		Theme.ASCII, //
		clock //
	);
	private final TestDescriptor engine = new EngineTestDescriptorStub( //
		UniqueId.forEngine("demo-engine"), //
		"%c ool test" //
	);

	@Test
	void executionSkipped() {
		var testPlan = testPlan(engine);

		listener.testPlanExecutionStarted(testPlan);
		listener.executionSkipped(TestIdentifier.from(engine), "Test%ndisabled".formatted());
		listener.testPlanExecutionFinished(testPlan);

		assertOutput("""
				Test plan execution started. Number of static tests: 0
				.
				+-- %c ool test
				|      tags: []
				|  uniqueId: [engine:demo-engine]
				|    parent: []
				|    reason: Test
				|              disabled
				|    status: [S] SKIPPED
				Test plan execution finished. Number of all tests: 0
				""");
	}

	@Test
	void reportingEntryPublished() {
		var testPlan = testPlan(engine);

		when(clock.instant()).thenReturn(Instant.EPOCH, Instant.EPOCH.plusMillis(42));
		listener.testPlanExecutionStarted(testPlan);
		listener.executionStarted(TestIdentifier.from(engine));
		listener.reportingEntryPublished(TestIdentifier.from(engine), ReportEntry.from("foo", "bar"));
		listener.executionFinished(TestIdentifier.from(engine), successful());
		listener.testPlanExecutionFinished(testPlan);

		assertOutput("""
				Test plan execution started. Number of static tests: 0
				.
				+-- %c ool test
				\\|   reports: ReportEntry \\[timestamp = .+, foo = 'bar']
				'-- %c ool test finished after 42 ms.
				Test plan execution finished. Number of all tests: 0
				""");
	}

	@Test
	void fileEntryPublished() {
		var testPlan = testPlan(engine);

		when(clock.instant()).thenReturn(Instant.EPOCH, Instant.EPOCH.plusMillis(42));
		listener.testPlanExecutionStarted(testPlan);
		listener.executionStarted(TestIdentifier.from(engine));
		listener.fileEntryPublished(TestIdentifier.from(engine), FileEntry.from(Path.of("test.txt"), "text/plain"));
		listener.executionFinished(TestIdentifier.from(engine), successful());
		listener.testPlanExecutionFinished(testPlan);

		assertOutput("""
				Test plan execution started. Number of static tests: 0
				.
				+-- %c ool test
				\\|   reports: FileEntry \\[timestamp = .+, path = test\\.txt, mediaType = 'text/plain']
				'-- %c ool test finished after 42 ms.
				Test plan execution finished. Number of all tests: 0
				""");
	}

	@Test
	void executionFinishedWithFailure() {
		var testPlan = testPlan(engine);

		when(clock.instant()).thenReturn(Instant.EPOCH, Instant.EPOCH.plusMillis(42));
		listener.testPlanExecutionStarted(testPlan);
		listener.executionStarted(TestIdentifier.from(engine));
		listener.executionFinished(TestIdentifier.from(engine), failed(new AssertionError("Boom!")));
		listener.testPlanExecutionFinished(testPlan);

		assertOutput("""
				Test plan execution started. Number of static tests: 0
				.
				+-- %c ool test
				|    caught: java.lang.AssertionError: Boom!
				>> STACKTRACE >>
				'-- %c ool test finished after 42 ms.
				Test plan execution finished. Number of all tests: 0
				""");
	}

	@Test
	void failureMessageWithFormatSpecifier() {
		TestPlan testPlan = testPlan(engine);

		when(clock.instant()).thenReturn(Instant.EPOCH, Instant.EPOCH.plusMillis(42));
		listener.testPlanExecutionStarted(testPlan);
		listener.executionStarted(TestIdentifier.from(engine));
		listener.executionFinished(TestIdentifier.from(engine), failed(new AssertionError("%crash")));
		listener.testPlanExecutionFinished(testPlan);

		assertOutput("""
				Test plan execution started. Number of static tests: 0
				.
				+-- %c ool test
				|    caught: java.lang.AssertionError: %crash
				>> STACKTRACE >>
				'-- %c ool test finished after 42 ms.
				Test plan execution finished. Number of all tests: 0
				""");
	}

	@Test
	void dynamicTestRegistered() {
		var container = new TestDescriptorStub(engine.getUniqueId().append("class", "DemoClass"), "DemoClass");
		var test = new TestDescriptorStub(container.getUniqueId().append("method", "demoTest()"), "demoTest()");
		engine.addChild(container);
		var testPlan = testPlan(engine);

		when(clock.instant()).thenReturn( //
			Instant.EPOCH, // engine started
			Instant.EPOCH, // container started
			Instant.EPOCH, // test started
			Instant.EPOCH.plusMillis(7), // test finished
			Instant.EPOCH.plusMillis(10), // container finished
			Instant.EPOCH.plusMillis(15)); // engine finished
		listener.testPlanExecutionStarted(testPlan);
		listener.executionStarted(TestIdentifier.from(engine));
		listener.executionStarted(TestIdentifier.from(container));
		container.addChild(test);
		listener.dynamicTestRegistered(TestIdentifier.from(test));
		listener.executionStarted(TestIdentifier.from(test));
		listener.executionFinished(TestIdentifier.from(test), successful());
		listener.executionFinished(TestIdentifier.from(container), successful());
		listener.executionFinished(TestIdentifier.from(engine), successful());
		listener.testPlanExecutionFinished(testPlan);

		assertOutput("""
				Test plan execution started. Number of static tests: 1
				.
				+-- %c ool test
				| +-- DemoClass
				| |      tags: []
				| |  uniqueId: [engine:demo-engine]/[class:DemoClass]
				| |    parent: [engine:demo-engine]
				| | +-- demoTest() dynamically registered
				| | +-- demoTest()
				| | |      tags: []
				| | |  uniqueId: [engine:demo-engine]/[class:DemoClass]/[method:demoTest()]
				| | |    parent: [engine:demo-engine]/[class:DemoClass]
				| | |  duration: 7 ms
				| | |    status: [OK] SUCCESSFUL
				| '-- DemoClass finished after 10 ms.
				'-- %c ool test finished after 15 ms.
				Test plan execution finished. Number of all tests: 1
				""");
	}

	@Test
	void indentationIsDerivedFromTheNumberOfAncestorsInTheTestPlan() {
		var container = new TestDescriptorStub(engine.getUniqueId().append("class", "DemoClass"), "DemoClass");
		var test = new TestDescriptorStub(container.getUniqueId().append("method", "demoTest()"), "demoTest()");
		engine.addChild(container);
		container.addChild(test);
		var testPlan = testPlan(engine);

		when(clock.instant()).thenReturn( //
			Instant.EPOCH, // engine started
			Instant.EPOCH, // container started
			Instant.EPOCH, // test started
			Instant.EPOCH.plusMillis(7), // test finished
			Instant.EPOCH.plusMillis(10), // container finished
			Instant.EPOCH.plusMillis(15)); // engine finished
		listener.testPlanExecutionStarted(testPlan);
		listener.executionStarted(TestIdentifier.from(engine));
		listener.executionStarted(TestIdentifier.from(container));
		listener.executionStarted(TestIdentifier.from(test));
		listener.executionFinished(TestIdentifier.from(test), successful());
		listener.executionFinished(TestIdentifier.from(container), successful());
		listener.executionFinished(TestIdentifier.from(engine), successful());
		listener.testPlanExecutionFinished(testPlan);

		assertOutput("""
				Test plan execution started. Number of static tests: 1
				.
				+-- %c ool test
				| +-- DemoClass
				| | +-- demoTest()
				| | |      tags: []
				| | |  uniqueId: [engine:demo-engine]/[class:DemoClass]/[method:demoTest()]
				| | |    parent: [engine:demo-engine]/[class:DemoClass]
				| | |  duration: 7 ms
				| | |    status: [OK] SUCCESSFUL
				| '-- DemoClass finished after 10 ms.
				'-- %c ool test finished after 15 ms.
				Test plan execution finished. Number of all tests: 1
				""");
	}

	@Test
	void indentationIsNotAffectedByOverlappingExecutions() {
		var first = new TestDescriptorStub(engine.getUniqueId().append("class", "FirstClass"), "FirstClass");
		var second = new TestDescriptorStub(engine.getUniqueId().append("class", "SecondClass"), "SecondClass");
		engine.addChild(first);
		engine.addChild(second);
		first.addChild(new TestDescriptorStub(first.getUniqueId().append("method", "firstTest()"), "firstTest()"));
		second.addChild(new TestDescriptorStub(second.getUniqueId().append("method", "secondTest()"), "secondTest()"));
		var testPlan = testPlan(engine);

		when(clock.instant()).thenReturn( //
			Instant.EPOCH, // engine started
			Instant.EPOCH, // container started
			Instant.EPOCH, // test started
			Instant.EPOCH.plusMillis(7), // first test finished
			Instant.EPOCH.plusMillis(10), // second test finished
			Instant.EPOCH.plusMillis(15)); // engine finished
		listener.testPlanExecutionStarted(testPlan);
		listener.executionStarted(TestIdentifier.from(engine));
		listener.executionStarted(TestIdentifier.from(first));
		listener.executionStarted(TestIdentifier.from(second));
		listener.executionFinished(TestIdentifier.from(first), successful());
		listener.executionFinished(TestIdentifier.from(second), successful());
		listener.executionFinished(TestIdentifier.from(engine), successful());
		listener.testPlanExecutionFinished(testPlan);

		assertOutput("""
				Test plan execution started. Number of static tests: 2
				.
				+-- %c ool test
				| +-- FirstClass
				| +-- SecondClass
				| '-- FirstClass finished after 7 ms.
				| '-- SecondClass finished after 10 ms.
				'-- %c ool test finished after 15 ms.
				Test plan execution finished. Number of all tests: 2
				""");
	}

	@Test
	void reportsItsOwnDurationWhenExecutionsOverlap() {
		var slow = new TestDescriptorStub(engine.getUniqueId().append("method", "slow()"), "slow()");
		var fast = new TestDescriptorStub(engine.getUniqueId().append("method", "fast()"), "fast()");
		engine.addChild(slow);
		engine.addChild(fast);
		var testPlan = testPlan(engine);

		when(clock.instant()).thenReturn( //
			Instant.EPOCH, // engine started
			Instant.EPOCH, // slow started
			Instant.EPOCH.plusMillis(100), // fast started
			Instant.EPOCH.plusMillis(300), // fast finished -> 200 ms
			Instant.EPOCH.plusMillis(700)); // slow finished -> 700 ms
		listener.testPlanExecutionStarted(testPlan);
		listener.executionStarted(TestIdentifier.from(engine));
		listener.executionStarted(TestIdentifier.from(slow));
		listener.executionStarted(TestIdentifier.from(fast));
		listener.executionFinished(TestIdentifier.from(fast), successful());
		listener.executionFinished(TestIdentifier.from(slow), successful());
		listener.executionFinished(TestIdentifier.from(engine), successful());
		listener.testPlanExecutionFinished(testPlan);

		assertOutput("""
				Test plan execution started. Number of static tests: 2
				.
				+-- %c ool test
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
				'-- %c ool test finished after 700 ms.
				Test plan execution finished. Number of all tests: 2
				""");
	}

	@Test
	@Timeout(10)
	void linesAreNotInterleavedWhenExecutionsArePrintedConcurrently() throws Exception {
		final int threadCount = 4;
		final int testsPerThread = 50;

		var testDescriptors = IntStream.range(0, testsPerThread * threadCount) //
				.mapToObj(i -> new TestDescriptorStub(engine.getUniqueId().append("method", "test" + i + "()"),
					"test" + i + "()")) //
				.toList();
		testDescriptors.forEach(engine::addChild);
		var identifiers = testDescriptors.stream().map(TestIdentifier::from).toList();
		var testPlan = testPlan(engine);

		when(clock.instant()).thenReturn(Instant.EPOCH);
		listener.testPlanExecutionStarted(testPlan);
		listener.executionStarted(TestIdentifier.from(engine));

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

		listener.executionFinished(TestIdentifier.from(engine), successful());
		listener.testPlanExecutionFinished(testPlan);

		var label = Pattern.compile("(?:tags|uniqueId|parent|source|duration|status): ");
		assertThat(output.toString().lines()) //
				.allSatisfy(line -> assertThat(label.matcher(line).results()) //
						.describedAs("labels in <%s>", line).hasSizeLessThan(2)) //
				.filteredOn(line -> line.contains("uniqueId: ")) //
				.hasSize(testsPerThread * threadCount);
	}

	@Test
	void listTests() {
		var container = new TestDescriptorStub(engine.getUniqueId().append("class", "DemoClass"), "DemoClass");
		var test = new TestDescriptorStub(container.getUniqueId().append("method", "demoTest()"), "demoTest()");
		engine.addChild(container);
		container.addChild(test);
		var testPlan = testPlan(engine);

		listener.listTests(testPlan);

		assertOutput("""
				+-- %c ool test
				| +-- DemoClass
				| | +-- demoTest()
				| | |      tags: []
				| | |  uniqueId: [engine:demo-engine]/[class:DemoClass]/[method:demoTest()]
				| | |    parent: [engine:demo-engine]/[class:DemoClass]
				| '-- DemoClass
				'-- %c ool test
				""");
	}

	@Test
	void listTestsForEngineWithoutTest() {
		var testPlan = testPlan(engine);

		listener.listTests(testPlan);

		assertOutput("""
				+-- %c ool test
				|      tags: []
				|  uniqueId: [engine:demo-engine]
				|    parent: []
				""");
	}

	private static TestPlan testPlan(TestDescriptor engineDescriptor) {
		return TestPlan.from(true, Set.of(engineDescriptor), mock(), dummyOutputDirectoryCreator());
	}

	private void assertOutput(String expectedOutput) {
		assertLinesMatch(expectedOutput.lines(), output.toString().lines());
	}

}

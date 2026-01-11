/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor.DYNAMIC_CONTAINER_SEGMENT_TYPE;
import static org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor.DYNAMIC_TEST_SEGMENT_TYPE;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectIteration;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.displayName;
import static org.junit.platform.testkit.engine.EventConditions.dynamicTestRegistered;
import static org.junit.platform.testkit.engine.EventConditions.engine;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.started;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.Event;
import org.junit.platform.testkit.engine.Events;

/**
 * Integration tests for {@link TestFactory @TestFactory}, {@link DynamicTest},
 * and {@link org.junit.jupiter.api.DynamicContainer}.
 *
 * @since 5.0
 */
class DynamicNodeGenerationTests extends AbstractJupiterTestEngineTests {

	@Test
	void testFactoryMethodsAreCorrectlyDiscoveredForClassSelector() {
		LauncherDiscoveryRequest request = request().selectors(selectClass(MyDynamicTestCase.class)).build();
		TestDescriptor engineDescriptor = discoverTests(request).getEngineDescriptor();
		assertThat(engineDescriptor.getDescendants()).as("# resolved test descriptors").hasSize(13);
	}

	@Test
	void testFactoryMethodIsCorrectlyDiscoveredForMethodSelector() {
		LauncherDiscoveryRequest request = request().selectors(
			selectMethod(MyDynamicTestCase.class, "dynamicStream")).build();
		TestDescriptor engineDescriptor = discoverTests(request).getEngineDescriptor();
		assertThat(engineDescriptor.getDescendants()).as("# resolved test descriptors").hasSize(2);
	}

	@Test
	void dynamicTestsAreExecutedFromStream() {
		EngineExecutionResults executionResults = executeTests(selectMethod(MyDynamicTestCase.class, "dynamicStream"));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(MyDynamicTestCase.class), started()), //
			event(container("dynamicStream"), started()), //
			event(dynamicTestRegistered("dynamic-test:#1")), //
			event(test("dynamic-test:#1", "succeedingTest"), started()), //
			event(test("dynamic-test:#1", "succeedingTest"), finishedSuccessfully()), //
			event(dynamicTestRegistered("dynamic-test:#2")), //
			event(test("dynamic-test:#2", "failingTest"), started()), //
			event(test("dynamic-test:#2", "failingTest"), finishedWithFailure(message("failing"))), //
			event(container("dynamicStream"), finishedSuccessfully()), //
			event(container(MyDynamicTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void dynamicTestsAreExecutedFromCollection() {
		EngineExecutionResults executionResults = executeTests(
			selectMethod(MyDynamicTestCase.class, "dynamicCollection"));
		Events containers = executionResults.containerEvents();
		Events tests = executionResults.testEvents();

		assertAll( //
			() -> assertEquals(3, containers.started().count(), "# container started"),
			() -> assertEquals(2, tests.dynamicallyRegistered().count(), "# dynamic registered"),
			() -> assertEquals(2, tests.started().count(), "# tests started"),
			() -> assertEquals(1, tests.succeeded().count(), "# tests succeeded"),
			() -> assertEquals(1, tests.failed().count(), "# tests failed"),
			() -> assertEquals(3, containers.finished().count(), "# container finished"));
	}

	@Test
	void dynamicTestsAreExecutedFromIterator() {
		EngineExecutionResults executionResults = executeTests(
			selectMethod(MyDynamicTestCase.class, "dynamicIterator"));
		Events containers = executionResults.containerEvents();
		Events tests = executionResults.testEvents();

		assertAll( //
			() -> assertEquals(3, containers.started().count(), "# container started"),
			() -> assertEquals(2, tests.dynamicallyRegistered().count(), "# dynamic registered"),
			() -> assertEquals(2, tests.started().count(), "# tests started"),
			() -> assertEquals(1, tests.succeeded().count(), "# tests succeeded"),
			() -> assertEquals(1, tests.failed().count(), "# tests failed"),
			() -> assertEquals(3, containers.finished().count(), "# container finished"));
	}

	@Test
	void dynamicTestsAreExecutedFromIterable() {
		EngineExecutionResults executionResults = executeTests(
			selectMethod(MyDynamicTestCase.class, "dynamicIterable"));
		Events containers = executionResults.containerEvents();
		Events tests = executionResults.testEvents();

		// @TestFactory methods are counted as both container and test
		assertAll( //
			() -> assertEquals(3, containers.started().count(), "# container started"),
			() -> assertEquals(2, tests.dynamicallyRegistered().count(), "# dynamic registered"),
			() -> assertEquals(2, tests.started().count(), "# tests started"),
			() -> assertEquals(1, tests.succeeded().count(), "# tests succeeded"),
			() -> assertEquals(1, tests.failed().count(), "# tests failed"),
			() -> assertEquals(3, containers.finished().count(), "# container finished"));
	}

	@Test
	void singleDynamicTestIsExecutedWhenDiscoveredByUniqueId() {
		UniqueId uniqueId = discoverUniqueId(MyDynamicTestCase.class, "dynamicStream") //
				.append(DYNAMIC_TEST_SEGMENT_TYPE, "#2");

		EngineExecutionResults executionResults = executeTests(selectUniqueId(uniqueId));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(MyDynamicTestCase.class), started()), //
			event(container("dynamicStream"), started()), //
			event(dynamicTestRegistered("dynamic-test:#2")), //
			event(test("dynamic-test:#2", "failingTest"), started()), //
			event(test("dynamic-test:#2", "failingTest"), finishedWithFailure(message("failing"))), //
			event(container("dynamicStream"), finishedSuccessfully()), //
			event(container(MyDynamicTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void singleDynamicTestIsExecutedWhenDiscoveredByIterationIndex() {
		var methodSelector = selectMethod(MyDynamicTestCase.class, "dynamicStream");

		EngineExecutionResults executionResults = executeTests(selectIteration(methodSelector, 1));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(MyDynamicTestCase.class), started()), //
			event(container("dynamicStream"), started()), //
			event(dynamicTestRegistered("dynamic-test:#2")), //
			event(test("dynamic-test:#2", "failingTest"), started()), //
			event(test("dynamic-test:#2", "failingTest"), finishedWithFailure(message("failing"))), //
			event(container("dynamicStream"), finishedSuccessfully()), //
			event(container(MyDynamicTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void multipleDynamicTestsAreExecutedWhenDiscoveredByIterationIndexAndUniqueId() {
		UniqueId uniqueId = discoverUniqueId(MyDynamicTestCase.class, "threeTests") //
				.append(DYNAMIC_TEST_SEGMENT_TYPE, "#3");

		var methodSelector = selectMethod(MyDynamicTestCase.class, "threeTests");

		EngineExecutionResults executionResults = executeTests(selectIteration(methodSelector, 1),
			selectUniqueId(uniqueId));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(MyDynamicTestCase.class), started()), //
			event(container("threeTests"), started()), //
			event(dynamicTestRegistered("dynamic-test:#2")), //
			event(test("dynamic-test:#2", "two"), started()), //
			event(test("dynamic-test:#2", "two"), finishedSuccessfully()), //
			event(dynamicTestRegistered("dynamic-test:#3")), //
			event(test("dynamic-test:#3", "three"), started()), //
			event(test("dynamic-test:#3", "three"), finishedSuccessfully()), //
			event(container("threeTests"), finishedSuccessfully()), //
			event(container(MyDynamicTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void dynamicContainersAreExecutedFromIterable() {
		EngineExecutionResults executionResults = executeTests(
			selectMethod(MyDynamicTestCase.class, "dynamicContainerWithIterable"));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(MyDynamicTestCase.class), started()), //
			event(container("dynamicContainerWithIterable"), started()), //
			event(dynamicTestRegistered("dynamic-container:#1")), //
			event(container("dynamic-container:#1"), started()), //
			event(dynamicTestRegistered("dynamic-test:#1")), //
			event(test("dynamic-test:#1", "succeedingTest"), started()), //
			event(test("dynamic-test:#1", "succeedingTest"), finishedSuccessfully()), //
			event(dynamicTestRegistered("dynamic-test:#2")), //
			event(test("dynamic-test:#2", "failingTest"), started()), //
			event(test("dynamic-test:#2", "failingTest"), finishedWithFailure(message("failing"))), //
			event(container("dynamic-container:#1"), finishedSuccessfully()), //
			event(container("dynamicContainerWithIterable"), finishedSuccessfully()), //
			event(container(MyDynamicTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));

		Events containers = executionResults.containerEvents();
		Events tests = executionResults.testEvents();

		assertAll( //
			() -> assertEquals(4, containers.started().count(), "# container started"),
			() -> assertEquals(1, containers.dynamicallyRegistered().count(), "# dynamic containers registered"),
			() -> assertEquals(2, tests.dynamicallyRegistered().count(), "# dynamic tests registered"),
			() -> assertEquals(2, tests.started().count(), "# tests started"),
			() -> assertEquals(1, tests.succeeded().count(), "# tests succeeded"),
			() -> assertEquals(1, tests.failed().count(), "# tests failed"),
			() -> assertEquals(4, containers.finished().count(), "# container finished"));
	}

	@Test
	void singleDynamicTestInNestedDynamicContainerIsExecutedWhenDiscoveredByUniqueId() {
		UniqueId uniqueId = discoverUniqueId(MyDynamicTestCase.class, "twoNestedContainersWithTwoTestsEach") //
				.append(DYNAMIC_CONTAINER_SEGMENT_TYPE, "#1") //
				.append(DYNAMIC_CONTAINER_SEGMENT_TYPE, "#1") //
				.append(DYNAMIC_TEST_SEGMENT_TYPE, "#2");

		EngineExecutionResults executionResults = executeTests(selectUniqueId(uniqueId));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(MyDynamicTestCase.class), started()), //
			event(container("twoNestedContainersWithTwoTestsEach"), started()), //
			event(dynamicTestRegistered(displayName("a"))), //
			event(container(displayName("a")), started()), //
			event(dynamicTestRegistered(displayName("a1"))), //
			event(container(displayName("a1")), started()), //
			event(dynamicTestRegistered("dynamic-test:#2")), //
			event(test("dynamic-test:#2", "failingTest"), started()), //
			event(test("dynamic-test:#2", "failingTest"), finishedWithFailure(message("failing"))), //
			event(container(displayName("a1")), finishedSuccessfully()), //
			event(container(displayName("a")), finishedSuccessfully()), //
			event(container("twoNestedContainersWithTwoTestsEach"), finishedSuccessfully()), //
			event(container(MyDynamicTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void allDynamicTestInNestedDynamicContainerAreExecutedWhenContainerIsDiscoveredByUniqueId() {
		UniqueId uniqueId = discoverUniqueId(MyDynamicTestCase.class, "twoNestedContainersWithTwoTestsEach") //
				.append(DYNAMIC_CONTAINER_SEGMENT_TYPE, "#2") //
				.append(DYNAMIC_CONTAINER_SEGMENT_TYPE, "#1");

		EngineExecutionResults executionResults = executeTests(selectUniqueId(uniqueId));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(MyDynamicTestCase.class), started()), //
			event(container("twoNestedContainersWithTwoTestsEach"), started()), //
			event(dynamicTestRegistered(displayName("b"))), //
			event(container(displayName("b")), started()), //
			event(dynamicTestRegistered(displayName("b1"))), //
			event(container(displayName("b1")), started()), //
			event(dynamicTestRegistered("dynamic-test:#1")), //
			event(test("dynamic-test:#1", "succeedingTest"), started()), //
			event(test("dynamic-test:#1", "succeedingTest"), finishedSuccessfully()), //
			event(dynamicTestRegistered("dynamic-test:#2")), //
			event(test("dynamic-test:#2", "failingTest"), started()), //
			event(test("dynamic-test:#2", "failingTest"), finishedWithFailure(message("failing"))), //
			event(container(displayName("b1")), finishedSuccessfully()), //
			event(container(displayName("b")), finishedSuccessfully()), //
			event(container("twoNestedContainersWithTwoTestsEach"), finishedSuccessfully()), //
			event(container(MyDynamicTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void nestedDynamicContainersAreExecuted() {
		EngineExecutionResults executionResults = executeTests(
			selectMethod(MyDynamicTestCase.class, "nestedDynamicContainers"));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(MyDynamicTestCase.class), started()), //
			event(container("nestedDynamicContainers"), started()), //
			event(dynamicTestRegistered(displayName("gift wrap"))), //
			event(container(displayName("gift wrap")), started()), //
			event(dynamicTestRegistered(displayName("box"))), //
			event(container(displayName("box")), started()), //
			event(dynamicTestRegistered("dynamic-test:#1")), //
			event(test("dynamic-test:#1", "succeedingTest"), started()), //
			event(test("dynamic-test:#1", "succeedingTest"), finishedSuccessfully()), //
			event(dynamicTestRegistered("dynamic-test:#2")), //
			event(test("dynamic-test:#2", "failingTest"), started()), //
			event(test("dynamic-test:#2", "failingTest"), finishedWithFailure(message("failing"))), //
			event(container(displayName("box")), finishedSuccessfully()), //
			event(container(displayName("gift wrap")), finishedSuccessfully()), //
			event(container("nestedDynamicContainers"), finishedSuccessfully()), //
			event(container(MyDynamicTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));

		Events containers = executionResults.containerEvents();
		Events tests = executionResults.testEvents();

		assertAll( //
			() -> assertEquals(5, containers.started().count(), "# container started"),
			() -> assertEquals(2, containers.dynamicallyRegistered().count(), "# dynamic containers registered"),
			() -> assertEquals(2, tests.dynamicallyRegistered().count(), "# dynamic tests registered"),
			() -> assertEquals(2, tests.started().count(), "# tests started"),
			() -> assertEquals(1, tests.succeeded().count(), "# tests succeeded"),
			() -> assertEquals(1, tests.failed().count(), "# tests failed"),
			() -> assertEquals(5, containers.finished().count(), "# container finished"));
	}

	@Test
	void legacyReportingNames() {
		Events dynamicRegistrations = executeTests(selectMethod(MyDynamicTestCase.class, "nestedDynamicContainers"))//
				.allEvents().dynamicallyRegistered();

		// @formatter:off
		Stream<String> legacyReportingNames = dynamicRegistrations
				.map(Event::getTestDescriptor)
				.map(TestDescriptor::getLegacyReportingName);
		assertThat(legacyReportingNames)
				.containsExactly("nestedDynamicContainers()[1]", "nestedDynamicContainers()[1][1]",
						"nestedDynamicContainers()[1][1][1]", "nestedDynamicContainers()[1][1][2]");
		// @formatter:on
	}

	@Test
	void dynamicContainersAreExecutedFromExceptionThrowingStream() {
		EngineExecutionResults executionResults = executeTests(
			selectMethod(MyDynamicTestCase.class, "dynamicContainerWithExceptionThrowingStream"));

		assertTrue(MyDynamicTestCase.exceptionThrowingStreamClosed.get(), "stream should be closed");

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(MyDynamicTestCase.class), started()), //
			event(container("dynamicContainerWithExceptionThrowingStream"), started()), //
			event(dynamicTestRegistered("dynamic-container:#1")), //
			event(container("dynamic-container:#1"), started()), //
			event(dynamicTestRegistered("dynamic-test:#1")), //
			event(test("dynamic-test:#1", "succeedingTest"), started()), //
			event(test("dynamic-test:#1", "succeedingTest"), finishedSuccessfully()), //
			event(dynamicTestRegistered("dynamic-test:#2")), //
			event(test("dynamic-test:#2", "failingTest"), started()), //
			event(test("dynamic-test:#2", "failingTest"), finishedWithFailure(message("failing"))), //
			event(container("dynamic-container:#1"),
				finishedWithFailure(instanceOf(ArrayIndexOutOfBoundsException.class))), //
			event(container("dynamicContainerWithExceptionThrowingStream"), finishedSuccessfully()), //
			event(container(MyDynamicTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));

		Events containers = executionResults.containerEvents();
		Events tests = executionResults.testEvents();

		assertAll( //
			() -> assertEquals(4, containers.started().count(), "# container started"),
			() -> assertEquals(1, containers.dynamicallyRegistered().count(), "# dynamic containers registered"),
			() -> assertEquals(2, tests.dynamicallyRegistered().count(), "# dynamic tests registered"),
			() -> assertEquals(2, tests.started().count(), "# tests started"),
			() -> assertEquals(1, tests.succeeded().count(), "# tests succeeded"),
			() -> assertEquals(1, tests.failed().count(), "# tests failed"),
			() -> assertEquals(4, containers.finished().count(), "# container finished"));
	}

	@Test
	void dynamicContainersChildrenMustNotBeNull() {
		EngineExecutionResults executionResults = executeTests(
			selectMethod(MyDynamicTestCase.class, "dynamicContainerWithNullChildren"));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(MyDynamicTestCase.class), started()), //
			event(container("dynamicContainerWithNullChildren"), started()), //
			event(dynamicTestRegistered("dynamic-container:#1")), //
			event(container("dynamic-container:#1"), started()), //
			event(container("dynamic-container:#1"), //
				finishedWithFailure(message("individual dynamic node must not be null"))), //
			event(container("dynamicContainerWithNullChildren"), finishedSuccessfully()), //
			event(container(MyDynamicTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void testFactoryMethodsMayReturnSingleDynamicContainer() {
		EngineExecutionResults executionResults = executeTests(
			selectMethod(MyDynamicTestCase.class, "singleContainer"));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(MyDynamicTestCase.class), started()), //
			event(container("singleContainer"), started()), //
			event(dynamicTestRegistered("dynamic-container:#1")), //
			event(container("dynamic-container:#1"), started()), //
			event(dynamicTestRegistered("dynamic-test:#1")), //
			event(test("dynamic-test:#1", "succeedingTest"), started()), //
			event(test("dynamic-test:#1", "succeedingTest"), finishedSuccessfully()), //
			event(dynamicTestRegistered("dynamic-test:#2")), //
			event(test("dynamic-test:#2", "failingTest"), started()), //
			event(test("dynamic-test:#2", "failingTest"), finishedWithFailure(message("failing"))), //
			event(container("dynamic-container:#1"), finishedSuccessfully()), //
			event(container("singleContainer"), finishedSuccessfully()), //
			event(container(MyDynamicTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void testFactoryMethodsMayReturnSingleDynamicTest() {
		EngineExecutionResults executionResults = executeTests(selectMethod(MyDynamicTestCase.class, "singleTest"));

		executionResults.allEvents().assertEventsMatchExactly( //
			event(engine(), started()), //
			event(container(MyDynamicTestCase.class), started()), //
			event(container("singleTest"), started()), //
			event(dynamicTestRegistered("dynamic-test:#1")), //
			event(test("dynamic-test:#1", "succeedingTest"), started()), //
			event(test("dynamic-test:#1", "succeedingTest"), finishedSuccessfully()), //
			event(container("singleTest"), finishedSuccessfully()), //
			event(container(MyDynamicTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	static class MyDynamicTestCase {

		private static final List<DynamicTest> list = Arrays.asList(
			dynamicTest("succeedingTest", () -> assertTrue(true, "succeeding")),
			dynamicTest("failingTest", () -> fail("failing")));

		private static final AtomicBoolean exceptionThrowingStreamClosed = new AtomicBoolean(false);

		@TestFactory
		Collection<DynamicTest> dynamicCollection() {
			return list;
		}

		@TestFactory
		Stream<DynamicTest> dynamicStream() {
			return list.stream();
		}

		@TestFactory
		Iterator<DynamicTest> dynamicIterator() {
			return list.iterator();
		}

		@TestFactory
		Iterable<DynamicTest> dynamicIterable() {
			return this::dynamicIterator;
		}

		@TestFactory
		Iterable<DynamicNode> dynamicContainerWithIterable() {
			return Set.of(dynamicContainer("box", list));
		}

		@TestFactory
		Iterable<DynamicNode> nestedDynamicContainers() {
			return Set.of(dynamicContainer("gift wrap", Set.of(dynamicContainer("box", list))));
		}

		@TestFactory
		Stream<DynamicNode> twoNestedContainersWithTwoTestsEach() {
			return Stream.of( //
				dynamicContainer("a", Set.of(dynamicContainer("a1", list))), //
				dynamicContainer("b", Set.of(dynamicContainer("b1", list))) //
			);
		}

		@TestFactory
		Iterable<DynamicNode> dynamicContainerWithExceptionThrowingStream() {
			// @formatter:off
			return Set.of(dynamicContainer("box",
					IntStream.rangeClosed(0, 100)
							.mapToObj(list::get)
							.onClose(() -> exceptionThrowingStreamClosed.set(true))));
			// @formatter:on
		}

		@TestFactory
		Iterable<DynamicNode> dynamicContainerWithNullChildren() {
			return Set.of(dynamicContainer("box", singleton(null)));
		}

		@TestFactory
		DynamicNode singleContainer() {
			return dynamicContainer("box", list);
		}

		@TestFactory
		DynamicNode singleTest() {
			return dynamicTest("succeedingTest", () -> assertTrue(true));
		}

		@TestFactory
		Stream<DynamicNode> threeTests() {
			return Stream.of( //
				dynamicTest("one", () -> assertTrue(true)), //
				dynamicTest("two", () -> assertTrue(true)), //
				dynamicTest("three", () -> assertTrue(true)) //
			);
		}

	}

}
